/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-06  Jimm Project

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
 File: src/jimm/comm/ActionListener.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Spassky Alexander, Igor Palkin
 *******************************************************************************/

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq;

import jimm.*;
import jimm.chat.message.*;
import jimm.comm.*;
import protocol.*;
import protocol.icq.packet.*;
import protocol.icq.plugin.*;
import jimm.modules.*;
import jimm.util.JLocale;

public final class IcqNetDefActions {
    public static final int FLAG_HAPPY    = 0x0008;
    public static final int FLAG_WEBAWARE = 0x0001;

    private IcqNetWorking connection;
    public IcqNetDefActions(IcqNetWorking net) {
        connection = net;
    }
    private Icq getIcq() {
        return connection.getIcq();
    }

    private void sendPacket(Packet packet) throws JimmException {
        connection.sendPacket(packet);
    }

    private void updateMessageStatus(SnacPacket packet) throws JimmException {
        long msgId = packet.getReader().getDWordBE() & 0xFFFFFFFFL;
        boolean toClient = (packet.getCommand() != 0x000C);
        int notifyType = toClient ? PlainMessage.NOTIFY_FROM_CLIENT : PlainMessage.NOTIFY_FROM_SERVER;
        connection.markMessageSended(msgId, notifyType);
    }
    /** ************************************************************************* */
    private void processOfflineMessage(ArrayReader reader) {
        // Extract UIN
        long uinRaw = reader.getDWordLE();
        String uin = String.valueOf(uinRaw);

        int year = reader.getWordLE();
        int mon = reader.getByte();
        int day = reader.getByte();
        int hour = reader.getByte();
        int min = reader.getByte();
        int sec = 0;

        // Get type
        int type = reader.getWordLE();

        // Get text length
        int textLen = reader.getWordLE();

        byte[] msgData = reader.getArray(textLen);
        String text = StringConvertor.byteArrayToString(msgData, 0, msgData.length);
        if (StringConvertor.isEmpty(text)) {
            return;
        }

        // Extract date of dispatch
        long date = Util.createGmtTime(year, mon, day, hour, min, sec) + 5 * 60 * 60;

        if (0x0001 == type) { // Normal message
            addOfflineMessage(uin, text, date);
        }
    }
    // Forwards received packet
    public void forward(Packet packet) throws JimmException {
        // Watch out for channel 4 (Disconnect) packets
        if (packet instanceof DisconnectPacket) {
            DisconnectPacket disconnectPacket = (DisconnectPacket) packet;

            // Throw exception
            throw disconnectPacket.makeException();
        }

        if (packet instanceof FromIcqSrvPacket) {
            FromIcqSrvPacket fromIcqSrvPacket = (FromIcqSrvPacket) packet;

            // Watch out for SRV_OFFLINEMSG
            if (FromIcqSrvPacket.SRV_OFFLINEMSG_SUBCMD == fromIcqSrvPacket.getSubcommand()) {
                processOfflineMessage(fromIcqSrvPacket.getReader());

                // Watch out for SRV_DONEOFFLINEMSGS
            } else if (FromIcqSrvPacket.SRV_DONEOFFLINEMSGS_SUBCMD == fromIcqSrvPacket.getSubcommand()) {
                sendPacket(new ToIcqSrvPacket(0x00000000, getIcq().getUserId(),
                        ToIcqSrvPacket.CLI_ACKOFFLINEMSGS_SUBCMD, new byte[0], new byte[0]));

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            } else  {
                unknownPacket(packet);
            // #sijapp cond.end#
            }

        } else if (packet instanceof SnacPacket) {
            SnacPacket snacPacket = (SnacPacket) packet;
            int family = snacPacket.getFamily();
            int command = snacPacket.getCommand();

            if (SnacPacket.CONTACT_FAMILY == family) {
                if (SnacPacket.SRV_USERONLINE_COMMAND == command) {
                    userOnline(snacPacket);

                } else if (SnacPacket.SRV_USEROFFLINE_COMMAND == command) {
                    userOffline(snacPacket);

                } else if (0x1F == command) {
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    MagicEye.addAction(getIcq(), "1", "Verification");
                    // #sijapp cond.end #
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    DebugLog.println("Verification");
                    // #sijapp cond.end #

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else  {
                    unknownPacket(packet);
                // #sijapp cond.end#
                }

            } else if (SnacPacket.CLI_ICBM_FAMILY == family) {
                if ((SnacPacket.SRV_MSG_ACK_COMMAND == command)
                        || (SnacPacket.CLI_ACKMSG_COMMAND == command)) {
                    updateMessageStatus(snacPacket);
                }
                if (SnacPacket.CLI_ACKMSG_COMMAND == command) {
                    ackMessage(snacPacket.getReader());

                } else if (SnacPacket.SRV_RECVMSG_COMMAND == command) {
                    try {
                        readMessage(snacPacket.getReader());
                    } catch (Exception e) {
                    }

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else if (SnacPacket.SRV_MISSED_MESSAGE_COMMAND == command) {
                    ArrayReader marker = snacPacket.getReader();
                    marker.skip(2);
                    int uinLen = marker.getByte();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            marker.getArray(uinLen), 0, uinLen);
                    marker.skip(2 /* warning level */);
                    int tlvCount = marker.getWordBE();
                    for (int i = 0; i < tlvCount; ++i) {
                        marker.skipTlv();
                    }
                    int count = marker.getWordBE();
                    int reasone = marker.getWordBE();
                    DebugLog.println("msg: " + uin + " [" + reasone +  "]x("+count+")");
                // #sijapp cond.end#

                // #sijapp cond.if modules_SOUND is "true" #
                } else if (SnacPacket.SRV_MTN_COMMAND == command) {
                    // Typing notify
                    ArrayReader buf = snacPacket.getReader();
                    buf.skip(10);
                    int uin_len = buf.getByte();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            buf.getArray(uin_len), 0, uin_len);
                    int flag = buf.getWordBE();
                    if (Options.getInt(Options.OPTION_TYPING_MODE) > 0) {
                        getIcq().beginTyping(uin, (0x0002 == flag));
                    }
                // #sijapp cond.end#

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else {
                    unknownPacket(packet);
                // #sijapp cond.end#
                }

                //	  Watch out for SRV_ADDEDYOU
            } else if (SnacPacket.SSI_FAMILY == family) {
                if (SnacPacket.SRV_ADDEDYOU_COMMAND == command) {
                    // Get UIN of the contact changing status
                    String uin = getUinByByteLen(snacPacket.getReader());

                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    MagicEye.addAction(getIcq(), uin,
                            JLocale.getString("youwereadded") + uin);
                    // #sijapp cond.end #

                    //	  Watch out for SRV_GRAND
                } else if (SnacPacket.SRV_GRANT_FUTURE_AUTH_COMMAND == command) {
                    ArrayReader authMarker = snacPacket.getReader();

                    // Get UIN of the contact changing status
                    int length = authMarker.getByte();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            authMarker.getArray(length), 0, length);

                    String reason = getReasone(authMarker);

                    getIcq().setAuthResult(uin, true);

                    //	  Watch out for SRV_AUTHREQ
                } else if (SnacPacket.SRV_AUTHREQ_COMMAND == command) {
                    ArrayReader authMarker = snacPacket.getReader();

                    // Get UIN of the contact changing status
                    int length = authMarker.getByte();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            authMarker.getArray(length), 0, length);

                    String reason = getReasone(authMarker);

                    getIcq().addMessage(new SystemNotice(getIcq(), SystemNotice.SYS_NOTICE_AUTHREQ, uin, reason));

                    //	  Watch out for SRV_AUTHREPLY
                } else if (SnacPacket.SRV_AUTHREPLY_COMMAND == command) {
                    ArrayReader authMarker = snacPacket.getReader();

                    // Get UIN of the contact changing status
                    int length = authMarker.getByte();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            authMarker.getArray(length), 0, length);

                    // Get granted boolean
                    boolean granted = (0x01 == authMarker.getByte());

                    String reason = getReasone(authMarker);

                    // Handle the new system notice
                    if (granted) {
                        getIcq().setAuthResult(uin, true);
                    } else {
                        Contact c = getIcq().getItemByUIN(uin);
                        if ((null != c) && !c.isTemp()) {
                            // #sijapp cond.if modules_MAGIC_EYE is "true" #
                            MagicEye.addAction(getIcq(), uin,
                                    JLocale.getString("denyedby") + uin);
                            // #sijapp cond.end #
                        }
                    }

                } else if (SnacPacket.CLI_ROSTERDELETE_COMMAND == command) {
                    ArrayReader buf = snacPacket.getReader();
                    int length = buf.getWordBE();
                    String uin = StringConvertor.byteArrayToAsciiString(
                            buf.getArray(length), 0, length);

                    buf.skip(4);
                    int type = buf.getWordBE();
                    if (0 == type) {
                        Contact c = getIcq().getItemByUIN(uin);
                        if ((null != c) && !c.isTemp()) {
                            c.setTempFlag(true);
                            String message = JLocale.getString("contact_has_been_removed");
                            if (c.hasChat()) {
                                getIcq().addMessage(new SystemNotice(getIcq(),
                                        SystemNotice.SYS_NOTICE_MESSAGE, uin, message));
                            }
                            // #sijapp cond.if modules_MAGIC_EYE is "true" #
                            MagicEye.addAction(getIcq(), uin, message);
                            // #sijapp cond.end #
                        }
                    }

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                } else  {
                    unknownPacket(packet);
                // #sijapp cond.end#
                }

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            } else  {
                unknownPacket(packet);
            // #sijapp cond.end#
            }
        }
    }
    private String getReasone(ArrayReader marker) {
        String reason = null;
        try {
            int length = marker.getWordBE();
            reason = StringConvertor.byteArrayToString(
                    marker.getArray(length), 0, length);
        } catch (Exception e) {
        }
        return reason;
    }

    // Merge two received capabilities into one byte array
    private byte[] mergeCapabilities(byte[] oldCaps, byte[] newCaps) {
        if (null == newCaps) {
            return oldCaps;
        }

        // Extend new capabilities to match with old ones
        int newCapsCount = 0;
        for (int i = 0; i < newCaps.length; i += 2) {
            if (0x13 != newCaps[i]) continue;
            newCapsCount++;
        }
        if (0 == newCapsCount) {
            return oldCaps;
        }
        final int oldCapsSize = (null == oldCaps) ? 0 : oldCaps.length;

        final byte[] allCaps = new byte[newCapsCount * 16 + oldCapsSize];
        int allCapsPos = 0;

        if (0 < oldCapsSize) {
            System.arraycopy(oldCaps, 0, allCaps, allCapsPos, oldCapsSize);
            allCapsPos += oldCapsSize;
        }

        final byte[] CAP_OLD = GUID.CAP_DC.toByteArray();
        for (int i = 0; i < newCaps.length; i += 2) {
            if (0x13 != newCaps[i]) continue;
            System.arraycopy(CAP_OLD, 0, allCaps, allCapsPos, CAP_OLD.length);
            System.arraycopy(newCaps, i, allCaps, allCapsPos + 2, 2);
            allCapsPos += 16;
        }

        return allCaps;
    }

    // #sijapp cond.if modules_DEBUGLOG is "true" #
    private void unknownPacket(Packet packet) {
        if (!(packet instanceof SnacPacket)) {
            return;
        }
        SnacPacket snacPacket = (SnacPacket) packet;
        int family = snacPacket.getFamily();
        int command = snacPacket.getCommand();
        if (0x1 == family) {
            if (0x21 == command) return;// my iMessage
            if (0x0F == command) return;// my status info
//            if (0x0A == command) {
//                DebugLog.dump("0x01 0x0A", snacPacket.getData());
//            }
        }
        if (packet instanceof FromIcqSrvPacket) {
            FromIcqSrvPacket fisp = (FromIcqSrvPacket)packet;
            ArrayReader reader = fisp.getReader();
            int cmd = reader.getWordLE();
            DebugLog.println("pkt: family 0x" + Integer.toHexString(family)
                    + " command 0x" + Integer.toHexString(command)
                    + " type 0x" + Integer.toHexString(fisp.getSubcommand())
                    + " subtype 0x" + Integer.toHexString(cmd)
                    + " length="  + snacPacket.getReader().getBuffer().length);
        } else {
            DebugLog.println("pkt: family 0x" + Integer.toHexString(family)
                    + " command 0x" + Integer.toHexString(command)
                    + " length="  + snacPacket.getReader().getBuffer().length);
        }
    }
    // #sijapp cond.end#

    private void ackMessage(ArrayReader reader) throws JimmException {
        reader.skip(10); // cookie (8), chanel (2)
        int uinLen = reader.getByte() /* userid length */;
        String uin = StringConvertor.byteArrayToAsciiString(
                reader.getArray(uinLen), 0, uinLen);
        reader.skip(2); // reasone (2)
        // Get message type
        if ((58 + uinLen) < reader.getBuffer().length) {
            reader.setOffset(58 + uinLen);
            int msgType = reader.getWordLE();
            reader.setOffset(64 + uinLen);
            int textLen = reader.getWordLE();
            reader.skip(textLen);
            if (MESSAGE_TYPE_EXTENDED == msgType) {
                // #sijapp cond.if modules_XSTATUSES is "true" #
                XtrazMessagePlugin plg = unpackPlugin(reader, uin);
                if (null != plg) {
                    sendPacket(plg.getPacket());
                }
                // #sijapp cond.end #
            }
        }
    }
    private String removeHtml(String text) {
        if (!text.startsWith("<HTML>")) {
            return text;
        }
        StringBuffer escapedText = new StringBuffer();
        boolean inTag = false;
        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (inTag) {
                if ('>' == ch) {
                    inTag = false;
                }

            } else {
                if ('<' == ch) {
                    inTag = true;
                    if (text.startsWith("br", i + 1) || text.startsWith("BR", i + 1)) {
                        escapedText.append("\n");
                    }
                } else {
                    escapedText.append(ch);
                }
            }
        }
        return Util.xmlUnescape(escapedText.toString());
    }

    private boolean isMrim(String userId) {
        return -1 != userId.indexOf('@');
    }
    private void addMessage(String uin, String text) {
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        if (isMrim(uin)) {
            // FIXME: it's hack
            text = Util.xmlUnescape(text);
            text = StringConvertor.convert(StringConvertor.MRIM2JIMM, text);
        }
        text = removeHtml(text);
        text = StringConvertor.removeCr(text);
        text = StringConvertor.trim(text);
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        getIcq().addMessage(new PlainMessage(uin, getIcq(),
                Jimm.getCurrentGmtTime(), text, false));
    }
    private void addOfflineMessage(String uin, String text, long date) {
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        text = StringConvertor.removeCr(text);
        text = StringConvertor.trim(text);
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        getIcq().addMessage(new PlainMessage(uin, getIcq(), date, text, true));
    }

    void sendRecivedFlag(byte[] buf, String uin, boolean msg2Buf) throws JimmException {
        // Acknowledge message
        Util packet = new Util();
        packet.writeByteArray(buf, 0, 8);
        packet.writeWordBE(0x0002);
        packet.writeShortLenAndUtf8String(uin);
        packet.writeWordBE(0x0003);

        packet.writeWordLE(0x001b);
        packet.writeWordLE(0x0008);
        packet.writeDWordLE(0);
        packet.writeDWordLE(0);
        packet.writeDWordLE(0);
        packet.writeDWordLE(0);
        packet.writeWordLE(0x0000);
        packet.writeDWordLE(0x00000003);
        packet.writeByte(0x00);
        packet.writeWordLE(0xFFFE);
        packet.writeWordLE(0x000E);
        packet.writeWordLE(0xFFFE);
        packet.writeDWordLE(0);
        packet.writeDWordLE(0);
        packet.writeDWordLE(0);
        packet.writeDWordLE(1);
        packet.writeWordLE(1);

        packet.writeWordLE(0x0001);
        packet.writeByte(0x00);

        sendPacket(new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                SnacPacket.CLI_ACKMSG_COMMAND, 0, packet.toByteArray()));
    }
    private void readMessage(ArrayReader marker) throws JimmException {

        // Get message format
        marker.skip(8 /* cookie */);
        int format = marker.getWordBE();


        // Get UIN length
        int uinLen = marker.getByte();

        // Get UIN
        String uin = StringConvertor.byteArrayToAsciiString(
                marker.getArray(uinLen), 0, uinLen);

        marker.skip(2 /* WARNING */);

        // Skip TLVS
        int tlvCount = marker.getWordBE();
        for (int i = 0; i < tlvCount; ++i) {
            marker.skipTlv();
        }

        // Get message data and initialize marker
        int tlvType;
        byte[] msgBuf;
        do {
            tlvType = marker.getTlvType();
            msgBuf = marker.getTlv();
        } while ((tlvType != 0x0002) && (tlvType != 0x0005));

        ArrayReader msgMarker = new ArrayReader(msgBuf, 0);

        //////////////////////
        // Message format 1 //
        //////////////////////
        if (format == 0x0001) {

            // Variables for all possible TLVs
            // byte[] capabilities = null;
            byte[] message = null;

            // Read all TLVs
            while (msgMarker.isNotEnd()) {

                // Get type of next TLV
                tlvType = msgMarker.getTlvType();
                // Get next TLV
                byte[] tlvValue = msgMarker.getTlv();

                // Save value
                switch (tlvType) {
                    case 0x0501:
                        // capabilities
                        // capabilities = tlvValue;
                        break;
                    case 0x0101:
                        // message
                        message = tlvValue;
                        break;
                }

            }

            // Process packet if at least the message TLV was present
            if (null != message) {

                boolean ucs2 = (0x0002 == Util.getWordBE(message, 0));
                // Get message text
                String text = null;
                if (ucs2) {
                    text = StringConvertor.ucs2beByteArrayToString(message, 4, message.length - 4);
                } else {
                    text = StringConvertor.byteArrayToWinString(message, 4, message.length - 4);
                }

                // Construct object which encapsulates the received
                // plain message
                addMessage(uin, text);
                sendRecivedFlag(marker.getBuffer(), uin, false);
            }

            //////////////////////
            // Message format 2 //
            //////////////////////
        } else if (format == 0x0002) {
            // TLV(A): Acktype 0x0000 - normal message
            //                 0x0001 - file request / abort request
            //                 0x0002 - file ack

            // Get and validate SUB_MSG_TYPE2.COMMAND
            int cmd = msgMarker.getWordBE();
            if (0x0000 != cmd) return; // Only normal messages are supported yet

            // Skip SUB_MSG_TYPE2.TIME and SUB_MSG_TYPE2.ID
            msgMarker.skip(4 + 4);

            // Skip SUB_MSG_TYPE2.CAPABILITY
            msgMarker.skip(16);

            // #sijapp cond.if modules_FILES is "true"#
            int ackType = -1;
            byte[] extIP = new byte[4];
            byte[] ip = new byte[4];
            int port = 0;
            // #sijapp cond.end#
            int status = -1;

            // Get message data and initialize marker
            byte[] msg2Buf = null;
            do {
                // Get type of next TLV
                tlvType = msgMarker.getTlvType();
                // Get next TLV
                byte[] infoBuf = msgMarker.getTlv();
                // #sijapp cond.if modules_FILES is "true"#
                switch (tlvType) {
                    case 0x0003: System.arraycopy(infoBuf, 0, extIP, 0, 4);  break;
                    case 0x0004: System.arraycopy(infoBuf, 0, ip, 0, 4);     break;
                    case 0x0005: port = Util.getWordBE(infoBuf, 0);         break;
                    case 0x000a: ackType = Util.getWordBE(infoBuf, 0);      break;
                }
                // #sijapp cond.end#
                if (0x2711 == tlvType) {
                    msg2Buf = infoBuf;
                }
            } while (msgMarker.isNotEnd());

            // Get message data and initialize marker
            if (null == msg2Buf) {
                return;
            }
            ArrayReader msg2Reader = new ArrayReader(msg2Buf, 0);

            // Skip values up to (and including) SUB_MSG_TYPE2.UNKNOWN
            // (before MSGTYPE)
            msg2Reader.skip(msg2Reader.getWordLE());
            msg2Reader.skip(msg2Reader.getWordLE());

            // Get and validate message type
            int msgType = msg2Reader.getWordLE();
            if (!((MESSAGE_TYPE_PLAIN == msgType)
                    || (MESSAGE_TYPE_URL == msgType)
                    || (MESSAGE_TYPE_PLUGIN == msgType)
                    || ((msgType >= 1000) && (msgType <= 1004)))) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                if (MESSAGE_TYPE_ADDED == msgType) {
                    DebugLog.println("msg type 0x" + Integer.toHexString(msgType));
                }
                // #sijapp cond.end #
                return;
            }

            status = msg2Reader.getWordLE();

            msg2Reader.skip(2 /* PRIORITY */);

            // Get length of text
            int textLen = msg2Reader.getWordLE();
            // Get raw text
            byte[] rawText = msg2Reader.getArray(textLen);

            // Plain message or URL message
            if (0x0001 == msgType) {
                if (rawText.length == 0) {
                    return;
                }

                msg2Reader.skip(4 /* FOREGROUND */ + 4 /* BACKGROUND */);

                // Check encoding (by checking GUID)
                boolean isUtf8 = false;
                if (msg2Buf.length >= msg2Reader.getOffset() + 4) {
                    int guidLen = (int) msg2Reader.getDWordLE();
                    if (guidLen == 38) {
                        byte[] guidBin = msg2Reader.getArray(guidLen);
                        String guid = StringConvertor.byteArrayToAsciiString(
                                guidBin, 0, guidBin.length);
                        if ("{0946134E-4C7F-11D1-8222-444553540000}".equals(guid)) {
                            isUtf8 = true;
                        }
                    }
                }

                // Decode text and create Message object
                String text = null;
                // Decode text
                if (isUtf8) {
                    text = StringConvertor.utf8beByteArrayToString(rawText, 0, rawText.length);
                } else {
                    text = StringConvertor.byteArrayToWinString(rawText, 0, rawText.length);
                }
                // Forward message object to contact list
                addMessage(uin, text);
                sendRecivedFlag(marker.getBuffer(), uin, true);

            } else if (0x0004 == msgType) { // URL

                // Extended message
            } else if (msgType == 0x001A) {
                // #sijapp cond.if modules_XSTATUSES is "true" #
                XtrazMessagePlugin plg = unpackPlugin(msg2Reader, uin);
                if (null != plg) {
                    marker.setOffset(0);
                    plg.setCookie(marker);
                    sendPacket(plg.getPacket());
                    return;
                }
                // #sijapp cond.end #
            }

            //////////////////////
            // Message format 4 //
            //////////////////////
        } else if (format == 0x0004) {

            // Skip SUB_MSG_TYPE4.UIN
            msgMarker.skip(4);

            // Get SUB_MSG_TYPE4.MSGTYPE
            int msgType = msgMarker.getWordLE();

            // Only plain messages and URL messagesa are supported
            if (msgType != 0x0001) return;

            // Get length of text
            int textLen = msgMarker.getWordLE();

            // Get text
            String text = StringConvertor.byteArrayToWinString(
                    msgMarker.getArray(textLen), 0, textLen);

            // Forward message to contact list
            addMessage(uin, text);
        }
    }

    private String getUinByByteLen(ArrayReader reader) {
        int len = reader.getByte();
        byte[] buf = reader.getArray(len);
        return StringConvertor.byteArrayToAsciiString(buf, 0, buf.length);
    }

    private String lastOfflineUin = null;
    private void userOffline(SnacPacket snacPacket) {
        // Get UIN of the contact that goes offline
        String uin = getUinByByteLen(snacPacket.getReader());

        IcqContact item = (IcqContact)getIcq().getItemByUIN(uin);
        if (null != item) {
            boolean hasBeenOffline = !item.isOnline();
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            if (getIcq().isConnected() && hasBeenOffline) {
                if (item.getUserId().equals(lastOfflineUin)) {
                    MagicEye.addAction(getIcq(), lastOfflineUin, "hiding_from_you");
                }
                lastOfflineUin = item.getUserId();
            }
            // #sijapp cond.end#
            if (hasBeenOffline) {
                return;
            }

            item.setOfflineStatus();
            // Update contact list
            getIcq().ui_changeContactStatus(item);

            // #sijapp cond.if modules_DEBUGLOG is "true" #
        } else {
            DebugLog.println("USER_OFFLINE for " + uin);
            // #sijapp cond.end#
        }
    }
    private void userOnline(SnacPacket snacPacket) {
        // DC variables
        byte[] internalIP = new byte[4];
        byte[] externalIP = new byte[4];
        int dcPort = 0;
        int dcType = -1;
        int authCookie = 0;
        int protocolVersion = 0;

        int dwFT1 = 0;
        int dwFT2 = 0;
        int dwFT3 = 0;
        byte[] capabilities_old = new byte[0]; // Buffer for old style capabilities (TLV 0x000D)
        byte[] capabilities_new = null; // Buffer for new style capabilities (TLV 0x0019)
        String statusText = null;
        String mood = null;

        // Time variables
        int idle = -1;
        int online = -1;
        long signon = -1;

        // Get UIN of the contact changing status
        int status = IcqStatusInfo.STATUS_ONLINE;

        // Get data
        ArrayReader marker = snacPacket.getReader();

        int uinLen = marker.getByte();
        String uin = StringConvertor.byteArrayToAsciiString(
                marker.getArray(uinLen), 0, uinLen);
        marker.skip(2 /* warning level */);

        // Get new status and client capabilities
        int tlvNum = marker.getWordBE();
        for (int i = 0; i < tlvNum; ++i) {
            int tlvType = marker.getTlvType();
            byte[] tlvData = marker.getTlv();

            if (tlvType == 0x0006) {// STATUS
                status = (int)Util.getDWordBE(tlvData, 0);
            } else if (tlvType == 0x000D) {// Old style CAPABILITIES
                capabilities_old = tlvData;
            } else if (tlvType == 0x0019) {// New style CAPABILITIES
                capabilities_new = tlvData;
            } else if (tlvType == 0x000A) {// External IP
                externalIP = tlvData;

            } else if (tlvType == 0x000C) {// DC Infos
                ArrayReader dcMarker = new ArrayReader(tlvData, 0);
                internalIP = dcMarker.getArray(4);
                dcPort = (int)dcMarker.getDWordBE();
                dcType = dcMarker.getByte();
                protocolVersion = dcMarker.getWordBE();
                authCookie = (int)dcMarker.getDWordBE();
                dcMarker.skip(8);
                dwFT1 = (int) dcMarker.getDWordBE();
                dwFT2 = (int) dcMarker.getDWordBE();
                dwFT3 = (int) dcMarker.getDWordBE();

            } else if (tlvType == 0x0003) {// Signon time
                signon = byteArrayToLong(tlvData); // GMT
            } else if (tlvType == 0x0004) {// Idle time
                idle   = (int)byteArrayToLong(tlvData);
            } else if (tlvType == 0x000F) {// Online time
                online = (int)byteArrayToLong(tlvData);

                // Icon service... and new style status message
            } else if (tlvType == 0x001D) {
                ArrayReader iconMarker = new ArrayReader(tlvData, 0);
                final int BART_STATUS_STR = 0x0002;
                final int BART_STATUS_ID  = 0x000E;
                while (iconMarker.isNotEnd()) {
                    int bartType = iconMarker.getWordBE();
                    iconMarker.skip(1);
                    int recordLen = iconMarker.getByte();

                    if (0 == recordLen) continue;
                    if (BART_STATUS_ID == bartType) {
                        mood = StringConvertor.utf8beByteArrayToString(
                                iconMarker.getBuffer(), iconMarker.getOffset(), recordLen);

                    } else if (BART_STATUS_STR == bartType) {
                        int len = iconMarker.getWordBE();
                        recordLen -= 2;
                        statusText = StringConvertor.utf8beByteArrayToString(
                                iconMarker.getBuffer(), iconMarker.getOffset(), len);
                    }
                    iconMarker.skip(recordLen);
                }
            }
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("mood for " + uin + " " + mood + " " + statusText);
        // #sijapp cond.end #
        IcqContact contact = (IcqContact)getIcq().getItemByUIN(uin);
        if (contact != null) {
            // TODO: happy flag
            int flags = (status >> 16) & 0xFFFF;

            // #sijapp cond.if modules_XSTATUSES is "true" #
            contact.setXStatus(Icq.xstatus.createXStatus(capabilities_old, mood), null);
            // #sijapp cond.end #
            statusText = StringConvertor.trim(statusText);
            if (!StringConvertor.isEmpty(statusText)) {
                // #sijapp cond.if modules_XSTATUSES is "true" #
                if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
                    contact.setXStatusMessage(statusText);
                    statusText = null;
                }
                // #sijapp cond.end #
            }
            getIcq().setContactStatus(contact,
                    IcqStatusInfo.getStatusIndex(status & 0xFFFF, capabilities_old),
                    statusText);


            contact.happyFlag = ((flags & FLAG_HAPPY) != 0)
                    || GUID.CAP_QIP_HAPPY.equals(capabilities_old);

            byte[] capa = mergeCapabilities(capabilities_old, capabilities_new);
            capabilities_old = null;
            capabilities_new = null;

            // #sijapp cond.if modules_CLIENTS is "true" #
            // Update time values
            contact.setTimeOfChaingingStatus(signon);
            ClientDetector.instance.execVM(contact, capa, new int[] {dwFT1, dwFT2, dwFT3}, protocolVersion);
            // #sijapp cond.end #

            // Update contact list
            getIcq().ui_changeContactStatus(contact);

            // #sijapp cond.if modules_DEBUGLOG is "true" #
        } else {
            DebugLog.println("USER_ONLINE for " + uin + " (0x" + Integer.toHexString(status) + ")");
            // #sijapp cond.end#
        }
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private XtrazMessagePlugin unpackPlugin(ArrayReader reader, String uin) {
        // size + XTRAZ_GUID + func
        int someLen = reader.getWordLE();
        int bufPos = reader.getOffset() + someLen;
        int guidPos = reader.getOffset();
        if (XtrazMessagePlugin.XTRAZ_GUID.equals(reader.getBuffer(), guidPos, 16)) {
            reader.setOffset(bufPos);
            return parseXtrazMessage(uin, reader);
        }
        return null;
    }

    private String getText(String text) {
        return Util.xmlUnescape(text);
    }

    private String getTagContent(String xml, String tag) {
        int begin = xml.indexOf("<" + tag + ">");
        int end   = xml.indexOf("</" + tag + ">");
        if (begin >= 0 && end > begin) {
            int offset = tag.length() + 2;
            return getText(xml.substring(begin + offset, end));
        }
        return "";
    }
    private String makeXPromt(String promt) {
        return Util.xmlEscape(promt);
    }

    private static String[] tags = Util.explode("title|desc", '|');
    private XtrazMessagePlugin parseXtrazMessage(String uin, ArrayReader reader) {
        Icq icq = getIcq();
        IcqContact contact = (IcqContact)icq.getItemByUIN(uin);
        reader.skip(4);
        int xmlLen = (int)reader.getDWordLE();
        xmlLen = Math.min(xmlLen, reader.getBuffer().length - reader.getOffset());
        String xml = StringConvertor.utf8beByteArrayToString(
                reader.getArray(xmlLen), 0, xmlLen);

        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        if (xml.startsWith("<N>")) {
            MagicEye.addAction(icq, uin, "read xtraz");
        }
        // #sijapp cond.end #

        if (contact == null) return null;

        if (xml.startsWith("<NR>")) {
            String res = getTagContent(xml, "RES");
            String title = StringConvertor.notNull(getTagContent(res, tags[0])).trim();
            String desc = StringConvertor.notNull(getTagContent(res, tags[1])).trim();
            String text = (title + ' ' + desc).trim();
            contact.setXStatusMessage(text);
            getIcq().updateStatusView(contact);

        } else if (xml.startsWith("<N>")) {
            if (!icq.isMeVisible(contact)) {
                return null;
            }
            int id = icq.getProfile().xstatusIndex;
            if (id < 0) {
                return null;
            }

            String title = icq.getProfile().xstatusTitle;
            String desc  = icq.getProfile().xstatusDescription;
            String str ="<ret event='OnRemoteNotification'><srv>"
                    + "<id>cAwaySrv</id><val srv_id='cAwaySrv'>"
                    + "<Root>"
                    + "<CASXtraSetAwayMessage></CASXtraSetAwayMessage>"
                    + "<uin>" + icq.getUserId() + "</uin>"
                    + "<index>" + id + "</index>"
                    + "<title>" + makeXPromt(title) + "</title>"
                    + "<desc>"  + makeXPromt(desc) + "</desc>"
                    + "</Root></val></srv></ret>";
            str = Util.replace(makeXPromt(str), "&apos;", "'");
            return new XtrazMessagePlugin(contact, "<NR><RES>" + str + "</RES></NR>");
        }
        return null;
    }
    // #sijapp cond.end #

    // Converts the specific 4 byte max buffer to an unsigned long
    private long byteArrayToLong(byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        if (b.length > 3) {
            l <<= 8;
            l |= b[2] & 0xFF;
            l <<= 8;
            l |= b[3] & 0xFF;
        }
        return l;
    }

    // Static variables for message type;
    public static final int MESSAGE_TYPE_AUTO     = 0x0000;
    public static final int MESSAGE_TYPE_NORM     = 0x0001;
    public static final int MESSAGE_TYPE_EXTENDED = 0x001a;
    public static final int MESSAGE_TYPE_AWAY     = 0x03e8;
    public static final int MESSAGE_TYPE_OCC      = 0x03e9;
    public static final int MESSAGE_TYPE_NA       = 0x03ea;
    public static final int MESSAGE_TYPE_DND      = 0x03eb;
    public static final int MESSAGE_TYPE_FFC      = 0x03ec;

//    public static final int MESSAGE_TYPE_UNKNOWN  = 0x0000; // Unknown message, only used internally by this plugin
    public static final int MESSAGE_TYPE_PLAIN    = 0x0001; // Plain text (simple) message
//    public static final int MESSAGE_TYPE_CHAT     = 0x0002; // Chat request message
    public static final int MESSAGE_TYPE_FILEREQ  = 0x0003; // File request / file ok message
    public static final int MESSAGE_TYPE_URL      = 0x0004; // URL message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHREQ  = 0x0006; // Authorization request message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHDENY = 0x0007; // Authorization denied message (0xFE formatted)
//    public static final int MESSAGE_TYPE_AUTHOK   = 0x0008; // Authorization given message (empty)
//    public static final int MESSAGE_TYPE_SERVER   = 0x0009; // Message from OSCAR server (0xFE formatted)
    public static final int MESSAGE_TYPE_ADDED    = 0x000C; // "You-were-added" message (0xFE formatted)
//    public static final int MESSAGE_TYPE_WWP      = 0x000D; // Web pager message (0xFE formatted)
//    public static final int MESSAGE_TYPE_EEXPRESS = 0x000E; // Email express message (0xFE formatted)
//    public static final int MESSAGE_TYPE_CONTACTS = 0x0013; // Contact list message
    public static final int MESSAGE_TYPE_PLUGIN   = 0x001A; // Plugin message described by text string
//    public static final int MESSAGE_TYPE_AWAY     = 0x03E8; // Auto away message
//    public static final int MESSAGE_TYPE_OCC      = 0x03E9; // Auto occupied message
//    public static final int MESSAGE_TYPE_NA       = 0x03EA; // Auto not available message
//    public static final int MESSAGE_TYPE_DND      = 0x03EB; // Auto do not disturb message
//    public static final int MESSAGE_TYPE_FFC      = 0x03EC; // Auto free for chat message
}
// #sijapp cond.end #
