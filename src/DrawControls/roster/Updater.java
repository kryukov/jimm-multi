package DrawControls.roster;

import jimm.cl.ContactList;
import jimm.comm.Util;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.06.13 19:38
 *
 * @author vladimir
 */
public class Updater {
    private Vector updateQueue = new Vector();

    private ContactListModel model;
    public void setModel(ContactListModel model) {
        this.model = model;
    }
    public void addGroup(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            model.addGroup(protocol, group);
        }
    }

    public void updateProtocol(Protocol protocol) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                Vector groups = protocol.getGroupItems();
                for (int i = 0; i < groups.size(); ++i) {
                    addGroup(protocol, (Group) groups.elementAt(i));
                }
                addGroup(protocol, protocol.getNotInListGroup());
            }
            update();
        }
    }

    public void removeGroup(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                model.removeGroup(protocol, group);
            }
            update();
        }
    }

    public void update() {
        ContactList.getInstance().getManager().update();
    }
    public void update(Protocol protocol) {
        if (model.hasProtocol(protocol)) {
            update();
        }
    }

    public void update(Group group) {
        update();
    }

    private void update(Contact contact) {
        update();
    }

    public void typing(Protocol protocol, Contact item) {
        if (model.hasProtocol(protocol)) {
            ContactList.getInstance().getManager().invalidate();
        }
    }

    public void setOffline(Protocol protocol) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                Vector groups = protocol.getGroupItems();
                for (int i = groups.size() - 1; i >= 0; --i) {
                    model.updateGroupData(protocol, (Group) groups.elementAt(i));
                }
            }
            update();
        }
    }

    public void removeFromGroup(Protocol protocol, Group g, Contact c) {
        if (model.hasProtocol(protocol)) {
            model.removeFromGroup(protocol, g, c);
            update(c);
        }
    }

    public void updateContact(Protocol protocol, Group group, Contact contact) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                putIntoQueue(new Update(protocol, group, contact, Update.UPDATE));
            }
            update(contact);
        }
    }

    public void addContactToGroup(Protocol protocol, Group group, Contact contact) {
        if (model.hasProtocol(protocol)) {
            if (null == group) {
                group = protocol.getNotInListGroup();
            }
            model.addToGroup(protocol, group, contact);
        }
    }

    public void updateGroupData(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                model.updateGroupData(protocol, group);
            }
            update(group);
        }
    }

    public void collapseAll() {
        int count = model.getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = model.getProtocol(i);
            Vector groups = p.getGroupItems();
            for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                model.getGroupNode(p, (Group)groups.elementAt(groupIndex)).setExpandFlag(false);
            }
            model.getGroupNode(p, p.getNotInListGroup()).setExpandFlag(false);
        }
        ContactList.getInstance().getManager().setAllToTop();
        update();
    }

    public void putIntoQueue(Update u) {
        if (-1 == Util.getIndex(updateQueue, u)) {
            updateQueue.addElement(u);
        }
    }

    public void updateTree() {
        while (!updateQueue.isEmpty()) {
            Update update = (Update)updateQueue.firstElement();
            updateQueue.removeElementAt(0);
            model.updateGroupOrder(update);
        }
    }

    public static class Update {
        public Protocol protocol;
        public Group group;
        public Contact contact;
        private byte event;
        private static final byte UPDATE = 1;
        private static final byte ADD    = 2;
        private static final byte REMOVE = 3;

        public Update(Protocol protocol, Group group, Contact contact, byte event) {
            this.protocol = protocol;
            this.group = group;
            this.contact = contact;
            this.event = event;
        }
    }
}
