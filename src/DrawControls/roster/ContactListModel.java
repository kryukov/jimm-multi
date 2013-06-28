/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/DrawControls/VirtualTree.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package DrawControls.roster;

import jimm.Options;
import jimm.comm.Util;
import protocol.*;

import java.util.Vector;

public abstract class ContactListModel {
    private final Vector protocolList = new Vector();

    protected TreeNode selectedItem = null;

    protected boolean hideOffline;

    public void removeAllProtocols() {
        protocolList.removeAllElements();
    }
    public void addProtocol(Protocol prot) {
        protocolList.addElement(prot);
    }
    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }

    public final Protocol getProtocol(int accountIndex) {
        return (Protocol) protocolList.elementAt(accountIndex);
    }
    public final int getProtocolCount() {
        return protocolList.size();
    }


    void updateOptions() {
        hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }

    Protocol getContactProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).inContactList(c)) {
                return getProtocol(i);
            }
        }
        return null;
    }
    protected final Protocol getProtocol(Group g) {
        // #sijapp cond.if modules_MULTI is "true" #
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (-1 < Util.getIndex(getProtocol(i).getGroupItems(), g)) {
                return getProtocol(i);
            }
            if (getProtocol(i).getNotInListGroup() == g) {
                return getProtocol(i);
            }
        }
        // #sijapp cond.else #
        if (true) return getProtocol(0);
        // #sijapp cond.end #
        return null;
    }

    public abstract void buildFlatItems(Vector items);

    public abstract void updateGroupOrder(Updater.Update u);

    public void removeFromGroup(Protocol protocol, Group group, Contact c) {
        GroupBranch groupBranch = getGroupNode(protocol, group);
        if (null == groupBranch) return;
        if (groupBranch.getContacts().removeElement(c)) {
            groupBranch.updateGroupData();
        }
    }

    public void addToGroup(Protocol protocol, Group group, Contact contact) {
        GroupBranch gb = getGroupNode(protocol, group);
        if (null == gb) return;
        gb.getContacts().addElement(contact);
    }

    public void updateGroupData(Protocol protocol, Group group) {
        GroupBranch gb = getGroupNode(protocol, group);
        if (null == gb) return;
        gb.updateGroupData();
    }

    protected GroupBranch createGroup(Group g) {
        GroupBranch group = new GroupBranch(g.getName());
        group.setMode(g.getMode());
        return group;
    }
    protected final void rebuildGroup(GroupBranch g, boolean show, Vector drawItems) {
        if (show || isNotEmpty(g.getContacts())) {
            drawItems.addElement(g);
            if (g.isExpanded()) {
                rebuildContacts(g.getContacts(), drawItems);
            }
        }
    }
    private boolean isNotEmpty(Vector contacts) {
        Contact c;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (!hideOffline || c.isVisibleInContactList() || (c == selectedItem)) {
                return true;
            }
        }
        return false;
    }
    protected final void rebuildContacts(Vector contacts, Vector drawItems) {
        Contact c;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (!hideOffline || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    public abstract void addGroup(Protocol protocol, Group group);
    public abstract void removeGroup(Protocol protocol, Group group);

    public abstract GroupBranch getGroupNode(Protocol protocol, Group group);

    public boolean hasProtocol(Protocol p) {
        return -1 < Util.getIndex(protocolList, p);
    }
}