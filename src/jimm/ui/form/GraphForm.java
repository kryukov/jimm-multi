/*
 * GraphForm.java
 *
 * Created on 3 Ноябрь 2010 г., 11:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.form;

import DrawControls.text.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.JLocale;

/**
 *
 * @author Vladimir Kryukov
 */
public final class GraphForm extends ScrollableArea implements TextBoxListener, SelectListener {

    private Vector controls = new Vector();
    private InputTextBox box;
    private MenuModel list;
    private Font boldFont;
    private Font normalFont;
    private Font[] fontSet;
    private FormListener formListener;
    private ControlStateListener controlListener;
    private static final byte CONTROL_TEXT = 0;
    private static final byte CONTROL_INPUT = 1;
    private static final byte CONTROL_CHECKBOX = 2;
    private static final byte CONTROL_SELECT = 3;
    private static final byte CONTROL_GAUGE = 4;
    private static final byte CONTROL_IMAGE = 5;
    private static final byte CONTROL_LINK = 6;
    private static final byte CONTROL_COLOR = 7;
    private int OFFSET = 3;
    private int BOTTOM = 3;

    private static class Control {
        public int id;
        public Par description;
        public boolean disabled;
        public byte type;
        public int height;

        // input
        public int inputType;
        public String text;
        private int size;
        // checkbox
        public boolean selected;
        // select
        public String[] items;
        public int current;
        // gauge
        public int level;
        // image
        public Image image;
    }

    public GraphForm(String caption, String ok, String cancel, FormListener l) {
        super(JLocale.getString(caption));
        formListener = l;
        fontSet = GraphicsEx.contactListFontSet;
        boldFont = fontSet[FONT_STYLE_BOLD];
        normalFont = fontSet[FONT_STYLE_PLAIN];
        setMovingPolicy(MP_SELECTABLE_OLNY);
        setSoftBarLabels(ok, ok, cancel, true);
    }

    public void setControlStateListener(ControlStateListener l) {
        controlListener = l;
    }

