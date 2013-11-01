/*
 * JabberContact.java
 *
 * Created on 13 Июль 2008 г., 10:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.xmpp;

import java.util.Vector;
import jimm.*;
import jimm.comm.*;
import jimmui.view.menu.*;
import jimm.util.JLocale;
import protocol.*;
import protocol.ui.ClientInfo;
import protocol.ui.StatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class XmppContact extends Contact implements SelectListener {
    /** Creates a new instance of JabberContact */
    public XmppContact(String jid, String name) {
        this.userId = jid;
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }

    protected String currentResource;



    public boolean isConference() {
        return false;
    }

    public String getDefaultGroupName() {
        return JLocale.getString(Xmpp.GENERAL_GROUP);
    }

    /////////////////////////////////////////////////////////////////////////
    public static final int USER_MENU_CONNECTIONS = 10;
    public static final int USER_MENU_REMOVE_ME   = 11;
    public static final int USER_MENU_ADHOC       = 12;

    public void addChatMenuItems(MenuModel model) {
        if (isOnline() && !(this instanceof XmppServiceContact)) {
            if (Options.getBoolean(Options.OPTION_ALARM)) {
                model.addItem("wake", USER_MENU_WAKE);
            }
        }
    }
    @Override
    public void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        addChatItems(contactMenu);

        if (0 < subContacts.size()) {
            contactMenu.addItem("list_of_connections", USER_MENU_CONNECTIONS);
        }
        addGeneralItems(protocol, contactMenu);
    }
    @Override
    public void initManageContactMenu(Protocol protocol, MenuModel menu) {
        if (protocol.isConnected()) {
            if (isOnline()) {
                menu.addItem("adhoc", USER_MENU_ADHOC);
            }
            if (isTemp()) {
                menu.addItem("add_user", USER_MENU_ADD_USER);

            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.addItem("move_to_group", USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addItem("requauth", USER_MENU_REQU_AUTH);
                }
            }
            if (!isTemp()) {
                menu.addItem("rename", USER_MENU_RENAME);
            }
        }
        if (protocol.isConnected() || (isTemp() && protocol.hasContact(this))) {
            menu.addSeparator();
            if (protocol.isConnected()) {
                menu.addItem("remove_me", USER_MENU_REMOVE_ME);
            }
            if (protocol.hasContact(this)) {
                menu.addItem("remove", USER_MENU_USER_REMOVE);
            }
        }
    }
    /////////////////////////////////////////////////////////////////////////
    String getReciverJid() {
        if (this instanceof XmppServiceContact) {
        } else if (!StringUtils.isEmpty(currentResource)) {
            return getUserId() + "/" + currentResource;
        }
        return getUserId();
    }

    public boolean execCommand(Protocol protocol, String msg) {
        final String cmd;
        final String param;
        int endCmd = msg.indexOf(' ');
        if (-1 != endCmd) {
            cmd = msg.substring(1, endCmd);
            param = msg.substring(endCmd + 1);
        } else {
            cmd = msg.substring(1);
            param = "";
        }
        String resource = param;
        String newMessage = "";

        int endNick = param.indexOf('\n');
        if (-1 != endNick) {
            resource = param.substring(0, endNick);
            newMessage = param.substring(endNick + 1);
        }
        String xml = null;
        final String on = "o" + "n";
        final String off = "o" + "f" + "f";
        if (on.equals(param) || off.equals(param)) {
            xml = Config.getConfigValue(cmd + ' ' + param, "/jabber-commands.txt");
        }
        if (null == xml) {
            xml = Config.getConfigValue(cmd, "/jabber-commands.txt");
        }
        if (null == xml) {
            return false;
        }

        XmppConnection xmppXml = ((Xmpp)protocol).getConnection();

        String jid = Jid.jimmJidToRealJid(getUserId());
        String fullJid = jid;
        if (isConference()) {
            String nick = ((XmppServiceContact)this).getMyName();
            fullJid = Jid.jimmJidToRealJid(getUserId() + '/' + nick);
        }

    	xml = Util.replace(xml, "${jimm.caps}", xmppXml.getCaps());
        xml = Util.replace(xml, "${c.jid}", Util.xmlEscape(jid));
        xml = Util.replace(xml, "${c.fulljid}", Util.xmlEscape(fullJid));
    	xml = Util.replace(xml, "${param.full}", Util.xmlEscape(param));
        xml = Util.replace(xml, "${param.res}", Util.xmlEscape(resource));
        xml = Util.replace(xml, "${param.msg}", Util.xmlEscape(newMessage));
        xml = Util.replace(xml, "${param.res.realjid}",
    		Util.xmlEscape(getSubContactRealJid(resource)));
        xml = Util.replace(xml, "${param.full.realjid}",
    		Util.xmlEscape(getSubContactRealJid(param)));

        xmppXml.requestRawXml(xml);
        return true;
    }
    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringUtils.notNull((null == c) ? null : c.realJid);
    }

    protected static class SubContact {
        public String resource;
        public String statusText;
        public String realJid;
        // #sijapp cond.if modules_CLIENTS is "true" #
        public short client = ClientInfo.CLI_NONE;
        // #sijapp cond.end #
        public byte status;
        public byte priority;
    }
    Vector<SubContact> subContacts = new Vector<SubContact>();
    private void removeSubContact(String resource) {
        for (int i = subContacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact) subContacts.elementAt(i);
            if (c.resource.equals(resource)) {
                c.status = StatusInfo.STATUS_OFFLINE;
                c.statusText = null;
                subContacts.removeElementAt(i);
                return;
            }
        }
    }
    protected SubContact getExistSubContact(String resource) {
        for (int i = subContacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact) subContacts.elementAt(i);
            if (c.resource.equals(resource)) {
                return c;
            }
        }
        return null;
    }
    protected SubContact getSubContact(String resource) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            return c;
        }
        c = new SubContact();
        c.resource = resource;
        c.status = StatusInfo.STATUS_OFFLINE;
        subContacts.addElement(c);
        return c;
    }
    void setRealJid(String resource, String realJid) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.realJid = realJid;
        }
    }
    SubContact getCurrentSubContact() {
        if ((0 == subContacts.size()) || isConference()) {
            return null;
        }
        SubContact currentContact = getExistSubContact(currentResource);
        if (null != currentContact) {
            return currentContact;
        }
        try {
            currentContact = (SubContact) subContacts.elementAt(0);
            byte maxPriority = currentContact.priority;
            for (int i = 1; i < subContacts.size(); ++i) {
                SubContact contact = (SubContact) subContacts.elementAt(i);
                if (maxPriority < contact.priority) {
                    maxPriority = contact.priority;
                    currentContact = contact;
                }
            }
        } catch (Exception e) {
            // synchronization error
        }
        return currentContact;
    }


    public void __setStatus(String resource, int priority, byte index, String statusText) {
        if (StatusInfo.STATUS_OFFLINE == index) {
            resource = StringUtils.notNull(resource);
            if (resource.equals(currentResource)) {
                currentResource = null;
            }
            removeSubContact(resource);
            if (0 == subContacts.size()) {
                setOfflineStatus();
            }

        } else {
            SubContact c = getSubContact(resource);
            c.priority = (byte)Math.min(127, Math.max(priority, -127));
            c.status = index;
            c.statusText = statusText;
        }
    }
    void updateMainStatus(Xmpp xmpp) {
        if (isSingleUserContact()) {
            SubContact c = getCurrentSubContact();
            if (null == c) {
                setOfflineStatus();

            } else if (this instanceof XmppServiceContact) {
                setStatus(c.status, c.statusText);

            } else {
                xmpp.setContactStatus(this, c.status, c.statusText);
            }
        }
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = XmppClient.createClient(caps);
        }
        SubContact cur = getCurrentSubContact();
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
    }
    // #sijapp cond.end #
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void setXStatus(String id, String text) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null != id) {
            jimm.modules.DebugLog.println("xstatus " + getUserId() + " " + id + " " + text);
        }
        // #sijapp cond.end #
        setXStatus(Xmpp.xStatus.createXStatus(id), text);
    }
    // #sijapp cond.end #

    public final void setOfflineStatus() {
        subContacts.removeAllElements();
        super.setOfflineStatus();
    }
    public void setActiveResource(String resource) {
        SubContact c = getExistSubContact(resource);
        currentResource = (null == c) ? null : c.resource;

        SubContact cur = getCurrentSubContact();
        if (null == cur) {
            setStatus(StatusInfo.STATUS_OFFLINE, null);
        } else {
            setStatus(cur.status, cur.statusText);
        }
        // #sijapp cond.if modules_CLIENTS is "true" #
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
        // #sijapp cond.end #
    }

    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return !isTemp();
    }
    public final void select(Select select, MenuModel model, int cmd) {
        String resource = model.getItemText(cmd);
        setActiveResource(resource);
        Jimm.getJimm().getDisplay().closeMenus();
    }
}
// #sijapp cond.end #