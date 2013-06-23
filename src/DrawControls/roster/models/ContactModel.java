package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
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
        rebuildFlatItemsWOG(items);
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
        Util.sort(contacts);
    }

    public void removeGroup(Protocol protocol, Group group) {
    }
    public void addGroup(Protocol protocol, Group group) {
    }
    public void updateGroup(Protocol protocol, Group group) {
    }
    public void addToGroup(Group group, Contact contact) {
        contacts.addElement(contact);
    }

    public void updateGroupData(Group group) {
    }

    public void removeFromGroup(Group group, Contact c) {
        contacts.removeElement(c);
    }


    public GroupBranch getGroupNode(Group group) {
        return null;
    }

    public void addProtocol(Protocol prot) {
        super.addProtocol(prot);
        Vector inContacts = prot.getContactItems();
        contacts.addAll(inContacts);
        Util.sort(contacts);
    }
}
