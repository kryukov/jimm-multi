/*
 * ConferenceParticipants.java
 *
 * Created on 12 Апрель 2009 г., 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.xmpp;

import jimmui.Clipboard;
import jimm.Jimm;
import jimmui.HotKeys;
import jimmui.view.base.touch.*;
import jimmui.view.icons.*;
import java.util.Vector;

import jimmui.view.*;
import jimmui.view.base.*;
import jimmui.view.menu.*;
import jimm.util.JLocale;
import jimm.comm.*;
import protocol.*;
import protocol.ui.InfoFactory;
import protocol.ui.MessageEditor;

import javax.microedition.lcdui.Font;

/**
 *
 * @author Vladimir Krukov
 */
public final class ConferenceParticipants extends SomeContent {
    private static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");
    private Font[] fontSet;

    private Xmpp protocol;
    private XmppServiceContact conference;
    private Vector<Object> contacts = new Vector<Object>();

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

    public ConferenceParticipants(Xmpp xmpp, XmppServiceContact conf) {
        fontSet = GraphicsEx.chatFontSet;
        protocol = xmpp;
        conference = conf;
        myRole = getRole(conference.getMyName());
        update();
    }


    protected final int getSize() {
        return contacts.size();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, TouchState state) {
        int itemHeight = getItemHeight(item);
        if (state.isLong || (view.getWidth() - itemHeight < x)) {
            view.showMenu(getContextMenu());
        } else {
            execJimmAction(NativeCanvas.JIMM_SELECT);
        }
    }
    // #sijapp cond.end#

