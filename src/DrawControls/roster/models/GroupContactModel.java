package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.ProtocolBranch;
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
        // build
        Vector groups = this.groups;
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            rebuildGroup((GroupBranch)groups.elementAt(groupIndex), !hideOffline, items);
        }
        rebuildGroup(notInListGroup, false, items);
    }

    public void updateOrder(Updater.Update u) {
        switch (u.event) {
            case Updater.Update.PROTOCOL_UPDATE:
                Vector groups = u.protocol.getGroupItems();
                for (int i = 0; i < groups.size(); ++i) {
                    u.group = (Group) groups.elementAt(i);
                    GroupBranch gb = getGroupNode(u);
                    gb.updateGroupData();
                    gb.sort();
                }
                u.group = null;
                break;
            case Updater.Update.UPDATE:
                GroupBranch groupBranch = getGroupNode(u);
                groupBranch.updateGroupData();
                groupBranch.sort();
                break;
        }
    }

    public void removeGroup(Updater.Update u) {
        boolean used = false;
        for (int i = 0; i < getProtocolCount(); ++i) {
            Protocol p = getProtocol(i);
            if ((u.protocol != p) && (null != p.getGroup(u.group.getName()))) {
                used = true;
                break;
            }
        }
        if (used) {
            updateGroupContent(getGroupNode(u));
        } else {
            groups.removeElement(getGroupNode(u));
        }

    }
    public void addGroup(Updater.Update u) {
        GroupBranch gb = getGroupNode(u);
        if (null == gb) {
            gb = createGroup(u.group);
            groups.addElement(gb);
            gb.getContacts().addAll(u.group.getContacts(u.protocol));
            Util.sort(groups);
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
            Group g = notInList ? null : p.getGroup(groupBranch.getName());
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




    public GroupBranch getGroupNode(Updater.Update u) {
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
        if (name.equals(notInListGroup.getName())) {
            return notInListGroup;
        }
        return null;
    }
    public ProtocolBranch getProtocolNode(Updater.Update u) {
        return null;
    }

    protected void addProtocol(Protocol prot) {
        Vector inGroups = prot.getGroupItems();
        Updater.Update u = new Updater.Update(prot,  null, null, Updater.Update.ADD);
        for (int i = 0; i < inGroups.size(); ++i) {
            u.group = (Group) inGroups.elementAt(i);
            addGroup(u);
        }
        u.group = null;
        addGroup(u);
        Util.sort(groups);
    }
}
