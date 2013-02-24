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
import javax.microedition.lcdui.*;

import jimm.chat.ChatHistory;
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


    // Index for current item of VL
    private int currItem = 0;
    protected MyActionBar bar = new MyActionBar();
    protected MySoftBar softBar = new MySoftBar();

    // Set of fonts for quick selecting
    private Font[] fontSet;


    //! Create new virtual list with default values
    public VirtualList(String capt) {
        setCaption(capt);
        setSoftBarLabels("menu", null, "back", false);
        fontSet = GraphicsEx.chatFontSet;
        setSize(NativeCanvas.getScreenWidth(), NativeCanvas.getScreenHeight());
    }

    public final void setSoftBarLabels(String more, String ok, String back, boolean direct) {
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
    protected final int getContentHeight() {
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
        // embed.doKeyReaction
    }

    protected final int[] getScroll() {
        // scroll bar
        int[] scroll = GraphicsEx.makeVertScroll(
                (getWidth() - scrollerWidth), bar.getHeight(),
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

    protected void touchCaptionTapped(int x) {
        if (MyActionBar.CAPTION_REGION_BACK == x) {
            back();
        } else if (MyActionBar.CAPTION_REGION_MENU == x) {
            showMenu(getMenu());
        } else if (MyActionBar.CAPTION_REGION_NEW_MESSAGE == x) {
            ChatHistory.instance.showChatList(true);
        }
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

    private static MyScrollBar scrollBar = new MyScrollBar();
    protected final void stylusPressed(int x, int y) {
        if (getHeight() < y) {
            NativeCanvas.getInstance().touchControl.setRegion(softBar);
            return;
        }
        if (y < bar.getHeight()) {
            NativeCanvas.getInstance().touchControl.setRegion(bar);
            return;
        }
        if (scrollBar.isScroll(this, x, y) && (0 < GraphicsEx.showScroll)) {
            NativeCanvas.getInstance().touchControl.setRegion(scrollBar);
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
        int item = getItemByCoord(toY);
        if (0 <= item) {
            TouchControl nat = NativeCanvas.getInstance().touchControl;
            setTopByOffset(nat.prevTopY + (fromY - toY));
            invalidate();
        }
    }

    protected final void stylusTap(int x, int y, boolean longTap) {
        int item = getItemByCoord(y);
        if (0 <= item) {
            touchItemTaped(item, x, longTap);
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

    public final void setTicker(String tickerString) {
        bar.setTicker(tickerString);
        invalidate();
    }

    protected boolean isCurrentItemSelectable() {
        return true;
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
        if (this == NativeCanvas.getInstance().getCanvas()) {
            GraphicsEx.showScroll();
        }
    }
    protected void sizeChanged(int prevW, int prevH, int w, int h) {
        int delta = prevH - h;
        setTopByOffset(getTopOffset() + delta);
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

    protected final void setTopByOffset(int offset) {
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
    protected final int getFullSize() {
        return getOffset(getSize());
    }

    ///////////////////////////////////////////////////////////
    protected abstract void set_Top(int item, int offset);
    protected abstract int get_Top();
    protected abstract int get_TopOffset();
    protected void paint(GraphicsEx g) {
        beforePaint();
        int bottom = getHeight();
        boolean onlySoftBar = (bottom <= g.getClipY());
        if (!onlySoftBar) {
            int captionHeight = bar.getHeight();
            paintContent(g, captionHeight);

            bar.paint(g, this, getWidth());
        }
        if (isSoftBarShown()) {
            softBar.paint(g, this, getHeight());
        }
    }
    protected void beforePaint() {
    }
    protected void drawProgress(GraphicsEx g, int width, int height) {
    }
    protected abstract void paintContent(GraphicsEx g, int captionHeight);

    protected final int getClientHeight() {
        return getHeight() - bar.getHeight();
    }

    protected MenuModel getMenu() {
        return null;
    }
    public final void showMenu(MenuModel m) {
        if ((null != m) && (0 < m.count())) {
            new Select(m).show();
        }
    }
}