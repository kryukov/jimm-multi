package jimmui.view.base;

import jimm.Jimm;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;

import javax.microedition.lcdui.Graphics;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 19:37
 *
 * @author vladimir
 */
public abstract class SomeContent {
    private int currItem;
    protected SomeContentList view;
    protected static final byte MP_ALL = 0;
    protected static final byte MP_SELECTABLE_ONLY = 1;
    private byte movingPolicy = MP_ALL;

    private int topItem = 0;
    private int topOffset = 0;

    public SomeContent(SomeContentList view) {
        this.view = view;
    }
    public SomeContent() {
    }
    void setView(SomeContentList view) {
        this.view = view;
    }

    protected abstract int getSize();

    protected abstract int getItemHeight(int itemIndex);

    protected abstract void doJimmAction(int keyCode);

    protected void drawItemBack(GraphicsEx g, int index, int selected, int x, int y, int w, int h, int skip, int to) {
    }

    protected final void execJimmAction(int keyCode) {
        doJimmAction(keyCode);
    }

    protected void invalidate() {
        jimm.Jimm.getJimm().getDisplay().getNativeCanvas().getCanvas().invalidate();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, boolean isLong) {
    }
    // #sijapp cond.end#

    protected boolean doKeyReaction(int keyCode, int actionCode, int type) {
        if ((CanvasEx.KEY_REPEATED == type) || (CanvasEx.KEY_PRESSED == type)) {
            navigationKeyReaction(keyCode, actionCode);
        }
        return true;
    }

    protected final void paintContent(GraphicsEx g, int top, int width, int height) {
        beforePaint();
        drawBack(g, top, width, height);
        if (0 == getSize()) {
            drawEmptyItems(g, top);
        } else {
            drawItems(g, top, width, height);
        }
    }

