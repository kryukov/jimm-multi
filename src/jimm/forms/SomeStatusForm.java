/*
 * SomeStatusForm.java
 *
 * Created on 22 Январь 2010 г., 12:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import jimm.cl.ContactList;
import jimm.ui.menu.*;
import protocol.Protocol;
import protocol.StatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class SomeStatusForm implements SelectListener {
    private Protocol protocol;
    
    public SomeStatusForm(Protocol protocol) {
        this.protocol = protocol;
    }

    private void addStatuses(MenuModel menu, byte[] statuses) {
        StatusInfo info = protocol.getStatusInfo();
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(protocol.getProfile().statusIndex);
    }

    public final void select(Select select, MenuModel model, int statusIndex) {
        protocol.setOnlineStatus(statusIndex, null);
        ContactList.getInstance().activate();
    }
    public final void show() {
        MenuModel menu = new MenuModel();
        addStatuses(menu, protocol.getStatusList());
        menu.setActionListener(this);
        new Select(menu).show();
    }
}
