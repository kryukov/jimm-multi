/*
 * SomeStatusForm.java
 *
 * Created on 22 Январь 2010 г., 12:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import jimm.Jimm;
import jimm.comm.StringUtils;
import jimmui.view.InputTextBox;
import jimmui.view.TextBoxListener;
import jimmui.view.UIBuilder;
import jimmui.view.menu.*;
import protocol.Protocol;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;

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
        addStatuses(menu, InfoFactory.factory.getStatusInfo(protocol), protocol.getProfile().statusIndex);
        menu.setActionListener(this);
        UIBuilder.createMenu(menu).show();
    }

    public final void select(Select select, MenuModel model, int status) {
        selectedStatus = status;
        boolean connecting = (StatusInfo.STATUS_OFFLINE != status);
        if (connecting && StringUtils.isEmpty(protocol.getPassword())) {
            requestPassword();
        } else {
            setStatus();
        }
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        if (ok && (box == passwordTextBox)) {
            protocol.setPassword(passwordTextBox.getString());
            passwordTextBox.back();
            if (!StringUtils.isEmpty(protocol.getPassword())) {
                setStatus();
            }
        }
    }

    private void addStatuses(MenuModel menu, StatusInfo info, int current) {
        byte[] statuses = info.applicableStatuses;
        final byte offline = StatusInfo.STATUS_OFFLINE;
        menu.addItem(info.getName(offline), info.getIcon(offline), offline);
        for (byte status : statuses) {
            menu.addItem(info.getName(status), info.getIcon(status), status);
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
        Jimm.getJimm().getCL().activate();
    }
}
