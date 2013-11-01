/*
 * Contact.java
 *
 * Created on 13 Май 2008 г., 15:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimmui.model.chat.ChatModel;
import jimmui.view.icons.*;
import jimm.*;
import jimm.chat.message.*;
import jimm.cl.ContactList;
import jimm.history.*;
import jimmui.view.menu.*;
import jimm.comm.*;
import jimmui.view.roster.items.TreeNode;
import protocol.ui.ClientInfo;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.ui.XStatusInfo;

/**
 *
 * @author vladimir
 */
abstract public class Contact implements TreeNode, Sortable {
    protected String userId;
    private String name;
    private int groupId = Group.NOT_IN_GROUP;
    private int booleanValues;
    private byte status = StatusInfo.STATUS_OFFLINE;
    private String statusText = null;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private int xstatus = XStatusInfo.XSTATUS_NONE;
    private String xstatusText = null;
    // #sijapp cond.end#
    // #sijapp cond.if modules_CLIENTS is "true" #
    public short clientIndex = ClientInfo.CLI_NONE;
    public String version = "";
    // #sijapp cond.end #
    public long chaingingStatusTime = 0;


    public final boolean isOnline() {
        return (StatusInfo.STATUS_OFFLINE != status);
    }
    public void setTimeOfChaingingStatus(long time) {
        chaingingStatusTime = time;
    }

    public final String getUserId() {
        return userId;
    }

    public final String getName() {
        return name;
    }
    public void setName(String newName) {
        if (!StringUtils.isEmpty(newName)) {
    	    name = newName;
        }
    }
    public final void setGroupId(int id) {
        groupId = id;
    }
    public final int getGroupId() {
        return groupId;
    }
    public final void setGroup(Group group) {
        setGroupId((null == group) ? Group.NOT_IN_GROUP : group.getId());
    }
    public String getDefaultGroupName() {
        return null;
    }
    private Protocol getProtocol() {
        return Jimm.getJimm().jimmModel.getProtocol(this);
    }

///////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public final void setXStatus(int index, String text) {
        xstatus = index;
        xstatusText = (XStatusInfo.XSTATUS_NONE == index) ? null : text;
    }
    public final int getXStatusIndex() {
        return xstatus;
    }
    public final String getXStatusText() {
        return xstatusText;
    }
    // #sijapp cond.end#
///////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_CLIENTS is "true" #
    public void setClient(short clientNum, String ver) {
        clientIndex = clientNum;
        version = StringUtils.notNull(ver);
    }
    // #sijapp cond.end #
///////////////////////////////////////////////////////////////////////////
    public void setOfflineStatus() {
        if (isOnline()) {
            setTimeOfChaingingStatus(Jimm.getCurrentGmtTime());
        }
        setStatus(StatusInfo.STATUS_OFFLINE, null);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        setXStatus(XStatusInfo.XSTATUS_NONE, null);
        // #sijapp cond.end #
        beginTyping(false);
    }
    public final byte getStatusIndex() {
        return status;
    }
    public final String getStatusText() {
        return statusText;
    }
    protected final void setStatus(byte statusIndex, String text) {
        if (!isOnline() && (StatusInfo.STATUS_OFFLINE != statusIndex)) {
            setTimeOfChaingingStatus(Jimm.getCurrentGmtTime());
        }
        status = statusIndex;
        statusText = (StatusInfo.STATUS_OFFLINE == status) ? null : text;
    }

    ///////////////////////////////////////////////////////////////////////////
    /* Activates the contact item menu */
    public void activate(Protocol p) {
        Jimm.getJimm().getCL().getUpdater().setCurrentContact(this);

        ChatModel chat = p.getChatModel(this);
        if (hasChat()) {
            Jimm.getJimm().getChatUpdater().activate(chat);
        } else {
            Jimm.getJimm().getChatUpdater().writeMessage(chat, null);
        }
    }
