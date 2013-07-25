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
package jimmui.view.chat;

import jimm.cl.SysTextList;
import jimmui.Clipboard;
import jimmui.HotKeys;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
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

public final class Chat extends SomeContentList {
    private Icon[] statusIcons = new Icon[7];
    private ChatModel model;
    private boolean selectMode;
    ///////////////////////////////////////////

    ///////////////////////////////////////////
    public Chat(ChatModel model) {
        super(null);
        content = new ChatContent(this, model);
        this.model = model;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            Jimm.getJimm().getChatUpdater().storeTopPosition(model, this);
            Jimm.getJimm().getCL().showNextPrevChat(model, (fromX > toX));
        }
    }
    // #sijapp cond.end#

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

    public static final String ADDRESS = ", ";

    public final void writeMessage(String initText) {
        if (model.writable) {
            // #sijapp cond.if modules_ANDROID is "true" #
            if (true) {
                activate();
                Jimm.getJimm().getDisplay().getNativeCanvas().getInput().setText(initText);
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

    private Protocol getProtocol() {
        return model.protocol;
    }

    public void beginTyping(boolean type) {
        updateStatusIcons();
        invalidate();
    }


    protected void restoring() {
        content.setTopByOffset(content.getTopOffset());
        Jimm.getJimm().getCL().getUpdater().setCurrentContact(getContact());
        softBar.setSoftBarLabels("menu", "reply", "close", false);
        bar.setCaption(getContact().getName());
    }

    public void activate() {
        ((ChatContent)content).resetSelected();
        showTop();
        Jimm.getJimm().getCL()._setActiveContact(getContact());
    }
    // #sijapp cond.if modules_ANDROID is "true" #
    public void sendMessage(String message) {
        Jimm.getJimm().jimmModel.registerChat(model);
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

    protected void beforePaint() {
        model.resetUnreadMessages();
        updateStatusIcons();
    }

    public boolean isVisibleChat() {
        return (this == Jimm.getJimm().getDisplay().getCurrentDisplay())
                && !Jimm.getJimm().isPaused();
    }


    private Contact getContact() {
        return model.contact;
    }

    public ChatModel getModel() {
        return model;
    }
}
