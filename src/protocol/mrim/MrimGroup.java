/*
 * MrimGroup.java
 *
 * Created on 28 Март 2008 г., 22:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.mrim;
// #sijapp cond.if protocols_MRIM is "true" #
import protocol.Group;

/**
 *
 * @author vladimir
 */
public class MrimGroup extends Group {
    private int flags;
    private static final int OTHER_GROUP = 0;
    public static final int PHONE_CONTACTS_GROUP = 103;

    public MrimGroup(int groupId, int flags, String name) {
        super(name);
        setGroupId(groupId);
        setFlags(flags);
        setMode(MODE_FULL_ACCESS);
    }
    public int getFlags() {
        return flags;
    }
    public final void setFlags(int f) {
        flags = f;
    }
    public final void setGroupId(int groupId) {
        int mode = MODE_FULL_ACCESS;
        if (OTHER_GROUP == groupId) {
            mode &= ~Group.MODE_REMOVABLE;
            mode |= Group.MODE_BOTTOM;

        } else if (PHONE_CONTACTS_GROUP == groupId) {
            mode &= ~Group.MODE_EDITABLE;
            mode &= ~Group.MODE_NEW_CONTACTS;
            mode |= Group.MODE_BOTTOM;
        }
        setMode(mode);
        super.setGroupId(groupId);
    }
    
}

// #sijapp cond.end #
