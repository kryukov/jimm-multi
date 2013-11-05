/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/DrawControls/VirtualList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Igor Palkin, Vladimir Kryukov
 *******************************************************************************/

package jimmui.view.base;
import javax.microedition.lcdui.*;

import jimm.Jimm;
import jimmui.view.UIBuilder;
import jimmui.view.base.touch.*;
import jimmui.view.menu.*;

/**
 * This class is base class of owner draw list controls
 *
 * It allows you to create list with different colors and images.
 * Base class of VirtualDrawList if Canvas, so it draw itself when
 * paint event is heppen. VirtualList have cursor controlled of
 * user
 */

public abstract class VirtualList extends CanvasEx {
    // Caption of VL
    private int topItem = 0;
    private int topOffset = 0;
    protected static final byte MP_ALL = 0;
    protected static final byte MP_SELECTABLE_ONLY = 1;
    private byte movingPolicy = MP_ALL;


    private int currItem = 0;
    protected MyActionBar bar = new MyActionBar();
    protected MySoftBar softBar = new MySoftBar();
    private static MyScrollBar scrollBar = new MyScrollBar();

    // Set of fonts for quick selecting
    private Font[] fontSet;


    //! Create new virtual list with default values
    public VirtualList(String capt) {
        setCaption(capt);
        setSoftBarLabels("menu", null, "back", false);
        fontSet = GraphicsEx.chatFontSet;
        setSize(Jimm.getJimm().getDisplay().getScreenWidth(), Jimm.getJimm().getDisplay().getScreenHeight());
    }

    protected final void setSoftBarLabels(String more, String ok, String back, boolean direct) {
        softBar.setSoftBarLabels(more, ok, back, direct);
    }

    /**
     * Request number of list elements to be shown in list.
     *
     * You must return number of list elements in successtor of
     * VirtualList. Class calls method "getSize" each time before it drawn
     */
    abstract protected int getSize();

    protected final Font[] getFontSet() {
        return fontSet;
    }

    protected final Font getDefaultFont() {
        return fontSet[FONT_STYLE_PLAIN];
    }

    protected final void setFontSet(Font[] set) {
        fontSet = set;
    }

    // returns height of draw area in pixels
    public final int getContentHeight() {
        return getHeight() - bar.getHeight() - 1;
    }

    /** Returns height of each item in list */
    protected abstract int getItemHeight(int itemIndex);

    protected void onCursorMove() {
    }

    public final int getCurrItem() {
        return currItem;
    }
    private void setCurrItem(int cItem) {
        currItem = Math.max(0, Math.min(cItem, getSize() - 1));
    }


