/*
 * AccountsForm.java
 *
 * Created on 11 Март 2011 г., 23:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm;

import jimm.ui.text.TextList;
import java.util.Vector;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.ui.form.*;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.ui.text.TextListModel;
import jimm.util.JLocale;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class AccountsForm implements FormListener, SelectListener, ControlStateListener {
    private GraphForm form;
    private MenuModel accountMenu = null;
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
        ContactList cl = ContactList.getInstance();
        Vector listOfProfiles = new Vector();
        // #sijapp cond.if modules_MULTI is "true" #
        for (int i = 0; i < Options.getAccountCount(); ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                listOfProfiles.addElement(p);
            }
        }
        if (listOfProfiles.isEmpty()) {
            Profile p = Options.getAccount(0);
            p.isActive = true;
            listOfProfiles.addElement(p);
        }
        // #sijapp cond.else #
        if (0 < Options.getAccountCount()) {
            listOfProfiles.addElement(Options.getAccount(Options.getCurrentAccount()));
        }
        // #sijapp cond.end #
        cl.addProtocols(listOfProfiles);
        cl.getManager().update();
        ContactList.getInstance().updateMainMenu();
    }

    ///////////////////////////////////////////////////////////////////////////

    private static final int MENU_ACCOUNT_EDIT        = 0;
    private static final int MENU_ACCOUNT_DELETE      = 1;
    private static final int MENU_ACCOUNT_UP          = 2;
    private static final int MENU_ACCOUNT_DOWN        = 3;
    private static final int MENU_ACCOUNT_SET_CURRENT = 4;
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
        int curItem = accountList.getCurrItem();
        int current = Options.getCurrentAccount();
        int accountCount = Options.getAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile account = Options.getAccount(i);
            boolean isCurrent = (current == i);
            // #sijapp cond.if modules_MULTI is "true" #
            isCurrent = account.isActive;
            // #sijapp cond.end #
            String text = account.userId + (isCurrent ? "*" : "");
            // #sijapp cond.if modules_MULTI is "true" #
            text = getProtocolName(account.protocolType) + ":\n" + text;
            // #sijapp cond.end #
            accountListModel.addItem(text, isCurrent);
        }
        final int maxAccount = Options.getMaxAccountCount();
        if (accountCount < maxAccount) {
            accountListModel.addItem(JLocale.getString("add_new"), false);
        }
        accountList.setModel(accountListModel, curItem);

        accountMenu = new MenuModel();
        boolean connected = ContactList.getInstance().isConnected();
        // #sijapp cond.if modules_MULTI is "true" #
        connected = false;
        // #sijapp cond.end #
        int defCount = MENU_ACCOUNT_EDIT;
        if ((0 < accountCount) && !connected) {
            // #sijapp cond.if modules_MULTI isnot "true" #
            accountMenu.addItem("set_current", MENU_ACCOUNT_SET_CURRENT);
            defCount = MENU_ACCOUNT_SET_CURRENT;
            // #sijapp cond.else #
            accountMenu.addItem("set_active", MENU_ACCOUNT_SET_ACTIVE);
            defCount = MENU_ACCOUNT_SET_ACTIVE;
            // #sijapp cond.end #
        }
        accountMenu.addItem("edit", MENU_ACCOUNT_EDIT);
        if ((0 < accountCount) && !connected) {
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
    private GraphForm initAccountEditor(int accNum) {
        editAccountNum = accNum;
        Profile account = Options.getAccount(editAccountNum);
        form = new GraphForm("options_account", "save", "back", this);

        // #sijapp cond.if modules_MULTI is "true"#
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
        // #sijapp cond.end #
        form.addLatinTextField(uinField, "UserID", account.userId, 64);
        form.addPasswordField(passField, "password", account.password, 40);
        form.addTextField(nickField, "nick", account.nick, 20);
        // #sijapp cond.if modules_MULTI is "true"#
        form.setControlStateListener(this);
        // #sijapp cond.end#
        updateAccountForm();
        return form;
    }

    public void controlStateChanged(GraphForm form, int controlId) {
        if (protocolTypeField == controlId) {
            updateAccountForm();
        }
    }
    private void updateAccountForm() {
        int id = 0;
        // #sijapp cond.if modules_MULTI is "true"#
        id = form.getSelectorValue(protocolTypeField);
        // #sijapp cond.end#
        form.setTextFieldLabel(uinField, Profile.protocolIds[id]);
    }
    public boolean setCurrentAccount(int accNum) {
        if (Options.getAccountCount() <= accNum) {
            return false;
        }
        if (accNum != Options.getCurrentAccount()) {
            Options.setCurrentAccount(accNum);
            // #sijapp cond.if modules_MULTI isnot "true"#
            Options.safeSave();
            // #sijapp cond.end#
            setCurrentProtocol();
            updateAccountList();
        }
        return true;
    }
    public void select(Select select, MenuModel menu, int cmd) {
        int num = accountList.getCurrItem();

        switch (cmd) {
                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_ACCOUNT_CREATE:
                accountList.restore();
                new protocol.jabber.JabberRegistration(this).show();
                break;
                // #sijapp cond.end #

            case MENU_ACCOUNT_UP:
                if ((0 != num) && (num < Options.getAccountCount())) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num - 1);
                    Options.setAccount(num - 1, up);
                    Options.setAccount(num, down);
                    accountList.setCurrentItemIndex(num - 1);
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
                    accountList.setCurrentItemIndex(num + 1);
                    setCurrentProtocol();
                    updateAccountList();
                }
                accountList.restore();
                break;
        }
        // #sijapp cond.if modules_MULTI is "true" #
        Profile account = Options.getAccount(num);
        Protocol p = ContactList.getInstance().getProtocol(account);
        if ((null != p) && p.isConnected()) {
            return;
        }
        // #sijapp cond.end #

        switch (cmd) {
            case MENU_ACCOUNT_DELETE:
                Options.delAccount(num);
                setCurrentProtocol();
                Options.safeSave();
                updateAccountList();
                accountList.restore();
                break;

                // #sijapp cond.if modules_MULTI is "true" #
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

                // #sijapp cond.end #
            case MENU_ACCOUNT_SET_CURRENT:
                if (setCurrentAccount(num)) {
                    accountList.restore();
                    break;
                }
                // break absent. It isn't bug!
                // create account if not exist
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
    public void formAction(GraphForm form, boolean apply) {
        if (apply) {
            // Save values, depending on selected option menu item
            Profile account = new Profile();
            if (1 < Profile.protocolTypes.length) {
                account.protocolType = Profile.protocolTypes[form.getSelectorValue(protocolTypeField)];
            }
            account.userId = form.getTextFieldValue(uinField).trim();
            if (StringConvertor.isEmpty(account.userId)) {
                return;
            }
            account.password = form.getTextFieldValue(passField);
            account.nick = form.getTextFieldValue(nickField);
            // #sijapp cond.if modules_MULTI is "true" #
            if (Options.getAccountCount() <= editAccountNum) {
                account.isActive = true;
            } else {
                account.isActive = Options.getAccount(editAccountNum).isActive;
            }
            // #sijapp cond.end #
            addAccount(editAccountNum, account);
        }
        form.back();
    }
    /** Creates a new instance of AccountsForm */
    public AccountsForm() {
    }

}
