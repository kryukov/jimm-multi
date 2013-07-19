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
File: src/jimm/comm/FromIcqSrvPacket.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.packet;

import jimm.*;
import jimm.comm.*;

public final class FromIcqSrvPacket extends SnacPacket {

    // SRV_OFFLINEMSG packet subcommand
    public static final int SRV_OFFLINEMSG_SUBCMD = 0x0041;
    // SRV_DONEOFFLINEMSGS packet subcommand
    public static final int SRV_DONEOFFLINEMSGS_SUBCMD = 0x0042;
    // SRV_META packet subcommand and types
    public static final int SRV_META_SUBCMD = 0x07DA;
    public static final int SRV_META_GENERAL_TYPE = 0x00C8;
    public static final int META_SET_FULLINFO_ACK = 0x0C3F;
    public static final int SRV_META_FULL_INFO = 0x0fb4;
    /****************************************************************************/
    /****************************************************************************/
    /****************************************************************************/
    // ICQ sequence number
    private int icqSequence;
    // UIN
    private String uin;
    // Subcommand
    private int subcommand;

    // Constructor
    public FromIcqSrvPacket(long reference, int snacFlags, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        super(SnacPacket.OLD_ICQ_FAMILY, SnacPacket.SRV_FROMICQSRV_COMMAND, snacFlags, reference, extData, data);
        this.icqSequence = icqSequence;
        this.uin = uin;
        this.subcommand = subcommand;
    }

    // Returns the ICQ sequence number
    public final int getIcqSequence() {
        return this.icqSequence;
    }

    // Sets the ICQ sequence number
    public final void setIcqSequence(int icqSequence) {
        this.icqSequence = icqSequence;
    }

    // Returns the subcommand
    public final int getSubcommand() {
        return this.subcommand;
    }

    // Returns the package as byte array
    public byte[] toByteArray() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        jimm.modules.DebugLog.panic("Unsupported operation (FromIcqSrvPacket.toByteArray()");
        // #sijapp cond.end #
        return null;
    }

    // Parses given byte array and returns a FromIcqSrvPacket object
    public static Packet parse(byte[] flapData) throws JimmException {
        ArrayReader reader = new ArrayReader(flapData, 4);
        // Get SNAC flags
        int snacFlags = reader.getWordBE();

        // Get SNAC reference
        long snacReference = reader.getDWordBE();

        // Get data and extra data (if available)
        byte[] extData;
        String uin;
        int subcommand;
        int icqSequence;
        if (snacFlags == 0x8000) {
            // Get length of extra data
            int extDataLength = reader.getWordBE();

            // Get extra data
            extData = reader.getArray(extDataLength);

        } else {
            extData = emptyArray;
        }

        reader.skip(4); // type, length
        int dataLength = reader.getWordLE() - (4 + 2 + 2);
        // Get uin, subcommand and icq sequence number
        uin = String.valueOf(reader.getDWordLE());
        subcommand = reader.getWordLE();
        icqSequence = reader.getWordLE();

        // Get data
        byte[] data = reader.getArray(dataLength);

        // Instantiate FromIcqSrvPacket
        return new FromIcqSrvPacket(snacReference, snacFlags, icqSequence, uin, subcommand, extData, data);
    }
}
// #sijapp cond.end #
