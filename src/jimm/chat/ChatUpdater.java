package jimm.chat;

import jimm.Jimm;
import jimm.Options;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimmui.view.base.CanvasEx;
import jimmui.view.base.GraphicsEx;
import jimmui.view.text.Par;
import jimmui.view.text.Parser;

import javax.microedition.lcdui.Font;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 02.07.13 23:18
 *
 * @author vladimir
 */
public class ChatUpdater {
    public void addMessage(ChatModel chat, Message message, boolean toHistory) {
        ChatHistory.instance.getChat(chat.contact).addMessage(message, toHistory);
    }

    // #sijapp cond.if modules_FILES="true"#
    public MessData addFileProgress(ChatModel chat, String caption, String text) {
        return ChatHistory.instance.getChat(chat.contact).addFileProgress(caption, text);
    }

    public void changeFileProgress(ChatModel chat, MessData mData, String caption, String text) {
        ChatHistory.instance.getChat(chat.contact).changeFileProgress(mData, caption, text);
    }

    public void activate(ChatModel chat) {
        ChatHistory.instance.getChat(chat.contact).activate();
    }

    public void invalidate(ChatModel chat) {
        ChatHistory.instance.getChat(chat.contact).invalidate();
    }
    // #sijapp cond.end#
}
