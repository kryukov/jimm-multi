/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the  Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/comm/Icq.java
 Version: ###VERSION###  Date: ###DATE###
 Author: Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq;

import DrawControls.icons.*;
import java.io.*;
import java.util.*;
import jimm.chat.message.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.forms.*;
import jimm.search.UserInfo;
import protocol.icq.action.*;
import protocol.icq.packet.*;
import jimm.search.Search;
import jimm.util.JLocale;
import jimm.*;
import protocol.*;
import protocol.icq.plugin.*;

public class Icq extends Protocol {
    private static final int[] statusIconIndex = {1, 0, 4, 3, 10, 11, 8, 9, 12, 5, 6, 7, 2, 2, 1};
    private static final ImageList statusIcons = ImageList.createImageList("/icq-status.png");

    private IcqNetWorking connection = null;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public static final IcqXStatus xstatus = new IcqXStatus();
    // #sijapp cond.end #

    public static final byte PSTATUS_ALL           = 0x01;
    public static final byte PSTATUS_NONE          = 0x02;
    public static final byte PSTATUS_VISIBLE_ONLY  = 0x03;
    public static final byte PSTATUS_NOT_INVISIBLE = 0x04;
    public static final byte PSTATUS_CL_ONLY       = 0x05;
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private Vector ignoreList    = new Vector();
    private Vector invisibleList = new Vector();
    private Vector visibleList   = new Vector();
    // #sijapp cond.end #

    public Icq() {
    }
    public String getUserIdName() {
        return "UIN";
    }
    protected void initStatusInfo() {
        info = new StatusInfo(statusIcons, statusIconIndex);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatusInfo = Icq.xstatus.getInfo();
        // #sijapp cond.end #
        // #sijapp cond.if modules_CLIENTS is "true" #
        clientInfo = ClientDetector.instance.get();
        // #sijapp cond.end #
    }

    public boolean isEmpty() {
        return super.isEmpty() || ((0 == Util.strToIntDef(getUserId(), 0)) && (getUserId().indexOf('@') <= 0));
    }

    public final void setRealUin(String uin) {
        if (!StringConvertor.isEmpty(uin)) {
            setUserId(uin);
        }
    }


