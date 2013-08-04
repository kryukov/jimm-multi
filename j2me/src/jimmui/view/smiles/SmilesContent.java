package jimmui.view.smiles;

import jimmui.ContentActionListener;
import jimmui.view.ActionListener;
import jimmui.view.icons.*;
import javax.microedition.lcdui.*;
import jimmui.view.base.*;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.07.13 12:02
 *
 * @author vladimir
 */
public class SmilesContent extends SomeContent {

    private String[] names;
    private ImageList icons;
    private String[] codes;

    private ContentActionListener listener;
    private int cols;
    private int rows;
    private int itemHeight;
    private int curCol;

    public SmilesContent(SomeContentList view, ImageList icons, String[] names, String[] codes) {
        super(view);
        this.icons = icons;
        this.names = names;
        this.codes = codes;
        setCurrentItem(0, 0);
    }


    public final void setSelectionListener(ContentActionListener listener) {
        this.listener = listener;
    }
    protected void restoring() {
        int drawWidth = view.getWidth();

        int heightSmall = Math.max(CanvasEx.minItemHeight, icons.getHeight() + 2);
        int moduloSmall = drawWidth % heightSmall;
        itemHeight = heightSmall + (moduloSmall * heightSmall / drawWidth);

        cols = drawWidth / itemHeight;
        rows = (names.length + cols - 1) / cols;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected final boolean touchItemPressed(int item, int x, int y) {
        final int pressedCol = Math.min(Math.max(x / itemHeight, 0), cols - 1);
        final int pressedRow = item;
        if ((getCurrentCol() != pressedCol) || (getCurrentRow() != pressedRow)) {
            setCurrentItem(pressedCol, pressedRow);
            return true;
        }
        return false;
    }
    protected void touchItemTaped(int item, int x, boolean isLong) {
        execJimmAction(NativeCanvas.JIMM_SELECT);
    }
    // #sijapp cond.end#

    protected final boolean isCurrentItemSelectable() {
        return false;
    }

    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        int xa = x1;
        int xb;
        int startIdx = cols * index;
        int imagesCount = icons.size();
        boolean isSelected = (getCurrentRow() == index);
        for (int i = 0; i < cols; ++i, ++startIdx) {
            if (startIdx >= names.length) break;
            int smileIdx = startIdx;

            xb = xa + itemHeight;

            if (isSelected && (i == getCurrentCol())) {
                g.setStrokeStyle(Graphics.SOLID);
                g.setThemeColor(CanvasEx.THEME_SELECTION_BACK);
                g.getGraphics().fillRoundRect(xa, y1, itemHeight - 1, h - 1, 4, 4);
            }

            if (smileIdx < imagesCount) {
                int centerX = xa + itemHeight / 2;
                int centerY = y1 + h / 2;
                try {
                    g.drawInCenter(icons.iconAt(smileIdx), centerX, centerY);
                } catch (Exception e) {
                }
            }

            if (isSelected && (i == getCurrentCol())) {
                g.setStrokeStyle(Graphics.SOLID);
                g.setThemeColor(CanvasEx.THEME_SELECTION_RECT);
                g.getGraphics().drawRoundRect(xa, y1, itemHeight - 1, h - 1, 4, 4);
            }
            xa = xb;
        }
    }

    public final int getSelectedIndex() {
        return getCurrentRow() * cols + getCurrentCol();
    }
    public final String getSelectedCode() {
        return codes[getSelectedIndex()];
    }

    private int getCurrentRow() {
        return getCurrItem();
    }
    private int getCurrentCol() {
        return curCol;
    }
    private void setCurrentItem(int col, int row) {
        curCol = col;
        setCurrentItemIndex(row);
        setCurrentItemToCaption();
        invalidate();
    }

    private void setCurrentItemToCaption() {
        int selIdx = getCurrentRow() * cols + getCurrentCol();
        if (names.length <= selIdx) return;
        ((Selector)view).setCaption(names[selIdx]);
    }

    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }

    protected final int getSize() {
        return rows;
    }

    protected void doJimmAction(int keyCode) {
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                select();
                return;

            case NativeCanvas.JIMM_BACK:
                view.back();
                return;
        }
    }
    protected boolean hasMenu() {
        return false;
    }
    protected boolean doKeyReaction(int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_RELEASED == type) {
            return true;
        }
        int newRow = getCurrentRow();
        int newCol = getCurrentCol();
        final int rowCount = getSize();
        int index;
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_FIRE:
                select();
                return true;

            case NativeCanvas.NAVIKEY_DOWN:
                newRow++;
                index = newCol + newRow * cols;
                if (index >= names.length) {
                    newRow = 0;
                    newCol = (newCol < (cols - 1)) ? newCol + 1 : 0;
                }
                break;

            case NativeCanvas.NAVIKEY_UP:
                newRow--;
                if (newRow < 0) {
                    newRow = rowCount - 1;
                    newCol = ((newCol == 0) ? cols : newCol) - 1;
                }
                break;

            case NativeCanvas.NAVIKEY_LEFT:
                if (newCol != 0) {
                    newCol--;
                } else {
                    newCol = cols - 1;
                    newRow--;
                }
                if (newRow < 0) {
                    newCol = (names.length - 1) % cols;
                    newRow = rowCount;
                }
                break;

            case NativeCanvas.NAVIKEY_RIGHT:
                if (newCol < (cols - 1)) {
                    newCol++;
                } else {
                    newCol = 0;
                    newRow++;
                }
                index = newCol + newRow * cols;
                if (index >= names.length) {
                    newCol = 0;
                    newRow = 0;
                }
                break;

            default:
                switch (keyCode) {
                    case NativeCanvas.KEY_NUM1:
                        setCurrentItem(curCol, 0);
                        break;

                    case NativeCanvas.KEY_NUM7:
                        setCurrentItem(curCol, getSize() - 1);
                        break;

                    case NativeCanvas.KEY_NUM3:
                        setCurrentItem(curCol, getCurrItem() - view.getContentHeight() / itemHeight);
                        break;

                    case NativeCanvas.KEY_NUM9:
                        setCurrentItem(curCol, getCurrItem() + view.getContentHeight() / itemHeight);
                        break;

                    default:
                        return true;
                }
                newRow = getCurrentRow();
        }

        index = newCol + newRow * cols;
        if (names.length <= index) {
            newRow--;
        }
        setCurrentItem(newCol, newRow);
        return true;
    }

    private void select() {
        if (null != listener) {
            view.back();
            listener.action(this, 0);
        }
        listener = null;
    }
}
