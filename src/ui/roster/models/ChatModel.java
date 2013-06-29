package ui.roster.models;

import jimm.chat.Chat;
import ui.roster.ContactListModel;
import ui.roster.GroupBranch;
import ui.roster.ProtocolBranch;
import ui.roster.Updater;
import jimm.chat.ChatHistory;
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
    public final Vector<Chat> chats = new Vector<Chat>();

    @Override
    public void buildFlatItems(Vector items) {
        for (int i = 0; i < chats.size(); ++i) {
            items.addElement(((Chat)chats.elementAt(i)).getContact());
        }
    }

    @Override
    public void updateOrder(Updater.Update u) {
    }

    @Override
    protected void addProtocol(Protocol prot) {
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
