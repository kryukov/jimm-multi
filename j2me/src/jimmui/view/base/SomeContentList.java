package jimmui.view.base;

import jimm.Jimm;
import jimmui.view.UIBuilder;
import jimmui.view.base.touch.*;
import jimmui.view.menu.MenuModel;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 19:36
 *
 * @author vladimir
 */
public class SomeContentList extends CanvasEx {
    protected MyActionBar bar = new MyActionBar();
    protected MySoftBar softBar = new MySoftBar();
    private static MyScrollBar scrollBar = new MyScrollBar();
    protected SomeContent content;

    public SomeContentList() {
        // #sijapp cond.if modules_ANDROID isnot "true"#
        bar.setCaption(null);
        softBar.setSoftBarLabels("menu", null, "back", false);
        setSize(Jimm.getJimm().getDisplay().getScreenWidth(),
                Jimm.getJimm().getDisplay().getScreenHeight());
        // #sijapp cond.end#
    }

    public SomeContentList(String capt) {
        bar.setCaption(capt);
        softBar.setSoftBarLabels("menu", null, "back", false);
        setSize(Jimm.getJimm().getDisplay().getScreenWidth(),
                Jimm.getJimm().getDisplay().getScreenHeight());
    }

    public SomeContentList(SomeContent content, String capt) {
        this.content = content;
        content.setView(this);
        bar.setCaption(capt);
        softBar.setSoftBarLabels("menu", null, "back", false);
        setSize(Jimm.getJimm().getDisplay().getScreenWidth(),
                Jimm.getJimm().getDisplay().getScreenHeight());
    }

    @Override
    protected final void doJimmAction(int keyCode) {
        content.doJimmAction(keyCode);
    }

    public final int getContentHeight() {
        return getHeight() - bar.getHeight() - 1;
    }

    protected void sizeChanged(int prevW, int prevH, int w, int h) {
        boolean prev = prevH < prevW;
        boolean curr = h < w;
        if (prev != curr) {
            int delta = prevH - h;
            content.setTopByOffset(content.getTopOffset() + delta);
        }
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected final void touchItemTaped(int item, int x, TouchState state) {
        content.touchItemTaped(item, x, state);
    }
    protected void stylusXMoved(TouchState state) {
        content.stylusXMoved(state);
    }
    protected final boolean touchItemPressed(int item, int x, int y) {
        return content.touchItemPressed(item, x, y);
    }

    protected final void stylusPressed(TouchState state) {
        if (getHeight() < state.y) {
            state.region = softBar;
            return;
        }
        if (state.y < bar.getHeight()) {
            state.region = bar;
            return;
        }
        touchUsed = true;
        touchPressed = true;
        int item = content.getItemByCoord(state.y - bar.getHeight());
        if (0 <= item) {
            content.currItem = -1;
            state.prevTopY = content.getTopOffset();
            touchItemPressed(item, state.x, state.y);
            state.isSecondTap = true;
        }
    }

    protected final void stylusGeneralYMoved(TouchState state) {
        int item = content.getItemByCoord(state.y - bar.getHeight());
        if (0 <= item) {
            content.setTopByOffset(state.prevTopY + (state.fromY - state.y));
            invalidate();
        }
    }

    protected final void stylusTap(TouchState state) {
        int item = content.getItemByCoord(state.y - bar.getHeight());
        if (0 <= item) {
            touchItemTaped(item, state.x, state);
        }
    }
    // #sijapp cond.end#


    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        if (content.doKeyReaction(keyCode, actionCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    protected int[] getScroll() {
        // scroll bar
        int[] scroll = MyScrollBar.makeVertScroll(
                (getWidth() - scrollerWidth), 0,//bar.getHeight(),
                scrollerWidth, getContentHeight() + 1,
                getContentHeight(), content.getFullSize());
        if (null != scroll) {
            scroll[MyScrollBar.SCROLL_TOP_VALUE] = content.getTopOffset();
        }
        return scroll;
    }

    protected void setScrollTop(int top) {
        content.setTopByOffset(top);
        invalidate();
    }
    protected int getScrollTop() {
        return content.getTopOffset();
    }

    @Override
    protected void paint(GraphicsEx g) {
        int bottom = getHeight();
        boolean onlySoftBar = (bottom <= g.getClipY());
        if (!onlySoftBar) {
            content.beforePaint();
            int captionHeight = bar.getHeight();
            g.getGraphics().translate(0, captionHeight);
            try {
                g.setClip(0, 0, getWidth(), bottom - captionHeight);
                content.paintContent(g, 0, getWidth(), bottom - captionHeight);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("content", e);
                // #sijapp cond.end #
            }
            g.getGraphics().translate(0, -captionHeight);

            g.setClip(0, captionHeight, getWidth(), getHeight());
            g.drawPopup(this, captionHeight);

            bar.paint(g, this, getWidth());
        }
        if (isSoftBarShown()) {
            softBar.paint(g, this, getHeight());
        }
    }

    public final void showMenu(MenuModel m) {
        if ((null != m) && (0 < m.count())) {
            UIBuilder.createMenu(m).show();
        }
    }

    public final SomeContent getContent() {
        return content;
    }

    protected void updateTask(long microTime) {
        content.updateTask(microTime);
    }
}