///////////////////////////////////////////////////////////////////////////
    // 00000000 | messageIconIndex | 00000000 | 00000000

    public static final byte CONTACT_NO_AUTH       = 1 << 1; /* Boolean */
    private static final byte CONTACT_IS_TEMP      = 1 << 3; /* Boolean */
    //public static final byte B_AUTOANSWER          = 1 << 2; /* Boolean */
    public static final byte SL_VISIBLE            = 1 << 4; /* Boolean */
    public static final byte SL_INVISIBLE          = 1 << 5; /* Boolean */
    public static final byte SL_IGNORE             = 1 << 6; /* Boolean */

    private static final int TYPING                = 1 << 8; /* Boolean */
    private static final int HAS_CHAT              = 1 << 9; /* Boolean */

    public final void setBooleanValue(byte key, boolean value) {
        if (value) {
            booleanValues |= key;
        } else {
            booleanValues &= ~key;
        }
    }
    public final boolean isTemp() {
        return (booleanValues & CONTACT_IS_TEMP) != 0;
    }
    public final boolean isAuth() {
        return (booleanValues & CONTACT_NO_AUTH) == 0;
    }
    public final void setBooleanValues(byte vals) {
        booleanValues = (booleanValues & ~0xFF) | (vals & 0x7F);
    }
    public final byte getBooleanValues() {
        return (byte)(booleanValues & 0x7F);
    }
    public final void setTempFlag(boolean isTemp) {
        setBooleanValue(Contact.CONTACT_IS_TEMP, isTemp);
    }
    public final void beginTyping(boolean typing) {
        if (typing && isOnline()) {
            booleanValues |= TYPING;
        } else {
            booleanValues &= ~TYPING;
        }
    }
    public final boolean isTyping() {
        return (booleanValues & TYPING) != 0;
    }
    public final boolean hasChat() {
        return (booleanValues & HAS_CHAT) != 0;
    }

    public final void updateChatState(ChatModel chat) {
        int icon = -1;
        if (null != chat) {
            icon = chat.getNewMessageIcon();
            booleanValues |= HAS_CHAT;
        } else {
            booleanValues &= ~HAS_CHAT;
        }
        booleanValues = (booleanValues & ~0x00FF0000) | ((icon + 1) << 16);
    }


    // #sijapp cond.if modules_SERVERLISTS is "true" #
    public final boolean inVisibleList() {
        return (booleanValues & SL_VISIBLE) != 0;
    }
    public final boolean inInvisibleList() {
        return (booleanValues & SL_INVISIBLE) != 0;
    }
    public final boolean inIgnoreList() {
        return (booleanValues & SL_IGNORE) != 0;
    }
    protected final void initPrivacyMenu(MenuModel menu) {
        if (!isTemp()) {
            String visibleList = inVisibleList()
                    ? "rem_visible_list" : "add_visible_list";
            String invisibleList = inInvisibleList()
                    ? "rem_invisible_list": "add_invisible_list";
            String ignoreList = inIgnoreList()
                    ? "rem_ignore_list": "add_ignore_list";

            menu.addItem(visibleList,   USER_MENU_PS_VISIBLE);
            menu.addItem(invisibleList, USER_MENU_PS_INVISIBLE);
            menu.addItem(ignoreList,    USER_MENU_PS_IGNORE);
        }
    }
    // #sijapp cond.end #

///////////////////////////////////////////////////////////////////////////

    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return !isTemp() && isSingleUserContact();
    }

///////////////////////////////////////////////////////////////////////////
    public boolean isVisibleInContactList() {
        return isOnline() || hasChat() || isTemp();
    }

    public void getLeftIcons(Icon[] lIcons) {
        if (isTyping()) {
            lIcons[0] = InfoFactory.msgIcons.iconAt(Message.ICON_TYPE);
        } else {
            lIcons[0] = InfoFactory.msgIcons.iconAt(getUnreadMessageIcon());
        }
        Protocol protocol = getProtocol();
        if (null != protocol) {
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (XStatusInfo.XSTATUS_NONE != getXStatusIndex()) {
                lIcons[1] = InfoFactory.factory.getXStatusInfo(protocol).getIcon(getXStatusIndex());
            }
            if (null == lIcons[0]) {
                lIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(getStatusIndex());
                if (InfoFactory.factory.onlyOneIcon(protocol) && (null != lIcons[1])) {
                    lIcons[0] = lIcons[1];
                    lIcons[1] = null;
                }
            }
            // #sijapp cond.end #
        }

        if (!isTemp() && !isAuth()) {
            lIcons[3] = InfoFactory.authIcon;
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        lIcons[4] = InfoFactory.factory.getServerListIcon(this);
        // #sijapp cond.end #
    }
    public final void getRightIcons(Icon[] icons) {
        // #sijapp cond.if modules_CLIENTS is "true" #
        ClientInfo info = InfoFactory.factory.getClientInfo(getProtocol());
        icons[0] = (null != info) ? info.getIcon(clientIndex) : null;
        // #sijapp cond.end #
    }
    public final String getText() {
        return name;
    }

    // Node weight declaration.
    // -3       - normal group
    // -2       - non editable group
    // -1       - non removable group
    //  9       - chat group (online)
    // 10       - contact with message
    // 20 - 49  - normal-contact (status)
    // 50       - chat group (offline)
    // 51       - offline-contact
    // 60       - temp-contact
    public final int getNodeWeight() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isSingleUserContact()) {
            return isOnline() ? 9 : 50;
        }
        // #sijapp cond.end #
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && hasUnreadMessage()) {
            return 10;
        }
        int sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
        if (ContactList.SORT_BY_NAME == sortType) {
            return 20;
        }
        if (isOnline()) {
            switch (sortType) {
                case ContactList.SORT_BY_STATUS:
                    // 29 = 49 - 20 last normal status
                    return 20 + StatusInfo.getWidth(getStatusIndex());
                case ContactList.SORT_BY_ONLINE:
                    return 20;
            }
        }

        if (isTemp()) {
            return 60;
        }
        return 51;
    }
