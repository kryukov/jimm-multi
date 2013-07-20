package jimmui.model.roster;

import jimm.comm.Util;
import jimmui.updater.RosterUpdater;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.GroupBranch;
import jimmui.view.roster.ProtocolBranch;
import protocol.Protocol;
import protocol.Roster;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 20.06.13 21:00
 *
 * @author vladimir
 */
public class ProtocolContactModel extends ContactListModel {
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
                rebuildContacts(root.getSortedContacts(), items);
            }
        }
    }

    public void updateOrder(RosterUpdater.Update u) {
        getProtocolNode(u).sort();
    }
    public void removeGroup(RosterUpdater.Update u) {
    }
    public void addGroup(RosterUpdater.Update u) {
        Vector contacts = u.protocol.getContacts(u.group);
        Util.removeAll(getProtocolNode(u).getSortedContacts(), contacts);
        Util.addAll(getProtocolNode(u).getSortedContacts(), contacts);
    }

    public void addToGroup(RosterUpdater.Update update) {
        ProtocolBranch pb = getProtocolNode(update);
        pb.getSortedContacts().addElement(update.contact);
    }

    public void removeFromGroup(RosterUpdater.Update update) {
        ProtocolBranch pb = getProtocolNode(update);
        pb.getSortedContacts().removeElement(update.contact);
    }

    public GroupBranch getGroupNode(RosterUpdater.Update u) {
        return null;
    }

    public ProtocolBranch getProtocolNode(RosterUpdater.Update u) {
        return (ProtocolBranch) protos.get(u.protocol);
    }

    @Override
    public boolean hasGroups() {
        return false;
    }

    public void updateProtocol(Protocol protocol, Roster oldRoster) {
        ProtocolBranch protocolBranch = new ProtocolBranch(protocol);
        protos.put(protocol, protocolBranch);
        Util.addAll(protocolBranch.getSortedContacts(), protocol.getContactItems());
        protocolBranch.sort();
    }
}
