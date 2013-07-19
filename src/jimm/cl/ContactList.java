/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/ContactList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm.cl;

import jimm.chat.message.Message;
import jimm.modules.*;
import jimmui.view.icons.Icon;
import jimmui.view.roster.*;
import jimm.*;
import jimm.chat.*;
import jimm.forms.*;
import jimmui.view.menu.*;
import jimmui.view.base.*;
import protocol.*;
import protocol.jabber.*;
import protocol.ui.ContactMenu;


public final class ContactList implements ContactListListener {
    private final ProtocolMenu mainMenu = new ProtocolMenu(null, true);
    private VirtualContactList contactList;

    public ContactList() {
    }
    public void initUI() {
        contactList = new VirtualContactList();
        contactList.setCLListener(this);
    }

    public void updateCl() {
        updateModel();
        updateMainMenu();
    }

    public void activate() {
        contactList.update();
        contactList.showMain();
    }
    public void _setActiveContact(Contact c) {
        if (null != c) {
            contactList.setActiveContact(c);
        }
        contactList.getModel().setAlwaysVisibleNode(c);
    }
    public void activate(Contact c) {
        _setActiveContact(c);
        activate();
    }
    public void activateWithMsg(String message) {
        activate();
        new Popup(contactList, message).show();
    }

    public void startUp() {
        if (0 == Options.getAccountCount()) {
            updateUnreadMessageCount();
            contactList.update();
            updateMainMenu();
            mainMenu.setDefaultItemCode(ProtocolMenu.MENU_STATUS);
            Jimm.getJimm().getDisplay().pushWindow(contactList);
            Jimm.getJimm().getDisplay().pushWindow(mainMenu.getView());
            new AccountsForm().showAccountEditor(null);

        } else {
            activate();
            ChatHistory.instance.loadUnreadMessages();
            updateUnreadMessageCount();
        }
    }

    /* *********************************************************** */
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME   = 2;

    /* *********************************************************** */

    public VirtualContactList getManager() {
        return contactList;
    }
    public Updater getUpdater() {
//        // #sijapp cond.if modules_ANDROID is "true" #
//        if (Jimm.isPaused()) return new EmptyUpdater();
//        // #sijapp cond.end #
        return contactList.getUpdater();
    }

    /**
     * Adds the given message to the message queue of the contact item
     * identified by the given UIN
     */
    public void setActiveContact(Contact contact) {
        boolean isShown = (Jimm.getJimm().getDisplay().getCurrentDisplay() == contactList);
        if (isShown && (0 == cursorLock)) {
            contactList.setActiveContact(contact);
        }
    }

    private int cursorLock = 0;
    public final void userActivity() {
        cursorLock = 4 /* * 250 = 1 sec */;
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.userActivity();
        // #sijapp cond.end #
    }
    public final void timerAction() {
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.updateTime();
        // #sijapp cond.end #
        if (0 < cursorLock) {
            cursorLock--;
        }
        Jimm.getJimm().jimmModel.saveRostersIfNeed();
    }

