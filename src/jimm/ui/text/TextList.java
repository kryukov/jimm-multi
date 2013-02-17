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


package jimm.ui.text;

import jimm.ui.TextListExCommands;
import jimm.ui.base.*;
import jimm.ui.menu.*;


/**
 * Text list.
 *
 * This class store text and data of lines internally.
 * You may use it to show text with colorised lines :)
 */
public final class TextList extends ScrollableArea {
    protected TextListModel pars;
    protected TextListController controller;
    private TextListExCommands vlCommands;
    private boolean isSeparate5 = false;

    public final int getSize() {
        return (null == pars) ? 0 : pars.getSize();
    }

    public TextList(String capt) {
        super(capt);
        setSoftBarLabels("menu", null, "back", false);
    }
    public void setSeparate5(boolean separate) {
        isSeparate5 = separate;
    }
    public void setModel(TextListModel model) {
        pars = model;
        updateSoftLabels();
        unlock();
    }
    public void setModel(TextListModel model, int current) {
        pars = model;
        setCurrentItemIndex(current);
        updateSoftLabels();
        unlock();
    }
    public void setController(TextListController controller) {
        this.controller = controller;
        controller.setList(this);
    }
    public TextListModel getModel() {
        return pars;
    }

    public TextListController getController() {
        return controller;
    }

    public final int getItemHeight(int itemIndex) {
        if (itemIndex >= getSize()) {
            return 1;
        }
        return pars.getPar(itemIndex).getHeight();
    }

    protected final boolean isCurrentItemSelectable() {
        return pars.isSelectable(getCurrItem());
    }

    // Overrides VirtualList.drawItemData
    protected final void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        pars.getPar(index).paint(pars.getFontSet(), g, 1, y1, skip, to);
    }

    protected final MenuModel getMenu() {
        return (null == controller)  ? null : controller.getMenu();
    }

    protected void doJimmAction(int keyCode) {
        controller.doJimmAction(keyCode);
    }
    private void updateSoftLabels() {
        MenuModel model = getMenu();
        String more = null;
        String ok = null;
        if (null != model) {
            more = "menu";
            ok = model.getItemText(controller.defaultCode);
        }
        setSoftBarLabels(more, ok, "back", false);
    }

    protected void restoring() {
        updateSoftLabels();
    }
    public void updateModel() {
        updateSoftLabels();
        setCurrentItemIndex(getCurrItem());
        unlock();
    }

    public final void setUpdateListener(TextListExCommands vlCommands) {
        this.vlCommands = vlCommands;
    }

    protected void beforePaint() {
        if (null != controller) {
            controller.beforePaint();
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((null != vlCommands) && (KEY_PRESSED == type)) {
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_LEFT:
                    vlCommands.onContentMove(pars, -1);
                    return;

                case NativeCanvas.NAVIKEY_RIGHT:
                    vlCommands.onContentMove(pars, +1);
                    return;
            }
        }
        if (isSeparate5) {
            if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
                if (CanvasEx.KEY_PRESSED == type) {
                    execJimmAction(('5' == keyCode)
                            ? NativeCanvas.JIMM_SELECT
                            : NativeCanvas.JIMM_ACTIVATE);
                }
                return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }
    // #sijapp cond.if modules_TOUCH is "true"#

    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        if (getWidth() / 2 < Math.abs(fromX - toX)) {
            vlCommands.onContentMove(pars, (fromX > toX) ? -1 : +1);
        }
    }
    // #sijapp cond.end#

    public final void removeFirstText() {
        int size = getSize();
        if (0 < size) {
            int top = Math.max(0, getTopOffset() - pars.getPar(0).getHeight());
            pars.removeFirst();
            setCurrentItemIndex(Math.max(0, getCurrItem() - 1));
            setTopByOffset(top);
            invalidate();
        }
    }
}