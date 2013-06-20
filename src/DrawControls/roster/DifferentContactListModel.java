package DrawControls.roster;

import jimm.comm.Util;
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
    public DifferentContactListModel(int count) {
        super(count);
    }

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
                    rebuildFlatItemsWG(p, items);
                } else {
                    rebuildFlatItemsWOG(p, items);
                }
            }
        }
    }

    private void rebuildFlatItemsWG(Protocol p, Vector drawItems) {
        Vector contacts;
        Contact c;
        GroupBranch groupBranch;
        int contactCounter;
        boolean all = !hideOffline;
        Vector groups = p.getSortedGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            groupBranch = getGroupNode((Group)groups.elementAt(groupIndex));
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

        groupBranch = getGroupNode((Group)p.getNotInListGroup());
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
    private void rebuildFlatItemsWOG(Protocol p, Vector drawItems) {
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
