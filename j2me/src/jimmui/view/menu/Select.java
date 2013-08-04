/*
 * Select.java
 *
 * Created on 22 Июнь 2007 г., 15:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.menu;

import jimm.Jimm;
import jimmui.view.base.touch.*;
import jimmui.view.icons.*;
import javax.microedition.lcdui.*;
import jimmui.view.base.*;

import java.util.Vector;

/**
 * @author vladimir
 */
public final class Select extends CanvasEx {
    private static final int ICON_INTERVAL = 2;
    private static final int WIDTH_SPACE = 6;

    private static final int SLEEP_BEFORE = 2000 / NativeCanvas.UIUPDATE_TIME;
    private static final int SLEEP_AFTER = 1000 / NativeCanvas.UIUPDATE_TIME;
    private static final int STEP = 10;
    private static final int EMPTY_WIDTH = 5 * STEP;

    private MenuModel items;
    private int topItem;
    private int selectedItemIndex;

    private int selectedItemPosX;
    private int sleep;

    private int left;
    private int top;
    private int width;

    private int itemPerPage;

    private int itemHeight;
    private int itemWidth;
    private int iconWidth;
    public MySoftBar softBar = new MySoftBar();

    private int getHeadSpace() {
        return Math.max(3, itemHeight / 4);
    }

