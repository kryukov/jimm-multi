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
File: src/jimm/comm/DisconnectPacket.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.packet;

import jimm.*;
import jimm.comm.*;

public class DisconnectPacket extends Packet {
    // Packet types

    public static final int TYPE_SRV_COOKIE = 1;
    public static final int TYPE_SRV_GOODBYE = 2;
    public static final int TYPE_CLI_GOODBYE = 3;
    // UIN as string (== null for CLI_GOODBYE packets)
    protected String uin;
    // Server (!= null only for SRV_COOKIE packets)
    protected String server;
    // Cookie (!= null only for SRV_COOKIE packets)
    protected byte[] cookie;
    // Reason for disconnect as an error code (>= 0 only for SRV_DISCONNECT packets)
    protected int error;
    // Reason for disconnect as a string (!= null only for SRV_DISCONNECT packets)
    protected String description;

    // Constructs a SRV_COOKIE packet
    public DisconnectPacket(String uin, String server, byte[] cookie) {
        this.uin = uin;
        this.server = server;
        this.cookie = new byte[cookie.length];
        System.arraycopy(cookie, 0, this.cookie, 0, cookie.length);
        this.error = -1;
        this.description = null;
    }

    // Constructs a SRV_GOODBYE packet
    public DisconnectPacket(int error, String description) {
        this.uin = null;
        this.server = null;
        this.cookie = null;
        this.error = error;
        this.description = description;
    }

    // Constructs a CLI_GOODBYE packet
    public DisconnectPacket() {
        this.uin = null;
        this.server = null;
        this.cookie = null;
        this.error = -1;
        this.description = null;
    }

    public String getUin() {
        return uin;
    }
    // Returns the packet type

    public int getType() {
        if (this.uin != null) {
            return DisconnectPacket.TYPE_SRV_COOKIE;
        } else if (this.error >= 0) {
            return DisconnectPacket.TYPE_SRV_GOODBYE;
        } else {
            return DisconnectPacket.TYPE_CLI_GOODBYE;
        }
    }

    public JimmException makeException() {
        // Unknown error
        int toThrow = 100;
        switch (getError()) {
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
        return new JimmException(toThrow, getError());
    }

    // Returns the server, or null if packet type is not SRV_COOKIE
    public String getServer() {
        if (this.getType() == DisconnectPacket.TYPE_SRV_COOKIE) {
            return this.server;
        } else {
            return null;
        }
    }

    // Returns the cookie, or null if packet type is not SRV_COOKIE
    public byte[] getCookie() {
        if (DisconnectPacket.TYPE_SRV_COOKIE == this.getType()) {
            byte[] result = new byte[this.cookie.length];
            System.arraycopy(this.cookie, 0, result, 0, this.cookie.length);
            return result;
        } else {
            return null;
        }
    }

    // Returns the error as an error code, or -1 if packet type is not SRV_GOODBYE
    public int getError() {
        return (TYPE_SRV_GOODBYE == getType()) ? error : -1;
    }

    // Returns the package as byte array
    public byte[] toByteArray() {

        Util buf = new Util();

        buf.writeZeroes(6);

        // Assemble SRV_COOKIE
        if (this.getType() == DisconnectPacket.TYPE_SRV_COOKIE) {

            // DISCONNECT.UIN
            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));

            // DISCONNECT.SERVER
            buf.writeTLV(0x0005, StringConvertor.stringToByteArray(this.server));

            // DISCONNECT.COOKIE
            buf.writeTLV(0x0006, this.cookie);

        } // Assemble SRV_GOODBYE
        else if (this.getType() == DisconnectPacket.TYPE_SRV_GOODBYE) {

            // DISCONNECT.UIN
            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));

            // DISCONNECT.DESCRIPTION
            buf.writeTLV(0x0004, StringConvertor.stringToByteArray(description));

            // DISCONNECT.ERROR
            buf.writeTLVWord(0x0008, error);
        }

        // Allocate memory
        byte[] _buf = buf.toByteArray();
        assembleFlapHeader(_buf, 0x04);
        return _buf;
    }

    // Parses given byte array and returns a Packet object
    public static Packet parse(byte[] flapData) throws JimmException {
        // Get length of FLAP data
        //int flapLength = Util.getWordBE(buf, offset + 4);

        // Variables for all possible TLVs
        String uin = null;
        String server = null;
        byte[] cookie = null;
        int error = -1;
        String description = null;

        // Read all TLVs
        ArrayReader marker = new ArrayReader(flapData, 0);
        while (marker.isNotEnd()) {
            int tlvType = marker.getTlvType();
            byte[] tlvValue = marker.getTlv();
            if (null == tlvValue) {
                throw new JimmException(135, 0);
            }

            // Save value
            switch (tlvType) {
                case 0x0001:   // uin
                    uin = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0005:   // server
                    server = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0006:   // cookie
                    cookie = tlvValue;
                    break;
                case 0x0008:   // error
                case 0x0009:   // error
                    error = Util.getWordBE(tlvValue, 0);
                    break;
                case 0x0004:   // description
                case 0x000B:   // description
                    description = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                default:
                    // Do nothing on default (ignore all unknown TLVs)
            }

        }

        // CLI_GOODBYE
        if ((uin == null) && (server == null) && (cookie == null) && (error == -1) && (description == null)) {
            return new DisconnectPacket();

            // SRV_COOKIE
        } else if ((uin != null) && (server != null) && (cookie != null) && (error == -1) && (description == null)) {
            return new DisconnectPacket(uin, server, cookie);

            // SRV_GOODBYYE
        } else if ((server == null) && (cookie == null) && (error != -1) && (description != null)) {
            return new DisconnectPacket(error, description);

        } else {
            // Other TLV combinations are not valid
            throw new JimmException(135, 2);
        }

    }
}
// #sijapp cond.end #
