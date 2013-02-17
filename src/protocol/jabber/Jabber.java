/*
 * Jabber.java
 *
 * Created on 12 Июль 2008 г., 19:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import java.util.Vector;
import jimm.*;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.search.*;
import jimm.ui.form.FormListener;
import jimm.ui.form.GraphForm;
import jimm.ui.menu.MenuModel;
import jimm.ui.menu.Select;
import jimm.util.JLocale;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class Jabber extends Protocol implements FormListener {

    private JabberXml connection;
    private Vector rejoinList = new Vector();
    private String resource;
    private ServiceDiscovery disco = null;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public static final JabberXStatus xStatus = new JabberXStatus();
    // #sijapp cond.end#
    private final Vector bots = new Vector();

    public Jabber() {
    }

    protected void initStatusInfo() {
        bots.addElement("juick@juick.com");
        bots.addElement("psto@psto.net");

        byte type = getProfile().protocolType;
        ImageList icons = createStatusIcons(type);
        final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, 6, -1, 5, -1, -1, 1};
        info = new StatusInfo(icons, statusIconIndex);

        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatusInfo = Jabber.xStatus.getInfo();
        // #sijapp cond.end #

        // #sijapp cond.if modules_CLIENTS is "true" #
        clientInfo = JabberClient.get();
        // #sijapp cond.end #
    }

    private ImageList createStatusIcons(byte type) {
        // #sijapp cond.if modules_MULTI is "true" #
        String file = "jabber";
        switch (type) {
            case Profile.PROTOCOL_GTALK:
                file = "gtalk";
                break;
            case Profile.PROTOCOL_FACEBOOK:
                file = "facebook";
                break;
            case Profile.PROTOCOL_LJ:
                file = "livejournal";
                break;
            case Profile.PROTOCOL_YANDEX:
                file = "ya";
                break;
            case Profile.PROTOCOL_VK:
                file = "vk";
                break;
            case Profile.PROTOCOL_QIP:
                file = "qip";
                break;
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                file = "o" + "k";
                break;
        }
        ImageList icons = ImageList.createImageList("/" + file + "-status.png");
        if (0 < icons.size()) {
            return icons;
        }
        // #sijapp cond.end #
        return ImageList.createImageList("/jabber-status.png");
    }

    public void addRejoin(String jid) {
        if (!rejoinList.contains(jid)) {
            rejoinList.addElement(jid);
        }
    }

    public void removeRejoin(String jid) {
        rejoinList.removeElement(jid);
    }

    public void rejoin() {
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String) rejoinList.elementAt(i);
            JabberServiceContact conf = (JabberServiceContact) getItemByUIN(jid);
            if ((null != conf) && !conf.isOnline()) {
                join(conf);
            }
        }
    }

    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    public final boolean isBlogBot(String jid) {
        return -1 < bots.indexOf(jid);
    }

    protected void startConnection() {
        connection = new JabberXml();
        connection.setJabber(this);
        connection.start();
    }

    JabberXml getConnection() {
        return connection;
    }

    public boolean hasS2S() {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        // #sijapp cond.end #
        return true;
    }

    public boolean hasVCardEditor() {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        // #sijapp cond.end #
        return true;
    }

    protected final void userCloseConnection() {
        rejoinList.removeAllElements();
    }

    protected final void closeConnection() {
        JabberXml c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = groups.size() - 1; i >= 0; --i) {
                Group group = (Group) groups.elementAt(i);
                if (group.getId() == id) {
                    id = -1;
                    break;
                }
            }
            if (0 <= id) {
                return id;
            }
        }
    }
    public static final String GENERAL_GROUP = "group_general";
    public static final String GATE_GROUP = "group_transports";
    public static final String CONFERENCE_GROUP = "group_conferences";

    public final Group createGroup(String name) {
        Group group = new Group(name);
        group.setGroupId(getNextGroupId());
        int mode = Group.MODE_FULL_ACCESS;
        if (JLocale.getString(Jabber.CONFERENCE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JLocale.getString(Jabber.GATE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_BOTTOM;
        }
        group.setMode(mode);
        return group;
    }

    /**
     * Create or get group.
     *
     * WARNING! This method adds new group to list of group.
     */
    public final Group getOrCreateGroup(String groupName) {
        if (StringConvertor.isEmpty(groupName)) {
            return null;
        }
        Group group = getGroup(groupName);
        if (null == group) {
            group = createGroup(groupName);
            addGroup(group);
        }
        return group;
    }

    protected final Contact createContact(String jid, String name) {
        name = (null == name) ? jid : name;
        jid = Jid.realJidToJimmJid(jid);

        boolean isGate = (-1 == jid.indexOf('@'));
        boolean isConference = Jid.isConference(jid);
        if (isGate || isConference) {
            JabberServiceContact c = new JabberServiceContact(jid, name);
            if (c.isConference()) {
                c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
                c.setMyName(getDefaultName());

            } else if (isConference /* private */) {
                JabberServiceContact conf = (JabberServiceContact) getItemByUIN(Jid.getBareJid(jid));
                if (null != conf) {
                    c.setPrivateContactStatus(conf);
                    c.setMyName(conf.getMyName());
                }
            }
            return c;
        }

        return new JabberContact(jid, name);
    }

    private String getDefaultName() {
        // FIXME: NullPointer
        String nick = getProfile().nick;
        if (StringConvertor.isEmpty(nick)) {
            return Jid.getNick(getUserId());
        }
        return nick;
    }

    protected void sendSomeMessage(PlainMessage msg) {
        getConnection().sendMessage(msg);
    }

    protected final void s_searchUsers(Search cont) {
        // FIXME
        UserInfo userInfo = new UserInfo(this);
        userInfo.uin = cont.getSearchParam(Search.UIN);
        if (null != userInfo.uin) {
            cont.addResult(userInfo);
        }
        cont.finished();
    }
    public final static int PRIORITY = 50;

    protected void s_updateOnlineStatus() {
        connection.setStatus(getProfile().statusIndex, "", PRIORITY);
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String) rejoinList.elementAt(i);
            JabberServiceContact c = (JabberServiceContact) getItemByUIN(jid);
            if (null != c) {
                connection.sendPresence(c);
            }
        }
    }

    void setConfContactStatus(JabberServiceContact conf, String resource,
            byte status, String statusText, int role) {
        conf.__setStatus(resource, role, status, statusText);
    }

    void setContactStatus(JabberContact c, String resource, byte status, String text, int priority) {
        c.__setStatus(resource, priority, status, text);
    }

    protected final void s_addedContact(Contact contact) {
        connection.updateContact((JabberContact) contact);
    }

    protected final void s_addGroup(Group group) {
    }

    protected final void s_removeGroup(Group group) {
    }

    protected final void s_removedContact(Contact contact) {
        if (!contact.isTemp()) {
            boolean unregister = Jid.isGate(contact.getUserId())
                    && !Jid.getDomain(getUserId()).equals(contact.getUserId());
            if (unregister) {
                getConnection().unregister(contact.getUserId());
            }
            connection.removeContact(contact.getUserId());
            if (unregister) {
                getConnection().removeGateContacts(contact.getUserId());
            }
        }
        if (contact.isOnline() && !contact.isSingleUserContact()) {
            getConnection().sendPresenceUnavailable(contact.getUserId());
        }
    }

    protected final void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateContacts(contacts);
    }

    protected final void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        connection.updateContact((JabberContact) contact);
    }

    protected final void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((JabberContact) contact);
    }

    public void grandAuth(String uin) {
        connection.sendSubscribed(uin);
    }

    public void denyAuth(String uin) {
        connection.sendUnsubscribed(uin);
    }

    public void autoDenyAuth(String uin) {
        denyAuth(uin);
    }

    public void requestAuth(String uin) {
        connection.requestSubscribe(uin);
    }

    private String getYandexDomain(String domain) {
        boolean nonPdd = "ya.ru".equals(domain)
                || "narod.ru".equals(domain)
                || domain.startsWith("yandex.");
        return nonPdd ? "xmpp.yandex.ru" : "domain-xmpp.ya.ru";
    }
    String getDefaultServer(String domain) {
        // #sijapp cond.if modules_MULTI is "true" #
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_GTALK:
                return "talk.google.com";
            case Profile.PROTOCOL_FACEBOOK:
                return "chat.facebook.com";
            case Profile.PROTOCOL_LJ:
                return "livejournal.com";
            case Profile.PROTOCOL_YANDEX:
                return getYandexDomain(domain);
            case Profile.PROTOCOL_VK:
                return "vkmessenger.com";
            case Profile.PROTOCOL_QIP:
                return "webim.qip.ru";
        }
        // #sijapp cond.end #
        if ("jabber.ru".equals(domain)) {
            return domain;
        }
        if ("xmpp.ru".equals(domain)) {
            return domain;
        }
        if ("ya.ru".equals(domain)) {
            return "xmpp.yandex.ru";
        }
        if ("gmail.com".equals(domain)) {
            return "talk.google.com";
        }
        if ("qip.ru".equals(domain)) {
            return "webim.qip.ru";
        }
        if ("livejournal.com".equals(domain)) {
            return "livejournal.com";
        }
        if ("api.com".equals(domain)) {
            return "vkmessenger.com";
        }
        if ("vkontakte.ru".equals(domain)) {
            return "vkmessenger.com";
        }
        if ("chat.facebook.com".equals(domain)) {
            return domain;
        }
        return null;
    }

    protected String processUin(String uin) {
        resource = Jid.getResource(uin, "Jimm");
        return Jid.getBareJid(uin);
    }

    public String getResource() {
        return resource;
    }

    protected void s_setPrivateStatus() {
    }

    void removeMe(String uin) {
        connection.sendUnsubscribed(uin);
    }

    public ServiceDiscovery getServiceDiscovery() {
        if (null == disco) {
            disco = new ServiceDiscovery();
        }
        disco.setProtocol(this);
        return disco;
    }

    public String getUserIdName() {
        return "JID";
    }

    // #sijapp cond.if modules_FILES is "true"#
    public void sendFile(FileTransfer transfer, String filename, String description) {
        getConnection().setIBB(new IBBFileTransfer(filename, description, transfer));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_XSTATUSES is "true" #

    protected void s_updateXStatus() {
        connection.setXStatus();
    }
    // #sijapp cond.end #

    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }
    private static final byte[] statuses = {
        StatusInfo.STATUS_CHAT,
        StatusInfo.STATUS_ONLINE,
        StatusInfo.STATUS_AWAY,
        StatusInfo.STATUS_XA,
        StatusInfo.STATUS_DND};

    public byte[] getStatusList() {
        return statuses;
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to instanceof JabberServiceContact) {
            return;
        }
        if (Profile.PROTOCOL_LJ == getProfile().protocolType) {
            return;
        }
        JabberContact c = (JabberContact) to;
        JabberContact.SubContact s = c.getCurrentSubContact();
        if (null != s) {
            connection.sendTypingNotify(to.getUserId() + "/" + s.resource, isTyping);
        }
    }

    public boolean isContactOverGate(String jid) {
        if (Jid.isGate(jid)) {
            return false;
        }
        Vector all = getContactItems();
        for (int i = all.size() - 1; 0 <= i; --i) {
            JabberContact c = (JabberContact) all.elementAt(i);
            if (Jid.isGate(c.getUserId())) {
                if (jid.endsWith(c.getUserId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void leave(JabberServiceContact conf) {
        if (conf.isOnline()) {
            getConnection().sendPresenceUnavailable(conf.getUserId() + "/" + conf.getMyName());
            conf.nickOffline(this, conf.getMyName(), 0, null);

            // Remove private contacts
            Vector all = getContactItems();
            String conferenceJid = conf.getUserId() + '/';
            for (int i = all.size() - 1; 0 <= i; --i) {
                JabberContact c = (JabberContact) all.elementAt(i);
                if (c.getUserId().startsWith(conferenceJid) && !c.hasUnreadMessage()) {
                    removeContact(c);
                }
            }
        }
    }

    public void join(JabberServiceContact c) {
        String jid = c.getUserId();
        if (!c.isOnline()) {
            setContactStatus(c, c.getMyName(), StatusInfo.STATUS_ONLINE, "", 0);
            c.doJoining();
        }

        connection.sendPresence(c);
        String password = c.getPassword();
        if (Jid.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            connection.sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            connection.sendMessage(nickserv, "IDENTIFY " + password);
        }
    }

    protected void doAction(Contact c, int cmd) {
        JabberContact contact = (JabberContact) c;
        switch (cmd) {
            case JabberServiceContact.GATE_CONNECT:
                getConnection().sendPresence((JabberServiceContact) contact);
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_DISCONNECT:
                getConnection().sendPresenceUnavailable(c.getUserId());
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_REGISTER:
                getConnection().register(c.getUserId());
                break;

            case JabberServiceContact.GATE_UNREGISTER:
                getConnection().unregister(c.getUserId());
                getConnection().removeGateContacts(c.getUserId());
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_ADD:
                Search s = this.getSearchForm();
                s.setJabberGate(c.getUserId());
                s.show("");
                break;

            case JabberServiceContact.USER_MENU_USERS_LIST:
                if (contact.isOnline() || !isConnected()) {
                    new ConferenceParticipants(this, (JabberServiceContact) c).show();
                } else {
                    ServiceDiscovery sd = getServiceDiscovery();
                    sd.setServer(contact.getUserId());
                    sd.showIt();
                }
                break;

            case JabberServiceContact.CONFERENCE_CONNECT:
                join((JabberServiceContact) c);
                getChat(c).activate();
                break;

            case JabberServiceContact.CONFERENCE_OPTIONS:
                showOptionsForm((JabberServiceContact) c);
                break;

            case JabberServiceContact.CONFERENCE_OWNER_OPTIONS:
                connection.requestOwnerForm(c.getUserId());
                break;

            case JabberServiceContact.CONFERENCE_DISCONNECT:
                leave((JabberServiceContact) c);
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.CONFERENCE_ADD:
                addContact(c);
                ContactList.getInstance().activate();
                break;

            case JabberContact.USER_MENU_CONNECTIONS:
                showListOfSubcontacts(contact);
                break;

            case JabberContact.USER_MENU_ADHOC:
                AdHoc adhoc = new AdHoc(this, contact);
                adhoc.show();
                break;

            case JabberContact.USER_MENU_REMOVE_ME:
                removeMe(c.getUserId());
                ContactList.getInstance().activate();
                break;

        }
    }

    private void showListOfSubcontacts(JabberContact c) {
        MenuModel sublist = new MenuModel();
        int selected = 0;
        StatusInfo statusInfo = getStatusInfo();
        for (int i = 0; i < c.subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) c.subcontacts.elementAt(i);
            sublist.addRawItem(contact.resource, statusInfo.getIcon(contact.status), i);
            if (contact.resource.equals(c.currentResource)) {
                selected = i;
            }
        }
        sublist.setDefaultItemCode(selected);
        sublist.setActionListener(c);
        new Select(sublist).show();
    }

    public void showUserInfo(Contact contact) {
        if (!contact.isSingleUserContact()) {
            doAction(contact, JabberContact.USER_MENU_USERS_LIST);
            return;
        }

        String realJid = contact.getUserId();
        if (Jid.isConference(realJid) && (-1 != realJid.indexOf('/'))) {
            JabberServiceContact conference = (JabberServiceContact) getItemByUIN(Jid.getBareJid(realJid));
            String r = conference.getRealJid(Jid.getResource(realJid, ""));
            if (!StringConvertor.isEmpty(r)) {
                realJid = r;
            }
        }
        UserInfo data;
        if (isConnected()) {
            data = getConnection().getUserInfo(contact);
            data.uin = realJid;
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this, contact.getUserId());
            data.uin = realJid;
            data.nick = contact.getName();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile();
    }

    public void updateStatusView(StatusView statusView, Contact contact) {
        if (statusView.getContact() != contact) {
            return;
        }
        String statusMessage = contact.getStatusText();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        String xstatusMessage = "";
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            xstatusMessage = contact.getXStatusText();
            String s = StringConvertor.notNull(statusMessage);
            if (!StringConvertor.isEmpty(xstatusMessage)
                    && s.startsWith(xstatusMessage)) {
                xstatusMessage = statusMessage;
                statusMessage = null;
            }
        }
        // #sijapp cond.end #

        statusView.initUI();
        statusView.addContactStatus();
        statusView.addStatusText(statusMessage);

        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            statusView.addXStatus();
            statusView.addStatusText(xstatusMessage);
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_CLIENTS is "true" #
        if (contact.isSingleUserContact()) {
            statusView.addClient();
        }
        // #sijapp cond.end #
        statusView.addTime();

        statusView.update();
    }

    public void showStatus(Contact contact) {
        StatusView statusView = ContactList.getInstance().getStatusView();
        try {
            if (contact.isOnline() && contact.isSingleUserContact()) {
                String jid = contact.getUserId();
                if (!(contact instanceof JabberServiceContact)) {
                    jid += '/' + ((JabberContact) contact).getCurrentSubContact().resource;
                }
                getConnection().requestClientVersion(jid);
            }
        } catch (Exception ignored) {
        }

        statusView.init(this, contact);
        updateStatusView(statusView, contact);
        statusView.showIt();
    }

    public String getUniqueUserId(Contact c) {
        String jid = c.getUserId();
        if (isContactOverGate(jid)) {
            return Jid.getNick(jid).replace('%', '@');
        }
        return jid.replace('%', '@');
    }
    // -----------------------------------------------------------------------
    private static GraphForm enterData = null;
    private static JabberServiceContact enterConf = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;

    void showOptionsForm(JabberServiceContact c) {
        enterConf = c;
        enterData = new GraphForm("conference", "ok", "cancel", this);
        enterData.addTextField(NICK, "nick", c.getMyName(), 32);
        enterData.addTextField(PASSWORD, "password", c.getPassword(), 32);
        if (!c.isTemp()) {
            enterData.addCheckBox(AUTOJOIN, "autojoin", c.isAutoJoin());
        }
        enterData.show();
        if (!Jid.isIrcConference(c.getUserId())) {
            getConnection().requestConferenceInfo(c.getUserId());
        }
    }

    void setConferenceInfo(String jid, String description) {
        if ((null != enterData) && enterConf.getUserId().equals(jid)) {
            enterData.addString("description", description);
        }
    }

    public void formAction(GraphForm form, boolean apply) {
        if (enterData == form) {
            if (apply) {
                if (enterConf.isConference()) {
                    String oldNick = enterConf.getMyName();
                    enterConf.setMyName(enterData.getTextFieldValue(NICK));
                    enterConf.setAutoJoin(!enterConf.isTemp() && enterData.getCheckBoxValue(AUTOJOIN));
                    enterConf.setPassword(enterData.getTextFieldValue(PASSWORD));

                    boolean needUpdate = !enterConf.isTemp();
                    if (needUpdate) {
                        getConnection().saveConferences();
                    }
                    if (enterConf.isOnline() && !oldNick.equals(enterConf.getMyName())) {
                        join(enterConf);
                    }
                }
                ContactList.getInstance().activate();

            } else {
                enterData.back();
            }
            enterData = null;
            enterConf = null;
        }
    }
}
// #sijapp cond.end #
