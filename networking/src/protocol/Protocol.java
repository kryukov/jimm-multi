            /*
 * Protocol.java
 *
 * Created on 13 Май 2008 г., 12:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import java.io.*;
import java.util.Vector;
import javax.microedition.rms.*;
import jimm.*;
import jimm.chat.message.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.io.Storage;
import jimm.modules.*;
import jimm.search.*;
import jimm.util.JLocale;
import jimmui.model.chat.ChatModel;
import jimmui.updater.RosterUpdater;
import protocol.jabber.*;
import protocol.ui.StatusInfo;

            /**
 *
 * @author vladimir
 */
abstract public class Protocol {
    protected Roster roster;
    private Profile profile;
    private String password;
    private String userid = "";
    private String rmsName = null;

    private boolean isReconnect;
    private int reconnect_attempts;
    private boolean needSave = false;

    private long lastStatusChangeTime;
    private byte progress = 100;
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private byte privateStatus = 0;
    // #sijapp cond.end #

    private final Object rosterLockObject = new Object();

    private Vector<String> autoGrand = new Vector<String>();

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
    public final void init() {
        initThat();

        // Status
        initStatus();
    }
    protected void initThat() {
    }

    public boolean hasVCardEditor() {
        return true;
    }

    public final void setContactListStub() {
        synchronized (rosterLockObject) {
            this.roster = new Roster();
        }
    }
    public final void setContactList(Vector<Group> groups, Vector<Contact> contacts) {
        setContactList(new Roster(groups, contacts), false);
    }
    public final void setContactList(Roster roster, boolean needSave) {
        Roster oldRoster;
        synchronized (rosterLockObject) {
            oldRoster = this.roster;
            if (null != oldRoster) {
                Util.removeAll(oldRoster.groups, roster.groups);
                Util.removeAll(oldRoster.contacts, roster.contacts);
            }
            this.roster = roster;
        }
        Jimm.getJimm().jimmModel.restoreContactsWithChat(this);

        if (null != getUpdater()) {
            getUpdater().updateProtocol(this, oldRoster);
        }
        if (needSave) {
            needSave();
        }
    }
    // #sijapp cond.if protocols_JABBER is "true" #
    public final void setContactListAddition(Group group) {
        if (null != getUpdater()) {
            synchronized (rosterLockObject) {
                getUpdater().addGroup(this, group);
                getUpdater().addGroup(this, null);
                getUpdater().update();
            }
        }
        needSave();
    }
    // #sijapp cond.end#

