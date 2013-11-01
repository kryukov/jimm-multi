package jimmui.updater;

import jimm.Jimm;
import jimm.Options;
import jimm.comm.StringUtils;
import jimmui.view.chat.Chat;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.history.HistoryStorage;
import jimm.history.HistoryStorageList;
import jimm.io.Storage;
import jimmui.view.base.GraphicsEx;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.XmppServiceContact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;


/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 02.07.13 23:18
 *
 * @author vladimir
 */
public class ChatUpdater {
    public ChatModel createModel(Protocol p, Contact contact) {
        ChatModel chat;
        synchronized (Jimm.getJimm().jimmModel.chats) {
            chat = Jimm.getJimm().jimmModel.getChatModel(contact);
            if (null != chat) {
                return chat;
            }
            chat = new ChatModel();
            chat.protocol = p;
            chat.contact = contact;
            chat.fontSet = GraphicsEx.chatFontSet;
            Jimm.getJimm().jimmModel.registerChat(chat);
        }
        // #sijapp cond.if modules_HISTORY is "true" #
        new MessageBuilder().fillFromHistory(chat);
        // #sijapp cond.end #
        return chat;
    }

    public void addMessage(ChatModel chat, Message message, boolean toHistory) {
        new MessageBuilder().addMessage(chat, message, toHistory);
    }

    public void addMyMessage(ChatModel chat, PlainMessage plainMsg) {
        new MessageBuilder().addMyMessage(chat, plainMsg);
    }

    // #sijapp cond.if modules_FILES="true"#
    public MessData addFileProgress(ChatModel chat, String caption, String text) {
        return new MessageBuilder().addFileProgress(chat, caption, text);
    }

    public void changeFileProgress(ChatModel chat, MessData mData, String caption, String text) {
        new MessageBuilder().changeFileProgress(chat, mData, caption, text);
    }
    // #sijapp cond.end#

    public void activate(ChatModel chat) {
        Chat view = Jimm.getJimm().getCL().getOrCreateChat(chat);
        view.activate();
    }

    public void invalidate(ChatModel chat) {
        Chat view = Jimm.getJimm().getCL().getChat(chat);
        if (null != view) {
            view.invalidate();
        }
    }

    public void removeReadMessages(ChatModel chat) {
        removeMessages(chat, chat.getUnreadMessageCount());
    }

    public void writeMessage(ChatModel chat, String message) {
        Chat view = Jimm.getJimm().getCL().getOrCreateChat(chat);
        view.writeMessage(message);
    }

    public void writeMessage(Protocol protocol, Contact contact, String message) {
        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(contact);
        if (null == chat) chat = createModel(protocol, contact);
        writeMessage(chat, message);
    }

    public void writeMessageTo(Protocol protocol, XmppServiceContact conference, String nick) {
        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(conference);
        if (null == chat) chat = createModel(protocol, conference);
        Chat view = Jimm.getJimm().getCL().getOrCreateChat(chat);
        view.writeMessageTo(nick);
    }

    public void showHistory(Contact contact) {
        HistoryStorage history = HistoryStorage.getHistory(contact);
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().externalApi.showHistory(history);
        // #sijapp cond.else #
        new HistoryStorageList(history).show();
        // #sijapp cond.end #
    }

    public void typing(ChatModel chat, boolean type) {
        Chat view = Jimm.getJimm().getCL().getChat(chat);
        if (null != view) {
            view.beginTyping(type);
        }
    }

    public void removeOldMessages(ChatModel chat) {
        removeMessages(chat, Options.getInt(Options.OPTION_MAX_MSG_COUNT));
    }
    public void removeMessagesAtCursor(ChatModel model) {
        restoreTopPositionToUI(model, Jimm.getJimm().getCL().getChat(model));
        removeMessages(model, model.size() - model.current - 1);
    }
    private void removeMessages(ChatModel chat, int limit) {
        if (chat.size() < limit) {
            return;
        }
        if ((0 < limit) && (0 < chat.size())) {
            while (limit < chat.size()) {
                if (-1 < chat.bottomOffset) {
                    chat.bottomOffset = Math.max(0, chat.bottomOffset - chat.getItemHeight(chat.getMessage(0)));
                }
                chat.current = Math.max(0, chat.current - 1);
                chat.removeTopMessage();
            }
            restoreTopPositionToUI(chat, Jimm.getJimm().getCL().getChat(chat));
            invalidate(chat);
        } else {
            Jimm.getJimm().jimmModel.unregisterChat(chat);
        }
    }

    public void restoreTopPositionToUI(ChatModel chat, Chat view) {
        if (null != view) {
            if (-1 == chat.bottomOffset) {
                view.getContent().setCurrentItemToTop(chat.current);
            } else {
                view.getContent().setTopByOffset(chat.bottomOffset - view.getContentHeight());
                view.getContent().setCurrentItemIndex(chat.current);
            }
        }
    }

    public void saveUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.delete();
            s.open(true);
            Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
            for (int i = 0; i < chats.size(); ++i) {
                ChatModel chat = (ChatModel) chats.elementAt(i);
                if (!chat.getContact().isSingleUserContact()) {
                    continue;
                }
                if (chat.getContact().isTemp()) {
                    continue;
                }
                if (!chat.getContact().hasHistory()) {
                    continue;
                }
                int count = chat.getUnreadMessageCount();
                for (int j = 0; j < count; ++j) {
                    MessData message = chat.getUnreadMessage(j);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream o = new DataOutputStream(out);
                    o.writeUTF(chat.getProtocol().getUserId());
                    o.writeUTF(chat.getContact().getUserId());
                    o.writeUTF(message.getNick());
                    o.writeUTF(message.getText());
                    o.writeLong(message.getTime());
                    s.addRecord(out.toByteArray());
                }
            }
        } catch (Exception ignored) {
        }
        s.close();
    }

    public void loadUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.open(false);
            for (int i = 1; i <= s.getNumRecords(); ++i) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(s.getRecord(i)));
                String account = in.readUTF();
                String userId = in.readUTF();
                String nick = in.readUTF();
                String text = in.readUTF();
                long time = in.readLong();
                Protocol protocol = Jimm.getJimm().jimmModel.getProtocol(account);
                if (null == protocol) {
                    continue;
                }
                PlainMessage msg = new PlainMessage(userId, protocol, time, text, true);
                if (!StringUtils.isEmpty(nick)) {
                    msg.setName(nick);
                }
                protocol.addMessage(msg, true);
            }
        } catch (Exception ignored) {
        }
        s.close();
        s.delete();
    }
}
