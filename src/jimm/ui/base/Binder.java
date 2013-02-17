/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimm.ui.base;

import jimm.ui.ActionListener;
import jimm.ui.menu.MenuModel;
import jimm.ui.menu.Select;
import jimm.ui.menu.SelectListener;

/**
 *
 * @author vladimir
 */
public class Binder implements SelectListener {
    private CanvasEx canvas;
    private ActionListener listener;
    public Binder(CanvasEx canvas) {
        this.canvas = canvas;
    }
    public Binder(ActionListener l) {
        listener = l;
    }
    public void select(Select select, MenuModel menu, int cmd) {
        if (null != canvas) {
            canvas.restore();
            canvas.execJimmAction(cmd);
        } else {
            listener.action(null, cmd);
        }
    }
}
