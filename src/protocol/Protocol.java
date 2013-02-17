            /*
 * Protocol.java
 *
 * Created on 13 Май 2008 г., 12:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.*;
import DrawControls.tree.*;
import java.io.*;
import java.util.Vector;
import javax.microedition.rms.*;
import jimm.*;
import jimm.chat.*;
import jimm.chat.message.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.io.Storage;
import jimm.modules.*;
import jimm.search.*;
import jimm.util.JLocale;
import protocol.jabber.*;

/**
 *
 * @author vladimir
 */
abstract public class Protocol {
    protected Vector contacts = new Vector();
    protected Vector groups = new Vector();
    private Profile profile;
    private String password;
    private String userid = "";
    protected StatusInfo info;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    protected XStatusInfo xstatusInfo;
    // #sijapp cond.end #
    // #sijapp cond.if modules_CLIENTS is "true" #
    public ClientInfo clientInfo;
    // #sijapp cond.end #
    private String rmsName = null;

    private boolean isReconnect;
    private int reconnect_attempts;
    private boolean needSave = false;

    private long lastStatusChangeTime;
    private byte progress = 100;
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private byte privateStatus = 0;
    // #sijapp cond.end #
    private boolean connectedByUser = false;

    private final Object rosterLockObject = new Object();
    private Vector sortedContacts = new Vector();
    private Vector sortedGroups = new Vector();
    private Group notInListGroup;

    private Vector autoGrand = new Vector();

    private static final int RECONNECT_COUNT = 20;

    private String getContactListRS() {
        return rmsName;
    }

    public abstract String getUserIdName();

    public final String getUserId() {
        return userid;
    }
    protected final void setUserId(String userId) {
        userid = userId;
    }
    public boolean isEmpty() {
        return StringConvertor.isEmpty(userid);
    }

    public final String getNick() {
        String nick = profile.nick;
        return (nick.length() == 0) ? JLocale.getString("me") : nick;
    }

    public final Profile getProfile() {
        return profile;
    }

    public final String getPassword() {
        return (null == password) ? profile.password : password;
    }
    public final void setPassword(String pass) {
        password = pass;
    }

