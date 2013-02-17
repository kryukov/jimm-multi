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

package jimm.chat;

import DrawControls.icons.Icon;
import java.util.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.ui.base.*;
import protocol.Protocol;
import jimm.ui.menu.*;
import java.io.*;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.io.Storage;
import jimm.util.JLocale;
import protocol.Contact;

public final class ChatHistory extends ScrollableArea {
    protected final Vector historyTable = new Vector();
    public static final ChatHistory instance = new ChatHistory();
    private final Icon[] leftIcons = new Icon[7];
    private int itemHeight;

    private ChatHistory() {
        super(JLocale.getString("chats"));
        itemHeight = Math.max(minItemHeight, getDefaultFont().getHeight());
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    public void touchCaptionTapped(boolean icon) {
        goBack();
    }
    // #sijapp cond.end#

    final public static int DEL_TYPE_ALL_EXCEPT_CUR = 1;
    final public static int DEL_TYPE_ALL            = 2;

    private int getTotal() {
        return historyTable.size();
    }
    private Chat chatAt(int index) {
        return (Chat)historyTable.elementAt(index);
    }
    private Contact contactAt(int index) {
        return chatAt(index).getContact();
    }

    public Chat getChat(Contact c) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            if (c == contactAt(i)) {
                return chatAt(i);
            }
        }
        return null;
    }

    public int getUnreadMessageCount() {
        int count = 0;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            count += chatAt(i).getUnreadMessageCount();
        }
        return count;
    }
    public int getPersonalUnreadMessageCount(boolean all) {
        int count = 0;
        Chat chat = null;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (all || chat.isHuman() || !chat.getContact().isSingleUserContact()) {
                count += chat.getPersonalUnreadMessageCount();
            }
        }
        return count;
    }
    private int getMoreImportant(int v1, int v2) {
        if ((Message.ICON_IN_MSG_HI == v1) || (Message.ICON_IN_MSG_HI == v2)) {
            return Message.ICON_IN_MSG_HI;
        }
        if ((Message.ICON_SYSREQ == v1) || (Message.ICON_SYSREQ == v2)) {
            return Message.ICON_SYSREQ;
        }
        if ((Message.ICON_IN_MSG == v1) || (Message.ICON_IN_MSG == v2)) {
            return Message.ICON_IN_MSG;
        }
        if ((Message.ICON_SYS_OK == v1) || (Message.ICON_SYS_OK == v2)) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }
    public Icon getUnreadMessageIcon() {
        int icon = -1;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            icon = getMoreImportant(icon, chatAt(i).getNewMessageIcon());
        }
        return Message.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Protocol p) {
        int icon = -1;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (chat.getProtocol() == p) {
                icon = getMoreImportant(icon, chat.getNewMessageIcon());
            }
        }
        return Message.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Vector contacts) {
        int icon = -1;
        Contact c;
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            c = (Contact)contacts.elementAt(i);
            icon = getMoreImportant(icon, c.getUnreadMessageIcon());
        }
        return Message.msgIcons.iconAt(icon);
    }

    private Chat getSelectedChat() {
        return (getCurrItem() < getSize()) ? chatAt(getCurrItem()) : null;
    }
    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        Chat chat = getSelectedChat();
        if ((KEY_PRESSED == type) && (NativeCanvas.CLEAR_KEY == keyCode)) {
            removeChat(chat);
            return;
        }
        if ((KEY_REPEATED == type) || (KEY_PRESSED == type)) {
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_DOWN:
                    setCurrentItemIndex((getCurrItem() + 1) % getSize());
                    invalidate();
                    return;
                case NativeCanvas.NAVIKEY_UP:
                    setCurrentItemIndex((getCurrItem() + getSize() - 1) % getSize());
                    invalidate();
                    return;
            }
        }

        if (JimmUI.execHotKey(null == chat ? null : chat.getProtocol(),
                null == chat ? null : chat.getContact(), keyCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    // Creates a new chat form
    public void registerChat(Chat item) {
        if (-1 == Util.getIndex(historyTable, item)) {
            historyTable.addElement(item);
            item.getContact().updateChatState(item);
            Icon[] icons = new Icon[7];
            item.getContact().getLeftIcons(icons);
            itemHeight = Math.max(itemHeight, GraphicsEx.getMaxImagesHeight(icons));
        }
    }

    public void unregisterChats(Protocol p) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat key = chatAt(i);
            if (key.getProtocol() == p) {
                historyTable.removeElement(key);
                key.clear();
                key.getContact().updateChatState(null);
            }
        }
        ContactList.getInstance().markMessages(null);
    }
    public void unregisterChat(Chat item) {
        if (null == item) return;
        historyTable.removeElement(item);
        item.clear();
        Contact c = item.getContact();
        c.updateChatState(null);
        item.getProtocol().ui_updateContact(c);
        if (0 < item.getUnreadMessageCount()) {
            ContactList.getInstance().markMessages(c);
        }
    }

    private void removeChat(Chat chat) {
        if (null != chat) {
            clearChat(chat);
            setCurrentItemIndex(getCurrItem());
            invalidate();
        }
        if (0 < getSize()) {
            restore();

        } else {
            ContactList.getInstance().activate();
        }
    }
    private void clearChat(Chat chat) {
        if (chat.isHuman() && !chat.getContact().isTemp()) {
            chat.removeReadMessages();

        } else {
            unregisterChat(chat);
        }
    }
    public void removeAll(Chat except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat chat = chatAt(i);
            if (except == chat) continue;
            clearChat(chat);
        }
        setCurrentItemIndex(getCurrItem());
        if (0 < getSize()) {
            restore();

        } else {
            ContactList.getInstance().activate();
        }
    }


    public void restoreContactsWithChat(Protocol p) {
        int total = getTotal();
        for (int i = 0; i < total; ++i) {
            Contact contact = contactAt(i);
            Chat chat = chatAt(i);
            if (p != chat.getProtocol()) {
                continue;
            }
            if (!p.inContactList(contact)) {
                Contact newContact = p.getItemByUIN(contact.getUserId());
                if (null != newContact) {
                    chat.setContact(newContact);
                    contact.updateChatState(null);
                    newContact.updateChatState(chat);
                    continue;
                }
                if (contact.isSingleUserContact()) {
                    contact.setTempFlag(true);
                    contact.setGroup(null);
                } else {
                    if (null == p.getGroup(contact)) {
                        contact.setGroup(p.getGroup(contact.getDefaultGroupName()));
                    }
                }
                p.addTempContact(contact);
            }
        }
    }

    void updateChatList() {
        invalidate();
    }
    private int getPreferredItem() {
        for (int i = 0; i < historyTable.size(); ++i) {
            if (0 < chatAt(i).getPersonalUnreadMessageCount()) {
                return i;
            }
        }
        Contact currentContact = ContactList.getInstance().getCurrentContact();
        int current  = 0;
        for (int i = 0; i < historyTable.size(); ++i) {
            Chat chat = chatAt(i);
            if (0 < chat.getUnreadMessageCount()) {
                return i;
            }
            if (currentContact == chat.getContact()) {
                current = i;
            }
        }
        return current;
    }
    public void showChatList(boolean forceGoToChat) {
        if (forceGoToChat) {
            Chat current = chatAt(getPreferredItem());
            if (0 < current.getUnreadMessageCount()) {
                current.activate();
                return;
            }
        }
        setCurrentItemIndex(getPreferredItem());
        show();
    }

    // shows next or previos chat
    public void showNextPrevChat(Chat item, boolean next) {
        int chatNum = historyTable.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int nextChatNum = (chatNum + (next ? 1 : -1) + getTotal()) % getTotal();
        chatAt(nextChatNum).activate();
    }

    private static final int MENU_SELECT = 1;
    private static final int MENU_DEL_CURRENT_CHAT = 2;
    private static final int MENU_DEL_ALL_CHATS_EXCEPT_CUR = 3;
    private static final int MENU_DEL_ALL_CHATS = 4;

    public void goBack() {
        back();
        Object o = Jimm.getJimm().getDisplay().getCurrentDisplay();
        if ((o instanceof Chat) && (-1 == Util.getIndex(historyTable, o))) {
            ContactList.getInstance().activate();
        }
    }

    protected void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_SELECT:
                getSelectedChat().activate();
                return;

            case NativeCanvas.JIMM_BACK:
                goBack();
                return;

            case NativeCanvas.JIMM_MENU:
                showMenu(getMenu());
                return;
        }
        Chat chat = getSelectedChat();
        switch (action) {
            case MENU_SELECT:
                execJimmAction(NativeCanvas.JIMM_SELECT);
                return;

            case MENU_DEL_CURRENT_CHAT:
                removeChat(chat);
                break;

            case MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                removeAll(chat);
                break;

            case MENU_DEL_ALL_CHATS:
                removeAll(null);
                return;
        }
    }

    protected final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (0 < getSize()) {
            menu.addItem("select",                  MENU_SELECT);
            menu.addItem("currect_contact",         MENU_DEL_CURRENT_CHAT);
            menu.addItem("all_contact_except_this", MENU_DEL_ALL_CHATS_EXCEPT_CUR);
            menu.addItem("all_contacts",            MENU_DEL_ALL_CHATS);
        }
        menu.setActionListener(new Binder(this));
        return menu;
    }
    protected int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    protected void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        for (int i = 0; i < leftIcons.length; ++i) {
            leftIcons[i] = null;
        }
        g.setThemeColor(THEME_TEXT);
        g.setFont(getDefaultFont());
        Chat chat = (Chat)historyTable.elementAt(index);
        chat.getContact().getLeftIcons(leftIcons);
        g.drawString(leftIcons, chat.getContact().getName(), null, x, y, w, h);
    }

    protected int getSize() {
        return historyTable.size();
    }

    public void saveUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.delete();
            s.open(true);
            for (int i = getTotal() - 1; 0 <= i; --i) {
                Chat chat = chatAt(i);
                if (!chat.getContact().isSingleUserContact()) {
                    continue;
                }
                if (chat.getContact().isTemp()) {
                    continue;
                }
                if (!chat.getContact().hasHistory()) {
                    continue;
                }
                int count = chat.getUnreadMessageCount();
                for (int j = 0; j < count; ++j) {
                    MessData message = chat.getUnreadMessage(j);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream o = new DataOutputStream(out);
                    o.writeUTF(chat.getProtocol().getUserId());
                    o.writeUTF(chat.getContact().getUserId());
                    o.writeUTF(message.getNick());
                    o.writeUTF(message.getText());
                    o.writeLong(message.getTime());
                    s.addRecord(out.toByteArray());
                }
            }
        } catch (Exception ex) {
        }
        s.close();
    }
    public void loadUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.open(false);
            for (int i = 1; i <= s.getNumRecords(); ++i) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(s.getRecord(i)));
                String account = in.readUTF();
                String userId = in.readUTF();
                String nick = in.readUTF();
                String text = in.readUTF();
                long time = in.readLong();
                Protocol protocol = ContactList.getInstance().getProtocol(account);
                if (null == protocol) {
                    continue;
                }
                PlainMessage msg = new PlainMessage(userId, protocol, time, text, true);
                if (!StringConvertor.isEmpty(nick)) {
                    msg.setName(nick);
                }
                protocol.addMessage(msg);
            }
        } catch (Exception ex) {
        }
        s.close();
        s.delete();
    }
}