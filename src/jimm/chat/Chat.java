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

import DrawControls.icons.Icon;
import DrawControls.text.*;
import java.util.*;
import jimm.*;
import jimm.chat.message.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.history.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.modules.*;
import protocol.*;
import protocol.jabber.*;
import javax.microedition.lcdui.Font;

public final class Chat extends ScrollableArea {
    private Protocol protocol;
    private Contact contact;
    private boolean writable = true;
    // #sijapp cond.if modules_HISTORY is "true" #
    private HistoryStorage history;
    // #sijapp cond.end#
    private static InputTextLine line = new InputTextLine();
    private boolean classic = false;
    private Icon[] statusIcons = new Icon[7];
    private Vector messData = new Vector();
    private boolean showStatus = true;
    ///////////////////////////////////////////

    private Parser createParser() {
        final int width = NativeCanvas.getInstance().getMinScreenMetrics() - 3;
        return new Parser(getFontSet(), width);
    }

    public final int getSize() {
        return messData.size();
    }

    private Par getPar(int index) {
        return getMessageDataByIndex(index).par;
    }

    private MessData getMessageDataByIndex(int index) {
        return (MessData) messData.elementAt(index);
    }

    ///////////////////////////////////////////
    public final void setWritable(boolean wr) {
        writable = wr;
    }

    public Chat(Protocol p, Contact item) {
        super(null);
        contact = item;
        protocol = p;

        setFontSet(GraphicsEx.chatFontSet);
        // #sijapp cond.if modules_HISTORY is "true" #
        fillFromHistory();
        // #sijapp cond.end #
        setTopByOffset(getFullSize());
    }

