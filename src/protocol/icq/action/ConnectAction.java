/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-05  Jimm Project

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
File: src/jimm/comm/ConnectAction.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;

import java.util.*;

import jimm.*;
import jimm.comm.*;
import protocol.*;
import protocol.icq.*;
import protocol.icq.packet.*;
import jimm.modules.*;
import jimm.ui.timers.*;

public class ConnectAction extends IcqAction {
    // Action states

    public static final int STATE_ERROR = -1;
    public static final int STATE_INIT = 0;
    public static final int STATE_INIT_DONE = 1;
    public static final int STATE_AUTHKEY_REQUESTED = 2;
    public static final int STATE_CLI_IDENT_SENT = 3;
    public static final int STATE_CLI_DISCONNECT_SENT = 4;
    public static final int STATE_CLI_COOKIE_SENT = 5;
    public static final int STATE_CLI_WANT_CAPS_SENT = 6;
    public static final int STATE_CLI_WANT_CAPS_SENT2 = 7;
    public static final int STATE_CLI_CHECKROSTER_SENT = 8;
    public static final int STATE_CLI_STATUS_INFO_SENT = 9;
    public static final int STATE_CLI_REQOFFLINEMSGS_SENT = 10;
    public static final int STATE_CLI_ACKOFFLINEMSGS_SENT = 11;
    private static final short[] FAMILIES_AND_VER_LIST = {
        0x0022, 0x0001,
        0x0001, 0x0004,
        0x0013, 0x0004,
        0x0002, 0x0001,
        0x0025, 0x0001,
        0x0003, 0x0001,
        0x0015, 0x0001,
        0x0004, 0x0001,
        0x0006, 0x0001,
        0x0009, 0x0001,
        0x000a, 0x0001,
        0x000b, 0x0001};
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private Vector ignoreList = new Vector();
    private Vector invisibleList = new Vector();
    private Vector visibleList = new Vector();
    // #sijapp cond.end #
    private int timestamp;
    private int count;
    private TemporaryRoster roster;
    // CLI_READY packet data
    private static final byte[] CLI_READY_DATA = {
        /*
        (byte)0x00, (byte)0x22, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x13, (byte)0x00, (byte)0x04, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x15, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f,
        (byte)0x00, (byte)0x0b, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x10, (byte)0x16, (byte)0x4f
         */
        (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x25, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x15, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
        (byte) 0x00, (byte) 0x0b, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2
    };
    // Timeout
    private static final int TIMEOUT = 20; // seconds
    private static final byte[] AIM_MD5_STRING = new byte[]{'A', 'O', 'L', ' ', 'I', 'n', 's', 't', 'a', 'n', 't', ' ',
        'M', 'e', 's', 's', 'e', 'n', 'g', 'e', 'r', ' ', '(', 'S', 'M', ')'};
    /** *********************************************************************** */
    // UIN
    private String uin;
    // Password
    private String password;
    // Action state
    private int state;
    private volatile boolean active;
    private String server;
    private byte[] cookie;

    private String getServerHostAndPort() {
        return "login.icq.com:5190";
    }
    // Constructor
    private boolean md5login;

    public ConnectAction(Icq icq) {
        super();
        this.uin = icq.getUserId();
        this.password = icq.getPassword();
        this.server = getServerHostAndPort();
        active();
        if (8 < password.length()) {
            this.password = password.substring(0, 8);
        }
        md5login = true && (-1 == uin.indexOf('@'));
    }

    private void setProgress(int progress) {
        getIcq().setConnectingProgress(progress);
    }

    public void init() throws JimmException {
        this.active = true;
        this.state = ConnectAction.STATE_INIT;
        active();

        // Open connection
        getConnection().connectTo(server);

        state = ConnectAction.STATE_INIT_DONE;

        this.active = false;
        // Init activity timestamp
        active();
    }

    // Forwards received packet, returns true if packet has been consumed
    public boolean forward(Packet packet) throws JimmException {
        if (null == getIcq()) {
            return false;
        }

        // Set activity flag
        this.active = true;
        active();
        // Catch JimmExceptions
        try {

            // Flag indicates whether packet has been consumed or not
            boolean consumed = false;

            // Watch out for STATE_INIT_DONE
            if (ConnectAction.STATE_INIT_DONE == this.state) {
                // Watch out for SRV_CLI_HELLO packet
                if (packet instanceof ConnectPacket) {
                    ConnectPacket connectPacket = (ConnectPacket) packet;
                    if (ConnectPacket.SRV_CLI_HELLO == connectPacket.getType()) {
                        if (md5login) {
                            sendPacket(new ConnectPacket());
                            Util stream = new Util();
                            stream.writeWordBE(0x0001);
                            stream.writeLenAndUtf8String(uin);
                            stream.writeTLV(0x4b, new byte[0]);
                            sendPacket(new SnacPacket(0x0017, 0x0006, -1, stream.toByteArray()));
                            state = STATE_AUTHKEY_REQUESTED;
                        } else {
                            // Send a CLI_IDENT packet as reply
                            sendPacket(new ConnectPacket(uin, password));
                            state = ConnectAction.STATE_CLI_IDENT_SENT;
                        }
                        // Packet has been consumed
                        consumed = true;
                    }
                }

            } else if (STATE_AUTHKEY_REQUESTED == state) {
                if (packet instanceof SnacPacket) {
                    SnacPacket snacPacket = (SnacPacket) packet;
                    if ((0x0017 == snacPacket.getFamily())
                            && (0x0007 == snacPacket.getCommand())) {
                        Util stream = new Util();
                        stream.writeTLV(0x0001, uin.getBytes());

                        ArrayReader rbuf = snacPacket.getReader();
                        byte[] authkey = rbuf.getArray(rbuf.getWordBE());

                        byte[] passwordRaw = StringConvertor.stringToByteArray(password);

                        byte[] passkey = new MD5().calculate(passwordRaw);

                        // Old style
                        //passkey = passwordRaw;

                        byte[] md5buf = new byte[authkey.length + passkey.length + AIM_MD5_STRING.length];
                        int md5marker = 0;
                        System.arraycopy(authkey, 0, md5buf, md5marker, authkey.length);
                        md5marker += authkey.length;
                        System.arraycopy(passkey, 0, md5buf, md5marker, passkey.length);
                        md5marker += passkey.length;
                        System.arraycopy(AIM_MD5_STRING, 0, md5buf, md5marker, AIM_MD5_STRING.length);

                        stream.writeTLV(0x0025, new MD5().calculate(md5buf));

                        ConnectPacket.putVersion(stream, true);

                        sendPacket(new SnacPacket(0x0017, 0x0002, -1, stream.toByteArray()));
                        state = STATE_CLI_IDENT_SENT;
                    } else {
                        // #sijapp cond.if modules_DEBUGLOG is "true"#
                        DebugLog.println("connect: family = 0x" + Integer.toHexString(snacPacket.getFamily())
                                + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));
                        // #sijapp cond.end#
                        throw new JimmException(100, 0);
                    }
                }
                consumed = true;

                // Watch out for STATE_CLI_IDENT_SENT
            } else if (STATE_CLI_IDENT_SENT == state) {
                int errcode = -1;
                if (md5login) {
                    if (packet instanceof SnacPacket) {
                        SnacPacket snacPacket = (SnacPacket) packet;
                        if ((0x0017 == snacPacket.getFamily())
                                && (0x0003 == snacPacket.getCommand())) {
                            ArrayReader marker = snacPacket.getReader();
                            while (marker.isNotEnd()) {
                                int tlvType = marker.getTlvType();
                                byte[] tlvData = marker.getTlv();
                                switch (tlvType) {
                                    case 0x0001:
                                        getIcq().setRealUin(StringConvertor.byteArrayToAsciiString(tlvData));
                                        this.uin = getIcq().getUserId();
                                        break;
                                    case 0x0008:
                                        errcode = Util.getWordBE(tlvData, 0);
                                        break;
                                    case 0x0005:
                                        this.server = StringConvertor.byteArrayToAsciiString(tlvData);
                                        break;
                                    case 0x0006:
                                        this.cookie = tlvData;
                                        break;
                                }
                            }
                        }
                    } else if (packet instanceof DisconnectPacket) {
                        consumed = true;
                    }
                } else {
                    // watch out for channel 4 packet
                    if (packet instanceof DisconnectPacket) {
                        DisconnectPacket disconnectPacket = (DisconnectPacket) packet;
                        // Watch out for SRV_COOKIE packet
                        if (DisconnectPacket.TYPE_SRV_COOKIE == disconnectPacket.getType()) {
                            // Save cookie
                            getIcq().setRealUin(disconnectPacket.getUin());
                            this.uin = getIcq().getUserId();
                            this.cookie = disconnectPacket.getCookie();
                            this.server = disconnectPacket.getServer();

                            // Watch out for SRV_GOODBYE packet
                        } else if (DisconnectPacket.TYPE_SRV_GOODBYE == disconnectPacket.getType()) {
                            errcode = disconnectPacket.getError();
                        }
                        consumed = true;
                    }
                }

                if (-1 != errcode) {
                    int toThrow = 100;
                    switch (errcode) {
                        // Multiple logins
                        case 0x0001:
                            toThrow = 110;
                            break;
                        // Bad password
                        case 0x0004:
                        case 0x0005:
                            toThrow = 111;
                            break;
                        // Non-existant UIN
                        case 0x0007:
                        case 0x0008:
                            toThrow = 112;
                            break;
                        // Too many clients from same IP
                        case 0x0015:
                        case 0x0016:
                            toThrow = 113;
                            break;
                        // Rate exceeded
                        case 0x0018:
                        case 0x001d:
                            toThrow = 114;
                            break;
                    }
                    if (111 == toThrow) {
                        getIcq().setPassword(null);
                    }
                    throw new JimmException(toThrow, errcode);
                }

                if (consumed & (null != this.server) & (null != this.cookie)) {
                    // Open connection
                    getConnection().connectTo(server);
                    // Move to next state
                    this.state = ConnectAction.STATE_CLI_DISCONNECT_SENT;
                }

                // Watch out for STATE_CLI_DISCONNECT_SENT
            } else if (STATE_CLI_DISCONNECT_SENT == state) {

                // Watch out for SRV_HELLO packet
                if (packet instanceof ConnectPacket) {
                    ConnectPacket connectPacket = (ConnectPacket) packet;
                    if (connectPacket.getType() == ConnectPacket.SRV_CLI_HELLO) {

                        // #sijapp cond.if modules_DEBUGLOG is "true" #
                        DebugLog.println("connect say 'hello'");
                        // #sijapp cond.end #
                        // Send a CLI_COOKIE packet as reply
                        ConnectPacket reply = new ConnectPacket(this.cookie);
                        sendPacket(reply);


                        // Move to next state
                        this.state = ConnectAction.STATE_CLI_COOKIE_SENT;
//                        this.state = ConnectAction.STATE_CLI_WANT_CAPS_SENT;

                        // Packet has been consumed
                        consumed = true;

                    }
                }

                // Watch out for STATE_CLI_COOKIE_SENT
            } else if (STATE_CLI_COOKIE_SENT == state) {
                Util stream = new Util();

                for (int i = 0; i < FAMILIES_AND_VER_LIST.length; ++i) {
                    stream.writeWordBE(FAMILIES_AND_VER_LIST[i]);
                }

                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_FAMILIES_COMMAND, SnacPacket.CLI_FAMILIES_COMMAND, stream.toByteArray()));
                this.state = ConnectAction.STATE_CLI_WANT_CAPS_SENT;

            } else if (STATE_CLI_WANT_CAPS_SENT == state) {

                if (packet instanceof SnacPacket) {
                    SnacPacket s = (SnacPacket) packet;
                    if ((SnacPacket.SERVICE_FAMILY == s.getFamily())
                            && (SnacPacket.SRV_MOTD_COMMAND == s.getCommand())) {
                        sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY,
                                SnacPacket.CLI_RATESREQUEST_COMMAND));
                        this.state = ConnectAction.STATE_CLI_WANT_CAPS_SENT2;
                    }
                }

            } else if (STATE_CLI_WANT_CAPS_SENT2 == state) {

                Util udata = null;
                udata = new Util();
                udata.writeWordBE(0x0001);
                udata.writeWordBE(0x0002);
                udata.writeWordBE(0x0003);
                udata.writeWordBE(0x0004);
                udata.writeWordBE(0x0005);
                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_ACKRATES_COMMAND, 0x6F4E0000, udata.toByteArray()));

                // Request own status
                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_REQINFO_COMMAND, 0x2BDD0000));

                udata = new Util();
                udata.writeTLVWord(0x000B, 0x00FF);
                sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_REQLISTS_COMMAND, SnacPacket.CLI_REQLISTS_COMMAND, udata.toByteArray()));

                SnacPacket reply2;
                timestamp = getIcq().getSsiListLastChangeTime();
                count = getIcq().getSsiNumberOfItems();
                if ((-1 == timestamp) || (0 == count) || (0 == getIcq().getContactItems().size())) {
                    reply2 = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_REQROSTER_COMMAND, 0x07630000);
                } else {
                    Util data = new Util();
                    data.writeDWordBE(timestamp);
                    data.writeWordBE(count);
                    reply2 = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_CHECKROSTER_COMMAND, data.toByteArray());
                }
                sendPacket(reply2);

                roster = new TemporaryRoster(getIcq());
                getIcq().setContactListInfo(-1, 0);
                getIcq().setContactListStub();

                // Request limits
                //sendPacket(new SnacPacket(SnacPacket.LOCATION_FAMILY, SnacPacket.CLI_REQLOCATION_COMMAND, SnacPacket.CLI_REQLOCATION_COMMAND));

                // Request roster limits
                //udata = new Util();
                //udata.writeTLVWord(0x0005, 0x000B);
                //sendPacket(new SnacPacket(SnacPacket.CONTACT_FAMILY, SnacPacket.CLI_REQBUDDY_COMMAND, SnacPacket.CLI_REQBUDDY_COMMAND, udata.toByteArray()));

                // request ICBM
                //sendPacket(new SnacPacket(SnacPacket.CLI_ICBM_FAMILY, SnacPacket.CLI_REQICBM_COMMAND, SnacPacket.CLI_REQICBM_COMMAND));

                // Request private lists limits
                //sendPacket(new SnacPacket(SnacPacket.BOS_FAMILY, SnacPacket.CLI_REQBOS_COMMAND, SnacPacket.CLI_REQBOS_COMMAND));

                // Move to next state
                this.state = ConnectAction.STATE_CLI_CHECKROSTER_SENT;

                // Watch out for STATE_CLI_CHECKROSTER_SENT
            } else if (STATE_CLI_CHECKROSTER_SENT == state) {
                if (packet instanceof SnacPacket) {
                    consumed = loadContactList((SnacPacket) packet);
                }

            } else if (STATE_CLI_STATUS_INFO_SENT == state) {
                // Send a CLI_TOICQSRV/CLI_REQOFFLINEMSGS packet
                sendPacket(new ToIcqSrvPacket(0x00000000, this.uin, ToIcqSrvPacket.CLI_REQOFFLINEMSGS_SUBCMD, new byte[0], new byte[0]));

                getConnection().initPing();
                getConnection().setIcqConnected();
                // Move to next state
                this.state = ConnectAction.STATE_CLI_REQOFFLINEMSGS_SENT;
            }

            // Update activity timestamp and reset activity flag
            active();
            this.active = false;
            updateProgress();
            // Return consumption flag
            return consumed;

            // Catch JimmExceptions
        } catch (JimmException e) {

            // Update activity timestamp and reset activity flag
            this.active = false;

            // Set error state if exception is critical
            this.state = ConnectAction.STATE_ERROR;

            // Forward exception
            throw e;
        }
    }

    private void requestOtherContacts() throws JimmException {
        Util stream = new Util();
        stream.writeTLVByte(0x08, 1);
        sendPacket(new SnacPacket(SnacPacket.CONTACT_FAMILY, SnacPacket.CLI_REQBUDDY_COMMAND, stream.toByteArray()));
    }

    private void processOtherContacts(SnacPacket snacPacket) throws JimmException {
    }

    private void sendStatusData() throws JimmException {
        byte pstatus = getIcq().getIcqPrivateStatus();
        sendPacket(getIcq().getPrivateStatusPacket(pstatus));
        // #sijapp cond.if modules_XSTATUSES is "true" #
        int x = getIcq().getProfile().xstatusIndex;
        String title = getIcq().getProfile().xstatusTitle;
        String desc = getIcq().getProfile().xstatusDescription;
        //sendPacket(OtherAction.getNew2XStatus(getIcq(), title, desc));
        title = StringConvertor.notNull(title);
        desc = StringConvertor.notNull(desc);
        String text = (title + " " + desc).trim();
        sendPacket(getIcq().getNewXStatusPacket(x, text));
        // #sijapp cond.end#
        sendPacket(getIcq().getStatusPacket());
        // Send to server sequence of unuthoruzed oldContacts to see their statuses
        ////////////////////////////////////
        // Set version information to this packet in our capability
        sendPacket(getIcq().getCapsPacket());
        sendPacket(getIcbmPacket());
    }

    private boolean loadContactList(SnacPacket snacPacket) throws JimmException {
        boolean consumed = false;
        if (SnacPacket.SSI_FAMILY != snacPacket.getFamily()) {
            return false;
        }
        boolean srvReplyRosterRcvd = false;
        boolean newRosterLoaded = false;
        // Watch out for
        // SRV_REPLYROSTEROK
        if (SnacPacket.SRV_REPLYROSTEROK_COMMAND == snacPacket.getCommand()) {
            srvReplyRosterRcvd = true;
            newRosterLoaded = false;
            // Packet has been consumed
            consumed = true;
            roster.useOld();

            // Update contact list
            getIcq().setContactList(roster.getGroups(), roster.mergeContacts());
            getIcq().setContactListInfo(timestamp, count);

            // watch out for SRV_REPLYROSTER
            // packet
        } else if (SnacPacket.SRV_REPLYROSTER_COMMAND == snacPacket.getCommand()) {
            if (1 != snacPacket.getFlags()) {
                srvReplyRosterRcvd = true;
                newRosterLoaded = true;
            }

            // Get data
            ArrayReader marker = snacPacket.getReader();

            // SRV_REPLYROSTER.UNKNOWN
            marker.skip(1);

            // Iterate through all
            // items
            count = marker.getWordBE();

            for (int i = 0; i < count; ++i) {
                // Get userId length
                int nameLen = marker.getWordBE();
                // Get userId
                String userId = StringConvertor.utf8beByteArrayToString(
                        marker.getArray(nameLen), 0, nameLen);

                // Get groupId, id and type
                int groupId = marker.getWordBE();
                int id = marker.getWordBE();
                int type = marker.getWordBE();

                // Get length of the following TLVs
                int len = marker.getWordBE();

                // Normal contact
                if (0x0000 == type) {
                    // Get nick
                    String nick = userId;

                    boolean noAuth = false;
                    int end = marker.getOffset() + len;
                    while (marker.getOffset() < end) {
                        int tlvType = marker.getTlvType();
                        if (0x0131 == tlvType) {
                            byte[] tlvData = marker.getTlv();
                            nick = StringConvertor.utf8beByteArrayToString(tlvData, 0, tlvData.length);
                        } else {
                            if (0x0066 == tlvType) {
                                noAuth = true;
                            }
                            marker.skipTlv();
                        }

                        //else if (tlvType == 0x006D) /* Server-side additional data */
                        //{
                        //    Util.writeWord(serverData, tlvType, true);
                        //    Util.writeWord(serverData, tlvData.length, true);
                        //    Util.writeByteArray(serverData, tlvData);
                        //
                        //    Util.showBytes(serverData.toByteArray());
                        //}
                    }
                    // Add this contact item to the vector
                    try {
                        //DebugLog.println("c " + userId + " " + nick);
                        //// only icq-contact (ignore aim contacts)
                        //Integer.parseInt(uin);
                        IcqContact item = (IcqContact) roster.makeContact(userId);
                        if (nick.equals(userId) && !StringConvertor.isEmpty(item.getName())) {
                            nick = item.getName();
                        }

                        item.init(id, groupId, nick, noAuth);
                        roster.addContact(item);
                    } catch (Exception e) {
                        // Contact with wrong uin was received
                    }

                    // Group of oldContacts
                } else if (0x0001 == type) {
                    marker.skip(len);
                    // Add this groupId item to the vector
                    if (0x0000 != groupId) {
                        Group grp = roster.getGroupById(groupId);
                        if (null == grp) {
                            grp = roster.makeGroup(userId);
                            grp.setGroupId(groupId);
                        } else {
                            grp.setName(userId);
                        }
                        if ("Not In List".equals(userId)) {
                            grp.setMode(Group.MODE_REMOVABLE | Group.MODE_BOTTOM);
                        } else {
                            grp.setMode(Group.MODE_FULL_ACCESS);
                        }
                        roster.addGroup(grp);
                    }

                    // #sijapp cond.if modules_SERVERLISTS is "true" #
                    // Permit record ("Allow" list in AIM, and "Visible" list in ICQ)
                } else if (0x0002 == type) {
                    marker.skip(len);
                    visibleList.addElement(new PrivacyItem(userId, id));

                    // Deny record ("Block" list in AIM, and "Invisible" list in ICQ)
                } else if (0x0003 == type) {
                    marker.skip(len);
                    invisibleList.addElement(new PrivacyItem(userId, id));

                    // Ignore list record.
                } else if (0x000E == type) {
                    marker.skip(len);
                    ignoreList.addElement(new PrivacyItem(userId, id));
                    // #sijapp cond.end #

                    // Permit/deny settings or/and bitmask of the AIM classes
                } else if (0x0004 == type) {
                    int end = marker.getOffset() + len;
                    while (marker.getOffset() < end) {
                        int tlvType = marker.getTlvType();
                        marker.skipTlv();

                        if (0x00CA == tlvType) {
                            getIcq().privateStatusId = id;
                        }
                    }

                    // All other item types
                } else {
                    // Skip TLVs
                    marker.skip(len);
                }
            }

            // Get timestamp
            timestamp = (int) marker.getDWordBE();

            // Packet has been consumed
            consumed = true;

        }

        // Check if all required packets have been received
        if (newRosterLoaded) {
            Vector contactItems = roster.mergeContacts();
            active();

            // #sijapp cond.if modules_SERVERLISTS is "true" #
            for (int i = 0; i < contactItems.size(); ++i) {
                IcqContact contact = (IcqContact) contactItems.elementAt(i);
                String userId = contact.getUserId();
                contact.setBooleanValue(Contact.SL_IGNORE, inList(ignoreList, userId));
                contact.setBooleanValue(Contact.SL_VISIBLE, inList(visibleList, userId));
                contact.setBooleanValue(Contact.SL_INVISIBLE, inList(invisibleList, userId));
            }
            getIcq().setPrivacyLists(ignoreList, invisibleList, visibleList);
            // #sijapp cond.end #

            // Update contact list
            getIcq().setContactList(roster.getGroups(), contactItems);
            getIcq().setContactListInfo(timestamp, count);
        }
        if (srvReplyRosterRcvd) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.memoryUsage("Connect memory usage");
            // #sijapp cond.end #

            sendStatusData();
            // Send a CLI_ROSTERACK packet
            sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_ROSTERACK_COMMAND));
            // Send a CLI_READY packet
            sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_READY_COMMAND, ConnectAction.CLI_READY_DATA));
            //// avatars
            //sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, 0x04, 0x6B1B0000, new byte[]{(byte)0x00, (byte)0x18}));

            // Move to next state
            this.state = ConnectAction.STATE_CLI_STATUS_INFO_SENT;
        }
        return consumed;
    }

    private SnacPacket getIcbmPacket() {
        // Calculate client flags
        // 0x0008 - typing notification
        // 0x1000 - turn off status message
        long flags = 0x0003;
        if (0 != Options.getInt(Options.OPTION_TYPING_MODE)) {
            flags |= 0x0B;
        }
        // ICQ6
        // flags = 0x3FDB

        Util icbm = new Util();
        icbm.writeWordBE(0x0000);
        icbm.writeDWordBE(flags); // message flags
        icbm.writeWordBE(8000); // Maximum message size
        icbm.writeWordBE(0x03E7);
        icbm.writeWordBE(0x03E7);
        icbm.writeWordBE(0x0000); // Minimum message interval
        icbm.writeWordBE(0x0000);

        return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY, SnacPacket.CLI_SETICBM_COMMAND, icbm.toByteArray());
    }

    private boolean inList(Vector list, String uin) {
        PrivacyItem item;
        for (int i = list.size() - 1; 0 <= i; --i) {
            item = (PrivacyItem) list.elementAt(i);
            if (uin.equals(item.userId)) {
                item.userId = uin;
                return true;
            }
        }
        return false;
    }

    // Returns true if the action is completed
    public boolean isCompleted() {
        return (null == getIcq()) || getIcq().isConnected();
    }

    // Returns true if an error has occured
    public boolean isError() {
        if (isCompleted()) {
            return false;
        }
        if (state == ConnectAction.STATE_ERROR) {
            return true;
        }
        if (!active && isNotActive(TIMEOUT)) {
            state = ConnectAction.STATE_ERROR;
            getConnection().processIcqException(new JimmException(118, 0));
        }
        return state == ConnectAction.STATE_ERROR;
    }

    private void updateProgress() {
        setProgress(getProgress());
    }

    public void initProgressBar() {
        setProgress(0);
    }
    // Returns a number between 0 and 100 (inclusive) which indicates the current progress

    private int getProgress() {
        switch (this.state) {
            case STATE_INIT:
                return 1;
            case STATE_INIT_DONE:
                return 12;
            case STATE_AUTHKEY_REQUESTED:
                return 25;
            case STATE_CLI_IDENT_SENT:
                return 37;
            case STATE_CLI_DISCONNECT_SENT:
                return 50;
            case STATE_CLI_COOKIE_SENT:
                return 62;
            case STATE_CLI_WANT_CAPS_SENT:
                return 68;
            case STATE_CLI_WANT_CAPS_SENT2:
                return 69;
            case STATE_CLI_CHECKROSTER_SENT:
                return 75;
            case STATE_CLI_STATUS_INFO_SENT:
                return 90;
            case STATE_CLI_REQOFFLINEMSGS_SENT:
                return 100;
        }
        return 2;
    }
}
// #sijapp cond.end #
