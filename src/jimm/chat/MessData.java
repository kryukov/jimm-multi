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
File: src/jimm/ChatHistory.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

/*
 * MessData.java
 *
 * Created on 19 Апрель 2007 г., 15:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package jimm.chat;

import DrawControls.text.Par;
import jimm.Jimm;
import jimm.comm.Util;

public final class MessData {
    private long time;
    private String text;
    private String nick;
    String strTime;
    public int iconIndex;
    public Par par;
    private short rowData;

    public static final short URLS = 1;
    public static final short INCOMING = 2;
    public static final short ME = 4;
    public static final short PROGRESS = 8;
    public static final short SERVICE = 16;
    public static final short MARKED = 32;

    public MessData(long time, String text, String nick, short flags, int iconIndex, Par par) {
        init(time, text, nick, flags, iconIndex);
        this.par = par;
    }
    public void init(long time, String text, String nick, short flags, int iconIndex) {
        this.text = text;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        this.iconIndex = iconIndex;
        boolean today = (Jimm.getCurrentGmtTime() - 24 * 60 * 60 < time);
        strTime = Util.getLocalDateString(time, today);
    }

    public long getTime() {
        return time;
    }

    public String getNick() {
        return nick;
    }

    public String getText() {
        return text;
    }

    public boolean isIncoming() {
        return (rowData & INCOMING) != 0;
    }

    public boolean isURL() {
        return (rowData & URLS) != 0;
    }

    public boolean isMe() {
        return (rowData & ME) != 0;
    }

    public boolean isFile() {
        return (rowData & PROGRESS) != 0;
    }

    public boolean isMarked() {
        return (rowData & MARKED) != 0;
    }
    public void setMarked(boolean marked) {
        rowData = (short) (marked ? (rowData | MARKED) : (rowData & ~MARKED));
    }

    public boolean isService() {
        return (rowData & SERVICE) != 0;
    }
}
