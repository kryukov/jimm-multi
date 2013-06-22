package DrawControls.roster;

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
public class DifferentContactListModel extends ContactListModel {
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
                if (useGroups) {
                    rebuildFlatItemsWG(root, items);
                } else {
                    rebuildFlatItemsWOG(root, items);
                }
            }
        }
    }

    private void rebuildFlatItemsWG(ProtocolBranch p, Vector drawItems) {
        Vector contacts;
        Contact c;
        GroupBranch groupBranch;
        int contactCounter;
        boolean all = !hideOffline;
        p.sort();
        Vector groups = p.getGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            groupBranch = (GroupBranch) groups.elementAt(groupIndex);
            contactCounter = 0;
            drawItems.addElement(groupBranch);
            contacts = groupBranch.getContacts();
            for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
                c = (Contact)contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (groupBranch.isExpanded()) {
                        drawItems.addElement(c);
                    }
                    contactCounter++;
                }
            }
            if (hideOffline && (0 == contactCounter)) {
                drawItems.removeElementAt(drawItems.size() - 1);
            }
        }

        groupBranch = p.getNotInListGroup();
        drawItems.addElement(groupBranch);
        contacts = groupBranch.getContacts();
        contactCounter = 0;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (groupBranch.isExpanded()) {
                    drawItems.addElement(c);
                }
                contactCounter++;
            }
        }
        if (0 == contactCounter) {
            drawItems.removeElementAt(drawItems.size() - 1);
        }
    }
    private void rebuildFlatItemsWOG(ProtocolBranch p, Vector drawItems) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = p.getSortedContacts();
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    public void updateGroupOrder(Protocol protocol, Group group) {
        if (useGroups) {
            GroupBranch groupBranch = getGroupNode(group);
            if (null == groupBranch) return;
            groupBranch.updateGroupData();
            groupBranch.sort();
        } else {
            getProtocolNode(protocol).sort();
        }
    }
    public void updateGroup(Protocol protocol, Group group) {
        addGroup(protocol, group);
    }
    public void removeGroup(Protocol protocol, Group group) {
        getProtocolNode(protocol).removeGroup(group);
    }
    public void addGroup(Protocol protocol, Group group) {
        Vector allItems = protocol.getContactItems();
        GroupBranch groupBranch = getGroupNode(group);
        if (null == groupBranch) return;
        Vector groupItems = groupBranch.getContacts();
        groupItems.removeAllElements();
        int size = allItems.size();
        int groupId = group.getId();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact)allItems.elementAt(i);
            if (item.getGroupId() == groupId) {
                groupItems.addElement(item);
            }
        }
        groupBranch.updateGroupData();
        groupBranch.sort();
    }

    public GroupBranch getGroupNode(Group group) {
        return getProtocolNode(getProtocol(group)).getGroupNode(group);
    }
    // #sijapp cond.if modules_MULTI is "true" #
    public ProtocolBranch getProtocolNode(Protocol p) {
        ProtocolBranch protocolBranch = (ProtocolBranch) protos.get(p);
        if (null == protocolBranch) {
            protocolBranch = new ProtocolBranch(p);
            protos.put(p, protocolBranch);
        }
        return protocolBranch;
    }
    // #sijapp cond.end #
    private Hashtable protos = new Hashtable();
}
