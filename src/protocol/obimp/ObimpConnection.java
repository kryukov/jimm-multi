/*
 * ObimpConnection.java
 *
 * Created on 5 Декабрь 2010 г., 13:59
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_OBIMP is "true" #
package protocol.obimp;

import java.util.Vector;
import jimm.*;
import jimm.chat.message.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.search.*;
import jimm.util.*;
import protocol.*;
import protocol.net.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ObimpConnection extends ClientConnection {
    private int seq = 0;
    private TcpSocket socket;
    private Obimp obimp;
    private final Vector outgoing = new Vector();
    private Group activeGroup;
    private ObimpContact activeContact;
    private UserInfo userInfo;
    private Search search = null;


    private int getSeq() {
        return seq++;
    }

    /** Creates a new instance of ObimpConnection */
    public ObimpConnection(Obimp obimp) {
        this.obimp = obimp;
    }

    public void disconnect() {
        connect = false;
        obimp = null;
    }
    private void send(ObimpPacket p) throws JimmException {
        socket.write(p.toByteArray(getSeq()));
        socket.flush();
    }
    private ObimpPacket recv() throws JimmException {
        final int headerLength = (1 + 4 + 2 + 2 + 4 + 4);
        byte[] header = new byte[headerLength];
        socket.readFully(header);
        int length = (int)Util.getDWordBE(header, 13);
        byte[] data = new byte[length];
        socket.readFully(data);
        return new ObimpPacket(Util.getWordBE(header, 5),
                Util.getWordBE(header, 7),
                new ObimpData(data));
    }

    protected Protocol getProtocol() {
        return obimp;
    }
    protected void closeSocket() {
        try {
            socket.close();
        } catch (Exception e) {
        }
        socket = null;
    }

    private String toStr(byte[] data) {
        return StringConvertor.utf8beByteArrayToString(data, 0, data.length);
    }
    protected void ping() throws JimmException {
        send(new ObimpPacket(0x0001, 0x0006));
    }
    protected boolean processPacket() throws JimmException {
        ObimpPacket packet = null;
        if (0 < outgoing.size()) {
            synchronized (outgoing) {
                packet = (ObimpPacket)outgoing.elementAt(0);
                outgoing.removeElementAt(0);
            }
        }
        if (null != packet) {
            send(packet);
            return true;
        }
        if (0 < socket.available()) {
            try {
                processPacket(recv());
            } catch (JimmException e) {
                throw e;
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("processPacket", e);
                // #sijapp cond.end#
            }
            return true;
        }
        return false;
    }
    private void processPacket(ObimpPacket pkt) throws JimmException {
        //DebugLog.println("in " + pkt.getType() + " " + pkt.getSubType());
        if (0x0001 == pkt.getType()) {
            if (0x0006 == pkt.getSubType()) {
                send(new ObimpPacket(0x0001, 0x0007));
            }

        } else if (0x0002 == pkt.getType()) {
            if (0x0011 == pkt.getSubType()) { // OBIMP_BEX_CL_SRV_DONE_OFFAUTH
                send(new ObimpPacket(0x0002, 0x0012)); // OBIMP_BEX_CL_CLI_DEL_OFFAUTH
            } else if (0x000D == pkt.getSubType()) { // OBIMP_BEX_CL_CLI_SRV_AUTH_REQUEST
                ObimpData authinfo = pkt.getData();
                String from = "";
                String reasone = "";
                boolean isOffline = false;
                long time = 0;
                while (!authinfo.isEof()) {
                    byte[] data = authinfo.getWtldData();
                    switch (authinfo.getWtldType()) {
                        case 1:
                            from = toStr(data);
                            break;
                        case 2:// message
                            break;
                        case 3:
                            isOffline = true;
                            break;
                        case 4:
                            time = Util.getDWordBE(data, 0);
                            break;
                    }
                    authinfo.skipWtld();
                }
                obimp.addMessage(new SystemNotice(obimp, SystemNotice.SYS_NOTICE_AUTHREQ, from, null));
            } else if (0x000E == pkt.getSubType()) { // OBIMP_BEX_CL_CLI_SRV_AUTH_REPLY
                ObimpData authinfo = pkt.getData();
                String from = "";
                boolean auth = false;
                while (!authinfo.isEof()) {
                    byte[] data = authinfo.getWtldData();
                    switch (authinfo.getWtldType()) {
                        case 1:
                            from = toStr(data);
                            break;
                        case 2:
                            auth = (0x0001 == Util.getWordBE(data, 0));
                            break;
                    }
                    authinfo.skipWtld();
                }
                if (auth) {
                    Contact c = obimp.getItemByUIN(from);
                    if (null != c) {
                        c.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        obimp.ui_changeContactStatus(c);
                    }
                }
            } else if (0x000F == pkt.getSubType()) { // OBIMP_BEX_CL_CLI_SRV_AUTH_REVOKE
                ObimpData authinfo = pkt.getData();
                String from = "";
                while (!authinfo.isEof()) {
                    byte[] data = authinfo.getWtldData();
                    switch (authinfo.getWtldType()) {
                        case 1:
                            from = toStr(data);
                            break;
                    }
                    authinfo.skipWtld();
                }
                Contact c = obimp.getItemByUIN(from);
                if (null != c) {
                    c.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
                    obimp.ui_changeContactStatus(c);
                }
            } else if (0x0008 == pkt.getSubType()) { // OBIMP_BEX_CL_SRV_ADD_ITEM_REPLY
                ObimpData authinfo = pkt.getData();
                long id = 0;
                int code = 0;
                while (!authinfo.isEof()) {
                    byte[] data = authinfo.getWtldData();
                    switch (authinfo.getWtldType()) {
                        case 1:
                            code = Util.getWordBE(data, 0);
                            break;
                        case 2:
                            id = Util.getDWordBE(data, 0);
                            break;
                    }
                    authinfo.skipWtld();
                }
                if (0 == code) {
                    if (null != activeGroup) {
                        activeGroup.setGroupId((int)id);
                    }
                    if (null != activeContact) {
                        activeContact.setId((int)id);
                    }
                }
                activeGroup = null;
                activeContact = null;
            }

        } else if (0x0003 == pkt.getType()) {
            if (0x0006 == pkt.getSubType()) {// OBIMP_BEX_PRES_SRV_CONTACT_ONLINE
                ObimpData prsinfo = pkt.getData();
                String uid = prsinfo.getWtld_str(1);
                long status = prsinfo.getWtld_dword(2);
                String statusText = prsinfo.getWtld_str(3);
                long xstatus = prsinfo.getWtld_dword(4) - 1;
                String xstatusDesc = prsinfo.getWtld_str(5);
                ObimpContact c = (ObimpContact)obimp.getItemByUIN(uid);
                if (null != c) {
                    obimp.setContactStatus(c, status2index(status), statusText);
                    c.setXStatus(xstatus, xstatusDesc);
                    obimp.ui_changeContactStatus(c);
                }

            } else if (0x0007 == pkt.getSubType()) {// OBIMP_BEX_PRES_SRV_CONTACT_OFFLINE
                String uid = toStr(pkt.getData().getWtldData());
                Contact c = obimp.getItemByUIN(uid);
                if (null != c) {
                    c.setOfflineStatus();
                    obimp.ui_changeContactStatus(c);
                }
            }

        } else if (0x0004 == pkt.getType()) {
            if (0x0004 == pkt.getSubType()) {// OBIMP_BEX_IM_SRV_DONE_OFFLINE
                // - OBIMP_BEX_IM_CLI_DEL_OFFLINE
                send(new ObimpPacket(0x0004, 0x0005));

            } else if (0x0007 == pkt.getSubType()) {//OBIMP_BEX_IM_SRV_MESSAGE
                ObimpData msginfo = pkt.getData();
                String from = msginfo.getWtld_str(0x0001);
                long messageId = msginfo.getWtld_dword(0x0002);
                long messageType = msginfo.getWtld_dword(0x0003);
                String message = msginfo.getWtld_str(0x0004);
                boolean isOffline = (null != msginfo.getWtld(0x0007));
                long time = Jimm.getCurrentGmtTime();
                byte[] data = msginfo.getWtld(0x0008);
                if (null != data) {
                    time = (Util.getDWordBE(data, 0) << 32) | Util.getDWordBE(data, 4);
                }
                PlainMessage msg = new PlainMessage(from, obimp, time, message, isOffline);
                obimp.addMessage(msg);
                if (null != msginfo.getWtld(0x0005)) {
                    ObimpPacket notify = new ObimpPacket(0x0004, 0x0008);
                    notify.writeWtld_str(0x0001, from);
                    notify.writeWtld_long(0x0002, messageId);
                    send(notify);
                }

            } else if (0x0008 == pkt.getSubType()) {//OBIMP_BEX_IM_CLI_SRV_MSG_REPORT
                setMessageSended(pkt.getData().getWtld_dword(0x0002));
            }

        } else if (0x0005 == pkt.getType()) {
            if (0x0004 == pkt.getSubType()) {
                ObimpData userinfo = pkt.getData();
                int code = userinfo.getWtld_word(0x0001);
                String userid = userinfo.getWtld_str(0x0002);
                UserInfo info = userInfo;
                if (!info.realUin.equals(userid)) {
                    return;
                }
                userInfo = null;
                info.uin = userid;
                if (0x0000 != code) {
                    info.updateProfileView();
                    return;
                }
                info.nick = userinfo.getWtld_str(0x0004);
                info.firstName = userinfo.getWtld_str(0x0005);
                info.lastName = userinfo.getWtld_str(0x0006);
                info.homeAddress = userinfo.getWtld_str(0x000B);
                info.gender = userinfo.getWtld_byte(0x000E);
                info.homePage = userinfo.getWtld_str(0x0010);
                info.about = userinfo.getWtld_str(0x0011);
                info.interests = userinfo.getWtld_str(0x0012);
                info.email = userinfo.getWtld_str(0x0013);
                info.homeState = userinfo.getWtld_str(0x0008);
                info.homeCity = userinfo.getWtld_str(0x0009);
                info.homePhones = userinfo.getWtld_str(0x0015);
                info.workPhone = userinfo.getWtld_str(0x0016);
                info.cellPhone = userinfo.getWtld_str(0x0017);
                info.workFax = userinfo.getWtld_str(0x0018);
                info.workCompany = userinfo.getWtld_str(0x001A);
                info.workDepartment = userinfo.getWtld_str(0x001B);
                info.workPosition = userinfo.getWtld_str(0x001C);
                info.updateProfileView();

            } else if (0x0008 == pkt.getSubType()) {
                Search s = search;
                if (null == s) {
                    return;
                }
                ObimpData searchinfo = pkt.getData();
                int code = searchinfo.getWtld_word(0x0001);
                if (0x0000 == code) {
                    UserInfo result = new UserInfo(obimp, "");
                    result.uin = searchinfo.getWtld_str(0x0002);
                    result.nick = searchinfo.getWtld_str(0x0003);
                    result.firstName = searchinfo.getWtld_str(0x0004);
                    result.lastName = searchinfo.getWtld_str(0x005);
                    result.age = searchinfo.getWtld_byte(0x0007);
                    result.gender = searchinfo.getWtld_byte(0x0006);
                    search.addResult(result);
                }
                if ((0x0000 != code) || (null != searchinfo.getWtld(0x0009))) {
                    search.finished();
                    search = null;
                }
            }
        }
        // - OBIMP_BEX_CL_CLI_DEL_OFFAUTH
    }


    protected void connect() throws JimmException {
        connect = true;
        setPingInterval(3 * 60);
        obimp.setConnectingProgress(0);
        String server = obimp.getServer();
        String account = obimp.getUserId();
        String password = obimp.getPassword();


        socket = new TcpSocket();
        socket.connectTo("socket://" + server + ":7023");

        //- OBIMP_BEX_COM_CLI_HELLO
        //wTLD 0x0001: UTF8, account name
        ObimpPacket hi = new ObimpPacket(0x0001, 0x0001);
        hi.writeWtld_str(0x0001, account);
        send(hi);

        //- OBIMP_BEX_COM_SRV_HELLO
        //wTLD 0x0001: Word, hello error (HELLO_ERROR_CODE)
        //- OBIMP_BEX_COM_SRV_HELLO
        //wTLD 0x0002: BLK, server key to generate one-time MD5 password hash
        //- OBIMP_BEX_COM_SRV_HELLO
        //wTLD 0x0007: empty, server requires plain-text password authentication
        hi = recv();
        obimp.setConnectingProgress(20);

        int type = hi.getData().getWtldType();
        byte[] data = hi.getData().getWtldData();

        ObimpPacket login = new ObimpPacket(0x0001, 0x0003);
        login.writeWtld_str(0x0001, account);
        if (0x0001 == type) {
            obimp.setPassword(null);
            throw new JimmException(111, 0);

        } else if (0x0002 == type) {
            MD5 md5 = new MD5();
            md5.init();
            md5.updateASCII(account);
            md5.updateASCII("OBIMPSALT");
            md5.updateASCII(password);
            md5.finish();
            byte[] hash = md5.getDigestBits();

            md5 = new MD5();
            md5.init();
            md5.update(hash);
            md5.update(data);
            md5.finish();
            hash = md5.getDigestBits();

            login.writeWtld(0x0002, hash);

        } else if (0x0007 == type) {
            login.writeWtld(0x0003, StringConvertor.stringToByteArrayUtf8(password));
        }
        send(login);

        //- OBIMP_BEX_COM_SRV_LOGIN_REPLY
        login = recv();
        //a) wTLD 0x0001: Word, login error (LOGIN_ERROR_CODE)
        //Optional: wTLD 0x0007: UTF8, password reminder URL if wrong password code
        //b) wTLD 0x0002: array of Word, server supported BEXs
        //wTLD 0x0003: LongWord, maximal client BEXs data BEngth
        //c) wTLD 0x0004: UTF8, new server host/ip
        //wTLD 0x0005: LongWord, new server port number
        //wTLD 0x0006: BLK, unique server cookie
        type = login.getData().getWtldType();
        data = login.getData().getWtldData();

        if ((0x0002 == type) || (0x0003 == type)) {
            // loged in
        } else {
            //obimp.setConnectingProgress(-1);
            obimp.setPassword(null);
            throw new JimmException(111, 0);
        }
        obimp.setConnectingProgress(50);
        // - OBIMP_BEX_PRES_CLI_SET_CAPS
        ObimpPacket caps = new ObimpPacket(0x0003, 0x0003);
        caps.writeWtld(0x0001, new byte[] {00, 01});
        caps.writeWtld(0x0002, new byte[] {00, 01});
        caps.writeWtld(0x0003, StringConvertor.stringToByteArrayUtf8("Bimoid Mobile"));
        caps.writeWtld(0x0004, getClientVersion());
        send(caps);


        // - OBIMP_BEX_CL_CLI_REQUEST 0x2, 0x3
        send(new ObimpPacket(0x0002, 0x0003));
        for (;;) {
            ObimpPacket roster = recv();
            if ((0x0002 == roster.getType()) && (0x0004 == roster.getSubType())) {
                processRoster(roster);
                break;
            }
        }
        // - OBIMP_BEX_CL_CLI_VERIFY 0x2, 0x5
        //send(new ObimpPacket(0x0002, 0x0005));

        send(makeStatusPacket());

        // - OBIMP_BEX_PRES_CLI_ACTIVATE
        send(new ObimpPacket(0x0003, 0x0005));


        // - OBIMP_BEX_IM_CLI_REQ_OFFLINE
        send(new ObimpPacket(0x0004, 0x0003));
        // - OBIMP_BEX_CL_CLI_REQ_OFFAUTH
        send(new ObimpPacket(0x0002, 0x0010));
        obimp.setConnectingProgress(100);
    }

    private byte[] getClientVersion() {
        byte[] version = new byte[] {00, 00,  00, 00,  00, 00,  00, 00};
        int index = 0;
        int value = 0;
        for (int i = 0; i < Jimm.VERSION.length(); ++i) {
            char ch = Jimm.VERSION.charAt(i);
            if ('.' == ch) {
                Util.putWordBE(version, index * 2, value);
                index++;
                value = 0;
                if (index > 3) {
                    break;
                }

            } else if (('0' <= ch) && (ch <= '9')) {
                value = value * 10 + (ch - '0');

            } else {
                Util.putWordBE(version, index * 2, value);
                break;
            }
        }
        return version;
    }

    private void processRoster(ObimpPacket packet) {
        TemporaryRoster roster = new TemporaryRoster(obimp);
        obimp.setContactListStub();
        ObimpData cl = new ObimpData(packet.getData().getWtldData());

        Group general = roster.makeGroup(JLocale.getString("group_general"));
        general.setGroupId(0);
        roster.addGroup(general);

        long number = cl.getDWordBE();
        while (!cl.isEof()) {
            int type = cl.getWordBE();
            int userIndex = (int)cl.getDWordBE();
            int groupIndex = (int)cl.getDWordBE();
            ObimpData contact = new ObimpData(cl.getData((int)cl.getDWordBE()));
            String name = null;
            String userId = null;
            byte privacy = 0;
            boolean auth = true;
            boolean serverside = false;
            while (!contact.isEof()) {
                byte[] data = contact.getStldData();
                switch (contact.getStldType()) {
                    case 1:
                    case 3:
                        name = StringConvertor.utf8beByteArrayToString(data, 0, data.length);
                        break;
                    case 2:
                        userId = StringConvertor.utf8beByteArrayToString(data, 0, data.length);
                        break;
                    case 4:
                        privacy = data[0];
                        break;
                    case 5:
                        auth = false;
                        break;
                    case 6:
                        serverside = true;
                        break;
                }
                contact.skipStld();
            }
            switch (type) {
                case 1:
                    Group g = roster.makeGroup(name);
                    g.setGroupId(userIndex);
                    System.out.println(name + " " + userIndex + " " + groupIndex);
                    roster.addGroup(g);
                    break;

                case 2:
                    Contact c = roster.makeContact(userId);
                    c.setName(name);
                    c.setBooleanValue(Contact.CONTACT_NO_AUTH, !auth);
                    c.setGroupId(groupIndex);
                    ((ObimpContact)c).setId(userIndex);
                    ((ObimpContact)c).setPrivacyType(privacy);
                    ((ObimpContact)c).setGeneral(serverside);
                    System.out.println(userId + " " + name + " ");
                    roster.addContact(c);
                    break;
            }
        }
        obimp.setContactList(roster.getGroups(), roster.mergeContacts());
    }

    private byte status2index(long statusCode) {
        byte index = StatusInfo.STATUS_ONLINE;
        switch ((int)(statusCode % 0x10)) {
            case 0: index = StatusInfo.STATUS_ONLINE; break;
            case 1: index = StatusInfo.STATUS_INVISIBLE; break;
            case 2: index = StatusInfo.STATUS_INVIS_ALL; break;
            case 3: index = StatusInfo.STATUS_CHAT; break;
            case 4: index = StatusInfo.STATUS_HOME; break;
            case 5: index = StatusInfo.STATUS_WORK; break;
            case 6: index = StatusInfo.STATUS_LUNCH; break;
            case 7: index = StatusInfo.STATUS_AWAY; break;
            case 8: index = StatusInfo.STATUS_NA; break;
            case 9: index = StatusInfo.STATUS_OCCUPIED; break;
            case 10: index = StatusInfo.STATUS_DND; break;
        }
        return index;
    }
    private long index2status(byte index) {
        switch (index) {
            case StatusInfo.STATUS_ONLINE: return 0;
            case StatusInfo.STATUS_INVISIBLE: return 1;
            case StatusInfo.STATUS_INVIS_ALL: return 2;
            case StatusInfo.STATUS_CHAT: return 3;
            case StatusInfo.STATUS_HOME: return 4;
            case StatusInfo.STATUS_WORK: return 5;
            case StatusInfo.STATUS_LUNCH: return 6;
            case StatusInfo.STATUS_AWAY: return 7;
            case StatusInfo.STATUS_NA: return 8;
            case StatusInfo.STATUS_OCCUPIED: return 9;
            case StatusInfo.STATUS_DND: return 10;
        }
        return 0;
    }
    private ObimpPacket makeStatusPacket() {
        // - OBIMP_BEX_PRES_CLI_SET_STATUS
        ObimpPacket status = new ObimpPacket(0x0003, 0x0004);
        status.writeWtld_long(0x0001, index2status(obimp.getProfile().statusIndex));
        status.writeWtld_str(0x0002, "");
        status.writeWtld_long(0x0003, obimp.getProfile().xstatusIndex + 1);
        status.writeWtld_str(0x0004, StringConvertor.notNull(obimp.getProfile().xstatusTitle));
        return status;
    }
    public void removeItem(long id) {
        ObimpPacket packet = new ObimpPacket(0x0002, 0x0009);
        packet.writeWtld_long(0x0001, id);
        put(packet);
    }
    private byte[] packGroupInfo(Group g) {
        byte[] name = StringConvertor.stringToByteArrayUtf8(g.getName());
        byte[] result = new byte[4 + name.length];
        Util.putWordBE(result, 0, 0x0001);
        Util.putWordBE(result, 2, name.length);
        System.arraycopy(name, 0, result, 4, name.length);
        return result;
    }
    private byte[] packContactInfo(ObimpContact c) {
        byte[] id = StringConvertor.stringToByteArrayUtf8(c.getUserId());
        byte[] name = StringConvertor.stringToByteArrayUtf8(c.getName());
        int auth = c.isAuth() ? 0 : 4;
        int general = c.isGeneral() ? 4 : 0;
        byte[] result = new byte[4 + id.length + 4 + name.length + 5 + auth + general];
        int ip = 0;
        Util.putWordBE(result, ip + 0, 0x0002);
        Util.putWordBE(result, ip + 2, id.length);
        System.arraycopy(id, 0, result, ip + 4, id.length);
        ip += 4 + id.length;
        Util.putWordBE(result, ip + 0, 0x0003);
        Util.putWordBE(result, ip + 2, name.length);
        System.arraycopy(name, 0, result, ip + 4, name.length);
        ip += 4 + name.length;
        Util.putWordBE(result, ip + 0, 0x0004);
        Util.putWordBE(result, ip + 2, 1);
        result[ip + 4] = c.getPrivacyType();
        ip += 5;
        if (0 != auth) {
            Util.putDWordBE(result, ip, 0x00050000);
            ip += 4;
        }
        if (0 != general) {
            Util.putDWordBE(result, ip, 0x00060000);
            ip += 4;
        }
        return result;
    }
    public void addGroup(Group g) {
        activeGroup = g;
        // OBIMP_BEX_CL_CLI_ADD_ITEM
        ObimpPacket packet = new ObimpPacket(0x0002, 0x0007);
        packet.writeWtld_word(0x0001, 0x0001);
        packet.writeWtld_long(0x0002, 0);
        packet.writeWtld(0x0003, packGroupInfo(g));
        put(packet);
    }
    public void updateGroup(Group g) {
        // OBIMP_BEX_CL_CLI_UPD_ITEM
        ObimpPacket packet = new ObimpPacket(0x0002, 0x000B);
        packet.writeWtld_long(0x0001, g.getId());
        packet.writeWtld_long(0x0002, 0);
        packet.writeWtld(0x0003, packGroupInfo(g));
        put(packet);
    }
    public void addContact(ObimpContact c) {
        activeContact = c;
        c.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
        // OBIMP_BEX_CL_CLI_ADD_ITEM
        ObimpPacket packet = new ObimpPacket(0x0002, 0x0007);
        packet.writeWtld_word(0x0001, 0x0002);
        packet.writeWtld_long(0x0002, c.getGroupId());
        packet.writeWtld(0x0003, packContactInfo(c));
        put(packet);
    }
    public void updateContact(ObimpContact c, int groupId) {
        // OBIMP_BEX_CL_CLI_UPD_ITEM
        ObimpPacket packet = new ObimpPacket(0x0002, 0x000B);
        packet.writeWtld_long(0x0001, c.getId());
        packet.writeWtld_long(0x0002, groupId);
        packet.writeWtld(0x0003, packContactInfo(c));
        put(packet);
    }
    public void sendAuthReply(String userId, boolean auth) {
        // OBIMP_BEX_CL_CLI_SRV_AUTH_REPLY
        ObimpPacket packet = new ObimpPacket(0x0002, 0x000E);
        packet.writeWtld_str(0x0001, userId);
        packet.writeWtld_word(0x0002, auth ? 0x0001 : 0x0002);
        put(packet);
    }

    public void sendAuthRequest(String userId) {
        // OBIMP_BEX_CL_CLI_SRV_AUTH_REQUEST
        ObimpPacket packet = new ObimpPacket(0x0002, 0x000D);
        packet.writeWtld_str(0x0001, userId);
        packet.writeWtld_str(0x0002, "");
        put(packet);
    }
    public void sendStatus() {
        put(makeStatusPacket());
    }

    public void sendMessage(PlainMessage message) {
        String to = message.getRcvrUin();
        String msg = message.getText();
        int id = Util.uniqueValue();
        message.setMessageId(id);
        ObimpPacket packet = new ObimpPacket(0x0004, 0x0006);
        packet.writeWtld_str(0x0001, to);
        packet.writeWtld_long(0x0002, id);
        packet.writeWtld_long(0x0003, 0x00000001);
        packet.writeWtld_str(0x0004, msg);
        packet.writeWtld_flag(0x0005);
        put(packet);
        if (true) {
            addMessage(message);
        }
    }
    public void saveVCard(UserInfo userInfo) {
        ObimpPacket packet = new ObimpPacket(0x0005, 0x0005);
        packet.writeWtld_str(0x0001, userInfo.realUin);
        packet.writeWtld_str(0x0002, userInfo.nick);
        packet.writeWtld_str(0x0003, userInfo.firstName);
        packet.writeWtld_str(0x0004, userInfo.lastName);
        packet.writeWtld_str(0x0006, userInfo.homeState);
        packet.writeWtld_str(0x0007, userInfo.homeCity);
        packet.writeWtld_str(0x0009, userInfo.homeAddress);
        packet.writeWtld_byte(0x000C, userInfo.gender);
        packet.writeWtld_str(0x000E, userInfo.homePage);
        packet.writeWtld_str(0x000F, userInfo.about);
        packet.writeWtld_str(0x0011, userInfo.email);
        packet.writeWtld_str(0x0013, userInfo.homePhones);
        packet.writeWtld_str(0x0014, userInfo.workPhone);
        packet.writeWtld_str(0x0015, userInfo.cellPhone);
        packet.writeWtld_str(0x0018, userInfo.workCompany);
        packet.writeWtld_str(0x0019, userInfo.workDepartment);
        packet.writeWtld_str(0x001A, userInfo.workPosition);
        put(packet);
    }
    public UserInfo getUserInfo(ObimpContact contact) {
        UserInfo info = new UserInfo(obimp, contact.getUserId());
        ObimpPacket packet = new ObimpPacket(0x0005, 0x0003);
        userInfo = info;
        packet.writeWtld_str(0x0001, info.realUin);
        put(packet);
        return info;
    }
    void searchUsers(Search cont) {
        this.search = cont;
        ObimpPacket packet = new ObimpPacket(0x0005, 0x0007);
        packet.writeWtld_notNullStr(0x0001, cont.getSearchParam(Search.UIN));
        packet.writeWtld_notNullStr(0x0002, cont.getSearchParam(Search.EMAIL));
        packet.writeWtld_notNullStr(0x0003, cont.getSearchParam(Search.NICK));
        packet.writeWtld_notNullStr(0x0004, cont.getSearchParam(Search.FIRST_NAME));
        packet.writeWtld_notNullStr(0x0005, cont.getSearchParam(Search.LAST_NAME));
        packet.writeWtld_notNullStr(0x0002, cont.getSearchParam(Search.EMAIL));
        packet.writeWtld_notNullStr(0x0007, cont.getSearchParam(Search.CITY));
        if ("1".equals(cont.getSearchParam(Search.ONLY_ONLINE))) {
            packet.writeWtld(0x000E, new byte[0]);
        }
        int g = Util.strToIntDef(cont.getSearchParam(Search.GENDER), 0);
        if (0 < g) {
            packet.writeWtld_byte(0x0009, g);
        }
        String[] age = Util.explode(cont.getSearchParam(Search.AGE), '-');
        if (2 == age.length) {
            if (0 < Util.strToIntDef(age[0], 0)) {
                packet.writeWtld_byte(0x000A, Util.strToIntDef(age[0], 1));
            }
            if (Util.strToIntDef(age[1], 100) < 99) {
                packet.writeWtld_byte(0x000B, Util.strToIntDef(age[1], 99));
            }
        }
        //DebugLog.dump("searchUsers", packet.toByteArray(0));
        put(packet);
    }


    private void put(ObimpPacket packet) {
        synchronized (outgoing) {
            outgoing.addElement(packet);
        }
    }

    private void setMessageSended(long msgId) {
        markMessageSended(msgId, PlainMessage.NOTIFY_FROM_CLIENT);
    }
}
// #sijapp cond.end #