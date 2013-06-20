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
    private final Protocol[] protocolList;
    private int count = 0;

    protected TreeNode selectedItem = null;

    protected boolean useGroups;
    protected boolean hideOffline;

    public ContactListModel(int maxCount) {
        protocolList = new Protocol[maxCount];
    }

    public void removeAllProtocols() {
        count = 0;
        for (int i = 0; i < protocolList.length; ++i) {
            protocolList[i] = null;
        }
    }
    public void addProtocol(Protocol prot) {
        if ((count < protocolList.length) && (null != prot)) {
            protocolList[count] = prot;
            count++;
        }
    }
    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }

    public final Protocol getProtocol(int accountIndex) {
        return protocolList[accountIndex];
    }
    public final int getProtocolCount() {
        return count;
    }


    void updateOptions() {
        boolean groups = useGroups;
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        if (groups && !useGroups) {
            sort();
        }
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
    private void sort() {
        for (int i = 0; i < getProtocolCount(); ++i) {
            Util.sort(getProtocol(i).getSortedContacts());
        }
    }

    public void updateGroup(Group group) {
        if (useGroups) {
            GroupBranch groupBranch = getGroupNode(group);
            groupBranch.updateGroupData();
            groupBranch.sort();
        } else {
            Util.sort(getProtocol(group).getSortedContacts());
        }
    }

    public void removeFromGroup(Group group, Contact c) {
        GroupBranch groupBranch = getGroupNode(group);
        if (groupBranch.getContacts().removeElement(c)) {
            groupBranch.updateGroupData();
        }
    }

    public void addToGroup(Group group, Contact contact) {
        getGroupNode(group).getContacts().addElement(contact);
    }

    public void updateGroupData(Group group) {
        GroupBranch groupBranch = getGroupNode(group);
        if (null == groupBranch) return;
        groupBranch.updateGroupData();
    }

    public void updateGroup(Protocol protocol, Group group) {
        Vector allItems = protocol.getContactItems();
        GroupBranch groupBranch = getGroupNode(group);
        if (null == groupBranch) return;
        Vector groupItems = groupBranch.getContacts();
        groupItems.removeAllElements();
        int size = allItems.size();
        int groupId = group.getId();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact)allItems.elementAt(i);
            if (item.getGroupId() == groupId) {
                groupItems.addElement(item);
            }
        }
        groupBranch.updateGroupData();
        groupBranch.sort();
    }

    public abstract GroupBranch getGroupNode(Group group);
}