package jimmui.view.roster;

import jimm.Jimm;
import jimm.Options;
import jimm.comm.StringUtils;
import jimm.comm.Util;
import jimm.modules.*;
import jimmui.HotKeys;
import jimmui.model.chat.ChatModel;
import jimmui.updater.RosterUpdater;
import jimmui.view.base.*;
import jimmui.view.base.touch.*;
import jimmui.view.icons.Icon;
import jimmui.view.roster.items.*;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.ui.XStatusInfo;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 24.07.13 20:13
 *
 * @author vladimir
 */
public class RosterContent extends SomeContent {
    private Vector drawItems = new Vector();
    private int itemHeight = 0;
    private ContactListListener clListener;
    private Icon[] leftIcons = new Icon[5];
    private Icon[] rightIcons = new Icon[1];
    private int stepSize;
    private int nodeRectHeight;
    private boolean showStatusLine;
    private ContactListModel model;
    private RosterUpdater updater = new RosterUpdater();
    private boolean rebuildList;
    private TreeNode currentNode = null;
    private Vector<TreeNode>[] listOfContactList = new Vector[]{new Vector<TreeNode>(), new Vector<TreeNode>()};
    private int visibleListIndex = 0;

    public RosterContent(SomeContentList view) {
        super(view);
    }

    @Override
    public int getSize() {
        return drawItems.size();
    }

    @Override
    public int getItemHeight(int itemIndex) {
        return itemHeight;
    }

