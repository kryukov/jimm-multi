/*
 * Alert.java
 *
 * @author Vladimir Krukov
 */
package jimm.ui;

import DrawControls.text.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.ui.base.*;
import jimm.util.*;

/**
 *
 * @author Vladimir Krukov
 */
public class Alert extends CanvasEx implements Runnable {

    private int x;
    private int y;
    private int alertWidth;
    private int alertHeight;
    private String text;
    private String title;
    private Par par;

    public Alert(String title, String text) {
        this.title = JLocale.getString(title);
        this.text = StringConvertor.notNull(text);

        setSoftBarLabels(null, null, "back", false);
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(int x, int y, boolean longTap) {
        back();
    }
    // #sijapp cond.end#

    protected int getHeight() {
        return alertHeight;
    }

    protected int getWidth() {
        return alertWidth;
    }

    private int getMaxHeight() {
        int height = getScreenHeight();
        return (height <= 200) ? (height - 15) : (height * 6 / 10);
    }

    private void setActiveHeight(int h) {
        y = getScreenHeight() - h - (getMaxHeight() == h ? 1 : 0);
        alertHeight = h;
    }

    protected void restoring() {
        alertWidth = getScreenWidth() - 4;
        x = getScreenWidth() - alertWidth - 2;
        Object prev = Jimm.getJimm().getDisplay().getStack().lastElement();
        boolean animate = (0 == alertHeight) && !(prev instanceof Alert);

        if (animate) {
            setActiveHeight(getMaxHeight() * 5 / 10);
            new Thread(this).start();
        } else {
            setActiveHeight(getMaxHeight());
        }

        Font[] set = GraphicsEx.popupFontSet;
        Parser parser = new Parser(set, alertWidth - 7);
        parser.addTextWithSmiles(text, THEME_POPUP_TEXT, FONT_STYLE_PLAIN);
        par = parser.getPar();
    }

    private int getTextHeight() {
        Font[] set = GraphicsEx.popupFontSet;
        return par.getHeight() + 8
                + (set[FONT_STYLE_PLAIN].getHeight() + 1) * 2;
    }

    protected void paint(GraphicsEx g) {
        int width = getWidth();
        int height = getHeight();

        paintBack(g);
        g.setClip(x, y, width, height);
        g.setStrokeStyle(Graphics.SOLID);

        Font font = GraphicsEx.popupFontSet[FONT_STYLE_PLAIN];
        g.setFont(font);
        int capHeight = font.getHeight() + 1;
        int contentX = x + 3;
        int contentY = y + 3 + capHeight;

        g.fillRect(x, y, width, height, THEME_POPUP_BACK);
        g.drawDoubleBorder(x, y, width, height, THEME_POPUP_BORDER);

        int capBkColor = g.getThemeColor(THEME_POPUP_BACK);
        g.fillGradRect(capBkColor, g.transformColorLight(capBkColor, -32), x + 1, y + 1, width - 1, capHeight - 1);
        g.setThemeColor(THEME_POPUP_BORDER);
        g.drawLine(x, y + capHeight, x + width, y + capHeight);

        g.setThemeColor(THEME_POPUP_TEXT);
        String caption = title;
        g.drawString(null, caption, null, contentX, y, width - 6, capHeight);
        showText(g, contentX, contentY, width - 7, height - 6 - capHeight);
    }

    private void showText(GraphicsEx g, int x, int y, int width, int height) {
        Font[] fontSet = GraphicsEx.popupFontSet;
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.clipRect(x, y, width, height);

        par.paint(fontSet, g, x, y, 0, height);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    protected void doJimmAction(int keyCode) {
        if (NativeCanvas.JIMM_BACK == keyCode) {
            back();
        }
    }
    protected boolean hasMenu() {
        return false;
    }

    public void run() {
        try {
            Thread.sleep(20);
        } catch (Exception e) {
        }
        setActiveHeight(getMaxHeight() * 9 / 10);
        invalidate();
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
        setActiveHeight(getMaxHeight());
        invalidate();
    }
}