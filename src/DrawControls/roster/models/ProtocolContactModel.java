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
public class ProtocolContactModel extends ContactListModel {
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
                rebuildContacts(root.getSortedContacts(), items);
            }
        }
    }

    public void updateGroupOrder(Updater.Update u) {
        getProtocolNode(u.protocol).sort();
    }
    public void removeGroup(Protocol protocol, Group group) {
    }
    public void addGroup(Protocol protocol, Group group) {
    }
    public void addToGroup(Protocol protocol, Group group, Contact contact) {
        ProtocolBranch pb = getProtocolNode(protocol);
        pb.getSortedContacts().addElement(contact);
        pb.sort();
    }
    public void updateGroupData(Protocol protocol, Group group) {
    }

    public void removeFromGroup(Protocol protocol, Group group, Contact c) {
        ProtocolBranch pb = getProtocolNode(protocol);
        pb.getSortedContacts().removeElement(c);
    }

    public GroupBranch getGroupNode(Protocol protocol, Group group) {
        return null;
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
        Vector inContacts = prot.getContactItems();
        Vector outContacts = protocolBranch.getSortedContacts();
        outContacts.addAll(inContacts);
        protocolBranch.sort();
    }
}
