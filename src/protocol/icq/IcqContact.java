/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-06  Jimm Project

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
 File: src/jimm/IcqContact.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq;

import jimm.ui.menu.*;
import DrawControls.icons.*;
import protocol.*;

public class IcqContact extends Contact {
    private static final ImageList happyIcon = ImageList.createImageList("/happy.png");

    public boolean happyFlag;

    private short contactId;

    // Static constants for menu actios
    public static final int USER_MENU_REMOVE_ME = 8;


    public int getContactId() {
        return ((int)contactId) & 0xFFFF;
    }
    public void setContactId(int id) {
        contactId = (short)id;
    }
///////////////////////////////////////////////////////////////////////////
    public Icon getHappyIcon() {
        return happyFlag ? happyIcon.iconAt(0) : null;
    }

    public void getLeftIcons(Icon[] leftIcons) {
        leftIcons[2] = getHappyIcon();
        super.getLeftIcons(leftIcons);
    }

///////////////////////////////////////////////////////////////////////////
    public void init(int id, int groupId, String name, boolean noAuth) {
        setContactId(id);
        setGroupId(groupId);
        setName(name);
        setBooleanValue(Contact.CONTACT_NO_AUTH, noAuth);
        setTempFlag(false);
        setOfflineStatus();
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        setBooleanValue(Contact.SL_IGNORE, false);
        setBooleanValue(Contact.SL_VISIBLE, false);
        setBooleanValue(Contact.SL_INVISIBLE, false);
        // #sijapp cond.end #
    }

    public IcqContact(String uin) {
        this.userId = uin;
        setOfflineStatus();
    }

    /** ************************************************************************* */
    public final void setOfflineStatus() {
        super.setOfflineStatus();
        happyFlag = false;
    }

    /****************************************************************************/


    // #sijapp cond.if modules_XSTATUSES is "true" #
    public final void setXStatusMessage(String text) {
        setXStatus(getXStatusIndex(), text);
    }
    // #sijapp cond.end #

    public void setStatusMessage(String msg) {
        setStatus(getStatusIndex(), msg);
    }



    protected void initManageContactMenu(Protocol protocol, MenuModel menu) {
        boolean connected = protocol.isConnected();
        boolean temp = isTemp();
        boolean inList = protocol.inContactList(this);
        if (connected) {
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            initPrivacyMenu(menu);
            // #sijapp cond.end #
            if (temp) {
                menu.addItem("add_user", USER_MENU_ADD_USER);

            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.addItem("move_to_group", USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.addItem("requauth", USER_MENU_REQU_AUTH);
                }
                if (inList) {
                    menu.addItem("rename", USER_MENU_RENAME);
                }
            }
        }
        if (connected || (temp && inList)) {
            menu.addSeparator();
            if (connected) {
                menu.addItem("remove_me", USER_MENU_REMOVE_ME);
            }
            if (inList) {
                menu.addItem("remove", USER_MENU_USER_REMOVE);
            }
        }
    }
}
// #sijapp cond.end #

