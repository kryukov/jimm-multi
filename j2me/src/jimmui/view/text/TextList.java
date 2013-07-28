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
 File: src/DrawControls/TextList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package jimmui.view.text;

import jimmui.view.base.*;
import jimmui.view.menu.*;


/**
 * Text list.
 *
 * This class store text and data of lines internally.
 * You may use it to show text with colorised lines :)
 */
public final class TextList extends SomeContentList {
    public TextList(String capt) {
        super(capt);
        content = new TextContent(this);
    }
    private void updateSoftLabels() {
        MenuModel model = getTextContent().getMenu();
        String more = null;
        String ok = null;
        if (null != model) {
            more = "menu";
            ok = model.getItemText(getTextContent().controller.defaultCode);
        }
        softBar.setSoftBarLabels(more, ok, "back", false);
    }
    protected void restoring() {
        updateSoftLabels();
    }
    public void updateModel() {
        updateSoftLabels();
        content.setCurrentItemIndex(content.getCurrItem());
        unlock();
    }
    public void setModel(TextListModel model) {
        ((TextContent)content).setModel(model);
        updateSoftLabels();
        unlock();
    }
    public void setModel(TextListModel model, int current) {
        ((TextContent)content).setModel(model);
        content.setCurrentItemIndex(current);
        updateSoftLabels();
        unlock();
    }
    public TextListModel getModel() {
        return ((TextContent)content).getModel();
    }
    public TextContent getTextContent() {
        return (TextContent) content;
    }
    public void setController(TextListController controller) {
        ((TextContent)content).setController(controller);
    }

    public void setCaption(String name) {
        bar.setCaption(name);
    }
}