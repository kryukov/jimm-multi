package DrawControls.roster.models;

import DrawControls.roster.ContactListModel;
import DrawControls.roster.GroupBranch;
import DrawControls.roster.ProtocolBranch;
import DrawControls.roster.Updater;
import jimm.comm.Util;
import protocol.Contacts;
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
        // build
        rebuildContacts(contacts, items);
    }

    public void updateOrder(Updater.Update u) {
        Util.sort(contacts);
    }

    public void removeGroup(Updater.Update u) {
    }
    public void addGroup(Updater.Update u) {
    }
    public void addToGroup(Updater.Update update) {
        contacts.addElement(update.contact);
    }

    public void removeFromGroup(Updater.Update update) {
        contacts.removeElement(update.contact);
    }


    public GroupBranch getGroupNode(Updater.Update u) {
        return null;
    }

    public ProtocolBranch getProtocolNode(Updater.Update u) {
        return null;
    }

    protected void addProtocol(Protocol prot) {
        Vector inContacts = prot.getContactItems();
        contacts.addAll(inContacts);
        Util.sort(contacts);
    }
}
