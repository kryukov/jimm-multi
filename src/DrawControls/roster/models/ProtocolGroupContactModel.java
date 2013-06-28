package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.ProtocolBranch;
import DrawControls.roster.Updater;
import protocol.Contact;
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
            ProtocolBranch root = getProtocolNode(p);
            items.addElement(root);
            if (!root.isExpanded()) continue;
            // #sijapp cond.end #
            synchronized (p.getRosterLockObject()) {
                rebuildFlatItemsWG(root, items);
            }
        }
    }

    private void rebuildFlatItemsWG(ProtocolBranch p, Vector drawItems) {
        Vector groups = p.getGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            rebuildGroup((GroupBranch)groups.elementAt(groupIndex), !hideOffline, drawItems);
        }
        rebuildGroup(p.getNotInListGroup(), false, drawItems);
    }

    public void updateGroupOrder(Updater.Update u) {
        GroupBranch groupBranch = getGroupNode(u.protocol, u.group);
        groupBranch.updateGroupData();
        groupBranch.sort();
    }
    public void removeGroup(Protocol protocol, Group group) {
        getProtocolNode(protocol).removeGroup(group);
    }
    public void addGroup(Protocol protocol, Group group) {
        GroupBranch groupBranch = getGroupNode(protocol, group);
        if (null == groupBranch) {
            groupBranch = createGroup(group);
            getProtocolNode(protocol).getGroups().addElement(groupBranch);
        }
        Vector groupItems = groupBranch.getContacts();
        groupItems.removeAllElements();
        groupItems.addAll(group.getContacts(protocol));
        groupBranch.updateGroupData();
        groupBranch.sort();
    }

    public GroupBranch getGroupNode(Protocol protocol, Group group) {
        return getProtocolNode(protocol).getGroupNode(group);
    }
    // #sijapp cond.if modules_MULTI is "true" #
    public ProtocolBranch getProtocolNode(Protocol p) {
        return (ProtocolBranch) protos.get(p);
    }
    // #sijapp cond.end #

    public void addProtocol(Protocol prot) {
        super.addProtocol(prot);
        ProtocolBranch protocolBranch = new ProtocolBranch(prot);
        protos.put(prot, protocolBranch);
        Vector inGroups = prot.getGroupItems();
        for (int i = 0; i < inGroups.size(); ++i) {
            addGroup(prot, (Group) inGroups.elementAt(i));
        }
        addGroup(prot, prot.getNotInListGroup());
    }
}
