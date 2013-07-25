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

import java.util.Hashtable;
import java.util.Vector;
import jimm.*;
import jimm.chat.message.PlainMessage;
import jimm.comm.*;
import jimm.search.*;
import jimmui.view.form.FormListener;
import jimmui.view.form.GraphForm;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimm.util.JLocale;
import protocol.*;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.ui.StatusView;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class Jabber extends Protocol implements FormListener {

    private JabberXml connection;
    private Vector<String> rejoinList = new Vector<String>();
    private String resource;
    private ServiceDiscovery disco = null;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public static final JabberXStatus xStatus = new JabberXStatus();
    // #sijapp cond.end#
    private static final String[] bots = {"juick@juick.com", "psto@psto.net"};

    public Jabber() {
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
            JabberServiceContact conf = (JabberServiceContact) getItemByUID(jid);
            if ((null != conf) && !conf.isOnline()) {
                join(conf);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    @Override
    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    @Override
    public final boolean isBlogBot(String jid) {
        for (int i = 0; i < bots.length; ++i) {
            if (bots[i].equals(jid)) return true;
        }
        return false;
    }

    @Override
    protected void startConnection() {
        connection = new JabberXml();
        connection.setJabber(this);
        connection.start();
    }

    JabberXml getConnection() {
        return connection;
    }

    public boolean hasS2S() {
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        return true;
    }

    @Override
    public boolean hasVCardEditor() {
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        return true;
    }

    @Override
    protected final void userCloseConnection() {
        rejoinList.removeAllElements();
    }

    @Override
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
            if (null != roster) {
                for (int i = roster.groups.size() - 1; i >= 0; --i) {
                    Group group = (Group) roster.groups.elementAt(i);
                    if (group.getId() == id) {
                        id = -1;
                        break;
                    }
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

    @Override
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
                c.setMyName(getDefaultName());

            } else if (isConference /* private */) {
                JabberServiceContact conf = (JabberServiceContact) getItemByUID(Jid.getBareJid(jid));
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

    @Override
    protected void sendSomeMessage(PlainMessage msg) {
        getConnection().sendMessage(msg);
    }

    @Override
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

    @Override
    protected void s_updateOnlineStatus() {
        connection.setStatus(getProfile().statusIndex, "", PRIORITY);
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String) rejoinList.elementAt(i);
            JabberServiceContact c = (JabberServiceContact) getItemByUID(jid);
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

    @Override
    protected final void s_addedContact(Contact contact) {
        connection.updateContact((JabberContact) contact);
    }

    @Override
    protected final void s_addGroup(Group group) {
    }

    @Override
    protected final void s_removeGroup(Group group) {
    }

    @Override
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

    @Override
    protected final void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateContacts(roster.contacts);
    }

    @Override
    protected final void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        connection.updateContact((JabberContact) contact);
    }

    @Override
    protected final void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((JabberContact) contact);
    }

    @Override
    public void grandAuth(String uin) {
        connection.sendSubscribed(uin);
    }

    @Override
    public void denyAuth(String uin) {
        connection.sendUnsubscribed(uin);
    }

    @Override
    public void autoDenyAuth(String uin) {
        denyAuth(uin);
    }

    @Override
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
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_GTALK:
                return "talk.google.com";
            case Profile.PROTOCOL_FACEBOOK:
                return "chat.facebook.com";
            case Profile.PROTOCOL_LJ:
                return "livejournal.com";
            case Profile.PROTOCOL_YANDEX:
                return getYandexDomain(domain);
            case Profile.PROTOCOL_QIP:
                return "webim.qip.ru";
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return "xmpp.odnoklassniki.ru";
        }
        return (String) defaultDomains.get(domain);
    }
    private static final Hashtable defaultDomains = new Hashtable();
    static {
        defaultDomains.put("livejournal.com", "livejournal.com");
        defaultDomains.put("chat.facebook.com", "chat.facebook.com");
        defaultDomains.put("qip.ru", "webim.qip.ru");
        defaultDomains.put("gmail.com", "talk.google.com");
        defaultDomains.put("ya.ru", "xmpp.yandex.ru");
        defaultDomains.put("xmpp.ru", "xmpp.ru");
        defaultDomains.put("jabber.ru", "jabber.ru");
    }

    @Override
    protected String processUin(String uin) {
        resource = Jid.getResource(uin, "Jimm");
        return Jid.getBareJid(uin);
    }

    public String getResource() {
        return resource;
    }

    @Override
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

    @Override
    public String getUserIdName() {
        return "JID";
    }

    // #sijapp cond.if modules_FILES is "true"#
    @Override
    public void sendFile(FileTransfer transfer, String filename, String description) {
        getConnection().setIBB(new IBBFileTransfer(filename, description, transfer));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_XSTATUSES is "true" #
    @Override
    protected void s_updateXStatus() {
        connection.setXStatus();
    }
    // #sijapp cond.end #

    @Override
    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }

    @Override
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

    @Override
    public void doAction(Contact c, int cmd) {
        JabberContact contact = (JabberContact) c;
        switch (cmd) {
            case JabberServiceContact.GATE_CONNECT:
                getConnection().sendPresence((JabberServiceContact) contact);
                Jimm.getJimm().getCL().activate();
                break;

            case JabberServiceContact.GATE_DISCONNECT:
                getConnection().sendPresenceUnavailable(c.getUserId());
                Jimm.getJimm().getCL().activate();
                break;

            case JabberServiceContact.GATE_REGISTER:
                getConnection().register(c.getUserId());
                break;

            case JabberServiceContact.GATE_UNREGISTER:
                getConnection().unregister(c.getUserId());
                getConnection().removeGateContacts(c.getUserId());
                Jimm.getJimm().getCL().activate();
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
                Jimm.getJimm().getChatUpdater().activate(getChatModel(c));
                break;

            case JabberServiceContact.CONFERENCE_OPTIONS:
                showOptionsForm((JabberServiceContact) c);
                break;

            case JabberServiceContact.CONFERENCE_OWNER_OPTIONS:
                connection.requestOwnerForm(c.getUserId());
                break;

            case JabberServiceContact.CONFERENCE_DISCONNECT:
                leave((JabberServiceContact) c);
                Jimm.getJimm().getCL().activate();
                break;

            case JabberServiceContact.CONFERENCE_ADD:
                addContact(c);
                Jimm.getJimm().getCL().activate();
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
                Jimm.getJimm().getCL().activate();
                break;

        }
    }

    private void showListOfSubcontacts(JabberContact c) {
        MenuModel sublist = new MenuModel();
        int selected = 0;
        StatusInfo statusInfo = InfoFactory.factory.getStatusInfo(this);
        for (int i = 0; i < c.subContacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) c.subContacts.elementAt(i);
            sublist.addRawItem(contact.resource, statusInfo.getIcon(contact.status), i);
            if (contact.resource.equals(c.currentResource)) {
                selected = i;
            }
        }
        sublist.setDefaultItemCode(selected);
        sublist.setActionListener(c);
        new Select(sublist).show();
    }

    @Override
    public void showUserInfo(Contact contact) {
        if (!contact.isSingleUserContact()) {
            doAction(contact, JabberContact.USER_MENU_USERS_LIST);
            return;
        }

        String realJid = contact.getUserId();
        if (Jid.isConference(realJid) && (-1 != realJid.indexOf('/'))) {
            JabberServiceContact conference = (JabberServiceContact) getItemByUID(Jid.getBareJid(realJid));
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

    @Override
    public void showStatus(Contact contact) {
        StatusView statusView = Jimm.getJimm().getStatusView();
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

    @Override
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

    @Override
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
                Jimm.getJimm().getCL().activate();

            } else {
                enterData.back();
            }
            enterData = null;
            enterConf = null;
        }
    }
}
// #sijapp cond.end #
