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
import jimm.comm.StringConvertor;
import jimm.ui.InputTextBox;
import jimm.ui.TextBoxListener;
import jimm.ui.menu.*;
import protocol.Protocol;
import protocol.StatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class SomeStatusForm implements SelectListener, TextBoxListener {
    private Protocol protocol;
    private InputTextBox passwordTextBox;

    public SomeStatusForm(Protocol protocol) {
        this.protocol = protocol;
    }

    private void addStatuses(MenuModel menu, byte[] statuses) {
        StatusInfo info = protocol.getStatusInfo();
        menu.addItem(info.getName(StatusInfo.STATUS_OFFLINE),
                info.getIcon(StatusInfo.STATUS_OFFLINE),
                StatusInfo.STATUS_OFFLINE);
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(protocol.getProfile().statusIndex);
    }

    public final void select(Select select, MenuModel model, int status) {
        protocol.setOnlineStatus(status, null);
        boolean connected = protocol.isConnected() || protocol.isConnecting();
        boolean connecting = (StatusInfo.STATUS_OFFLINE != status);
        if (connecting) {
            if (!connected) {
                connect();
                return;
            }
        } else {
            if (connected) {
                disconnect();
                return;
            }
        }
        ContactList.getInstance().activate();
    }
    public final void show() {
        MenuModel menu = new MenuModel();
        addStatuses(menu, protocol.getStatusList());
        menu.setActionListener(this);
        new Select(menu).show();
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        if (box == passwordTextBox) {
            if (ok) {
                protocol.setPassword(passwordTextBox.getString());
                passwordTextBox.back();
                if (!StringConvertor.isEmpty(protocol.getPassword())) {
                    connect();
                    return;
                }
            }
            protocol.setOnlineStatus(StatusInfo.STATUS_OFFLINE, null);
        }
    }

    private void connect() {
        if (StringConvertor.isEmpty(protocol.getPassword())) {
            passwordTextBox = new InputTextBox().create("password", 32, InputTextBox.PASSWORD);
            passwordTextBox.setTextBoxListener(this);
            passwordTextBox.show();
        } else {
            protocol.connect();
            ContactList.getInstance().activate();
        }
    }
    private void disconnect() {
        protocol.disconnect(true);
        ContactList.getInstance().activate();
    }
}
