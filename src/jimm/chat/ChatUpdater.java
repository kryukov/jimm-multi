package jimm.chat;

import jimm.Options;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.history.HistoryStorage;
import jimm.history.HistoryStorageList;
import jimmui.view.base.GraphicsEx;
import protocol.Contact;
import protocol.Protocol;
import protocol.jabber.JabberServiceContact;


/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 02.07.13 23:18
 *
 * @author vladimir
 */
public class ChatUpdater {
    public ChatModel createModel(Protocol p, Contact contact) {
        ChatModel chat = new ChatModel();
        chat.protocol = p;
        chat.contact = contact;
        chat.fontSet = GraphicsEx.chatFontSet;
        // #sijapp cond.if modules_HISTORY is "true" #
        new MessageBuilder().fillFromHistory(chat);
        // #sijapp cond.end #
        return chat;
    }

    public void addMessage(ChatModel chat, Message message, boolean toHistory) {
        ChatHistory.instance.registerChat(chat);
        new MessageBuilder().addMessage(chat, message, toHistory);
    }

    public void addMyMessage(ChatModel chat, PlainMessage plainMsg) {
        ChatHistory.instance.registerChat(chat);
        new MessageBuilder().addMyMessage(chat, plainMsg);
    }

    // #sijapp cond.if modules_FILES="true"#
    public MessData addFileProgress(ChatModel chat, String caption, String text) {
        ChatHistory.instance.registerChat(chat);
        return new MessageBuilder().addFileProgress(chat, caption, text);
    }

    public void changeFileProgress(ChatModel chat, MessData mData, String caption, String text) {
        new MessageBuilder().changeFileProgress(chat, mData, caption, text);
    }
    // #sijapp cond.end#

    public void activate(ChatModel chat) {
        Chat view = ChatHistory.instance.getOrCreateChat(chat);
        view.activate();
    }

    public void invalidate(ChatModel chat) {
        Chat view = ChatHistory.instance.getChat(chat);
        if (null != view) {
            view.invalidate();
        }
    }

    public void removeReadMessages(ChatModel chat) {
        removeMessages(chat, chat.getUnreadMessageCount());
    }

    public void writeMessage(ChatModel chat, String message) {
        Chat view = ChatHistory.instance.getOrCreateChat(chat);
        view.writeMessage(message);
    }

    public void writeMessage(Contact contact, String message) {
        writeMessage(ChatHistory.instance.getChatModel(contact), message);
    }

    public void showHistory(Contact contact) {
        HistoryStorage history = HistoryStorage.getHistory(contact);
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().externalApi.showHistory(history);
        // #sijapp cond.else #
        new HistoryStorageList(history).show();
        // #sijapp cond.end #
    }

    public void writeMessageTo(JabberServiceContact conference, String nick) {
        ChatModel chat = ChatHistory.instance.getChatModel(conference);
        Chat view = ChatHistory.instance.getOrCreateChat(chat);
        view.writeMessageTo(nick);
    }

    public void typing(ChatModel chat, boolean type) {
        Chat view = ChatHistory.instance.getChat(chat);
        if (null != view) {
            view.beginTyping(type);
        }
    }

    public void removeOldMessages(ChatModel chat) {
        removeMessages(chat, Options.getInt(Options.OPTION_MAX_MSG_COUNT));
    }
    public void removeMessagesAtCursor(ChatModel model) {
        restoreTopPositionToUI(model, ChatHistory.instance.getChat(model));
        removeMessages(model, model.size() - model.current - 1);
    }
    private void removeMessages(ChatModel chat, int limit) {
        if (chat.size() < limit) {
            return;
        }
        if ((0 < limit) && (0 < chat.size())) {
            storeTopPosition(chat);
            while (limit < chat.size()) {
                chat.topOffset = Math.max(0, chat.topOffset - chat.getItemHeight(chat.getMessage(0)));
                chat.current = Math.max(0, chat.current - 1);
                chat.removeTopMessage();
            }
            restoreTopPositionToUI(chat, ChatHistory.instance.getChat(chat));
            invalidate(chat);
        } else {
            ChatHistory.instance.unregisterChat(chat);
        }
    }

    void restoreTopPositionToUI(ChatModel chat, Chat view) {
        if (null != view) {
            if (-1 == chat.topOffset) {
                view.setCurrentItemToTop(chat.current);
            } else {
                view.setTopByOffset(chat.topOffset);
                view.setCurrentItemIndex(chat.current);
            }
        }
    }

    void storeTopPosition(ChatModel chat) {
        Chat view = ChatHistory.instance.getChat(chat);
        if (null != view) {
            chat.topOffset = view.getTopOffset();
            chat.current = view.getCurrItem();
        }
    }
}
