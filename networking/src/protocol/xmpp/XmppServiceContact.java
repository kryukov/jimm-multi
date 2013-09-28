/*
 * JabberServiceContact.java
 *
 * Created on 4 Январь 2009 г., 19:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.xmpp;

import java.util.Vector;

import jimm.Jimm;
import jimm.comm.StringConvertor;
import jimm.chat.message.*;
import jimmui.view.menu.*;
import jimm.util.JLocale;
import protocol.*;
import protocol.ui.ContactMenu;
import protocol.ui.StatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class XmppServiceContact extends XmppContact {
    public static final int GATE_CONNECT = 0;
    public static final int GATE_DISCONNECT = 1;
    public static final int GATE_REGISTER = 2;
    public static final int GATE_UNREGISTER = 3;
    public static final int GATE_ADD = 4;
    public static final int CONFERENCE_CONNECT = 5;
    public static final int CONFERENCE_OPTIONS = 6;
    public static final int CONFERENCE_OWNER_OPTIONS = 7;
    public static final int CONFERENCE_ADD = 9;

    private boolean isPrivate;
    private boolean isConference;
    private boolean isGate;

    private boolean autojoin;
    private String password;
    private String myNick;

    private String baseMyNick;

    public static final byte ROLE_MASK = 0x30;
    public static final byte ROLE_MODERATOR   = 2 << 4;
    public static final byte ROLE_PARTICIPANT = 1 << 4;
    public static final byte ROLE_VISITOR     = 0 << 4;
    public static final byte AFFILIATION_MASK  = 0x03;
    public static final byte AFFILIATION_OWNER  = 0;
    public static final byte AFFILIATION_ADMIN  = 1;
    public static final byte AFFILIATION_MEMBER = 2;
    public static final byte AFFILIATION_NONE   = 3;


    public void setAutoJoin(boolean auto) {
        autojoin = auto;
    }
    public boolean isAutoJoin() {
        return autojoin;
    }


    public void setPassword(String pass) {
        password = pass;
    }
    public String getPassword() {
        return password;
    }

    /** Creates a new instance of JabberContact */
    public XmppServiceContact(String jid, String name) {
        super(jid, name);

        isGate = Jid.isGate(jid);
        if (isGate) {
            return;
        }

        isPrivate = (-1 != jid.indexOf('/'));
        if (isPrivate) {
            String resource = Jid.getResource(jid, "");
            setName(resource + "@" + Jid.getNick(jid));
            return;
        }

        isConference = Jid.isConference(jid);
        if (isConference) {
            setMyName("_");
            if (jid.equals(name)) {
                setName(Jid.getNick(jid));
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void setXStatus(String id, String text) {
    }
    // #sijapp cond.end #

    public boolean isSingleUserContact() {
        return isPrivate || isGate;
    }
    public boolean isConference() {
        return isConference;
    }
    public boolean isVisibleInContactList() {
        return !isConference() || super.isVisibleInContactList();
    }
    /////////////////////////////////////////////////////////////////////////
    public final void setMyName(String nick) {
        if (!StringConvertor.isEmpty(nick)) {
            myNick = nick;
            if (!isOnline()) {
                baseMyNick = myNick;
            }
        }
    }
    public String getMyName() {
        return (isConference || isPrivate) ? myNick : null;
    }


    void doJoining() {
        setStatus(StatusInfo.STATUS_AWAY, "");
    }
    void nickChainged(Xmpp xmpp, String oldNick, String newNick) {
        if (isConference) {
            if (baseMyNick.equals(oldNick)) {
                setMyName(newNick);
                baseMyNick = newNick;
            }
            String jid = Jid.realJidToJimmJid(getUserId() + "/" + oldNick);
            XmppServiceContact c = (XmppServiceContact) xmpp.getItemByUID(jid);
            if (null != c) {
                c.nickChainged(xmpp, oldNick, newNick);
            }

        } else if (isPrivate) {
            userId = Jid.getBareJid(userId) + "/" + newNick;
            setName(newNick + "@" + Jid.getNick(getUserId()));
            setOfflineStatus();
        }
    }
    void nickOnline(Xmpp xmpp, String nick) {
        if (hasChat()) {
            xmpp.getChatModel(this).setWritable(canWrite());
        }
        SubContact sc = getExistSubContact(nick);
        if (null != sc) {
            Jimm.getJimm().getCL().setContactStatus(xmpp, this, nick, sc.status);
        }
        if (myNick.equals(nick)) {
            setStatus(StatusInfo.STATUS_ONLINE, getStatusText());
            xmpp.addRejoin(getUserId());
        }
    }
    void nickError(Xmpp xmpp, String nick, int code, String reasone) {
        boolean isConnected = (StatusInfo.STATUS_ONLINE == getStatusIndex());
        if (409 == code) {
            if (!StringConvertor.isEmpty(reasone)) {
                xmpp.addMessage(new SystemNotice(xmpp,
                        SystemNotice.TYPE_NOTICE_ERROR, getUserId(), reasone));
            }
            if (!myNick.equals(baseMyNick)) {
                myNick = baseMyNick;
                return;
            }

        } else {
            xmpp.addMessage(new SystemNotice(xmpp,
                    SystemNotice.TYPE_NOTICE_ERROR, getUserId(), reasone));
        }

        if (myNick.equals(nick)) {
            if (isConnected) {
                xmpp.leave(this);

            } else {
                nickOffline(xmpp, nick, 0, null);
            }

        } else {
            nickOffline(xmpp, nick, 0, null);
        }
    }
    void nickOffline(Xmpp xmpp, String nick, int code, String reasone) {
        if (getMyName().equals(nick)) {
            if (isOnline()) {
                xmpp.removeRejoin(getUserId());
            }
            String text = null;
            if (301 == code) {
                text = "you_was_baned";
            } else if (307 == code) {
                text = "you_was_kicked";
            } else if (404 == code) {
                text = "error";
            }
            if (null != text) {
                text = JLocale.getString(text);
                if (!StringConvertor.isEmpty(reasone)) {
                    text += " (" + reasone + ")";
                }
                text += '.';
                xmpp.addMessage(new SystemNotice(xmpp,
                        SystemNotice.TYPE_NOTICE_ERROR, getUserId(), text));
            }
            for (int i = 0; i < subContacts.size(); ++i) {
                ((SubContact) subContacts.elementAt(i)).status = StatusInfo.STATUS_OFFLINE;
            }
            String startUin = getUserId() + '/';
            Vector contactList = xmpp.getContactItems();
            for (int i = contactList.size() - 1; 0 <= i; --i) {
                Contact c = (Contact)contactList.elementAt(i);
                if (c.getUserId().startsWith(startUin)) {
                    c.setOfflineStatus();
                }
            }
            setOfflineStatus();
            xmpp.ui_changeContactStatus(this);

        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        } else {
            String event = null;
            if (301 == code) {
                event = "was_baned";
            } else if (307 == code) {
                event = "was_kicked";
            }
            if (null != event) {
                event = JLocale.getString(event);
                jimm.modules.MagicEye.addAction(xmpp, getUserId(), nick + " " + event, reasone);
            }
        // #sijapp cond.end #
        }
        if (hasChat()) {
            xmpp.getChatModel(this).setWritable(canWrite());
        }
        Jimm.getJimm().getCL().setContactStatus(xmpp, this, nick, StatusInfo.STATUS_OFFLINE);
    }

    String getRealJid(String nick) {
        for (int i = subContacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact) subContacts.elementAt(i);
        }
        SubContact sc = getExistSubContact(nick);
        return (null == sc) ? null : sc.realJid;
    }

    public final String getDefaultGroupName() {
        if (isConference) {
            return JLocale.getString(Xmpp.CONFERENCE_GROUP);
        }
        if (isGate) {
            return JLocale.getString(Xmpp.GATE_GROUP);
        }
        return null;
    }
    public void setSubject(String subject) {
        if (isConference && isOnline()) {
            setStatus(StatusInfo.STATUS_ONLINE, subject);
        }
    }
    XmppContact.SubContact getContact(String nick) {
        if (StringConvertor.isEmpty(nick)) {
            return null;
        }
        for (int i = 0; i < subContacts.size(); ++i) {
            XmppContact.SubContact contact = (XmppContact.SubContact) subContacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }

    @Override
    public void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        if (!protocol.isConnected()) {
            return;
        }
        if (isGate) {
            if (isOnline()) {
                contactMenu.addItem("disconnect", GATE_DISCONNECT);
                contactMenu.addItem("adhoc", USER_MENU_ADHOC);
                contactMenu.setDefaultItemCode(GATE_DISCONNECT);
            } else {
                contactMenu.addItem("connect", GATE_CONNECT);
                contactMenu.addItem("register", GATE_REGISTER);
                contactMenu.addItem("unregister", GATE_UNREGISTER);
                contactMenu.setDefaultItemCode(GATE_CONNECT);
            }
        }
        if (isConference) {
            if (isOnline()) {
                contactMenu.addItem("leave_chat", CONFERENCE_DISCONNECT);
                contactMenu.setDefaultItemCode(CONFERENCE_DISCONNECT);
            } else {
                contactMenu.addItem("connect", CONFERENCE_CONNECT);
                contactMenu.setDefaultItemCode(CONFERENCE_CONNECT);
            }
            contactMenu.addItem("list_of_users", USER_MENU_USERS_LIST);
            contactMenu.addItem("options", CONFERENCE_OPTIONS);
            if (isOnline()) {
                SubContact my = getContact(getMyName());
                if ((null != my) && (AFFILIATION_OWNER == (my.priority & AFFILIATION_MASK))) {
                    contactMenu.addItem("owner_options", CONFERENCE_OWNER_OPTIONS);
                }
            }
        }
        if ((isOnline() && isConference && canWrite()) || isPrivate) {
            addChatItems(contactMenu);
        }
        if (isPrivate || isGate) {
            contactMenu.addItem("info", Contact.USER_MENU_USER_INFO);
        }
        if (!isPrivate) {
            contactMenu.addItem("manage", USER_MANAGE_CONTACT);
        }
        if (isOnline() && !isGate) {
            contactMenu.addItem("user_statuses", USER_MENU_STATUSES);
        }
        if (isPrivate) {
            initManageContactMenu(protocol, contactMenu);
        }
    }
    @Override
    public void initManageContactMenu(Protocol protocol, MenuModel menu) {
        if (protocol.isConnected()) {
            if (isOnline() && isPrivate) {
                menu.addItem("adhoc", USER_MENU_ADHOC);
            }
            if (isConference && isTemp()) {
                menu.addItem("add_user", CONFERENCE_ADD);
            }
            if (isGate) {
                if ((1 < protocol.getGroupItems().size()) && !isTemp()) {
                    menu.addItem("move_to_group", USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addItem("requauth", USER_MENU_REQU_AUTH);
                }
                if (!protocol.getGroupItems().isEmpty()) {
                    menu.addItem("add_user", GATE_ADD);
                }
                menu.addItem("remove_me", USER_MENU_REMOVE_ME);
            }
        }
        if (protocol.hasContact(this)) {
            if (!isPrivate) {
                menu.addItem("rename", USER_MENU_RENAME);
            }
            menu.addSeparator();
            menu.addItem("remove", USER_MENU_USER_REMOVE);
        }
    }

    public String getNick(String resource) {
        SubContact c = getExistSubContact(resource);
        return (null == c) ? resource : c.resource;
    }

    boolean canWrite() {
        if (isOnline()) {
            if (isConference) {
                SubContact sc = getExistSubContact(getMyName());
                return (null != sc) && (ROLE_VISITOR != sc.priority);
            }
            return true;
        }
        return !isPrivate;
    }
    public void activate(Protocol p) {
        if (isOnline() || isPrivate || hasChat()) {
            super.activate(p);

        } else if (isConference && p.isConnected()) {
            new ContactMenu(p, this).doAction(CONFERENCE_CONNECT);
        }
    }

    public boolean hasHistory() {
        return false;
    }

    public final void setPrivateContactStatus(XmppServiceContact conf) {
        String nick = Jid.getResource(getUserId(), "");
        SubContact sc = (null == conf) ? null : conf.getExistSubContact(nick);
        if (null == sc) {
            setOfflineStatus();
            // #sijapp cond.if modules_CLIENTS is "true" #
            setClient(XmppClient.CLIENT_NONE, null);
            // #sijapp cond.end #

        } else {
            if (subContacts.isEmpty()) {
                subContacts.addElement(sc);
            } else {
                subContacts.setElementAt(sc, 0);
            }
            setStatus(sc.status, sc.statusText);
            // #sijapp cond.if modules_CLIENTS is "true" #
            setClient(sc.client, null);
            // #sijapp cond.end #
        }
    }
}
// #sijapp cond.end #