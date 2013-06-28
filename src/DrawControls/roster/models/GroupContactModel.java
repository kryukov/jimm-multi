package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.Updater;
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
public class GroupContactModel extends ContactListModel {
    private Vector groups = new Vector();
    private GroupBranch notInListGroup;

    public GroupContactModel() {
        // Not In List Group
        notInListGroup = new GroupBranch(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
    }

    public void buildFlatItems(Vector items) {
        // prepare
        GroupBranch groupBranch;
        for (int i = 0; i < groups.size(); ++i) {
            groupBranch = ((GroupBranch)groups.elementAt(i));
            groupBranch.updateGroupData();
            groupBranch.sort();
        }
        Util.sort(groups);
        // build
        rebuildFlatItemsWG(items);
    }


    private void rebuildFlatItemsWG(Vector drawItems) {
        Vector groups = this.groups;
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            rebuildGroup((GroupBranch)groups.elementAt(groupIndex), !hideOffline, drawItems);
        }
        rebuildGroup(notInListGroup, false, drawItems);
    }

    public void updateGroupOrder(Updater.Update u) {
        GroupBranch groupBranch = getGroupNode(u.protocol, u.group);
        groupBranch.updateGroupData();
        groupBranch.sort();
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
            updateGroupContent(getGroupNode(protocol, group));
        } else {
            groups.removeElement(getGroupNode(protocol, group));
        }

    }
    public void addGroup(Protocol protocol, Group group) {
        GroupBranch gb = getGroupNode(protocol, group);
        if (null == gb) {
            gb = createGroup(group);
            groups.addElement(gb);
            gb.getContacts().addAll(group.getContacts(protocol));
        } else {
            updateGroupContent(gb);
        }
        gb.updateGroupData();
        gb.sort();
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




    public GroupBranch getGroupNode(Protocol protocol, Group group) {
        if (null == group) {
            return notInListGroup;
        }
        String name = group.getName();
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

    public void addProtocol(Protocol prot) {
        jimm.modules.DebugLog.println("proto " + prot.getGroupItems().size());
        super.addProtocol(prot);
        Vector inGroups = prot.getGroupItems();
        for (int i = 0; i < inGroups.size(); ++i) {
            addGroup(prot, (Group) inGroups.elementAt(i));
        }
        addGroup(prot, prot.getNotInListGroup());
    }
}
