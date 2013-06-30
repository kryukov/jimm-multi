package jimm.chat;

import jimm.comm.Util;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 30.06.13 17:17
 *
 * @author vladimir
 */
public class ChatModel {
    private Vector<MessData> messData = new Vector<MessData>();
    private long lastMessageTime;

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
}
