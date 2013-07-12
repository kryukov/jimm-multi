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

import jimm.util.JLocale;
import jimmui.view.icons.Icon;
import jimmui.view.text.*;
import java.util.Vector;
import javax.microedition.lcdui.*;

import jimm.*;
import jimm.chat.*;
import jimm.comm.*;
import jimm.modules.*;
import jimmui.view.base.*;
import protocol.*;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class VirtualContactList extends VirtualList {

    private Vector drawItems = new Vector();
    private int itemHeight = 0;
    private ContactListListener clListener;
    private Icon[] leftIcons = new Icon[5];
    private Icon[] rightIcons = new Icon[1];
    private int stepSize;
    private int nodeRectHeight;
    private boolean showStatusLine;
    private ContactListModel model;
    // #sijapp cond.if modules_MULTI isnot "true" #
    private Icon[] capIcons = new Icon[2];
    // #sijapp cond.end #
    private Par textMessage = null;

    private TreeNode currentNode = null;
    private boolean rebuildList = false;

    private Vector[] listOfContactList = new Vector[]{new Vector(), new Vector()};
    private int visibleListIndex = 0;
    private Updater updater = new Updater();

    public VirtualContactList() {
        super("");
        model = updater.createModel();
        // #sijapp cond.if modules_MULTI is "true" #
        // #sijapp cond.if modules_TOUCH is "true"#
        softBar = new RosterToolBar();
        // #sijapp cond.end #
        // #sijapp cond.end #
        updateOption();
    }
    public ContactListModel getModel() {
        return model;
    }
    public void setCLListener(ContactListListener listener) {
        clListener = listener;
    }

    public void setModel(ContactListModel model) {
        this.model = model;
        updateTitle();
        update();
        invalidate();
    }

    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    private void updateItemHeight() {
        int iconHeight = 0;
        if (0 < getModel().getProtocolCount()) {
            Protocol p = getModel().getProtocol(0);
            Icon icon = InfoFactory.factory.getStatusInfo(p).getIcon(StatusInfo.STATUS_ONLINE);
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

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            boolean isTrue = fromX < toX;
            int currentModel = 0;
            if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) currentModel = 1;
            if (model == updater.getChatModel()) currentModel = 2;
            currentModel = (currentModel + 3 + (isTrue ? -1 : +1)) % 3;
            switch (currentModel) {
                case 0:
                    model = updater.getModel();
                    Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
                    model.hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                    Options.safeSave();
                    break;
                case 1:
                    model = updater.getModel();
                    Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, true);
                    model.hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                    Options.safeSave();
                    break;
                case 2:
                    model = updater.getChatModel();
                    break;
            }
            jimm.cl.ContactList.getInstance().activate();
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
        setTopByOffset(getTopOffset());
        setSoftBarLabels("menu", "context_menu", "context_menu", false);
        setFontSet(GraphicsEx.contactListFontSet);
        rebuildList = true;
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

    protected void beforePaint() {
        if (rebuildList) {
            updater.updateTree();
            rebuildList = false;
            try {
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

    public void updateOption() {
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
        stepSize = Math.max(getFontSet()[FONT_STYLE_PLAIN].getHeight() / 4, 2);
        model.hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }

    /**
     * Build path to node int tree.
     */
    private void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && model.hasGroups()) {
            Contact c = (Contact)node;
            Protocol p = model.getContactProtocol(c);
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                model.expandPath(new Updater.Update(p, group, c, Updater.Update.EXPAND));
            }
        }
    }


    private void buildFlatItems(Vector items) {
        items.removeAllElements();
        model.buildFlatItems(items);
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
            ((Contact)item).activate(model.getContactProtocol((Contact)item));

        } else if (item instanceof GroupBranch) {
            GroupBranch group = (GroupBranch)item;
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

    //Updates the title of the list
    public void updateTitle() {
        String text = "";
        // #sijapp cond.if modules_MULTI isnot "true" #
        Protocol protocol = getModel().getProtocol(0);
        capIcons[0] = null;
        capIcons[1] = null;
        if (null != protocol) {
            if (protocol.isConnected() && !protocol.isConnecting()) {
                capIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(protocol.getProfile().statusIndex);
            } else {
                capIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(StatusInfo.STATUS_OFFLINE);
            }
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (null != InfoFactory.factory.getXStatusInfo(protocol)) {
                capIcons[1] = InfoFactory.factory.getXStatusInfo(protocol).getIcon(protocol.getProfile().xstatusIndex);
            }
            // #sijapp cond.end #
        }
        bar.setImages(capIcons);
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
        if (model == updater.getChatModel()) {
            text = JLocale.getString("chats");
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
                if (getModel() == getUpdater().getChatModel()) {
                    setModel(getUpdater().getModel());
                    back();
                } else {
                    showContextMenu(getCurrentNode());
                }
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
                        ChatModel chat = ChatHistory.instance.getChatModel((Contact)item);
                        ChatHistory.instance.getUpdater().removeReadMessages(chat);
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
        if (node instanceof Contact) {
            return model.getContactProtocol((Contact) node);
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
            int height = getHeight() - top_y;
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
        if (model.hasGroups()) {
            if (node instanceof GroupBranch) {
                g.setThemeColor(CanvasEx.THEME_GROUP);
                g.setFont(getFontSet()[FONT_STYLE_PLAIN]);

                drawNodeRect(g, (GroupBranch)node, x, y1, y1 + h);
                x += nodeRectHeight + 2;
                g.drawString(leftIcons, node.getText(), rightIcons, x, y1, w + x1 - x, h);
                return;
            }
            x += stepSize;
        }

        Contact c = (Contact)node;
        g.setThemeColor(c.getTextTheme());
        if (showStatusLine) {
            drawContact(g, c, x, y1, w + x1 - x, h);
        } else {
            g.setFont(getFontSet()[c.hasChat() ? FONT_STYLE_BOLD : FONT_STYLE_PLAIN]);
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

        Font contactFont = getFontSet()[c.hasChat() ? FONT_STYLE_BOLD : FONT_STYLE_PLAIN];
        Font statusFont = GraphicsEx.statusLineFont;
        int statusHeight = statusFont.getHeight();

        y += (h - (contactFont.getHeight() + statusHeight)) / 2;

        g.setFont(contactFont);
        g.drawString(c.getName(), x, y, Graphics.LEFT + Graphics.TOP);

        g.setFont(statusFont);
        g.setThemeColor(THEME_CONTACT_STATUS);
        g.drawString(getStatusMessage(c), x, y + statusHeight, Graphics.LEFT + Graphics.TOP);
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
            message = InfoFactory.factory.getXStatusInfo(protocol).getName(contact.getXStatusIndex());
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }
        // #sijapp cond.end #
        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        return InfoFactory.factory.getStatusInfo(protocol).getName(contact.getStatusIndex());
    }

    public Updater getUpdater() {
        return updater;
    }
}
