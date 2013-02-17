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

package DrawControls.tree;

import DrawControls.icons.Icon;
import DrawControls.text.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.base.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class VirtualContactList extends ScrollableArea {

    private Vector drawItems = new Vector();
    private int itemHeight = 0;
    private ContactListListener clListener;
    private Icon[] leftIcons = new Icon[5];
    private Icon[] rightIcons = new Icon[1];
    private int stepSize;
    private int nodeRectHeight;
    private boolean useGroups;
    private boolean showStatusLine;
    private boolean hideOffline;
    private ContactListModel model;
    // #sijapp cond.if modules_MULTI isnot "true" #
    private Icon[] capIcons = new Icon[2];
    // #sijapp cond.end #
    private Par textMessage = null;

    private TreeNode currentNode = null;
    private TreeNode selectedItem = null;
    private boolean rebuildList = false;

    private Vector updateQueue = new Vector();

    private Vector[] listOfContactList = new Vector[]{new Vector(), new Vector()};
    private int visibleListIndex = 0;

    public VirtualContactList() {
        super("");
        // #sijapp cond.if modules_MULTI is "true" #
        model = new ContactListModel(10);
        // #sijapp cond.else #
        model = new ContactListModel(1);
        // #sijapp cond.end #
        updateOption();
    }
    public ContactListModel getModel() {
        return model;
    }
    public void setCLListener(ContactListListener listener) {
        clListener = listener;
    }

    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    private void updateItemHeight() {
        int iconHeight = 0;
        if (0 < getModel().getProtocolCount()) {
            Protocol p = getModel().getProtocol(0);
            Icon icon = p.getStatusInfo().getIcon(StatusInfo.STATUS_ONLINE);
            iconHeight = (null == icon) ? 0 : (icon.getHeight() + 2);
        }
        int fontHeight = getFontSet()[FONT_STYLE_PLAIN].getHeight();
        if (showStatusLine) {
            fontHeight += GraphicsEx.statusLineFont.getHeight();
        }
        itemHeight = Math.max(Math.max(iconHeight, fontHeight), CanvasEx.minItemHeight);
        nodeRectHeight = Math.max(itemHeight / 2, 7);
        if (0 == (nodeRectHeight & 1)) {
            nodeRectHeight--;
        }
    }

    // #sijapp cond.if modules_MULTI isnot "true" #
    protected final void drawProgress(GraphicsEx g, int width, int height) {
        if (0 < model.getProtocolCount()) {
            int x = width * model.getProtocol(0).getConnectingProgress() / 100;
            g.setThemeColor(THEME_BACKGROUND);
            g.fillRect(x, 0, width - x, height - 1);
        }
    }
    // #sijapp cond.end #

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            boolean isTrue = fromX < toX;
            if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE) != isTrue) {
                Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, isTrue);
                Options.safeSave();
                jimm.cl.ContactList.getInstance().activate();
            }
        }
    }
    protected void touchItemTaped(int item, int x, boolean isLong) {
        int itemHeight = getItemHeight(item);
        TreeNode currentNode = getSafeNode(item);
        if (isLong || (getWidth() - itemHeight < x)) {
            showContextMenu(currentNode);
        } else if (NativeCanvas.getInstance().touchControl.isSecondTap) {
            itemSelected(currentNode);
        }
    }
    // #sijapp cond.end#

    //! For internal use only
    protected final int getSize() {
        return drawItems.size();
    }


    protected void restoring() {
        setSoftBarLabels("menu", "context_menu", "context_menu", false);
        setFontSet(GraphicsEx.contactListFontSet);
        rebuildList = true;
    }
    public void update(TreeNode node) {
        // TODO: update contact only if group and protocol is expanded
        // TODO: update group only if protocol is expanded
        rebuildList = true;
        //invalidate();
    }
    public final void update() {
        rebuildList = true;
        //invalidate();
    }
    private void updateTree() {
        rebuildList = true;
        invalidate();
    }
    protected void updateTask(long microTime) {
        if (rebuildList) {
            invalidate();
        }
    }

    public void putIntoQueue(Group g) {
        if (-1 == Util.getIndex(updateQueue, g)) {
            updateQueue.addElement(g);
        }
    }
    protected void beforePaint() {
        if (rebuildList) {
            while (!updateQueue.isEmpty()) {
                Group group = (Group)updateQueue.firstElement();
                updateQueue.removeElementAt(0);
                if (useGroups) {
                    group.updateGroupData();
                    group.sort();
                } else {
                    Util.sort(getProtocol(group).getSortedContacts());
                }
            }
            rebuildList = false;
            try {
                updateOption();
                buildList();
                updateTitle();
                updateItemHeight();
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("update ", e);
                // #sijapp cond.end #
            }
        }
    }
    private Protocol getProtocol(Group g) {
        for (int i = 0; i < getModel().getProtocolCount(); ++i) {
            Protocol p = getModel().getProtocol(i);
            if (-1 != Util.getIndex(p.getGroupItems(), g)) {
                return p;
            }
        }
        return getModel().getProtocol(0);
    }

    /**
     * Tree control call this function for request of data
     * for tree node to be drawn
     */
    private TreeNode getDrawItem(int index) {
        return (TreeNode) drawItems.elementAt(index);
    }

    /** Returns current selected node */
    private TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }
    private TreeNode getSafeNode(int index) {
        if ((index < drawItems.size()) && (index >= 0)) {
            return getDrawItem(index);
        }
        return null;
    }

    private void updateOption() {
        boolean groups = useGroups;
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
        hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        stepSize = Math.max(getFontSet()[FONT_STYLE_PLAIN].getHeight() / 4, 2);
        if (groups && !useGroups) {
            for (int i = 0; i < model.getProtocolCount(); ++i) {
                Protocol p = model.getProtocol(i);
                Util.sort(p.getSortedContacts());
            }
        }
    }
    private Protocol getContactProtocol(Contact c) {
        for (int i = 0; i < model.getProtocolCount(); ++i) {
            if (model.getProtocol(i).inContactList(c)) {
                return model.getProtocol(i);
            }
        }
        return null;
    }
    /**
     * Build path to node int tree.
     */
    private void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && useGroups) {
            Contact c = (Contact)node;
            Protocol p = getContactProtocol(c);
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                if (null == group) {
                    group = p.getNotInListGroup();
                }
                group.setExpandFlag(true);
            }
        }
    }

    private void rebuildFlatItemsWG(Protocol p, boolean onlineOnly, Vector drawItems) {
        Vector contacts;
        Group g;
        Contact c;
        int contactCounter;
        boolean all = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        Vector groups = p.getSortedGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (Group)groups.elementAt(groupIndex);
            contactCounter = 0;
            drawItems.addElement(g);
            contacts = g.getContacts();
            for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
                c = (Contact)contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (g.isExpanded()) {
                        drawItems.addElement(c);
                    }
                    contactCounter++;
                }
            }
            if (onlineOnly && (0 == contactCounter)) {
                drawItems.removeElementAt(drawItems.size() - 1);
            }
        }

        g = p.getNotInListGroup();
        drawItems.addElement(g);
        contacts = g.getContacts();
        contactCounter = 0;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (g.isExpanded()) {
                    drawItems.addElement(c);
                }
                contactCounter++;
            }
        }
        if (0 == contactCounter) {
            drawItems.removeElementAt(drawItems.size() - 1);
        }
    }
    private void rebuildFlatItemsWOG(Protocol p, Vector drawItems) {
        boolean all = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        Contact c;
        Vector contacts = p.getSortedContacts();
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact)contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    private void buildFlatItems(Vector items) {
        items.removeAllElements();
        final int count = model.getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = model.getProtocol(i);
            // #sijapp cond.if modules_MULTI is "true" #
            ProtocolBranch root = p.getProtocolBranch();
            items.addElement(root);
            if (!root.isExpanded()) continue;
            // #sijapp cond.end #
            synchronized (p.getRosterLockObject()) {
                if (useGroups) {
                    rebuildFlatItemsWG(p, hideOffline, items);
                } else {
                    rebuildFlatItemsWOG(p, items);
                }
            }
        }
    }

    private void buildList() {
        TreeNode current = currentNode;
        currentNode = null;
        int prevIndex = getCurrItem();
        if (null != current) {
            expandNodePath(current);
        } else {
            current = getSafeNode(prevIndex);
        }
        visibleListIndex ^= 1;
        Vector items = listOfContactList[visibleListIndex];
        buildFlatItems(items);
        drawItems = items;
        if (null != current) {
            int currentIndex = Util.getIndex(items, current);
            if ((prevIndex != currentIndex) && (-1 != currentIndex)) {
                setCurrentItemIndex(currentIndex);
            }
        }
        if (items.size() <= getCurrItem()) {
            setCurrentItemIndex(0);
        }
        //if (0 == model.getProtocolCount()) {
        //    setMessage(ResourceBundle.getString("No accounts"));
        //}
        listOfContactList[visibleListIndex ^ 1].removeAllElements();
    }

    /**
     * Set node as current. Make autoscroll if needs.
     */
    private void setCurrentNode(TreeNode node) {
        if (null != node) {
            currentNode = node;
        }
    }
    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }
    /**
     * Expand or collapse tree node.
     * NOTE: this is not recursive operation!
     */
    private void setExpandFlag(TreeBranch node, boolean value) {
        setCurrentNode(getCurrentNode());
        node.setExpandFlag(value);
        updateTree();
    }
    private void itemSelected(TreeNode item) {
        if (null == item) {
            return;
        }
        if (item instanceof Contact) {
            ((Contact)item).activate(getContactProtocol((Contact)item));

        } else if (item instanceof Group) {
            Group group = (Group)item;
            setExpandFlag(group, !group.isExpanded());

        } else if (item instanceof TreeBranch) {
            TreeBranch root = (TreeBranch)item;
            setExpandFlag(root, !root.isExpanded());
        }
    }

    public final void setActiveContact(Contact cItem) {
        setCurrentNode(cItem);
        updateTree();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected final void touchCaptionTapped(boolean icon) {
        jimm.chat.ChatHistory.instance.showChatList(icon);
    }
    // #sijapp cond.end#

    //Updates the title of the list
    public void updateTitle() {
        String text = "";
        // #sijapp cond.if modules_MULTI isnot "true" #
        Protocol protocol = getModel().getProtocol(0);
        capIcons[0] = null;
        capIcons[1] = null;
        if (null != protocol) {
            protocol.getCapIcons(capIcons);
        }
        setCapImages(capIcons);
        // #sijapp cond.end #

        // #sijapp cond.if modules_TRAFFIC is "true" #
        int traffic = Traffic.getInstance().getSessionTraffic();
        if (1024 <= traffic) {
            text = StringConvertor.bytesToSizeString(traffic, false);
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_MULTI isnot "true" #
        if (StringConvertor.isEmpty(text) && (null != protocol)) {
            text = protocol.getUserId();
        }
        // #sijapp cond.end#
        if (StringConvertor.isEmpty(text)) {
            // #sijapp cond.if modules_MULTI is "true" #
            text = "Jimm Multi";
            // #sijapp cond.elseif protocols_OBIMP is "true" #
            text = "Bimoid Mobile";
            // #sijapp cond.else #
            text = "Jimm aspro";
            // #sijapp cond.end #
        }
        setCaption(text);
    }


    protected void doJimmAction(int keyCode) {
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                itemSelected(getCurrentNode());
                return;

            case NativeCanvas.JIMM_MENU:
                clListener.activateMainMenu();
                return;

            case NativeCanvas.JIMM_BACK:
                showContextMenu(getCurrentNode());
                return;
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        TreeNode item = getCurrentNode();
        Contact current = (item instanceof Contact) ? (Contact)item : null;
        if (CanvasEx.KEY_PRESSED == type) {
            clListener.setCurrentContact(current);
            switch (keyCode) {
                case NativeCanvas.CLEAR_KEY:
                    if ((item instanceof Contact) && ((Contact)item).hasChat()) {
                        Chat chat = ChatHistory.instance.getChat((Contact)item);
                        chat.removeReadMessages();
                    }
                    return;
            }
        }

        Protocol p = getProtocol(item);
        if (JimmUI.execHotKey(p, current, keyCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private void showContextMenu(TreeNode item) {
        Protocol p = getProtocol(item);
        if ((null == item) || p.isConnecting()) {
            // #sijapp cond.if modules_MULTI is "true"#
            if (!(item instanceof ProtocolBranch)) {
                return;
            }
            // #sijapp cond.else#
            return;
            // #sijapp cond.end#
        }
        showMenu(this.clListener.getContextMenu(p, item));
    }

    private Protocol getProtocol(TreeNode node) {
        // #sijapp cond.if modules_MULTI is "true" #
        if (node instanceof ProtocolBranch) {
            return ((ProtocolBranch)node).getProtocol();
        }

        Protocol last = null;
        for (int i = 0; i < drawItems.size(); ++i) {
            if (drawItems.elementAt(i) instanceof ProtocolBranch) {
                last = ((ProtocolBranch)drawItems.elementAt(i)).getProtocol();

            } else if (drawItems.elementAt(i) == node) {
                return last;
            }
        }
        // #sijapp cond.end #
        return model.getProtocol(0);
    }
    public final Protocol getCurrentProtocol() {
        Protocol protocol = getProtocol(getCurrentNode());
        if ((null != protocol) && (null == protocol.getProfile())) {
            protocol = model.getProtocol(0);
        }
        return protocol;
    }


    /** draw + or - before node text */
    private void drawNodeRect(GraphicsEx g, TreeBranch branch,
            int x, int y1, int y2) {

        int height = nodeRectHeight;
        final int half = (height + 1) / 2;
        final int quarter = (half + 1) / 2;
        int y = (y1 + y2 - height) / 2;
        if (branch.isEmpty()) {
            x += quarter;
            g.drawLine(x, y, x, y + height);
            while (0 < height) {
                g.drawLine(x, y, x, y + 1);
                g.drawLine(x, y + height - 1, x, y + height);
                height -= 2;
                y += 1;
                x += 1;
            }
            return;
        }
        if (branch.isExpanded()) {
            y += quarter;
            while (0 < height) {
                g.drawLine(x, y, x + height, y);
                height -= 2;
                y += 1;
                x += 1;
            }

        } else {
            x += quarter;
            while (0 < height) {
                g.drawLine(x, y, x, y + height);
                height -= 2;
                y += 1;
                x += 1;
            }
        }
    }

    private void setMessage(String text) {
        Parser parser = new Parser(getFontSet(), getWidth() * 8 / 10);
        parser.addText(text, CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
        textMessage = parser.getPar();
        invalidate();
    }
    protected void drawEmptyItems(GraphicsEx g, int top_y) {
        if (null != textMessage) {
            int height = getScreenHeight() - top_y;
            textMessage.paint(getFontSet(), g, getWidth() / 10,
                    top_y + (height - textMessage.getHeight()) / 2,
                    0, textMessage.getHeight());
        }
    }
    // #sijapp cond.if modules_MULTI is "true" #
    protected void drawItemBack(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        TreeNode node = getDrawItem(index);
        if (node instanceof ProtocolBranch) {
            final int x = x1;
            if (getCurrItem() == index) {
                g.setThemeColor(THEME_SELECTION_BACK, CanvasEx.THEME_PROTOCOL_BACK, 0xA0);
            } else {
                g.setThemeColor(CanvasEx.THEME_PROTOCOL_BACK);
            }
            byte progress = ((ProtocolBranch)node).getProtocol().getConnectingProgress();
            int width = w + x - x1 + 4;
            if (progress < 100) {
                width = width * progress / 100;
            }
            g.fillRect(x - 2, y1, width, h);
        }
    }
    // #sijapp cond.end #
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        TreeNode node = getDrawItem(index);

        for (int i = 0; i < leftIcons.length; ++i) {
            leftIcons[i] = null;
        }
        rightIcons[0] = null;
        node.getLeftIcons(leftIcons);
        node.getRightIcons(rightIcons);

        int x = x1;
        // #sijapp cond.if modules_MULTI is "true" #
        if (node instanceof ProtocolBranch) {
            g.setThemeColor(CanvasEx.THEME_PROTOCOL);
            g.setFont(getFontSet()[FONT_STYLE_PLAIN]);
            drawNodeRect(g, (TreeBranch)node, x, y1, y1 + h);
            x += nodeRectHeight + 2;
            g.drawString(leftIcons, node.getText(), rightIcons, x, y1, w + x1 - x, h);
            return;
        }
        // #sijapp cond.end #
        if (useGroups) {
            if (node instanceof Group) {
                g.setThemeColor(CanvasEx.THEME_GROUP);
                g.setFont(getFontSet()[FONT_STYLE_PLAIN]);

                drawNodeRect(g, (TreeBranch)node, x, y1, y1 + h);
                x += nodeRectHeight + 2;
                g.drawString(leftIcons, node.getText(), rightIcons, x, y1, w + x1 - x, h);
                return;
            }
            x += stepSize;
        }

        Contact c = (Contact)node;
        g.setThemeColor(c.getTextTheme());
        g.setFont(getFontSet()[c.hasChat() ? FONT_STYLE_BOLD : FONT_STYLE_PLAIN]);
        if (showStatusLine) {
            drawContact(g, c, x, y1, w + x1 - x, h);
        } else {
            g.drawString(leftIcons, c.getName(), rightIcons, x, y1, w + x1 - x, h);
        }
    }

    private void drawContact(GraphicsEx g, Contact c, int x, int y, int w, int h) {
        int lWidth = g.drawImages(leftIcons, x, y, h);
        if (lWidth > 0) {
            lWidth++;
        }
        int rWidth = g.getImagesWidth(rightIcons);
        if (rWidth > 0) {
            rWidth++;
        }
        g.drawImages(rightIcons, x + w - rWidth, y, h);

        w -= lWidth + rWidth;
        x += lWidth;
        g.setClip(x, y, w, h);

        g.drawString(c.getName(), x, y, Graphics.LEFT + Graphics.TOP);

        Font f = GraphicsEx.statusLineFont;
        g.setFont(f);
        g.setThemeColor(THEME_CONTACT_STATUS);
        int fh = f.getHeight();
        g.drawString(getStatusMessage(c), x, y + fh, Graphics.LEFT + Graphics.TOP);
    }
    private String getStatusMessage(Contact contact) {
        String message;
        Protocol protocol = getProtocol(contact);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            message = protocol.getXStatusInfo().getName(contact.getXStatusIndex());
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }
        // #sijapp cond.end #
        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        return protocol.getStatusInfo().getName(contact.getStatusIndex());
    }
}
