package DrawControls.roster.alloy;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import jimm.comm.Util;
import jimm.util.JLocale;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 12.03.13 19:56
 *
 * @author vladimir
 */
public class AlloyContactListModel extends ContactListModel {
    private Vector groups = new Vector();
    private Vector contacts = new Vector();
    private Vector prevGroups = new Vector();
    private Vector prevContacts = new Vector();
    private GroupBranch notInListGroup;

    public AlloyContactListModel(int maxCount) {
        super(maxCount);
        // Not In List Group
        notInListGroup = new GroupBranch(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
    }

    public void buildFlatItems(Vector items) {
        // init
        prevContacts = contacts;
        prevGroups = groups;
        groups = new Vector();
        contacts = new Vector();
        notInListGroup.getContacts().removeAllElements();
        for (int i = 0; i < getProtocolCount(); ++i) {
            putProtocol(getProtocol(i));
        }
        // prepare
        if (useGroups) {
            GroupBranch groupBranch;
            for (int i = 0; i < groups.size(); ++i) {
                groupBranch = ((GroupBranch)groups.elementAt(i));
                groupBranch.updateGroupData();
                groupBranch.sort();
            }
            Util.sort(groups);
        } else {
            Util.sort(contacts);
        }
        // build
        if (useGroups) {
            rebuildFlatItemsWG(items);
        } else {
            rebuildFlatItemsWOG(items);
        }
    }


    private void rebuildFlatItemsWG(Vector drawItems) {
        Vector contacts;
        GroupBranch g;
        Contact c;
        int contactCounter;
        boolean all = !hideOffline;
        Vector groups = this.groups;
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (GroupBranch)groups.elementAt(groupIndex);
            contactCounter = 0;
            drawItems.addElement(g);
            contacts = g.getContacts();
            for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
                c = (Contact)contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (g.isExpanded()) {
                        drawItems.addElement(c);
                    }
                    contactCounter++;
                }
            }
            if (hideOffline && (0 == contactCounter)) {
                drawItems.removeElementAt(drawItems.size() - 1);
            }
        }

        g = notInListGroup;
        drawItems.addElement(g);
        contacts = g.getContacts();
        contactCounter = 0;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (g.isExpanded()) {
                    drawItems.addElement(c);
                }
                contactCounter++;
            }
        }
        if (0 == contactCounter) {
            drawItems.removeElementAt(drawItems.size() - 1);
        }
    }
    private void rebuildFlatItemsWOG(Vector drawItems) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = this.contacts;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    public void updateGroup(Group g) {
        if (useGroups) {
            GroupBranch group = getGroupNode(g);
            if (null == group) {
                group = createGroup(g);
            }
            group.updateGroupData();
            group.sort();
        } else {
            Util.sort(getProtocol(g).getSortedContacts());
        }
    }





    private void putProtocol(Protocol p) {
        Vector gs = p.getGroupItems();
        for (int i = 0; i < gs.size(); ++i) {
            putGroup((Group) gs.elementAt(i));
        }
        Vector cs = p.getContactItems();
        for (int i = 0; i < cs.size(); ++i) {
            putContact(p, (Contact) cs.elementAt(i));
        }
    }

    private void putGroup(Group g) {
        GroupBranch group = getGroupNode(g);
        if (null == group) {
            group = getGroupNode(prevGroups, g.getName());
            if (null != group) {
                groups.addElement(group);
                group.getContacts().removeAllElements();
                return;
            }
            group = createGroup(g);
        }
    }
    private GroupBranch createGroup(Group g) {
        GroupBranch group = new GroupBranch(g.getName());
        group.setMode(g.getMode());
        groups.addElement(group);
        return group;
    }

    private GroupBranch getGroup(String name) {
        GroupBranch g;
        for (int i = 0; i < groups.size(); ++i) {
            g = (GroupBranch) groups.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    private void putContact(Protocol p, Contact contact) {
        if (-1 == Util.getIndex(contacts, contact)) {
            contacts.addElement(contact);
            if (Group.NOT_IN_GROUP != contact.getGroupId()) {
                getGroupNode(p.getGroupById(contact.getGroupId())).getContacts().addElement(contact);
            } else {
                notInListGroup.getContacts().addElement(contact);
            }
        }
    }

    private GroupBranch getGroupNode(Vector groups, String name) {
        GroupBranch g;
        for (int i = 0; i < groups.size(); ++i) {
            g = (GroupBranch) groups.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }
    public GroupBranch getGroupNode(Group group) {
        return getGroup(group.getName());
    }
}
