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
File: src/jimm/comm/Systemjava
Version: ###VERSION###  Date: ###DATE###
Author(s): Andreas Rossbacher
 *******************************************************************************/
package jimm.chat.message;

import jimm.*;
import jimm.comm.*;
import jimm.util.JLocale;
import protocol.Protocol;

public class SystemNotice extends Message {
    /****************************************************************************/
    // Type of the note
    private int sysnotetype;
    // What was the reason
    private String reason;

    // Constructs system notice
    public SystemNotice(Protocol protocol, int _sysnotetype, String _uin, String _reason) {
        super(Jimm.getCurrentGmtTime(), protocol, _uin, true);
        sysnotetype = _sysnotetype;
        reason = StringUtils.notNull(_reason);
        setName(JLocale.getString("sysnotice"));
    }

    // Get Sysnotetype
    public int getMessageType() {
        return sysnotetype;
    }

    public String getText() {
        if (TYPE_FILE == getMessageType()) {
            return reason;
        }
        String text = "";
        if (TYPE_NOTICE_MESSAGE == getMessageType()) {
            return "* " + reason;

        }
        if (TYPE_NOTICE_ERROR == getMessageType()) {
            return reason;
        }
        if (TYPE_NOTICE_AUTHREQ == getMessageType()) {
            text = getSndrUin() + JLocale.getString("wantsyourauth");
        }
        if (StringUtils.isEmpty(text)) {
            return reason;
        }
        text += ".";
        if (!StringUtils.isEmpty(reason)) {
            text += "\n" + JLocale.getString("reason") + ": " + reason;
        }
        return text;
    }

}
