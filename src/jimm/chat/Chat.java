/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/ChatHistory.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

/*
 * ChatTextList.java
 *
 * Created on 19 Апрель 2007 г., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package jimm.chat;

import jimmui.view.icons.Icon;
import jimm.*;
import jimm.chat.message.*;
import jimm.comm.*;
import jimm.history.*;
import jimmui.view.base.*;
import jimmui.view.menu.*;
import protocol.*;
import protocol.ui.ContactMenu;
import protocol.ui.InfoFactory;
import protocol.ui.MessageEditor;

import javax.microedition.lcdui.Font;

public final class Chat extends VirtualList {
    private static InputTextLine line = new InputTextLine();
    private boolean classic = false;
    private Icon[] statusIcons = new Icon[7];
    private ChatModel model = new ChatModel();
    private static boolean selectMode;
    ///////////////////////////////////////////

    public final int getSize() {
        return model.size();
    }

    ///////////////////////////////////////////
    public Chat(ChatModel model) {
        super(null);
        this.model = model;
        setFontSet(model.fontSet);
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            ChatHistory.instance.getUpdater().storeTopPosition(model, this);
            ChatHistory.instance.showNextPrevChat(model, (fromX > toX));
        }
    }
    // #sijapp cond.end#

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, boolean isLong) {
        if (isLong || (getWidth() - minItemHeight < x)) {
            showMenu(getContextMenu());
        } else if (selectMode) {
            markItem(item);
        } else {
            super.touchItemTaped(item, x, isLong);
        }
    }
    // #sijapp cond.end#

    private void markItem(int item) {
        MessData mData = model.getMessage(item);
        mData.setMarked(!mData.isMarked());
        selectMode = hasSelectedItems();
        invalidate();
    }
    private boolean hasSelectedItems() {
        for (int i = 0; i < model.size(); ++i) {
            MessData md = model.getMessage(i);
            if (md.isMarked()) {
                return true;
            }
        }
        return false;
    }

    private void updateStatusIcons() {
        for (int i = 0; i < statusIcons.length; ++i) {
            statusIcons[i] = null;
        }
        getContact().getLeftIcons(statusIcons);
        bar.setImages(statusIcons);
    }
    public void updateStatus() {
        updateStatusIcons();
        invalidate();
    }

    private byte getInOutColor(boolean incoming) {
        return incoming ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG;
    }
    public static final String ADDRESS = ", ";

    public final void writeMessage(String initText) {
        if (model.writable) {
            if (classic) {
                line.setString(initText);
                line.setVisible(true);
                invalidate();
                restore();
                return;
            }
            // #sijapp cond.if modules_ANDROID is "true" #
            if (true) {
                activate();
                NativeCanvas.getInstance().getInput().setText(initText);
                return;
            }
            // #sijapp cond.end #
            MessageEditor editor = Jimm.getJimm().getMessageEditor();
            if (null != editor) {
                editor.writeMessage(getProtocol(), getContact(), initText);
            }
        }
    }

    public final void writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += ADDRESS;

        } else {
            nick = "";
        }
        writeMessage(nick);
    }

    private String getBlogPostId(String text) {
        if (StringConvertor.isEmpty(text)) {
            return null;
        }
        String lastLine = text.substring(text.lastIndexOf('\n') + 1);
        if (0 == lastLine.length()) {
            return null;
        }
        if ('#' != lastLine.charAt(0)) {
            return null;
        }
        int numEnd = lastLine.indexOf(' ');
        if (-1 != numEnd) {
            lastLine = lastLine.substring(0, numEnd);
        }
        return lastLine + " ";
    }

    Protocol getProtocol() {
        return model.protocol;
    }

    void onMessageSelected() {
        if (selectMode) {
            markItem(getCurrItem());
            return;
        }
        if (getContact().isSingleUserContact()) {
            if (model.isBlogBot()) {
                writeMessage(getBlogPostId(getCurrentText()));
                return;
            }
            writeMessage(null);
            return;
        }
        MessData md = getCurrentMsgData();
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        writeMessageTo(model.getMyName().equals(nick) ? null : nick);
    }

    protected boolean qwertyKey(int keyCode, int type) {
        return classic && line.qwertyKey(this, keyCode, type);
    }

    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        if (classic && line.doKeyReaction(this, keyCode, actionCode, type)) {
            return;
        }
        if (CanvasEx.KEY_PRESSED == type) {
            model.resetUnreadMessages();
            switch (keyCode) {
                case NativeCanvas.CALL_KEY:
                    actionCode = 0;
                    break;

                case NativeCanvas.CLEAR_KEY:
                    execJimmAction(ACTION_DEL_CHAT);
                    return;
            }
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_LEFT:
                case NativeCanvas.NAVIKEY_RIGHT:
                    ChatHistory.instance.getUpdater().storeTopPosition(model, this);
                    ChatHistory.instance.showNextPrevChat(model, NativeCanvas.NAVIKEY_RIGHT == actionCode);
                    return;
            }
        }
        if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
            if (CanvasEx.KEY_RELEASED != type) {
                return;
            }
            if ('5' == keyCode) {
                execJimmAction(NativeCanvas.JIMM_SELECT);
            } else {
                if (NativeCanvas.isLongFirePress()) {
                    markItem(getCurrItem());
                } else {
                    writeMessage(null);
                }
            }
            return;
        }
        if (!JimmUI.execHotKey(getProtocol(), getContact(), keyCode, type)) {
            super.doKeyReaction(keyCode, actionCode, type);
        }
    }

    protected void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_MENU:
                showMenu(getMenu());
                return;

            case NativeCanvas.JIMM_BACK:
                ChatHistory.instance.getUpdater().storeTopPosition(model, this);
                Jimm.getJimm().getCL().activate(getContact());
                return;

            case NativeCanvas.JIMM_SELECT:
                onMessageSelected();
                return;
        }
        if (!model.writable && ((ACTION_REPLY == action)
                || (Contact.USER_MENU_MESSAGE == action))) {
            return;
        }
        switch (action) {
            case ACTION_REPLY:
                execJimmAction(NativeCanvas.JIMM_SELECT);
                break;

            case ACTION_COPY_TEXT:
                copyText();
                break;

            case ACTION_GOTO_URL:
                Jimm.getJimm().getCL().gotoUrl(getCurrentText());
                break;

            case ACTION_SELECT:
                markItem(getCurrItem());
                break;

            // #sijapp cond.if modules_HISTORY is "true" #
            case ACTION_ADD_TO_HISTORY:
                addTextToHistory();
                break;
            // #sijapp cond.end#

            case ACTION_DEL_CHAT:
                ChatHistory.instance.getUpdater().removeMessagesAtCursor(model);
                if (0 < getSize()) {
                    restore();
                } else {
                    ChatHistory.instance.unregisterChat(model);
                    Jimm.getJimm().getCL().activate(null);
                }
                break;

            // #sijapp cond.if modules_FILES="true"#
            case ACTION_FT_CANCEL:
                Jimm.getJimm().jimmModel.removeTransfer(getCurrentMsgData(), true);
                break;
            // #sijapp cond.end#

            default:
                new ContactMenu(getProtocol(), getContact()).doAction(action);
        }
    }
    private static final int ACTION_FT_CANCEL = 900;
    private static final int ACTION_REPLY = 901;
    private static final int ACTION_ADD_TO_HISTORY = 902;
    private static final int ACTION_COPY_TEXT = 903;
    private static final int ACTION_GOTO_URL = 904;
    private static final int ACTION_DEL_CHAT = 905;
    private static final int ACTION_SELECT = 906;

    private MenuModel getContextMenu() {
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_FILES="true"#
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        } else {
            menu.addItem("select", ACTION_SELECT);
        }
        // #sijapp cond.else#
        menu.addItem("select", ACTION_SELECT);
        // #sijapp cond.end#
        if (!selectMode && (null != md) && md.isURL()) {
            menu.addItem("goto_url", ACTION_GOTO_URL);
        }
        menu.addItem("copy_text", ACTION_COPY_TEXT);
        // #sijapp cond.if modules_HISTORY is "true" #
        if (!selectMode && !Options.getBoolean(Options.OPTION_HISTORY) && getContact().hasHistory()) {
            menu.addItem("add_to_history", ACTION_ADD_TO_HISTORY);
        }
        // #sijapp cond.end#
        menu.setActionListener(new Binder(this));
        return menu;
    }
    protected MenuModel getMenu() {
        if (selectMode) {
            MenuModel menu = new MenuModel();
            menu.addItem("copy_text", ACTION_COPY_TEXT);
            menu.setActionListener(new Binder(this));
            return menu;
        }
        boolean accessible = model.writable && (getContact().isSingleUserContact() || getContact().isOnline());
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_ANDROID isnot "true" #
        // #sijapp cond.if modules_FILES="true"#
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
        if (0 < model.authRequestCounter) {
            menu.addItem("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.addItem("deny", Contact.USER_MENU_DENY_AUTH);
        }

        // #sijapp cond.if modules_ANDROID isnot "true" #
        if (getContact().isSingleUserContact()) {
            if (model.isBlogBot()) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            } else {
                menu.addItem("reply", Contact.USER_MENU_MESSAGE);
            }
        } else {
            if (model.writable) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            }
        }
        // #sijapp cond.end#
        if (!getContact().isSingleUserContact()) {
            menu.addItem("list_of_users", Contact.USER_MENU_USERS_LIST);
        }
        // #sijapp cond.if modules_ANDROID isnot "true" #
        if ((null != md) && md.isURL()) {
            menu.addItem("goto_url", ACTION_GOTO_URL);
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_FILES is "true"#
        if (accessible) {
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addItem("ft_name", Contact.USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.addItem("ft_cam", Contact.USER_MENU_CAM_TRANS);
            }
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_ANDROID isnot "true" #
        menu.addItem("copy_text", ACTION_COPY_TEXT);
        // #sijapp cond.end#
        if (accessible) {
            if (!JimmUI.isClipBoardEmpty()) {
                menu.addItem("paste", Contact.USER_MENU_PASTE);
            }
        }
        getContact().addChatMenuItems(menu);


        // #sijapp cond.if modules_ANDROID isnot "true" #
        // #sijapp cond.if modules_HISTORY is "true" #
        if (!Options.getBoolean(Options.OPTION_HISTORY) && getContact().hasHistory()) {
            menu.addItem("add_to_history", ACTION_ADD_TO_HISTORY);
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
        if (!getContact().isAuth()) {
            menu.addItem("requauth", Contact.USER_MENU_REQU_AUTH);
        }
        //menu.addItem("user_menu",   USER_MENU_SHOW);
        //if (!getContact().isSingleUserContact() && getContact().isOnline()) {
        //    menu.addItem("leave_chat", Contact.CONFERENCE_DISCONNECT);
        //}
        menu.addItem("delete_chat", ACTION_DEL_CHAT);
        menu.setActionListener(new Binder(this));
        return menu;
    }

    public void beginTyping(boolean type) {
        updateStatusIcons();
        invalidate();
    }


    private String getFrom(Message message) {
        String senderName = message.getName();
        if (null == senderName) {
            senderName = message.isIncoming()
                    ? getContact().getName()
                    : model.getMyName();
        }
        return senderName;
    }

    protected void restoring() {
        setTopByOffset(getTopOffset());
        Jimm.getJimm().getCL().setCurrentContact(getContact());
        classic = Options.getBoolean(Options.OPTION_CLASSIC_CHAT);
        int h = line.getRealHeight();
        line.setSize(getHeight() - h, getWidth(), h);

        setSoftBarLabels("menu", "reply", "close", false);
        setCaption(getContact().getName());
        if (!classic) {
            line.setVisible(false);
        }
    }

    public void activate() {
        resetSelected();
        line.setString("");
        showTop();
        Jimm.getJimm().getCL()._setActiveContact(getContact());
        // #sijapp cond.if modules_ANDROID is "true" #
        NativeCanvas.getInstance().getInput().setOwner(this);
        // #sijapp cond.end #
    }
    // #sijapp cond.if modules_ANDROID is "true" #
    public void sendMessage(String message) {
        ChatHistory.instance.registerChat(model);
        NativeCanvas.getInstance().getInput().resetText();
        if (!getContact().isSingleUserContact() && message.endsWith(", ")) {
            message = "";
        }
        if (!StringConvertor.isEmpty(message)) {
            getProtocol().sendMessage(getContact(), message, true);
        }
    }
    // #sijapp cond.end #

    protected boolean isCurrentItemSelectable() {
        return true;
    }

    protected int getItemHeight(int itemIndex) {
        return model.getItemHeight(model.getMessage(itemIndex));
    }

    protected void drawItemBack(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        byte bg;
        if (mData.isMarked()) {
            bg = THEME_CHAT_BG_MARKED;
        } else if (mData.isService()) {
            bg = THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = mData.isIncoming() ? THEME_CHAT_BG_IN : THEME_CHAT_BG_OUT;
        } else {
            bg = mData.isIncoming() ? THEME_CHAT_BG_IN_ODD : THEME_CHAT_BG_OUT_ODD;
        }
        if (g.notEqualsColor(THEME_BACKGROUND, bg)) {
            if (getCurrItem() == index) {
                g.setThemeColor(THEME_SELECTION_BACK, bg, 0xA0);
            } else {
                g.setThemeColor(bg);
            }
            g.fillRect(x, y + skip, w, to);
        }
    }


    protected void drawItemData(GraphicsEx g, int index, int x, int y,
                                int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        int header = model.getMessageHeaderHeight(mData);
        if (0 < header) {
            drawMessageHeader(g, mData, x, y, w, header);
            y += header;
            h -= header;
            skip -= header;
        }
        model.getMessage(index).par.paint(getFontSet(), g, 1, y, skip, to);
    }

    private void drawMessageHeader(GraphicsEx g, MessData mData, int x1, int y1, int w, int h) {
        Icon icon = InfoFactory.msgIcons.iconAt(mData.iconIndex);
        if (null != icon) {
            int iconWidth = g.drawImage(icon, x1, y1, h) + 1;
            x1 += iconWidth;
            w -= iconWidth;
        }

        Font[] set = getFontSet();
        Font boldFont = set[FONT_STYLE_BOLD];
        g.setFont(boldFont);
        g.setThemeColor(getInOutColor(mData.isIncoming()));

        Font plainFont = set[FONT_STYLE_PLAIN];
        String time = mData.isMarked() ? "  v  " : mData.strTime;
        int timeWidth = plainFont.stringWidth(time);

        g.drawString(mData.getNick(), x1, y1, w - timeWidth, h);

        g.setFont(plainFont);
        g.drawString(time, x1 + w - timeWidth, y1, timeWidth, h);
    }

    protected void paint(GraphicsEx g) {
        model.resetUnreadMessages();
        updateStatusIcons();
        super.paint(g);
        if (classic) {
            if (line.isVisible()) {
                line.paint(g.getGraphics());
                setSoftBarLabels("menu", "send", "backspace", false);
            } else {
                setSoftBarLabels("menu", "reply", "close", false);
            }
        }
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    private void addTextToHistory() {
        MessData md = getCurrentMsgData();
        if ((null == md) || (null == md.getText())) {
            return;
        }
        if (getContact().hasHistory()) {
            HistoryStorage.getHistory(getContact()).addText(md.getText(), md.isIncoming(), md.getNick(), md.getTime());
        }
    }
    // #sijapp cond.end#

    private MessData getCurrentMsgData() {
        try {
            int messIndex = getCurrItem();
            return (messIndex < 0) ? null : model.getMessage(messIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentText() {
        MessData md = getCurrentMsgData();
        return (null == md) ? "" : md.getText();
    }

    private void resetSelected() {
        selectMode = false;
        for (int i = 0; i < model.size(); ++i) {
            model.getMessage(i).setMarked(false);
        }
    }
    private String copySelected() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < model.size(); ++i) {
            MessData md = model.getMessage(i);
            if (md.isMarked()) {
                String msg = md.getText();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(JimmUI.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n");
            }
        }
        return 0 == sb.length() ? null : sb.toString();
    }
    private void copyText() {
        String all = copySelected();
        if (null != all) {
            resetSelected();
            JimmUI.setClipBoardText(null, all);
            return;
        }
        MessData md = getCurrentMsgData();
        if (null == md) {
            return;
        }
        String msg = md.getText();
        if (md.isMe()) {
            msg = "*" + md.getNick() + " " + msg;
        }
        JimmUI.setClipBoardText(md.isIncoming(), md.getNick(), md.strTime, msg);
    }

    public boolean empty() {
        return 0 == model.size();
    }

    public boolean isVisibleChat() {
        return (this == Jimm.getJimm().getDisplay().getCurrentDisplay())
                && !Jimm.getJimm().isPaused();
    }


    public Contact getContact() {
        return model.contact;
    }

    public ChatModel getModel() {
        return model;
    }
}
