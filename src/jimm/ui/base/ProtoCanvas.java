package jimm.ui.base;

import jimm.Jimm;
import jimm.comm.Util;
import jimm.modules.*;
import javax.microedition.lcdui.*;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.01.13 1:26
 *
 * @author vladimir
 */
public class ProtoCanvas {
    private GraphicsEx graphicsEx = new GraphicsEx();
    private CanvasEx canvas = null;
    private String time = "";
    private int softBarHeight = 0;
    private boolean showSoftBar;
    private int width;
    private int height;

    public void setCanvas(CanvasEx c) {
        canvas = c;
        showSoftBar = canvas.isSoftBarShown();
        softBarHeight = graphicsEx.getSoftBarSize();
    }
    public CanvasEx getCanvas() {
        return canvas;
    }

    void paintAllOnGraphics(Graphics g) {
        graphicsEx.setGraphics(g);
        CanvasEx c = getCanvas();
        int bottom = getWindowHeight();
        try {
            boolean onlySoft = showSoftBar && (bottom <= graphicsEx.getClipY());
            if (!onlySoft) {
                c.paint(graphicsEx);
            }
        } catch(Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("native", e);
            // #sijapp cond.end #
        }
        if (showSoftBar) {
            int h = softBarHeight;
            graphicsEx.setStrokeStyle(Graphics.SOLID);
            String[] labels = c.getSoftLabels();
            int w = width;
            if (NativeCanvas.isOldSeLike()) {
                graphicsEx.drawSoftBar(labels[1], time,
                        c.hasRightSoft() ? labels[0] : null,
                        h, w, bottom);
            } else {
                graphicsEx.drawSoftBar(labels[0], time, labels[2],
                        h, w, bottom);
            }
        }
        graphicsEx.reset();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    private static final byte TM_USE = 0;
    private static final byte TM_SOFT_BAR = 1;
    private static final byte TM_SCROLL = 2;
    private byte touchMode = TM_USE;
    private int scrollPrevTop;

    protected void stylusPressed(int x, int y) {
        touchMode = TM_USE;
        if (getWindowHeight() < y) {
            touchMode = TM_SOFT_BAR;
            NativeCanvas.getInstance().touchControl.kineticOn = false;
            return;
        }
        if (isScroll(x, y) && (0 < GraphicsEx.showScroll)) {
            touchMode = TM_SCROLL;
            scrollPrevTop = canvas.getScrollTop();
            NativeCanvas.getInstance().touchControl.kineticOn = false;
            GraphicsEx.showScroll = GraphicsEx.UNLIMIT_SCROLL_TIME;
            return;
        }
        if (TM_USE == touchMode) canvas.stylusPressed(x, y);
    }
    protected void stylusTap(int x, int y, boolean longTap) {
        if (TM_SOFT_BAR == touchMode) {
            int w = width;
            int lsoftWidth = w / 2 - (w * 10 / 100);
            int rsoftWidth = w - lsoftWidth;
            NativeCanvas nat = NativeCanvas.getInstance();
            if (x < lsoftWidth) {
                nat.emulateKey(canvas, NativeCanvas.LEFT_SOFT);

            } else if (rsoftWidth < x) {
                nat.emulateKey(canvas, NativeCanvas.RIGHT_SOFT);

            } else {
                nat.emulateKey(canvas, NativeCanvas.NAVIKEY_FIRE);
            }
        }
        if (TM_USE == touchMode) canvas.stylusTap(x, y, longTap);
    }
    protected void stylusMoved(int fromX, int fromY, int toX, int toY, boolean horizontalDirection, int type) {
        if (TM_SCROLL == touchMode) {
            doScrolling(fromY, toY);

        } else if (TM_USE == touchMode) {
            if (horizontalDirection) {
                if (TouchControl.DRAGGING == type) {
                    canvas.stylusXMoving(fromX, fromY, toX, toY);
                } else if (TouchControl.DRAGGED == type) {
                    canvas.stylusXMoved(fromX, fromY, toX, toY);
                }
            } else {
                canvas.stylusGeneralYMoved(fromX, fromY, toX, toY, type);
            }
        }
    }

    public void stylusReleased() {
        if (TM_SCROLL == touchMode) {
            GraphicsEx.showScroll();
        }
    }

    private boolean isScroll(int x, int y) {
        int[] scroll = canvas.getScroll();
        if (null == scroll) return false;
        int[] location = GraphicsEx.getScrollLocation(scroll);
        if (null == location) return false;
        location[0] += scroll[GraphicsEx.SCROLL_TOP];
        return between(location[0], location[0] + location[1], y)
                && between(scroll[GraphicsEx.SCROLL_LEFT] - scroll[GraphicsEx.SCROLL_WIDTH],
                scroll[GraphicsEx.SCROLL_LEFT] + 2 * scroll[GraphicsEx.SCROLL_WIDTH],
                x);
    }
    private void doScrolling(int fromY, int toY) {
        CanvasEx c = getCanvas();
        int[] scroll = c.getScroll();
        if (null == scroll) return;
        int newTopScroll = GraphicsEx.getScrollTopValue(scroll, (toY - fromY), scrollPrevTop);
        scroll[GraphicsEx.SCROLL_TOP_VALUE] = newTopScroll;
        c.setScrollTop(newTopScroll);
        GraphicsEx.showScroll = GraphicsEx.UNLIMIT_SCROLL_TIME;
    }
    // #sijapp cond.end#

    final int getSoftBarHeight() {
        return softBarHeight;
    }
    final void refreshClock() {
        time = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        if (showSoftBar) {
            int h = softBarHeight;
            NativeCanvas.getInstance().repaint(0, height - h, width, h);
        }
    }

    boolean is(CanvasEx canvas) {
        return this.canvas == canvas;
    }

    void setSize(int w, int h) {
        width = w;
        height = h;
    }
    private int getWindowHeight() {
        return showSoftBar ? height - softBarHeight : height;
    }

    private boolean between(int from, int to, int value) {
        return (from <= value) && (value <= to);
    }
}