    protected boolean isItemSelectable(int index) {
        return true;
    }


    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((CanvasEx.KEY_REPEATED == type) || (CanvasEx.KEY_PRESSED == type)) {
            navigationKeyReaction(keyCode, actionCode);
        }
    }

    protected final int[] getScroll() {
        // scroll bar
        int[] scroll = MyScrollBar.makeVertScroll(
                (getWidth() - scrollerWidth), bar.getHeight(),
                scrollerWidth, getContentHeight() + 1,
                getContentHeight(), getFullSize());
        if (null != scroll) {
            scroll[MyScrollBar.SCROLL_TOP_VALUE] = getTopOffset();
        }
        return scroll;
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected final void setScrollTop(int top) {
        setTopByOffset(top);
        invalidate();
    }
    protected final int getScrollTop() {
        return getTopOffset();
    }
    protected final int getItemByCoord(int y) {
        int size = getSize();
        // is pointing on data area
        int itemY1 = bar.getHeight() - get_TopOffset();
        if (y < itemY1) {
            for (int i = get_Top(); 0 <= i; --i) {
                if (itemY1 <= y) {
                    return i;
                }
                itemY1 -= getItemHeight(i);
            }

        } else {
            for (int i = get_Top(); i < size; ++i) {
                itemY1 += getItemHeight(i);
                if (y < itemY1) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected void touchItemTaped(int item, int x, TouchState state) {
        if (state.isLong) {
            showMenu(getMenu());
        } else if (state.isSecondTap) {
            execJimmAction(NativeCanvas.JIMM_SELECT);
        }
    }
    protected boolean touchItemPressed(int item, int x, int y) {
        touchPressed = true;
        if (getCurrItem() != item) {
            setCurrItem(item);
            onCursorMove();
            invalidate();
            return true;
        }
        return false;
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
        int item = getItemByCoord(state.y);
        if (0 <= item) {
            currItem = -1;
            state.prevTopY = getTopOffset();
            touchItemPressed(item, state.x, state.y);
            state.isSecondTap = true;
        }
    }

    protected final void stylusGeneralYMoved(TouchState state) {
        int item = getItemByCoord(state.y);
        if (0 <= item) {
            setTopByOffset(state.prevTopY + (state.fromY - state.y));
            invalidate();
        }
    }

    protected final void stylusTap(TouchState state) {
        int item = getItemByCoord(state.y);
        if (0 <= item) {
            touchItemTaped(item, state.x, state);
        }
    }
    // #sijapp cond.end#


    /**
     * Set caption text for list
     */
    public final void setCaption(String capt) {
        bar.setCaption(capt);
    }

    public final String getCaption() {
        return bar.getCaption();
    }

    ///////////////////////////////////////////////////////////
    public final void setAllToTop() {
        setTopByOffset(0);
        setCurrItem(0);
    }
    public final void setAllToBottom() {
        setCurrentItemIndex(getSize() - 1);
    }
    private void setTop(int item, int offset) {
        set_Top(item, offset);
        if (this == Jimm.getJimm().getDisplay().getNativeCanvas().getCanvas()) {
            MyScrollBar.showScroll();
        }
    }
    protected void sizeChanged(int prevW, int prevH, int w, int h) {
        boolean prev = prevH < prevW;
        boolean curr = h < w;
        if (prev != curr) {
            int delta = prevH - h;
            setTopByOffset(getTopOffset() + delta);
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
            final int contentHeight = getContentHeight();
            int maxTopHeight = getOffset(size) - contentHeight;
            top = Math.min(top, getItemByOffset(maxTopHeight));
            setTop(top, Math.max(0, getItemHeight(top) - contentHeight));

        } else {
            top = Math.min(top, size - 1);
            int height;
            int offset = getContentHeight();
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

    public final void setTopByOffset(int offset) {
        offset = Math.max(0, Math.min(offset, getFullSize() - getContentHeight()));
        int top = getItemByOffset(offset);
        setTop(top, offset - getOffset(top));
    }
    public final int getTopOffset() {
        return getOffset(get_Top()) + get_TopOffset();
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

    ///////////////////////////////////////////////////////////
    protected void paint(GraphicsEx g) {
        beforePaint();
        int bottom = getHeight();
        boolean onlySoftBar = (bottom <= g.getClipY());
        if (!onlySoftBar) {
            int captionHeight = bar.getHeight();
            paintContent(g, captionHeight, getWidth(), getHeight() - captionHeight);

            g.setClip(0, captionHeight, getWidth(), getHeight());
            g.drawPopup(this, captionHeight);

            bar.paint(g, this, getWidth());
        }
        if (isSoftBarShown()) {
            softBar.paint(g, this, getHeight());
        }
    }
    protected void beforePaint() {
    }

    protected final int getClientHeight() {
        return getHeight() - bar.getHeight();
    }

    protected MenuModel getMenu() {
        return null;
    }
    public final void showMenu(MenuModel m) {
        if ((null != m) && (0 < m.count())) {
            UIBuilder.createMenu(m).show();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////

    protected final void setMovingPolicy(byte mp) {
        movingPolicy = mp;
    }
    byte getMovingPolicy() {
        return movingPolicy;
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

    protected final void paintContent(GraphicsEx g, int top, int width, int height) {
        g.setClip(0, top, width, height);
        drawBack(g, top, width, height);
        drawItems(g, top, width, height);
    }

    protected void drawEmptyItems(GraphicsEx g, int top_y) {
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

    private void drawItems(GraphicsEx g, int top_y, int itemWidth, int height) {
        int size = getSize();
        int bottom = height + top_y;

        if (0 == size) {
            drawEmptyItems(g, top_y);
            return;
        }

        boolean showCursor = false;
        int currentY = 0;
        int currentIndex = isCurrentItemSelectable() ? getCurrItem() : -1;
        // #sijapp cond.if modules_TOUCH is "true"#
        if (touchUsed && !touchPressed) currentIndex = -1;
        // #sijapp cond.end#

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
            if (0 < MyScrollBar.showScroll) {
                g.setClip(0, top_y, itemWidth, bottom - top_y);
                MyScrollBar.paint(g, this, CanvasEx.THEME_SCROLL_BACK);
            }
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
            g.setClip(0, currentY, itemWidth, itemHeight + 1);
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
    //////////////////////////////////////////////////////////////////////////////////

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
        if (touchUsed) {
            touchUsed = false;
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
        if (VirtualList.MP_SELECTABLE_ONLY == getMovingPolicy()) {
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
}