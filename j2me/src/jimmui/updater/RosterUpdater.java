package jimmui.updater;

import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.model.roster.*;
import jimm.Options;
import jimm.comm.Util;
import jimmui.view.roster.ContactListModel;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.06.13 19:38
 *
 * @author vladimir
 */
public class RosterUpdater {
    private Vector<Update> updateQueue = new Vector<Update>();

    private ContactListModel chatModel = new ChatsModel();
    private ContactListModel model;
    private Contact currentContact;

    public void addGroup(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            model.addGroup(new Update(protocol,  group, null, Update.GROUP_ADD));
        }
    }

    public void unregisterChat(ChatModel item) {
        update(item.getContact());
    }

    public void registerChat(ChatModel item) {
        update(item.getContact());
    }


    public void updateProtocol(Protocol protocol, Roster oldRoster) {
        if (model.hasProtocol(protocol)) {

            synchronized (protocol.getRosterLockObject()) {
                model.updateProtocol(protocol, oldRoster);
            }
            update();
        }
    }

    public void removeGroup(Protocol protocol, Group group) {
        if (model.hasProtocol(protocol)) {
            synchronized (protocol.getRosterLockObject()) {
                model.removeGroup(new Update(protocol, group, null, Update.GROUP_REMOVE));
            }
            update();
        }
    }

    public void update() {
        Jimm.getJimm().getCL().getManager().update();
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
    public void repaint() {
        Jimm.getJimm().getCL().getManager().invalidate();
    }

    public void typing(Protocol protocol, Contact item) {
        if (model.hasProtocol(protocol)) {
            // TODO: if contact visible only
            Jimm.getJimm().getCL().getManager().invalidate();
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
            model.removeFromGroup(new Update(protocol, g, c, Update.REMOVE));
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
        Jimm.getJimm().getCL().getManager().getContent().setTopByOffset(0);
        Jimm.getJimm().getCL().getManager().getContent().setCurrentItemIndex(0);
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
        if (!Options.getBoolean(Options.OPTION_USER_ACCOUNTS)) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                model = new GroupContactModel();
            } else {
                model = new ContactModel();
            }
        } else {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                model = new ProtocolGroupContactModel();
            } else {
                model = new ProtocolContactModel();
            }
        }
        return model;
    }

    public ContactListModel getModel() {
        return model;
    }

    public ContactListModel getChatModel() {
        return chatModel;
    }

    public void addProtocols(Vector<Protocol> protocols) {
        model.addProtocols(protocols);
        chatModel.addProtocols(protocols);
    }

    public void updateConnectionStatus() {
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.updateConnectionState();
        // #sijapp cond.end #
    }

    public void setCurrentContact(Contact currentContact) {
        this.currentContact = currentContact;
    }

    public Contact getCurrentContact() {
        return currentContact;
    }

    public void updateModel() {
        for (int i = 0; i < model.getProtocolCount(); ++i) {
            Protocol p = model.getProtocol(i);
            synchronized (p.getRosterLockObject()) {
                model.updateProtocol(p, null);
            }
        }
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
