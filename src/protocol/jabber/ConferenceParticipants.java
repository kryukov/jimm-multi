/*
 * ConferenceParticipants.java
 *
 * Created on 12 Апрель 2009 г., 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import java.util.Vector;
import jimm.JimmUI;
import jimm.cl.ContactList;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.JLocale;
import jimm.comm.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class ConferenceParticipants extends ScrollableArea {
    private static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");

    private Jabber protocol;
    private JabberServiceContact conference;
    private Vector contacts = new Vector();

    private final Icon[] leftIcons = new Icon[2];
    private final Icon[] rightIcons = new Icon[1];

    /** Creates a new instance of ConferenceParticipants */
    private static final int COMMAND_REPLY = 0;
    private static final int COMMAND_PRIVATE = 1;
    private static final int COMMAND_INFO = 2;
    private static final int COMMAND_STATUS = 3;
    private static final int COMMAND_COPY = 4;
    private static final int COMMAND_KICK = 5;
    private static final int COMMAND_BAN = 6;
    private static final int COMMAND_DEVOICE = 7;
    private static final int COMMAND_VOICE = 8;

    private int myRole;
    public ConferenceParticipants(Jabber jabber, JabberServiceContact conf) {
        super(conf.getName());
        protocol = jabber;
        conference = conf;
        myRole = getRole(conference.getMyName());
        update();
    }

    protected final int getSize() {
        return contacts.size();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, boolean isLong) {
        int itemHeight = getItemHeight(item);
        if (isLong || (getWidth() - itemHeight < x)) {
            showMenu(getMenu());
        } else {
            super.touchItemTaped(item, x, isLong);
        }
    }
    // #sijapp cond.end#

    private String getCurrentContact() {
        int contactIndex = getCurrItem();
        if ((contactIndex < 0) || (getSize() <= contactIndex)) {
            return null;
        }
        Object o = contacts.elementAt(contactIndex);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            return c.resource;
        }
        return null;
    }

    protected void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_SELECT:
                if (conference.canWrite()) {
                    execJimmAction(COMMAND_REPLY);
                }
                return;

            case NativeCanvas.JIMM_BACK:
                back();
                return;

            case NativeCanvas.JIMM_MENU:
                showMenu(getMenu());
                return;
        }
        String nick = getCurrentContact();
        if (null == nick) {
            return;
        }
        switch (action) {
            case COMMAND_COPY:
                JimmUI.setClipBoardText(getCaption(), nick);
                restore();
                break;

            case COMMAND_REPLY:
                MessageEditor editor = ContactList.getInstance().getMessageEditor();
                if (editor.isActive(conference)) {
                    InputTextBox box = editor.getTextBox();
                    String text = box.getRawString();
                    if (!StringConvertor.isEmpty(text)) {
                        String space = box.getSpace();
                        if (text.endsWith(space)) {
                        } else if (1 == space.length()) {
                            text += space;
                        } else {
                            text += text.endsWith(" ") ? " " : space;
                        }
                        if (text.endsWith("," + space)) {
                            text += nick + "," + space;
                        } else {
                            text += nick + space;
                        }
                        box.setString(text);
                        box.show();
                        return;
                    }
                }
                protocol.getChat(conference).writeMessageTo(nick);
                break;

            case COMMAND_PRIVATE:
                nickSelected(nick);
                break;

            case COMMAND_INFO:
                protocol.showUserInfo(getContactForVCard(nick));
                break;

            case COMMAND_STATUS:
                protocol.showStatus(getPrivateContact(nick));
                break;

            case COMMAND_KICK:
                setMucRole(nick, "n" + "o" + "ne");
                update();
                restore();
                break;

            case COMMAND_BAN:
                setMucAffiliation(nick, "o" + "utcast");
                update();
                restore();
                break;

            case COMMAND_DEVOICE:
                setMucRole(nick, "v" + "isitor");
                update();
                restore();
                break;

            case COMMAND_VOICE:
                setMucRole(nick, "partic" + "ipant");
                update();
                restore();
                break;
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (JimmUI.isHotKey(keyCode, type)) {
            String nick = getCurrentContact();
            Contact c = (null == nick) ? null : getPrivateContact(nick);
            if (JimmUI.execHotKey(protocol, c, keyCode, type)) {
                return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }


    protected final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.setActionListener(new Binder(this));
        String nick = getCurrentContact();
        if (null == nick) {
            return menu;
        }

        int defaultCode = -1;
        if (conference.canWrite()) {
            menu.addItem("reply", COMMAND_REPLY);
            defaultCode = COMMAND_REPLY;
        }
        menu.addItem("private_chat", COMMAND_PRIVATE);
        menu.addItem("info", COMMAND_INFO);
        menu.addItem("user_statuses", COMMAND_STATUS);
        menu.addItem("copy_text", COMMAND_COPY);

        if (JabberServiceContact.ROLE_MODERATOR == myRole) {
            int role = getRole(nick);
            if (JabberServiceContact.ROLE_MODERATOR != role) {
                if (JabberServiceContact.ROLE_PARTICIPANT == role) {
                    menu.addItem("devoice", COMMAND_DEVOICE);
                } else {
                    menu.addItem("voice", COMMAND_VOICE);
                }
                menu.addItem("kick", COMMAND_KICK);
                menu.addItem("ban", COMMAND_BAN);
            }
        }

        return menu;
    }

    private void update() {
        super.lock();
        int currentIndex = getCurrItem();
        contacts.removeAllElements();
        addLayerToListOfSubcontacts("list_of_moderators", JabberServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts("list_of_participants", JabberServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts("list_of_visitors", JabberServiceContact.ROLE_VISITOR);
        setCurrentItemIndex(currentIndex);
        super.unlock();
    }

    private int getRole(String nick) {
        JabberContact.SubContact c = conference.getContact(nick);
        int priority = (null == c) ? JabberServiceContact.ROLE_VISITOR : c.priority;
        return priority & JabberServiceContact.ROLE_MASK;
    }


    private void addLayerToListOfSubcontacts(String layer, byte priority) {
        boolean hasLayer = false;
        contacts.addElement(JLocale.getString(layer));
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if ((contact.priority & JabberServiceContact.ROLE_MASK) == priority) {
                contacts.addElement(contact);
                hasLayer = true;
            }
        }
        if (!hasLayer) {
            contacts.removeElementAt(contacts.size() - 1);
            return;
        }
    }
    protected int getItemHeight(int itemIndex) {
        Object o = contacts.elementAt(itemIndex);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            int height = getDefaultFont().getHeight() + 1;
            // #sijapp cond.if modules_CLIENTS is "true" #
            Icon client = protocol.clientInfo.getIcon(c.client);
            if (null != client) {
                height = Math.max(height, client.getHeight());
            }
            // #sijapp cond.end #
            Icon icon = protocol.getStatusInfo().getIcon(c.status);
            if (null != icon) {
                height = Math.max(height, icon.getHeight());
            }
            height = Math.max(height, CanvasEx.minItemHeight);
            return height;
        }
        return getFontSet()[FONT_STYLE_BOLD].getHeight() + 1;
    }

    protected void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        g.setThemeColor(THEME_TEXT);
        Object o = contacts.elementAt(index);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            g.setFont(getDefaultFont());
            leftIcons[0] = protocol.getStatusInfo().getIcon(c.status);
            leftIcons[1] = affiliationIcons.iconAt(c.priority & JabberServiceContact.AFFILIATION_MASK);
            // #sijapp cond.if modules_CLIENTS is "true" #
            rightIcons[0] = protocol.clientInfo.getIcon(c.client);
            // #sijapp cond.end #
            g.drawString(leftIcons, c.resource, rightIcons, x, y, w, h);
            return;
        }
        String header = (String)o;
        g.setFont(getFontSet()[FONT_STYLE_BOLD]);
        g.drawString(header, x, y, w, h);
    }

    private Contact getPrivateContact(String nick) {
        String jid = Jid.realJidToJimmJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }
    private Contact getContactForVCard(String nick) {
        String jid = Jid.realJidToJimmJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }
    private void nickSelected(String nick) {
        String jid = Jid.realJidToJimmJid(conference.getUserId() + "/" + nick);
        JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
        if (null == c) {
            c = (JabberServiceContact)protocol.createTempContact(jid);
            protocol.addTempContact(c);
        }
        c.activate(protocol);
    }

    public void setMucRole(String nick, String role) {
        protocol.getConnection().setMucRole(conference.getUserId(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        JabberContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliation(conference.getUserId(),
                c.realJid, affiliation);
    }
}
// #sijapp cond.end #
