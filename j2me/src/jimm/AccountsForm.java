/*
 * AccountsForm.java
 *
 * Created on 11 Март 2011 г., 23:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm;

import jimm.comm.StringUtils;
import jimmui.view.UIBuilder;
import jimmui.view.form.ControlStateListener;
import jimmui.view.form.Form;
import jimmui.view.form.FormListener;
import jimmui.view.text.TextList;
import jimmui.view.menu.*;
import jimmui.view.text.TextListController;
import jimmui.view.text.TextListModel;
import jimm.util.JLocale;
import protocol.*;
import protocol.xmpp.XmppRegistration;

/**
 *
 * @author Vladimir Kryukov
 */
public class AccountsForm implements FormListener, SelectListener, ControlStateListener {
    private Form form;
    private int editAccountNum;
    private TextList accountList = new TextList(JLocale.getString("options_account"));

    private static final int protocolTypeField = 1011;
    private static final int uinField = 1012;
    private static final int passField = 1013;
    private static final int nickField = 1014;
    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        setCurrentProtocol();
        updateAccountList();
    }

    private void setCurrentProtocol() {
        Jimm.getJimm().jimmModel.updateAccounts();
        Jimm.getJimm().getCL().updateCl();
    }

    ///////////////////////////////////////////////////////////////////////////

    private static final int MENU_ACCOUNT_EDIT        = 0;
    private static final int MENU_ACCOUNT_DELETE      = 1;
    private static final int MENU_ACCOUNT_UP          = 2;
    private static final int MENU_ACCOUNT_DOWN        = 3;
    private static final int MENU_ACCOUNT_SET_ACTIVE  = 5;
    private static final int MENU_ACCOUNT_CREATE      = 6;
    //private static final int MENU_ACCOUNT_BACK        = 6;

    private String getProtocolName(byte type) {
        for (int i = 0; i < Profile.protocolNames.length; ++i) {
            if (Profile.protocolTypes[i] == type) {
                return Profile.protocolNames[i];
            }
        }
        return null;
    }
    private void updateAccountList() {
        TextListModel accountListModel = new TextListModel();
        int curItem = accountList.getTextContent().getCurrItem();
        int accountCount = Options.getAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile account = Options.getAccount(i);
            String text = getProtocolName(account.protocolType) + ":\n"
                    + account.userId + (account.isActive ? "*" : "");
            accountListModel.addItem(text, account.isActive);
        }
        final int maxAccount = Options.getMaxAccountCount();
        if (accountCount < maxAccount) {
            accountListModel.addItem(JLocale.getString("add_new"), false);
        }
        accountList.setModel(accountListModel, curItem);

        MenuModel accountMenu = new MenuModel();
        int defCount = MENU_ACCOUNT_EDIT;
        if (0 < accountCount) {
            accountMenu.addItem("set_active", MENU_ACCOUNT_SET_ACTIVE);
            defCount = MENU_ACCOUNT_SET_ACTIVE;
        }
        accountMenu.addItem("edit", MENU_ACCOUNT_EDIT);
        if (0 < accountCount) {
//            if (accountCount < maxAccount) {
//                accountMenu.addItem("add_new", MENU_ACCOUNT_NEW);
//            }
            accountMenu.addItem("delete", MENU_ACCOUNT_DELETE);
        }
        if (1 < accountCount) {
            accountMenu.addItem("lift up", MENU_ACCOUNT_UP);
            accountMenu.addItem("put down", MENU_ACCOUNT_DOWN);
        }
        // #sijapp cond.if protocols_JABBER is "true" #
        accountMenu.addItem("register_new", MENU_ACCOUNT_CREATE);
        // #sijapp cond.end #

        accountMenu.setActionListener(this);
        accountMenu.setDefaultItemCode(MENU_ACCOUNT_EDIT);
        TextListController controller = new TextListController();
        controller.setMenu(accountMenu, defCount);

        accountList.setModel(accountListModel);
        accountList.setController(controller);
    }

    public void showAccountEditor(Profile p) {
        initAccountEditor(Options.getAccountIndex(p)).show();
    }
    private Form initAccountEditor(int accNum) {
        editAccountNum = accNum;
        Profile account = Options.getAccount(editAccountNum);
        form = UIBuilder.createForm("options_account", "save", "back", this);

        if (1 < Profile.protocolTypes.length) {
            int protocolIndex = 0;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (account.protocolType == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            form.addSelector(protocolTypeField, "protocol", Profile.protocolNames, protocolIndex);
        }
        form.addLatinTextField(uinField, "UserID", account.userId, 64);
        form.addPasswordField(passField, "password", account.password, 40);
        form.addTextField(nickField, "nick", account.nick, 20);
        form.setControlStateListener(this);
        updateAccountForm();
        return form;
    }

    public void controlStateChanged(Form form, int controlId) {
        if (protocolTypeField == controlId) {
            updateAccountForm();
        }
    }
    private void updateAccountForm() {
        int id = form.getSelectorValue(protocolTypeField);
        form.setTextFieldLabel(uinField, Profile.protocolIds[id]);
    }
    public void select(Select select, MenuModel menu, int cmd) {
        int num = accountList.getContent().getCurrItem();

        switch (cmd) {
                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_ACCOUNT_CREATE:
                accountList.restore();
                new XmppRegistration(this).show();
                break;
                // #sijapp cond.end #

            case MENU_ACCOUNT_UP:
                if ((0 != num) && (num < Options.getAccountCount())) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num - 1);
                    Options.setAccount(num - 1, up);
                    Options.setAccount(num, down);
                    accountList.getContent().setCurrentItemIndex(num - 1);
                    setCurrentProtocol();
                    updateAccountList();
                }
                accountList.restore();
                break;

            case MENU_ACCOUNT_DOWN:
                if (num < Options.getAccountCount() - 1) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num + 1);
                    Options.setAccount(num, down);
                    Options.setAccount(num + 1, up);
                    accountList.getContent().setCurrentItemIndex(num + 1);
                    setCurrentProtocol();
                    updateAccountList();
                }
                accountList.restore();
                break;
        }
        Profile account = Options.getAccount(num);
        Protocol p = Jimm.getJimm().jimmModel.getProtocol(account);
        if ((null != p) && p.isConnected()) {
            return;
        }

        switch (cmd) {
            case MENU_ACCOUNT_DELETE:
                Options.delAccount(num);
                setCurrentProtocol();
                Options.safeSave();
                updateAccountList();
                accountList.restore();
                break;

            case MENU_ACCOUNT_SET_ACTIVE:
                if (num < Options.getAccountCount()) {
                    account.isActive = !account.isActive;
                    Options.saveAccount(account);
                    setCurrentProtocol();
                    updateAccountList();
                    accountList.restore();
                    break;
                }
                initAccountEditor(num).show();
                break;

//            case MENU_ACCOUNT_NEW:
            case MENU_ACCOUNT_EDIT:
                initAccountEditor(num).show();
                break;
        }
    }

    public void show() {
        updateAccountList();
        accountList.show();
    }
    public void formAction(Form form, boolean apply) {
        if (apply) {
            // Save values, depending on selected option menu item
            Profile account = new Profile();
            if (1 < Profile.protocolTypes.length) {
                account.protocolType = Profile.protocolTypes[form.getSelectorValue(protocolTypeField)];
            }
            account.userId = form.getTextFieldValue(uinField).trim();
            if (StringUtils.isEmpty(account.userId)) {
                return;
            }
            account.password = form.getTextFieldValue(passField);
            account.nick = form.getTextFieldValue(nickField);
            account.isActive = Options.getAccountCount() <= editAccountNum || Options.getAccount(editAccountNum).isActive;
            addAccount(editAccountNum, account);
        }
        form.back();
    }
    /** Creates a new instance of AccountsForm */
    public AccountsForm() {
    }

}
