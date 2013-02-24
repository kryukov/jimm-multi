package jimm.ui.base;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 24.02.13 23:24
 *
 * @author vladimir
 */
public class MyScrollBar extends ActiveRegion {
    // #sijapp cond.if modules_TOUCH is "true"#
    private int scrollPrevTop;
    protected void stylusPressed(CanvasEx c, int x, int y) {
        scrollPrevTop = c.getScrollTop();
        GraphicsEx.showScroll = GraphicsEx.UNLIMIT_SCROLL_TIME;
    }
    protected void stylusMoved(CanvasEx c, int fromX, int fromY, int toX, int toY, boolean horizontalDirection, int type) {
        int[] scroll = c.getScroll();
        if (null == scroll) return;
        int newTopScroll = GraphicsEx.getScrollTopValue(scroll, (toY - fromY), scrollPrevTop);
        scroll[GraphicsEx.SCROLL_TOP_VALUE] = newTopScroll;
        c.setScrollTop(newTopScroll);
        GraphicsEx.showScroll = GraphicsEx.UNLIMIT_SCROLL_TIME;
    }
    public void stylusReleased() {
        GraphicsEx.showScroll();
    }
    public boolean isScroll(CanvasEx c, int x, int y) {
        int[] scroll = c.getScroll();
        if (null == scroll) return false;
        int[] location = GraphicsEx.getScrollLocation(scroll);
        if (null == location) return false;
        location[0] += scroll[GraphicsEx.SCROLL_TOP];
        return between(location[0], location[0] + location[1], y)
                && between(scroll[GraphicsEx.SCROLL_LEFT] - scroll[GraphicsEx.SCROLL_WIDTH],
                scroll[GraphicsEx.SCROLL_LEFT] + 2 * scroll[GraphicsEx.SCROLL_WIDTH],
                x);
    }
    private boolean between(int from, int to, int value) {
        return (from <= value) && (value <= to);
    }
    // #sijapp cond.end#
}
