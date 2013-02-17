/*
 * MrimPhoneContact.java
 *
 * Created on 22 Октябрь 2008 г., 21:41
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.mrim;

import DrawControls.icons.*;
import jimm.ui.menu.*;
import protocol.*;
// #sijapp cond.if protocols_MRIM is "true" #
/**
 *
 * @author Vladimir Krukov
 */
public class MrimPhoneContact extends MrimContact {
    static final String PHONE_UIN = "pho" + "ne";

    public MrimPhoneContact(String phones) {
        super(PHONE_UIN, PHONE_UIN);
        setPhones(phones);
        setName(phones);
        setBooleanValue(Contact.CONTACT_NO_AUTH, false);
    }

    int getFlags() {
        return 0x100000;
    }

    public void getLeftIcons(Icon[] icons) {
        icons[0] = Mrim.getPhoneContactIcon();
    }
    public void activate(Protocol p) {
        if (hasChat()) {
            p.getChat(this).activate();
            
        } else {
            new ContactMenu(p, this).doAction(USER_MENU_SEND_SMS);
        }
    }
    protected void initContextMenu(Protocol protocol, MenuModel contactMenu) {
        contactMenu.addItem("send_sms", USER_MENU_SEND_SMS);
        contactMenu.addItem("info", Contact.USER_MENU_USER_INFO);
        if ((protocol.getGroupItems().size() > 1) && !isTemp()) {
            contactMenu.addItem("move_to_group", USER_MENU_MOVE);
        }
        contactMenu.addItem("remove", USER_MENU_USER_REMOVE);
        contactMenu.addItem("rename", USER_MENU_RENAME);
    }
    public boolean isVisibleInContactList() {
        return true;
    }
}
// #sijapp cond.end#