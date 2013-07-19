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
import jimm.comm.*;
import protocol.Protocol;
import jimmui.view.menu.*;
import java.io.*;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.io.Storage;
import protocol.Contact;
import jimmui.view.roster.VirtualContactList;
import protocol.ui.InfoFactory;

public final class ChatHistory implements SelectListener {
    public static final ChatHistory instance = new ChatHistory();
    private ChatUpdater updater = new ChatUpdater();

    private ChatHistory() {
    }

    public ChatUpdater getUpdater() {
        return updater;
    }

    private int getTotal() {
        return Jimm.getJimm().jimmModel.chats.size();
    }
    private ChatModel chatModelAt(int index) {
        return (ChatModel) Jimm.getJimm().jimmModel.chats.elementAt(index);
    }

    public ChatModel getChatModel(Contact c) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            if (c == chatModelAt(i).contact) {
                return chatModelAt(i);
            }
        }
        return null;
    }
    public Chat getChat(ChatModel c) {
        if (null == c) return null;
        Object view = Jimm.getJimm().getDisplay().getCurrentDisplay();
        if (view instanceof Chat) {
            Chat chat = (Chat) view;
            if (chat.getModel() == c) {
                return chat;
            }
        }
        return null;
    }
    public Chat getOrCreateChat(ChatModel c) {
        Chat chat = getChat(c);
        if (null != c) {
            chat = new Chat(c);
            updater.restoreTopPositionToUI(c, chat);
        }
        return chat;
    }

    public int getUnreadMessageCount() {
        int count = 0;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            count += chatModelAt(i).getUnreadMessageCount();
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
            icon = getMoreImportant(icon, chatModelAt(i).getNewMessageIcon());
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Protocol p) {
        int icon = -1;
        ChatModel chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatModelAt(i);
            if (chat.getProtocol() == p) {
                icon = getMoreImportant(icon, chat.getNewMessageIcon());
            }
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Vector contacts) {
        int icon = -1;
        Contact c;
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            c = (Contact)contacts.elementAt(i);
            icon = getMoreImportant(icon, c.getUnreadMessageIcon());
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }

    public boolean registerChat(ChatModel item) {
        if (Jimm.getJimm().jimmModel.registerChat(item)) {
            Jimm.getJimm().getCL().getUpdater().registerChat(item);
            return true;
        }
        return false;
    }

    public void unregisterChats(Protocol p) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            ChatModel key = chatModelAt(i);
            if (key.getProtocol() == p) {
                Jimm.getJimm().jimmModel.unregisterChat(key);
                Jimm.getJimm().getCL().getUpdater().unregisterChat(key);
            }
        }
        Jimm.getJimm().getCL().markMessages(null, null);
    }
    public void unregisterChat(ChatModel item) {
        if (null == item) return;
        Jimm.getJimm().jimmModel.unregisterChat(item);
        Jimm.getJimm().getCL().getUpdater().unregisterChat(item);
        Contact c = item.getContact();
        c.updateChatState(null);
        item.getProtocol().ui_updateContact(c);
        if (0 < item.getUnreadMessageCount()) {
            Jimm.getJimm().getCL().markMessages(item.protocol, c);
        }
    }

    private void removeChat(ChatModel chat) {
        if (null != chat) {
            clearChat(chat);
            Chat view = getChat(chat);
            if ((null != view) && Jimm.getJimm().getDisplay().remove(view)) {
                Jimm.getJimm().getCL()._setActiveContact(null);
            }
            Jimm.getJimm().getCL().getUpdater().update();
        }
        if (0 == getTotal()) {
            Jimm.getJimm().getCL().getManager().setModel(Jimm.getJimm().getCL().getUpdater().getChatModel());
            Jimm.getJimm().getCL().activate();
        }
    }
    private void clearChat(ChatModel chat) {
        if (chat.isHuman() && !chat.getContact().isTemp()) {
            updater.removeReadMessages(chat);

        } else {
            unregisterChat(chat);
        }
    }
    public void removeAll(ChatModel except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            ChatModel chat = chatModelAt(i);
            if (except == chat) continue;
            clearChat(chat);
        }
        Jimm.getJimm().getCL().getUpdater().update();
        if (0 == getTotal()) {
            Jimm.getJimm().getCL().getManager().setModel(Jimm.getJimm().getCL().getUpdater().getChatModel());
            Jimm.getJimm().getCL().activate();
        }
    }


    public void restoreContactsWithChat(Protocol p) {
        int total = getTotal();
        for (int i = 0; i < total; ++i) {
            ChatModel chat = chatModelAt(i);
            Contact contact = chat.contact;
            if (p != chat.getProtocol()) {
                continue;
            }
            if (!p.hasContact(contact)) {
                Contact newContact = p.getItemByUID(contact.getUserId());
                if (null != newContact) {
                    chat.contact = newContact;
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
            if (0 < chatModelAt(i).getPersonalUnreadMessageCount()) {
                return i;
            }
        }
        Contact currentContact = Jimm.getJimm().getCL().getUpdater().getCurrentContact();
        int current  = 0;
        for (int i = 0; i < getTotal(); ++i) {
            ChatModel chat = chatModelAt(i);
            if (0 < chat.getUnreadMessageCount()) {
                return i;
            }
            if (currentContact == chat.getContact()) {
                current = i;
            }
        }
        return current;
    }
    // shows next or previos chat
    public void showNextPrevChat(ChatModel item, boolean next) {
        int chatNum = Jimm.getJimm().jimmModel.chats.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int nextChatNum = (chatNum + (next ? 1 : -1) + getTotal()) % getTotal();
        updater.activate(chatModelAt(nextChatNum));
    }

    private static final int MENU_SELECT = 1;
    private static final int MENU_DEL_CURRENT_CHAT = 2;
    private static final int MENU_DEL_ALL_CHATS_EXCEPT_CUR = 3;
    private static final int MENU_DEL_ALL_CHATS = 4;

    @Override
    public void select(Select select, MenuModel menu, int cmd) {
        ChatModel chat = getChatModel(Jimm.getJimm().getCL().getUpdater().getCurrentContact());
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
                ChatModel chat = chatModelAt(i);
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
                Protocol protocol = Jimm.getJimm().jimmModel.getProtocol(account);
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
            ChatModel current = chatModelAt(getPreferredItem());
            if (0 < current.getUnreadMessageCount()) {
                updater.activate(current);
                return;
            }
        }
        Jimm.getJimm().getCL().getManager().setModel(Jimm.getJimm().getCL().getUpdater().getChatModel());
        Jimm.getJimm().getCL().setActiveContact(chatModelAt(getPreferredItem()).getContact());
        Jimm.getJimm().getCL().getManager().show();
    }

    public void back() {
        Jimm.getJimm().getCL().getManager().setModel(Jimm.getJimm().getCL().getUpdater().getModel());
        Jimm.getJimm().getDisplay().back(Jimm.getJimm().getCL().getManager());
    }

    public static boolean isChats(CanvasEx canvas) {
        if (canvas instanceof VirtualContactList) {
            VirtualContactList vcl = ((VirtualContactList) canvas);
            return vcl.getModel() == vcl.getUpdater().getChatModel();
        }
        return false;
    }
}