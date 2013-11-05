/*
 * ContactMenu.java
 *
 * Created on 17 Июнь 2011 г., 0:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.ui;

import jimm.*;
import jimm.chat.message.PlainMessage;
import jimm.forms.ManageContactListForm;
import jimm.history.*;
import jimmui.Clipboard;
import jimmui.view.UIBuilder;
import jimmui.view.menu.*;
import protocol.Contact;
import protocol.Protocol;

/**
 *
 * @author Vladimir Kryukov
 */
public class ContactMenu implements SelectListener {
    private Contact contact;
    private Protocol protocol;
    

    public ContactMenu(Protocol p, Contact c) {
        contact = c;
        protocol = p;
    }
    public void select(Select select, MenuModel model, int cmd) {
        doAction(cmd);
    }

    public MenuModel getContextMenu() {
        MenuModel contactMenu = new MenuModel();
        contact.initContextMenu(protocol, contactMenu);
        contactMenu.setActionListener(this);
        return (0 < contactMenu.count()) ? contactMenu : null;
    }
    public void doAction(int cmd) {
        switch (cmd) {
            case Contact.USER_MENU_MESSAGE: /* Send plain message */
                Jimm.getJimm().getChatUpdater().writeMessage(protocol, contact, null);
                break;
                
            case Contact.USER_MENU_PASTE: /* Send plain message without quotation */
                Jimm.getJimm().getChatUpdater().writeMessage(protocol, contact, Clipboard.getClipBoardText());
                break;
                
            case Contact.USER_MENU_ADD_USER:
                new ManageContactListForm(protocol, contact).showContactAdd();
                break;

            case Contact.USER_MENU_USER_REMOVE:
                // #sijapp cond.if modules_HISTORY is "true" #
                HistoryStorage.getHistory(contact).removeHistory();
                // #sijapp cond.end#
                protocol.removeContact(contact);
                Jimm.getJimm().getCL().activate();
                break;

            case Contact.USER_MENU_STATUSES: /* Show user statuses */
                protocol.showStatus(contact);
                break;

            case Contact.USER_MENU_WAKE:
                protocol.sendMessage(contact, PlainMessage.CMD_WAKEUP, true);
                Jimm.getJimm().getChatUpdater().activate(Jimm.getJimm().jimmModel.getChatModel(contact));
                break;

            // #sijapp cond.if modules_FILES is "true"#
            case Contact.USER_MENU_FILE_TRANS:
                // Send a filetransfer with a file given by path
                new FileTransfer(protocol, contact).startFileTransfer();
                break;
                
            case Contact.USER_MENU_CAM_TRANS:
                // Send a filetransfer with a camera image
                new FileTransfer(protocol, contact).startPhotoTransfer();
                break;
            // #sijapp cond.end#

            case Contact.USER_MENU_RENAME:
                /* Rename the contact local and on the server
                   Reset and display textbox for entering name */
                new ManageContactListForm(protocol, contact).showContactRename();
                break;
                                
            // #sijapp cond.if modules_HISTORY is "true" #
            case Contact.USER_MENU_HISTORY: /* Stored history */
                showHistory();
                break;
            // #sijapp cond.end #

            case Contact.USER_MENU_MOVE:
                new ManageContactListForm(protocol, contact).showContactMove();
                break;
            
            case Contact.USER_MENU_USER_INFO:
                protocol.showUserInfo(contact);
                break;

            case Contact.USER_MANAGE_CONTACT:
                MenuModel manageContact = new MenuModel();
                contact.initManageContactMenu(protocol, manageContact);
                manageContact.setActionListener(this);
                if (0 < manageContact.count()) {
                    UIBuilder.createMenu(manageContact).show();
                }
                break;

            case Contact.USER_MENU_REQU_AUTH: /* Request auth */
                protocol.requestAuth(contact);
                Jimm.getJimm().getDisplay().closeMenus();
                break;

            case Contact.USER_MENU_GRANT_AUTH:
                protocol.grandAuth(contact.getUserId());
                protocol.getChatModel(contact).resetAuthRequests();
                Jimm.getJimm().getDisplay().closeMenus();
                break;

            case Contact.USER_MENU_DENY_AUTH:
                protocol.denyAuth(contact.getUserId());
                protocol.getChatModel(contact).resetAuthRequests();
                Jimm.getJimm().getDisplay().closeMenus();
                break;
                
            default:
                protocol.doAction(contact, cmd);
        }
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    private void showHistory() {
        if (contact.hasHistory()) {
            Jimm.getJimm().getChatUpdater().showHistory(contact);
        }
    }
    // #sijapp cond.end#
}
