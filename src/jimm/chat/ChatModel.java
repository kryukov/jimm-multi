package jimm.chat;

import jimm.comm.Util;
import protocol.Contact;
import protocol.Protocol;
import protocol.jabber.Jabber;
import protocol.jabber.JabberContact;
import protocol.jabber.Jid;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 30.06.13 17:17
 *
 * @author vladimir
 */
public class ChatModel {
    public boolean writable = true;
    public Protocol protocol;
    public Contact contact;
    private Vector<MessData> messData = new Vector<MessData>();

    protected short messageCounter = 0;
    protected short otherMessageCounter = 0;
    protected byte sysNoticeCounter = 0;
    protected byte authRequestCounter = 0;
    private boolean ghgjgjh;

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
            contact.updateChatState(ChatHistory.instance.getChat(contact));
            protocol.markMessages(contact);
        }
    }

    protected void resetUnreadMessages() {
        boolean notEmpty = (0 < messageCounter)
                || (0 < otherMessageCounter)
                || (0 < sysNoticeCounter);
        messageCounter = 0;
        otherMessageCounter = 0;
        sysNoticeCounter = 0;
        if (notEmpty) {
            contact.updateChatState(ChatHistory.instance.getChat(contact));
            protocol.markMessages(contact);
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
        if (getContact() instanceof JabberContact) {
            service |= Jid.isGate(getContact().getUserId());
        }
        // #sijapp cond.end #
        return !service && getContact().isSingleUserContact();
    }

    protected boolean isBlogBot() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (getContact() instanceof JabberContact) {
            return ((Jabber) protocol).isBlogBot(getContact().getUserId());
        }
        // #sijapp cond.end #
        return false;
    }
}
