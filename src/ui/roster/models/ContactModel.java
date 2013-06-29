package ui.roster.models;

import ui.roster.ContactListModel;
import ui.roster.GroupBranch;
import ui.roster.ProtocolBranch;
import ui.roster.Updater;
import jimm.comm.Util;
import protocol.Contact;
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
    private Vector<Contact> contacts = new Vector<Contact>();

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

    public boolean hasGroups() {
        return false;
    }

    protected void addProtocol(Protocol prot) {
        Vector inContacts = prot.getContactItems();
        addAll(contacts, inContacts);
        Util.sort(contacts);
    }
}
