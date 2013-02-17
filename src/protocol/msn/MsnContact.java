/* JIMMY - Instant Mobile Messenger
   Copyright (C) 2006  JIMMY Project
 
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
 **********************************************************************
 File: jimmy/MsnContact.java
 Version: pre-alpha  Date: 2006/05/12
 Author(s): Matevz Jekovec, Zoran Mesec
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import DrawControls.icons.Icon;
import jimm.JimmUI;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.ui.menu.*;
import protocol.*;

/**
 * Class MsnContact represents a single user contact.
 * 
 * @author Matevz Jekovec
 */
public class MsnContact extends Contact {
    private String userID_; //user ID
    private String screenName_; //user's nick/screen name
    private String groupName_; //group which this contact is part of*/
    
    private String userHash;
    private short lists;
    
    //contact possible status follow
    /*public static final byte ST_OFFLINE = 0;
    public static final byte ST_ONLINE = 1;
    public static final byte ST_AWAY = 2;
    public static final byte ST_BUSY = 3;*/
    
    /**
     * Create a new contact.
     *
     * @param userID User ID on the server (usually the e-mail address)
     * @param protocol Reference to the corresponding protocol
     * @param status User status - online, offline, away, busy etc.
     * @param groupName Group's name which the user belongs to
     * @param screenName User's nick or the name shown on the display
     */
    public MsnContact(String userID, String name) {
        this.userId = userID;
        setGroupId(Group.NOT_IN_GROUP);
        this.setName(name);
        setOfflineStatus();
    }
    
    /**
     * Sets a new user hash
     *
     * @param hash hash
     */
    public void setUserHash(String hash) {
        this.userHash = hash;
    }
    /**
     * Get the user hash
     *
     * @return
     */
    public String getUserHash() {
        return userHash;
    }
    
    /**
     * Sets a number for lists
     *
     * @param short list
     */
    public void setLists(short list) {
        this.lists = list;
    }
    
    /**
     * Get the user's lists number
     *
     * @return
     */
    public short getLists() {return this.lists;}
            

    
    //contact possible status follow
    public static final byte ST_OFFLINE = 0;
    public static final byte ST_ONLINE = 1;
    public static final byte ST_AWAY = 2;
    public static final byte ST_BUSY = 3;
    
    /////////////////////////////////////////////////////////////////
    public static final int CONVERSATION_DISCONNECT = 5;
    protected void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        if (protocol.isConnected()) {
            addChatItems(contactMenu);
            if (hasChat()) {
                contactMenu.addItem("leave_chat", CONVERSATION_DISCONNECT);
            }
        }
    }
    protected void initManageContactMenu(Protocol protocol, MenuModel menu) {
    }
}
// #sijapp cond.end #