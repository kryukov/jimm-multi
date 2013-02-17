/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimm.ui.text;

import jimm.JimmUI;
import jimm.ui.base.NativeCanvas;
import jimm.ui.menu.MenuModel;
import jimm.ui.menu.Select;
import jimm.ui.menu.SelectListener;

/**
 *
 * @author vladimir
 */
public class TextListController implements SelectListener {
    protected int defaultCode = -1;
    private MenuModel menu;
    protected TextList list;

    public TextListController() {
    }
    public TextListController(MenuModel menu, int def) {
        this.menu = menu;
        this.defaultCode = def;
    }
    void setList(TextList list) {
        this.list = list;
    }

    public final void copy(boolean all) {
        String text = all ? list.getModel().getAllText()
                : list.getModel().getParText(list.getCurrItem());
        if (null != text) {
            JimmUI.setClipBoardText(list.getCaption(), text);
        }
    }

    protected MenuModel getMenu() {
        return menu;
    }
    public final void setDefaultCode(int def) {
        defaultCode = def;
    }

    public final void setMenu(MenuModel menu, int defCode) {
        this.menu = menu;
        setDefaultCode(defCode);
        if (null != list) {
            list.updateModel();
        }
    }

    protected final void doJimmBaseAction(int keyCode) {
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                MenuModel m = getMenu();
                if ((-1 != defaultCode) && (null != m)) {
                    m.exec(null, defaultCode);
                }
                return;

            case NativeCanvas.JIMM_BACK:
                list.back();
                return;

            case NativeCanvas.JIMM_MENU:
                list.showMenu(getMenu());
                return;
        }
    }
    public final void select(Select select, MenuModel menu, int action) {
        if (null != select) {
            select.back();
        }
        doJimmAction(action);
    }
    protected void doJimmAction(int keyCode) {
        doJimmBaseAction(keyCode);
    }
    protected void beforePaint() {
    }
}
