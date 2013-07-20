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
 File: src/jimm/comm/Message.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm.chat.message;

import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.view.icons.*;
import jimmui.view.text.*;
import jimmui.model.chat.MessData;
import protocol.Contact;
import protocol.Protocol;
import protocol.ui.InfoFactory;

public abstract class Message {
    // Types of system messages
    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_NOTICE_AUTHREQ = 1;
    public static final int TYPE_NOTICE_ERROR = 2;
    public static final int TYPE_NOTICE_MESSAGE = 3;
    public static final int TYPE_FILE = 4;

    public static final int ICON_NONE = -1;
    public static final int ICON_SYSREQ = 0;
    public static final int ICON_SYS_OK = 1;
    public static final int ICON_TYPE = 2;
    public static final int ICON_IN_MSG_HI = 3;
    public static final int ICON_IN_MSG = 4;
    public static final int ICON_OUT_MSG = 5;
    public static final int ICON_OUT_MSG_FROM_SERVER = 6;
    public static final int ICON_OUT_MSG_FROM_CLIENT = 7;
    //public static final int ICON_ERROR = 6;

    public static final int NOTIFY_OFF = -1;
    public static final int NOTIFY_NONE = ICON_OUT_MSG;
    public static final int NOTIFY_FROM_SERVER = ICON_OUT_MSG_FROM_SERVER;
    public static final int NOTIFY_FROM_CLIENT = ICON_OUT_MSG_FROM_CLIENT;


    protected boolean isIncoming;
    protected String contactId;
    protected Contact contact;
    protected Protocol protocol;
    private String senderName;
    protected Par par = null;
    private MessData mData = null;
    private long newDate; // local time

    protected Message(long date, Protocol protocol, String contactId, boolean isIncoming) {
    	this.newDate = date;
    	this.protocol = protocol;
    	this.contactId = contactId;
        this.isIncoming = isIncoming;
    }
    protected Message(long date, Protocol protocol, Contact contact, boolean isIncoming) {
    	this.newDate = date;
    	this.protocol = protocol;
    	this.contact = contact;
        this.isIncoming = isIncoming;
    }

    public final void setVisibleIcon(Par listItem, MessData mData) {
        this.par = listItem;
        this.mData = mData;
    }
    public final void setSendingState(int state) {
        if (mData.isMe()) {
            Icon icon = InfoFactory.msgIcons.iconAt(state);
            if ((null != par) && (null != icon)) {
                par.replaceFirstIcon(icon);
            }
        } else {
            mData.iconIndex = state;
        }
        Contact rcvr = getRcvr();
        if (rcvr.hasChat()) {
            ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(rcvr);
            if (null != chat) {
                Jimm.getJimm().getChatUpdater().invalidate(chat);
            }
        }
    }
    public final void setName(String name) {
        senderName = name;
    }
    private String getContactUin() {
        return (null == contact) ? contactId : contact.getUserId();
    }
    // Returns the senders UIN
    public final String getSndrUin() {
        return isIncoming ? getContactUin() : protocol.getUserId();
    }

    // Returns the receivers UIN
    public final String getRcvrUin() {
        return isIncoming ? protocol.getUserId() : getContactUin();
    }
    public final boolean isIncoming() {
        return isIncoming;
    }

    // Returns the receiver
    protected final Contact getRcvr() {
        return (null == contact) ? protocol.getItemByUID(contactId) : contact;
    }

    public boolean isOffline() {
    	return false;
    }

    public final long getNewDate() {
    	return newDate;
    }

    public String getName() {
        return senderName;
    }

    public abstract String getText();
    public String getProcessedText() {
        return getText();
    }
    public boolean isWakeUp() {
        return false;
    }

    public int getMessageType() {
        return SystemNotice.TYPE_MESSAGE;
    }
}