/*
 * ProtocolBranch.java
 *
 * Created on 24 Март 2010 г., 15:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_MULTI is "true" #
package DrawControls.tree;

import DrawControls.icons.Icon;
import java.util.Vector;
import jimm.Options;
import jimm.chat.ChatHistory;
import jimm.comm.Util;
import protocol.*;
/**
 *
 * @author Vladimir Kryukov
 */
public class ProtocolBranch extends TreeBranch {
    private Protocol protocol;
    public ProtocolBranch(Protocol p) {
        protocol = p;
        setExpandFlag(false);
    }
    public boolean isProtocol(Protocol p) {
        return protocol == p;
    }
    public Protocol getProtocol() {
        return protocol;
    }
    public boolean isEmpty() {
        if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) {
            Vector contacts = protocol.getContactItems();
            for (int i = contacts.size() - 1; 0 <= i; --i) {
                if (((Contact)contacts.elementAt(i)).isVisibleInContactList()) {
                    return false;
                }
            }
            return true;
        }
        return (0 == protocol.getContactItems().size())
                && (0 == protocol.getGroupItems().size());
    }

    public String getText() {
        return protocol.getUserId();
    }

    public int getNodeWeight() {
        return 0;
    }
    public void sort() {
        synchronized (protocol.getRosterLockObject()) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                Util.sort(protocol.getSortedGroups());
            } else {
                Util.sort(protocol.getSortedContacts());
            }
        }
    }
    public final void getLeftIcons(Icon[] leftIcons) {
        protocol.getCapIcons(leftIcons);
    }
    public final void getRightIcons(Icon[] rightIcons) {
        if (!isExpanded()) {
            rightIcons[0] = ChatHistory.instance.getUnreadMessageIcon(protocol);
        }
    }
}
// #sijapp cond.end #