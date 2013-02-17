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
File: src/jimm/comm/PlainMessage.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/
package jimm.chat.message;

import jimm.comm.*;
import jimm.util.JLocale;
import protocol.Contact;
import protocol.Protocol;

public final class PlainMessage extends Message {

    private String text;
    private int messageId;
    private boolean offline;
    public static final String CMD_WAKEUP = "/wakeup";
    public static final String CMD_ME = "/me ";
    // unicode message (max len / sizeof char)
    public static final int MESSAGE_LIMIT = 1024;

    // Constructs an incoming message
    public PlainMessage(String contactUin, Protocol protocol, long date, String text, boolean offline) {
        super(date, protocol, contactUin, true);
        if ('\n' == text.charAt(0)) {
            text = text.substring(1);
        }
        this.text = text;
        this.offline = offline;
    }

    // Constructs an outgoing message
    public PlainMessage(Protocol protocol, Contact rcvr, long date, String text) {
        super(date, protocol, rcvr, false);
        this.text = StringConvertor.notNull(text);
        this.offline = false;
    }

    public boolean isOffline() {
        return offline;
    }

    // Returns the message text
    public String getText() {
        return text;
    }

    public String getProcessedText() {
        String messageText = text;
        if (isWakeUp()) {
            if (isIncoming()) {
                messageText = PlainMessage.CMD_ME + JLocale.getString("wake_you_up");
            } else {
                messageText = PlainMessage.CMD_ME + JLocale.getString("wake_up");
            }
        }
        return messageText;
    }

    public boolean isWakeUp() {
        return text.startsWith(PlainMessage.CMD_WAKEUP)
                && getRcvr().isSingleUserContact();
    }

    public void setMessageId(int id) {
        messageId = id;
    }

    public int getMessageId() {
        return messageId;
    }
}
