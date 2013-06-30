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
import jimmui.view.InputTextBox;
import jimmui.view.TextBoxListener;
import jimmui.view.menu.*;
import protocol.Protocol;
import protocol.StatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class SomeStatusForm implements SelectListener, TextBoxListener {
    private Protocol protocol;
    private InputTextBox passwordTextBox;
    private int selectedStatus;

    public SomeStatusForm(Protocol protocol) {
        this.protocol = protocol;
    }

    public final void show() {
        MenuModel menu = new MenuModel();
        addStatuses(menu, protocol.getStatusInfo(), protocol.getProfile().statusIndex);
        menu.setActionListener(this);
        new Select(menu).show();
    }

    public final void select(Select select, MenuModel model, int status) {
        selectedStatus = status;
        boolean connecting = (StatusInfo.STATUS_OFFLINE != status);
        if (connecting && StringConvertor.isEmpty(protocol.getPassword())) {
            requestPassword();
        } else {
            setStatus();
        }
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        if (ok && (box == passwordTextBox)) {
            protocol.setPassword(passwordTextBox.getString());
            passwordTextBox.back();
            if (!StringConvertor.isEmpty(protocol.getPassword())) {
                setStatus();
            }
        }
    }

    private void addStatuses(MenuModel menu, StatusInfo info, int current) {
        byte[] statuses = info.applicableStatuses;
        final byte offline = StatusInfo.STATUS_OFFLINE;
        menu.addItem(info.getName(offline), info.getIcon(offline), offline);
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(current);
    }
    private void requestPassword() {
        passwordTextBox = new InputTextBox().create("password", 32, InputTextBox.PASSWORD);
        passwordTextBox.setTextBoxListener(this);
        passwordTextBox.show();
    }
    private void setStatus() {
        protocol.setStatus(selectedStatus, null);
        ContactList.getInstance().activate();
    }
}
