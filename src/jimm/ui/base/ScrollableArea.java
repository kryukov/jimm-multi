/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimm.ui.base;

import javax.microedition.lcdui.Graphics;

/**
 *
 * @author vladimir
 */
public abstract class ScrollableArea extends VirtualList {
    // Index of top visible item
    private int topItem = 0;
    private int topOffset = 0;
    // #sijapp cond.if modules_TOUCH is "true"#
    // #sijapp cond.end#
    protected static final byte MP_ALL = 0;
    protected static final byte MP_SELECTABLE_OLNY = 1;
    private byte movingPolicy = MP_ALL;

    protected final void setMovingPolicy(byte mp) {
        movingPolicy = mp;
    }

    protected void set_Top(int item, int offset) {
        topItem = item;
        topOffset = offset;
    }
    protected final int get_Top() {
        return topItem;
    }
    protected final int get_TopOffset() {
        return topOffset;
    }
    public ScrollableArea(String title) {
        super(title);
    }

    protected final void paintContent(GraphicsEx g, int captionHeight) {
        g.setClip(0, captionHeight, getWidth(), getHeight());
        drawBack(g, captionHeight);
        drawItems(g, captionHeight);

        g.setClip(0, captionHeight, getWidth(), getHeight());
        g.drawPopup(this, captionHeight);
    }