    private void drawBack(GraphicsEx g, int top, int width, int height) {
        // Fill background
        g.setThemeColor(CanvasEx.THEME_BACKGROUND);
        g.fillRect(0, top, width, height);

        g.setClip(0, top, width, height);
        if (null != Scheme.backImage) {
            int offset = 0;
            if (0 < getSize()) {
                offset = Math.max(0, Scheme.backImage.getHeight() - height)
                        * getTopOffset() / getFullSize();
            }
            g.drawImage(Scheme.backImage, 0, top - offset,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    protected void drawEmptyItems(GraphicsEx g, int top_y) {
    }

    private void drawItems(GraphicsEx g, int top_y, int itemWidth, int height) {
        int size = getSize();
        int bottom = height + top_y;

        boolean showCursor = false;
        int currentY = 0;
        int currentIndex = isCurrentItemSelectable() ? getCurrItem() : -1;

        int topItem = get_Top();
        { // background
            int offset = topOffset;
            int y = top_y;
            for (int i = topItem; i < size; ++i) {
                int itemHeight = getItemHeight(i);
                int realHeight = Math.min(itemHeight - offset, bottom - y + 1);
                g.setClip(0, y, itemWidth, realHeight + 1);
                g.setStrokeStyle(Graphics.SOLID);
                if (i == currentIndex) {
                    currentY = y - offset;
                    if (g.notEqualsColor(CanvasEx.THEME_BACKGROUND, CanvasEx.THEME_SELECTION_BACK)) {
                        g.setThemeColor(CanvasEx.THEME_SELECTION_BACK);
                        g.fillRect(0, currentY, itemWidth - 1, itemHeight);
                    }
                    drawItemBack(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                    showCursor = true;
                } else {
                    drawItemBack(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                }
                y += itemHeight - offset;
                if (y >= bottom) break;
                offset = 0;
            }
        }

        if (0 < MyScrollBar.showScroll) {
            g.setClip(0, top_y, itemWidth, bottom - top_y);
            MyScrollBar.paint(g, view, CanvasEx.THEME_SCROLL_BACK);
        }

        { // Draw items
            g.setColor(0);
            int offset = topOffset;
            int y = top_y;
            for (int i = topItem; i < size; ++i) {
                int itemHeight = getItemHeight(i);
                int realHeight = Math.min(itemHeight, bottom - y + 1);
                g.setClip(0, y, itemWidth, realHeight + 1);
                g.setStrokeStyle(Graphics.SOLID);
                drawItemData(g, i, 2, y - offset, itemWidth - 4, itemHeight, offset, realHeight);
                y += itemHeight - offset;
                if (y >= bottom) break;
                offset = 0;
            }
        }
        if (showCursor) {
            int itemHeight = getItemHeight(currentIndex);
            g.setClip(0, currentY, itemWidth, Math.min(itemHeight + 1, bottom - currentY));
            g.setThemeColor(CanvasEx.THEME_SELECTION_RECT);
            g.setStrokeStyle(Graphics.SOLID);
            g.drawSimpleRect(0, currentY, itemWidth - 1, itemHeight);
        }
    }

    protected void drawItemBack(GraphicsEx g,
                                int index, int x1, int y1, int w, int h, int skip, int to) {
    }
    protected abstract void drawItemData(GraphicsEx g,
                                         int index, int x1, int y1, int w, int h, int skip, int to);

    protected boolean isCurrentItemSelectable() {
        return true;
    }


    private void set_Top(int item, int offset) {
        topItem = item;
        topOffset = offset;
    }
    private int get_Top() {
        return topItem;
    }
    private int get_TopOffset() {
        return topOffset;
    }
    public final int getTopOffset() {
        return getOffset(get_Top()) + get_TopOffset();
    }

    public final void setTopByOffset(int offset) {
        offset = Math.max(0, Math.min(offset, getFullSize() - view.getContentHeight()));
        int top = getItemByOffset(offset);
        setTop(top, offset - getOffset(top));
    }

    protected final int getOffset(int max) {
        int height = 0;
        for (int i = 0; i < max; ++i) {
            height += getItemHeight(i);
        }
        return height;
    }
    public final int getFullSize() {
        return getOffset(getSize());
    }

    private void setTop(int item, int offset) {
        set_Top(item, offset);
        if (view == Jimm.getJimm().getDisplay().getNativeCanvas().getCanvas()) {
            MyScrollBar.showScroll();
        }
    }

    @Deprecated
    public final void setAllToTop() {
        setTopByOffset(0);
        setCurrItem(0);
    }
    @Deprecated
    public void setAllToBottom() {
        setCurrentItemIndex(getSize() - 1);
    }

    public final int getCurrItem() {
        return currItem;
    }
    protected void setCurrItem(int cItem) {
        currItem = Math.max(0, Math.min(cItem, getSize() - 1));
    }

    public final void setCurrentItemIndex(int current) {
        int last = getCurrItem();
        setCurrItem(current);
        if (getCurrItem() != last) {
            setOptimalTopItem();
            onCursorMove();
        }
    }
    protected int getItemByOffset(int offset) {
        int size = getSize();
        for (int i = 0; i < size; ++i) {
            int height = getItemHeight(i);
            if (offset < height) {
                return i;
            }
            offset -= height;
        }
        return size;
    }

    public void setCurrentItemToTop(int current) {
        setCurrItem(current);
        setTopByOffset(getFullSize());
        setOptimalTopItem();
        int top = get_Top();
        if (top == getCurrItem()) {
            setTop(top, 0);
        }
    }

    private void setOptimalTopItem() {
        int size = getSize();
        if (0 == size) {
            setTopByOffset(0);
            return;
        }
        int current = Math.max(0, getCurrItem());
        int top = get_Top();
        int topOffset = get_TopOffset();
        if (current <= top) {
            top = current;
            final int contentHeight = view.getContentHeight();
            int maxTopHeight = getOffset(size) - contentHeight;
            top = Math.min(top, getItemByOffset(maxTopHeight));
            setTop(top, Math.max(0, getItemHeight(top) - contentHeight));

        } else {
            top = Math.min(top, size - 1);
            int height;
            int offset = view.getContentHeight();
            for (int item = current; top <= item; --item) {
                height = getItemHeight(item);
                offset -= height;
                if (offset <= 0) {
                    offset = -offset;
                    if (item == current) {
                        offset = 0;
                    }
                    if (item < top) {
                    } else if ((item == top) && (offset < topOffset)) {
                    } else {
                        setTop(item, offset);
                    }
                    return;
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////
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
                setTopByOffset(getFullSize() - view.getContentHeight());
                setCurrentItemIndex(getSize() - 1);
                invalidate();
                break;

            case NativeCanvas.KEY_NUM3:
                int top = getTopVisibleItem();
                if (getCurrItem() == top) {
                    setTopByOffset(getTopOffset() - view.getContentHeight() * 9 / 10);
                    top = getTopVisibleItem();
                }
                setCurrentItemIndex(top);
                invalidate();
                break;

            case NativeCanvas.KEY_NUM9:
                int bottom = getBottomVisibleItem();
                if (getCurrItem() == bottom) {
                    setTopByOffset(getTopOffset() + view.getContentHeight() * 9 / 10);
                    bottom = getBottomVisibleItem();
                }
                setCurrentItemIndex(bottom);
                invalidate();
                break;
        }
    }

    private void moveCursor(int step) {
        int top     = getTopOffset();
        int visible = view.getContentHeight();
        // #sijapp cond.if modules_TOUCH is "true"#
        TouchControl nat = Jimm.getJimm().getDisplay().getNativeCanvas().touchControl;
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
        if (SomeContent.MP_SELECTABLE_ONLY == movingPolicy) {
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
    private int getTopVisibleItem() {
        int size = getSize();
        int cur = get_Top();
        int offset = get_TopOffset();
        if ((cur + 1 < size) && (0 < offset)) {
            int used = getItemHeight(cur) - offset;
            int height = view.getContentHeight() - used;
            if ((getItemHeight(cur + 1) < height) || (used < 5)) {
                cur++;
            }
        }
        return cur;
    }
    private int getBottomVisibleItem() {
        int size = getSize();
        int cur = size;
        int offset = view.getContentHeight() + get_TopOffset();
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

    protected boolean isItemSelectable(int index) {
        return true;
    }

    public void onCursorMove() {
        //To change body of created methods use File | Settings | File Templates.
    }

    protected final int getItemByCoord(int y) {
        int item = getItemByOffset( y + getTopOffset());
        return (item == getSize()) ? -1 : item;
    }

    protected void updateTask(long microTime) {
    }
    protected void beforePaint() {
    }
}
