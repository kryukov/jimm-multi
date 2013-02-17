/*
 * ContactMenu.java
 *
 * Created on 17 Июнь 2011 г., 0:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimm.*;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.forms.ManageContactListForm;
import jimm.history.*;
import jimm.ui.menu.*;

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
                protocol.getChat(contact).writeMessage(null);
                break;
                
            case Contact.USER_MENU_QUOTE: /* Send plain message with quotation */
            case Contact.USER_MENU_PASTE: /* Send plain message without quotation */
                protocol.getChat(contact).writeMessage(JimmUI.getClipBoardText(Contact.USER_MENU_QUOTE == cmd));
                break;
                
            case Contact.USER_MENU_ADD_USER:
                protocol.getSearchForm().show(contact.getUserId());
                break;

            case Contact.USER_MENU_USER_REMOVE:
                // #sijapp cond.if modules_HISTORY is "true" #
                HistoryStorage.getHistory(contact).removeHistory();
                // #sijapp cond.end#
                protocol.removeContact(contact);
                ContactList.getInstance().activate();
                break;

            case Contact.USER_MENU_STATUSES: /* Show user statuses */
                protocol.showStatus(contact);
                break;

            case Contact.USER_MENU_WAKE:
                protocol.sendMessage(contact, PlainMessage.CMD_WAKEUP, true);
                protocol.getChat(contact).activate();
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
                    new Select(manageContact).show();
                }
                break;

            case Contact.USER_MENU_REQU_AUTH: /* Request auth */
                protocol.requestAuth(contact.getUserId());
                Jimm.getJimm().getDisplay().closeMenus();
                break;

            case Contact.USER_MENU_GRANT_AUTH:
                protocol.grandAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                Jimm.getJimm().getDisplay().closeMenus();
                break;

            case Contact.USER_MENU_DENY_AUTH:
                protocol.denyAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                Jimm.getJimm().getDisplay().closeMenus();
                break;
                
            default:
                protocol.doAction(contact, cmd);
        }
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    private void showHistory() {
        if (contact.hasHistory()) {
            HistoryStorage history;
            if (contact.hasChat()) {
                history = protocol.getChat(contact).getHistory();
            } else {
                history = HistoryStorage.getHistory(contact);
            }
            new HistoryStorageList(history).show();
        }
    }
    // #sijapp cond.end#
}
