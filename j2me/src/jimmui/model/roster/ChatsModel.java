package jimmui.model.roster;

import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.items.GroupBranch;
import jimmui.view.roster.items.ProtocolBranch;
import jimmui.updater.RosterUpdater;
import jimmui.view.roster.items.TreeNode;
import protocol.Protocol;
import protocol.Roster;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 29.06.13 12:53
 *
 * @author vladimir
 */
public class ChatsModel extends ContactListModel {
    @Override
    public void buildFlatItems(Vector<TreeNode> items) {
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        for (int i = 0; i < chats.size(); ++i) {
            items.addElement(((ChatModel)chats.elementAt(i)).getContact());
        }
    }

    @Override
    public void updateOrder(RosterUpdater.Update u) {
    }

    @Override
    public void updateProtocol(Protocol protocol, Roster oldRoster) {
    }

    @Override
    public void addGroup(RosterUpdater.Update u) {
    }

    @Override
    public void removeGroup(RosterUpdater.Update u) {
    }

    @Override
    public GroupBranch getGroupNode(RosterUpdater.Update u) {
        return null;
    }

    @Override
    public ProtocolBranch getProtocolNode(RosterUpdater.Update u) {
        return null;
    }

    @Override
    public boolean hasGroups() {
        return false;
    }
}
