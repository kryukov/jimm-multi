/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jimm.ui.text;

import DrawControls.icons.Icon;
import DrawControls.text.Par;
import DrawControls.text.Parser;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import jimm.comm.StringConvertor;
import jimm.ui.base.CanvasEx;
import jimm.ui.base.GraphicsEx;
import jimm.ui.base.NativeCanvas;
import jimm.util.JLocale;

/**
 *
 * @author vladimir
 */
public final class TextListModel {
    private final Vector pars = new Vector();
    private Font[] fontSet = GraphicsEx.chatFontSet;
    private String header = null;

    Font[] getFontSet() {
        return fontSet;
    }
    public void updateFontSet() {
        fontSet = GraphicsEx.chatFontSet;
    }

    public final void addPar(Parser item) {
        pars.addElement(item.getPar());
    }

    protected final Par getPar(int index) {
        return (Par) pars.elementAt(index);
    }
    public final int getSize() {
        return pars.size();
    }

    private Parser createParser(boolean selectable) {
        final int width = NativeCanvas.getInstance().getMinScreenMetrics() - 3;
        Parser parser = new Parser(fontSet, width);
        parser.setSelectable(selectable);
        return parser;
    }
    public final Parser createNewParser(boolean selectable) {
        return createParser(selectable);
    }

    protected final String getAllText() {
        StringBuffer result = new StringBuffer();

        // Fills the lines
        int size = getSize();
        for (int i = 0; i < size; ++i) {
            String text = getPar(i).getText();
            if (null != text) {
                result.append(text).append("\n");
            }
        }
        String retval = result.toString().trim();
        return (0 == retval.length()) ? null : retval;
    }

    public final boolean isSelectable(int index) {
        return getPar(index).selectable;
    }

    public final String getParText(int index) {
        Par par = getPar(index);
        return par.selectable ? par.getText() : null;
    }


    /** Remove all lines form list */
    public void clear() {
        pars.removeAllElements();
        header = null;
    }

    public void removeFirst() {
        pars.removeElementAt(0);
    }

    ///////////////////////////////////////////////////////////////////////////
    public final void addItem(String text, boolean active) {
        byte type = active ?  CanvasEx.FONT_STYLE_BOLD :  CanvasEx.FONT_STYLE_PLAIN;
        Parser item = createParser(true);
        item.useMinHeight();
        item.addText(text, CanvasEx.THEME_TEXT, type);
        addPar(item);
    }
    ///////////////////////////////////////////////////////////////////////////
    public final void setHeader(String header) {
        this.header = header;
    }

    public final void setInfoMessage(String text) {
        Parser par = createParser(false);
        par.addText(text, CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
        addPar(par);
    }

    private void addHeader() {
        if (null != header) {
            Parser line = createParser(false);
            line.addText(JLocale.getString(header),
                    CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_BOLD);
            addPar(line);
            header = null;
        }
    }

    public void addParam(String langStr, String str) {
        if (!StringConvertor.isEmpty(str)) {
            addHeader();
            Parser line = createParser(true);
            line.addText(JLocale.getString(langStr) + ": ",
                    CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
            line.addText(str, CanvasEx.THEME_PARAM_VALUE, CanvasEx.FONT_STYLE_PLAIN);
            addPar(line);
        }
    }

    public void addParamImage(String langStr, Icon img) {
        if (null != img) {
            addHeader();
            Parser line = createParser(true);
            if (!StringConvertor.isEmpty(langStr)) {
                line.addText(JLocale.getString(langStr) + ": ",
                        CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
            }
            line.addImage(img);
            addPar(line);
        }
    }
}
