package DrawControls.roster;

import DrawControls.roster.models.ContactModel;
import DrawControls.roster.models.GroupContactModel;
import DrawControls.roster.models.ProtocolContactModel;
import DrawControls.roster.models.ProtocolGroupContactModel;
import jimm.Options;
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
            model.addGroup(new Update(protocol,  group, null, Update.GROUP_ADD));
        }
    }

    public void updateProtocol(Protocol protocol) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                Vector groups = protocol.getGroupItems();
                for (int i = 0; i < groups.size(); ++i) {
                    addGroup(protocol, (Group) groups.elementAt(i));
                }
                addGroup(protocol, null);
            }
            update();
        }
    }

    public void removeGroup(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                model.removeGroup(new Update(protocol,  group, null, Update.GROUP_REMOVE));
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
            // TODO: if contact visible only
            ContactList.getInstance().getManager().invalidate();
        }
    }

    public void setOffline(Protocol protocol) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                putIntoQueue(new Update(protocol, null, null, Update.PROTOCOL_UPDATE));
            }
            update();
        }
    }

    public void removeFromGroup(Protocol protocol, Group g, Contact c) {
        if (model.hasProtocol(protocol)) {
            model.removeFromGroup(new Update(protocol,  g, c, Update.REMOVE));
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
            model.addToGroup(new Update(protocol, group, contact, Update.ADD));
            updateContact(protocol, group, contact);
        }
    }

    public void collapseAll() {
        try {
            int count = model.getProtocolCount();
            for (int i = 0; i < count; ++i) {
                Protocol p = model.getProtocol(i);
                Vector groups = p.getGroupItems();
                for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                    model.getGroupNode(new Update(p, (Group)groups.elementAt(groupIndex), null, Update.EXPAND)).setExpandFlag(false);
                }
                model.getGroupNode(new Update(p, null, null, Update.EXPAND)).setExpandFlag(false);
            }
        } catch (Exception e) {
            // no groups mode
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
            model.updateOrder(update);
        }
    }

    public ContactListModel createModel() {
        // #sijapp cond.if modules_MULTI is "true" #
        if (!Options.getBoolean(Options.OPTION_USER_ACCOUNTS)) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                model = new GroupContactModel();
            } else {
                model = new ContactModel();
            }
            setModel(model);
            return model;
        }
        // #sijapp cond.end #
        if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
            model = new ProtocolGroupContactModel();
        } else {
            model = new ProtocolContactModel();
        }
        setModel(model);
        return model;
    }

    public static class Update {
        public Protocol protocol;
        public Group group;
        public Contact contact;
        public byte event;
        public static final byte UPDATE = 1;
        public static final byte PROTOCOL_UPDATE = 2;
        public static final byte ADD    = 3;
        public static final byte GROUP_ADD    = 4;
        public static final byte REMOVE = 5;
        public static final byte GROUP_REMOVE = 6;
        public static final byte EXPAND = 7;

        public Update(Protocol protocol, Group group, Contact contact, byte event) {
            this.protocol = protocol;
            this.group = group;
            this.contact = contact;
            this.event = event;
        }
    }
}
