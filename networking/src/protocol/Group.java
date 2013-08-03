/*
 * Group.java
 *
 * Created on 14 Май 2008 г., 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimmui.view.roster.items.GroupBranch;

import java.util.Vector;

/**
 *
 * @author vladimir
 */
public class Group {//extends GroupBranch {
    public static final byte MODE_NONE         = 0x00;
    public static final byte MODE_REMOVABLE    = 0x01;
    public static final byte MODE_EDITABLE     = 0x02;
    public static final byte MODE_NEW_CONTACTS = 0x04;
    public static final byte MODE_FULL_ACCESS  = 0x0F;

    public static final byte MODE_TOP          = GroupBranch.MODE_TOP;
    public static final byte MODE_BOTTOM       = GroupBranch.MODE_BOTTOM;

    private String name;
    private int groupId;
    private byte mode;

    public static final int NOT_IN_GROUP = -1;

    /** Creates a new instance of Group */
    public Group(String name) {
        setName(name);
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

    public final int getId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public final void setMode(int newMode) {
        mode = (byte)newMode;
    }
    public final byte getMode() {
        return mode;
    }
    public final boolean hasMode(byte type) {
        return (mode & type) != 0;
    }

    public boolean isEmpty(Protocol p) {
        Vector contacts = p.getContactItems();
        for (int i = 0; i < contacts.size(); ++i) {
            if (((Contact) contacts.elementAt(i)).getGroupId() == groupId) {
                return false;
            }
        }
        return true;
    }
}