    public void requestSimpleAction(IcqAction act) {
        if (null != connection) {
            connection.requestAction(act);
        }
    }
    private void requestSimpleAction(Packet pkt) {
        requestSimpleAction(new OtherAction(pkt));
    }
    /**
     * Construct plain message object and request new SendMessageAction
     * Add the new message to the chat history
     */
    protected void sendSomeMessage(PlainMessage msg) {
        msg.setMessageId(Util.uniqueValue());
        // Get UIN
        byte[] uinRaw = StringConvertor.stringToByteArray(msg.getRcvrUin());

        String text = StringConvertor.restoreCrLf(msg.getText());

        Util buffer = new Util();

        if (true) {
            byte[] textRaw = StringConvertor.stringToUcs2beByteArray(text);

            buffer.writeDWordBE(msg.getMessageId()); // CLI_SENDMSG.TIME
            buffer.writeDWordBE(0x00000000);         // CLI_SENDMSG.ID
            buffer.writeWordBE(0x0001); // CLI_SENDMSG.FORMAT
            buffer.writeByte(uinRaw.length); // CLI_SENDMSG.UIN
            buffer.writeByteArray(uinRaw);

            buffer.writeWordBE(0x0002); // CLI_SENDMSG.SUB_MSG_TYPE1
            buffer.writeWordBE(5 + 4 + 4 + textRaw.length);
            buffer.writeTLVByte(0x0501, 0x01); // SUB_MSG_TYPE1.CAPABILITIES

            buffer.writeWordBE(0x0101); // SUB_MSG_TYPE1.MESSAGE
            buffer.writeWordBE(4 + textRaw.length);
            buffer.writeDWordBE(0x00020000); // MESSAGE.ENCODING
            buffer.writeByteArray(textRaw); // MESSAGE.MESSAGE
        } else {
            byte[] textRaw = StringConvertor.stringToByteArrayUtf8(text);
            Util tlv1127 = new Util();

            // Put 0x1b00
            tlv1127.writeWordLE(0x001B); // length

            // Put ICQ protocol version in LE
            tlv1127.writeWordLE(0x0008);

            // Put capablilty (16 zero bytes)
            tlv1127.writeDWordBE(0x00000000);
            tlv1127.writeDWordBE(0x00000000);
            tlv1127.writeDWordBE(0x00000000);
            tlv1127.writeDWordBE(0x00000000);

            // Put some unknown stuff
            tlv1127.writeWordLE(0x0000);
            tlv1127.writeByte(0x03);

            // Set the DC_TYPE to "normal" if we send a file transfer request
            tlv1127.writeDWordBE(0x00000000);

            // Put cookie, unkown 0x0e00 and cookie again
            final int SEQ1 = 0xffff;
            tlv1127.writeWordLE(SEQ1);
            tlv1127.writeWordLE(0x000E); // length
            tlv1127.writeWordLE(SEQ1);

            // Put 12 unknown zero bytes
            tlv1127.writeDWordLE(0x00000000);
            tlv1127.writeDWordLE(0x00000000);
            tlv1127.writeDWordLE(0x00000000);

            tlv1127.writeWordLE(0x0001); // Put message type
            tlv1127.writeWordLE(0); // Put contact statu

            // Put priority
            tlv1127.writeWordLE(0x0001);

            // Put message
            tlv1127.writeWordLE(textRaw.length + 1);
            tlv1127.writeByteArray(textRaw);
            tlv1127.writeByte(0x00);

            // Put foreground, background color and guidlength
            tlv1127.writeDWordBE(0x00000000);
            tlv1127.writeDWordBE(0x00FFFFFF);

            byte[] utf8 = "{0946134E-4C7F-11D1-8222-444553540000}".getBytes();
            tlv1127.writeDWordLE(utf8.length);
            tlv1127.writeByteArray(utf8);

            Util tlv5 = new Util();
            tlv5.writeWordBE(0x0000);
            tlv5.writeDWordBE(msg.getMessageId());
            tlv5.writeDWordBE(0x00000000);
            tlv5.writeByteArray(GUID.CAP_AIM_SERVERRELAY.toByteArray());
            tlv5.writeTLVWord(0x000a, 0x0001);
            tlv5.writeTLV(0x000f, null);

            // Set TLV 0x2711
            tlv5.writeTLV(0x2711, tlv1127.toByteArray());

            buffer.writeDWordBE(msg.getMessageId()); // CLI_SENDMSG.TIME
            buffer.writeDWordBE(0x00000000);         // CLI_SENDMSG.ID
            buffer.writeWordBE(0x0002); // CLI_SENDMSG.FORMAT
            buffer.writeByte(uinRaw.length); // CLI_SENDMSG.UIN
            buffer.writeByteArray(uinRaw);
            buffer.writeTLV(0x0005, tlv5.toByteArray());
        }

        // flag of delivery to server
        buffer.writeTLV(0x0003, null);
        // store message if recipient offline
        buffer.writeTLV(0x0006, null);

        // Send packet
        byte[] buf = buffer.toByteArray();
        connection.addMessage(msg);
        requestSimpleAction(new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                SnacPacket.CLI_SENDMSG_COMMAND, buf));
    }

    protected void s_addGroup(Group group) {
        IcqAction act = new UpdateContactListAction(group,
                UpdateContactListAction.ACTION_ADD);
        requestSimpleAction(act);
    }

    // Adds a IcqContact to the server saved contact list
    protected void s_addContact(Contact contact) {
        // Request contact item adding
        IcqAction act = new UpdateContactListAction(this, contact, UpdateContactListAction.ACTION_ADD);
        requestSimpleAction(act);
    }

    /** Connects to the ICQ network */
    protected void startConnection() {
        connection = new IcqNetWorking();
        connection.initNet(this);
        ConnectAction act = new ConnectAction(this);
        connection.requestAction(act);
        act.initProgressBar();
        connection.start();
    }

    /** Disconnects from the ICQ network */
    protected void closeConnection() {
        IcqNetWorking icqState = connection;
        connection = null;
        if (null != icqState) {
            icqState.disconnect();
        }
    }

    // Checks whether the comm. subsystem is in STATE_CONNECTED
    public boolean isConnected() {
        return (null != connection) && connection.isIcqConnected();
    }

    // Dels a IcqContact to the server saved contact list
    protected void s_removeContact(Contact cItem) {
        requestSimpleAction(new UpdateContactListAction(this, cItem,
                UpdateContactListAction.ACTION_DEL));
    }

    /** ********************************************************************* */
    protected void doAction(Contact contact, int action) {
        switch (action) {
            case IcqContact.USER_MENU_REMOVE_ME:
                // Remove me from other users contact list
                sendRemoveMePacket(contact.getUserId());
                ContactList.getInstance().activate();
                break;

            // #sijapp cond.if modules_SERVERLISTS is "true" #
            case Contact.USER_MENU_PS_VISIBLE:
            case Contact.USER_MENU_PS_INVISIBLE:
            case Contact.USER_MENU_PS_IGNORE:
                int list = IGNORE_LIST;
                switch (action) {
                    case Contact.USER_MENU_PS_VISIBLE:
                        list = VISIBLE_LIST;
                        break;
                    case Contact.USER_MENU_PS_INVISIBLE:
                        list = INVISIBLE_LIST;
                        break;
                    case Contact.USER_MENU_PS_IGNORE:
                        list = IGNORE_LIST;
                        break;
                }
                changeServerList(list, (IcqContact) contact);
                ContactList.getInstance().activate();
                break;
            // #sijapp cond.end #
        }
    }

    /** ********************************************************************* */
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void requestXStatusMessage(Contact c) {
        StringBuffer str = new StringBuffer();
        str.append("<N><QUERY>");
        str.append(Util.xmlEscape("<Q><PluginID>srvMng</PluginID></Q>"));
        str.append("</QUERY><NOTIFY>");
        str.append(Util.xmlEscape("<srv><id>cAwaySrv</id><req><id>AwayStat</id><trans>1</trans><senderId>"
                + getUserId()
                + "</senderId></req></srv>"));
        str.append("</NOTIFY></N>");
        XtrazMessagePlugin plugin = new XtrazMessagePlugin((IcqContact)c, str.toString());
        requestSimpleAction(plugin.getPacket());
        ((IcqContact)c).setXStatusMessage("");
        updateStatusView(c);
    }
    // #sijapp cond.end #

    /** *********************************************************************** */
    public boolean isMeVisible(Contact to) {
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        switch (getPrivateStatus()) {
            case PrivateStatusForm.PSTATUS_NONE:
                return false;

            case PrivateStatusForm.PSTATUS_NOT_INVISIBLE:
                return !to.inInvisibleList();

            case PrivateStatusForm.PSTATUS_VISIBLE_ONLY:
                return to.inVisibleList();

            case PrivateStatusForm.PSTATUS_CL_ONLY:
                return !to.isTemp();
        }
        // #sijapp cond.else #
        long status = getProfile().statusIndex;
        if ((StatusInfo.STATUS_INVISIBLE == status)
                || (StatusInfo.STATUS_INVIS_ALL == status)) {
            return false;
        }
        // #sijapp cond.end #
        return true;
    }
    public UserInfo getUserInfo(Contact c) {
        UserInfo data = new UserInfo(this);
        RequestInfoAction getInfoAction = new RequestInfoAction(data, (IcqContact) c);
        requestSimpleAction(getInfoAction);
        return data;
    }

    /** *********************************************************************** */

    protected void s_updateOnlineStatus() {
        byte bCode = getPrivateStatusByStatus();
        // #sijapp cond.if modules_SERVERLISTS isnot "true" #
        if (-1 == bCode) {
            bCode = Icq.PSTATUS_NOT_INVISIBLE;
            sendPrivateStatus(bCode);
        }
        // #sijapp cond.end #
        sendCaps();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        sendNewXStatus();
        // #sijapp cond.end #
        sendStatus();

        // #sijapp cond.if modules_SERVERLISTS isnot "true" #
        if (Icq.PSTATUS_NOT_INVISIBLE != bCode) {
            sendPrivateStatus(bCode);
        }
        // #sijapp cond.else #
        if (-1 != bCode) {
            setPrivateStatus((Icq.PSTATUS_NONE == bCode)
                    ? (byte)PrivateStatusForm.PSTATUS_NONE
                    : (byte)PrivateStatusForm.PSTATUS_VISIBLE_ONLY);
        }
        // #sijapp cond.end #
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    protected void s_updateXStatus() {
        sendCaps();
        sendNewXStatus();
    }
    // #sijapp cond.end #
    protected void s_setPrivateStatus() {
        sendPrivateStatus(getIcqPrivateStatus());
    }

    private byte getPrivateStatusByStatus() {
        switch (getProfile().statusIndex) {
            case StatusInfo.STATUS_INVIS_ALL: return Icq.PSTATUS_NONE;
            case StatusInfo.STATUS_INVISIBLE: return Icq.PSTATUS_VISIBLE_ONLY;
        }
        return -1;
    }
    public byte getIcqPrivateStatus() {
        byte p = getPrivateStatusByStatus();
        if (-1 != p) return p;
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        switch (getPrivateStatus()) {
            case PrivateStatusForm.PSTATUS_ALL:           return Icq.PSTATUS_ALL;
            case PrivateStatusForm.PSTATUS_VISIBLE_ONLY:  return Icq.PSTATUS_VISIBLE_ONLY;
            case PrivateStatusForm.PSTATUS_NOT_INVISIBLE: return Icq.PSTATUS_NOT_INVISIBLE;
            case PrivateStatusForm.PSTATUS_CL_ONLY:       return Icq.PSTATUS_CL_ONLY;
            case PrivateStatusForm.PSTATUS_NONE:          return Icq.PSTATUS_NONE;
        }
        // #sijapp cond.end #
        return Icq.PSTATUS_NOT_INVISIBLE;
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void updateClientMask() {
        if (isConnected()) {
            requestSimpleAction(getStatusPacket());
            sendCaps();
        }
    }
    // #sijapp cond.end #

    public int privateStatusId = 0;

    private int ssiListLastChangeTime = -1;
    private int ssiNumberOfItems = 0;
    /* Returns the id number #1 which identifies (together with id number #2)
       the saved contact list version */
    public int getSsiListLastChangeTime() {
        return ssiListLastChangeTime;
    }
    /* Returns the id number #2 which identifies (together with id number #1)
       the saved contact list version */
    public int getSsiNumberOfItems() {
        return ssiNumberOfItems;
    }
    public void setContactListInfo(int timestamp, int numberOfItems) {
        ssiListLastChangeTime = timestamp;
        ssiNumberOfItems = numberOfItems;
    }

    public Group createGroup(String name) {
        Group g = new Group(name);
        g.setMode(Group.MODE_FULL_ACCESS);
        g.setGroupId(createRandomId());
        return g;
    }

    protected Contact createContact(String uin, String name) {
        name = (null == name) ? uin : name;
        try {
            IcqContact c = new IcqContact(uin);
            c.init(-1, Group.NOT_IN_GROUP, name, false);
            return c;
        } catch (Exception e) {
            // Message from non-icq contact
            return null;
        }
    }

    private boolean isExistId(final int id) {
        if (id == privateStatusId) {
            return true;
        }
        for (int i = groups.size() - 1; i >= 0; --i) {
            Group group = (Group)groups.elementAt(i);
            if (group.getId() == id) {
                return true;
            }
        }
        for (int i = contacts.size() - 1; i >= 0; --i) {
            IcqContact item = (IcqContact)contacts.elementAt(i);
            if ((item.getContactId() == id)) {
                return true;
            }
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        if (isExistId(invisibleList, id) || isExistId(visibleList, id)
                || isExistId(ignoreList, id)) {
            return true;
        }
        // #sijapp cond.end #
        return false;
    }
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private boolean isExistId(Vector list, int id) {
        for (int i = list.size() - 1; 0 <= i; --i) {
            if (((PrivacyItem)list.elementAt(i)).id == id) {
                return true;
            }
        }
        return false;
    }
    // #sijapp cond.end #
    // Create a random id which is not used yet
    public int createRandomId() {
        int id;
        do {
            // Max value is probably 0x7FFF, lowest value is unknown.
            // We use range 0x1000-0x7FFF.
            // From miranda source
            id = Util.nextRandInt() % 0x6FFF + 0x1000;
        } while (isExistId(id));
        return id;
    }


   protected void loadProtocolData(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        ssiListLastChangeTime = dis.readInt();
        ssiNumberOfItems = dis.readUnsignedShort();
    }
    protected byte[] saveProtocolData() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(getSsiListLastChangeTime());
        dos.writeShort((short)getSsiNumberOfItems());
        return baos.toByteArray();
    }
    protected Contact loadContact(DataInputStream dis) throws Exception {
        int contactId = dis.readInt();
        int groupId = dis.readInt();
        byte flags = dis.readByte();
        String uin = dis.readUTF();
        String name = dis.readUTF();
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        int visibleId = dis.readInt();
        int invisibleId = dis.readInt();
        int ignoreId = dis.readInt();
        // #sijapp cond.end #

        IcqContact contact = (IcqContact) createContact(uin, name);
        contact.setContactId(contactId);
        contact.setGroupId(groupId);
        contact.setBooleanValues(flags);
        contact.setName(name);
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        setVisibleId(contact, visibleId);
        setInvisibleId(contact, invisibleId);
        setIgnoreId(contact, ignoreId);
        // #sijapp cond.end #
        contact.setOfflineStatus();
        return contact;
    }
    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        IcqContact icqContact = ((IcqContact)contact);
        out.writeByte(0);
        out.writeInt(icqContact.getContactId());
        out.writeInt(icqContact.getGroupId());
        out.writeByte(icqContact.getBooleanValues());
        out.writeUTF(icqContact.getUserId());
        out.writeUTF(icqContact.getName());
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        out.writeInt(getVisibleId(icqContact));
        out.writeInt(getInvisibleId(icqContact));
        out.writeInt(getIgnoreId(icqContact));
        // #sijapp cond.end #
    }
    public void getAvatar(UserInfo userInfo) {
        new jimm.ui.timers.GetVersion(userInfo).get();
    }



    protected void s_searchUsers(Search cont) {
        String userId = cont.getSearchParam(Search.UIN);
        if (null != userId) {
            UserInfo userInfo = new UserInfo(this);
            userInfo.uin = userId;
            cont.addResult(userInfo);
            cont.finished();
        } else {
            requestSimpleAction(new SearchAction(cont));
        }
    }
    protected void s_removeGroup(Group group) {
        requestSimpleAction(new UpdateContactListAction(group, UpdateContactListAction.ACTION_DEL));
    }
    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        IcqAction act = new UpdateContactListAction(group, UpdateContactListAction.ACTION_RENAME);
        requestSimpleAction(act);
    }
    /* Sets new contact name */
    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        /* Try to save ContactList to server */
        requestSimpleAction(new UpdateContactListAction(this, contact,
                UpdateContactListAction.ACTION_RENAME));
    }

    protected void s_moveContact(Contact contact, Group to) {
        requestSimpleAction(new UpdateContactListAction(contact, getGroup(contact), to));
    }
    public void saveUserInfo(UserInfo userInfo) {
        requestSimpleAction(makeSaveInfoPacket(userInfo));
    }
    private ToIcqSrvPacket makeSaveInfoPacket(UserInfo userInfo) {
        Util stream = new Util();

        /* 0x0C3A */
        stream.writeWordLE(ToIcqSrvPacket.CLI_SET_FULLINFO);

        stream.writeProfileAsciizTLV(NICK_TLV_ID, userInfo.nick);
        stream.writeProfileAsciizTLV(FIRSTNAME_TLV_ID, userInfo.firstName);
        stream.writeProfileAsciizTLV(LASTNAME_TLV_ID, userInfo.lastName);

        stream.writeProfileAsciizTLV(CITY_TLV_ID, userInfo.homeCity);
        stream.writeProfileAsciizTLV(STATE_TLV_ID, userInfo.homeState);

        stream.writeProfileAsciizTLV(HOME_PAGE_TLD, userInfo.homePage);
        stream.writeProfileAsciizTLV(WORK_COMPANY_TLV, userInfo.workCompany);
        stream.writeProfileAsciizTLV(WORK_DEPARTMENT_TLV, userInfo.workDepartment);
        stream.writeProfileAsciizTLV(WORK_POSITION_TLV, userInfo.workPosition);

        /* Birsday */
        if (userInfo.birthDay != null) {
            String[] bDate = Util.explode(userInfo.birthDay, '.');
            try {
                if (bDate.length == 3) {
                    int year  = Integer.parseInt(bDate[2]);
                    int month = Integer.parseInt(bDate[1]);
                    int day   = Integer.parseInt(bDate[0]);
                    stream.writeWordLE(BDAY_TLV_ID);
                    stream.writeWordLE(6);
                    stream.writeWordLE(year);
                    stream.writeWordLE(month);
                    stream.writeWordLE(day);
                }
            } catch (Exception e) {
            }
        }

        /* Gender */
        stream.writeWordLE(GENDER_TLV_ID);
        stream.writeWordLE(1);
        stream.writeByte(userInfo.gender);

        /* Email */
        stream.writeTlvECombo(EMAIL_TLV_ID, userInfo.email, 0);

        return new ToIcqSrvPacket(0, getUserId(),
                ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], stream.toByteArray());
    }
    //TLVs
    private static final int NICK_TLV_ID = 0x0154;
    private static final int FIRSTNAME_TLV_ID = 0x0140;
    private static final int LASTNAME_TLV_ID = 0x014A;
    private static final int EMAIL_TLV_ID = 0x015E;
    private static final int BDAY_TLV_ID = 0x023A;
    private static final int GENDER_TLV_ID = 0x017C;
    private static final int HOME_PAGE_TLD = 0x0213;
    private static final int CITY_TLV_ID = 0x0190;
    private static final int STATE_TLV_ID = 0x019A;
    private static final int WORK_COMPANY_TLV = 0x01AE;
    private static final int WORK_DEPARTMENT_TLV = 0x01B8;
    private static final int WORK_POSITION_TLV = 0x01C2;


    private static final byte[] statuses = {
        StatusInfo.STATUS_CHAT,
        StatusInfo.STATUS_ONLINE,
        StatusInfo.STATUS_AWAY,
        StatusInfo.STATUS_NA,
        StatusInfo.STATUS_OCCUPIED,
        StatusInfo.STATUS_DND,
        StatusInfo.STATUS_EVIL,
        StatusInfo.STATUS_DEPRESSION,
        StatusInfo.STATUS_LUNCH,
        StatusInfo.STATUS_HOME,
        StatusInfo.STATUS_WORK,
        StatusInfo.STATUS_INVISIBLE,
        StatusInfo.STATUS_INVIS_ALL};
    public byte[] getStatusList() {
        return statuses;
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        sendBeginTyping(to, isTyping);
    }

    /** ************************************************************************* */
    public SnacPacket getStatusPacket() {
        long status = 0x10000000 | IcqStatusInfo.getNativeStatus(getProfile().statusIndex);
        // Send a CLI_SETSTATUS packet
        byte[] data = new byte[4 + 4 + 4 + 0x25];//CLI_SETSTATUS_DATA;
        Util.putDWordBE(data, 0, 0x00060004);
        Util.putDWordBE(data, 4, status);
        Util.putDWordBE(data, 8, 0x000C0025);
        Util.putDWordBE(data, 23, 0xE36C96A9);

        Util.putWordBE (data, 21, 0x0009);
        Util.putDWordBE(data, 35, 0xFFFFFFFE);
        Util.putDWordBE(data, 39, 0x00100000);
        Util.putDWordBE(data, 43, 0xFFFFFFFE);
        return new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_SETSTATUS_COMMAND, data);
    }
    public SnacPacket getCapsPacket() {
        Vector guids = new Vector();

        guids.addElement(GUID.CAP_AIM_ISICQ);
        guids.addElement(GUID.CAP_AIM_SERVERRELAY);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        guids.addElement(GUID.CAP_XTRAZ);
        // #sijapp cond.end #
        guids.addElement(GUID.CAP_JIMM);
        guids.addElement(GUID.CAP_UTF8);

        // #sijapp cond.if modules_XSTATUSES is "true" #
        GUID x = Icq.xstatus.getIcqGuid(getProfile().xstatusIndex);
        if (null != x) {
            guids.addElement(x);
        }
        // #sijapp cond.end #

        if (Options.getInt(Options.OPTION_TYPING_MODE) > 0) {
            guids.addElement(GUID.CAP_MTN);
        }


        // Send a CLI_SETUSERINFO packet
        // Set version information to this packet in our capability
        byte extStatus = IcqStatusInfo.getExtStatus(getProfile().statusIndex);
        int extStatusCount = (0 == extStatus ? 0 : 1);
        int guidsCount = guids.size() + extStatusCount;
        byte[] packet = new byte[guidsCount * 16 + 4];
        packet[0] = 0x00;
        packet[1] = 0x05;
        packet[2] = (byte)((guidsCount * 16) / 0x0100);
        packet[3] = (byte)((guidsCount * 16) % 0x0100);

        if (0 != extStatus) {
            System.arraycopy(GUID.CAP_QIP_STATUS.toByteArray(), 0, packet, 4, 16);
            Util.putByte(packet, 4 + 15, extStatus); // status
        }

        for (int i = extStatusCount; i < guidsCount; ++i) {
            System.arraycopy(((GUID)guids.elementAt(i - extStatusCount)).toByteArray(), 0, packet, i * 16 + 4, 16);
        }

        return new SnacPacket(SnacPacket.LOCATION_FAMILY, SnacPacket.CLI_SETUSERINFO_COMMAND, packet);
    }
    public SnacPacket getPrivateStatusPacket(byte status) {
        int id = privateStatusId;
        int cmd = SnacPacket.CLI_ROSTERUPDATE_COMMAND;
        if (id == 0) {
            id = createRandomId();
            cmd = SnacPacket.CLI_ROSTERADD_COMMAND;
            privateStatusId = id;
        }
        Util stream = new Util();
        stream.writeWordBE(0);    // name (null)
        stream.writeWordBE(0);    // GroupID
        stream.writeWordBE(id);   // EntryID
        stream.writeWordBE(4);    // EntryType
        stream.writeWordBE(5);    // Length in bytes of following TLV
        stream.writeTLVByte(0xCA, status);
        return new SnacPacket(SnacPacket.SSI_FAMILY, cmd, stream.toByteArray());
    }

    private void sendCaps() {
        requestSimpleAction(getCapsPacket());
    }
    private void sendStatus() {
        long status = 0x10000000 | IcqStatusInfo.getNativeStatus(getProfile().statusIndex);
        Util data = new Util();
        data.writeTLVDWord(0x0006, status);

        SnacPacket p = new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_SETSTATUS_COMMAND, data.toByteArray());
        requestSimpleAction(p);
    }
    private void sendPrivateStatus(byte status) {
        requestSimpleAction(getPrivateStatusPacket(status));
    }

    private static final byte[] MTN_PACKET_BEGIN = {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x01
    };

    private void sendBeginTyping(Contact contact, boolean isTyping) {
        Util stream = new Util();
        stream.writeByteArray(MTN_PACKET_BEGIN);
        stream.writeShortLenAndUtf8String(contact.getUserId());
        stream.writeWordBE(isTyping ? 0x0002 : 0x0000);
        SnacPacket snacPkt = new SnacPacket(0x0004, 0x0014, stream.toByteArray());
        requestSimpleAction(snacPkt);
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private void addMoodToStatus(Util out, int xStatus, String msg) {
        int moodIndex = Icq.xstatus.getIcqMood(xStatus);
        String mood = (moodIndex < 0) ? "" : ("0icqmood" + moodIndex);
        msg = (msg.length() < 250) ? msg : (msg.substring(0, 250 - 3) + "...");
        /* xStatus */
        byte[] moodArr = StringConvertor.stringToByteArrayUtf8(mood);
        byte[] xMsgArr = StringConvertor.stringToByteArrayUtf8(msg);
        int xMsgLen = (0 == xMsgArr.length) ? 0 : (2 + xMsgArr.length + 2);
        int tlvLen = (2 + 1 + 1 + xMsgLen)
                + (2 + 1 + 1 + moodArr.length);

        // TLV (0x1D)
        out.writeWordBE(0x1D);
        out.writeWordBE(tlvLen);

        // subTLV 0x000E (mood)
        out.writeWordBE(0x000E);
        out.writeWordBE(moodArr.length);
        out.writeByteArray(moodArr);

        // subTLV 0x0002 (text)
        out.writeWordBE(0x0002);
        out.writeByte(0x04);
        out.writeByte(xMsgLen);
        if (0 < xMsgLen) {
            out.writeWordBE(xMsgArr.length);
            out.writeByteArray(xMsgArr);
            out.writeWordBE(0x0000);
        }
    }
    public SnacPacket getNewXStatusPacket(int xStatus, String msg) {
        Util mood = new Util();
        addMoodToStatus(mood, xStatus, msg.trim());
        return new SnacPacket(SnacPacket.SERVICE_FAMILY,
                SnacPacket.CLI_SETSTATUS_COMMAND, mood.toByteArray());
    }
    private void sendNewXStatus() {
        int index = getProfile().xstatusIndex;
        String title = getProfile().xstatusTitle;
        String desc  = getProfile().xstatusDescription;
        title = StringConvertor.notNull(title);
        desc = StringConvertor.notNull(desc);
        String text = (title + " " + desc).trim();
        requestSimpleAction(getNewXStatusPacket(index, text));
    }
    // #sijapp cond.end#

    void sendRemoveMePacket(String uin) {
        byte[] uinRaw = StringConvertor.stringToByteArray(uin);

        byte[] buf = new byte[1 + uinRaw.length];

        // Assemble the packet
        Util.putByte(buf, 0, uinRaw.length);
        System.arraycopy(uinRaw, 0, buf, 1, uinRaw.length);
        SnacPacket pkt = new SnacPacket(SnacPacket.SSI_FAMILY,
                SnacPacket.CLI_REMOVEME_COMMAND, 0x00000003, buf);
        requestSimpleAction(pkt);
    }


    /** ************************************************************************* */
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private static final int VISIBLE_LIST = 0x0002;
    private static final int INVISIBLE_LIST = 0x0003;
    private static final int IGNORE_LIST = 0x000E;
    private static final int ADD_INTO_LIST = 0;
    private static final int REMOVE_FROM_LIST = 1;
    private void changeServerList(int list, IcqContact item) {
        int id = 0;
        switch (list) {
            case VISIBLE_LIST:
                id = getVisibleId(item);
                break;

            case INVISIBLE_LIST:
                id = getInvisibleId(item);
                break;

            case IGNORE_LIST:
                id = getIgnoreId(item);
                break;
        }

        int subaction;
        if (id == 0) {
            id = createRandomId();
            subaction = ADD_INTO_LIST;
        } else {
            subaction = REMOVE_FROM_LIST;
        }

        Util stream = new Util();

        stream.writeLenAndUtf8String(item.getUserId());
        stream.writeWordBE(0);
        stream.writeWordBE(id);
        stream.writeWordBE(list);
        stream.writeWordLE(0);

        SnacPacket packet = null;
        switch (subaction) {
            case ADD_INTO_LIST:
                packet = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_ROSTERADD_COMMAND, stream.toByteArray());
                break;

            case REMOVE_FROM_LIST:
                packet = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_ROSTERDELETE_COMMAND, stream.toByteArray());
                id = 0;
                break;
        }
        requestSimpleAction(packet);

        switch (list) {
            case VISIBLE_LIST:
                setVisibleId(item, id);
                break;

            case INVISIBLE_LIST:
                setInvisibleId(item, id);
                break;

            case IGNORE_LIST:
                setIgnoreId(item, id);
                break;
        }
        ui_updateContact(item);
    }
    public void setPrivacyLists(Vector ignore, Vector invisible, Vector visible) {
        ignoreList = ignore;
        invisibleList = invisible;
        visibleList = visible;
    }
    private void setIgnoreId(IcqContact c, int id) {
        setPrivacy(ignoreList, c, Contact.SL_IGNORE, id);
    }

    private void setVisibleId(IcqContact c, int id) {
        setPrivacy(visibleList, c, Contact.SL_VISIBLE, id);
    }

    private void setInvisibleId(IcqContact c, int id) {
        setPrivacy(invisibleList, c, Contact.SL_INVISIBLE, id);
    }
    private void setPrivacy(Vector list, Contact c, byte l, int id) {
        PrivacyItem item = getListItem(list, c);
        if (0 == id) {
            invisibleList.removeElement(item);
        } else if (null == item) {
            item = new PrivacyItem(c.getUserId(), id);
            list.addElement(item);
        } else {
            item.id = id;
        }
        c.setBooleanValue(l, id != 0);
    }
    private PrivacyItem getListItem(Vector list, Contact c) {
        String uin = c.getUserId();
        PrivacyItem item;
        for (int i = list.size() - 1; 0 <= i; --i) {
            item = (PrivacyItem)list.elementAt(i);
            if (uin.equals(item.userId)) {
                item.userId = uin;
                return item;
            }
        }
        return null;
    }
    public int getIgnoreId(Contact c) {
        PrivacyItem item = getListItem(ignoreList, c);
        return (null == item) ? 0 : item.id;
    }
    public int getVisibleId(Contact c) {
        PrivacyItem item = getListItem(visibleList, c);
        return (null == item) ? 0 : item.id;
    }
    public int getInvisibleId(Contact c) {
        PrivacyItem item = getListItem(invisibleList, c);
        return (null == item) ? 0 : item.id;
    }
    // #sijapp cond.end #
    /** ************************************************************************* */
    private void sendAuthResult(String userId, boolean confirm) {
        Util stream = new Util();
        stream.writeShortLenAndUtf8String(userId);
        stream.writeByte(confirm ? 0x01 : 0x00);
        stream.writeLenAndUtf8String("" /* reason */);
        // Send a CLI_AUTHORIZE packet
        requestSimpleAction(new SnacPacket(SnacPacket.SSI_FAMILY,
                SnacPacket.CLI_AUTHORIZE_COMMAND, 0x0000001A,
                stream.toByteArray()));
    }
    protected void requestAuth(String userId) {
        final String reason = "";
        Util stream = new Util();
        stream.writeShortLenAndUtf8String(userId);
        stream.writeLenAndUtf8String(reason);
        stream.writeWordBE(0x0000);
        // Send a CLI_REQUAUTH packet
        requestSimpleAction(new SnacPacket(SnacPacket.SSI_FAMILY,
                SnacPacket.CLI_REQAUTH_COMMAND, 0x00000018,
                stream.toByteArray()));
    }

    protected void grandAuth(String userId) {
        sendAuthResult(userId, true);
    }

    protected void denyAuth(String userId) {
        sendAuthResult(userId, false);
    }

    public void showUserInfo(Contact contact) {
        final UserInfo data;
        if (isConnected()) {
            data = getUserInfo(contact);
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this);
            data.nick = contact.getName();
            data.uin = contact.getUserId();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile();
    }
    public void showStatus(Contact contact) {
        StatusView statusView = ContactList.getInstance().getStatusView();

        ContactList.getInstance().setCurrentContact(contact);
        _updateStatusView(statusView, contact);
	statusView.showIt();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                && (null == contact.getXStatusText())
                && isMeVisible(contact)) {
            requestXStatusMessage(contact);
        }
        // #sijapp cond.end #
    }
    public void updateStatusView(Contact contact) {
        StatusView statusView = ContactList.getInstance().getStatusView();
        if (contact == statusView.getContact()) {
            _updateStatusView(statusView, contact);
        }
    }
    private void _updateStatusView(StatusView statusView, Contact contact) {
        statusView.init(this, contact);
        statusView.initUI();
        statusView.addContactStatus();

        if (contact.isOnline()) {
            Icon happy = ((IcqContact)contact).getHappyIcon();
            if (null != happy) {
                statusView.addPlain(happy,
                        JLocale.getString("status_happy_flag"));
            }
            String statusText = contact.getStatusText();
            if (!StringConvertor.isEmpty(statusText)) {
                statusView.addStatusText(statusText);
            }

            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
                statusView.addXStatus();
                String xText = contact.getXStatusText();
                if (!StringConvertor.isEmpty(xText)) {
                    statusView.addStatusText(xText);
                }
            }
            // #sijapp cond.end #
        }

        // #sijapp cond.if modules_CLIENTS is "true" #
        statusView.addClient();
        // #sijapp cond.end #
        statusView.addTime();
        statusView.update();
    }
}
// #sijapp cond.end #