    // #sijapp cond.if modules_MULTI is "true" #
    private String getDefaultDomain(byte type) {
        switch (type) {
            case Profile.PROTOCOL_GTALK:    return "@gmail.com";
            case Profile.PROTOCOL_FACEBOOK: return "@chat.facebook.com";
            case Profile.PROTOCOL_LJ:       return "@livejournal.com";
            case Profile.PROTOCOL_YANDEX:   return "@ya.ru";
            case Profile.PROTOCOL_VK:       return "@vk.com";
            case Profile.PROTOCOL_QIP:      return "@qip.ru";
            case Profile.PROTOCOL_ODNOKLASSNIKI: return "@odnoklassniki.ru";
        }
        return null;
    }
    // #sijapp cond.end #
    public final void setProfile(Profile account) {
        this.profile = account;
        String rawUin = StringConvertor.notNull(account.userId);
        // #sijapp cond.if modules_MULTI is "true" #
        if (!StringConvertor.isEmpty(rawUin)) {
            byte type = account.protocolType;
            if ((Profile.PROTOCOL_VK == type)
                    && (0 < Util.strToIntDef(rawUin, 0))) {
                rawUin = "id" + rawUin;
                account.userId = rawUin;
            }
            String domain = getDefaultDomain(type);
            if ((null != domain) && (-1 == rawUin.indexOf('@'))) {
                rawUin += domain;
            }
        }
        // #sijapp cond.end #
        userid = StringConvertor.isEmpty(rawUin) ? "" : processUin(rawUin);
        if (!StringConvertor.isEmpty(account.password)) {
            setPassword(null);
        }

        String rms = "cl-" + getUserId();
        rmsName = (32 < rms.length()) ? rms.substring(0, 32) : rms;
    }
    protected String processUin(String uin) {
        return uin;
    }
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public final XStatusInfo getXStatusInfo() {
        return xstatusInfo;
    }
    // #sijapp cond.end #
    public final void init() {
        // Not In List Group
        notInListGroup = new Group(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
        notInListGroup.setGroupId(Group.NOT_IN_GROUP);

        // Status info
        initStatusInfo();

        // Status
        initStatus();
    }
    protected abstract void initStatusInfo();

    private Icon getCurrentStatusIcon() {
        if (isConnected() && !isConnecting()) {
            return getStatusInfo().getIcon(getProfile().statusIndex);
        }
        return getStatusInfo().getIcon(StatusInfo.STATUS_OFFLINE);
    }
    public final void getCapIcons(Icon[] capIcons) {
        capIcons[0] = getCurrentStatusIcon();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (null != xstatusInfo) {
            capIcons[1] = xstatusInfo.getIcon(getProfile().xstatusIndex);
        }
        // #sijapp cond.end #
    }

    public final void sort() {
        synchronized (rosterLockObject) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                Util.sort(getSortedGroups());
            } else {
                Util.sort(getSortedContacts());
            }
        }
    }

    public final void setContactListStub() {
        synchronized (rosterLockObject) {
            contacts = new Vector();
            groups = new Vector();
        }
    }
    public final void setContactList(Vector groups, Vector contacts) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((contacts.size() > 0) && !(contacts.elementAt(0) instanceof Contact)) {
            DebugLog.panic("contacts is not list of Contact");
            contacts = new Vector();
        }
        if ((groups.size() > 0) && !(groups.elementAt(0) instanceof Group)) {
            DebugLog.panic("groups is not list of Group");
            groups = new Vector();
        }
        // #sijapp cond.end #
        synchronized (rosterLockObject) {
            this.contacts = contacts;
            this.groups = groups;
        }
        ChatHistory.instance.restoreContactsWithChat(this);

        synchronized (rosterLockObject) {
            sortedContacts = new Vector();
            for (int i = 0; i < contacts.size(); ++i) {
                sortedContacts.addElement(contacts.elementAt(i));
            }
            sortedGroups = new Vector();
            for (int i = 0; i < groups.size(); ++i) {
                Group g = (Group)groups.elementAt(i);
                updateContacts(g);
                sortedGroups.addElement(g);
            }
            Util.sort(sortedGroups);
            updateContacts(notInListGroup);
        }
        getContactList().getManager().update();
        needSave();
    }
    // #sijapp cond.if protocols_JABBER is "true" #
    public final void setContactListAddition(Group group) {
        synchronized (rosterLockObject) {
            updateContacts(group);

            updateContacts(notInListGroup);
            Vector groupItems = group.getContacts();
            for (int i = 0; i < groupItems.size(); ++i) {
                if (-1 == Util.getIndex(sortedContacts, groupItems.elementAt(i))) {
                    sortedContacts.addElement(groupItems.elementAt(i));
                }
            }
        }
        getContactList().getManager().update();
        needSave();
    }
    // #sijapp cond.end#
    private void updateContacts(Group group) {
        Vector allItems = getContactItems();
        Vector groupItems = group.getContacts();
        groupItems.removeAllElements();
        int size = allItems.size();
        int groupId = group.getId();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact)allItems.elementAt(i);
            if (item.getGroupId() == groupId) {
                groupItems.addElement(item);
            }
        }
        group.updateGroupData();
        group.sort();
    }

    /* ********************************************************************* */
    public final void setConnectingProgress(int percent) {
        this.progress = (byte)((percent < 0) ? 100 : percent);
        if (100 == percent) {
            reconnect_attempts = RECONNECT_COUNT;
            getContactList().updateConnectionStatus();
        }
        getContactList().getManager().invalidate();
    }
    public final boolean isConnecting() {
        return 100 != progress;
    }
    public final byte getConnectingProgress() {
        return progress;
    }
    /* ********************************************************************* */
    // #sijapp cond.if modules_FILES is "true"#
    public void sendFile(FileTransfer transfer, String filename, String description) {
    }
    // #sijapp cond.end#
    public void getAvatar(UserInfo userInfo) {
    }
    /* ********************************************************************* */
    protected abstract void requestAuth(String userId);
    protected abstract void grandAuth(String userId);
    protected abstract void denyAuth(String userId);
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    protected abstract void s_setPrivateStatus();
    public final void setPrivateStatus(byte status) {
        privateStatus = status;
        if (isConnected()) {
            s_setPrivateStatus();
        }
    }
    public final byte getPrivateStatus() {
        return privateStatus;
    }
    // #sijapp cond.end #
    public final void requestAuth(Contact contact) {
        requestAuth(contact.getUserId());
        autoGrandAuth(contact.getUserId());
    }
    private void autoGrandAuth(String userId) {
        autoGrand.addElement(userId);
    }
    /* ********************************************************************* */
    public final void safeLoad() {
        if ("".equals(getUserId())) {
            return;
        }
        if (isConnected()) {
            return;
        }
        try {
            if (new Storage(getContactListRS()).exist()) {
                load();
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("roster load", e);
            // #sijapp cond.end #
            setContactList(new Vector(), new Vector());
        }
    }

    public final void needSave() {
        needSave = true;
        ContactList.getInstance().needRosterSave();
    }
    // FIXME
    public final boolean safeSave() {
        boolean save = needSave;
        needSave = false;
        // Try to delete the record store
        if (!save || "".equals(getUserId())) {
            return false;
        }
        synchronized (this) {
            String storage = getContactListRS();
            try {
                RecordStore.deleteRecordStore(storage);
            } catch (Exception e) {
                // Do nothing
            }

            RecordStore cl = null;
            try {
                // Create new record store
                cl = RecordStore.openRecordStore(storage, true);
                save(cl);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("roster save", e);
                // #sijapp cond.end #
            }
            try {
                // Close record store
                cl.closeRecordStore();
            } catch (Exception e) {
                // Do nothing
            }
        }
        return true;
    }
    // Tries to load contact list from record store
    private void load() throws Exception {
        // Initialize vectors
        Vector cItems = new Vector();
        Vector gItems = new Vector();

        // Open record store
        RecordStore cl = RecordStore.openRecordStore(getContactListRS(), false);
        try {
            // Temporary variables
            byte[] buf;
            ByteArrayInputStream bais;
            DataInputStream dis;

            // Get version info from record store
            buf = cl.getRecord(1);
            bais = new ByteArrayInputStream(buf);
            dis = new DataInputStream(bais);
            if (!dis.readUTF().equals(Jimm.VERSION)) {
                throw new Exception();
            }

            // Get version ids from the record store
            loadProtocolData(cl.getRecord(2));

            // Read all remaining items from the record store
            for (int marker = 3; marker <= cl.getNumRecords(); ++marker) {
                try {
                    buf = cl.getRecord(marker);
                    if ((null == buf) || (0 == buf.length)) {
                        continue;
                    }

                    bais = new ByteArrayInputStream(buf);
                    dis = new DataInputStream(bais);
                    // Loop until no more items are available
                    while (0 < dis.available()) {
                        // Get type of the next item
                        byte type = dis.readByte();
                        switch (type) {
                            case 0:
                                cItems.addElement(loadContact(dis));
                                break;
                            case 1:
                                gItems.addElement(loadGroup(dis));
                                break;
                        }
                    }
                } catch (EOFException e) {
                }
            }
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.memoryUsage("clload");
            // #sijapp cond.end#
        } finally {
            // Close record store
            cl.closeRecordStore();
        }
        setContactList(gItems, cItems);
    }

    // Save contact list to record store
    private void save(RecordStore cl) throws Exception {
        // Temporary variables
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        byte[] buf;

        // Add version info to record store
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeUTF(Jimm.VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);

        // Add version ids to the record store
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);

        // Initialize buffer
        baos.reset();

        // Iterate through all contact items
        int cItemsCount = contacts.size();
        int totalCount  = cItemsCount + groups.size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                saveContact(dos, (Contact)contacts.elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, (Group)groups.elementAt(i - cItemsCount));
            }

            // Start new record if it exceeds 4000 bytes
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                // Save record
                buf = baos.toByteArray();
                cl.addRecord(buf, 0, buf.length);

                // Initialize buffer
                baos.reset();
            }
        }
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        String uin = dis.readUTF();
        String name = dis.readUTF();
        int groupId = dis.readInt();
        byte booleanValues = dis.readByte();
        Contact contact = createContact(uin, name);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        return contact;
    }
    protected Group loadGroup(DataInputStream dis) throws Exception {
        int groupId = dis.readInt();
        String name = dis.readUTF();
        Group group = createGroup(name);
        group.setGroupId(groupId);
        group.setExpandFlag(dis.readBoolean());
        return group;
    }
    protected void loadProtocolData(byte[] data) throws Exception {
    }
    protected byte[] saveProtocolData() throws Exception {
        return new byte[0];
    }
    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeByte(0);
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
    }
    protected void saveGroup(DataOutputStream out, Group group) throws Exception {
        out.writeInt(group.getId());
        out.writeUTF(group.getName());
        out.writeBoolean(group.isExpanded());
    }

    /* ********************************************************************* */

    protected void s_removeContact(Contact contact) {};
    protected void s_removedContact(Contact contact) {};
    public final void removeContact(Contact contact) {
        // Check whether contact item is temporary
        if (contact.isTemp()) {
        } else if (isConnected()) {
            // Request contact item removal
            s_removeContact(contact);
        } else {
            return;
        }
        removeLocalContact(contact);
    }

    abstract protected void s_renameContact(Contact contact, String name);
    public final void renameContact(Contact contact, String name) {
        if (StringConvertor.isEmpty(name)) {
            return;
        }
        if (!inContactList(contact)) {
            contact.setName(name);
            return;
        }
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_renameContact(contact, name);
        } else {
            return;
        }
        contact.setName(name);
        cl_renameContact(contact);
        needSave();
    }

    abstract protected void s_moveContact(Contact contact, Group to);
    public final void moveContactTo(Contact contact, Group to) {
        s_moveContact(contact, to);
        cl_moveContact(contact, to);
    }
    protected void s_addContact(Contact contact) {};
    protected void s_addedContact(Contact contact) {}
    public final void addContact(Contact contact) {
        s_addContact(contact);
        contact.setTempFlag(false);
        cl_addContact(contact);
        getContactList().getManager().setActiveContact(contact);
        needSave();
        s_addedContact(contact);
    }

    public final void addTempContact(Contact contact) {
        cl_addContact(contact);
    }

    abstract protected void s_removeGroup(Group group);
    public final void removeGroup(Group group) {
        s_removeGroup(group);
        cl_removeGroup(group);
        needSave();
    }
    abstract protected void s_renameGroup(Group group, String name);
    public final void renameGroup(Group group, String name) {
        s_renameGroup(group, name);
        group.setName(name);
        cl_renameGroup(group);
        needSave();
    }
    abstract protected void s_addGroup(Group group);
    public final void addGroup(Group group) {
        s_addGroup(group);
        cl_addGroup(group);
        needSave();
    }

    abstract public boolean isConnected();
    abstract protected void startConnection();
    abstract protected void closeConnection();
    protected void userCloseConnection() {
    }
    public final void disconnect(boolean user) {
        setConnectingProgress(-1);
        closeConnection();
        if (user) {
            profile.isConnected = false;
            Options.saveAccount(profile);
            userCloseConnection();
        }
        /* Reset all contacts offline */
        setStatusesOffline();
        /* Disconnect */

        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().safeSave();
        // #sijapp cond.end#
        getContactList().getManager().update();
        getContactList().updateConnectionStatus();
        if (user) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("disconnect " + getUserId());
            // #sijapp cond.end #
        }
    }


    abstract public Group createGroup(String name);
    abstract protected Contact createContact(String uin, String name);
    public final Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUIN(uin);
        if (null != contact) {
            return contact;
        }
        contact = createContact(uin, name);
        if (null != contact) {
            contact.setTempFlag(true);
        }
        return contact;
    }
    public final Contact createTempContact(String uin) {
        return createTempContact(uin, uin);
    }

    abstract protected void s_searchUsers(Search cont);
    public final void searchUsers(Search cont) {
        s_searchUsers(cont);
    }
    public final Search getSearchForm() {
        if (groups.isEmpty()) {
            return null;
        }
        return new Search(this);
    }

    public final Vector getContactItems() {
        return contacts;
    }
    public final Vector getGroupItems() {
        return groups;
    }
    // #sijapp cond.if modules_SOUND is "true" #
    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUIN(uin);
        if (null != item) {
            beginTyping(item, type);
        }
    }
    private void beginTyping(Contact item, boolean type) {
        if (null == item) {
            return;
        }
        if (item.isTyping() != type) {
            item.beginTyping(type);
            Chat chat = ChatHistory.instance.getChat(item);
            if (null != chat) {
                chat.beginTyping(type);
            }
            if (type && isConnected()) {
                playNotification(Notify.NOTIFY_TYPING);
            }
            getContactList().getManager().invalidate();
        }
    }
    // #sijapp cond.end #
    private void updateChatStatus(Contact c) {
        Chat chat = ChatHistory.instance.getChat(c);
        if (null != chat) {
            chat.updateStatus();
        }
    }

    protected final void setStatusesOffline() {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact c = (Contact)contacts.elementAt(i);
            c.setOfflineStatus();
        }
        synchronized (rosterLockObject) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                for (int i = groups.size() - 1; i >= 0; --i) {
                    ((Group)groups.elementAt(i)).updateGroupData();
                }
            }
        }
    }

    public final Contact getItemByUIN(String uin) {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = (Contact)contacts.elementAt(i);
            if (contact.getUserId().equals(uin)) {
                return contact;
            }
        }
        return null;
    }
    public final Group getGroupById(int id) {
        synchronized (rosterLockObject) {
            for (int i = groups.size() - 1; 0 <= i; --i) {
                Group group = (Group)groups.elementAt(i);
                if (group.getId() == id) {
                    return group;
                }
            }
        }
        return null;
    }
    public final Group getGroup(Contact contact) {
        return getGroupById(contact.getGroupId());
    }

    public final Group getGroup(String name) {
        synchronized (rosterLockObject) {
            for (int i = groups.size() - 1; 0 <= i; --i) {
                Group group = (Group)groups.elementAt(i);
                if (group.getName().equals(name)) {
                    return group;
                }
            }
        }
        return null;
    }

    public final ContactList getContactList() {
        return ContactList.getInstance();
    }

    public final boolean inContactList(Contact contact) {
        return -1 != Util.getIndex(contacts, contact);
    }

    public final StatusInfo getStatusInfo() {
        return info;
    }

    protected abstract void s_updateOnlineStatus();
    public final void setOnlineStatus(int statusIndex, String msg) {
        profile.statusIndex = (byte)statusIndex;
        profile.statusMessage = msg;
        Options.saveAccount(profile);

        setLastStatusChangeTime();
        if (isConnected()) {
            s_updateOnlineStatus();
        }
    }
    // #sijapp cond.if modules_XSTATUSES is "true" #
    protected abstract void s_updateXStatus();
    public final void setXStatus(int xstatus, String title, String desc) {
        profile.xstatusIndex = (byte)xstatus;
        profile.xstatusTitle = title;
        profile.xstatusDescription = desc;
        Options.saveAccount(profile);
        if (isConnected()) {
            s_updateXStatus();
        }
    }
    // #sijapp cond.end #
    private void initStatus() {
        setLastStatusChangeTime();
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        setPrivateStatus((byte)Options.getInt(Options.OPTION_PRIVATE_STATUS));
        // #sijapp cond.end #
    }

    ///////////////////////////////////////////////////////////////////////////
    private void ui_removeFromAnyGroup(Contact c) {
        Vector allGroups = getGroupItems();
        for (int i = 0; i < allGroups.size(); ++i) {
            Group group = (Group)allGroups.elementAt(i);
            if (group.removeContact(c)) {
                group.updateGroupData();
                return;
            }
        }
        notInListGroup.removeContact(c);
        notInListGroup.updateGroupData();
    }
    private void ui_addContactToGroup(Contact contact, Group group) {
        ui_removeFromAnyGroup(contact);
        contact.setGroup(group);
        if (null != group) {
            group.addContact(contact);
        } else {
            notInListGroup.addContact(contact);
        }
    }
    private void ui_updateContactInGroup(Contact contact, Group group) {
        // TODO: sort contact only if group is expanded or when group is going to be expanded
        synchronized (rosterLockObject) {
            if (null == group) {
                group = notInListGroup;
            }
            getContactList().getManager().putIntoQueue(group);
        }
    }
    private void ui_updateGroup(Group group) {
        if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
            synchronized (rosterLockObject) {
                group.updateGroupData();
                Util.sort(sortedGroups);
            }
            ui_updateCL(group);
        }
    }
    public final Group getNotInListGroup() {
        return notInListGroup;
    }
    private void ui_updateCL(Contact c) {
        // #sijapp cond.if modules_MULTI is "true" #
        if (!getProtocolBranch().isExpanded()) return;
        // #sijapp cond.end #
        getContactList().getManager().update(c);
    }
    private void ui_updateCL(Group g) {
        // #sijapp cond.if modules_MULTI is "true" #
        if (!getProtocolBranch().isExpanded()) return;
        // #sijapp cond.end #
        getContactList().getManager().update(g);
    }

    // #sijapp cond.if modules_MULTI is "true" #
    private ProtocolBranch branch;
    public final ProtocolBranch getProtocolBranch() {
        if (null == branch) {
            branch = new ProtocolBranch(this);
        }
        return branch;
    }
    // #sijapp cond.end #
    ///////////////////////////////////////////////////////////////////////////
    public final Vector getSortedContacts() {
        return sortedContacts;
    }

    public final Vector getSortedGroups() {
        return sortedGroups;
    }

    public final Object getRosterLockObject() {
        return rosterLockObject;
    }
    ///////////////////////////////////////////////////////////////////////////
    public final void markMessages(Contact contact) {
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
            ui_updateContact(contact);
        }
        getContactList().markMessages(contact);
    }
    public final void ui_changeContactStatus(Contact contact) {
        updateChatStatus(contact);
        ui_updateContact(contact);
    }
    public final void ui_updateContact(Contact contact) {
        ui_updateContactInGroup(contact, getGroup(contact));
        ui_updateCL(contact);
    }

    private void cl_addContact(Contact contact) {
        if (null == contact) {
            return;
        }
        Group g = getGroup(contact);
        boolean hasnt = !inContactList(contact);
        if (hasnt) {
            contacts.addElement(contact);
        }
        synchronized (rosterLockObject) {
            if (hasnt) {
                sortedContacts.addElement(contact);
            }
            ui_addContactToGroup(contact, g);
        }
        ui_updateContact(contact);
    }

    private void cl_renameContact(Contact contact) {
        ui_updateContact(contact);
    }
    private void cl_moveContact(Contact contact, Group to) {
        synchronized (rosterLockObject) {
            ui_addContactToGroup(contact, to);
        }
        ui_updateContact(contact);
    }
    private void cl_removeContact(Contact contact) {
        contacts.removeElement(contact);
        synchronized (rosterLockObject) {
            sortedContacts.removeElement(contact);
            ui_removeFromAnyGroup(contact);
        }
        ui_updateCL(contact);
    }

    private void cl_addGroup(Group group) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (-1 != Util.getIndex(groups, group)) {
            DebugLog.panic("Group '" + group.getName() + "' already added");
        }
        // #sijapp cond.end #
        groups.addElement(group);
        synchronized (rosterLockObject) {
            sortedGroups.addElement(group);
            ui_updateGroup(group);
        }
    }
    private void cl_renameGroup(Group group) {
        ui_updateGroup(group);
    }
    private void cl_removeGroup(Group group) {
        groups.removeElement(group);
        synchronized (rosterLockObject) {
            sortedGroups.removeElement(group);
        }
        ui_updateCL(group);
    }

    public final void addLocalContact(Contact contact) {
        cl_addContact(contact);
    }
    public final void removeLocalContact(Contact contact) {
        if (null == contact) {
            return;
        }
        boolean inCL = inContactList(contact);
        if (inCL) {
            cl_removeContact(contact);
        }
        if (contact.hasChat()) {
            ChatHistory.instance.unregisterChat(ChatHistory.instance.getChat(contact));
        }
        if (inCL) {
            if (isConnected()) {
                s_removedContact(contact);
            }
            needSave();
        }
    }
    ///////////////////////////////////////////////////////////////////////////

    public final long getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }
    private void setLastStatusChangeTime() {
        lastStatusChangeTime = Jimm.getCurrentGmtTime();
    }

    private boolean isEmptyMessage(String text) {
        for (int i = 0; i < text.length(); ++i) {
            if (' ' < text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public final void addMessage(Message message) {
        addMessage(message, false);
    }
    public final void addMessage(Message message, boolean silent) {
        Contact contact = (Contact)getItemByUIN(message.getSndrUin());
        // #sijapp cond.if modules_ANTISPAM is "true" #
        if ((null == contact) && AntiSpam.isSpam(this, message)) {
            return;
        }
        // #sijapp cond.end #

        // Add message to contact
        if (null == contact) {
            // Create a temporary contact entry if no contact entry could be found
            // do we have a new temp contact
            contact = createTempContact(message.getSndrUin());
            addTempContact(contact);
        }
        if (null == contact) {
            return;
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        if (contact.inIgnoreList()) {
            return;
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_SOUND is "true" #
        beginTyping(contact, false);
        // #sijapp cond.end #
        boolean isPlain = (message instanceof PlainMessage);
        if (isPlain && isEmptyMessage(message.getText())) {
            return;
        }

        // Adds a message to the message display
        Chat chat = getChat(contact);
        chat.addMessage(message, !silent && !message.isWakeUp());
        if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                if (autoGrand.contains(contact.getUserId())) {
                    grandAuth(contact.getUserId());
                    autoGrand.removeElement(contact.getUserId());
                    chat.resetAuthRequests();
                }
            }
        }
        if (!silent) {
            addMessageNotify(chat, contact, message);
            if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
                ui_updateContact(contact);
            }
        }
        ContactList.getInstance().receivedMessage(contact);
    }
    private void addMessageNotify(Chat chat, Contact contact, Message message) {
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = isBlogBot(contact.getUserId());
        boolean isHuman = isBlog || chat.isHuman() || !contact.isSingleUserContact();
        if (isBot(contact)) {
            isHuman = false;
        }
        boolean isMention = false;
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isPersonal && !message.isOffline() && (contact instanceof JabberContact)) {
            String msg = message.getText();
            String myName = ((JabberServiceContact)contact).getMyName();
            // regexp: "^nick. "
            isPersonal = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMention = Chat.isHighlight(msg, myName);
        }
        // #sijapp cond.end #

        boolean isPaused = false;
        // #sijapp cond.if target is "MIDP2" #
        isPaused = Jimm.isPaused() && ContactList.getInstance().isCollapsible();
        if (isPaused && isPersonal && isHuman) {
            if (Options.getBoolean(Options.OPTION_BRING_UP)) {
                Jimm.maximize(getChat(contact));
                isPaused = false;
            }
        }
//        if (isPaused && isPlainMsg && isSingleUser) {
//            Jimm.getJimm().addEvent(message.getName(),
//                    message.getProcessedText(), null);
//        }
        // #sijapp cond.end #

        if (!isPaused && isHuman) {
            if (isPersonal) {
                ContactList.getInstance().setActiveContact(contact);
            }
            // #sijapp cond.if modules_LIGHT is "true" #
            if (isPersonal || isMention) {
                CustomLight.setLightMode(CustomLight.ACTION_MESSAGE);
            }
            // #sijapp cond.end#
        }

        // #sijapp cond.if modules_SOUND is "true" #
        if (message.isOffline()) {
            // Offline messages don't play sound

        } else if (isPersonal) {
            if (contact.isSingleUserContact()
                    && contact.isAuth() && !contact.isTemp()
                    && message.isWakeUp()) {
                playNotification(Notify.NOTIFY_ALARM);

            } else if (isBlog) {
                playNotification(Notify.NOTIFY_BLOG);

            } else if (isHuman) {
                playNotification(Notify.NOTIFY_MESSAGE);
            }

            // #sijapp cond.if protocols_JABBER is "true" #
        } else if (isMention) {
            playNotification(Notify.NOTIFY_MULTIMESSAGE);
            // #sijapp cond.end #
        }
        // #sijapp cond.end#
    }
    protected boolean isBlogBot(String userId) {
        return false;
    }
    public final boolean isBot(Contact contact) {
        return contact.getName().endsWith("-bot");
    }

    public final void setAuthResult(String uin, boolean auth) {
        Contact c = getItemByUIN(uin);
        if (null == c) {
            return;
        }
        if (auth == c.isAuth()) {
            return;
        }
        c.setBooleanValue(Contact.CONTACT_NO_AUTH, !auth);
        if (!auth) {
            c.setOfflineStatus();
        }
        ui_changeContactStatus(c);
    }

    public final void connect() {
        isReconnect = false;
        reconnect_attempts = RECONNECT_COUNT;
        profile.isConnected = true;
        Options.saveAccount(profile);
        disconnect(false);
        startConnection();
        setLastStatusChangeTime();
    }

    public final boolean isReconnect() {
        return isReconnect;
    }
    public final void playNotification(int type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (!getStatusInfo().isAway(getProfile().statusIndex)
                || Options.getBoolean(Options.OPTION_NOTIFY_IN_AWAY)) {
            Notify.getSound().playSoundNotification(type);
        }
        // #sijapp cond.end #
    }

    public final void processException(JimmException e) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("process exception: " + e.getMessage());
        // #sijapp cond.end #
        // #sijapp cond.if modules_ANDROID is "true" #
        if (!ru.net.jimm.JimmActivity.getInstance().isNetworkAvailable()) {
            e = new JimmException(123, 0);
        }
        // #sijapp cond.end#
        if (e.isReconnectable()) {
            reconnect_attempts--;
            if (0 < reconnect_attempts) {
                if (isConnected() && !isConnecting()) {
                    isReconnect = true;
                }
                try {
                    int iter = RECONNECT_COUNT - reconnect_attempts;
                    int sleep = Math.min(iter * 10, 2 * 60);
                    Thread.sleep(sleep * 1000);
                } catch (Exception ignored) {
                }
                if (isConnected() || isConnecting()) {
                    disconnect(false);
                    // #sijapp cond.if modules_SOUND is "true" #
                    playNotification(Notify.NOTIFY_RECONNECT);
                    // #sijapp cond.end #
                    startConnection();
                }
                return;
            }
        }

        disconnect(false);
        showException(e);
    }

    public final void showException(JimmException e) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_ERROR);
        // #sijapp cond.end#
        Jimm.unlockJimm();
        getContactList().activate(getUserId() + "\n" + e.getMessage());
    }

    /**
     *  Release all resources used by the protocol.
     */
    public final void dismiss() {
        disconnect(false);
        userCloseConnection();
        ChatHistory.instance.unregisterChats(this);
        safeSave();
        sortedContacts = null;
        sortedGroups = null;
        profile = null;
        contacts = null;
        groups = null;
        // #sijapp cond.if modules_MULTI is "true" #
        branch = null;
        // #sijapp cond.end #
    }

    public void autoDenyAuth(String uin) {
    }

    public abstract void saveUserInfo(UserInfo info);

    public abstract byte[] getStatusList();

    public boolean isMeVisible(Contact to) {
        return true;
    }
    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
    }
    public final void sendTypingNotify(Contact to, boolean isTyping) {
        if (isConnected() && isMeVisible(to)
                && (1 < Options.getInt(Options.OPTION_TYPING_MODE))) {
            s_sendTypingNotify(to, isTyping);
        }
    }

    protected abstract void sendSomeMessage(PlainMessage msg);
    public final void sendMessage(Contact to, String msg, boolean addToChat) {
        msg = StringConvertor.trim(msg);
        if (StringConvertor.isEmpty(msg)) {
            return;
        }
        PlainMessage plainMsg = new PlainMessage(this, to, Jimm.getCurrentGmtTime(), msg);
        if (isConnected()) {
            // #sijapp cond.if protocols_JABBER is "true" #
            if (msg.startsWith("/") && !msg.startsWith("/me ") && !msg.startsWith("/wakeup") && (to instanceof JabberContact)) {
                boolean cmdExecuted = ((JabberContact)to).execCommand(this, msg);
                if (!cmdExecuted) {
                    String text = JLocale.getString("jabber_command_not_found");
                    SystemNotice notice = new SystemNotice(this, SystemNotice.SYS_NOTICE_MESSAGE, to.getUserId(), text);
                    getChat(to).addMessage(notice, false);
                }
                return;
            }
            // #sijapp cond.end #
            sendSomeMessage(plainMsg);
        }
        if (addToChat) {
            getChat(to).addMyMessage(plainMsg);
        }
    }

    protected void doAction(Contact contact, int cmd) {
    }

    public void showUserInfo(Contact contact) {
    }
    public void showStatus(Contact contact) {
    }

    public final void setContactStatus(Contact contact, byte status, String text) {
        byte prev = contact.getStatusIndex();
        contact.setStatus(status, text);
        if (isConnected() && !isConnecting()) {
            byte curr = contact.getStatusIndex();
            if ((prev == curr) || !contact.isSingleUserContact()) {
                return;
            }
            // #sijapp cond.if modules_SOUND is "true" #
            if (!getStatusInfo().isAway(curr) && getStatusInfo().isAway(prev)) {
                playNotification(Notify.NOTIFY_ONLINE);
            }
            // #sijapp cond.end #
            contact.showTopLine(getStatusInfo().getName(curr));
        }
    }

    public String getUniqueUserId(Contact contact) {
        return contact.getUserId();
    }
    public final Chat getChat(Contact contact) {
        Chat chat = ChatHistory.instance.getChat(contact);
        if (null == chat) {
            chat = new Chat(this, contact);
            if (!inContactList(contact)) {
                contact.setTempFlag(true);
                addLocalContact(contact);
            }
            if (!chat.empty() || !contact.isSingleUserContact()) {
                ChatHistory.instance.registerChat(chat);
            }
        }
        return chat;
    }
}
