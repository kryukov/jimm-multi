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

import jimmui.Clipboard;
import jimmui.ContentActionListener;
import jimmui.view.text.TextList;
import jimmui.view.text.TextListModel;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.io.Storage;
import jimmui.view.base.*;
import jimmui.view.menu.*;
import jimmui.view.text.TextListController;
import jimm.util.*;

public final class Templates implements SelectListener, CommandListener {
    private final Command addCommand    = new Command(JLocale.getString("ok"), Command.OK, 1);
    private final Command editCommand   = new Command(JLocale.getString("ok"), Command.OK, 1);
    private final Command cancelCommand = new Command(JLocale.getString("back"), Command.BACK, 2);

    public static final String TEMPLATE_STORAGE = "rms-templates";

    private ContentActionListener selectionListener;
    private Vector<String> templates = new Vector<String>();
    private String selectedTemplate;
    private TextBox templateTextBox;
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
    public void showTemplatesList(ContentActionListener selectionListener_) {
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
                    selectedTemplate = getTemplate();
                    list.back();
                    selectionListener.action(list.getTextContent(), 0);
                }
                selectionListener = null;
                break;

            case MENU_ADD:
                templateTextBox = new TextBox(JLocale.getString("new_template"), null, 1000, TextField.ANY);
                templateTextBox.addCommand(addCommand);
                templateTextBox.addCommand(cancelCommand);
                templateTextBox.setCommandListener(this);
                Jimm.getJimm().getDisplay().show(templateTextBox);
                break;

            case MENU_EDIT:
                templateTextBox = new TextBox(JLocale.getString("new_template"), getTemplate(), 1000, TextField.ANY);
                templateTextBox.addCommand(editCommand);
                templateTextBox.addCommand(cancelCommand);
                templateTextBox.setCommandListener(this);
                Jimm.getJimm().getDisplay().show(templateTextBox);
                break;

            case MENU_PASTE:
                String text = Clipboard.getClipBoardText();
                templates.addElement(text);
                save();
                refreshList();
                list.restore();
                break;

            case MENU_DELETE:
                templates.removeElementAt(list.getTextContent().getCurrItem());
                save();
                refreshList();
                list.restore();
                break;
        }
    }

    public void commandAction(Command c, Displayable d) {
        String text = templateTextBox.getString();
        if (StringUtils.isEmpty(text)) {
            c = cancelCommand;
        }
        if (c == addCommand) {
            templates.addElement(text);
            save();
            refreshList();

        } else if (c == editCommand) {
            templates.setElementAt(text, list.getTextContent().getCurrItem());
            save();
            refreshList();

        //} else if (c == cancelCommand) {
        }
        list.restore();
        templateTextBox = null;
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
        if (!Clipboard.isClipBoardEmpty()) {
            menu.addItem("paste", MENU_PASTE);
        }
        menu.setDefaultItemCode(MENU_ADD);
        menu.setActionListener(this);
        return menu;
    }

    private void refreshList() {
        list.getTextContent().setAllToTop();
        TextListModel model = new TextListModel();
        int count = templates.size();
        for ( int i = 0; i < count; ++i) {
            model.addItem((String)templates.elementAt(i), false);
        }
        list.setModel(model);
        list.setController(new TextListController(getMenu(), MENU_SELECT));
    }

    public void load() {
        templates = Storage.loadListOfString(TEMPLATE_STORAGE);
    }

    private void save() {
        Storage.saveListOfString(TEMPLATE_STORAGE, templates);
    }

    private String getTemplate() {
        return (list.getTextContent().getSize() == 0)
                ? ""
                : (String)templates.elementAt(list.getTextContent().getCurrItem());
    }

    public boolean is(SomeContent canvas) {
        return list.getTextContent() == canvas;
    }
}

