package jimmui.view.text;

import jimmui.view.TextListExCommands;
import jimmui.view.base.*;
import jimmui.view.base.touch.*;
import jimmui.view.menu.*;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.07.13 12:22
 *
 * @author vladimir
 */
public class TextContent extends SomeContent {
    protected TextListModel pars;
    private TextListExCommands vlCommands;
    private boolean isSeparate5 = false;
    protected TextListController controller;

    public TextContent(TextList textList) {
        super(textList);
    }

    public final int getSize() {
        return (null == pars) ? 0 : pars.getSize();
    }

    public void setSeparate5(boolean separate) {
        isSeparate5 = separate;
    }
    public void setModel(TextListModel model) {
        pars = model;
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

    public final void setUpdateListener(TextListExCommands vlCommands) {
        this.vlCommands = vlCommands;
    }

    protected void beforePaint() {
        if (null != controller) {
            controller.beforePaint();
        }
    }
    protected boolean doKeyReaction(int keyCode, int actionCode, int type) {
        if ((null != vlCommands) && (CanvasEx.KEY_PRESSED == type)) {
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_LEFT:
                    vlCommands.onContentMove(pars, -1);
                    return false;

                case NativeCanvas.NAVIKEY_RIGHT:
                    vlCommands.onContentMove(pars, +1);
                    return false;
            }
        }
        if (isSeparate5) {
            if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
                if (CanvasEx.KEY_PRESSED == type) {
                    execJimmAction(('5' == keyCode)
                            ? NativeCanvas.JIMM_SELECT
                            : NativeCanvas.JIMM_ACTIVATE);
                }
                return false;
            }
        }
        return super.doKeyReaction(keyCode, actionCode, type);
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, TouchState state) {
        if (state.isLong || (view.getWidth() - view.minItemHeight < x)) {
            view.showMenu(getMenu());
        } else if (state.isSecondTap) {
            execJimmAction(NativeCanvas.JIMM_SELECT);
        }
    }

    protected void stylusXMoved(TouchState state) {
        if (view.getWidth() / 2 < Math.abs(state.fromX - state.x)) {
            vlCommands.onContentMove(pars, (state.fromX > state.x) ? -1 : +1);
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

    public void setController(TextListController controller) {
        this.controller = controller;
        controller.setList((TextList) view);
        controller.setContent(this);
    }
}