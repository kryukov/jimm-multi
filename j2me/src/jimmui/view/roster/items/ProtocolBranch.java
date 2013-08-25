/*
 * ProtocolBranch.java
 *
 * Created on 24 Март 2010 г., 15:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.roster.items;

import jimm.Jimm;
import jimmui.view.icons.Icon;
import java.util.Vector;
import jimm.Options;
import jimm.comm.Util;
import jimm.util.JLocale;
import protocol.*;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class ProtocolBranch implements TreeBranch {
    private Protocol protocol;
    private Vector<GroupBranch> items = new Vector<GroupBranch>();
    private Vector<Contact> sortedContacts = new Vector<Contact>();
    private GroupBranch notInListGroup;
    private boolean expanded = false;

    public ProtocolBranch(Protocol p) {
        protocol = p;
        setExpandFlag(false);
        // Not In List Group
        notInListGroup = new GroupBranch(JLocale.getString("group_not_in_list"));
        notInListGroup.setMode(Group.MODE_NONE);
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

    public void sort() {
        synchronized (protocol.getRosterLockObject()) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                Util.sort(items);
            } else {
                Util.sort(sortedContacts);
            }
        }
    }
    public final void getLeftIcons(Icon[] leftIcons) {
        if (protocol.isConnected() && !protocol.isConnecting()) {
            leftIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(protocol.getProfile().statusIndex);
        } else {
            leftIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(StatusInfo.STATUS_OFFLINE);
        }
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (null != InfoFactory.factory.getXStatusInfo(protocol)) {
            leftIcons[1] = InfoFactory.factory.getXStatusInfo(protocol).getIcon(protocol.getProfile().xstatusIndex);
        }
        // #sijapp cond.end #
    }
    public final void getRightIcons(Icon[] rightIcons) {
        if (!isExpanded()) {
            rightIcons[0] = Jimm.getJimm().getCL().getUnreadMessageIcon(protocol);
        }
    }

    public GroupBranch getGroupNode(Group group) {
        if (null == group) {
            return notInListGroup;
        }
        String name = group.getName();
        GroupBranch g;
        for (int i = 0; i < items.size(); ++i) {
            g = (GroupBranch) items.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    public void removeGroup(Group group) {
        items.removeElement(getGroupNode(group));
    }


    public GroupBranch getNotInListGroup() {
        return notInListGroup;
    }

    public Vector<GroupBranch> getGroups() {
        return items;
    }
    public final Vector<Contact> getSortedContacts() {
        return sortedContacts;
    }
    public final boolean isExpanded() {
        return expanded;
    }
    public final void setExpandFlag(boolean value) {
        expanded = value;
        if (expanded) sort();
    }
}
