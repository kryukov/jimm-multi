/*
 * MrimChatContact.java
 *
 * Created on 25 Апрель 2010 г., 22:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MRIM is "true" #
package protocol.mrim;

import java.util.Vector;
import jimm.ui.menu.MenuModel;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class MrimChatContact extends MrimContact {

    /** Creates a new instance of MrimChatContact */
    public MrimChatContact(String uin, String name) {
        super(uin, name);
        setStatus(StatusInfo.STATUS_ONLINE, null);
    }

    private Vector members = new Vector();
    void setMembers(Vector inChat) {
        members = inChat;
    }
    Vector getMembers() {
        return members;
    }
    public boolean hasHistory() {
        return false;
    }
    public boolean isSingleUserContact() {
        return false;
    }
    protected void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        if (isTemp()) {
            contactMenu.addItem("connect", USER_MENU_ADD_USER);
            contactMenu.setDefaultItemCode(USER_MENU_ADD_USER);
        } else {
            contactMenu.addItem("leave_chat", CONFERENCE_DISCONNECT);
            contactMenu.addItem("list_of_users", USER_MENU_USERS_LIST);
            contactMenu.setDefaultItemCode(USER_MENU_USER_REMOVE);
        }
        addChatItems(contactMenu);
    }
}
// #sijapp cond.end #