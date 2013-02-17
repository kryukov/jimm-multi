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

package jimm.ui.base;
import DrawControls.icons.Icon;
import javax.microedition.lcdui.*;
import jimm.ui.menu.*;

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
    private Icon[] capImages;
    private String caption;
    private String ticker;

    private static Icon messageIcon;

    // Index for current item of VL
    private int currItem = 0;

    // Set of fonts for quick selecting
    private Font[] fontSet;

    protected static final byte MP_ALL = 0;
    protected static final byte MP_SELECTABLE_OLNY = 1;
    private byte movingPolicy = MP_ALL;


    //! Create new virtual list with default values
    public VirtualList(String capt) {
        setCaption(capt);
        setSoftBarLabels("menu", null, "back", false);
        fontSet = GraphicsEx.chatFontSet;
    }
    public static void setMessageIcon(Icon icon) {
        messageIcon = icon;
    }
    public static Icon getMessageIcon() {
        return messageIcon;
    }
    protected final void setMovingPolicy(byte mp) {
        movingPolicy = mp;
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
    protected final int getContentHeight() {
        return getHeight() - getCapHeight() - 1;
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

    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((KEY_REPEATED == type) || (KEY_PRESSED == type)) {
            navigationKeyReaction(keyCode, actionCode);
        }
    }

    protected final int[] getScroll() {
        // scroll bar
        int[] scroll = GraphicsEx.makeVertScroll(
                (getWidth() - scrollerWidth), getCapHeight(),
                scrollerWidth, getContentHeight() + 1,
                getContentHeight(), getFullSize());
        if (null != scroll) {
            scroll[GraphicsEx.SCROLL_TOP_VALUE] = getTopOffset();
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
        int itemY1 = getCapHeight() - get_TopOffset();
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

    protected void touchCaptionTapped(boolean icon) {
    }
    protected void touchItemTaped(int item, int x, boolean isLong) {
        if (isLong || NativeCanvas.getInstance().touchControl.isSecondTap) {
            execJimmAction(NativeCanvas.JIMM_SELECT);
        }
    }
    protected boolean touchItemPressed(int item, int x, int y) {
        if (getCurrItem() != item) {
            setCurrItem(item);
            onCursorMove();
            invalidate();
            return true;
        }
        return false;
    }

    protected final void stylusPressed(int x, int y) {
        if (y < getCapHeight()) {
            return;
        }
        TouchControl nat = NativeCanvas.getInstance().touchControl;
        nat.touchUsed = true;
        int item = getItemByCoord(y);
        if (0 <= item) {
            nat.prevTopY = getTopOffset();
            nat.isSecondTap = !touchItemPressed(item, x, y);
        }
    }

    protected final void stylusGeneralYMoved(int fromX, int fromY, int toX, int toY, int type) {
        if (fromY < getCapHeight()) {
            if (TouchControl.DRAGGED == type) {
                touchCaptionTapped(false);
            }
            return;
        }
        int item = getItemByCoord(toY);
        if (0 <= item) {
            TouchControl nat = NativeCanvas.getInstance().touchControl;
            setTopByOffset(nat.prevTopY + (fromY - toY));
            invalidate();
        }
    }

    protected final void stylusTap(int x, int y, boolean longTap) {
        if (y < getCapHeight()) {
            touchCaptionTapped(getWidth() - getCapHeight() < x);
            return;
        }
        int item = getItemByCoord(y);
        if (0 <= item) {
            touchItemTaped(item, x, longTap);
        }
    }
    // #sijapp cond.end#


    protected final void setCapImages(Icon[] images) {
        capImages = images;
    }

    /**
     * Set caption text for list
     */
    public final void setCaption(String capt) {
        if ((null == caption) || !caption.equals(capt)) {
            caption = capt;
        }
    }

    public final String getCaption() {
        return caption;
    }

    public final void setTicker(String tickerString) {
        if ((null != tickerString) && tickerString.equals(ticker)) {
            return;
        }
        ticker = tickerString;
        invalidate();
    }

    private int getCapHeight() {
        return GraphicsEx.calcCaptionHeight(capImages, caption);
    }

    protected boolean isCurrentItemSelectable() {
        return true;
    }

    ///////////////////////////////////////////////////////////
    public final void setAllToTop() {
        setTop(0, 0);
        setCurrItem(0);
    }
    public final void setAllToBottom() {
        setCurrentItemIndex(getSize() - 1);
    }
    private void setTop(int item, int offset) {
        set_Top(item, offset);
        if (this == NativeCanvas.getInstance().getCanvas()) {
            GraphicsEx.showScroll();
        }
    }

    private void setOptimalTopItem() {
        int size = getSize();
        if (0 == size) {
            setTop(0, 0);
            return;
        }
        int current = Math.max(0, getCurrItem());
        int top = get_Top();
        int topOffset = get_TopOffset();
        if (current <= top) {
            top = current;
            int maxTopHeight = getOffset(size) - getContentHeight();
            top = Math.min(top, getItemByOffset(maxTopHeight));
            setTop(top, Math.max(0, getItemHeight(top) - getContentHeight()));

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


    public final void setCurrentItemToTop(int index) {
        setCurrItem(index);
        setTopByOffset(getFullSize() - getClientHeight());
        setOptimalTopItem();
        int top = get_Top();
        if (top == getCurrItem()) {
            setTop(top, 0);
        }
    }

    private int getItemByOffset(int offset) {
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

    protected final void setTopByOffset(int offset) {
        offset = Math.max(0, Math.min(offset, getFullSize() - getContentHeight()));
        int top = getItemByOffset(offset);
        setTop(top, offset - getOffset(top));
    }
    public final int getTopOffset() {
        return getOffset(get_Top()) + get_TopOffset();
    }

    private int getOffset(int max) {
        int height = 0;
        for (int i = 0; i < max; ++i) {
            height += getItemHeight(i);
        }
        return height;
    }
    protected final int getFullSize() {
        return getOffset(getSize());
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
    ///////////////////////////////////////////////////////////
    protected abstract void set_Top(int item, int offset);
    protected abstract int get_Top();
    protected abstract int get_TopOffset();
    protected void paint(GraphicsEx g) {
        beforePaint();

        int captionHeight = getCapHeight();
        paintContent(g, captionHeight);

        g.setStrokeStyle(Graphics.SOLID);
        g.setClip(0, 0, getWidth(), captionHeight + 1);
        g.drawBarBack(0, captionHeight, Scheme.captionImage, getWidth());
        drawProgress(g, getWidth(), captionHeight);
        g.drawCaption(capImages, (null == ticker) ? caption : ticker,
                messageIcon, captionHeight, getWidth());
    }
    protected void beforePaint() {
    }
    protected void drawProgress(GraphicsEx g, int width, int height) {
    }
    protected abstract void paintContent(GraphicsEx g, int captionHeight);

    protected int getHeight() {
        return getScreenHeight();
    }

    protected final int getClientHeight() {
        return getScreenHeight() - getCapHeight();
    }

    protected int getWidth() {
        return getScreenWidth();
    }

    public final void showMenu(MenuModel m) {
        if ((null != m) && (0 < m.count())) {
            new Select(m).show();
        }
    }
}