package jimm.cl;

import jimm.chat.Chat;
import jimm.chat.ChatModel;
import jimm.comm.Util;
import protocol.Protocol;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 29.06.13 14:44
 *
 * @author vladimir
 */
public class JimmModel {
    public Vector<Protocol> protocols = new Vector<Protocol>();
    public final Vector<ChatModel> chats = new Vector<ChatModel>();
    public final Hashtable modelToChat = new Hashtable();

    public boolean registerChat(Chat item) {
        if (-1 == Util.getIndex(chats, item)) {
            chats.addElement(item.getModel());
            modelToChat.put(item.getModel(), item);
            item.getContact().updateChatState(item);
            return true;
        }
        return false;
    }
    public boolean unregisterChat(Chat item) {
        if (null == item) return false;
        chats.removeElement(item);
        item.getModel().clear();
        item.getContact().updateChatState(null);
        return true;
    }
}