    public final void receivedMessage(Protocol protocol, Contact contact, Message message, boolean silent) {
        // Notify splash canvas
        if (Jimm.getJimm().isLocked()) {
            Jimm.getJimm().splash.messageAvailable();
        }
        if (!silent) {
            addMessageNotify(protocol, protocol.getChatModel(contact), contact, message);
            if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
                getUpdater().updateContact(protocol, protocol.getGroup(contact), contact);
            }
        }
        updateUnreadMessageCount();
    }

    private void addMessageNotify(Protocol p, ChatModel chat, Contact contact, Message message) {
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = p.isBlogBot(contact.getUserId());
        boolean isHuman = isBlog || chat.isHuman() || !contact.isSingleUserContact();
        if (p.isBot(contact)) {
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
            isMention = MessageBuilder.isHighlight(msg, myName);
        }
        // #sijapp cond.end #

        boolean isPaused = false;
        // #sijapp cond.if target is "MIDP2" #
        isPaused = Jimm.getJimm().isPaused() && Jimm.getJimm().phone.isCollapsible();
        if (isPaused && isPersonal && isHuman) {
            if (Options.getBoolean(Options.OPTION_BRING_UP)) {
                Chat view = ChatHistory.instance.getOrCreateChat(chat);
                Jimm.getJimm().maximize(view);
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
                Jimm.getJimm().getCL().setActiveContact(contact);
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
                playNotification(p, Notify.NOTIFY_ALARM);

            } else if (isBlog) {
                playNotification(p, Notify.NOTIFY_BLOG);

            } else if (isHuman) {
                playNotification(p, Notify.NOTIFY_MESSAGE);
            }

            // #sijapp cond.if protocols_JABBER is "true" #
        } else if (isMention) {
            playNotification(p, Notify.NOTIFY_MULTIMESSAGE);
            // #sijapp cond.end #
        }
        // #sijapp cond.end#
    }

    public final void playNotification(Protocol p, int type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (!p.isAway(p.getProfile().statusIndex)
                || Options.getBoolean(Options.OPTION_NOTIFY_IN_AWAY)) {
            Notify.getSound().playSoundNotification(type);
        }
        // #sijapp cond.end #
    }

    public final void markMessages(Protocol protocol, Contact contact) {
        if (null != contact) {
            if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
                getUpdater().updateContact(protocol, protocol.getGroup(contact), contact);
            }
        }
        if (null != MyActionBar.getMessageIcon()) {
            updateUnreadMessageCount();
        }
    }
    private void updateUnreadMessageCount() {
        Icon icon = ChatHistory.instance.getUnreadMessageIcon();
        if (icon != MyActionBar.getMessageIcon()) {
            MyActionBar.setMessageIcon(icon);
            jimmui.view.base.NativeCanvas.getInstance().repaint();
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.updateAppIcon();
        // #sijapp cond.end #
    }

    /////////////////////////////////////////////////////////////////

    /** ************************************************************************* */

    /* Builds the main menu (visual list) */
    public void activateMainMenu() {
        updateMainMenu();
        mainMenu.setDefaultItemCode(ProtocolMenu.MENU_STATUS);
        mainMenu.getView().show();
    }

    public void updateMainMenu() {
        // #sijapp cond.if modules_MULTI isnot "true" #
        mainMenu.setProtocol((Protocol) Jimm.getJimm().jimmModel.protocols.elementAt(0));
        // #sijapp cond.end #
        int currentCommand = mainMenu.getSelectedItemCode();
        mainMenu.updateMenu();
        Select menuView = mainMenu.getView();
        mainMenu.setDefaultItemCode(currentCommand);
        menuView.update();
    }

    public final MenuModel getContextMenu(Protocol p, TreeNode node) {
        if (contactList.getModel() == getUpdater().getChatModel()) {
            return ChatHistory.instance.getMenu();
        }
        if (node instanceof Contact) {
            return new ContactMenu(p, (Contact) node).getContextMenu();
        }
        if (node instanceof Group) {
            if (p.isConnected()) {
                return new ManageContactListForm(p, (Group) node).getMenu();
            }
            return null;
        }
        // #sijapp cond.if modules_MULTI is "true" #
        if ((node instanceof ProtocolBranch) || (null == node)) {
            ProtocolMenu menu = new ProtocolMenu(p, false);
            menu.updateMenu();
            return menu.getModel();
        }
        // #sijapp cond.end #
        return null;
    }

    public void updateModel() {
        contactList.setModel(contactList.getUpdater().createModel());
        contactList.getUpdater().addProtocols(Jimm.getJimm().jimmModel.protocols);
        contactList.updateOption();
    }

    public void typing(Protocol protocol, Contact item, boolean type) {
        if (type && protocol.isConnected()) {
            playNotification(protocol, Notify.NOTIFY_TYPING);
        }

        getUpdater().typing(protocol, item);
    }

    public void setContactStatus(Protocol protocol, Contact contact, byte prev, byte curr) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (!protocol.isAway(curr) && protocol.isAway(prev)) {
            playNotification(protocol, Notify.NOTIFY_ONLINE);
        }
        // #sijapp cond.end #
        UIUpdater.showTopLine(protocol, contact, null, contact.getStatusIndex());
    }

    public void disconnected(Protocol protocol) {
        // #sijapp cond.if modules_SOUND is "true" #
        playNotification(protocol, Notify.NOTIFY_RECONNECT);
        // #sijapp cond.end #
    }
}