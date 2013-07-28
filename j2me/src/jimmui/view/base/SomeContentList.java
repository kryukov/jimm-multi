package jimmui.view.base;

import jimm.Jimm;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;

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

    public SomeContentList(String capt) {
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

    // #sijapp cond.if modules_TOUCH is "true"#
    protected final void touchItemTaped(int item, int x, boolean isLong) {
        content.touchItemTaped(item, x, isLong);
    }
    protected final boolean touchItemPressed(int item, int x, int y) {
        if (content.getCurrItem() != item) {
            content.setCurrItem(item);
            content.onCursorMove();
            invalidate();
            return true;
        }
        return false;
    }

    protected final void stylusPressed(int x, int y) {
        if (getHeight() < y) {
            Jimm.getJimm().getDisplay().getNativeCanvas().touchControl.setRegion(softBar);
            return;
        }
        if (y < bar.getHeight()) {
            Jimm.getJimm().getDisplay().getNativeCanvas().touchControl.setRegion(bar);
            return;
        }
        TouchControl nat = Jimm.getJimm().getDisplay().getNativeCanvas().touchControl;
        nat.touchUsed = true;
        int item = content.getItemByCoord(y - bar.getHeight());
        if (0 <= item) {
            nat.prevTopY = content.getTopOffset();
            touchItemPressed(item, x, y);
            nat.isSecondTap = true;
        }
    }

    protected final void stylusGeneralYMoved(int fromX, int fromY, int toX, int toY, int type) {
        int item = content.getItemByCoord(toY - bar.getHeight());
        if (0 <= item) {
            TouchControl nat = Jimm.getJimm().getDisplay().getNativeCanvas().touchControl;
            content.setTopByOffset(nat.prevTopY + (fromY - toY));
            invalidate();
        }
    }

    protected final void stylusTap(int x, int y, boolean longTap) {
        int item = content.getItemByCoord(y - bar.getHeight());
        if (0 <= item) {
            touchItemTaped(item, x, longTap);
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
            int captionHeight = bar.getHeight();
            g.getGraphics().translate(0, captionHeight);
            g.setClip(0, 0, getWidth(), bottom - captionHeight);
            content.paintContent(g, 0, getWidth(), bottom - captionHeight);
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
            new Select(m).show();
        }
    }

    public final SomeContent getContent() {
        return content;
    }

    protected void updateTask(long microTime) {
        content.updateTask(microTime);
    }
}
