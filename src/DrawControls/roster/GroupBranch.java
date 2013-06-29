package DrawControls.roster;

import DrawControls.icons.Icon;
import jimm.chat.ChatHistory;
import jimm.comm.Sortable;
import protocol.Contact;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 12.06.13 10:46
 *
 * @author vladimir
 */
public class GroupBranch extends TreeBranch implements Sortable {
    public static final byte MODE_TOP          = 0x10;
    public static final byte MODE_BOTTOM       = 0x20;
    public static final byte MODE_BOTTOM2      = 0x40;

    private String caption = null;
    private String name;
    private byte mode;

    public GroupBranch(String name) {
        setName(name);
    }

    // Calculates online/total values for group
    public final void updateGroupData() {
        int onlineCount = 0;
        int total = items.size();
        for (int i = 0; i < total; ++i) {
            Contact item = (Contact)items.elementAt(i);
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

    public final void setMode(int newMode) {
        mode = (byte)newMode;
    }
    public final byte getMode() {
        return mode;
    }
    public final boolean hasMode(byte type) {
        return (mode & type) != 0;
    }

    // Returns the group item name
    public String getName() {
        return this.name;
    }

    // Sets the group item name
    public void setName(String name) {
        this.name = name;
    }

    public final int getNodeWeight() {
        if (hasMode(MODE_TOP)) return -4;
        if (hasMode(MODE_BOTTOM)) return -2;
        if (hasMode(MODE_BOTTOM2)) return -1;
        //if (!hasMode(MODE_EDITABLE)) return -2;
        //if (!hasMode(MODE_REMOVABLE)) return -1;
        return -3;
    }

    public final void getLeftIcons(Icon[] icons) {
    }

    public final void getRightIcons(Icon[] rightIcons) {
        if (isExpanded()) {
            return;
        }
        rightIcons[0] = ChatHistory.instance.getUnreadMessageIcon(getContacts());
    }


    public final Vector<Contact> getContacts() {
        return items;
    }

    public final boolean isEmpty() {
        return (0 == items.size());
    }
}