    private String getCurrentContact() {
        int contactIndex = getCurrItem();
        if ((contactIndex < 0) || (getSize() <= contactIndex)) {
            return null;
        }
        Object o = contacts.elementAt(contactIndex);
        if (o instanceof XmppContact.SubContact) {
            XmppContact.SubContact c = (XmppContact.SubContact)o;
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
                view.back();
                return;

            case NativeCanvas.JIMM_MENU:
                view.showMenu(getContextMenu());
                return;
        }
        String nick = getCurrentContact();
        if (null == nick) {
            return;
        }
        switch (action) {
            case COMMAND_COPY:
                Clipboard.setClipBoardText(nick);
                view.restore();
                break;

            case COMMAND_REPLY:
                MessageEditor editor = Jimm.getJimm().getMessageEditor();
                if (editor.isActive(conference)) {
                    InputTextBox box = editor.getTextBox();
                    String text = box.getRawString();
                    if (!StringUtils.isEmpty(text)) {
                        String space = box.getSpace();
                        if (text.endsWith(space)) {
                            // do nothing
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
                Jimm.getJimm().getChatUpdater().writeMessageTo(protocol, conference, nick);
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
                view.restore();
                break;

            case COMMAND_BAN:
                setMucAffiliation(nick, "o" + "utcast");
                update();
                view.restore();
                break;

            case COMMAND_DEVOICE:
                setMucRole(nick, "v" + "isitor");
                update();
                view.restore();
                break;

            case COMMAND_VOICE:
                setMucRole(nick, "partic" + "ipant");
                update();
                view.restore();
                break;
        }
    }
    @Override
    protected boolean doKeyReaction(int keyCode, int actionCode, int type) {
        if (HotKeys.isHotKey(keyCode)) {
            String nick = getCurrentContact();
            Contact c = (null == nick) ? null : getPrivateContact(nick);
            if (HotKeys.execHotKey(protocol, c, keyCode, type)) {
                return true;
            }
        }
        return super.doKeyReaction(keyCode, actionCode, type);
    }


    protected final MenuModel getContextMenu() {
        MenuModel menu = new MenuModel();
        menu.setActionListener(new Binder(this));
        String nick = getCurrentContact();
        if (null == nick) {
            return menu;
        }

        if (conference.canWrite()) {
            menu.addItem("reply", COMMAND_REPLY);
        }
        menu.addItem("private_chat", COMMAND_PRIVATE);
        menu.addItem("info", COMMAND_INFO);
        menu.addItem("user_statuses", COMMAND_STATUS);
        menu.addItem("copy_text", COMMAND_COPY);

        if (XmppServiceContact.ROLE_MODERATOR == myRole) {
            int role = getRole(nick);
            if (XmppServiceContact.ROLE_MODERATOR != role) {
                if (XmppServiceContact.ROLE_PARTICIPANT == role) {
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
        if (null != view) view.lock();
        int currentIndex = getCurrItem();
        contacts.removeAllElements();
        addLayerToListOfSubcontacts("list_of_moderators", XmppServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts("list_of_participants", XmppServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts("list_of_visitors", XmppServiceContact.ROLE_VISITOR);
        setCurrentItemIndex(currentIndex);
        if (null != view) view.unlock();
    }

    private int getRole(String nick) {
        XmppContact.SubContact c = conference.getContact(nick);
        int priority = (null == c) ? XmppServiceContact.ROLE_VISITOR : c.priority;
        return priority & XmppServiceContact.ROLE_MASK;
    }


    private void addLayerToListOfSubcontacts(String layer, byte priority) {
        boolean hasLayer = false;
        contacts.addElement(JLocale.getString(layer));
        Vector subcontacts = conference.subContacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            XmppContact.SubContact contact = (XmppContact.SubContact)subcontacts.elementAt(i);
            if ((contact.priority & XmppServiceContact.ROLE_MASK) == priority) {
                contacts.addElement(contact);
                hasLayer = true;
            }
        }
        if (!hasLayer) {
            contacts.removeElementAt(contacts.size() - 1);
        }
    }
    protected int getItemHeight(int itemIndex) {
        Object o = contacts.elementAt(itemIndex);
        if (o instanceof XmppContact.SubContact) {
            XmppContact.SubContact c = (XmppContact.SubContact)o;
            int height = fontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight() + 1;
            // #sijapp cond.if modules_CLIENTS is "true" #
            Icon client = InfoFactory.factory.getClientInfo(protocol).getIcon(c.client);
            if (null != client) {
                height = Math.max(height, client.getHeight());
            }
            // #sijapp cond.end #
            Icon icon = InfoFactory.factory.getStatusInfo(protocol).getIcon(c.status);
            if (null != icon) {
                height = Math.max(height, icon.getHeight());
            }
            height = Math.max(height, CanvasEx.minItemHeight);
            return height;
        }
        return fontSet[CanvasEx.FONT_STYLE_BOLD].getHeight() + 1;
    }

    protected void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        g.setThemeColor(CanvasEx.THEME_TEXT);
        Object o = contacts.elementAt(index);
        if (o instanceof XmppContact.SubContact) {
            XmppContact.SubContact c = (XmppContact.SubContact)o;
            g.setFont(fontSet[CanvasEx.FONT_STYLE_PLAIN]);
            leftIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(c.status);
            leftIcons[1] = affiliationIcons.iconAt(c.priority & XmppServiceContact.AFFILIATION_MASK);
            // #sijapp cond.if modules_CLIENTS is "true" #
            rightIcons[0] = InfoFactory.factory.getClientInfo(protocol).getIcon(c.client);
            // #sijapp cond.end #
            g.drawString(leftIcons, c.resource, rightIcons, x, y, w, h);
            return;
        }
        String header = (String)o;
        g.setFont(fontSet[CanvasEx.FONT_STYLE_BOLD]);
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
        XmppServiceContact c = (XmppServiceContact)protocol.getItemByUID(jid);
        if (null == c) {
            c = (XmppServiceContact)protocol.createTempContact(jid);
            protocol.addTempContact(c);
        }
        c.activate(protocol);
    }

    public void setMucRole(String nick, String role) {
        protocol.getConnection().setMucRole(conference.getUserId(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        XmppContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliation(conference.getUserId(),
                c.realJid, affiliation);
    }
}
// #sijapp cond.end #
