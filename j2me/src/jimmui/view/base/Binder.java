/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimmui.view.base;

import jimmui.view.ActionListener;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimmui.view.menu.SelectListener;

/**
 *
 * @author vladimir
 */
public class Binder implements SelectListener {
    private CanvasEx canvas;
    private ActionListener listener;
    private SomeContent content;
    public Binder(CanvasEx canvas) {
        this.canvas = canvas;
    }
    public Binder(SomeContent content) {
        this.content = content;
    }
    public Binder(ActionListener l) {
        listener = l;
    }
    public void select(Select select, MenuModel menu, int cmd) {
        if (null != canvas) {
            canvas.restore();
            canvas.execJimmAction(cmd);
        } else if (null != content) {
            content.getView().restore();
            content.execJimmAction(cmd);
        } else {
            listener.action(null, cmd);
        }
    }
}
