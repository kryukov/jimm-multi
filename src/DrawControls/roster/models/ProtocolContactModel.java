package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.ProtocolBranch;
import DrawControls.roster.Updater;
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

    public void updateOrder(Updater.Update u) {
        getProtocolNode(u).sort();
    }
    public void removeGroup(Updater.Update u) {
    }
    public void addGroup(Updater.Update u) {
    }
    public void addToGroup(Updater.Update update) {
        ProtocolBranch pb = getProtocolNode(update);
        pb.getSortedContacts().addElement(update.contact);
    }

    public void removeFromGroup(Updater.Update update) {
        ProtocolBranch pb = getProtocolNode(update);
        pb.getSortedContacts().removeElement(update.contact);
    }

    public GroupBranch getGroupNode(Updater.Update u) {
        return null;
    }

    public ProtocolBranch getProtocolNode(Updater.Update u) {
        return (ProtocolBranch) protos.get(u.protocol);
    }

    @Override
    public boolean hasGroups() {
        return false;
    }

    protected void addProtocol(Protocol prot) {
        ProtocolBranch protocolBranch = new ProtocolBranch(prot);
        protos.put(prot, protocolBranch);
        addAll(protocolBranch.getSortedContacts(), prot.getContactItems());
        protocolBranch.sort();
    }
}
