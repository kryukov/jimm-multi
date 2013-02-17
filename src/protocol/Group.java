/*
 * Group.java
 *
 * Created on 14 Май 2008 г., 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.tree.*;
import DrawControls.icons.Icon;
import java.util.Vector;
import jimm.chat.ChatHistory;
import jimm.comm.Sortable;
import jimm.comm.Util;

/**
 *
 * @author vladimir
 */
public class Group extends TreeBranch implements Sortable {
    private String name;
    private final Vector contacts = new Vector();
    private byte mode;
    private String caption = null;
    private int groupId;

    public static final int NOT_IN_GROUP = -1;

    public static final byte MODE_NONE         = 0x00;
    public static final byte MODE_REMOVABLE    = 0x01;
    public static final byte MODE_EDITABLE     = 0x02;
    public static final byte MODE_NEW_CONTACTS = 0x04;
    public static final byte MODE_FULL_ACCESS  = 0x0F;

    public static final byte MODE_TOP          = 0x10;
    public static final byte MODE_BOTTOM       = 0x20;
    public static final byte MODE_BOTTOM2      = 0x40;

    /** Creates a new instance of Group */
    public Group(String name) {
        setName(name);
        caption = name;
        setMode(Group.MODE_FULL_ACCESS);
    }

    // Returns the group item name
    public final String getName() {
        return this.name;
    }

    // Sets the group item name
    public final void setName(String name) {
        this.name = name;
    }

    public final void setMode(int newMode) {
        mode = (byte)newMode;
    }
    public final boolean hasMode(byte type) {
        return (mode & type) != 0;
    }

    public int getNodeWeight() {
        if (hasMode(MODE_TOP)) return -4;
        if (hasMode(MODE_BOTTOM)) return -2;
        if (hasMode(MODE_BOTTOM2)) return -1;
        //if (!hasMode(MODE_EDITABLE)) return -2;
        //if (!hasMode(MODE_REMOVABLE)) return -1;
        return -3;
    }

    public final int getId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public final void getLeftIcons(Icon[] icons) {
    }

    public final void getRightIcons(Icon[] rightIcons) {
        if (isExpanded()) {
            return;
        }
        rightIcons[0] = ChatHistory.instance.getUnreadMessageIcon(getContacts());
    }

    public boolean isEmpty() {
        return (0 == contacts.size());
    }
    final void addContact(Contact c) {
        contacts.addElement(c);
    }
    final boolean removeContact(Contact c) {
        return contacts.removeElement(c);
    }

    public final Vector getContacts() {
        return contacts;
    }

    // Calculates online/total values for group
    public final void updateGroupData() {
        int onlineCount = 0;
        int total = contacts.size();
        for (int i = 0; i < total; ++i) {
            Contact item = (Contact)contacts.elementAt(i);
            if (item.isOnline()) {
                onlineCount++;
            }
        }
        caption = getName();
        if (0 < total) {
            caption += " (" + onlineCount + "/" + total + ")";
        }
    }
    public final String getText() {
        return caption;
    }
    public final void sort() {
        if (isExpanded()) {
            Util.sort(contacts);
        }
    }
}

