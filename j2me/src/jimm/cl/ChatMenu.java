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

package jimm.cl;

import jimm.*;
import jimmui.view.chat.Chat;
import jimmui.model.chat.ChatModel;
import jimmui.view.menu.*;

public final class ChatMenu implements SelectListener {
    public ChatMenu() {
    }

    private int getTotal() {
        return Jimm.getJimm().jimmModel.chats.size();
    }
    private ChatModel chatModelAt(int index) {
        return (ChatModel) Jimm.getJimm().jimmModel.chats.elementAt(index);
    }



    private void removeChat(ChatModel chat) {
        if (null != chat) {
            clearChat(chat);
            Chat view = Jimm.getJimm().getCL().getChat(chat);
            if ((null != view) && Jimm.getJimm().getDisplay().remove(view)) {
                Jimm.getJimm().getCL()._setActiveContact(null);
            }
            Jimm.getJimm().getCL().getUpdater().update();
        }
        if (0 == getTotal()) {
            Jimm.getJimm().getCL().showChatList(false);
        }
    }
    private void clearChat(ChatModel chat) {
        if (chat.isHuman() && !chat.getContact().isTemp()) {
            Jimm.getJimm().getChatUpdater().removeReadMessages(chat);

        } else {
            Jimm.getJimm().jimmModel.unregisterChat(chat);
        }
    }
    private void removeAll(ChatModel except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            ChatModel chat = chatModelAt(i);
            if (except == chat) continue;
            clearChat(chat);
        }
        Jimm.getJimm().getCL().getUpdater().update();
        if (0 == getTotal()) {
            Jimm.getJimm().getCL().showChatList(false);
        }
    }


    private static final int MENU_SELECT = 1;
    private static final int MENU_DEL_CURRENT_CHAT = 2;
    private static final int MENU_DEL_ALL_CHATS_EXCEPT_CUR = 3;
    private static final int MENU_DEL_ALL_CHATS = 4;

    @Override
    public void select(Select select, MenuModel menu, int cmd) {
        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(Jimm.getJimm().getCL().getUpdater().getCurrentContact());
        switch (cmd) {
            case MENU_SELECT:
                Jimm.getJimm().getChatUpdater().activate(chat);
                break;

            case MENU_DEL_CURRENT_CHAT:
                removeChat(chat);
                select.back();
                break;

            case MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                removeAll(chat);
                select.back();
                break;

            case MENU_DEL_ALL_CHATS:
                removeAll(null);
                select.back();
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
}