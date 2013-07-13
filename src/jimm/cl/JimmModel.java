package jimm.cl;

import jimm.*;
import jimm.chat.ChatModel;
import jimm.chat.MessData;
import jimm.comm.Util;
import protocol.Contact;
import protocol.Profile;
import protocol.Protocol;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;

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
    // #sijapp cond.if modules_FILES="true"#
    private Vector transfers = new Vector();
    // #sijapp cond.end#

    public boolean registerChat(ChatModel item) {
        if (-1 == Util.getIndex(chats, item)) {
            chats.addElement(item);
            item.getContact().updateChatState(item);
            return true;
        }
        return false;
    }
    public boolean unregisterChat(ChatModel item) {
        if (null == item) return false;
        chats.removeElement(item);
        item.clear();
        item.getContact().updateChatState(null);
        return true;
    }

    public Protocol[] getProtocols() {
        Protocol[] all = new Protocol[protocols.size()];
        for (int i = 0; i < all.length; ++i) {
            all[i] = (Protocol) protocols.elementAt(i);
        }
        return all;
    }

    // #sijapp cond.if modules_FILES="true"#
    public void addTransfer(FileTransfer ft) {
        transfers.addElement(ft);
    }
    public void removeTransfer(MessData par, boolean cancel) {
        for (int i = 0; i < transfers.size(); ++i) {
            FileTransfer ft = (FileTransfer)transfers.elementAt(i);
            if (ft.is(par)) {
                transfers.removeElementAt(i);
                if (cancel) {
                    ft.cancel();
                }
                return;
            }
        }
    }
    // #sijapp cond.end#

    public byte getGlobalStatus() {
        byte globalStatus = StatusInfo.STATUS_OFFLINE;
        int globalStatusWidth = StatusInfo.getWidth(globalStatus);
        for (int i = 0; i < protocols.size(); ++i) {
            byte status = ((Protocol)protocols.elementAt(i)).getProfile().statusIndex;
            if (StatusInfo.getWidth(status) < globalStatusWidth) {
                globalStatus = status;
                globalStatusWidth = StatusInfo.getWidth(globalStatus);
            }
        }
        if (null == InfoFactory.factory.global.getIcon(globalStatus)) {
            globalStatus = StatusInfo.STATUS_ONLINE;
        }
        return globalStatus;
    }

    public Protocol getProtocol(String account) {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.getUserId().equals(account)) {
                return p;
            }
        }
        return null;
    }
    public Protocol getProtocol(Profile profile) {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.getProfile() == profile) {
                return p;
            }
        }
        return null;
    }

    public Protocol getProtocol(Contact c) {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.hasContact(c)) {
                return p;
            }
        }
        return null;
    }

    public boolean isConnected() {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.isConnected() && !p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnecting() {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean disconnect() {
        boolean disconnecting = false;
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            if (p.isConnected()) {
                p.disconnect(false);
                disconnecting = true;
            }
        }
        return disconnecting;
    }

    public void safeSave() {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            p.safeSave();
        }
    }

}