    @Override
    public void doJimmAction(int keyCode) {
        TreeNode item = getCurrentNode();
        updater.setCurrentContact((item instanceof Contact) ? (Contact)item : null);
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                itemSelected(getCurrentNode());
                break;

            case NativeCanvas.JIMM_MENU:
                clListener.activateMainMenu();
                break;

            case NativeCanvas.JIMM_BACK:
                if (getModel() == getUpdater().getChatModel()) {
                    ((VirtualContactList)view).setModel(getUpdater().getModel());
                    view.back();

                } else {
                    showContextMenu(getCurrentNode());
                }
                break;
        }
    }

    @Override
    protected boolean doKeyReaction(int keyCode, int actionCode, int type) {
        TreeNode item = getCurrentNode();
        Contact current = (item instanceof Contact) ? (Contact)item : null;
        if (CanvasEx.KEY_PRESSED == type) {
            updater.setCurrentContact(current);
            switch (keyCode) {
                case NativeCanvas.CLEAR_KEY:
                    if ((item instanceof Contact) && ((Contact)item).hasChat()) {
                        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel((Contact)item);
                        Jimm.getJimm().getChatUpdater().removeReadMessages(chat);
                    }
                    return true;
            }
        }

        Protocol p = getProtocol(item);
        return HotKeys.execHotKey(p, current, keyCode, type)
                || super.doKeyReaction(keyCode, actionCode, type);
    }


    @Override
    public void drawItemBack(GraphicsEx g, int index, int current, int x, int y, int w, int h, int skip, int to) {
        TreeNode node = getDrawItem(index);
        if (node instanceof ProtocolBranch) {
            if (current == index) {
                g.setThemeColor(CanvasEx.THEME_SELECTION_BACK, CanvasEx.THEME_PROTOCOL_BACK, 0xA0);
            } else {
                g.setThemeColor(CanvasEx.THEME_PROTOCOL_BACK);
            }
            byte progress = ((ProtocolBranch)node).getProtocol().getConnectingProgress();
            int width = w + x - x + 4;
            if (progress < 100) {
                width = width * progress / 100;
            }
            g.fillRect(x - 2, y, width, h);
        }
    }

    @Override
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        TreeNode node = getDrawItem(index);

        for (int i = 0; i < leftIcons.length; ++i) {
            leftIcons[i] = null;
        }
        rightIcons[0] = null;
        node.getLeftIcons(leftIcons);
        node.getRightIcons(rightIcons);

        int x = x1;
        if (node instanceof ProtocolBranch) {
            g.setThemeColor(CanvasEx.THEME_PROTOCOL);
            g.setFont(getFontSet()[CanvasEx.FONT_STYLE_PLAIN]);
            drawNodeRect(g, (TreeBranch)node, x, y1, y1 + h);
            x += nodeRectHeight + 2;
            g.drawString(leftIcons, node.getText(), rightIcons, x, y1, w + x1 - x, h);
            return;
        }
        if (model.hasGroups()) {
            if (node instanceof GroupBranch) {
                g.setThemeColor(CanvasEx.THEME_GROUP);
                g.setFont(getFontSet()[CanvasEx.FONT_STYLE_PLAIN]);

                drawNodeRect(g, (GroupBranch)node, x, y1, y1 + h);
                x += nodeRectHeight + 2;
                g.drawString(leftIcons, node.getText(), rightIcons, x, y1, w + x1 - x, h);
                return;
            }
            x += stepSize;
        }

        drawContact(g, (Contact)node, x, y1, w + x1 - x, h);
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

    private void drawContact(GraphicsEx g, Contact c, int x, int y, int w, int h) {
        g.setThemeColor(getContactTheme(c));
        if (!showStatusLine) {
            g.setFont(getFontSet()[c.hasChat() ? CanvasEx.FONT_STYLE_BOLD : CanvasEx.FONT_STYLE_PLAIN]);
            g.drawString(leftIcons, c.getName(), rightIcons, x, y, w, h);
            return;
        }
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

        Font contactFont = getFontSet()[c.hasChat() ? CanvasEx.FONT_STYLE_BOLD : CanvasEx.FONT_STYLE_PLAIN];
        Font statusFont = GraphicsEx.statusLineFont;
        int statusHeight = statusFont.getHeight();

        y += (h - (contactFont.getHeight() + statusHeight)) / 2;

        g.setFont(contactFont);
        g.drawString(c.getName(), x, y, Graphics.LEFT + Graphics.TOP);

        g.setFont(statusFont);
        g.setThemeColor(CanvasEx.THEME_CONTACT_STATUS);
        g.drawString(getStatusMessage(c), x, y + statusHeight, Graphics.LEFT + Graphics.TOP);
    }

    private byte getContactTheme(Contact c) {
        if (c.isTemp()) {
            return CanvasEx.THEME_CONTACT_TEMP;
        }
        if (c.hasChat()) {
            return CanvasEx.THEME_CONTACT_WITH_CHAT;
        }
        if (c.isOnline()) {
            return CanvasEx.THEME_CONTACT_ONLINE;
        }
        return CanvasEx.THEME_CONTACT_OFFLINE;
    }

    private Font[] getFontSet() {
        return GraphicsEx.contactListFontSet;
    }

    private String getStatusMessage(Contact contact) {
        String message;
        Protocol protocol = getProtocol(contact);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringUtils.isEmpty(message)) {
                return message;
            }
            message = InfoFactory.factory.getXStatusInfo(protocol).getName(contact.getXStatusIndex());
            if (!StringUtils.isEmpty(message)) {
                return message;
            }
        }
        // #sijapp cond.end #
        message = contact.getStatusText();
        if (!StringUtils.isEmpty(message)) {
            return message;
        }
        return InfoFactory.factory.getStatusInfo(protocol).getName(contact.getStatusIndex());
    }


    public ContactListModel getModel() {
        return model;
    }
    public void setCLListener(ContactListListener listener) {
        clListener = listener;
    }

    public void setModel(ContactListModel model) {
        this.model = model;
        update();
    }

    public RosterUpdater getUpdater() {
        return updater;
    }

    private void showContextMenu(TreeNode item) {
        Protocol p = getProtocol(item);
        if ((null == item) || p.isConnecting()) {
            if (!(item instanceof ProtocolBranch)) {
                return;
            }
        }
        view.showMenu(this.clListener.getContextMenu(p, item));
    }

    private Protocol getProtocol(TreeNode node) {
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
        return model.getProtocol(0);
    }
    public final Protocol getCurrentProtocol() {
        Protocol protocol = getProtocol(getCurrentNode());
        if ((null != protocol) && (null == protocol.getProfile())) {
            protocol = model.getProtocol(0);
        }
        return protocol;
    }
    private TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }
    private TreeNode getSafeNode(int index) {
        if ((index < drawItems.size()) && (index >= 0)) {
            return getDrawItem(index);
        }
        return null;
    }
    private TreeNode getDrawItem(int index) {
        return (TreeNode) drawItems.elementAt(index);
    }


    private void updateTree() {
        rebuildList = true;
        invalidate();
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

    // #sijapp cond.if modules_TOUCH is "true"#
    @Override
    protected void touchItemTaped(int item, int x, TouchState state) {
        int itemHeight = getItemHeight(item);
        TreeNode currentNode = getSafeNode(item);
        if (state.isLong || (view.getWidth() - itemHeight < x)) {
            showContextMenu(currentNode);
        } else if (state.isSecondTap) {
            itemSelected(currentNode);
        }
    }
    // #sijapp cond.end#
    public void updateOption() {
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
        stepSize = Math.max(getFontSet()[CanvasEx.FONT_STYLE_PLAIN].getHeight() / 4, 2);
        model.hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }

    public void buildList() {
        TreeNode current = currentNode;
        currentNode = null;
        int prevIndex = getCurrItem();
        if (null != current) {
            expandNodePath(current);
        } else {
            current = getSafeNode(prevIndex);
        }
        visibleListIndex ^= 1;
        Vector<TreeNode> items = listOfContactList[visibleListIndex];
        items.removeAllElements();
        model.buildFlatItems(items);
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
     * Build path to node int tree.
     */
    private void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && model.hasGroups()) {
            Contact c = (Contact)node;
            Protocol p = model.getContactProtocol(c);
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                model.expandPath(new RosterUpdater.Update(p, group, c, RosterUpdater.Update.EXPAND));
            }
        }
    }

    public void setActiveContact(Contact cItem) {
        setCurrentNode(cItem);
        updateTree();
    }

    void updateItemHeight() {
        int iconHeight = 0;
        if (0 < getModel().getProtocolCount()) {
            Protocol p = getModel().getProtocol(0);
            Icon icon = InfoFactory.factory.getStatusInfo(p).getIcon(StatusInfo.STATUS_ONLINE);
            iconHeight = (null == icon) ? 0 : (icon.getHeight() + 2);
        }
        int fontHeight = GraphicsEx.contactListFontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight();
        if (showStatusLine) {
            fontHeight += GraphicsEx.statusLineFont.getHeight();
        }
        itemHeight = Math.max(Math.max(iconHeight, fontHeight), CanvasEx.minItemHeight);
        nodeRectHeight = Math.max(itemHeight / 2, 7);
        if (0 == (nodeRectHeight & 1)) {
            nodeRectHeight--;
        }
    }

    public void update() {
        rebuildList = true;
    }

    @Override
    protected void updateTask(long microTime) {
        if (rebuildList) {
            invalidate();
        }
    }

    protected void beforePaint() {
        if (rebuildList) {
            rebuildList = false;
            updater.updateTree();
            try {
                buildList();
                updateItemHeight();
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("update ", e);
                // #sijapp cond.end #
            }
        }
    }
}
