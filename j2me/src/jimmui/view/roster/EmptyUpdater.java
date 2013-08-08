package jimmui.view.roster;

import jimmui.model.chat.ChatModel;
import jimmui.updater.RosterUpdater;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 14.07.13 23:36
 *
 * @author vladimir
 */
public class EmptyUpdater extends RosterUpdater {
    public void addGroup(Protocol protocol, Group group) {
    }

    public void unregisterChat(ChatModel item) {
    }

    public void registerChat(ChatModel item) {
    }


    public void updateProtocol(Protocol protocol, Roster oldRoster) {
    }

    public void removeGroup(Protocol protocol, Group group) {
    }

    public void update() {
    }
    public void update(Protocol protocol) {
    }

    public void update(Group group) {
    }

    private void update(Contact contact) {
    }
    public void repaint() {
    }

    public void typing(Protocol protocol, Contact item) {
    }

    public void setOffline(Protocol protocol) {
    }

    public void removeFromGroup(Protocol protocol, Group g, Contact c) {
    }

    public void updateContact(Protocol protocol, Group group, Contact contact) {
    }

    public void addContactToGroup(Protocol protocol, Group group, Contact contact) {
    }

    public void collapseAll() {
    }

    public void putIntoQueue(Update u) {
    }

    public void updateTree() {
    }

    public ContactListModel createModel() {
        return null;
    }

    public ContactListModel getModel() {
        return null;
    }

    public ContactListModel getChatModel() {
        return null;
    }

    public void addProtocols(Vector<Protocol> protocols) {
    }

    public void updateConnectionStatus() {
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.updateConnectionState();
        // #sijapp cond.end #
    }

    public void updateModel() {
    }
}
