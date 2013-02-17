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
File: src/jimm/comm/ConnectPacket.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.packet;

import jimm.JimmException;
import jimm.comm.*;

public class ConnectPacket extends Packet {

    // Packet types
    public static final int SRV_CLI_HELLO = 1;
    public static final int CLI_COOKIE = 2;
    public static final int CLI_IDENT = 3;
    // Cookie (!= null only if packet type is CLI_COOKIE)
    protected byte[] cookie;
    // UIN (!= null only if packet type is CLI_IDENT)
    protected String uin;
    // Password (unencrypted, != null only if packet type is CLI_IDENT)
    protected String password;

    // Constructs a SRV_HELLO/CLI_HELLO packet
    public ConnectPacket() {
        this.cookie = null;
        this.uin = null;
        this.password = null;
    }

    // Constructs a CLI_COOKIE packet
    public ConnectPacket(byte[] cookie) {
        this.cookie = new byte[cookie.length];
        System.arraycopy(cookie, 0, this.cookie, 0, cookie.length);
        this.uin = null;
        this.password = null;
    }

    // Constructs a CLI_IDENT packet
    public ConnectPacket(String uin, String password) {
        this.cookie = null;
        this.uin = uin;
        this.password = password;
    }

    // Returns the packet type
    public int getType() {
        if ((this.cookie == null) && (this.uin == null)) {
            return (ConnectPacket.SRV_CLI_HELLO);
        } else if (this.uin != null) {
            return (ConnectPacket.CLI_IDENT);
        } else {
            return (ConnectPacket.CLI_COOKIE);
        }
    }

    // Returns the cookie, or null if packet type is not CLI_COOKIE
    public byte[] getCookie() {
        if (ConnectPacket.CLI_COOKIE == this.getType()) {
            byte[] result = new byte[this.cookie.length];
            System.arraycopy(this.cookie, 0, result, 0, this.cookie.length);
            return (result);
        } else {
            return (null);
        }
    }

    public static void putVersion(Util stream, boolean first) {
        if (first) {
            stream.writeTLV(0x4C, null);
        }
        stream.writeTLVWord(0xA2, 0x05);
        stream.writeTLVWord(0xA3, 0x05);
        stream.writeTLVWord(0xA4, 0x00);
        stream.writeTLVWord(0xA5, 0x17F2);
        stream.writeTLV(0x03, "ICQ Client".getBytes());
        stream.writeTLVWord(0x17, 20);
        //stream.writeTLVWord(0x1B, 0x00);
        stream.writeTLVWord(0x18, 52);
        stream.writeTLVWord(0x19, 0);
        stream.writeTLVWord(0x1A, 3003);
        stream.writeTLVWord(0x16, 266);
        stream.writeTLV(0x14, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x3D});
        stream.writeTLV(0x0F, "en".getBytes());
        stream.writeTLV(0x0E, "us".getBytes());
        if (!first) {
            stream.writeTLVWord(0x9e, 0x0002);
        }
        if (!first) {
            stream.writeTLVWord(0x9F, 0x0000);
            stream.writeTLVWord(0xA0, 0x0000);
            stream.writeTLVWord(0xA1, 0x08AF);
            stream.writeTLVByte(0x94, 0x00);
        }

