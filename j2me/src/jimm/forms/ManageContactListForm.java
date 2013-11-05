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

import jimm.Jimm;
import jimm.comm.StringUtils;
import jimm.search.*;
import jimmui.view.*;
import jimmui.view.form.ControlStateListener;
import jimmui.view.form.Form;
import jimmui.view.form.FormListener;
import jimmui.view.menu.*;
import jimm.util.*;
import protocol.*;
import protocol.mrim.Mrim;
import protocol.xmpp.Xmpp;

/**
 *
 * @author vladimir
 */
public final class ManageContactListForm implements SelectListener, FormListener, ControlStateListener {
    private static final int ADD_CONTACT = 1;
    private static final int SEARCH_CONTACT = 2;
    private static final int ADD_GROUP    = 3;
    private static final int RENAME_GROUP = 4;
    private static final int DEL_GROUP    = 5;
    private static final int RENAME_CONTACT = 6;
    private static final int MOVE_CONTACT = 7;

    private static final int ACCOUNT  = 9;
    private static final int GROUP    = 10;
    private static final int GROUP_NEW_NAME = 11;
    private static final int CONTACT_NEW_NAME = 12;
    private static final int CONTACT_USER_ID = 13;
    private static final int REQ_AUTH = 14;
    private static final int PROFILE_LINK = 15;

