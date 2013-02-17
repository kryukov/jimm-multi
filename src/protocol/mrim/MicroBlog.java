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
File: src/jimm/DebugLog.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Artyomov Denis
*******************************************************************************/

// #sijapp cond.if protocols_MRIM is "true" #
// #sijapp cond.if modules_MAGIC_EYE is "true" #
package protocol.mrim;

import jimm.ui.text.TextListModel;
import jimm.ui.text.TextList;
import DrawControls.icons.*;
import DrawControls.text.Parser;
import jimm.chat.message.Message;
import jimm.ui.base.CanvasEx;
import jimm.ui.base.NativeCanvas;
import java.util.Vector;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.util.*;
import protocol.*;

public final class MicroBlog extends TextListController implements TextBoxListener {
    private TextListModel model = new TextListModel();
    private Vector emails = new Vector();
    private Vector ids = new Vector();
    private Mrim mrim;
    private boolean hasNewMessage;

    public MicroBlog(Mrim mrim) {
        this.mrim = mrim;
        list = new TextList(JLocale.getString("microblog"));
        list.setModel(model);
        setDefaultCode(MENU_WRITE);
        list.setSeparate5(true);
    }

    public void activate() {
        list.setController(this);
        list.setAllToBottom();
        list.show();
    }
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.addItem("message",       MENU_WRITE);
        menu.addItem("reply",         MENU_REPLY);
        menu.addItem("user_menu",     MENU_USER_MENU);
        menu.addItem("copy_text",     MENU_COPY);
        menu.addItem("clear",         MENU_CLEAN);
        menu.setActionListener(this);
        return menu;
    }
    public Icon getIcon() {
        return hasNewMessage ? Message.msgIcons.iconAt(Message.ICON_IN_MSG_HI) : null;
    }

    private void removeOldRecords() {
        final int maxRecordCount = 50;
        while (maxRecordCount < model.getSize()) {
            ids.removeElementAt(0);
            emails.removeElementAt(0);
            list.removeFirstText();
        }
    }

    public boolean addPost(String from, String nick, String post, String postid,
            boolean reply, long gmtTime) {
        if (StringConvertor.isEmpty(post) || ids.contains(postid)) {
            return false;
        }

        String date = Util.getLocalDateString(gmtTime, false);
        Contact contact = mrim.getItemByUIN(from);
        emails.addElement(from);
        ids.addElement(postid);

        Parser par = model.createNewParser(true);
        par.useMinHeight();
        if (null != contact) {
            nick = contact.getName();
        }
        if (StringConvertor.isEmpty(nick)) {
            nick = from;
        }
        par.addText(nick, CanvasEx.THEME_MAGIC_EYE_USER, CanvasEx.FONT_STYLE_PLAIN);
        if (reply) {
            par.addText(" (reply)", CanvasEx.THEME_MAGIC_EYE_USER, CanvasEx.FONT_STYLE_PLAIN);
        }
        par.addText(" " + date + ":", CanvasEx.THEME_MAGIC_EYE_NUMBER, CanvasEx.FONT_STYLE_PLAIN);
        par.doCRLF();
        //CanvasEx.THEME_MAGIC_EYE_ACTION
        par.addTextWithSmiles(post, CanvasEx.THEME_MAGIC_EYE_TEXT, CanvasEx.FONT_STYLE_PLAIN);

        list.lock();
        model.addPar(par);
        removeOldRecords();
        if (this != Jimm.getJimm().getDisplay().getCurrentDisplay()) {
            hasNewMessage = true;
        }
        list.updateModel();
        return true;
    }

    private static final int MENU_WRITE     = 0;
    private static final int MENU_REPLY     = 1;
    private static final int MENU_COPY      = 2;
    private static final int MENU_CLEAN     = 3;
    private static final int MENU_USER_MENU = 4;

    private InputTextBox postEditor;
    private String replayTo = "";
    // FIXME: remove it
    public void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_SELECT:
                action = MENU_REPLY;
                break;

            case NativeCanvas.JIMM_ACTIVATE:
                action = MENU_WRITE;
                break;
        }
        doJimmBaseAction(action);
        switch (action) {
            case MENU_WRITE:
                list.restore();
                write("");
                break;

            case MENU_REPLY:
                String to = "";
                int cur = list.getCurrItem();
                if (cur < ids.size()) {
                    to = (String)ids.elementAt(cur);
                }
                write(to);
                break;

            case MENU_COPY:
                list.getController().copy(false);
                list.restore();
                break;

            case MENU_CLEAN:
                synchronized (this) {
                    emails.removeAllElements();
                    ids.removeAllElements();
                    model.clear();
                    list.updateModel();
                }
                list.restore();
                break;

            case MENU_USER_MENU:
                try {
                    int item = list.getCurrItem();
                    String uin = (String)emails.elementAt(item);
                    list.showMenu(ContactList.getInstance().getContextMenu(mrim,
                            mrim.createTempContact(uin)));
                } catch (Exception e) {
                }
                break;
        }
    }
    private void write(String to) {
        replayTo = StringConvertor.notNull(to);
        postEditor = new InputTextBox().create(StringConvertor.isEmpty(replayTo)
                ? "message" : "reply", 250);
        postEditor.setTextBoxListener(this);
        postEditor.show();
    }
    protected final void beforePaint() {
        if (hasNewMessage) {
            hasNewMessage = false;
            ContactList.getInstance().updateMainMenu();
        }
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        MrimConnection c = mrim.getConnection();
        if (ok && mrim.isConnected() && (null != c)) {
            String text = postEditor.getString();
            if (!StringConvertor.isEmpty(text)) {
                c.postToMicroBlog(text, replayTo);
                list.setAllToBottom();
            }
            list.restore();
        }
    }
}
// #sijapp cond.end#
// #sijapp cond.end#
