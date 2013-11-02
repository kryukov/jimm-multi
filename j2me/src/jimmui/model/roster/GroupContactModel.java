package jimmui.model.roster;

import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.items.GroupBranch;
import jimmui.view.roster.items.ProtocolBranch;
import jimmui.updater.RosterUpdater;
import jimm.comm.Util;
import jimm.util.JLocale;
import jimmui.view.roster.items.TreeNode;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 12.03.13 19:56
 *
 * @author vladimir
 */
public class GroupContactModel extends ContactListModel {
    private Vector<GroupBranch> groups = new Vector<GroupBranch>();
    private GroupBranch notInListGroup;

    public GroupContactModel() {
        // Not In List Group
        notInListGroup = new GroupBranch(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
    }

    public void buildFlatItems(Vector<TreeNode> items) {
        // build
        Vector groups = this.groups;
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            rebuildGroup((GroupBranch)groups.elementAt(groupIndex), !hideOffline, items);
        }
        rebuildGroup(notInListGroup, false, items);
    }

    public void updateOrder(RosterUpdater.Update u) {
        switch (u.event) {
            case RosterUpdater.Update.PROTOCOL_UPDATE:
                Vector groups = u.protocol.getGroupItems();
                for (int i = 0; i < groups.size(); ++i) {
                    u.group = (Group) groups.elementAt(i);
                    GroupBranch gb = getGroupNode(u);
                    gb.updateGroupData();
                    gb.sort();
                }
                u.group = null;
                break;
            case RosterUpdater.Update.UPDATE:
                GroupBranch groupBranch = getGroupNode(u);
                groupBranch.updateGroupData();
                groupBranch.sort();
                break;
        }
    }

    public void removeGroup(RosterUpdater.Update u) {
        boolean used = (null == u.group);
        if (!used) {
            for (int i = 0; i < getProtocolCount(); ++i) {
                Protocol p = getProtocol(i);
                if ((u.protocol != p) && (null != p.getGroup(u.group.getName()))) {
                    used = true;
                    break;
                }
            }
        }
        if (used) {
            updateGroupContent(getGroupNode(u));
        } else {
            groups.removeElement(getGroupNode(u));
        }

    }
    public void addGroup(RosterUpdater.Update u) {
        GroupBranch gb = getGroupNode(u);
        if (null == gb) {
            gb = createGroup(u.group);
            groups.addElement(gb);
            Util.addAll(gb.getContacts(), u.protocol.getContacts(u.group));
        } else {
            updateGroupContent(gb);
        }
        gb.updateGroupData();
        gb.sort();
        Util.sort(groups);
    }

    private void updateGroupContent(GroupBranch groupBranch) {
        boolean notInList = groupBranch == notInListGroup;
        Vector groupContacts = groupBranch.getContacts();
        groupContacts.removeAllElements();
        for (int i = 0; i < getProtocolCount(); ++i) {
            Protocol p = getProtocol(i);
            Group g = notInList ? null : p.getGroup(groupBranch.getName());
            if (notInList || (null != g)) {
                Util.addAll(groupContacts, p.getContacts(g));
            }
        }
    }




    public GroupBranch getGroupNode(RosterUpdater.Update u) {
        if (null == u.group) {
            return notInListGroup;
        }
        String name = u.group.getName();
        GroupBranch g;
        for (int i = 0; i < groups.size(); ++i) {
            g = (GroupBranch) groups.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }
    public ProtocolBranch getProtocolNode(RosterUpdater.Update u) {
        return null;
    }

    @Override
    public boolean hasGroups() {
        return true;
    }

    public void updateProtocol(Protocol protocol, Roster oldRoster) {
        RosterUpdater.Update u = new RosterUpdater.Update(protocol,  null, null, RosterUpdater.Update.GROUP_REMOVE);
        if (null != oldRoster) {
            for (int i = 0; i < oldRoster.groups.size(); ++i) {
                u.group = (Group) oldRoster.groups.elementAt(i);
                removeGroup(u);
            }
            u.group = null;
            removeGroup(u);
        }

        u.event = RosterUpdater.Update.GROUP_ADD;
        Vector inGroups = protocol.getGroupItems();
        for (int i = 0; i < inGroups.size(); ++i) {
            u.group = (Group) inGroups.elementAt(i);
            addGroup(u);
        }
        u.group = null;
        addGroup(u);
        Util.sort(groups);
    }
}
