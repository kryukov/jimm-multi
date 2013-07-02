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

import jimmui.view.base.CanvasEx;
import jimmui.view.icons.Icon;
import java.util.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import protocol.Protocol;
import jimmui.view.menu.*;
import java.io.*;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.io.Storage;
import protocol.Contact;
import jimmui.view.roster.VirtualContactList;

public final class ChatHistory implements SelectListener {
    public static final ChatHistory instance = new ChatHistory();
    private ChatUpdater updater = new ChatUpdater();

    private ChatHistory() {
    }

    public ChatUpdater getUpdater() {
        return updater;
    }

    private int getTotal() {
        return ContactList.getInstance().jimmModel.chats.size();
    }
    private ChatModel chatModelAt(int index) {
        return (ChatModel) ContactList.getInstance().jimmModel.chats.elementAt(index);
    }
    private Chat chatAt(int index) {
        return (Chat) ContactList.getInstance().jimmModel.modelToChat.get(ContactList.getInstance().jimmModel.chats.elementAt(index));
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
            count += chatAt(i).getModel().getUnreadMessageCount();
        }
        return count;
    }
    public int getPersonalUnreadMessageCount(boolean all) {
        int count = 0;
        ChatModel chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatModelAt(i);
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

    // Creates a new chat form
    public void registerChat(Chat item) {
        if (ContactList.getInstance().jimmModel.registerChat(item)) {
            ContactList.getInstance().getUpdater().registerChat(item);
        }
    }

    public void unregisterChats(Protocol p) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat key = chatAt(i);
            if (key.getProtocol() == p) {
                ContactList.getInstance().jimmModel.unregisterChat(key);
                ContactList.getInstance().getUpdater().unregisterChat(key);
            }
        }
        ContactList.getInstance().markMessages(null);
    }
    public void unregisterChat(Chat item) {
        if (null == item) return;
        ContactList.getInstance().jimmModel.unregisterChat(item);
        ContactList.getInstance().getUpdater().unregisterChat(item);
        Contact c = item.getContact();
        c.updateChatState(null);
        item.getProtocol().ui_updateContact(c);
        if (0 < item.getModel().getUnreadMessageCount()) {
            ContactList.getInstance().markMessages(c);
        }
    }

    private void removeChat(Chat chat) {
        if (null != chat) {
            clearChat(chat);
            if (Jimm.getJimm().getDisplay().remove(chat)) {
                ContactList.getInstance()._setActiveContact(null);
            }
            ContactList.getInstance().getUpdater().update();
        }
        if (0 == getTotal()) {
            ContactList.getInstance().getManager().setModel(ContactList.getInstance().getUpdater().getChatModel());
            ContactList.getInstance().activate();
        }
    }
    private void clearChat(Chat chat) {
        if (chat.getModel().isHuman() && !chat.getModel().getContact().isTemp()) {
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
        ContactList.getInstance().getUpdater().update();
        if (0 == getTotal()) {
            ContactList.getInstance().getManager().setModel(ContactList.getInstance().getUpdater().getChatModel());
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

    private int getPreferredItem() {
        for (int i = 0; i < getTotal(); ++i) {
            if (0 < chatAt(i).getModel().getPersonalUnreadMessageCount()) {
                return i;
            }
        }
        Contact currentContact = ContactList.getInstance().getCurrentContact();
        int current  = 0;
        for (int i = 0; i < getTotal(); ++i) {
            Chat chat = chatAt(i);
            if (0 < chat.getModel().getUnreadMessageCount()) {
                return i;
            }
            if (currentContact == chat.getContact()) {
                current = i;
            }
        }
        return current;
    }
    // shows next or previos chat
    public void showNextPrevChat(Chat item, boolean next) {
        int chatNum = ContactList.getInstance().jimmModel.chats.indexOf(item);
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

    @Override
    public void select(Select select, MenuModel menu, int cmd) {
        Chat chat = getChat(ContactList.getInstance().getCurrentContact());
        switch (cmd) {
            case MENU_DEL_CURRENT_CHAT:
                removeChat(chat);
                break;

            case MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                removeAll(chat);
                break;

            case MENU_DEL_ALL_CHATS:
                removeAll(null);
                break;
        }
    }

    public final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (0 < getTotal()) {
            menu.addItem("select",                  MENU_SELECT);
            menu.addItem("delete_chat",             MENU_DEL_CURRENT_CHAT);
            menu.addItem("all_contact_except_this", MENU_DEL_ALL_CHATS_EXCEPT_CUR);
            menu.addItem("all_contacts",            MENU_DEL_ALL_CHATS);
        }
        menu.setActionListener(this);
        return menu;
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
                int count = chat.getModel().getUnreadMessageCount();
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
        } catch (Exception ignored) {
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
                protocol.addMessage(msg, true);
            }
        } catch (Exception ignored) {
        }
        s.close();
        s.delete();
    }

    public void showChatList(boolean forceGoToChat) {
        if (forceGoToChat) {
            Chat current = chatAt(getPreferredItem());
            if (0 < current.getModel().getUnreadMessageCount()) {
                current.activate();
                return;
            }
        }
        ContactList.getInstance().getManager().setModel(ContactList.getInstance().getUpdater().getChatModel());
        ContactList.getInstance().setActiveContact(chatAt(getPreferredItem()).getContact());
        ContactList.getInstance().getManager().show();
    }

    public void back() {
        ContactList.getInstance().getManager().setModel(ContactList.getInstance().getUpdater().getModel());
        Jimm.getJimm().getDisplay().back(ContactList.getInstance().getManager());
    }

    public static boolean isChats(CanvasEx canvas) {
        if (canvas instanceof VirtualContactList) {
            VirtualContactList vcl = ((VirtualContactList) canvas);
            return vcl.getModel() == vcl.getUpdater().getChatModel();
        }
        return false;
    }
}