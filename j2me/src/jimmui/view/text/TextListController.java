/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimmui.view.text;

import jimmui.Clipboard;
import jimmui.view.base.NativeCanvas;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimmui.view.menu.SelectListener;

/**
 *
 * @author vladimir
 */
public class TextListController implements SelectListener {
    protected int defaultCode = -1;
    private MenuModel menu;
    protected TextList list;
    protected TextContent content;

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
                : list.getModel().getParText(list.getTextContent().getCurrItem());
        if (null != text) {
            Clipboard.setClipBoardText(text);
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
                break;

            case NativeCanvas.JIMM_BACK:
                list.back();
                break;

            case NativeCanvas.JIMM_MENU:
                list.showMenu(getMenu());
                break;
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

    public void setContent(TextContent content) {
        this.content = content;
    }
}
