/*
 * XtrazMessagePlugin.java
 *
 * Created on 11 ������� 2007 �., 0:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package protocol.icq.plugin;

import jimm.Jimm;
import jimm.comm.*;
import protocol.icq.*;
import protocol.icq.packet.SnacPacket;

/**
 *
 * @author vladimir
 */
public class XtrazMessagePlugin {
    private long time;
    private long cookie1;
    private long cookie2;
    private IcqContact rcvr;
    private String msg;
    private static final String NAME = "Script Plug-in: Remote Notification Arrive";
    public static final GUID XTRAZ_GUID = new GUID(new byte[]{(byte)0x3b, (byte)0x60, (byte)0xb3, (byte)0xef, (byte)0xd8, (byte)0x2a, (byte)0x6c, (byte)0x45, (byte)0xa4, (byte)0xe0, (byte)0x9c, (byte)0x5a, (byte)0x5e, (byte)0x67, (byte)0xe8, (byte)0x65});
    public static final int MGTYPE_SCRIPT_NOTIFY = 0x0008;

    public void setCookie(ArrayReader reader) {
        cookie1 = reader.getDWordBE();
        cookie2 = reader.getDWordBE();
    }

    /** Creates a new instance of XtrazMessagePlugin */
    public XtrazMessagePlugin(IcqContact rcvr, String msg) {
        this.rcvr = rcvr;
        this.msg = msg;
    }


    private boolean isRequest() {
        return msg.startsWith("<N>");
    }


    private byte[] getData() {
        byte[] str = StringConvertor.stringToByteArrayUtf8(msg);
        Util buffer = new Util();
        buffer.writeDWordLE(str.length);
        buffer.writeByteArray(str);
        return buffer.toByteArray();
    }


    public SnacPacket getPacket() {
        time = Jimm.getCurrentGmtTime() * 1000;
        if (!isRequest()) {
            return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                    SnacPacket.CLI_ACKMSG_COMMAND,
                    initAckMsg());
        } else {
            return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                    SnacPacket.CLI_SENDMSG_COMMAND,
                    initReqMsg());
        }
    }
    private byte[] initAckMsg() {
        // Get UIN
        byte[] uinRaw = StringConvertor.stringToByteArray(rcvr.getUserId());

        Util buffer = new Util();

        buffer.writeDWordBE(cookie1); // CLI_SENDMSG.TIME
        buffer.writeDWordBE(cookie2); // CLI_SENDMSG.ID
        buffer.writeWordBE(0x0002); // CLI_SENDMSG.FORMAT
        buffer.writeByte(uinRaw.length); // CLI_SENDMSG.UIN
        buffer.writeByteArray(uinRaw);
        buffer.writeWordBE(0x0003); // CLI_SENDMSG.FORMAT
        buffer.writeByteArray(makeTlv1127());

        return buffer.toByteArray();
    }



    private byte[] makeTlv5() {
        Util tlv5 = new Util();
        tlv5.writeWordBE(0x0000);
        tlv5.writeDWordBE(time);
        tlv5.writeDWordBE(0x00000000);
        // SUB_MSG_TYPE2.CAPABILITY
        tlv5.writeByteArray(GUID.CAP_AIM_SERVERRELAY.toByteArray());

        // Set TLV 0x0a to 0x0001
        tlv5.writeTLVWord(0x000a, 0x0001);

        // Set emtpy TLV 0x0f
        tlv5.writeTLV(0x000f, null);

        // Set TLV 0x2711
        tlv5.writeTLV(0x2711, makeTlv1127());
        return tlv5.toByteArray();
    }
    private byte[] initReqMsg() {
        // Get UIN
        byte[] uinRaw = StringConvertor.stringToByteArray(rcvr.getUserId());

        // Build the packet
        Util buffer = new Util();

        buffer.writeDWordBE(time); // CLI_SENDMSG.TIME
        buffer.writeDWordBE(0x00000000); // CLI_SENDMSG.ID
        buffer.writeWordBE(0x0002); // CLI_SENDMSG.FORMAT
        buffer.writeByte(uinRaw.length); // CLI_SENDMSG.UIN
        buffer.writeByteArray(uinRaw);

        // TYPE2 Specific Data
        buffer.writeTLV(0x0005, makeTlv5());
        buffer.writeTLV(0x0003, null); // CLI_SENDMSG.UNKNOWN
        return buffer.toByteArray();
    }

    private byte[] makeTlv1127() {
        byte[] textRaw = new byte[0];
        byte[] pluginData = pluginData();
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
        int SEQ1 = 0xffff - 1;
        tlv1127.writeWordLE(SEQ1);
        tlv1127.writeWordLE(0x000E); // length
        tlv1127.writeWordLE(SEQ1);

        // Put 12 unknown zero bytes
        tlv1127.writeDWordLE(0x00000000);
        tlv1127.writeDWordLE(0x00000000);
        tlv1127.writeDWordLE(0x00000000);

        // Put message type 0x0001 if normal message else 0x001a for file request
        tlv1127.writeWordLE(IcqNetDefActions.MESSAGE_TYPE_EXTENDED);

        // Put contact status
        tlv1127.writeWordLE(IcqStatusInfo.STATUS_ONLINE);

        // Put priority
        tlv1127.writeWordLE(0x0001);

        // Put message
        tlv1127.writeWordLE(textRaw.length + 1);
        tlv1127.writeByteArray(textRaw);
        tlv1127.writeByte(0x00);
        tlv1127.writeByteArray(pluginData);

        return tlv1127.toByteArray();
    }

    private byte[] pluginData() {
        byte[] subType = StringConvertor.stringToByteArray(NAME);
        byte[] data = getData();
        GUID guid = XTRAZ_GUID;
        int command = MGTYPE_SCRIPT_NOTIFY;
        int flag2 = 0x00000000;

        int headerLen = 16 + 2 +  4 + subType.length  + 4 + 4 + 4 + 2 + 1;
        Util buffer = new Util();

        buffer.writeWordLE(headerLen);

        buffer.writeByteArray(guid.toByteArray());
        buffer.writeWordLE(command);

        // plugin name
        buffer.writeDWordLE(subType.length);
        buffer.writeByteArray(subType);

        buffer.writeDWordBE(0x00000100);
        buffer.writeDWordBE(flag2);
        buffer.writeDWordBE(0x00000000);
        buffer.writeWordBE(0x0000);
        buffer.writeByte(0x00);


        buffer.writeDWordLE(data.length);
        buffer.writeByteArray(data);

        return buffer.toByteArray();
    }
}
// #sijapp cond.end#
// #sijapp cond.end#