    protected final int[] getScroll() {
        // scroll bar
        int[] scroll = MyScrollBar.makeVertScroll(
                left + itemWidth - scrollerWidth, top, scrollerWidth + 1,
                (itemPerPage) * itemHeight + 1 + 2 * getHeadSpace(),
                itemPerPage, items.count());
        if (null != scroll) {
            scroll[MyScrollBar.SCROLL_TOP_VALUE] = topItem;
        }
        return scroll;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    private int getItemStartY() {
        return topItem * itemHeight + getHeadSpace();
    }
    private int getItemByCoord(int relativeY) {
        relativeY -= getHeadSpace();
        final int size = items.count();
        for (int i = topItem; i < size; ++i) {
            if (relativeY < itemHeight) {
                return i;
            }
            relativeY -= itemHeight;
        }
        return -1;
    }

    private boolean checkRegion(int fromX, int fromY) {
        int relativeX = fromX - left;
        int relativeY = fromY - top - getHeadSpace();
        int curHeight = itemPerPage * itemHeight;
        int curWidth = itemWidth;
        return (relativeX >= 0) && (relativeX < curWidth)
                && (relativeY >= 0) && (relativeY < curHeight);
    }
    private boolean isItemsRegion(int absoluteX) {
        return (absoluteX - left) < itemWidth;
    }

    protected void stylusPressed(TouchState state) {
        if (getHeight() < state.y) {
            state.region = softBar;
            return;
        }
        if (!checkRegion(state.x, state.y)) {
            return;
        }
        int posY = state.y - top;

        if (isItemsRegion(state.x)) { // menu items
            state.prevTopY = getItemStartY();
            int cur = getItemByCoord(posY);
            if (-1 != cur) {
                setSelectedItemIndex(cur);
            }
        }
    }
    private void setTop(int pos) {
        int newTop = Math.max(0, Math.min(pos, items.count() - itemPerPage));

        boolean update = (newTop != topItem);
        topItem = newTop;
        if (update) {
            invalidate();
        }
    }
    protected void setScrollTop(int top) {
        setTop(top);
    }
    protected int getScrollTop() {
        return topItem;
    }

    protected void stylusGeneralYMoved(TouchState state) {
        if (checkRegion(state.fromX, state.fromY) && isItemsRegion(state.fromX)) {
            setTop((state.prevTopY - state.y + state.fromY + getHeadSpace()) / itemHeight);
        }
    }

    protected void stylusTap(TouchState state) {
        if (checkRegion(state.x, state.y)) {
            if (isItemsRegion(state.x)) {
                int posY = state.y - this.top;
                int cur = getItemByCoord(posY);
                if (-1 != cur) {
                    setSelectedItemIndex(getIndex(cur, 0));
                    go(items.itemAt(cur).code);
                }
            }
        } else {
            back();
        }
    }
    // #sijapp cond.end#

    public Select(MenuModel menu) {
        setModel(menu);
        softBar.setSoftBarLabels("select", "select", "back", false);
    }

    private void calcMetrix(int screenWidth) {
        Font menuFont = GraphicsEx.menuFont;
        itemHeight = menuFont.getHeight();
        int textWidth = 0;
        iconWidth = 0;

        final int size = items.count();
        for (int i = 0; i < size; ++i) {
            MenuItem item = items.itemAt(i);
            if (null == item.text) continue;
            Icon icon = item.icon;
            if (null != icon) {
                itemHeight = Math.max(itemHeight, icon.getHeight());
                iconWidth  = Math.max(iconWidth,  icon.getWidth());
            }
            textWidth = Math.max(textWidth, menuFont.stringWidth(item.text));
        }
        textWidth = Math.max(screenWidth * 2 / 5, textWidth);
        itemHeight = Math.max(itemHeight, CanvasEx.minItemHeight);
        int emptySpaceWidth = Math.max(scrollerWidth, CanvasEx.minItemHeight / 3);

        if (0 < iconWidth) {
            iconWidth = Math.max(iconWidth, itemHeight);
        } else {
            iconWidth = emptySpaceWidth - ICON_INTERVAL;
        }

        int _itemWidth = textWidth + iconWidth + ICON_INTERVAL + emptySpaceWidth;
        int maxItemWidth = screenWidth - (WIDTH_SPACE + emptySpaceWidth);
        _itemWidth = between(_itemWidth, CanvasEx.minItemWidth, maxItemWidth);
        final int prevWidth = prevMenuWidth();
        if (prevWidth == _itemWidth) {
            _itemWidth += emptySpaceWidth - ICON_INTERVAL;
            _itemWidth = between(_itemWidth, CanvasEx.minItemWidth, maxItemWidth);
            if (prevWidth == _itemWidth) {
                _itemWidth -= emptySpaceWidth - ICON_INTERVAL;
                _itemWidth = between(_itemWidth, CanvasEx.minItemWidth, maxItemWidth);
            }
        }
        itemWidth = _itemWidth;
    }
    private int prevMenuWidth() {
        Object last = jimm.Jimm.getJimm().getDisplay().getCurrentDisplay();
        if (this == last) {
            Vector v = jimm.Jimm.getJimm().getDisplay().getStack();
            last = (0 < v.size()) ? v.elementAt(v.size() - 1) : null;
        }
        if (last instanceof Select) {
            return ((Select)last).itemWidth;
        }
        return 0;
    }

    protected void showing() {
        selectedItemIndex = items.getDefaultItemIndex();
        selectedItemPosX = 0;
        sleep = 0;
    }
    private int between(int x, int min, int max) {
        return Math.max(min, Math.min(x, max));
    }
    protected void restoring() {
        selectedItemPosX = 0;
        sleep = 0;
        int screenHeight = getScreenHeight();
        int screenWidth = getScreenWidth();
        calcMetrix(screenWidth);

        itemPerPage = Math.min(screenHeight / itemHeight - 1, items.count());
        width  = between(itemWidth, screenWidth / 3, screenWidth - 10);
        left = (screenWidth - width) / 2;
        int height = itemHeight * itemPerPage + 2 * getHeadSpace();
        top = (screenHeight - height) / 3;

        setSelectedItemIndex(selectedItemIndex);
        relocateTop();
    }

    public void update() {
        restoring();
        invalidate();
    }

    public final void setModel(MenuModel model) {
        items = model;
    }
    public int getSelectedItemCode() {
        return items.getItemCodeByIndex(selectedItemIndex);
    }


    protected void paint(GraphicsEx g) {
        final int size = items.count();
        final boolean hasScroll = (size > itemPerPage);

        // get top item
        final int curWidth = this.width;
        final int curHeight = itemHeight * itemPerPage + 2 * getHeadSpace();
        final int currentIndex = selectedItemIndex;

        int y = this.top;
        int x = this.left;
        paintBack(g);
        g.setStrokeStyle(Graphics.SOLID);
        // #sijapp cond.if modules_ANDROID is "true" #
        g.setClip(x, y, curWidth, curHeight);
        g.setThemeColor(THEME_MENU_BACK);
        g.getGraphics().fillRoundRect(x, y, curWidth, curHeight, getHeadSpace(), getHeadSpace());
        g.setThemeColor(THEME_MENU_BORDER);
        g.getGraphics().drawRoundRect(x, y, curWidth, curHeight, getHeadSpace(), getHeadSpace());
        // #sijapp cond.else #
        g.fillRect(x, y, curWidth, curHeight, THEME_MENU_BACK);
        g.drawDoubleBorder(x, y, curWidth, curHeight, THEME_MENU_BORDER);
        // #sijapp cond.end #
        g.setClip(x, y, curWidth + 1, curHeight + 1);
        y += getHeadSpace();
        paintItems(g, x, y, itemPerPage, currentIndex);
        y -= getHeadSpace();
        g.setClip(x, y, curWidth + 1, curHeight + 1);
        if (hasScroll) {
            MyScrollBar.paint(g, this, THEME_MENU_BORDER);
        }
        if (isSoftBarShown()) {
            softBar.paint(g, this, getHeight());
        }
    }

    private void paintItems(GraphicsEx g, int baseX, int baseY, int count, int currentIndex) {
        Font menuFont = GraphicsEx.menuFont;
        final int textWidth = itemWidth - (iconWidth + ICON_INTERVAL);

        int iconX  = baseX + iconWidth / 2;
        int iconY  = baseY + itemHeight / 2;
        int promtX = baseX + iconWidth + ICON_INTERVAL;
        int promtY = baseY + itemHeight / 2 - menuFont.getHeight() / 2;

        g.setFont(menuFont);
        for (int i = topItem; count > 0; ++i, --count) {
            g.setClip(baseX, baseY, itemWidth + 1, itemHeight + 1);
            if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_BACK);
                int capBkColor = g.getThemeColor(THEME_MENU_SEL_BACK);
                g.fillGradRect(capBkColor, g.transformColorLight(capBkColor, -32),
                        baseX, baseY, itemWidth, itemHeight);
            }
            MenuItem item = items.itemAt(i);
            g.drawInCenter(item.icon, iconX, iconY);
            g.setClip(baseX, baseY - 1, itemWidth, itemHeight + 2);
            if (null == item.text) {
                int posY = baseY + itemHeight / 2;
                g.setThemeColor(THEME_MENU_BORDER);
                g.drawLine(baseX, posY, baseX + itemWidth, posY);
                g.drawLine(baseX, posY + 1, baseX + itemWidth, posY + 1);

            } else if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_TEXT);
                g.drawString(item.text, promtX - selectedItemPosX, promtY, Graphics.TOP | Graphics.LEFT);

            } else {
                g.setThemeColor(THEME_MENU_TEXT);
                g.drawString(item.text, promtX, promtY, Graphics.TOP | Graphics.LEFT);
            }
            baseY += itemHeight;
            iconY += itemHeight;
            promtY += itemHeight;
        }
    }
    private int getIndex(int currentIndex, int moveTo) {
        return Math.max(Math.min(currentIndex + moveTo, items.count() - 1), 0);
    }
    private void setSelectedItemIndex(int index) {
        index = getIndex(index, 0);
        if (index != selectedItemIndex) {
            selectedItemIndex = index;
            selectedItemPosX = 0;
            sleep = 0;
            invalidate();
        }
    }
    private void relocateTop() {
        final int size = items.count();
        final boolean hasScroll = (size > itemPerPage);
        int oldTop = topItem;
        if (hasScroll) {
            int newTop = topItem;
            newTop = Math.max(newTop, getIndex(selectedItemIndex, 1 + 1 - itemPerPage));
            newTop = Math.min(newTop, getIndex(size, -itemPerPage));
            newTop = Math.min(newTop, getIndex(selectedItemIndex, -1));
            topItem = newTop;
        } else {
            topItem = 0;
        }
        if (oldTop != topItem) {
            invalidate();
        }
    }

    private void nextPrevItem(boolean next) {
        final int size = items.count();
        setSelectedItemIndex((selectedItemIndex + (next ? 1 : size - 1)) % size);
        relocateTop();
    }


    protected void doJimmAction(int keyCode) {
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                // #sijapp cond.if modules_ANDROID is "true" #
                back();
                // #sijapp cond.else #
                go(getSelectedItemCode());
                // #sijapp cond.end #
                break;
            case NativeCanvas.JIMM_BACK:
                back();
                break;
        }
    }
    protected boolean hasMenu() {
        return false;
    }
    protected void doKeyReaction(int keyCode, int gameAct, int type) {
        if (KEY_RELEASED == type) {
            return;
        }
        switch (gameAct) {
            case NativeCanvas.NAVIKEY_DOWN:
            case NativeCanvas.NAVIKEY_UP:
                nextPrevItem(NativeCanvas.NAVIKEY_DOWN == gameAct);
                return;
        }
        if (KEY_PRESSED != type) {
            return;
        }
        switch (keyCode) {
            case NativeCanvas.CLEAR_KEY:
                execJimmAction(NativeCanvas.JIMM_BACK);
                return;
            case NativeCanvas.KEY_NUM1:
                setSelectedItemIndex(0);
                relocateTop();
                return;
            case NativeCanvas.KEY_NUM7:
                setSelectedItemIndex(items.count() - 1);
                relocateTop();
                return;
            case NativeCanvas.KEY_NUM3:
            case NativeCanvas.KEY_NUM9:
                int count = itemPerPage;
                if (NativeCanvas.KEY_NUM3 == keyCode) {
                    count = -count;
                }
                setSelectedItemIndex(selectedItemIndex + count);
                relocateTop();
                return;
        }
        switch (gameAct) {
            case NativeCanvas.NAVIKEY_RIGHT: // ????
            case NativeCanvas.NAVIKEY_FIRE:
                execJimmAction(NativeCanvas.JIMM_SELECT);
                break;
            case NativeCanvas.NAVIKEY_LEFT:
                execJimmAction(NativeCanvas.JIMM_BACK);
                break;
        }
    }

    protected void updateTask(long microTime) {
        sleep++;
        if ((0 == selectedItemPosX) && (sleep < SLEEP_BEFORE)) return;
        Font menuFont = GraphicsEx.menuFont;
        int fullWidth = menuFont.stringWidth(items.itemAt(selectedItemIndex).text);
        int visWidth = itemWidth - (iconWidth + ICON_INTERVAL);
        if (fullWidth <= visWidth) return;

        if (selectedItemPosX + visWidth > (fullWidth + EMPTY_WIDTH)) {
            if (sleep < SLEEP_AFTER) return;
            selectedItemPosX = 0;
        } else {
            selectedItemPosX += STEP;
        }
        sleep = 0;
        invalidate();
    }

    private void go(int code) {
        items.exec(this, code);
    }

    public final int getScreenWidth() {
        return Jimm.getJimm().getDisplay().getScreenWidth();
    }
    public final int getScreenHeight() {
        if (isSoftBarShown()) {
            return Jimm.getJimm().getDisplay().getScreenHeight() - softBar.getHeight();
        }
        return Jimm.getJimm().getDisplay().getScreenHeight();
    }
}