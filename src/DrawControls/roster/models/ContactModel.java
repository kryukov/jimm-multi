package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.Updater;
import jimm.comm.Util;
import protocol.Contact;
import protocol.Contacts;
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
public class ContactModel extends ContactListModel {
    private Contacts contacts = new Contacts();

    public ContactModel() {
    }

    public void buildFlatItems(Vector items) {
        // prepare
        Util.sort(contacts);
        // build
        rebuildContacts(contacts, items);
    }

    public void updateGroupOrder(Updater.Update u) {
        Util.sort(contacts);
    }

    public void removeGroup(Protocol protocol, Group group) {
    }
    public void addGroup(Protocol protocol, Group group) {
    }
    public void addToGroup(Protocol protocol, Group group, Contact contact) {
        contacts.addElement(contact);
    }

    public void updateGroupData(Protocol protocol, Group group) {
    }

    public void removeFromGroup(Protocol protocol, Group group, Contact c) {
        contacts.removeElement(c);
    }


    public GroupBranch getGroupNode(Protocol protocol, Group group) {
        return null;
    }

    public void addProtocol(Protocol prot) {
        super.addProtocol(prot);
        Vector inContacts = prot.getContactItems();
        contacts.addAll(inContacts);
        Util.sort(contacts);
    }
}