    private Protocol protocol;
    private Group group;
    private Contact contact;
    private int action;
    // #sijapp cond.if protocols_JABBER is "true" #
    private String xmppGate = null;
    // #sijapp cond.end #

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
        action = RENAME_CONTACT;
        Form form = UIBuilder.createForm("rename", "ok", "back", this);
        addProtocol(form, false);
        form.addString("contact", contact.getName());
        form.addTextField(CONTACT_NEW_NAME, "new_name", contact.getName(), 64);
        form.show();
    }
    public void showContactAdd() {
        action = ADD_CONTACT;
        Form form = UIBuilder.createForm("add", "ok", "back", this);
        addProtocol(form, false);
        form.addTextField(CONTACT_USER_ID, protocol.getUserIdName(),
                null == contact ? "" : contact.getUserId(), 64);
        addGroupSelector(form, getGroups(Group.MODE_NEW_CONTACTS));
        boolean request_auth = true;
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            request_auth = false;
        }
        // #sijapp cond.end #
        if (request_auth) {
            form.addCheckBox(REQ_AUTH, "requauth", true);
        }
        form.show();
    }
    public void showContactMove() {
        action = MOVE_CONTACT;
        Form form = UIBuilder.createForm("move", "ok", "back", this);
        addProtocol(form, false);
        group = protocol.getGroupById(contact.getGroupId());
        form.addString("contact", contact.getName());
        addGroupSelector(form, getGroups(Group.MODE_NEW_CONTACTS));
        form.addLink(PROFILE_LINK, JLocale.getString("info"));
        form.setControlStateListener(this);
        form.show();
    }

    public MenuModel getMenu() {
        MenuModel manageCL = new MenuModel();
        boolean canAdd = !protocol.getGroupItems().isEmpty()
                && ((null == group) || group.hasMode(Group.MODE_NEW_CONTACTS));
        if (canAdd) {
            manageCL.addItem("add_user", ADD_CONTACT);
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!(protocol instanceof Xmpp)) {
                manageCL.addItem("search_user", SEARCH_CONTACT);
            }
            // #sijapp cond.else #
            manageCL.addItem("search_user", SEARCH_CONTACT);
            // #sijapp cond.end #
        }
        manageCL.addItem("add_group", ADD_GROUP);
        if (null != group) {
            if (group.hasMode(Group.MODE_EDITABLE)) {
                manageCL.addItem("rename_group", RENAME_GROUP);
            }
            if (group.isEmpty(protocol) && group.hasMode(Group.MODE_REMOVABLE)) {
                manageCL.addItem("del_group", DEL_GROUP);
            }
        } else {
            manageCL.addItem("rename_group", RENAME_GROUP);
            manageCL.addItem("del_group", DEL_GROUP);
        }
        manageCL.setActionListener(this);
        return manageCL;
    }

    public void select(Select select, MenuModel model, int cmd) {
        action = cmd;
        switch (cmd) {
            case ADD_CONTACT: /* Add user */
                showContactAdd();
                break;

            case SEARCH_CONTACT: /* Search for User */
                Search searchUser = protocol.getSearchForm();
                searchUser.putToGroup(group);
                searchUser.show();
                break;

            case ADD_GROUP: /* Add group */ {
                Form form = UIBuilder.createForm("add_group", "ok", "back", this);
                addProtocol(form, true);
                form.addTextField(GROUP_NEW_NAME, "new_group_name", "", 64);
                form.show();
                break;
            }

            case RENAME_GROUP: /* Rename group */ {
                Form form = UIBuilder.createForm("rename_group", "ok", "back", this);
                addProtocol(form, true);
                addGroupSelector(form, getGroups(Group.MODE_EDITABLE));
                form.addTextField(GROUP_NEW_NAME, "new_group_name", "", 64);
                form.show();
                break;
            }

            case DEL_GROUP: /* Delete group */ {
                Form form = UIBuilder.createForm("del_group", "delete", "back", this);
                addProtocol(form, true);
                addGroupSelector(form, getGroups(Group.MODE_REMOVABLE));
                form.show();
                break;
            }
        }
    }

    private Vector getGroups(byte mode) {
        Vector<Group> all = protocol.getGroupItems();
        Vector<Group> groups = new Vector<Group>();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group)all.elementAt(i);
            if (g.hasMode(mode)) {
                if ((Group.MODE_REMOVABLE == mode) && !g.isEmpty(protocol)) continue;
                groups.addElement(g);
            }
        }
        return groups;
    }

    public void show() {
        UIBuilder.createMenu(getMenu()).show();
    }

    public void formAction(Form form, boolean apply) {
        if (!apply) {
            Jimm.getJimm().getCL().activate();
            return;
        }
        switch (action) {
            case ADD_CONTACT: {
                String userid = form.getTextFieldValue(CONTACT_USER_ID).trim();
                userid = StringUtils.toLowerCase(userid);
                if (StringUtils.isEmpty(userid)) {
                    return;
                }
                // #sijapp cond.if protocols_JABBER is "true" #
                if ((null != xmppGate) && !userid.endsWith(xmppGate)) {
                    userid = userid.replace('@', '%') + '@' + xmppGate;
                }
                // #sijapp cond.end #

                Contact contact = protocol.createTempContact(userid);
                if (null != contact) {
                    if (contact.isTemp()) {
                        String g = null;
                        if (!contact.isSingleUserContact()) {
                            g = contact.getDefaultGroupName();
                        }
                        if (null == g) {
                            g = form.getSelectorString(GROUP);
                        }
                        contact.setGroup(protocol.getGroup(g));
                        protocol.addContact(contact);
                        if (form.getCheckBoxValue(REQ_AUTH) && contact.isSingleUserContact()) {
                            protocol.requestAuth(contact);
                        }
                    }
                    Jimm.getJimm().getCL().activate(contact);
                }
                break;
            }

            case RENAME_CONTACT: {
                String newName = form.getTextFieldValue(CONTACT_NEW_NAME);
                protocol.renameContact(contact, newName);
                Jimm.getJimm().getCL().activate();
                return;
            }
            case MOVE_CONTACT: {
                String groupName = form.getSelectorString(GROUP);
                Group group = protocol.getGroup(groupName);
                if (group != protocol.getGroupById(contact.getGroupId())) {
                    protocol.moveContactTo(contact, group);
                }
                Jimm.getJimm().getCL().activate();
                return;
            }
        }
        if (!form.hasControl(GROUP)) {
            Jimm.getJimm().getCL().activate();
            return;
        }
        switch (action) {
            case ADD_GROUP: {
                String groupName = form.getTextFieldValue(GROUP_NEW_NAME);
                boolean isExist = null != protocol.getGroup(groupName);
                if (0 == groupName.length()) {
                    Jimm.getJimm().getCL().activate();
                    return;
                }
                if (isExist) {
                    form.addString(JLocale.getString("group_already_exist"));
                } else {
                    protocol.addGroup(protocol.createGroup(groupName));
                    Jimm.getJimm().getCL().activate();
                }
                break;
            }

            case RENAME_GROUP: {
                Vector groups = getGroups(Group.MODE_EDITABLE);
                Group g = (Group) groups.elementAt(form.getSelectorValue(GROUP));
                String oldGroupName = form.getSelectorString(GROUP);
                String newGroupName = form.getTextFieldValue(GROUP_NEW_NAME);
                boolean isExist = null != protocol.getGroup(newGroupName);
                boolean isMyName = oldGroupName.equals(newGroupName);
                if (!oldGroupName.equals(g.getName())) {
                    // invalid group
                    Jimm.getJimm().getCL().activate();

                } else if (StringUtils.isEmpty(newGroupName)) {
                    Jimm.getJimm().getCL().activate();

                } else if (isMyName) {
                    Jimm.getJimm().getCL().activate();

                } else if (isExist) {
                    form.addString(JLocale.getString("group_already_exist"));

                } else {
                    protocol.renameGroup(g, newGroupName);
                    Jimm.getJimm().getCL().activate();
                }
                break;
            }

            case DEL_GROUP: {
                Vector groups = getGroups(Group.MODE_REMOVABLE);
                Group g = (Group) groups.elementAt(form.getSelectorValue(GROUP));
                String oldGroupName = form.getSelectorString(GROUP);
                if (oldGroupName.equals(g.getName())) {
                    protocol.removeGroup(g);
                }
                Jimm.getJimm().getCL().activate();
                break;
            }
        }
    }

    // #sijapp cond.if protocols_JABBER is "true" #
    public void setXmppGate(String gate) {
        xmppGate = gate;
    }
    // #sijapp cond.end #


    private void addProtocol(Form form, boolean selectable) {
        if (selectable) {
            Vector protocols = getAccounts();
            if (!protocols.isEmpty()) {
                String[] list = new String[protocols.size()];
                int def = 0;
                for (int i = 0; i < protocols.size(); ++i) {
                    Protocol p = (Protocol) protocols.elementAt(i);
                    list[i] = p.getUserId();
                    if (p == protocol) {
                        def = i;
                    }
                }
                form.addSelector(ACCOUNT, "protocol", list, def);

            } else {
                form.addString(JLocale.getString("no_protocols_available"));
            }
        } else {
            form.addString("protocol", protocol.getUserId());
        }
    }
    private Vector getAccounts() {
        return Jimm.getJimm().jimmModel.protocols;
    }
    private void addGroupSelector(Form form, Vector groups) {
        if (!groups.isEmpty()) {
            String[] list = new String[groups.size()];
            int def = 0;
            for (int i = 0; i < groups.size(); ++i) {
                Group g = (Group) groups.elementAt(i);
                list[i] = g.getName();
                if (g == group) {
                    def = i;
                }
            }
            form.addSelector(GROUP, "group", list, def);

        } else {
            form.addString(JLocale.getString("no_groups_available"));
        }
    }

    @Override
    public void controlStateChanged(Form form, int id) {
        if (PROFILE_LINK == id) {
            String userId = form.getTextFieldValue(CONTACT_USER_ID);
            if (StringUtils.isEmpty(userId)) {
                return;
            }
            // #sijapp cond.if protocols_JABBER is "true" #
            if ((null != xmppGate) && !userId.endsWith(xmppGate)) {
                userId = userId.replace('@', '%') + '@' + xmppGate;
            }
            // #sijapp cond.end #
            Contact contact = protocol.createTempContact(userId);
            if (null != contact) {
                protocol.showUserInfo(contact);
            }

        } else if (ACCOUNT == id) {
            protocol = (Protocol) getAccounts().elementAt(form.getSelectorValue(ACCOUNT));
            // todo update other components
        }
    }
}