///////////////////////////////////////////////////////////////////////////
    public final boolean hasUnreadMessage() {
        return 0 != (booleanValues & 0x00FF0000);
    }
    public final int getUnreadMessageIcon() {
        return ((booleanValues >>> 16) & 0xFF) - 1;
    }


///////////////////////////////////////////////////////////////////////////
    public static final int USER_MENU_MESSAGE          = 1001;
    public static final int USER_MENU_PASTE            = 1002;

    public static final int USER_MENU_REQU_AUTH        = 1004;

    public static final int USER_MENU_USER_REMOVE      = 1007;
    public static final int USER_MENU_RENAME           = 1009;
    //public static final int USER_MENU_LOCAL_INFO       = 1011;
    public static final int USER_MENU_USER_INFO        = 1012;
    public static final int USER_MENU_MOVE             = 1015;
    public static final int USER_MENU_STATUSES         = 1016;
    public static final int USER_MENU_HISTORY          = 1025;
    public static final int USER_MENU_ADD_USER         = 1018;

    public static final int USER_MENU_GRANT_AUTH       = 1021;
    public static final int USER_MENU_DENY_AUTH        = 1022;


    public static final int USER_MENU_PS_VISIBLE       = 1034;
    public static final int USER_MENU_PS_INVISIBLE     = 1035;
    public static final int USER_MENU_PS_IGNORE        = 1036;

    public static final int USER_MENU_USERS_LIST = 1037;
    public static final int USER_MANAGE_CONTACT = 1038;

    public static final int USER_MENU_WAKE = 13;
    public static final int USER_MENU_FILE_TRANS = 1005;
    public static final int USER_MENU_CAM_TRANS  = 1006;

    public static final int CONFERENCE_DISCONNECT = 1040;

///////////////////////////////////////////////////////////////////////////
    public abstract void initManageContactMenu(Protocol protocol, MenuModel menu);
    public void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        addChatItems(contactMenu);
        addGeneralItems(protocol, contactMenu);
    }
    public void addChatMenuItems(MenuModel model) {
    }

    protected final void addChatItems(MenuModel menu) {
        if (isSingleUserContact()) {
            menu.addItem("send_message", USER_MENU_MESSAGE);
            if (!isAuth()) {
                menu.addItem("requauth", USER_MENU_REQU_AUTH);
            }
        }
        if (isSingleUserContact() || isOnline()) {
            // #sijapp cond.if modules_FILES is "true"#
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addItem("ft_name", USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.addItem("ft_cam", USER_MENU_CAM_TRANS);
            }
            // #sijapp cond.end#
            addChatMenuItems(menu);
        }
    }
    protected final void addGeneralItems(Protocol protocol, MenuModel menu) {
        menu.addItem("info", USER_MENU_USER_INFO);
        menu.addItem("manage", USER_MANAGE_CONTACT);

        // #sijapp cond.if modules_HISTORY is "true" #
        if (!isTemp()) {
            // #sijapp cond.if modules_ANDROID is "true" #
            HistoryStorage history = HistoryStorage.getHistory(this);
            if (null != history.getAndroidStorage().getTextFile()) {
            // #sijapp cond.end#
                menu.addItem("history", Contact.USER_MENU_HISTORY);
            // #sijapp cond.if modules_ANDROID is "true" #
            }
            // #sijapp cond.end#
        }
        // #sijapp cond.end#
        if (protocol.isConnected()) {
            menu.addItem("user_statuses", USER_MENU_STATUSES);
        }
    }
}
