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
File: src/jimm/comm/Packet.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.packet;

import jimm.comm.*;

public abstract class Packet {

    protected final void assembleFlapHeader(byte[] buf, int channel) {
        Util.putByte(buf, 0, 0x2a);
        Util.putByte(buf, 1, channel);
        Util.putWordBE(buf, 2, 0 /* stub */);
        Util.putWordBE(buf, 4, buf.length - 6);
    }

    // Returns the package as byte array
    public abstract byte[] toByteArray();
}
// #sijapp cond.end #
