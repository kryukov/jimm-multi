/*
 * Mrim.java
 *
 * Created on 7 Март 2008 г., 20:48
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_MRIM is "true" #
package protocol.mrim;

import jimmui.view.text.TextList;
import jimmui.view.icons.*;
import java.io.*;
import java.util.Vector;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.search.*;
import jimmui.view.menu.MenuModel;
import jimmui.view.text.TextListController;
import jimmui.view.text.TextListModel;
import jimm.util.JLocale;
import protocol.*;

/**
 *
 * @author vladimir
 */
public class Mrim extends Protocol {
    private MrimConnection connection = null;
    // #sijapp cond.if modules_MAGIC_EYE is "true" #
    private MicroBlog microBlog;
    // #sijapp cond.end#
    private static final ImageList statusIcons = ImageList.createImageList("/mrim-status.png");
    private static final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, -1, 5, -1, 2, -1, 1};

    static Icon getPhoneContactIcon() {
        int phoneContactIndex = statusIconIndex[StatusInfo.STATUS_OFFLINE];
        if (6 < statusIcons.size()) {
            phoneContactIndex = statusIcons.size() - 1;
        }
        return statusIcons.iconAt(phoneContactIndex);
    }

    // #sijapp cond.if modules_MAGIC_EYE is "true" #
    public MicroBlog getMicroBlog() {
        return microBlog;
    }
    // #sijapp cond.end#

    private static final byte[] statuses = {
        StatusInfo.STATUS_CHAT,
        StatusInfo.STATUS_ONLINE,
        StatusInfo.STATUS_AWAY,
        StatusInfo.STATUS_UNDETERMINATED,
        StatusInfo.STATUS_INVISIBLE};

    /** Creates a new instance of Mrim */
    public Mrim() {
    }
    protected void initStatusInfo() {
        info = new StatusInfo(statusIcons, statusIconIndex, statuses);
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        microBlog = new MicroBlog(this);
        // #sijapp cond.end #

        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatusInfo = Mrim.xStatus.getInfo();
        // #sijapp cond.end #

        // #sijapp cond.if modules_CLIENTS is "true" #
        clientInfo = MrimClient.get();
        // #sijapp cond.end #
    }
    protected String processUin(String uin) {
        return uin.toLowerCase();
    }
    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    public String getUniqueUserId(Contact contact) {
        String userId = contact.getUserId();
        if (userId.endsWith("@uin.icq")) {
            return userId.substring(0, userId.indexOf("@"));
        }
        return contact.getUserId();
    }

    protected void startConnection() {
        connection = new MrimConnection(this);
        connection.start();
    }
    public MrimConnection getConnection() {
        return connection;
    }
    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    private Group getPhoneGroup() {
        MrimGroup phoneGroup = (MrimGroup)getGroupById(MrimGroup.PHONE_CONTACTS_GROUP);
        if (null != phoneGroup) {
            return phoneGroup;
        }
        phoneGroup = (MrimGroup)createGroup(JLocale.getString("phone_contacts"));
        phoneGroup.setFlags(0);
        phoneGroup.setGroupId(MrimGroup.PHONE_CONTACTS_GROUP);
        addGroup(phoneGroup);
        return phoneGroup;
    }
    protected Contact createContact(String uin, String name) {
        name = (null == name) ? uin : name;
        if (-1 == uin.indexOf('@')) {
            if (0 < Util.strToIntDef(uin, 0)) {
                uin = uin + "@uin.icq";
            }
            if ("phone".equals(uin)) {
                return new MrimPhoneContact("");
            }
        }
        if (uin.endsWith("@chat.agent")) {
            return new MrimChatContact(uin, name);
        }
        return new MrimContact(uin, name);
    }

    protected void closeConnection() {
        MrimConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }
    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to.isSingleUserContact()) {
            connection.sendTypingNotify(to.getUserId(), isTyping);
        }
    }

    public boolean isMeVisible(Contact to) {
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        if (to.inInvisibleList()) {
            return false;
        }
        if (to.inIgnoreList()) {
            return false;
        }
        // #sijapp cond.end #
        return true;
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    public static final MrimXStatusInfo xStatus = new MrimXStatusInfo();
    protected void s_updateXStatus() {
        connection.setStatus();
    }
    // #sijapp cond.end #
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    protected void s_setPrivateStatus() {
        if (isConnected()) {
            connection.setStatus();
        }
    }
    // #sijapp cond.end #

    public int getPrivateStatusMask() {
//        // #sijapp cond.if modules_SERVERLISTS is "true" #
//        byte pstatus = getPrivateStatus();
//        if (PrivateStatusForm.PSTATUS_VISIBLE_ONLY == pstatus) {
//            return MrimStatusInfo.STATUS_FLAG_INVISIBLE;
//        }
//        // #sijapp cond.end #
        return 0x00000000;
    }

    protected void s_searchUsers(Search cont) {
        String uin = cont.getSearchParam(Search.UIN);
        if ((null != uin) && (-1 == uin.indexOf('@'))) {
            // Phone contact...

            UserInfo userInfo = new UserInfo(this);
            userInfo.uin = uin;
            if (null != userInfo.uin) {
                cont.addResult(userInfo);
            }
            cont.putToGroup(getPhoneGroup());
            cont.finished();
            return;
        }
        connection.searchUsers(cont);
    }
    protected void s_updateOnlineStatus() {
        connection.setStatus();
    }

    protected void s_addContact(Contact contact) {
        connection.addContact((MrimContact)contact);
    }

    public void requestAuth(String uin) {
        connection.requestAuth(uin, getUserId());
    }
    public void grandAuth(String uin) {
        connection.grandAuth(uin);
    }
    protected void denyAuth(String userId) {
    }
    protected void s_removeContact(Contact contact) {
        connection.removeContact((MrimContact) contact);
    }

    protected void s_addGroup(Group group) {
        connection.addGroup((MrimGroup) group);
    }
    public Group createGroup(String name) {
        return new MrimGroup(-1, 0, name);
    }

    protected void s_removeGroup(Group group) {
        connection.removeGroup((MrimGroup) group);
    }
    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.renameGroup((MrimGroup) group);
    }
    protected void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        getConnection().updateContact((MrimContact) contact);
    }
    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        getConnection().updateContact((MrimContact) contact);
    }
    public void sendSms(String phone, String text) {
        getConnection().sendSms(phone, text);
    }

    public MrimContact getContactByPhone(String phone) {
        for (int i = contacts.size() - 1; i >= 0; i--) {
            MrimContact contact = (MrimContact)contacts.elementAt(i);
            String phones = contact.getPhones();
            if ((null != phones) && (-1 != phones.indexOf(phone))) {
                return contact;
            }
        }
        return null;
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        // Get item type
        int contactId = dis.readInt();
        String uin = dis.readUTF();
        String name = dis.readUTF();
        String phones = dis.readUTF();
        int groupId = dis.readInt();
        final int serverFlags = 0;
        byte booleanValues = dis.readByte();
        int flags = dis.readInt();
        MrimContact c = (MrimContact) createContact(uin, name);
        c.setPhones(phones);
        c.init(contactId, name, phones, groupId, serverFlags, flags);
        c.setBooleanValues(booleanValues);
        return c;
    }
    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        MrimContact mrimContact = (MrimContact)contact;
        if (contact instanceof MrimPhoneContact) return;
        out.writeByte(0);
        out.writeInt(mrimContact.getContactId());
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeUTF(StringConvertor.notNull(mrimContact.getPhones()));
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
        out.writeInt(mrimContact.getFlags());
    }

    public void getAvatar(UserInfo userInfo) {
        new jimmui.view.timers.GetVersion(userInfo).get();
    }

    public String getUserIdName() {
        return "E-mail";
    }
    public void saveUserInfo(UserInfo userInfo) {
    }
    protected void doAction(Contact c, int action) {
        MrimContact contact = (MrimContact)c;
        switch (action) {
            case MrimContact.USER_MENU_SEND_SMS:
                new jimm.forms.SmsForm(this, contact.getPhones()).show();
                break;

            case Contact.CONFERENCE_DISCONNECT:
                new ContactMenu(this, c).doAction(Contact.USER_MENU_USER_REMOVE);
                break;

            // #sijapp cond.if modules_SERVERLISTS is "true" #
            case MrimContact.USER_MENU_PS_VISIBLE:
            case MrimContact.USER_MENU_PS_INVISIBLE:
            case MrimContact.USER_MENU_PS_IGNORE:
                int flags = contact.getFlags();
                switch (action) {
                    case MrimContact.USER_MENU_PS_VISIBLE:   flags ^= MrimContact.CONTACT_FLAG_VISIBLE; break;
                    case MrimContact.USER_MENU_PS_INVISIBLE: flags ^= MrimContact.CONTACT_FLAG_INVISIBLE; break;
                    case MrimContact.USER_MENU_PS_IGNORE:    flags ^= MrimContact.CONTACT_FLAG_IGNORE; break;
                }
                contact.setFlags(flags);
                getConnection().updateContact(contact);
                ContactList.getInstance().activate();
                break;
            // #sijapp cond.end #
        }
        if (MrimChatContact.USER_MENU_USERS_LIST == action) {
            TextListModel list = new TextListModel();
            Vector members = ((MrimChatContact)c).getMembers();
            for (int i = 0; i < members.size(); ++i) {
                list.addItem((String)members.elementAt(i), false);
            }
            TextList tl = new TextList(JLocale.getString("list_of_users"));
            tl.setAllToTop();
            tl.setModel(list);
            tl.setController(new TextListController(null, -1));
            tl.show();

        } else if (MrimChatContact.USER_MENU_ADD_USER == action) {
            if (isConnected()) {
                addContact(contact);
                getConnection().putMultiChatGetMembers(contact.getUserId());
            }
        }
    }

    public void showUserInfo(Contact contact) {
        UserInfo data = null;
        if (contact instanceof MrimPhoneContact) {
            data = new UserInfo(this);
            data.nick = contact.getName();
            data.homePhones = ((MrimContact)contact).getPhones();
            data.createProfileView(contact.getName());
            data.updateProfileView();

        } else if (isConnected()) {
            data = getConnection().getUserInfo((MrimContact) contact);
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this, contact.getUserId());
            data.uin = contact.getUserId();
            data.nick = contact.getName();
            data.homePhones = ((MrimContact)contact).getPhones();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile();
    }

    public void showStatus(Contact contact) {
        if (contact instanceof MrimPhoneContact) {
            return;
        }
        StatusView statusView = ContactList.getInstance().getStatusView();
        MenuModel menu = new MenuModel();

        statusView.init(this, contact);
        statusView.initUI();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
    	    statusView.addXStatus();
    	    statusView.addStatusText(contact.getXStatusText());
        } else {
            statusView.addContactStatus();
    	    statusView.addStatusText(contact.getStatusText());
        }
        // #sijapp cond.else #
        statusView.addContactStatus();
        // #sijapp cond.end #

        // #sijapp cond.if modules_CLIENTS is "true" #
        statusView.addClient();
        // #sijapp cond.end #
        statusView.addTime();
        statusView.showIt();
    }
}
// #sijapp cond.end #