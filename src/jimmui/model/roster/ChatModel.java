package jimmui.model.roster;

import jimm.chat.Chat;
import jimm.cl.ContactList;
import protocol.Contact;
import protocol.Group;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.GroupBranch;
import jimmui.view.roster.ProtocolBranch;
import jimmui.view.roster.Updater;
import protocol.Protocol;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 29.06.13 12:53
 *
 * @author vladimir
 */
public class ChatModel extends ContactListModel {
    @Override
    public void buildFlatItems(Vector items) {
        Vector<Chat> chats = ContactList.getInstance().jimmModel.chats;
        for (int i = 0; i < chats.size(); ++i) {
            items.addElement(((Chat)chats.elementAt(i)).getContact());
        }
    }

    @Override
    public void updateOrder(Updater.Update u) {
    }

    @Override
    public void updateProtocol(Protocol protocol, Vector<Group> oldGroups, Vector<Contact> oldContacts) {
    }

    @Override
    public void addGroup(Updater.Update u) {
    }

    @Override
    public void removeGroup(Updater.Update u) {
    }

    @Override
    public GroupBranch getGroupNode(Updater.Update u) {
        return null;
    }

    @Override
    public ProtocolBranch getProtocolNode(Updater.Update u) {
        return null;
    }

    @Override
    public boolean hasGroups() {
        return false;
    }
}
