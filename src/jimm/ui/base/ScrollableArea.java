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
}
