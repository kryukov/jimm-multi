/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-04  Jimm Project

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
File: src/jimm/comm/Action.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Vladimir Kryukov
 *******************************************************************************/
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;

import jimm.Jimm;
import jimm.JimmException;
import protocol.icq.packet.*;
import protocol.icq.*;

public abstract class IcqAction {
    private IcqNetWorking connection;
    private long lastActivity;

    protected final void active() {
        lastActivity = Jimm.getCurrentGmtTime();
    }
    protected final boolean isNotActive(long timeout) {
        return lastActivity + timeout < Jimm.getCurrentGmtTime();
    }

    // Returns true if the action is completed
    public abstract boolean isCompleted();

    // Returns true if an error has occured
    public abstract boolean isError();

    public final void setConnection(IcqNetWorking connection) {
        this.connection = connection;
    }

    protected final Icq getIcq() {
        return connection.getIcq();
    }

    protected final IcqNetWorking getConnection() {
        return connection;
    }

    protected final void sendPacket(Packet packet) throws JimmException {
        connection.sendPacket(packet);
        active();
    }

    public abstract void init() throws JimmException;
    // Forwards received packet, returns true if packet was consumed

    public abstract boolean forward(Packet packet) throws JimmException;
}
// #sijapp cond.end #

