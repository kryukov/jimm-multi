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

package jimmui.view.roster;

import jimm.Options;
import jimmui.updater.RosterUpdater;
import jimmui.view.roster.items.GroupBranch;
import jimmui.view.roster.items.ProtocolBranch;
import jimmui.view.roster.items.TreeNode;
import protocol.*;

import java.util.Vector;

public abstract class ContactListModel {
    private Protocol[] protocols;

    protected TreeNode selectedItem = null;

    protected boolean hideOffline;

    public void addProtocols(Vector<Protocol> protocols) {
        this.protocols = new Protocol[protocols.size()];
        protocols.copyInto(this.protocols);
        for (Protocol protocol : this.protocols) {
            updateProtocol(protocol, null);
        }
    }
    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }

    public final Protocol getProtocol(int accountIndex) {
        return protocols[accountIndex];
    }
    public final int getProtocolCount() {
        return protocols.length;
    }


    public ContactListModel() {
        hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }

    Protocol getContactProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).hasContact(c)) {
                return getProtocol(i);
            }
        }
        return null;
    }

    public abstract void buildFlatItems(Vector<TreeNode> items);

    public abstract void updateOrder(RosterUpdater.Update u);

    public void removeFromGroup(RosterUpdater.Update update) {
        GroupBranch groupBranch = getGroupNode(update);
        if (null == groupBranch) return;
        if (groupBranch.getContacts().removeElement(update.contact)) {
            groupBranch.updateGroupData();
        }
    }

    public void addToGroup(RosterUpdater.Update update) {
        GroupBranch gb = getGroupNode(update);
        if (null == gb) return;
        gb.getContacts().addElement(update.contact);
    }

    protected GroupBranch createGroup(Group g) {
        GroupBranch group = new GroupBranch(g.getName());
        group.setMode(g.getMode());
        return group;
    }
    protected final void rebuildGroup(GroupBranch g, boolean show, Vector<TreeNode> drawItems) {
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
    protected final void rebuildContacts(Vector contacts, Vector<TreeNode> drawItems) {
        Contact c;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (!hideOffline || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    public abstract void updateProtocol(Protocol protocol, Roster oldRoster);

    public abstract void addGroup(RosterUpdater.Update u);
    public abstract void removeGroup(RosterUpdater.Update u);

    public abstract GroupBranch getGroupNode(RosterUpdater.Update u);
    public abstract ProtocolBranch getProtocolNode(RosterUpdater.Update u);

    public boolean hasProtocol(Protocol p) {
        for (Protocol protocol : this.protocols) {
            if (p == protocol) return true;
        }
        return false;
    }

    public void expandPath(RosterUpdater.Update update) {
        GroupBranch gb = getGroupNode(update);
        if (null != gb) gb.setExpandFlag(true);
    }

    protected final void addGroupContacts() {

    }

    public abstract boolean hasGroups();
}