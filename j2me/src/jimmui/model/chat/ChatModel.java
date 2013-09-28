package jimmui.model.chat;

import jimm.Jimm;
import jimmui.view.chat.Chat;
import jimm.chat.message.Message;
import jimm.comm.Util;
import jimmui.view.icons.Icon;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import protocol.xmpp.Jid;
import protocol.ui.InfoFactory;

import javax.microedition.lcdui.Font;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 30.06.13 17:17
 *
 * @author vladimir
 */
public class ChatModel {
    public static final int MAX_HIST_LAST_MESS = 5;

    public boolean writable = true;
    public Protocol protocol;
    public Contact contact;
    private Vector<MessData> messData = new Vector<MessData>();

    public short messageCounter = 0;
    public short otherMessageCounter = 0;
    public byte sysNoticeCounter = 0;
    public byte authRequestCounter = 0;
    public int bottomOffset = -1;
    public int current;
    public Font[] fontSet;

    public int size() {
        return messData.size();
    }

    public MessData getMessage(int index) {
        return (MessData) messData.elementAt(index);
    }

    public void add(MessData mData) {
        messData.addElement(mData);
    }

    public int getIndex(MessData mData) {
        return Util.getIndex(messData, mData);
    }

    public void clear() {
        messData.removeAllElements();
    }

    public long getLastMessageTime() {
        if (0 == messData.size()) {
            return 0;
        }
        MessData md = (MessData) messData.lastElement();
        return md.getTime();
    }

    public void removeTopMessage() {
        messData.removeElementAt(0);
    }

    public final void setWritable(boolean wr) {
        writable = wr;
    }

    public void resetAuthRequests() {
        boolean notEmpty = (0 < authRequestCounter);
        authRequestCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            Jimm.getJimm().getCL().markMessages(protocol, contact);
        }
    }

    public void resetUnreadMessages() {
        boolean notEmpty = (0 < messageCounter)
                || (0 < otherMessageCounter)
                || (0 < sysNoticeCounter);
        messageCounter = 0;
        otherMessageCounter = 0;
        sysNoticeCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            Jimm.getJimm().getCL().markMessages(protocol, contact);
        }
    }

    public int getUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter
                + otherMessageCounter;
    }
    public int getPersonalUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter;
    }

    public Contact getContact() {
        return contact;
    }

    public boolean isHuman() {
        boolean service = isBlogBot() || protocol.isBot(getContact());
        // #sijapp cond.if protocols_JABBER is "true" #
        if (getContact() instanceof XmppContact) {
            service |= Jid.isGate(getContact().getUserId());
        }
        // #sijapp cond.end #
        return !service && getContact().isSingleUserContact();
    }

    public boolean isBlogBot() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (getContact() instanceof XmppContact) {
            return ((Xmpp) protocol).isBlogBot(getContact().getUserId());
        }
        // #sijapp cond.end #
        return false;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public MessData getUnreadMessage(int num) {
        return getMessage(size() - getUnreadMessageCount() + num);
    }

    public final int getNewMessageIcon() {
        if (0 < messageCounter) {
            return Message.ICON_IN_MSG_HI;
        } else if (0 < authRequestCounter) {
            return Message.ICON_SYSREQ;
        } else if (0 < otherMessageCounter) {
            return Message.ICON_IN_MSG;
        } else if (0 < sysNoticeCounter) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }
    public final String getMyName() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (getContact() instanceof XmppServiceContact) {
            String nick = ((XmppServiceContact)getContact()).getMyName();
            if (null != nick) return nick;
        }
        // #sijapp cond.end#
        return getProtocol().getNick();
    }

    public int getMessageHeaderHeight(MessData mData) {
        if ((null == mData) || mData.isMe()) return 0;

        int height = fontSet[Chat.FONT_STYLE_BOLD].getHeight();
        Icon icon = InfoFactory.msgIcons.iconAt(mData.iconIndex);
        if (null != icon) {
            height = Math.max(height, icon.getHeight());
        }
        return height;
    }

    public int getItemHeight(MessData mData) {
        return mData.par.getHeight() + getMessageHeaderHeight(mData);
    }

    public boolean hasAuthRequests() {
        return 0 < authRequestCounter;
    }
}
