/*
 * MenuModel.java
 *
 * Created on 22 Январь 2010 г., 11:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.menu;

import DrawControls.icons.Icon;
import java.util.Vector;
import jimm.util.JLocale;

/**
 *
 * @author Vladimir Krukov
 */
public final class MenuModel {
    public static final int UNDEFINED_CODE   = -10000;
    public static final int DELIMITER_CODE    = -10001;
    private Vector items = new Vector();
    private SelectListener listener;
    private int selectedItemIndex;
    /** Creates a new instance of MenuModel */
    public MenuModel() {
    }

    public void setActionListener(SelectListener listener) {
        this.listener = listener;
    }
    public final void clean() {
        items.removeAllElements();
        selectedItemIndex = 0;
    }
    public void addRawItem(String promt, Icon icon, int itemCode) {
        items.addElement(new MenuItem(promt, icon, itemCode));
    }
    public void addEllipsisItem(String promt, int itemCode) {
        addRawItem(JLocale.getEllipsisString(promt), null, itemCode);
    }
    public void addItem(String promt, Icon icon, int itemCode) {
        addRawItem(JLocale.getString(promt), icon, itemCode);
    }
    public void addItem(String promt, int itemCode) {
        addRawItem(JLocale.getString(promt), null, itemCode);
    }
    public final void addSeparator() {
        if (0 < count()) {
            addRawItem(null, null, DELIMITER_CODE);
        }
    }
    public void setRawItem(int itemCode, String promt, Icon icon) {
        int i = getIndexByItemCode(itemCode);
        if (0 <= i) {
            MenuItem item = itemAt(i);
            item.text = promt;
            item.icon = icon;
        }
    }
    public void setItem(int itemCode, String promt, Icon icon) {
        setRawItem(itemCode, JLocale.getString(promt), icon);
    }
    public void exec(Select menu, int itemCode) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null == listener) {
            jimm.modules.DebugLog.panic("select listener is null");
            jimm.cl.ContactList.getInstance().activate();
            return;
        }
        // #sijapp cond.end#
        try {
            if (DELIMITER_CODE == itemCode) {
                menu.restore();
                return;
            }
            if (0 <= getIndexByItemCode(itemCode)) {
                listener.select(menu, this, itemCode);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("select", e);
            // #sijapp cond.end #
            if (null != menu) {
                menu.back();
            } else {
                jimm.cl.ContactList.getInstance().activate();
            }
        }
    }
    public final String getItemText(int itemCode) {
        int index = getIndexByItemCode(itemCode);
        return (-1 == index) ? null : itemAt(index).text;
    }
    public final void setDefaultItemCode(int itemCode) {
        selectedItemIndex = Math.max(0, getIndexByItemCode(itemCode));
    }
    int getDefaultItemIndex() {
        return selectedItemIndex;
    }

    MenuItem itemAt(int i) {
        return (MenuItem)items.elementAt(i);
    }
    public int count() {
        return items.size();
    }
    int getItemCodeByIndex(int index) {
        return ((0 <= index) && (index < count()))
                ? itemAt(index).code : MenuModel.UNDEFINED_CODE;
    }
    int getIndexByItemCode(int itemCode) {
        final int size = count();
        for (int i = 0; i < size; ++i) {
            if (itemAt(i).code == itemCode) {
                return i;
            }
        }
        return -1;
    }
}

final class MenuItem {
    public String text;
    public Icon icon;
    public int code;
    public MenuItem(String itemText, Icon itemIcon, int itemCode) {
        text = itemText;
        icon = itemIcon;
        code = itemCode;
    }
}
