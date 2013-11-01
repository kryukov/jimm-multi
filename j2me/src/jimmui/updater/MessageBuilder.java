package jimmui.updater;

import jimm.Jimm;
import jimm.Options;
import jimm.comm.StringUtils;
import jimmui.view.chat.Chat;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.chat.message.SystemNotice;
import jimm.comm.Util;
import jimm.history.CachedRecord;
import jimm.history.HistoryStorage;
import jimm.modules.MagicEye;
import jimmui.view.base.CanvasEx;
import jimmui.view.icons.Icon;
import jimmui.view.text.Par;
import jimmui.view.text.Parser;
import protocol.ui.InfoFactory;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 05.07.13 22:50
 *
 * @author vladimir
 */
public class MessageBuilder {
    public void addMessage(ChatModel model, Message message, boolean toHistory) {
        Chat chat = Jimm.getJimm().getCL().getChat(model);
        boolean inc = (null == chat) || !chat.isVisibleChat();
        if (message instanceof PlainMessage) {

            buildMessage(model, message);
            // #sijapp cond.if modules_HISTORY is "true" #
            if (toHistory && Options.getBoolean(Options.OPTION_HISTORY)) {
                final String nick = getFrom(model, message);
                addToHistory(model, message.getText(), true, nick, message.getNewDate());
            }
            // #sijapp cond.end#
            if (inc) {
                model.messageCounter = inc(model.messageCounter);
                // #sijapp cond.if protocols_JABBER is "true" #
                if (!model.getContact().isSingleUserContact()
                        && !MessageBuilder.isHighlight(message.getProcessedText(), model.getMyName())) {
                    model.otherMessageCounter = inc(model.otherMessageCounter);
                    model.messageCounter--;
                }
                // #sijapp cond.end#
            }

        } else if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.TYPE_NOTICE_AUTHREQ == notice.getMessageType()) {
                inc = true;
                model.authRequestCounter = inc(model.authRequestCounter);

            } else if (inc) {
                model.sysNoticeCounter = inc(model.sysNoticeCounter);
            }

            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            MagicEye.addAction(model.getProtocol(), model.getContact().getUserId(), message.getText());
            // #sijapp cond.end #
            buildMessage(model, message);
        }
        if (inc) {
            model.getContact().updateChatState(model);
        }
    }

    public void addMyMessage(ChatModel chat, PlainMessage message) {
        chat.resetUnreadMessages();
        buildMessage(chat, message);
        // #sijapp cond.if modules_HISTORY is "true" #
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            addToHistory(chat, message.getText(), false, getFrom(chat, message), message.getNewDate());
        }
        // #sijapp cond.end#
    }

    public MessData buildMessage(ChatModel chat, Message message) {
        String from = getFrom(chat, message);
        boolean incoming = message.isIncoming();

        String messageText = message.getProcessedText();
        messageText = StringUtils.removeCr(messageText);
        if (StringUtils.isEmpty(messageText)) {
            return null;
        }
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return null;
            }
        }

        Parser parser = createParser(chat, null);

        final byte captColor = getInOutColor(incoming);
        final byte plain = CanvasEx.FONT_STYLE_PLAIN;
        if (isMe) {
            Icon icon = InfoFactory.msgIcons.iconAt(getIcon(chat, message));
            if (null != icon) {
                parser.addImage(icon);
            }
            parser.addText("*", captColor, plain);
            parser.addText(from, captColor, plain);
            parser.addText(" ", captColor, plain);
            parser.addTextWithSmiles(messageText, captColor, plain);

        } else {
            byte color = CanvasEx.THEME_TEXT;
            // #sijapp cond.if protocols_JABBER is "true" #
            if (incoming && !chat.getContact().isSingleUserContact()
                    && isHighlight(messageText, chat.getMyName())) {
                color = CanvasEx.THEME_CHAT_HIGHLIGHT_MSG;
            }
            // #sijapp cond.end#
            parser.addTextWithSmiles(messageText, color, plain);
        }
        short flags = 0;
        if (SystemNotice.TYPE_FILE == message.getMessageType()) {
            parser.addProgress(CanvasEx.THEME_TEXT);
            flags |= MessData.PROGRESS;
        }

        if (incoming) {
            flags |= MessData.INCOMING;
        }
        if (isMe) {
            flags |= MessData.ME;
        }
        if (Util.hasURL(messageText)) {
            flags |= MessData.URLS;
        }
        if (message instanceof SystemNotice) {
            flags |= MessData.SERVICE;
        }

        Par par = parser.getPar();
        MessData mData = new MessData(message.getNewDate(), messageText, from, flags,
                getIcon(chat, message), par);
        if (!incoming) {
            message.setVisibleIcon(par, mData);
        }
        Chat view = Jimm.getJimm().getCL().getChat(chat);
        synchronized (chat) {
            boolean atTheEnd = chatAtTheEnd(view);
            if (null != view) {
                view.lock();
            }
            chat.add(mData);
            setCursor(chat, view, incoming, atTheEnd);
            Jimm.getJimm().getChatUpdater().removeOldMessages(chat);
            if (null != view) {
                Jimm.getJimm().getChatUpdater().restoreTopPositionToUI(chat, view);
                view.unlock();
            }
        }
        return mData;
    }
    private boolean chatAtTheEnd(Chat chat) {
        try {
            return (null == chat) || (chat.getContent().getFullSize() - chat.getContent().getTopOffset() <= chat.getContentHeight());
        } catch (Exception ignored) {
            return true;
        }
    }
    private void setCursor(ChatModel chat, Chat view, boolean incoming, boolean atTheEnd) {
        int size = chat.size();

        if (incoming) {
            int currentMessageIndex = chat.current;
            if ((null != view) && view.isVisibleChat()) {
                // #sijapp cond.if modules_TOUCH is "true"#
                if (atTheEnd) {
                    atTheEnd = (currentMessageIndex == size - 2);
                    if (view.touchUsed) {
                        atTheEnd = true;
                    }
                }
                // #sijapp cond.end#
                if (atTheEnd) {
                    chat.current = chat.size() - 1;
                }

            } else {
                int unread = chat.getUnreadMessageCount();
                if (size - unread - 2 <= currentMessageIndex) {
                    chat.current = Math.max(0, size - 1 - unread);
                    chat.bottomOffset = -1;
                }
            }

        } else if (chat.isBlogBot()) {
            if (atTheEnd) {
                chat.current = chat.size() - 1;
            }

        } else {
            chat.bottomOffset = -1;
            chat.current = chat.size() - 1;
        }
    }

    private int getIcon(ChatModel chat, Message message) {
        if (message instanceof SystemNotice) {
            int type = ((SystemNotice)message).getMessageType();
            if (SystemNotice.TYPE_NOTICE_MESSAGE == type) {
                return Message.ICON_NONE;
            }
            if (SystemNotice.TYPE_FILE == type) {
                return Message.ICON_NONE;
            }
            return Message.ICON_SYSREQ;
        }
        if (message.isIncoming()) {
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!chat.getContact().isSingleUserContact()
                    && !isHighlight(message.getProcessedText(), chat.getMyName())) {
                return Message.ICON_IN_MSG;
            }
            // #sijapp cond.end#
            return Message.ICON_IN_MSG_HI;
        }
        return Message.ICON_OUT_MSG;
    }

    private byte getInOutColor(boolean incoming) {
        return incoming ? CanvasEx.THEME_CHAT_INMSG : CanvasEx.THEME_CHAT_OUTMSG;
    }

    private Parser createParser(ChatModel chat, Par par) {
        if (null == par) {
            return new Parser(chat.fontSet, Jimm.getJimm().getDisplay().getMinScreenMetrics() - 3);
        } else {
            return new Parser(par, chat.fontSet, Jimm.getJimm().getDisplay().getMinScreenMetrics() - 3);
        }
    }

    private String getFrom(ChatModel chat, Message message) {
        String senderName = message.getName();
        if (null == senderName) {
            senderName = message.isIncoming()
                    ? chat.getContact().getName()
                    : chat.getMyName();
        }
        return senderName;
    }

    // #sijapp cond.if protocols_JABBER is "true" #
    public static boolean isHighlight(String text, String nick) {
        if (null == nick) {
            return false;
        }
        for (int index = text.indexOf(nick); -1 != index; index = text.indexOf(nick, index + 1)) {
            if (0 < index) {
                char before = text.charAt(index - 1);
                if ((' ' != before) && ('\n' != before) && ('\t' != before)) {
                    continue;
                }
            }
            if (index + nick.length() + 2 < text.length()) {
                // Calculate space char...
                // ' a': min(' ', 'a') is ' '
                // 'a ': min('a', ' ') is ' '
                char after = (char) Math.min(text.charAt(index + nick.length()),
                        text.charAt(index + nick.length() + 1));
                if ((' ' != after) && ('\n' != after) && ('\t' != after)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    // #sijapp cond.end#

    // #sijapp cond.if modules_HISTORY is "true" #
    public void fillFromHistory(ChatModel chat) {
        if (!chat.contact.hasHistory()) {
            return;
        }
        if (chat.isBlogBot()) {
            return;
        }
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            if (0 != chat.size()) {
                return;
            }
            HistoryStorage hist = HistoryStorage.getHistory(chat.contact);
            hist.openHistory();
            int recCount = hist.getHistorySize();
            if (0 == recCount) {
                return;
            }

            int loadOffset = Math.max(recCount - ChatModel.MAX_HIST_LAST_MESS, 0);
            for (int i = loadOffset; i < recCount; ++i) {
                CachedRecord rec = hist.getRecord(i);
                if (null == rec) {
                    continue;
                }
                long date = Util.createLocalDate(rec.date);
                PlainMessage message;
                if (rec.isIncoming()) {
                    message = new PlainMessage(rec.from, chat.getProtocol(), date, rec.text, true);
                } else {
                    message = new PlainMessage(chat.getProtocol(), chat.getContact(), date, rec.text);
                }
                buildMessage(chat, message);
            }
            hist.closeHistory();
        }
    }
    private void addToHistory(ChatModel chat, String msg, boolean incoming, String nick, long time) {
        if (chat.getContact().hasHistory()) {
            HistoryStorage.getHistory(chat.contact).addText(msg, incoming, nick, time);
        }
    }
    // #sijapp cond.end#

    private short inc(short val) {
        return (short) ((val < Short.MAX_VALUE) ? (val + 1) : val);
    }
    private byte inc(byte val) {
        return (byte) ((val < Byte.MAX_VALUE) ? (val + 1) : val);
    }

    // #sijapp cond.if modules_FILES="true"#
    public MessData addFileProgress(ChatModel model, String caption, String text) {
        SystemNotice notice = new SystemNotice(model.protocol, SystemNotice.TYPE_FILE, model.getContact().getUserId(), text);
        notice.setName(caption);
        return buildMessage(model, notice);
    }

    public void changeFileProgress(ChatModel model, MessData mData, String caption, String text) {
        Parser parser = createParser(model, mData.par);
        parser.addText(text, CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);

        long time = Jimm.getCurrentGmtTime();
        short flags = (short)(mData.rowData & ~MessData.PROGRESS);
        synchronized (this) {
            int index = model.getIndex(mData);

            if ((0 < model.size()) && (0 <= index)) {
                Chat view = Jimm.getJimm().getCL().getChat(model);
                if (null != view) view.lock();
                mData.init(time, text, caption, flags, Message.ICON_NONE);
                parser.commit();
                if (null != view) view.unlock();
            }
        }
    }
    // #sijapp cond.end#
}
