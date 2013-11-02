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

import jimm.Jimm;
import jimm.comm.StringUtils;
import jimmui.view.text.TextList;

import java.io.*;
import java.util.Vector;
import jimm.chat.message.PlainMessage;
import jimm.comm.Util;
import jimm.search.*;
import jimmui.view.text.TextListController;
import jimmui.view.text.TextListModel;
import jimm.util.JLocale;
import protocol.*;
import protocol.ui.ContactMenu;
import protocol.ui.StatusView;
import protocol.ui.XStatusInfo;

/**
 *
 * @author vladimir
 */
public class Mrim extends Protocol {
    private MrimConnection connection = null;
    // #sijapp cond.if modules_MAGIC_EYE is "true" #
    private MicroBlog microBlog;
    // #sijapp cond.end#

    // #sijapp cond.if modules_MAGIC_EYE is "true" #
    public MicroBlog getMicroBlog() {
        return microBlog;
    }
    // #sijapp cond.end#


    public Mrim() {
    }

    @Override
    protected void initThat() {
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        microBlog = new MicroBlog(this);
        // #sijapp cond.end #
    }

    @Override
    protected String processUin(String uin) {
        return uin.toLowerCase();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    @Override
    public String getUniqueUserId(Contact contact) {
        String userId = contact.getUserId();
        if (userId.endsWith("@uin.icq")) {
            return userId.substring(0, userId.indexOf("@"));
        }
        return contact.getUserId();
    }

    @Override
    protected void startConnection() {
        connection = new MrimConnection(this);
        connection.start();
    }

    public MrimConnection getConnection() {
        return connection;
    }

    @Override
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

    @Override
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

    @Override
    protected void closeConnection() {
        MrimConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    @Override
    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }

    @Override
    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to.isSingleUserContact()) {
            connection.sendTypingNotify(to.getUserId(), isTyping);
        }
    }

    @Override
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
    @Override
    protected void s_updateXStatus() {
        connection.setStatus();
    }
    // #sijapp cond.end #
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    @Override
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

    @Override
    protected void s_searchUsers(Search cont) {
        String uin = cont.getSearchParam(Search.UIN);
        if ((null != uin) && (-1 == uin.indexOf('@'))) {
            // Phone contact...

            UserInfo userInfo = new UserInfo(this);
            userInfo.uin = uin;
            cont.addResult(userInfo);
            cont.putToGroup(getPhoneGroup());
            cont.finished();
            return;
        }
        connection.searchUsers(cont);
    }
    @Override
    protected void s_updateOnlineStatus() {
        connection.setStatus();
    }

    @Override
    protected void s_addContact(Contact contact) {
        connection.addContact((MrimContact)contact);
    }

    @Override
    public void requestAuth(String uin) {
        connection.requestAuth(uin, getUserId());
    }
    @Override
    public void grandAuth(String uin) {
        connection.grandAuth(uin);
    }
    @Override
    public void denyAuth(String userId) {
    }
    @Override
    protected void s_removeContact(Contact contact) {
        connection.removeContact((MrimContact) contact);
    }

    @Override
    protected void s_addGroup(Group group) {
        connection.addGroup((MrimGroup) group);
    }
    @Override
    public Group createGroup(String name) {
        return new MrimGroup(-1, 0, name);
    }

    @Override
    protected void s_removeGroup(Group group) {
        connection.removeGroup((MrimGroup) group);
    }
    @Override
    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.renameGroup((MrimGroup) group);
    }
    @Override
    protected void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        getConnection().updateContact((MrimContact) contact);
    }
    @Override
    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        getConnection().updateContact((MrimContact) contact);
    }

    public void sendSms(String phone, String text) {
        getConnection().sendSms(phone, text);
    }

    public MrimContact getContactByPhone(String phone) {
        for (int i = roster.contacts.size() - 1; i >= 0; i--) {
            MrimContact contact = (MrimContact)roster.contacts.elementAt(i);
            String phones = contact.getPhones();
            if ((null != phones) && (-1 != phones.indexOf(phone))) {
                return contact;
            }
        }
        return null;
    }

    @Override
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

    @Override
    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        MrimContact mrimContact = (MrimContact)contact;
        if (contact instanceof MrimPhoneContact) return;
        out.writeByte(0);
        out.writeInt(mrimContact.getContactId());
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeUTF(StringUtils.notNull(mrimContact.getPhones()));
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
        out.writeInt(mrimContact.getFlags());
    }

    @Override
    public void getAvatar(UserInfo userInfo) {
        new jimmui.view.timers.GetVersion(userInfo).get();
    }

    @Override
    public String getUserIdName() {
        return "E-mail";
    }

    @Override
    public void saveUserInfo(UserInfo userInfo) {
    }

    @Override
    public void doAction(Contact c, int action) {
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
                Jimm.getJimm().getCL().activate();
                break;
            // #sijapp cond.end #
        }
        if (MrimChatContact.USER_MENU_USERS_LIST == action) {
            TextListModel list = new TextListModel();
            Vector<String> members = ((MrimChatContact)c).getMembers();
            for (int i = 0; i < members.size(); ++i) {
                list.addItem((String)members.elementAt(i), false);
            }
            TextList tl = new TextList(JLocale.getString("list_of_users"));
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

    @Override
    public void showUserInfo(Contact contact) {
        UserInfo data;
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

    @Override
    public void showStatus(Contact contact) {
        if (contact instanceof MrimPhoneContact) {
            return;
        }
        StatusView statusView = Jimm.getJimm().getStatusView();

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