    protected void drawEmptyItems(GraphicsEx g, int top_y) {
    }
    private void drawBack(GraphicsEx g, int top_y) {
        int height = getHeight() - top_y;
        int itemWidth = getWidth();
        // Fill background
        g.setThemeColor(THEME_BACKGROUND);
        g.fillRect(0, top_y, itemWidth, height);

        g.setClip(0, top_y, itemWidth, height);
        if (null != Scheme.backImage) {
            int offset = 0;
            if (0 < getSize()) {
                offset = Math.max(0, Scheme.backImage.getHeight() - height)
                        * getTopOffset() / getFullSize();
            }
            g.drawImage(Scheme.backImage, 0, top_y - offset,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    private void drawItems(GraphicsEx g, int top_y) {
        int size = getSize();
        int height = getHeight();
        int itemWidth = getWidth();

        if (0 == size) {
            drawEmptyItems(g, top_y);
            return;
        }

        boolean showCursor = false;
        int currentY = 0;
        int currentIndex = isCurrentItemSelectable() ? getCurrItem() : -1;

        { // background
            int offset = topOffset;
            int y = top_y;
            for (int i = topItem; i < size; ++i) {
                int itemHeight = getItemHeight(i);
                int realHeight = Math.min(itemHeight - offset, height - y + 1);
                g.setClip(0, y, itemWidth, realHeight + 1);
                g.setStrokeStyle(Graphics.SOLID);
                if (i == currentIndex) {
                    currentY = y - offset;
                    if (g.notEqualsColor(THEME_BACKGROUND, THEME_SELECTION_BACK)) {
                        g.setThemeColor(THEME_SELECTION_BACK);
                        g.fillRect(0, currentY, itemWidth - 1, itemHeight);
                    }
                    drawItemBack(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                    showCursor = true;
                } else {
                    drawItemBack(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                }
                y += itemHeight - offset;
                if (y >= height) break;
                offset = 0;
            }
            if (0 < GraphicsEx.showScroll) {
                g.setClip(0, top_y, itemWidth, height - top_y);
                g.drawVertScroll(getScroll(), THEME_SCROLL_BACK);
            }
        }

        { // Draw items
            g.setColor(0);
            int offset = topOffset;
            int y = top_y;
            for (int i = topItem; i < size; ++i) {
                int itemHeight = getItemHeight(i);
                int realHeight = Math.min(itemHeight, height - y + 1);
                g.setClip(0, y, itemWidth, realHeight + 1);
                g.setStrokeStyle(Graphics.SOLID);
                drawItemData(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                y += itemHeight - offset;
                if (y >= height) break;
                offset = 0;
            }
        }
        if (showCursor) {
            int itemHeight = getItemHeight(currentIndex);
            g.setClip(0, currentY, itemWidth, itemHeight + 1);
            g.setThemeColor(THEME_SELECTION_RECT);
            g.setStrokeStyle(Graphics.SOLID);
            g.drawSimpleRect(0, currentY, itemWidth - 1, itemHeight);
        }
    }

    protected void drawItemBack(GraphicsEx g,
            int index, int x1, int y1, int w, int h, int skip, int to) {
    }
    protected abstract void drawItemData(GraphicsEx g,
            int index, int x1, int y1, int w, int h, int skip, int to);


    //////////////////////////////////////////////////////////////////////////////////
    private int getTopVisibleItem() {
        int size = getSize();
        int cur = get_Top();
        int offset = get_TopOffset();
        if ((cur + 1 < size) && (0 < offset)) {
            int used = getItemHeight(cur) - offset;
            int height = getContentHeight() - used;
            if ((getItemHeight(cur + 1) < height) || (used < 5)) {
                cur++;
            }
        }
        return cur;
    }
    private int getBottomVisibleItem() {
        int size = getSize();
        int cur = size;
        int offset = getContentHeight() + get_TopOffset();
        for (int i = get_Top(); i < size; ++i) {
            int height = getItemHeight(i);
            if (offset < height) {
                cur = i;
                break;
            }
            offset -= height;
        }
        cur = (size == cur) ? size - 1 : Math.max(get_Top(), cur - 1);
        return cur;
    }
    //////////////////////////////////////////////////////////////////////////////////

    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((KEY_REPEATED == type) || (KEY_PRESSED == type)) {
            navigationKeyReaction(keyCode, actionCode);
        }
    }
    private void navigationKeyReaction(int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_DOWN:
                moveCursor(+1);
                invalidate();
                break;
            case NativeCanvas.NAVIKEY_UP:
                moveCursor(-1);
                invalidate();
                break;
            case NativeCanvas.NAVIKEY_FIRE:
                execJimmAction(NativeCanvas.JIMM_SELECT);
                break;
        }
        switch (keyCode) {
            case NativeCanvas.KEY_NUM1:
                setTopByOffset(0);
                setCurrentItemIndex(0);
                invalidate();
                break;

            case NativeCanvas.KEY_NUM7:
                setTopByOffset(getFullSize() - getContentHeight());
                setCurrentItemIndex(getSize() - 1);
                invalidate();
                break;

            case NativeCanvas.KEY_NUM3:
                int top = getTopVisibleItem();
                if (getCurrItem() == top) {
                    setTopByOffset(getTopOffset() - getContentHeight() * 9 / 10);
                    top = getTopVisibleItem();
                }
                setCurrentItemIndex(top);
                invalidate();
                break;

            case NativeCanvas.KEY_NUM9:
                int bottom = getBottomVisibleItem();
                if (getCurrItem() == bottom) {
                    setTopByOffset(getTopOffset() + getContentHeight() * 9 / 10);
                    bottom = getBottomVisibleItem();
                }
                setCurrentItemIndex(bottom);
                invalidate();
                break;
        }
    }

    private void moveCursor(int step) {
        int top     = getTopOffset();
        int visible = getContentHeight();
        // #sijapp cond.if modules_TOUCH is "true"#
        TouchControl nat = NativeCanvas.getInstance().touchControl;
        if (nat.touchUsed) {
            nat.touchUsed = false;
            int curr = getCurrItem();
            int current = getOffset(curr);
            if ((current + getItemHeight(curr) < top) || (top + visible < current)) {
                int offset = (step < 0) ? (top + visible - 1) : (top + 1);
                setCurrentItemIndex(getItemByOffset(offset));
                return;
            }
        }
        // #sijapp cond.end#
        int next = getCurrItem() + step;
        if (MP_SELECTABLE_OLNY == movingPolicy) {
            while (!isItemSelectable(next)) {
                next += step;
                if ((next < 0) || (getSize() <= next)) {
                    break;
                }
            }
        }
        next = Math.max(-1, Math.min(next, getSize()));
        if (0 < step) {
            if (getSize() == next) {
                int end = getFullSize() - visible;
                if (top < end) {
                    setTopByOffset(Math.min(end, top + visible / 3));
                    return;
                }
            } else {
                int nextOffset = getOffset(next);
                if (top + visible < nextOffset) {
                    setTopByOffset(top + visible / 3);
                    return;
                }
            }
        } else {
            if (-1 == next) {
                if (0 < top) {
                    setTopByOffset(Math.max(0, top - visible / 3));
                    return;
                }
            } else {
                if (getOffset(next) + getItemHeight(next) < top) {
                    setTopByOffset(top - visible / 3);
                    return;
                }
            }
        }
        if ((next < 0) || (getSize() <= next)) {
            return;
        }
        setCurrentItemIndex(next);
    }
}
