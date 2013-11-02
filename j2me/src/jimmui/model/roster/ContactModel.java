package jimmui.model.roster;

import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.items.GroupBranch;
import jimmui.view.roster.items.ProtocolBranch;
import jimmui.updater.RosterUpdater;
import jimm.comm.Util;
import jimmui.view.roster.items.TreeNode;
import protocol.Contact;
import protocol.Protocol;
import protocol.Roster;

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

    public void buildFlatItems(Vector<TreeNode> items) {
        // build
        rebuildContacts(contacts, items);
    }

    public void updateOrder(RosterUpdater.Update u) {
        Util.sort(contacts);
    }

    public void removeGroup(RosterUpdater.Update u) {
    }
    public void addGroup(RosterUpdater.Update u) {
        Util.addNew(this.contacts, u.protocol.getContacts(u.group));
    }
    public void addToGroup(RosterUpdater.Update update) {
        contacts.addElement(update.contact);
    }

    public void removeFromGroup(RosterUpdater.Update update) {
        contacts.removeElement(update.contact);
    }


    public GroupBranch getGroupNode(RosterUpdater.Update u) {
        return null;
    }

    public ProtocolBranch getProtocolNode(RosterUpdater.Update u) {
        return null;
    }

    public boolean hasGroups() {
        return false;
    }

    public void updateProtocol(Protocol protocol, Roster oldRoster) {
        contacts.removeAllElements();
        for (int i = 0; i < getProtocolCount(); ++i) {
            Util.addAll(contacts, getProtocol(i).getContactItems());
        }
        Util.sort(contacts);
    }
}
