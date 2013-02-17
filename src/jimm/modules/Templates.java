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
 * File: src/jimm/Templates.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Igor Palkin
 *******************************************************************************/


package jimm.modules;

import jimm.ui.text.TextList;
import jimm.ui.text.TextListModel;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.io.Storage;
import jimm.ui.ActionListener;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.util.*;

public final class Templates implements SelectListener, CommandListener {
    private final Command addCommand    = new Command(JLocale.getString("ok"), Command.OK, 1);
    private final Command editCommand   = new Command(JLocale.getString("ok"), Command.OK, 1);
    private final Command cancelCommand = new Command(JLocale.getString("back"), Command.BACK, 2);

    private ActionListener selectionListener;
    private Vector templates = new Vector();
    private String selectedTemplate;
    private TextBox templateTextbox;
    private TextList list = new TextList(JLocale.getString("templates"));

    private static final int MENU_SELECT = 0;
    private static final int MENU_ADD    = 1;
    private static final int MENU_PASTE  = 2;
    private static final int MENU_DELETE = 3;
    private static final int MENU_EDIT   = 4;

    private Templates() {
    }

    private static final Templates instance = new Templates();
    public static Templates getInstance() {
        return instance;
    }
    public void showTemplatesList(ActionListener selectionListener_) {
        selectionListener = selectionListener_;
        refreshList();
        list.show();
    }

    public final String getSelectedTemplate() {
        return selectedTemplate;
    }


    public void select(Select select, MenuModel menu, int action) {
        switch (action) {
            case MENU_SELECT:
                if (null != selectionListener) {
                    selectedTemplate = getTemlate();
                    list.back();
                    selectionListener.action(list, 0);
                }
                selectionListener = null;
                break;

            case MENU_ADD:
                templateTextbox = new TextBox(JLocale.getString("new_template"), null, 1000, TextField.ANY);
                templateTextbox.addCommand(addCommand);
                templateTextbox.addCommand(cancelCommand);
                templateTextbox.setCommandListener(this);
                Jimm.getJimm().getDisplay().show(templateTextbox);
                break;

            case MENU_EDIT:
                templateTextbox = new TextBox(JLocale.getString("new_template"), getTemlate(), 1000, TextField.ANY);
                templateTextbox.addCommand(editCommand);
                templateTextbox.addCommand(cancelCommand);
                templateTextbox.setCommandListener(this);
                Jimm.getJimm().getDisplay().show(templateTextbox);
                break;

            case MENU_PASTE:
                String text = JimmUI.getClipBoardText(false);
                templates.addElement(text);
                save();
                refreshList();
                list.restore();
                break;

            case MENU_DELETE:
                templates.removeElementAt(list.getCurrItem());
                save();
                refreshList();
                list.restore();
                break;
        }
    }

    public void commandAction(Command c, Displayable d) {
        String text = templateTextbox.getString();
        if (StringConvertor.isEmpty(text)) {
            c = cancelCommand;
        }
        if (c == addCommand) {
            templates.addElement(text);
            save();
            refreshList();

        } else if (c == editCommand) {
            templates.setElementAt(text, list.getCurrItem());
            save();
            refreshList();

        //} else if (c == cancelCommand) {
        }
        list.restore();
        templateTextbox = null;
    }

    private MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.clean();
        if (templates.size() > 0) {
            menu.addItem("select", MENU_SELECT);
            menu.addItem("delete", MENU_DELETE);
            menu.addItem("edit",   MENU_EDIT);
        }
        menu.addItem("add_new", MENU_ADD);
        if (!JimmUI.isClipBoardEmpty()) {
            menu.addItem("paste", MENU_PASTE);
        }
        menu.setDefaultItemCode(MENU_ADD);
        menu.setActionListener(this);
        return menu;
    }

    private void refreshList() {
        list.setAllToTop();
        TextListModel model = new TextListModel();
        int count = templates.size();
        for ( int i = 0; i < count; ++i) {
            model.addItem((String)templates.elementAt(i), false);
        }
        list.setModel(model);
        list.setController(new TextListController(getMenu(), MENU_SELECT));
    }

    public void load() {
        Storage storage = new Storage("rms-templates");
        try {
            storage.open(false);
            templates = storage.loadListOfString();
        } catch (Exception e) {
            templates = new Vector();
        }
        storage.close();
        // #sijapp cond.if modules_ANDROID is "true" #
        templates = new ru.net.jimm.config.Templates().load(templates);
        // #sijapp cond.end#
    }

    private void save() {
        Storage storage = new Storage("rms-templates");
        try {
            storage.delete();
            if (0 == templates.size()) {
                return;
            }
            storage.open(true);
            storage.saveListOfString(templates);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("template save", e);
            // #sijapp cond.end#
        }
        storage.close();
        // #sijapp cond.if modules_ANDROID is "true" #
        new ru.net.jimm.config.Templates().store(templates);
        // #sijapp cond.end#
    }

    private String getTemlate() {
        return (list.getSize() == 0)
                ? ""
                : (String)templates.elementAt(list.getCurrItem());
    }

    public boolean is(CanvasEx canvas) {
        return list == canvas;
    }
}

