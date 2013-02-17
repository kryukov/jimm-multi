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
File: src/jimm/comm/ToIcqSrvPacket.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.packet;

import jimm.comm.*;

public final class ToIcqSrvPacket extends SnacPacket {

    // CLI_REQOFFLINEMSGS packet subcommand
    public static final int CLI_REQOFFLINEMSGS_SUBCMD = 0x003C;
    // CLI_ACKOFFLINEMSGS packet subcommand
    public static final int CLI_ACKOFFLINEMSGS_SUBCMD = 0x003E;
    // CLI_META packet subcommand and types
    public static final int CLI_META_SUBCMD = 0x07D0;
    public static final int CLI_META_REQINFO_TYPE = 0x04D0;   // doesn't work
    public static final int CLI_META_REQMOREINFO_TYPE = 0x04B2;
    public static final int CLI_META_REQUEST_FULL_INFO = 0x0FA0;
    public static final int CLI_SET_FULLINFO = 0x0C3A;
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
    public ToIcqSrvPacket(long reference, int snacFlags, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        super(SnacPacket.OLD_ICQ_FAMILY, SnacPacket.CLI_TOICQSRV_COMMAND, snacFlags, reference, extData, data);
        this.icqSequence = icqSequence;
        this.uin = uin;
        this.subcommand = subcommand;
    }

    // Constructor
    public ToIcqSrvPacket(long reference, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        this(reference, 0, icqSequence, uin, subcommand, extData, data);
    }

    // Constructor
    public ToIcqSrvPacket(long reference, String uin, int subcommand, byte[] extData, byte[] data) {
        this(reference, 0, -1, uin, subcommand, extData, data);
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
        Util buf = new Util();
        buf.writeZeroes(16);
        // Assemlbe SNAC.DATA
        if (extData.length > 0) {
            buf.writeWordBE(extData.length);
            buf.writeByteArray(extData);
        }
        buf.writeWordBE(0x0001);
        buf.writeWordBE(10 + data.length);
        buf.writeWordLE(8 + data.length);       // CLI_TOICQSRV.LENGTH in Little Endian
        buf.writeDWordLE(Long.parseLong(uin));  // CLI_TOICQSRV.UIN in Little Endian
        buf.writeWordLE(subcommand);            // CLI_TOICQSRV.SUBCOMMAND in Little Endian
        buf.writeWordLE(icqSequence);           // CLI_TOICQSRV.SEQUENCE in Little Endian
        buf.writeByteArray(data);               // CLI_TOICQSRV.DATA in Little Endian

        byte _buf[] = buf.toByteArray();
        assembleFlapHeader(_buf, 0x02);
        assembleSnacHeader(_buf);
        return _buf;
    }
}
// #sijapp cond.end #
