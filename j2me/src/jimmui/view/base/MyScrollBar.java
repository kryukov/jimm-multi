package jimmui.view.base;

import javax.microedition.lcdui.Graphics;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 24.02.13 23:24
 *
 * @author vladimir
 */
public class MyScrollBar extends ActiveRegion {
    public static final int VISIBLE_SCROLL_TIME = 4 /* 0.25 sec */;
    public static final int UNLIMIT_SCROLL_TIME = 999999 * 4 /* 0.25 sec */;

    public static final int SCROLL_LEFT = 0;
    public static final int SCROLL_TOP = 1;
    public static final int SCROLL_WIDTH = 2;
    public static final int SCROLL_HEIGHT = 3;
    public static final int SCROLL_VISIBLE_ITEMS = 4;
    public static final int SCROLL_TOTAL = 5;
    public static final int SCROLL_TOP_VALUE = 6;
    public static int showScroll;

    // #sijapp cond.if modules_TOUCH is "true"#
    private int scrollPrevTop;

    protected void stylusPressed(CanvasEx c, int x, int y) {
        scrollPrevTop = c.getScrollTop();
        showScroll = UNLIMIT_SCROLL_TIME;
    }
    protected void stylusMoved(CanvasEx c, int fromX, int fromY, int toX, int toY, boolean horizontalDirection, int type) {
        int[] scroll = c.getScroll();
        if (null == scroll) return;
        int newTopScroll = getScrollTopValue(scroll, (toY - fromY), scrollPrevTop);
        scroll[SCROLL_TOP_VALUE] = newTopScroll;
        c.setScrollTop(newTopScroll);
        showScroll = UNLIMIT_SCROLL_TIME;
    }
    public void stylusReleased() {
        showScroll();
    }
    public boolean isScroll(CanvasEx c, int x, int y) {
        int[] scroll = c.getScroll();
        if (null == scroll) return false;
        int[] location = getScrollLocation(scroll);
        if (null == location) return false;
        location[0] += scroll[SCROLL_TOP];
        return between(location[0], location[0] + location[1], y)
                && between(scroll[SCROLL_LEFT] - scroll[SCROLL_WIDTH],
                scroll[SCROLL_LEFT] + 2 * scroll[SCROLL_WIDTH],
                x);
    }
    private boolean between(int from, int to, int value) {
        return (from <= value) && (value <= to);
    }
    // #sijapp cond.end#

    public static void showScroll() {
        showScroll = VISIBLE_SCROLL_TIME;
    }

    public static int[] makeVertScroll(int left, int top,
                                       int width, int height,
                                       int visible, int total) {
        int topValue = 0;
        return new int[]{left, top, width, height, visible, total, topValue};
    }

    public static int[] getScrollLocation(int[] scroll) {
        int height = scroll[SCROLL_HEIGHT];
        int len = scroll[SCROLL_VISIBLE_ITEMS];
        int total = scroll[SCROLL_TOTAL];
        int pos = Math.min(total - len, scroll[SCROLL_TOP_VALUE]);

        if ((0 == total) || (total <= len)) return null;
        int minHeight = Math.max(CanvasEx.minItemHeight,
                GraphicsEx.chatFontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight());
        int sliderSize = Math.max(minHeight, (len * height) / total);
        int scrollerY1 = pos * (height - sliderSize) / (total - len);
        return new int[]{scrollerY1, sliderSize};
    }

    private int getScrollTopValue(int[] scroll, int delta, int prevTopValue) {
        if (null == scroll) return 0;
        int height = scroll[SCROLL_HEIGHT];
        int len = scroll[SCROLL_VISIBLE_ITEMS];
        int total = scroll[SCROLL_TOTAL];
        int pos = Math.min(total - len, scroll[SCROLL_TOP_VALUE]);
        if ((0 == total) || (total <= len)) return 0;

        int minHeight = Math.max(CanvasEx.minItemHeight,
                GraphicsEx.chatFontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight());
        int sliderSize = Math.max(minHeight, (len * height) / total);
        int scrollerY1 = prevTopValue * (height - sliderSize) / (total - len);
        return (scrollerY1 + delta) * (total - len) / (height - sliderSize);
    }

    public static void paint(GraphicsEx g, CanvasEx c, byte fore) {
        int[] scroll = c.getScroll();
        int x = scroll[MyScrollBar.SCROLL_LEFT];
        int y = scroll[MyScrollBar.SCROLL_TOP];
        int width = scroll[MyScrollBar.SCROLL_WIDTH];

        int[] location = getScrollLocation(scroll);
        if (null != location) {
            location[0] += y;
            g.setStrokeStyle(Graphics.SOLID);
            g.setThemeColor(fore);
            g.fillRect(x, location[0], width - 1, location[1]);
        }
    }
}
