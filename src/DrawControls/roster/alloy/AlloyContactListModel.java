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
    private GroupBranch notInListGroup;

    public AlloyContactListModel() {
        // Not In List Group
        notInListGroup = new GroupBranch(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
    }

    public void buildFlatItems(Vector items) {
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

    public void updateGroupOrder(Protocol protocol, Group g) {
        if (useGroups) {
            GroupBranch group = getGroupNode(g);
            if (null == group) {
                group = createGroup(g);
            }
            group.updateGroupData();
            group.sort();
        } else {
            Util.sort(contacts);
        }
    }

    public void removeGroup(Protocol protocol, Group group) {
        boolean used = false;
        for (int i = 0; i < getProtocolCount(); ++i) {
            Protocol p = getProtocol(i);
            if (null != p.getGroup(group.getName())) {
                used = true;
                break;
            }
        }
        if (used) {
            updateGroupContent(getGroupNode(group));
        } else {
            groups.removeElement(getGroupNode(group));
        }

    }
    public void addGroup(Protocol protocol, Group group) {
        updateGroupContent(getGroupNode(group));
    }
    public void updateGroup(Protocol protocol, Group group) {
        GroupBranch groupBranch = getGroupNode(group);
        updateGroupContent(groupBranch);
        groupBranch.updateGroupData();
        groupBranch.sort();
    }
    private void updateGroupContent(GroupBranch groupBranch) {
        boolean notInList = groupBranch == notInListGroup;
        groupBranch.getContacts().removeAllElements();
        for (int i = 0; i < getProtocolCount(); ++i) {
            Protocol p = getProtocol(i);
            Group g = notInList ? p.getNotInListGroup() : p.getGroup(groupBranch.getName());
            if (null == g) continue;
            int id = g.getId();
            Vector contacts = p.getContactItems();
            for (int j = 0; j < contacts.size(); ++j) {
                Contact c = (Contact) contacts.elementAt(j);
                if (id == c.getGroupId()) {
                    groupBranch.getContacts().addElement(c);
                }
            }
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
        if (name.equals(notInListGroup.getName())) {
            return notInListGroup;
        }
        return null;
    }

    public GroupBranch getGroupNode(Group group) {
        GroupBranch groupBranch = getGroup(group.getName());
        if (null == groupBranch) {
            groupBranch = createGroup(group);
        }
        return groupBranch;
    }
}