    void setContact(Contact item) {
        contact = item;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            ChatHistory.instance.showNextPrevChat(this, (fromX > toX));
        }
    }
    // #sijapp cond.end#

    // #sijapp cond.if modules_TOUCH is "true"#
    public void touchCaptionTapped(boolean icon) {
        ChatHistory.instance.showChatList(icon);
    }
    protected void touchItemTaped(int item, int x, boolean isLong) {
        if (isLong) {
            showMenu(getMenu());
        } else if (getWidth() - minItemHeight < x) {
            markItem(item);
        } else {
            super.touchItemTaped(item, x, isLong);
        }
    }
    // #sijapp cond.end#

    private void markItem(int item) {
        MessData mData = getMessageDataByIndex(item);
        mData.setMarked(!mData.isMarked());
        invalidate();
    }

    private void updateStatusIcons() {
        for (int i = 0; i < statusIcons.length; ++i) {
            statusIcons[i] = null;
        }
        contact.getLeftIcons(statusIcons);
        setCapImages(statusIcons);
    }
    public void updateStatus() {
        updateStatusIcons();
        showStatus = true;
        showStatusPopup();
        invalidate();
    }

    private byte getInOutColor(boolean incoming) {
        return incoming ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG;
    }
    public static final String ADDRESS = ", ";

    public final void writeMessage(String initText) {
        if (writable) {
            if (classic) {
                line.setString(initText);
                line.setVisible(true);
                invalidate();
                restore();
                return;
            }
            MessageEditor editor = ContactList.getInstance().getMessageEditor();
            if (null != editor) {
                editor.writeMessage(protocol, contact, initText);
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
    private boolean isBlogBot() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (contact instanceof JabberContact) {
            return ((Jabber) protocol).isBlogBot(contact.getUserId());
        }
        // #sijapp cond.end #
        return false;
    }
    public boolean isHuman() {
        boolean service = isBlogBot() || protocol.isBot(contact);
        // #sijapp cond.if protocols_JABBER is "true" #
        if (contact instanceof JabberContact) {
            service |= Jid.isGate(contact.getUserId());
        }
        // #sijapp cond.end #
        return !service && contact.isSingleUserContact();
    }

    Protocol getProtocol() {
        return protocol;
    }

    void onMessageSelected() {
        if (contact.isSingleUserContact()) {
            if (isBlogBot()) {
                writeMessage(getBlogPostId(getCurrentText()));
                return;
            }
            writeMessage(null);
            return;
        }
        MessData md = getCurrentMsgData();
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        writeMessageTo(getMyName().equals(nick) ? null : nick);
    }

    protected boolean qwertyKey(int keyCode, int type) {
        return classic && line.qwertyKey(this, keyCode, type);
    }

    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        if (classic && line.doKeyReaction(this, keyCode, actionCode, type)) {
            return;
        }
        if (CanvasEx.KEY_PRESSED == type) {
            resetUnreadMessages();
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
                    ChatHistory.instance.showNextPrevChat(this, NativeCanvas.NAVIKEY_RIGHT == actionCode);
                    return;
            }
        }
        if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
            if (CanvasEx.KEY_RELEASED != type) {
                return;
            }
            if (NativeCanvas.isLongFirePress()) {
                markItem(getCurrItem());
            } else if ('5' == keyCode) {
                execJimmAction(NativeCanvas.JIMM_SELECT);
            } else {
                writeMessage(null);
            }
            return;
        }
        if (!JimmUI.execHotKey(protocol, contact, keyCode, type)) {
            super.doKeyReaction(keyCode, actionCode, type);
        }
    }

    protected void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_MENU:
                showMenu(getMenu());
                return;

            case NativeCanvas.JIMM_BACK:
                ContactList.getInstance().activate(contact);
                return;

            case NativeCanvas.JIMM_SELECT:
                onMessageSelected();
                return;
        }
        if (!writable && ((ACTION_REPLY == action)
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
                ContactList.getInstance().gotoUrl(getCurrentText());
                break;

            // #sijapp cond.if modules_HISTORY is "true" #
            case ACTION_ADD_TO_HISTORY:
                addTextToHistory();
                break;
            // #sijapp cond.end#

            case ACTION_DEL_CHAT:
                removeMessagesAtCursor();
                if (0 < getSize()) {
                    restore();
                } else {
                    ContactList.getInstance().activate();
                }
                break;

            // #sijapp cond.if modules_FILES="true"#
            case ACTION_FT_CANCEL:
                ContactList.getInstance().removeTransfer(getCurrentMsgData(), true);
                break;
            // #sijapp cond.end#

            default:
                new ContactMenu(protocol, contact).doAction(action);
        }
    }
    private static final int ACTION_FT_CANCEL = 900;
    private static final int ACTION_REPLY = 999;
    private static final int ACTION_ADD_TO_HISTORY = 998;
    private static final int ACTION_COPY_TEXT = 1024;
    private static final int ACTION_GOTO_URL = 1033;
    private static final int ACTION_DEL_CHAT = 1027;

    public MenuModel getMenu() {
        boolean accessible = writable && (contact.isSingleUserContact() || contact.isOnline());
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_FILES="true"#
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        }
        // #sijapp cond.end#
        if (0 < authRequestCounter) {
            menu.addItem("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.addItem("deny", Contact.USER_MENU_DENY_AUTH);
        }

        if (contact.isSingleUserContact()) {
            if (isBlogBot()) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            } else {
                menu.addItem("reply", Contact.USER_MENU_MESSAGE);
            }
        } else {
            if (writable) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            }
            menu.addItem("list_of_users", Contact.USER_MENU_USERS_LIST);
        }

        if ((null != md) && md.isURL()) {
            menu.addItem("goto_url", ACTION_GOTO_URL);
        }

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

        menu.addItem("copy_text", ACTION_COPY_TEXT);
        if (accessible) {
            if (!JimmUI.isClipBoardEmpty()) {
                menu.addItem("paste", Contact.USER_MENU_PASTE);
                menu.addItem("quote", Contact.USER_MENU_QUOTE);
            }
        }
        contact.addChatMenuItems(menu);


        // #sijapp cond.if modules_HISTORY is "true" #
        if (!Options.getBoolean(Options.OPTION_HISTORY) && hasHistory()) {
            menu.addItem("add_to_history", ACTION_ADD_TO_HISTORY);
        }
        // #sijapp cond.end#
        if (!contact.isAuth()) {
            menu.addItem("requauth", Contact.USER_MENU_REQU_AUTH);
        }
        //menu.addItem("user_menu",   USER_MENU_SHOW);
        //if (!contact.isSingleUserContact() && contact.isOnline()) {
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

    // #sijapp cond.if protocols_JABBER is "true" #
    public static boolean isHighlight(String text, String nick) {
        if (null == nick) {
            return false;
        }
        for (int index = text.indexOf(nick); -1 != index; index = text.indexOf(nick, index + 1)) {
            if (0 < index) {
                char before = text.charAt(index - 1);
                if ((' ' != before) && ('\n' != before) && ('\t' != before)) {
                    continue;
                }
            }
            if (index + nick.length() + 2 < text.length()) {
                // Calculate space char...
                // ' a': min(' ', 'a') is ' '
                // 'a ': min('a', ' ') is ' '
                char after = (char) Math.min(text.charAt(index + nick.length()),
                        text.charAt(index + nick.length() + 1));
                if ((' ' != after) && ('\n' != after) && ('\t' != after)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    // #sijapp cond.end#

    // #sijapp cond.if modules_FILES="true"#
    public MessData addFileProgress(String caption, String text) {
        long time = Jimm.getCurrentGmtTime();
        short flags = MessData.PROGRESS;
        Parser parser = createParser();
        parser.addText(text, THEME_TEXT, FONT_STYLE_PLAIN);
        parser.addProgress(THEME_TEXT);
        Par par = parser.getPar();
        MessData mData = new MessData(time, "", caption, flags, Message.ICON_NONE, par);
        synchronized (this) {
            boolean atTheEnd = (getFullSize() - getTopOffset() <= getContentHeight());
            lock();
            messData.addElement(mData);
            setCurrentItemIndex(getSize() - 1);
            removeOldMessages();
            unlock();
        }
        ChatHistory.instance.registerChat(this);
        return mData;
    }

    public void changeFileProgress(MessData mData, String caption, String text) {
        final int width = NativeCanvas.getInstance().getMinScreenMetrics() - 3;
        Parser parser = new Parser(mData.par, getFontSet(), width);
        parser.addText(text, THEME_TEXT, FONT_STYLE_PLAIN);

        long time = Jimm.getCurrentGmtTime();
        short flags = MessData.PROGRESS;
        synchronized (this) {
            int index = Util.getIndex(messData, mData);
            if ((0 < getSize()) && (0 <= index)) {
                lock();
                mData.init(time, text, caption, flags, Message.ICON_NONE);
                parser.commit();
                unlock();
            }
        }
    }
    // #sijapp cond.end#

    private int getIcon(Message message) {
        if (message instanceof SystemNotice) {
            int type = ((SystemNotice)message).getSysnoteType();
            if (SystemNotice.SYS_NOTICE_MESSAGE == type) {
                return Message.ICON_NONE;
            }
            return Message.ICON_SYSREQ;
        }
        if (message.isIncoming()) {
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!contact.isSingleUserContact()
                    && !isHighlight(message.getProcessedText(), getMyName())) {
                return Message.ICON_IN_MSG;
            }
            // #sijapp cond.end#
            return Message.ICON_IN_MSG_HI;
        }
        return Message.ICON_OUT_MSG;
    }

    private String getMyName() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (contact instanceof JabberServiceContact) {
            String nick = ((JabberServiceContact)contact).getMyName();
            if (null != nick) return nick;
        }
        // #sijapp cond.end#
        return protocol.getNick();
    }
    private String getFrom(Message message) {
        String senderName = message.getName();
        if (null == senderName) {
            senderName = message.isIncoming()
                    ? contact.getName()
                    : getMyName();
        }
        return senderName;
    }
    private void addTextToForm(Message message) {
        String from = getFrom(message);
        boolean incoming = message.isIncoming();
        boolean offline = message.isOffline();

        String messageText = message.getProcessedText();
        messageText = StringConvertor.removeCr(messageText);
        if (StringConvertor.isEmpty(messageText)) {
            return;
        }
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return;
            }
        }

        Parser parser = createParser();

        final byte captColor = getInOutColor(incoming);
        final byte plain = FONT_STYLE_PLAIN;
        if (isMe) {
            Icon icon = Message.msgIcons.iconAt(getIcon(message));
            if (null != icon) {
                parser.addImage(icon);
            }
            parser.addText("*", captColor, plain);
            parser.addText(from, captColor, plain);
            parser.addText(" ", captColor, plain);
            parser.addTextWithSmiles(messageText, captColor, plain);

        } else {
            byte color = THEME_TEXT;
            // #sijapp cond.if protocols_JABBER is "true" #
            if (incoming && !contact.isSingleUserContact()
                    && isHighlight(messageText, getMyName())) {
                color = CanvasEx.THEME_CHAT_HIGHLIGHT_MSG;
            }
            // #sijapp cond.end#
            parser.addTextWithSmiles(messageText, color, plain);
        }

        short flags = 0;
        if (incoming) {
            flags |= MessData.INCOMING;
        }
        if (isMe) {
            flags |= MessData.ME;
        }
        if (Util.hasURL(messageText)) {
            flags |= MessData.URLS;
        }
        if (message instanceof SystemNotice) {
            flags |= MessData.SERVICE;
        }

        Par par = parser.getPar();
        MessData mData = new MessData(message.getNewDate(), messageText, from, flags, getIcon(message), par);
        if (!incoming) {
            message.setVisibleIcon(par, mData);
        }
        synchronized (this) {
            boolean atTheEnd = (getFullSize() - getTopOffset() <= getContentHeight());
            lock();
            messData.addElement(mData);

            int size = getSize();
            if (incoming) {
                int currentMessageIndex = getCurrItem();
                if (isVisibleChat()) {
                    if (atTheEnd) {
                        // #sijapp cond.if modules_TOUCH is "true"#
                        atTheEnd = (currentMessageIndex == size - 2);
                        if (NativeCanvas.getInstance().touchControl.touchUsed) {
                            atTheEnd = true;
                        }
                        // #sijapp cond.end#
                    }
                    if (atTheEnd) {
                        setCurrentItemIndex(size - 1);
                    }

                } else {
                    int unread = getUnreadMessageCount();
                    if (size - unread - 2 <= currentMessageIndex) {
                        setCurrentItemToTop(Math.max(0, size - 1 - unread));
                    }
                }

            } else if (isBlogBot()) {
                if (atTheEnd) {
                    setCurrentItemIndex(size - 1);
                }

            } else {
                setCurrentItemIndex(size - 1);
            }

            removeOldMessages();
            unlock();
        }
    }

    protected void restoring() {
        ContactList.getInstance().setCurrentContact(contact);
        classic = Options.getBoolean(Options.OPTION_CLASSIC_CHAT);
        int h = line.getRealHeight();
        line.setSize(getScreenHeight() - h, getWidth(), h);

        setSoftBarLabels("menu", "reply", "close", false);
        setCaption(contact.getName());
        if (!classic) {
            line.setVisible(false);
        }
    }

    public void activate() {
        resetSelected();
        line.setString("");
        showTop();
        if (showStatus) {
            showStatusPopup();
        }
    }
    private void showStatusPopup() {
        showStatus = false;
//        String status = contact.getStatusText();
//        if (StringConvertor.isEmpty(status)) {
//            status = contact.getXStatusText();
//        }
//        if (!StringConvertor.isEmpty(status)) {
//            new Popup(this, status).show();
//        }
    }

    protected int getHeight() {
        return classic ? getScreenHeight() - line.getHeight() : getScreenHeight();
    }

    protected boolean isCurrentItemSelectable() {
        return true;
    }

    protected int getItemHeight(int itemIndex) {
        MessData mData = getMessageDataByIndex(itemIndex);
        return getPar(itemIndex).getHeight() + getMessageHeaderHeight(mData);
    }

    // Overrides VirtualList.drawItemData
    protected void drawItemBack(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        MessData mData = getMessageDataByIndex(index);
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
        MessData mData = getMessageDataByIndex(index);
        int header = getMessageHeaderHeight(mData);
        if (0 < header) {
            drawMessageHeader(g, mData, x, y, w, header);
            y += header;
            h -= header;
            skip -= header;
        }
        getPar(index).paint(getFontSet(), g, 1, y, skip, to);
    }

    private int getMessageHeaderHeight(MessData mData) {
        if ((null == mData) || mData.isMe()) return 0;

        int height = getFontSet()[FONT_STYLE_BOLD].getHeight();
        Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
        if (null != icon) {
            height = Math.max(height, icon.getHeight());
        }
        return height;
    }

    private void drawMessageHeader(GraphicsEx g, MessData mData, int x1, int y1, int w, int h) {
        Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
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
        resetUnreadMessages();
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
    final static private int MAX_HIST_LAST_MESS = 5;

    private boolean hasHistory() {
        return contact.hasHistory();
    }
    private void fillFromHistory() {
        if (!hasHistory()) {
            return;
        }
        if (isBlogBot()) {
            return;
        }
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            if (0 != getSize()) {
                return;
            }
            HistoryStorage hist = getHistory();
            hist.openHistory();
            int recCount = hist.getHistorySize();
            if (0 == recCount) {
                return;
            }

            int loadOffset = Math.max(recCount - MAX_HIST_LAST_MESS, 0);
            for (int i = loadOffset; i < recCount; ++i) {
                CachedRecord rec = hist.getRecord(i);
                if (null == rec) {
                    continue;
                }
                long date = Util.createLocalDate(rec.date);
                PlainMessage message;
                if (rec.isIncoming()) {
                    message = new PlainMessage(rec.from, protocol, date, rec.text, true);
                } else {
                    message = new PlainMessage(protocol, contact, date, rec.text);
                }
                addTextToForm(message);
            }
            hist.closeHistory();
        }
    }

    public HistoryStorage getHistory() {
        if ((null == history) && hasHistory()) {
            history = HistoryStorage.getHistory(contact);
        }
        return history;
    }

    private void addToHistory(String msg, boolean incoming, String nick, long time) {
        if (hasHistory()) {
            getHistory().addText(msg, incoming, nick, time);
        }
    }

    private void addTextToHistory() {
        if (!hasHistory()) {
            return;
        }
        MessData md = getCurrentMsgData();
        if ((null == md) || (null == md.getText())) {
            return;
        }
        addToHistory(md.getText(), md.isIncoming(), md.getNick(), md.getTime());
    }
    // #sijapp cond.end#

    private MessData getCurrentMsgData() {
        try {
            int messIndex = getCurrItem();
            return (messIndex < 0) ? null : (MessData) messData.elementAt(messIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentText() {
        MessData md = getCurrentMsgData();
        return (null == md) ? "" : md.getText();
    }

    public void clear() {
        messData.removeAllElements();
    }

    private void removeMessages(int limit) {
        if (getSize() < limit) {
            return;
        }
        if ((0 < limit) && (0 < getSize())) {
            while (limit < messData.size()) {
                int top = Math.max(0, getTopOffset() - getItemHeight(0));
                messData.removeElementAt(0);
                setCurrentItemIndex(Math.max(0, getCurrItem() - 1));
                setTopByOffset(top);
            }
            invalidate();
        } else {
            ChatHistory.instance.unregisterChat(this);
        }
    }

    private void removeOldMessages() {
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
    }

    public void removeReadMessages() {
        removeMessages(getUnreadMessageCount());
    }

    public void removeMessagesAtCursor() {
        removeMessages(messData.size() - getCurrItem() - 1);
    }


    private void resetSelected() {
        for (int i = 0; i < messData.size(); ++i) {
            getMessageDataByIndex(i).setMarked(false);
        }
    }
    private String copySelected() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < messData.size(); ++i) {
            MessData md = getMessageDataByIndex(i);
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
        return (0 == messData.size()) && (0 == getSize());
    }

    public long getLastMessageTime() {
        if (0 == messData.size()) {
            return 0;
        }
        MessData md = (MessData) messData.lastElement();
        return md.getTime();
    }

    public boolean isVisibleChat() {
        return (this == Jimm.getJimm().getDisplay().getCurrentDisplay())
                && !Jimm.isPaused();
    }

    private short messageCounter = 0;
    private short otherMessageCounter = 0;
    private byte sysNoticeCounter = 0;
    private byte authRequestCounter = 0;

    public void resetAuthRequests() {
        boolean notEmpty = (0 < authRequestCounter);
        authRequestCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            protocol.markMessages(contact);
        }
    }

    private void resetUnreadMessages() {
        boolean notEmpty = (0 < messageCounter)
                || (0 < otherMessageCounter)
                || (0 < sysNoticeCounter);
        messageCounter = 0;
        otherMessageCounter = 0;
        sysNoticeCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            protocol.markMessages(contact);
        }
    }

    public int getUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter
                + otherMessageCounter;
    }
    public int getPersonalUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter;
    }

    public final int getNewMessageIcon() {
        if (0 < messageCounter) {
            return Message.ICON_IN_MSG_HI;
        } else if (0 < authRequestCounter) {
            return Message.ICON_SYSREQ;
        } else if (0 < otherMessageCounter) {
            return Message.ICON_IN_MSG;
        } else if (0 < sysNoticeCounter) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }

    public void addMyMessage(PlainMessage message) {
        ChatHistory.instance.registerChat(this);
        resetUnreadMessages();
        addTextToForm(message);
        // #sijapp cond.if modules_HISTORY is "true" #
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            addToHistory(message.getText(), false, getFrom(message), message.getNewDate());
        }
        // #sijapp cond.end#
    }
    // Adds a message to the message display

    private short inc(short val) {
        return (short) ((val < Short.MAX_VALUE) ? (val + 1) : val);
    }
    private byte inc(byte val) {
        return (byte) ((val < Byte.MAX_VALUE) ? (val + 1) : val);
    }
    public void addMessage(Message message, boolean toHistory) {
        ChatHistory.instance.registerChat(this);
        boolean inc = !isVisibleChat();
        if (message instanceof PlainMessage) {

            addTextToForm(message);
            // #sijapp cond.if modules_HISTORY is "true" #
            if (toHistory && Options.getBoolean(Options.OPTION_HISTORY)) {
                final String nick = getFrom(message);
                addToHistory(message.getText(), true, nick, message.getNewDate());
            }
            // #sijapp cond.end#
            if (inc) {
                messageCounter = inc(messageCounter);
                // #sijapp cond.if protocols_JABBER is "true" #
                if (!contact.isSingleUserContact()
                        && !isHighlight(message.getProcessedText(), getMyName())) {
                    otherMessageCounter = inc(otherMessageCounter);
                    messageCounter--;
                }
                // #sijapp cond.end#
            }

        } else if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                inc = true;
                authRequestCounter = inc(authRequestCounter);

            } else if (inc) {
                sysNoticeCounter = inc(sysNoticeCounter);
            }

            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            MagicEye.addAction(protocol, contact.getUserId(), message.getText());
            // #sijapp cond.end #
            addTextToForm(message);
        }
        if (inc) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public Contact getContact() {
        return contact;
    }

    MessData getUnreadMessage(int num) {
        int index = messData.size() - getUnreadMessageCount() - num;
        return (MessData) messData.elementAt(index);
    }
}
