package ui.roster.models;

import jimm.comm.Util;
import protocol.Contact;
import ui.roster.ContactListModel;
import ui.roster.GroupBranch;
import ui.roster.ProtocolBranch;
import ui.roster.Updater;
import protocol.Group;
import protocol.Protocol;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 20.06.13 21:00
 *
 * @author vladimir
 */
public class ProtocolGroupContactModel extends ContactListModel {
    private Hashtable protos = new Hashtable();

    public void buildFlatItems(Vector items) {
        final int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            // #sijapp cond.if modules_MULTI is "true" #
            ProtocolBranch root = (ProtocolBranch) protos.get(p);
            items.addElement(root);
            if (!root.isExpanded()) continue;
            // #sijapp cond.end #
            synchronized (p.getRosterLockObject()) {
                Vector groups = root.getGroups();
                for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                    rebuildGroup((GroupBranch)groups.elementAt(groupIndex), !hideOffline, items);
                }
                rebuildGroup(root.getNotInListGroup(), false, items);
            }
        }
    }

    public void updateOrder(Updater.Update u) {
        switch (u.event) {
            case Updater.Update.PROTOCOL_UPDATE:
                Vector groups = getProtocolNode(u).getGroups();
                for (int i = 0; i < groups.size(); ++i) {
                    GroupBranch gb = (GroupBranch) groups.elementAt(i);
                    gb.updateGroupData();
                    gb.sort();
                }
                break;
            case Updater.Update.UPDATE:
                GroupBranch groupBranch = getGroupNode(u);
                groupBranch.updateGroupData();
                groupBranch.sort();
                break;
        }
    }
    public void removeGroup(Updater.Update u) {
        getProtocolNode(u).removeGroup(u.group);
    }
    public void addGroup(Updater.Update u) {
        GroupBranch groupBranch = getGroupNode(u);
        if (null == groupBranch) {
            groupBranch = createGroup(u.group);
            getProtocolNode(u).getGroups().addElement(groupBranch);
        }
        Vector groupItems = groupBranch.getContacts();
        groupItems.removeAllElements();
        Util.addAll(groupItems, u.protocol.getContacts(u.group));
        groupBranch.updateGroupData();
        groupBranch.sort();
    }

    public GroupBranch getGroupNode(Updater.Update u) {
        return getProtocolNode(u).getGroupNode(u.group);
    }

    public ProtocolBranch getProtocolNode(Updater.Update u) {
        return (ProtocolBranch) protos.get(u.protocol);
    }

    @Override
    public boolean hasGroups() {
        return true;
    }

    public void updateProtocol(Protocol protocol, Vector<Group> oldGroups, Vector<Contact> oldContacts) {
        ProtocolBranch protocolBranch = new ProtocolBranch(protocol);
        protos.put(protocol, protocolBranch);

        Updater.Update u = new Updater.Update(protocol,  null, null, Updater.Update.GROUP_REMOVE);
        for (int i = 0; i < oldGroups.size(); ++i) {
            u.group = (Group) oldGroups.elementAt(i);
            removeGroup(u);
        }
        u.group = null;
        removeGroup(u);

        u.event = Updater.Update.GROUP_ADD;
        Vector inGroups = protocol.getGroupItems();
        for (int i = 0; i < inGroups.size(); ++i) {
            u.group = (Group) inGroups.elementAt(i);
            addGroup(u);
        }
        u.group = null;
        addGroup(u);
    }
}
