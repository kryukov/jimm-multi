package jimm.cl;

import jimm.*;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
import jimm.comm.Util;
import protocol.Contact;
import protocol.Profile;
import protocol.Protocol;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.icq.*;
import protocol.mrim.*;
import protocol.xmpp.*;

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
    private int contactListSaveDelay = 0;
    // #sijapp cond.if modules_FILES="true"#
    private Vector<FileTransfer> transfers = new Vector<FileTransfer>();
    // #sijapp cond.end#

    public boolean registerChat(ChatModel item) {
        if (-1 == Util.getIndex(chats, item)) {
            chats.addElement(item);
            item.getContact().updateChatState(item);
            Jimm.getJimm().getCL().getUpdater().registerChat(item);
            return true;
        }
        return false;
    }
    public boolean unregisterChat(ChatModel item) {
        if (null == item) return false;
        chats.removeElement(item);
        item.clear();
        item.getContact().updateChatState(null);
        Contact c = item.getContact();
        c.updateChatState(null);
        Jimm.getJimm().getCL().getUpdater().unregisterChat(item);
        if (0 < item.getUnreadMessageCount()) {
            Jimm.getJimm().getCL().markMessages(item.protocol, c);
        }
        return true;
    }

    public ChatModel getChatModel(Contact c) {
        synchronized (chats) {
            for (int i = chats.size() - 1; 0 <= i; --i) {
                if (c == ((ChatModel)chats.elementAt(i)).contact) {
                    return (ChatModel)chats.elementAt(i);
                }
            }
        }
        return null;
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


    public void updateAccounts() {
        Protocol[] oldProtocols = getProtocols();
        Vector<Protocol> newProtocols = new Vector<Protocol>();
        int accountCount = Options.getAccountCount();
        for (int i = 0; i < accountCount; ++i) {
            Profile profile = Options.getAccount(i);
            if (!profile.isActive) continue;
            for (int j = 0; j < oldProtocols.length; ++j) {
                Protocol protocol = oldProtocols[j];
                if ((null != protocol) && profile.equalsTo(protocol.getProfile())) {
                    if (protocol.getProfile() != profile) {
                        protocol.setProfile(profile);
                    }
                    oldProtocols[j] = null;
                    profile = null;
                    newProtocols.addElement(protocol);
                    break;
                }
            }
            if (null != profile) {
                Protocol p = createProtocol(profile);
                if (null != p) {
                    newProtocols.addElement(p);
                }
            }
        }
        if (0 == newProtocols.size()) {
            Profile profile = Options.getAccount(0);
            profile.isActive = true;
            newProtocols.addElement(createProtocol(profile));
        }
        for (Protocol protocol : oldProtocols) {
            if (null != protocol) {
                protocol.disconnect(true);
                protocol.safeSave();
                for (int j = chats.size() - 1; 0 <= j; --j) {
                    ChatModel key = (ChatModel) chats.elementAt(j);
                    if (key.getProtocol() == protocol) {
                        Jimm.getJimm().jimmModel.unregisterChat(key);
                    }
                }
                Jimm.getJimm().getCL().markMessages(null, null);
                protocol.dismiss();
            }
        }
        protocols = newProtocols;
    }

    private Protocol createProtocol(Profile account) {
        Protocol protocol = null;
        switch (account.getEffectiveType()) {
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                protocol = new Icq();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                protocol = new Mrim();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_XMPP:
                protocol = new Xmpp();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_OBIMP is "true" #
            case Profile.PROTOCOL_OBIMP:
                protocol = new protocol.obimp.Obimp();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_VKAPI is "true" #
            case Profile.PROTOCOL_VK_API:
                protocol = new protocol.vk.Vk();
                break;
            // #sijapp cond.end #
        }
        if (null == protocol) {
            return null;
        }
        protocol.setProfile(account);
        protocol.init();
        protocol.safeLoad();
        return protocol;
    }

    public final void needRosterSave() {
        contactListSaveDelay = 60 * 4 /* * 250 = 60 sec */;
        // #sijapp cond.if modules_ANDROID is "true" #
        synchronized (this) {
            saveRosters();
        }
        // #sijapp cond.end #
    }
    public void saveRostersIfNeed() {
        // #sijapp cond.if modules_ANDROID isnot "true" #
        if (0 < contactListSaveDelay) {
            contactListSaveDelay--;
            if (0 == contactListSaveDelay) {
                saveRosters();
            }
        }
        // #sijapp cond.end #
    }

    private void saveRosters() {
        int count = protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) protocols.elementAt(i);
            p.safeSave();
        }
    }

    public void restoreContactsWithChat(Protocol p) {
        for (int i = 0; i < chats.size(); ++i) {
            ChatModel chat = (ChatModel) chats.elementAt(i);
            Contact contact = chat.contact;
            if (p != chat.getProtocol()) {
                continue;
            }
            if (!p.hasContact(contact)) {
                Contact newContact = p.getItemByUID(contact.getUserId());
                if (null != newContact) {
                    chat.contact = newContact;
                    contact.updateChatState(null);
                    newContact.updateChatState(chat);
                    continue;
                }
                if (contact.isSingleUserContact()) {
                    contact.setTempFlag(true);
                    contact.setGroup(null);
                } else {
                    if (null == p.getGroup(contact)) {
                        contact.setGroup(p.getGroup(contact.getDefaultGroupName()));
                    }
                }
                p.addTempContact(contact);
            }
        }
    }
}