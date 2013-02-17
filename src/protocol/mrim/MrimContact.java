/*
 * MrimContact.java
 *
 * Created on 7 Март 2008 г., 18:30
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_MRIM is "true" #
package protocol.mrim;

import DrawControls.icons.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.menu.*;
import protocol.*;

/**
 *
 * @author vladimir
 */
public class MrimContact extends Contact {
    private int contactId;
    private int flags;
    private String phones;
    public static final int CONTACT_INTFLAG_NOT_AUTHORIZED = 0x0001;

    public static final int USER_MENU_SEND_SMS = 1;

    public static final int CONTACT_FLAG_INVISIBLE = 0x04;
    public static final int CONTACT_FLAG_VISIBLE   = 0x08;
    public static final int CONTACT_FLAG_IGNORE    = 0x10;

    public void init(int contactId, String name, String phone, int groupId, int serverFlags, int flags) {
        setContactId(contactId);
        setName(name.length() > 0 ? name : userId);
        setGroupId(groupId);
        setFlags(flags);
        this.phones = phone;
        setBooleanValue(Contact.CONTACT_NO_AUTH, (CONTACT_INTFLAG_NOT_AUTHORIZED & serverFlags) != 0);
        setTempFlag(false);
        setOfflineStatus();
    }
    final void setFlags(int flags) {
        this.flags = flags;
        setBooleanValue(SL_VISIBLE, (flags & CONTACT_FLAG_VISIBLE) != 0);
        setBooleanValue(SL_INVISIBLE, (flags & CONTACT_FLAG_INVISIBLE) != 0);
        setBooleanValue(SL_IGNORE, (flags & CONTACT_FLAG_IGNORE) != 0);
    }
    //private static int NOT_IN_GROUP = 666;
    public MrimContact(String uin, String name) {
        this.userId = uin;
        contactId = -1;
        setFlags(0);
        setGroupId(Group.NOT_IN_GROUP);
        this.setName(name);
        setOfflineStatus();
    }
    void setContactId(int id) {
        contactId = id;
    }
    int getContactId() {
        return contactId;
    }

    int getFlags() {
        return flags;
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void setClient(String cl) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("client " + userId + " " + cl);
        // #sijapp cond.end#
        MrimClient.createClient(this, cl);
    }
    // #sijapp cond.end #
    /////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void getLeftIcons(Icon[] leftIcons) {
        super.getLeftIcons(leftIcons);
        if (!isTyping() && !hasUnreadMessage()) {
            Icon x = leftIcons[1];
            if (null != x) {
                leftIcons[0] = x;
                leftIcons[1] = null;
            }
        }
    }
    // #sijapp cond.end #
    /////////////////////////////////////////////////////////////////////////

    public void addChatMenuItems(MenuModel model) {
        if (isOnline() && Options.getBoolean(Options.OPTION_ALARM)) {
            model.addItem("wake", USER_MENU_WAKE);
        }
    }
    protected void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        addChatItems(contactMenu);
        if (!StringConvertor.isEmpty(phones)) {
            contactMenu.addItem("send_sms", USER_MENU_SEND_SMS);
        }
        addGeneralItems(protocol, contactMenu);
    }
    protected void initManageContactMenu(Protocol protocol, MenuModel menu) {
        if (protocol.isConnected()) {
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            initPrivacyMenu(menu);
            // #sijapp cond.end #
            if (isTemp()) {
                menu.addItem("add_user", USER_MENU_ADD_USER);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.addItem("move_to_group", USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addItem("requauth", USER_MENU_REQU_AUTH);
                }
                menu.addItem("rename", USER_MENU_RENAME);
            }
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            menu.addSeparator();
            menu.addItem("remove", USER_MENU_USER_REMOVE);
        }
    }

    /////////////////////////////////////////////////////////////////////////


    public void setMood(String moodCode, String title, String desc) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (!StringConvertor.isEmpty(moodCode)) {
            DebugLog.println("mrim: mood " + getUserId() + " " + moodCode + " " + title);
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_XSTATUSES is "true" #
        String message = StringConvertor.trim(title + " " + desc);
        int x = Mrim.xStatus.createStatus(moodCode);

        setXStatus(x, message);
        if (XStatusInfo.XSTATUS_NONE == x) {
            setStatus(getStatusIndex(), message);
        }
        // #sijapp cond.end#
    }

    String getPhones() {
        return phones;
    }
    void setPhones(String listOfPhones) {
        phones = listOfPhones;
    }
}
// #sijapp cond.end #