        stream.writeTLVByte(0x4A, 0x01);
        if (!first) {
            stream.writeTLVByte(0xAC, 0x00);
        }
    }

    public static void putLiteVersion(Util stream, boolean first) {
        if (first) {
            stream.writeTLV(0x4C, null);
        }
        stream.writeTLVWord(0xA2, 0x05);
        stream.writeTLVWord(0xA3, 0x05);
        stream.writeTLVWord(0xA4, 0x00);
        stream.writeTLVWord(0xA5, 0x17F2);
        stream.writeTLV(0x03, "ICQ Client".getBytes());
        stream.writeTLVWord(0x17, 0x14);
        //stream.writeTLVWord(0x1B, 0x00);
        stream.writeTLVWord(0x18, 0x34);
        stream.writeTLVWord(0x19, 0x00);
        stream.writeTLVWord(0x1A, 0x0BBB);
        stream.writeTLVWord(0x16, 0x010A);
        stream.writeTLV(0x14, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x3D});
        stream.writeTLV(0x0F, "ru".getBytes());
        stream.writeTLV(0x0E, "ru".getBytes());
        if (!first) {
            stream.writeTLVWord(0x9e, 0x0002);
        }
        if (!first) {
            stream.writeTLVWord(0x9F, 0x0000);
            stream.writeTLVWord(0xA0, 0x0000);
            stream.writeTLVWord(0xA1, 0x08AF);
            stream.writeTLVByte(0x94, 0x00);
        }

        stream.writeTLVByte(0x4A, 0x01);
        if (!first) {
            stream.writeTLVByte(0xAC, 0x00);
        }
    }

    // Returns the package as byte array
    public byte[] toByteArray() {
        Util buf = new Util();
        // header
        buf.writeZeroes(6);

        // Assemble HELLO.HELLO
        buf.writeDWordBE(0x00000001);

        // Assemble CLI_COOKIE
        if (this.getType() == ConnectPacket.CLI_COOKIE) {

            // HELLO.COOKIE
            buf.writeTLV(0x0006, this.cookie);

            // Assemble CLI_IDENT
        } else if (this.getType() == ConnectPacket.CLI_IDENT) {
            if (-1 != uin.indexOf('@')) {
                buf.writeTLV(0x0056, null);
            }

            // HELLO.UIN
            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));

            // HELLO.PASSWORD
            buf.writeTLV(0x0002, Util.decipherPassword(
                    StringConvertor.stringToByteArray(this.password)));

        } else {
            buf.writeTLVDWord(0x8003, 0x00100000);
        }

        if (this.getType() == ConnectPacket.CLI_COOKIE) {
            ConnectPacket.putVersion(buf, false);
            buf.writeTLVDWord(0x8003, 0x00100000);

        } else if (this.getType() == ConnectPacket.CLI_IDENT) {
            if (0 < uin.indexOf('@')) {
                ConnectPacket.putVersion(buf, true);
                buf.writeTLVDWord(0x8003, 0x00100000);
            }
        }

        byte[] _buf = buf.toByteArray();
        assembleFlapHeader(_buf, 0x01);
        return _buf;
    }

    // Parses given byte array and returns a Packet object
    public static Packet parse(byte[] flapData) throws JimmException {
        // Check HELLO
        if ((flapData.length < 4) || (Util.getDWordBE(flapData, 0) != 0x00000001)) {
            throw new JimmException(132, 0);
        }

        // Variables for all possible TLVs
        byte[] cookie = null;
        String uin = null;
        String password = null;
        String version = null;
        byte[] unknown = null;

        // Read all TLVs
        ArrayReader marker = new ArrayReader(flapData, 4);
        while (marker.isNotEnd()) {
            int tlvType = marker.getTlvType();
            byte[] tlvValue = marker.getTlv();
            if (tlvValue == null) {
                throw new JimmException(132, 1);
            }

            // Save value
            switch (tlvType) {
                case 0x0006:   // cookie
                    cookie = tlvValue;
                    break;
                case 0x0001:   // uin
                    uin = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0002:   // password
                    password = StringConvertor.byteArrayToAsciiString(Util.decipherPassword(tlvValue));
                    break;
                case 0x0003:   // version
                    version = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0016:   // unknown
                    unknown = tlvValue;
                    break;
            }

        }

        // SRV_HELLO/CLI_HELLO
        if ((cookie == null) && (uin == null) && (password == null) && (version == null) && (unknown == null)) {
            return new ConnectPacket();

            // SRV_COOKIE
        } else if ((cookie != null) && (uin == null) && (password == null) && (version == null) && (unknown == null)) {
            return new ConnectPacket(cookie);

            // CLI_IDENT
        } else if ((cookie == null) && (uin != null) && (password != null) && (version != null) && (unknown != null)) {
            return new ConnectPacket(uin, password);

            // Other TLV combinations are not valid
        } else {
            throw new JimmException(132, 3);
        }

    }
}
// #sijapp cond.end #
