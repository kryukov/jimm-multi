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

import DrawControls.icons.Icon;
import DrawControls.tree.*;
import javax.microedition.lcdui.TextField;
import jimm.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.forms.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import java.util.*;
import jimm.ui.base.*;
import protocol.*;
import protocol.icq.*;
import protocol.mrim.*;
import protocol.jabber.*;


public final class ContactList implements TextBoxListener, SelectListener, ContactListListener {
    private static final ContactList instance = new ContactList();
    private final MenuModel mainMenu = new MenuModel();
    private MessageEditor editor;
    private Select mainMenuView;
    private VirtualContactList contactList;
    private final StatusView statusView = new StatusView();
    private Contact currentContact;
    private Protocol activeProtocol;
    private InputTextBox passwordTextBox;
    // #sijapp cond.if modules_FILES="true"#
    private Vector transfers = new Vector();
    // #sijapp cond.end#

    public ContactList() {
    }
    public void initUI() {
        contactList = new VirtualContactList();
        contactList.setCLListener(this);
        mainMenu.setActionListener(this);
        mainMenuView = new Select(mainMenu);
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
    public void addProtocols(Vector accounts) {
        int count = contactList.getModel().getProtocolCount();
        Protocol[] protocols = new Protocol[count];
        for (int i = 0; i < count; ++i) {
            protocols[i] = contactList.getModel().getProtocol(i);
        }
        contactList.getModel().removeAllProtocols();
        for (int i = 0; i < accounts.size(); ++i) {
            Profile profile = (Profile)accounts.elementAt(i);
            for (int j = 0; j < protocols.length; ++j) {
                Protocol protocol = protocols[j];
                if ((null != protocol) && is(protocol, profile)) {
                    if (protocol.getProfile() != profile) {
                        protocol.setProfile(profile);
                    }
                    protocols[j] = null;
                    profile = null;
                    contactList.getModel().addProtocol(protocol);
                    break;
                }
            }
            if (null != profile) {
                addProtocol(profile, true);
            }
        }
        for (int i = 0; i < protocols.length; ++i) {
            Protocol protocol = protocols[i];
            if (null != protocol) {
                protocol.disconnect(true);
                protocol.needSave();
                protocol.dismiss();
            }
        }
    }
    public void initAccounts() {
        // #sijapp cond.if modules_MULTI is "true" #
        int count = Math.max(1, Options.getAccountCount());
        for (int i = 0; i < count; ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                addProtocol(p, false);
            }
        }
        // #sijapp cond.else #
        addProtocol(Options.getAccount(Options.getCurrentAccount()), false);
        // #sijapp cond.end #
    }
    public void loadAccounts() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol protocol = contactList.getModel().getProtocol(i);
            protocol.safeLoad();
        }
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
    private void addProtocol(Profile account, boolean load) {
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
            return;
        }
        protocol.setProfile(account);
        protocol.init();
        if (load) {
            protocol.safeLoad();
        }
        contactList.getModel().addProtocol(protocol);
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
        ContactListModel model = contactList.getModel();
        Protocol[] all = new Protocol[model.getProtocolCount()];
        for (int i = 0; i < all.length; ++i) {
            all[i] = model.getProtocol(i);
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
        contactList.setAlwaysVisibleNode(null);
        contactList.update();
        contactList.showTop();
    }
    public void activate(Contact c) {
        contactList.setActiveContact(c);
        contactList.setAlwaysVisibleNode(c);
        contactList.update();
        contactList.showTop();
    }
    public void activate(String message) {
        activate();
        new Popup(contactList, message).show();
    }

    public void startUp() {
        if (0 == Options.getAccountCount()) {
            updateUnreadMessageCount();
            contactList.update();
            updateMainMenu();
            mainMenu.setDefaultItemCode(MENU_STATUS);
            Jimm.getJimm().getDisplay().pushWindow(contactList);
            Jimm.getJimm().getDisplay().pushWindow(mainMenuView);
            new AccountsForm().showAccountEditor(null);

        } else {
            contactList.showTop();
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
            if (!"".equals(p.getPassword()) && p.getProfile().isConnected) {
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

    public void collapseAll() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            Vector groups = p.getGroupItems();
            for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                ((TreeBranch)groups.elementAt(groupIndex)).setExpandFlag(false);
            }
            p.getNotInListGroup().setExpandFlag(false);
        }
        contactList.setAllToTop();
        contactList.update();
    }
    public VirtualContactList getManager() {
        return contactList;
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
        if (null != VirtualList.getMessageIcon()) {
            updateUnreadMessageCount();
        }
    }
    private void updateUnreadMessageCount() {
        Icon icon = ChatHistory.instance.getUnreadMessageIcon();
        if (icon != VirtualList.getMessageIcon()) {
            VirtualList.setMessageIcon(icon);
            jimm.ui.base.NativeCanvas.getInstance().repaint();
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().updateAppIcon();
        // #sijapp cond.end #
    }

    public void updateConnectionStatus() {
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().updateAppIcon();
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

    /* Static constants for menu actios */
    private static final int MENU_CONNECT = 1;
    private static final int MENU_DISCONNECT = 2;
    private static final int MENU_DISCO = 3;
    private static final int MENU_OPTIONS = 4;
    private static final int MENU_KEYLOCK = 6;
    private static final int MENU_STATUS = 7;
    private static final int MENU_XSTATUS = 8;
    private static final int MENU_PRIVATE_STATUS = 9;
    private static final int MENU_GROUPS = 10;
    private static final int MENU_SEND_SMS = 11;
    private static final int MENU_ABOUT = 12;
    private static final int MENU_MINIMIZE = 13;
    private static final int MENU_SOUND = 14;
    private static final int MENU_MYSELF = 15;
    private static final int MENU_MICROBLOG = 19;
    private static final int MENU_EXIT = 21;

    /////////////////////////////////////////////////////////////////

    /** ************************************************************************* */
    public boolean isCollapsible() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (true) return true;
        // #sijapp cond.end #
        return Jimm.isPhone(Jimm.PHONE_SE) || Jimm.isPhone(Jimm.PHONE_NOKIA_S60);
    }
    private boolean isSmsSupported() {
        // #sijapp cond.if protocols_MRIM is "true" #
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if ((p instanceof Mrim) && p.isConnected()) {
                return true;
            }
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_FILES="true"#
        if (!isCollapsible()) {
            return true;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        return false;
    }


    /* Builds the main menu (visual list) */
    public void activateMainMenu() {
        updateMainMenu();
        mainMenu.setDefaultItemCode(MENU_STATUS);
        mainMenuView.show();
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private int getXStatusCount(Protocol protocol) {
        if (null != protocol.getXStatusInfo()) {
            return protocol.getXStatusInfo().getXStatusCount();
        }
        return 0;
    }
    // #sijapp cond.end #
    public void protocolMenu(MenuModel menu, Protocol protocol, boolean main) {
        activeProtocol = protocol;
        if (protocol.isConnecting()) {
            menu.addItem("disconnect", MENU_DISCONNECT);
            return;
        }
        if (protocol.isConnected()) {
            menu.addItem("disconnect", MENU_DISCONNECT);

        } else {
            menu.addItem("connect", MENU_CONNECT);
        }
        menu.addItem("set_status",
                protocol.getStatusInfo().getIcon(protocol.getProfile().statusIndex),
                MENU_STATUS);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (0 < getXStatusCount(protocol)) {
            Icon icon = protocol.getXStatusInfo().getIcon(protocol.getProfile().xstatusIndex);
            menu.addItem("set_xstatus", icon, MENU_XSTATUS);
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            menu.addItem("private_status", PrivateStatusForm.getIcon(protocol),
                    MENU_PRIVATE_STATUS);
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        if (protocol.isConnected()) {
            boolean hasVCard = true;
            // #sijapp cond.if protocols_JABBER is "true" #
            if (protocol instanceof Jabber) {
                hasVCard = ((Jabber)protocol).hasVCardEditor();
                if (((Jabber)protocol).hasS2S()) {
                    menu.addItem("service_discovery", MENU_DISCO);
                }
            }
            // #sijapp cond.end #
            if (!main) {
                menu.addItem("manage_contact_list", MENU_GROUPS);
                if (hasVCard) {
                    menu.addItem("myself", MENU_MYSELF);
                }
            }
            // #sijapp cond.if protocols_MRIM is "true" #
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            if (protocol instanceof Mrim) {
                menu.addItem("microblog",
                        ((Mrim) protocol).getMicroBlog().getIcon(), MENU_MICROBLOG);
            }
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
    }
    public void updateMainMenu() {
        int currentCommand = mainMenuView.getSelectedItemCode();
        mainMenu.clean();
        // #sijapp cond.if modules_ANDROID isnot "true" #
        mainMenu.addItem("keylock_enable",  MENU_KEYLOCK);
        // #sijapp cond.end #
        if (0 < getManager().getModel().getProtocolCount()) {
            // #sijapp cond.if modules_MULTI is "true" #
            protocolMenu(mainMenu, getManager().getCurrentProtocol(), true);
            // #sijapp cond.else #
            protocolMenu(mainMenu, getManager().getCurrentProtocol(), false);
            // #sijapp cond.end #
        }
        if (isSmsSupported()) {
            mainMenu.addItem("send_sms", MENU_SEND_SMS);
        }
        mainMenu.addItem("options_lng", MENU_OPTIONS);

        // #sijapp cond.if modules_SOUND is "true" #
        boolean isSilent = Options.getBoolean(Options.OPTION_SILENT_MODE);
        mainMenu.addItem(isSilent ? "#sound_on" : "#sound_off", MENU_SOUND);
        // #sijapp cond.end#

        mainMenu.addItem("about", MENU_ABOUT);
        // #sijapp cond.if target is "MIDP2" #
        if (isCollapsible()) {
            mainMenu.addItem("minimize", MENU_MINIMIZE);
        }
        // #sijapp cond.end#
        mainMenu.addItem("exit", MENU_EXIT);

        mainMenu.setDefaultItemCode(currentCommand);
        mainMenuView.setModel(mainMenu);
        mainMenuView.update();
    }

    private void doExit(boolean anyway) {
        Jimm.getJimm().quit();
    }

    /* Command listener */
    public void textboxAction(InputTextBox box, boolean ok) {
        if ((box == passwordTextBox) && ok) {
            final Protocol protocol = activeProtocol;
            protocol.setPassword(passwordTextBox.getString());
            passwordTextBox.back();
            if (!StringConvertor.isEmpty(protocol.getPassword())) {
                protocol.connect();
            }
        }
    }

    private void execCommand(int cmd) {
        final Protocol proto = activeProtocol;
        switch (cmd) {
            case MENU_CONNECT:
                if (proto.isEmpty()) {
                    new AccountsForm().showAccountEditor(proto.getProfile());

                } else if (StringConvertor.isEmpty(proto.getPassword())) {
                    passwordTextBox = new InputTextBox().create("password", 32, TextField.PASSWORD);
                    passwordTextBox.setTextBoxListener(this);
                    passwordTextBox.show();

                } else {
                    contactList.restore();
                    proto.connect();
                }
                break;

            case MENU_DISCONNECT:
                proto.disconnect(true);
                Thread.yield();
                activate();
                break;

            case MENU_KEYLOCK:
                Jimm.lockJimm();
                break;

            case MENU_STATUS:
                new SomeStatusForm(proto).show();
                break;

                // #sijapp cond.if modules_XSTATUSES is "true" #
            case MENU_XSTATUS:
                new SomeXStatusForm(proto).show();
                break;
                // #sijapp cond.end #

                // #sijapp cond.if protocols_ICQ is "true" #
                // #sijapp cond.if modules_SERVERLISTS is "true" #
            case MENU_PRIVATE_STATUS:
                new PrivateStatusForm(proto).show();
                break;
                // #sijapp cond.end #
                // #sijapp cond.end #

                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_DISCO:
                ((Jabber)proto).getServiceDiscovery().showIt();
                break;
                // #sijapp cond.end #

                // #sijapp cond.if protocols_MRIM is "true" #
                // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case MENU_MICROBLOG:
                ((Mrim)proto).getMicroBlog().activate();
                updateMainMenu();
                break;
                // #sijapp cond.end #
                // #sijapp cond.end #

            case MENU_OPTIONS:
                new OptionsForm().show();
                break;

            case MENU_ABOUT:
                new SysTextList().makeAbout().show();
                break;

            case MENU_GROUPS:
                new ManageContactListForm(proto).show();
                break;

            case MENU_MINIMIZE:
                /* Minimize Jimm (if supported) */
                contactList.restore();
                Jimm.minimize();
                break;

            // #sijapp cond.if modules_SOUND is "true" #
            case MENU_SOUND:
                Notify.getSound().changeSoundMode(false);
                updateMainMenu();
                break;
            // #sijapp cond.end#

            case MENU_MYSELF:
                proto.showUserInfo(proto.createTempContact(proto.getUserId(), proto.getNick()));
                break;

            case MENU_SEND_SMS:
                new SmsForm(null, null).show();
                break;

            case MENU_EXIT:
                doExit(false);
                break;
        }
    }
    public void select(Select select, MenuModel model, int cmd) {
        execCommand(cmd);
    }
    public final MenuModel getContextMenu(Protocol p, TreeNode node) {
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
        if (node instanceof ProtocolBranch) {
            MenuModel protocolMenu = new MenuModel();
            protocolMenu(protocolMenu, p, false);
            protocolMenu.setActionListener(this);
            return protocolMenu;
        }
        // #sijapp cond.end #
        return null;
    }
}