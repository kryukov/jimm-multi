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

import jimmui.view.icons.Icon;
import jimmui.view.roster.*;
import jimm.*;
import jimm.chat.*;
import jimm.forms.*;
import jimmui.view.menu.*;
import java.util.*;
import jimmui.view.base.*;
import protocol.*;
import protocol.icq.*;
import protocol.mrim.*;
import protocol.jabber.*;


public final class ContactList implements ContactListListener {
    private static final ContactList instance = new ContactList();
    private final ProtocolMenu mainMenu = new ProtocolMenu(null, true);
    private MessageEditor editor;
    private VirtualContactList contactList;
    private final StatusView statusView = new StatusView();
    private Contact currentContact;
    // #sijapp cond.if modules_FILES="true"#
    private Vector transfers = new Vector();
    // #sijapp cond.end#
    public JimmModel jimmModel = new JimmModel();

    public ContactList() {
    }
    public void initUI() {
        contactList = new VirtualContactList();
        contactList.setCLListener(this);
    }
    public void initMessageEditor() {
        editor = new MessageEditor();
    }

    public byte getProtocolType(Profile account) {
        for (int i = 0; i < Profile.protocolTypes.length; ++i) {
            if (account.protocolType == Profile.protocolTypes[i]) {
                return account.protocolType;
            }
        }
        return Profile.protocolTypes[0];
    }
    private boolean is(Protocol protocol, Profile profile) {
        Profile exist = protocol.getProfile();
        if (exist == profile) {
            return true;
        }
        return (exist.protocolType == profile.protocolType)
                && exist.userId.equals(profile.userId);
    }
    public void updateAccounts() {
        Protocol[] oldProtocols = getProtocols();
        Vector newProtocols = new Vector();
        // #sijapp cond.if modules_MULTI is "true" #
        int accountCount = Options.getAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile profile = Options.getAccount(i);
            if (!profile.isActive) continue;
            for (int j = 0; j < oldProtocols.length; ++j) {
                Protocol protocol = oldProtocols[j];
                if ((null != protocol) && is(protocol, profile)) {
                    if (protocol.getProfile() != profile) {
                        protocol.setProfile(profile);
                    }
                    oldProtocols[j] = null;
                    profile = null;
                    newProtocols.addElement(protocol);
                    break;
                }
            }
            if (null != profile) {
                newProtocols.addElement(createProtocol(profile));
            }
        }
        if (0 == newProtocols.size()) {
            Profile profile = Options.getAccount(0);
            profile.isActive = true;
            newProtocols.addElement(profile);
        }
        // #sijapp cond.else #
        newProtocols.addElement(createProtocol(Options.getAccount(Options.getCurrentAccount())));
        mainMenu.setProtocol((Protocol) newProtocols.elementAt(0));
        // #sijapp cond.end #
        for (int i = 0; i < oldProtocols.length; ++i) {
            Protocol protocol = oldProtocols[i];
            if (null != protocol) {
                protocol.disconnect(true);
                protocol.needSave();
                protocol.dismiss();
            }
        }
        jimmModel.protocols = newProtocols;
        updateModel();
        updateMainMenu();
    }

    public void gotoUrl(String textWithUrls) {
        SysTextList.gotoURL(textWithUrls);
    }

    private byte getRealType(byte type) {
        // #sijapp cond.if protocols_JABBER is "true" #
        // #sijapp cond.if modules_MULTI is "true" #
        switch (type) {
            case Profile.PROTOCOL_GTALK:
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_QIP:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return Profile.PROTOCOL_JABBER;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        return type;
    }
    private Protocol createProtocol(Profile account) {
        Protocol protocol = null;
        byte type = getProtocolType(account);
        switch (getRealType(type)) {
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                protocol = new Icq();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                protocol = new Mrim();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_JABBER:
                protocol = new Jabber();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            case Profile.PROTOCOL_MSN:
                protocol = new protocol.msn.Msn();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_OBIMP is "true" #
            case Profile.PROTOCOL_OBIMP:
                protocol = new protocol.obimp.Obimp();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_VKAPI is "true" #
            case Profile.PROTOCOL_VK_API:
                protocol = new protocol.vk.Vk();
                break;
            // #sijapp cond.end #
        }
        if (null == protocol) {
            return null;
        }
        protocol.setProfile(account);
        protocol.init();
        protocol.safeLoad();
        return protocol;
    }

    public Protocol getProtocol(Profile profile) {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.getProfile() == profile) {
                return p;
            }
        }
        return null;
    }

    public Protocol getProtocol(String account) {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.getUserId().equals(account)) {
                return p;
            }
        }
        return null;
    }

    public static ContactList getInstance() {
        return instance;
    }
    public Protocol[] getProtocols() {
        Protocol[] all = new Protocol[jimmModel.protocols.size()];
        for (int i = 0; i < all.length; ++i) {
            all[i] = (Protocol) jimmModel.protocols.elementAt(i);
        }
        return all;
    }

    public Protocol getProtocol(Contact c) {
        ContactListModel model = contactList.getModel();
        for (int i = 0; i < model.getProtocolCount(); ++i) {
            if (model.getProtocol(i).inContactList(c)) {
                return model.getProtocol(i);
            }
        }
        return null;
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
            autoConnect();
        }
    }
    public void autoConnect() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (!ru.net.jimm.JimmActivity.getInstance().isNetworkAvailable()) {
            return;
        }
        // #sijapp cond.end#
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (!"".equals(p.getPassword()) && p.getProfile().isConnected()) {
                p.connect();
            }
        }
    }

    public MessageEditor getMessageEditor() {
        return editor;
    }

    /* *********************************************************** */
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME   = 2;

    /* *********************************************************** */
    // #sijapp cond.if modules_FILES="true"#
    public void addTransfer(FileTransfer ft) {
        transfers.addElement(ft);
    }
    public void removeTransfer(MessData par, boolean cancel) {
        for (int i = 0; i < transfers.size(); ++i) {
            FileTransfer ft = (FileTransfer)transfers.elementAt(i);
            if (ft.is(par)) {
                transfers.removeElementAt(i);
                if (cancel) {
                    ft.cancel();
                }
                return;
            }
        }
    }
    // #sijapp cond.end#

    public boolean isConnected() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnected() && !p.isConnecting()) {
                return true;
            }
        }
        return false;
    }
    public boolean isConnecting() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnecting()) {
                return true;
            }
        }
        return false;
    }
    public boolean disconnect() {
        boolean disconnecting = false;
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnected()) {
                p.disconnect(false);
                disconnecting = true;
            }
        }
        return disconnecting;
    }

    public void safeSave() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            p.safeSave();
        }
    }

    public VirtualContactList getManager() {
        return contactList;
    }
    public Updater getUpdater() {
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
    private int contactListSaveDelay = 0;
    public final void userActivity() {
        cursorLock = 4 /* * 250 = 1 sec */;
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.userActivity();
        // #sijapp cond.end #
    }
    public final void needRosterSave() {
        contactListSaveDelay = 60 * 4 /* * 250 = 60 sec */;
    }
    public final void timerAction() {
        // #sijapp cond.if modules_ABSENCE is "true" #
        jimm.modules.AutoAbsence.instance.updateTime();
        // #sijapp cond.end #
        if (0 < cursorLock) {
            cursorLock--;
        }
        if (0 < contactListSaveDelay) {
            contactListSaveDelay--;
            if (0 == contactListSaveDelay) {
                int count = contactList.getModel().getProtocolCount();
                for (int i = 0; i < count; ++i) {
                    Protocol p = contactList.getModel().getProtocol(i);
                    p.safeSave();
                }
            }
        }
    }

    public final void receivedMessage(Contact contact) {
        // Notify splash canvas
        if (Jimm.isLocked()) {
            Jimm.getJimm().splash.messageAvailable();
        }
        updateUnreadMessageCount();
    }
    public final void markMessages(Contact contact) {
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

    public void updateConnectionStatus() {
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.updateConnectionState();
        // #sijapp cond.end #
    }



    public final Contact getCurrentContact() {
        return currentContact;
    }
    public final void setCurrentContact(Contact contact) {
        currentContact = contact;
    }
    public StatusView getStatusView() {
        return statusView;
    }

    /////////////////////////////////////////////////////////////////

    /** ************************************************************************* */
    public boolean isCollapsible() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (true) return true;
        // #sijapp cond.end #
        return Jimm.isPhone(Jimm.PHONE_SE) || Jimm.isPhone(Jimm.PHONE_NOKIA_S60);
    }


    /* Builds the main menu (visual list) */
    public void activateMainMenu() {
        updateMainMenu();
        mainMenu.setDefaultItemCode(ProtocolMenu.MENU_STATUS);
        mainMenu.getView().show();
    }

    public void updateMainMenu() {
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
        contactList.getUpdater().addProtocols(jimmModel.protocols);
        contactList.updateOption();
    }
}