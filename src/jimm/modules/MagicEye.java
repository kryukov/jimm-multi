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

// #sijapp cond.if modules_MAGIC_EYE is "true" #
package jimm.modules;

import jimm.ui.text.TextListModel;
import jimm.ui.text.TextList;
import DrawControls.text.Parser;
import java.util.Vector;
import jimm.Jimm;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.ui.base.CanvasEx;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.util.*;
import protocol.*;

public final class MagicEye implements SelectListener {
    private static final MagicEye instance = new MagicEye();
    private TextListModel model = new TextListModel();
    private TextList list = new TextList(JLocale.getString("magic eye"));
    private Vector uins = new Vector();
    private Vector protocols = new Vector();

    private MagicEye() {
        list.setModel(model);
    }

    public static void activate() {
        MenuModel menu = new MenuModel();
        menu.addItem("user_menu",     MENU_USER_MENU);
        menu.addItem("copy_text",     MENU_COPY);
        menu.addItem("copy_all_text", MENU_COPY_ALL);
        menu.addItem("clear",         MENU_CLEAN);
        menu.setActionListener(instance);
        menu.setDefaultItemCode(MENU_COPY);
        instance.list.setController(new TextListController(menu, MENU_USER_MENU));

        instance.list.setAllToBottom();
        instance.list.show();
    }

    private synchronized void registerAction(Protocol protocol, String userId,
            String action, String msg) {
        uins.addElement(userId);
        protocols.addElement(protocol);

        String date = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        action = JLocale.getString(action);
        Contact contact = protocol.getItemByUIN(userId);

        Parser record = model.createNewParser(true);
        record.useMinHeight();
        record.addText(date + ": ", CanvasEx.THEME_MAGIC_EYE_NUMBER, CanvasEx.FONT_STYLE_PLAIN);
        if (null == contact) {
            record.addText(userId, CanvasEx.THEME_MAGIC_EYE_NL_USER, CanvasEx.FONT_STYLE_PLAIN);
        } else {
            record.addText(contact.getName(),
                    CanvasEx.THEME_MAGIC_EYE_USER, CanvasEx.FONT_STYLE_PLAIN);
        }
        record.doCRLF();
        record.addText(action, CanvasEx.THEME_MAGIC_EYE_ACTION, CanvasEx.FONT_STYLE_PLAIN);
        if (null != msg) {
            record.doCRLF();
            record.addText(msg, CanvasEx.THEME_MAGIC_EYE_TEXT, CanvasEx.FONT_STYLE_PLAIN);
        }

        model.addPar(record);
        removeOldRecords();
        list.updateModel();
    }

    private void removeOldRecords() {
        final int maxRecordCount = 50;
        while (maxRecordCount < model.getSize()) {
            protocols.removeElementAt(0);
            uins.removeElementAt(0);
            list.removeFirstText();
        }
    }

    public static void addAction(Protocol protocol, String userId, String action, String msg) {
        instance.registerAction(protocol, userId, action, msg);
    }

    public static void addAction(Protocol protocol, String userId, String action) {
        instance.registerAction(protocol, userId, action, null);
    }

    private static final int MENU_COPY      = 0;
    private static final int MENU_COPY_ALL  = 1;
    private static final int MENU_CLEAN     = 2;
    private static final int MENU_USER_MENU = 3;

    public void select(Select select, MenuModel menu, int action) {
        switch (action) {
            case MENU_COPY:
            case MENU_COPY_ALL:
                list.getController().copy(MENU_COPY_ALL == action);
                list.restore();
                break;

            case MENU_CLEAN:
                synchronized (instance) {
                    uins.removeAllElements();
                    protocols.removeAllElements();
                    model.clear();
                    list.updateModel();
                }
                list.restore();
                break;

            case MENU_USER_MENU:
                try {
                    int item = list.getCurrItem();
                    String userId = (String)uins.elementAt(item);
                    Protocol protocol = (Protocol)protocols.elementAt(item);
                    list.showMenu(ContactList.getInstance().getContextMenu(protocol,
                            protocol.createTempContact(userId)));
                } catch (Exception e) {
                    list.restore();
                }
                return;
        }
    }
}
// #sijapp cond.end#