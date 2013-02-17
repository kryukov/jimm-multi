/*
 * ObimpContact.java
 *
 * Created on 5 Декабрь 2010 г., 13:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_OBIMP is "true" #
package protocol.obimp;

import jimm.ui.menu.MenuModel;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ObimpContact extends Contact {
    private int id;
    private byte privacy;
    private boolean general;
    public ObimpContact(String userID, String name) {
        this.userId = userID;
        setGroupId(Group.NOT_IN_GROUP);
        this.setName(name);
        setOfflineStatus();
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    public void setPrivacyType(byte privacy) {
        this.privacy = privacy;
    }
    public byte getPrivacyType() {
        return privacy;
    }

    void setGeneral(boolean g) {
        general = g;
    }
    boolean isGeneral() {
        return general;
    }

    /////////////////////////////////////////////////////////////////////////
//    public void getRightIcons(Icon[] rightIcons) {
//    }
    
    /////////////////////////////////////////////////////////////////////////


    protected void initManageContactMenu(Protocol protocol, MenuModel menu) {
        if (protocol.isConnected()) {
            if (isTemp()) {
                menu.addItem("add_user", USER_MENU_ADD_USER);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.addItem("move_to_group", USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addItem("requauth", USER_MENU_REQU_AUTH);
                }
                menu.addItem("rename", USER_MENU_RENAME);
            }
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            menu.addSeparator();
            menu.addItem("remove", USER_MENU_USER_REMOVE);
        }
    }

    void setXStatus(long xIndex, String xstatusDesc) {
        // #sijapp cond.if modules_XSTATUSES is "true" #
        setXStatus((int)xIndex, xstatusDesc);
        // #sijapp cond.end #
    }
}
// #sijapp cond.end #