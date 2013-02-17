/*
 * ManageContactListForm.java
 *
 * Created on 10 Июнь 2007 г., 21:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import java.util.Vector;
import jimm.cl.ContactList;
import jimm.search.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.*;
import protocol.*;

/**
 *
 * @author vladimir
 */
public final class ManageContactListForm implements TextBoxListener, SelectListener {
    private static final int ADD_USER     = 1;
    private static final int SEARCH_USER  = 2;
    private static final int ADD_GROUP    = 3;
    private static final int RENAME_GROUP = 4;
    private static final int DEL_GROUP    = 5;

    private Protocol protocol;

    private Group group;
    private InputTextBox groupName;

    private Contact contact;
    private InputTextBox renameContactTextbox;
    private MenuModel groupList;
    private int action;

    /** Creates a new instance of ManageContactListForm */
    public ManageContactListForm(Protocol protocol) {
        this(protocol, (Group)null);
    }
    public ManageContactListForm(Protocol protocol, Group group) {
        this.protocol = protocol;
        this.group = group;
    }
    public ManageContactListForm(Protocol protocol, Contact contact) {
        this.protocol = protocol;
        this.contact = contact;
    }
    public void showContactRename() {
        renameContactTextbox = new InputTextBox().create("rename", 64);
        renameContactTextbox.setString(contact.getName());
        renameContactTextbox.setTextBoxListener(this);
        renameContactTextbox.show();
    }
    public void showContactMove() {
        Vector groups = protocol.getGroupItems();
        Group myGroup = protocol.getGroup(contact);
        groupList = new MenuModel();
        for (int i = 0; i < groups.size(); ++i) {
            Group g = (Group)groups.elementAt(i);
            if ((myGroup != g) && g.hasMode(Group.MODE_NEW_CONTACTS)) {
                groupList.addRawItem(g.getName(), null, g.getId());
            }
        }
        groupList.setActionListener(this);
        new Select(groupList).show();
    }

    public MenuModel getMenu() {
        MenuModel manageCL = new MenuModel();
        boolean canAdd = !protocol.getGroupItems().isEmpty()
                && ((null == group) || group.hasMode(Group.MODE_NEW_CONTACTS));
        if (canAdd) {
            manageCL.addItem("add_user", ADD_USER);
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!(protocol instanceof protocol.jabber.Jabber)) {
                manageCL.addItem("search_user", SEARCH_USER);
            }
            // #sijapp cond.else #
            manageCL.addItem("search_user", SEARCH_USER);
            // #sijapp cond.end #
        }
        manageCL.addItem("add_group", ADD_GROUP);
        if (null != group) {
            if (group.hasMode(Group.MODE_EDITABLE)) {
                manageCL.addItem("rename_group", RENAME_GROUP);
            }
            if (group.getContacts().isEmpty() && group.hasMode(Group.MODE_REMOVABLE)) {
                manageCL.addItem("del_group", DEL_GROUP);
            }
        }
        manageCL.setActionListener(this);
        return manageCL;
    }

    public void select(Select select, MenuModel model, int cmd) {
        if (groupList == model) {
            groupList = null;
            protocol.moveContactTo(contact, protocol.getGroupById(cmd));
            ContactList.getInstance().activate();
            return;
        }
        action = cmd;
        switch (cmd) {
            case ADD_USER: /* Add user */
                Search search = protocol.getSearchForm();
                search.putToGroup(group);
                search.show("");
                break;

            case SEARCH_USER: /* Search for User */
                Search searchUser = protocol.getSearchForm();
                searchUser.putToGroup(group);
                searchUser.show();
                break;

            case ADD_GROUP: /* Add group */
                showTextBox("add_group", null);
                break;

            case RENAME_GROUP: /* Rename group */
                showTextBox("rename_group", group.getName());
                break;

            case DEL_GROUP: /* Delete group */
                protocol.removeGroup(group);
                ContactList.getInstance().activate();
                break;
        }
    }

    /* Show form for adding user */
    private void showTextBox(String caption, String text) {
        groupName = new InputTextBox().create("group_name", 16);
        groupName.setCaption(JLocale.getString(caption));
        groupName.setString(text);
        groupName.setTextBoxListener(this);
        groupName.show();
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        if (!ok) {
            return;
        }
        if (null != contact) {
            if (renameContactTextbox == box) {
                protocol.renameContact(contact, renameContactTextbox.getString());
                ContactList.getInstance().activate();
                renameContactTextbox.setString(null);
            }
            return;
        }
        if (groupName != box) {
            return;
        }

        /* Return to contact list */
        String groupName_ = groupName.getString();
        boolean isExist = null != protocol.getGroup(groupName_);
        if (0 == groupName_.length()) {
            ContactList.getInstance().activate();
            return;
        }
        switch (action) {
            case ADD_GROUP:
                if (!isExist) {
                    protocol.addGroup(protocol.createGroup(groupName_));
                    ContactList.getInstance().activate();
                }
                break;

            case RENAME_GROUP:
                boolean isMyName = group.getName().equals(groupName_);
                if (isMyName) {
                    ContactList.getInstance().activate();

                } else if (!isExist) {
                    protocol.renameGroup(group, groupName_);
                    ContactList.getInstance().activate();
                }
                break;
        }
    }
    public void show() {
        new Select(getMenu()).show();
    }
}