    /* ********************************************************************* */
    public final void setConnectingProgress(int percent) {
        this.progress = (byte)((percent < 0) ? 100 : percent);
        if (100 == percent) {
            reconnect_attempts = RECONNECT_COUNT;
            getUpdater().updateConnectionStatus();
        } else if (0 == percent) {
            getUpdater().updateConnectionStatus();
        }
        getUpdater().repaint();
    }
    public final boolean isConnecting() {
        return (100 != progress)
                || ((StatusInfo.STATUS_OFFLINE != profile.statusIndex) && !isConnected());
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
    public abstract void grandAuth(String userId);
    public abstract void denyAuth(String userId);
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
            setContactList(new Roster(), false);
        }
    }

    public final void needSave() {
        needSave = true;
        Jimm.getJimm().jimmModel.needRosterSave();
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
                if (null != cl) cl.closeRecordStore();
            } catch (Exception e) {
                // Do nothing
            }
        }
        return true;
    }
    // Tries to load contact list from record store
    private void load() throws Exception {
        // Initialize vectors
        Roster roster = new Roster();

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
            if (!dis.readUTF().equals(Jimm.getJimm().VERSION)) {
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
                                roster.contacts.addElement(loadContact(dis));
                                break;
                            case 1:
                                roster.groups.addElement(loadGroup(dis));
                                break;
                        }
                    }
                } catch (EOFException ignored) {
                }
            }
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.memoryUsage("clload");
            // #sijapp cond.end#
        } finally {
            // Close record store
            cl.closeRecordStore();
        }
        setContactList(roster, false);
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
        dos.writeUTF(Jimm.getJimm().VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);

        // Add version ids to the record store
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);

        // Initialize buffer
        baos.reset();

        // Iterate through all contact items
        int cItemsCount = roster.contacts.size();
        int totalCount  = cItemsCount + roster.groups.size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                saveContact(dos, (Contact)roster.contacts.elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, (Group)roster.groups.elementAt(i - cItemsCount));
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
        dis.readBoolean();//group.setExpandFlag(dis.readBoolean());
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
        out.writeBoolean(false);//out.writeBoolean(group.isExpanded());
    }

    /* ********************************************************************* */

    protected void s_removeContact(Contact contact) {}
    protected void s_removedContact(Contact contact) {}
    public final void removeContact(Contact contact) {
        // Check whether contact item is temporary
        if (contact.isTemp()) {
            // do nothing
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
        if (!roster.hasContact(contact)) {
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
        ui_updateContact(contact);
        needSave();
    }

    abstract protected void s_moveContact(Contact contact, Group to);
    public final void moveContactTo(Contact contact, Group to) {
        Group from = getGroupById(contact.getGroupId());
        s_moveContact(contact, to);
        getUpdater().removeFromGroup(this, from, contact);
        ui_addContactToGroup(contact, to);
    }
    protected void s_addContact(Contact contact) {}
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
        roster.groups.removeElement(group);
        getUpdater().removeGroup(this, group);
        needSave();
    }
    abstract protected void s_renameGroup(Group group, String name);
    public final void renameGroup(Group group, String name) {
        getUpdater().removeGroup(this, group);
        s_renameGroup(group, name);
        group.setName(name);
        getUpdater().addGroup(this, group);
        getUpdater().update(group);
        needSave();
    }
    abstract protected void s_addGroup(Group group);
    public final void addGroup(Group group) {
        s_addGroup(group);
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if ((null != roster) && -1 != Util.getIndex(roster.groups, group)) {
            DebugLog.panic("Group '" + group.getName() + "' already added");
        }
        // #sijapp cond.end #
        roster.groups.addElement(group);
        getUpdater().addGroup(this, group);
        getUpdater().update(group);
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
            userCloseConnection();
        }
        /* Reset all contacts offline */
        setStatusesOffline();
        /* Disconnect */

        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().safeSave();
        // #sijapp cond.end#
        getUpdater().update();
        getUpdater().updateConnectionStatus();
        if (user) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("disconnect " + getUserId());
            // #sijapp cond.end #
        }
    }


    abstract public Group createGroup(String name);
    abstract protected Contact createContact(String uin, String name);
    public final Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUID(uin);
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
        if (roster.groups.isEmpty()) {
            return null;
        }
        return new Search(this);
    }

    // #sijapp cond.if modules_SOUND is "true" #
    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUID(uin);
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
            ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(item);
            if (null != chat) {
                Jimm.getJimm().getChatUpdater().typing(chat, type);
            }
            Jimm.getJimm().getCL().typing(this, item, type);
        }
    }
    // #sijapp cond.end #

    protected final void setStatusesOffline() {
        for (int i = roster.contacts.size() - 1; i >= 0; --i) {
            Contact c = (Contact)roster.contacts.elementAt(i);
            c.setOfflineStatus();
        }
        getUpdater().setOffline(this);
    }

    public final Vector<Contact> getContactItems() {
        return null == roster ? null : roster.contacts;
    }
    public final Vector<Group> getGroupItems() {
        return null == roster ? null : roster.groups;
    }
    public final Contact getItemByUID(String uin) {
        return null == roster ? null : roster.getItemByUID(uin);
    }
    public final Group getGroupById(int id) {
        synchronized (rosterLockObject) {
            return null == roster ? null : roster.getGroupById(id);
        }
    }
    public final Group getGroup(Contact contact) {
        return null == roster ? null : roster.getGroup(contact);
    }

    public final Group getGroup(String name) {
        synchronized (rosterLockObject) {
            return null == roster ? null : roster.getGroup(name);
        }
    }
    public final boolean hasContact(Contact contact) {
        return (null != roster) && roster.hasContact(contact);
    }


    private ContactList getContactList() {
        return Jimm.getJimm().getCL();
    }
    private RosterUpdater getUpdater() {
        return (null == Jimm.getJimm().getCL()) ? null : Jimm.getJimm().getCL().getUpdater();
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
    public final void setStatus(int statusIndex, String msg) {
        boolean connected = StatusInfo.STATUS_OFFLINE != profile.statusIndex;
        boolean connecting = StatusInfo.STATUS_OFFLINE != statusIndex;
        if (connected && !connecting) {
            disconnect(true);
        }
        setOnlineStatus(statusIndex, msg);
        if (!connected && connecting) {
            connect();
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
        getUpdater().removeFromGroup(this, getGroupById(c.getGroupId()), c);
    }
    private void ui_addContactToGroup(Contact contact, Group group) {
        ui_removeFromAnyGroup(contact);
        contact.setGroup(group);
        getUpdater().addContactToGroup(this, group, contact);
    }

    ///////////////////////////////////////////////////////////////////////////
    public final Object getRosterLockObject() {
        return rosterLockObject;
    }
    ///////////////////////////////////////////////////////////////////////////
    public final void ui_changeContactStatus(Contact contact) {
        getContactList().changeContactStatus(this, contact);
    }
    public final void ui_updateContact(Contact contact) {
        getUpdater().updateContact(this, getGroup(contact), contact);
    }

    private void cl_addContact(Contact contact) {
        if (null == contact) {
            return;
        }
        Group g = getGroup(contact);
        boolean hasnt = !roster.hasContact(contact);
        if (hasnt) {
            roster.contacts.addElement(contact);
        }
        ui_addContactToGroup(contact, g);
    }

    public final void addLocalContact(Contact contact) {
        cl_addContact(contact);
    }
    public final void removeLocalContact(Contact contact) {
        if (null == contact) {
            return;
        }
        boolean inCL = roster.hasContact(contact);
        if (inCL) {
            roster.contacts.removeElement(contact);
            ui_removeFromAnyGroup(contact);
        }
        if (contact.hasChat()) {
            Jimm.getJimm().jimmModel.unregisterChat(Jimm.getJimm().jimmModel.getChatModel(contact));
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
        Contact contact = (Contact) getItemByUID(message.getSndrUin());
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
        ChatModel chat = getChatModel(contact);
        Jimm.getJimm().getChatUpdater().addMessage(chat, message, !silent && !message.isWakeUp());
        if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.TYPE_NOTICE_AUTHREQ == notice.getMessageType()) {
                if (autoGrand.contains(contact.getUserId())) {
                    grandAuth(contact.getUserId());
                    autoGrand.removeElement(contact.getUserId());
                    chat.resetAuthRequests();
                }
            }
        }
        Jimm.getJimm().getCL().receivedMessage(this, contact, message, silent);
    }
    public boolean isBlogBot(String userId) {
        return false;
    }
    public final boolean isBot(Contact contact) {
        return contact.getName().endsWith("-bot");
    }

    public final void setAuthResult(String uin, boolean auth) {
        Contact c = getItemByUID(uin);
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
        disconnect(false);
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.connect(this);
        // #sijapp cond.else #
        startConnection();
        // #sijapp cond.end #
        setLastStatusChangeTime();
    }

    public final boolean isReconnect() {
        return isReconnect;
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
                    Jimm.getJimm().getCL().disconnected(this);
                    startConnection();
                }
                return;
            }
        }
        disconnect(false);
        setOnlineStatus(StatusInfo.STATUS_OFFLINE, null);
        showException(e);
    }

    public final void showException(JimmException e) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_ERROR);
        // #sijapp cond.end#
        Jimm.getJimm().unlockJimm();
        getContactList().activateWithMsg(getUserId() + "\n" + e.getMessage());
    }

    /**
     *  Release all resources used by the protocol.
     */
    public final void dismiss() {
        disconnect(false);
        userCloseConnection();
        safeSave();
        profile = null;
        roster.contacts = null;
        roster.groups = null;
        roster = null;
    }

    public void autoDenyAuth(String uin) {
    }

    public abstract void saveUserInfo(UserInfo info);

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
                    SystemNotice notice = new SystemNotice(this, SystemNotice.TYPE_NOTICE_MESSAGE, to.getUserId(), text);
                    Jimm.getJimm().getChatUpdater().addMessage(getChatModel(to), notice, false);
                }
                return;
            }
            // #sijapp cond.end #
            sendSomeMessage(plainMsg);
        }
        if (addToChat) {
            Jimm.getJimm().getChatUpdater().addMyMessage(getChatModel(to), plainMsg);
        }
    }

    @Deprecated
    public void doAction(Contact contact, int cmd) {
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
            Jimm.getJimm().getCL().setContactStatus(this, contact, prev, curr);
        }
    }

    public String getUniqueUserId(Contact contact) {
        return contact.getUserId();
    }
    public final ChatModel getChatModel(Contact contact) {
        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(contact);
        if (null == chat) {
            chat = Jimm.getJimm().getChatUpdater().createModel(this, contact);
            if (!roster.hasContact(contact)) {
                contact.setTempFlag(true);
                addLocalContact(contact);
            }
            if ((0 < chat.size()) || !contact.isSingleUserContact()) {
                Jimm.getJimm().jimmModel.registerChat(chat);
            }
        }
        return chat;
    }

    public Vector<Contact> getContacts(Group g) {
        int id = null == g ? Group.NOT_IN_GROUP : g.getId();
        Contact c;
        Vector<Contact> result = new Vector<Contact>();
        Vector<Contact> contacts = getContactItems();
        for (int i = 0; i < contacts.size(); ++i) {
            c = (Contact) contacts.elementAt(i);
            if (c.getGroupId() == id) {
                result.addElement(c);
            }
        }
        return result;
    }

    public final boolean isAway(byte statusIndex) {
        switch (statusIndex) {
            case StatusInfo.STATUS_OFFLINE:
            case StatusInfo.STATUS_AWAY:
            case StatusInfo.STATUS_DND:
            case StatusInfo.STATUS_XA:
            case StatusInfo.STATUS_UNDETERMINATED:
            case StatusInfo.STATUS_INVISIBLE:
            case StatusInfo.STATUS_INVIS_ALL:
            case StatusInfo.STATUS_NOT_IN_LIST:
                return true;
        }
        return false;
    }
}