    protected void doJimmAction(int keyCode) {
        switch (keyCode) {
            case NativeCanvas.JIMM_SELECT:
                onControlSelected();
                return;

            case NativeCanvas.JIMM_MENU:
                if (null == formListener) {
                    back();
                    return;
                }
                formListener.formAction(this, true);
                return;
            case NativeCanvas.JIMM_BACK:
                if (null == formListener) {
                    back();
                    return;
                }
                formListener.formAction(this, false);
                return;
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (KEY_RELEASED != type) {
            if (key(actionCode)) {
                return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, boolean isLong) {
        execJimmAction(NativeCanvas.JIMM_SELECT);
        if (item != getCurrItem()) {
            return;
        }
        Control c = (Control)controls.elementAt(item);
        if (c.disabled) {
            return;
        }
        if (CONTROL_GAUGE == c.type) {
            setGaugeLevel(c, x);
        }
    }
    protected void stylusXMoving(int fromX, int fromY, int toX, int toY) {
        stylusXMoved(fromX, fromY, toX, toY);
    }
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        int itemIndex = getItemByCoord(toY);
        if (itemIndex < 0) {
            return;
        }
        Control c = (Control)controls.elementAt(itemIndex);
        if (c.disabled) {
            return;
        }
        if (CONTROL_GAUGE == c.type) {
            setGaugeLevel(c, toX);
        }
    }
    private void setGaugeLevel(Control c, int x) {
        final int cursorBorder = 1;
        x -= cursorBorder;
        int gaugeWidth = getWidth() - 2 * (OFFSET + cursorBorder) - scrollerWidth;
        int sectionWidth = gaugeWidth / 11;

        c.level = Math.max(0, Math.min((x - OFFSET) / sectionWidth, 10));
        invalidate();
    }
    // #sijapp cond.end#

    public void destroy() {
        clearForm();
        formListener = null;
        controlListener = null;
    }
////////////////////////////////////////////////////////////////////////////////
    private Control create(int controlId, byte type, String label, String desc) {
        Control c = new Control();
        c.id = controlId;
        c.type = type;
        if ((null != label) || (null != desc)) {
            int width = getWidth() - scrollerWidth - OFFSET * 2;
            if (CONTROL_CHECKBOX == type) {
                width -= calcIconSize() + OFFSET;
            }
            Parser parser = new Parser(fontSet, width);
            if (null != label) {
                parser.addText(label, THEME_TEXT, FONT_STYLE_BOLD);
                parser.doCRLF();
            }
            if (null != desc) {
                parser.addText(desc, THEME_TEXT, FONT_STYLE_PLAIN);
            }
            c.description = parser.getPar();
        }
        return c;
    }
    public void clearForm() {
        setAllToTop();
        controls.removeAllElements();
    }
    public void addSelector(int controlId, String label, String items, int index) {
        String[] all = Util.explode(items, '|');
        for (int i = 0; i < all.length; ++i) {
            all[i] = JLocale.getString(all[i]);
        }
        addSelector(controlId, label, all, index);
    }
    public void addSelector(int controlId, String label, String[] items, int index) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_SELECT, null, JLocale.getString(label));
        c.items = items;
        c.current = index % c.items.length;
        add(c);
    }
    public void addVolumeControl(int controlId, String label, int current) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_GAUGE, null, JLocale.getString(label));
        c.level = current / 10;
        add(c);
    }
    public void addCheckBox(int controlId, String label, boolean selected) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_CHECKBOX, null, JLocale.getString(label));
        c.selected = selected;
        add(c);
    }
    public void addHeader(String label) {
        add(create(-1, CONTROL_TEXT, JLocale.getString(label), null));
    }
    public void addString(String label, String text) {
        add(create(-1, CONTROL_TEXT, JLocale.getString(label), text));
    }
    public void addString(String text) {
        addString(null, text);
    }
    public void addString(int controlId, String text) {
        add(create(controlId, CONTROL_TEXT, null, text));
    }
    public void addLink(int controlId, String text) {
        add(create(controlId, CONTROL_LINK, null, text));
    }
    public void addLatinTextField(int controlId, String label, String text, int size) {
        addTextField(controlId, label, text, size, TextField.ANY);
    }
    public void addTextField(int controlId, String label, String text, int size) {
        addTextField(controlId, label, text, size, TextField.ANY);
    }
    public void addPasswordField(int controlId, String label, String text, int size) {
        addTextField(controlId, label, text, size, TextField.PASSWORD);
    }
    public void addTextField(int controlId, String label, String text, int size, int type) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_INPUT, null, JLocale.getString(label));
        text = StringConvertor.notNull(text);
        text = (text.length() > size) ? text.substring(0, size) : text;
        c.text = text;
        c.inputType = type;
        c.size = size;
        if (TextField.UNEDITABLE == type) {
            c.disabled = true;
        }
        add(c);
    }
    public void addImage(Image img) {
        Control c = create(-1, CONTROL_IMAGE, null, null);
        c.image = img;
        add(c);
    }
    public void remove(int controlId) {
        try {
            for (int num = 0; num < controls.size(); ++num) {
                if (((Control)controls.elementAt(num)).id == controlId) {
                    controls.removeElementAt(num);
                    invalidate();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }
////////////////////////////////////////////////////////////////////////////////
    private Control get(int controlId) {
        for (int num = 0; num < controls.size(); ++num) {
            if (((Control)controls.elementAt(num)).id == controlId) {
                return (Control)controls.elementAt(num);
            }
        }
        return null;
    }
    public boolean hasControl(int controlId) {
        return null != get(controlId);
    }
    public void setTextFieldLabel(int controlId, String desc) {
        Control c = get(controlId);
        byte type = c.type;
        if (null != desc) {
            int width = getWidth() - scrollerWidth;
            if (CONTROL_CHECKBOX == type) {
                width -= calcIconSize();
            }
            Parser parser = new Parser(fontSet, width);
            parser.addText(desc, THEME_TEXT, FONT_STYLE_PLAIN);
            c.description = parser.getPar();
        }
        c.height = calcControlHeight(c);
        invalidate();
    }

    public int getGaugeValue(int controlId) {
        try {
            return get(controlId).level;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getGaugeValue", e);
            // #sijapp cond.end#
        }
        return 0;
    }
    public int getVolumeValue(int controlId) {
        return getGaugeValue(controlId) * 10;
    }
    public String getTextFieldValue(int controlId) {
        try {
            return get(controlId).text;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getTextFieldValue", e);
            // #sijapp cond.end#
        }
        return null;
    }
    public int getSelectorValue(int controlId) {
        try {
            return get(controlId).current;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getSelectorValue", e);
            // #sijapp cond.end#
        }
        return 0;
    }
    public String getSelectorString(int controlId) {
        try {
            return get(controlId).items[get(controlId).current];
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getSelectorString", e);
            // #sijapp cond.end#
        }
        return null;
    }

    public boolean getCheckBoxValue(int controlId) {
        try {
            return get(controlId).selected;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getChoiceItemValue", e);
            // #sijapp cond.end#
        }
        return false;
    }

    public int getSize() {
        return controls.size();
    }

////////////////////////////////////////////////////////////////////////////////
    private void controlUpdated(Control c) {
        if (null != controlListener) {
            controlListener.controlStateChanged(this, c.id);
        }
        invalidate();
    }
    private int calcIconSize() {
        return fontSet[FONT_STYLE_PLAIN].getHeight();
    }
    private int calcControlHeight(Control c) {
        int height = (null == c.description) ? 0 : c.description.getHeight();
        switch (c.type) {
            case CONTROL_INPUT:
            case CONTROL_SELECT:
                height += normalFont.getHeight() + BOTTOM;
                break;

            case CONTROL_GAUGE:
                height += Math.max(minItemHeight, 3 * calcIconSize());
                break;

            case CONTROL_TEXT:
            case CONTROL_CHECKBOX:
            case CONTROL_LINK:
                height += BOTTOM;
                break;

            case CONTROL_IMAGE:
                height += c.image.getHeight() + 2;
                break;

            case CONTROL_COLOR:
                height += normalFont.getHeight() + BOTTOM;
                break;
        }
        if ((CONTROL_TEXT != c.type) && (CONTROL_IMAGE != c.type)) {
            height = Math.max(height, minItemHeight);
        }
        return height + 2;
    }

    private void add(Control c) {
        c.height = calcControlHeight(c);
        controls.addElement(c);
        invalidate();
    }

    protected int getItemHeight(int itemIndex) {
        return ((Control)controls.elementAt(itemIndex)).height;
    }

    protected boolean isItemSelectable(int index) {
        if ((index < 0) || (controls.size() <= index)) {
            return false;
        }
        byte type = ((Control)controls.elementAt(index)).type;
        if (CONTROL_TEXT == type) {
            return false;
        }
        return true;
    }

    protected boolean isCurrentItemSelectable() {
        int index = getCurrItem();
        byte type = ((Control)controls.elementAt(index)).type;
        return (CONTROL_TEXT != type);
    }

    private void onControlSelected() {
        Control c = (Control)controls.elementAt(getCurrItem());
        if (c.disabled) {
            return;
        }
        switch (c.type) {
            case CONTROL_CHECKBOX:
                c.selected = !c.selected;
                controlUpdated(c);
                break;

            case CONTROL_INPUT:
                box = new InputTextBox().create(c.description.getText(), c.size, c.inputType);
                box.setTextBoxListener(this);
                box.setString(c.text);
                box.show();
                break;

            case CONTROL_SELECT:
                list = new MenuModel();
                for (int i = 0; i < c.items.length; ++i) {
                    list.addRawItem(c.items[i], null, i);
                }
                list.setDefaultItemCode(c.current);
                list.setActionListener(this);
                new Select(list).show();
                break;

            case CONTROL_LINK:
                controlUpdated(c);
                break;
        }
    }

    private boolean key(int actionCode) {
        if (controls.isEmpty()) {
            return false;
        }
        Control c = (Control)controls.elementAt(getCurrItem());
        if (c.disabled) {
            return false;
        }
        if (CONTROL_GAUGE == c.type) {
            int level = c.level;
            if (NativeCanvas.NAVIKEY_LEFT == actionCode) {
                level--;

            } else if (NativeCanvas.NAVIKEY_RIGHT == actionCode) {
                level++;

            } else {
                return false;
            }
            c.level = Math.max(0, Math.min(level, 10));
            controlUpdated(c);
            return true;
        }
        if (CONTROL_SELECT == c.type) {
            if (NativeCanvas.NAVIKEY_LEFT == actionCode) {
                c.current = (c.current - 1 + c.items.length) % c.items.length;

            } else if (NativeCanvas.NAVIKEY_RIGHT == actionCode) {
                c.current = (c.current + 1) % c.items.length;

            } else {
                return false;
            }

            controlUpdated(c);
            return true;
        }
        return false;
    }

    public void textboxAction(InputTextBox b, boolean ok) {
        if ((box == b) && ok) {
            Control c = (Control)controls.elementAt(getCurrItem());
            c.text = box.getString();
            box = null;
            controlUpdated(c);
            restore();
        }
    }

    public void select(Select select, MenuModel menu, int cmd) {
        Control c = (Control)controls.elementAt(getCurrItem());
        c.current = cmd;
        controlUpdated(c);
        select.back();
    }


    private void drawSelectIcon(GraphicsEx g, int x, int y, int width, int height) {
        g.setThemeColor(THEME_FORM_BORDER);
        int size = (calcIconSize() - 4) | 1;
        int half = (size + 1) / 2;
        y += height / 2 - (half / 3);
        x += 2;
        while (0 < size) {
            g.drawLine(x, y, x + size, y);
            size -= 2;
            x += 1;
            y += 1;
        }
    }
    private void drawCheckIcon(GraphicsEx g, int x, int y, int width, int height, boolean checked) {
        int size = calcIconSize() - 4 & ~1;
        x += 2;
        y += 2;
        g.setThemeColor(THEME_FORM_BACK);
        g.fillRect(x + 1, y + 1, size - 1, size - 1);
        g.setThemeColor(THEME_FORM_BORDER);
        g.drawSimpleRect(x, y, size, size);
        if (checked) {
            g.setThemeColor(THEME_FORM_BORDER);
            x += 3;
            y += 3;
            size -= 5;
            g.fillRect(x, y, size, size);
        }
    }

    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        Control c = (Control)controls.elementAt(index);
        h -= BOTTOM;
        x1 += OFFSET;
        w -= 2 * OFFSET;

        if (CONTROL_CHECKBOX == c.type) {
            int iconSize = calcIconSize();
            drawCheckIcon(g, x1, y1, w, h, c.selected);
            x1 += iconSize + OFFSET;

        } else if (CONTROL_GAUGE == c.type) {
            int sectionWidth = w / 11;
            g.setThemeColor(THEME_FORM_TEXT);
            int hh = h - boldFont.getHeight();
            for (int i = 0; i < 11; ++i) {
                int h2 = Math.max(1, i * hh / 11);
                g.fillRect(x1 + i * sectionWidth, y1 + h - h2 - 1, sectionWidth, h2);
            }
            g.setThemeColor(THEME_FORM_EDIT);
            for (int i = 0; i < c.level + 1; ++i) {
                int h2 = Math.max(1, i * hh / 11);
                g.fillRect(x1 + i * sectionWidth, y1 + h - h2 - 1, sectionWidth, h2);
            }
        }

        int hfont = 0;
        if (null != c.description) {
            c.description.paint(fontSet, g, x1, y1, 0, h);
            hfont += c.description.getHeight();
        }


        if ((CONTROL_INPUT == c.type) || (CONTROL_SELECT == c.type)) {
            h -= 3;
            int inputH = h - hfont;
            g.setThemeColor(THEME_FORM_BACK);
            g.fillRect(x1 + 1, y1 + hfont, w - 1, inputH);
            g.setThemeColor(THEME_FORM_BORDER);
            g.drawSimpleRect(x1, y1 + hfont, w, inputH);
            String text = (CONTROL_SELECT == c.type) ? c.items[c.current] : c.text;
            x1 += OFFSET;
            w -= 2 * OFFSET;
            if (!StringConvertor.isEmpty(text)) {
                if (TextField.PASSWORD == c.inputType) {
                    text = "******";
                }
                g.setThemeColor(c.disabled ? THEME_FORM_TEXT : THEME_FORM_EDIT);
                g.setFont(normalFont);

                int width = w;
                if (CONTROL_SELECT == c.type) {
                    width -= calcIconSize() - OFFSET;
                }
                g.drawString(text, x1, y1 + hfont, width, normalFont.getHeight());
            }
            if (CONTROL_SELECT == c.type) {
                g.setThemeColor(THEME_FORM_TEXT);
                int e = calcIconSize();
                drawSelectIcon(g, x1 + w - calcIconSize(), y1 + hfont, e, inputH);
            }
        }
        if (CONTROL_IMAGE == c.type) {
            g.drawImageInCenter(c.image, x1, y1 + hfont, w, c.image.getHeight());
        }
    }
}
