/*
 * Popup.java
 *
 * Created on 24 Август 2011 г., 21:59
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import DrawControls.text.*;
import javax.microedition.lcdui.*;
import jimm.comm.*;

/**
 *
 * @author Vladimir Kryukov
 */
public final class Popup implements Runnable {
    private int popupHeight;

    private String text;
    private CanvasEx canvas;

    private Par par;

    public Popup(CanvasEx forIt, String text) {
        this.text = StringConvertor.notNull(text);
        this.canvas = forIt;
    }
    public Popup(String text) {
        this.text = StringConvertor.notNull(text);
        this.canvas = NativeCanvas.getInstance().getCanvas();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(int x, int y, boolean longTap) {
        NativeCanvas.getInstance().setPopup(null);
    }
    // #sijapp cond.end#

    private int getMaxHeight() {
        int height = NativeCanvas.getScreenHeight();
        height = (height <= 200) ? (height - 15) : (height * 6 / 10);
        return Math.min(height, par.getHeight() + 6);
    }
    private void setActiveHeight(int h) {
        popupHeight = h;
    }
    public void show() {
        Font[] set = GraphicsEx.popupFontSet;
        Parser parser = new Parser(set, getPopupWidth() - 7);
        parser.addText(text, CanvasEx.THEME_POPUP_TEXT, CanvasEx.FONT_STYLE_PLAIN);
        par = parser.getPar();

        setActiveHeight(0);
        NativeCanvas.getInstance().setPopup(this);
    }
    private int getPopupWidth() {
        return NativeCanvas.getScreenWidth() - 4;
    }

    protected CanvasEx getCanvas() {
        return canvas;
    }
    protected int paint(GraphicsEx g, int from) {
        if (0 == popupHeight) {
            new Thread(this).start();
            return 0;
        }
        int width = getPopupWidth();
        int height = popupHeight;
        int y = from;
        int x = (NativeCanvas.getScreenWidth() - width) / 2;

        g.setClip(x, y, width, height);
        g.setStrokeStyle(Graphics.SOLID);

        Font font = GraphicsEx.popupFontSet[CanvasEx.FONT_STYLE_PLAIN];
        g.setFont(font);
        int contentX = x + 3;
        int contentY = y + 3;

        g.fillRect(x, y, width, height, CanvasEx.THEME_POPUP_BACK);
        g.drawDoubleBorder(x, y, width, height, CanvasEx.THEME_POPUP_BORDER);

        int capBkCOlor = g.getThemeColor(CanvasEx.THEME_POPUP_BACK);
        g.setThemeColor(CanvasEx.THEME_POPUP_TEXT);
        par.paint(GraphicsEx.popupFontSet, g, contentX, contentY, 0, height - 6);

        return height;
    }

    public void run() {
        setActiveHeight(getMaxHeight() * 5 / 10);
        NativeCanvas.getInstance().repaint();
        try { Thread.sleep(20); } catch (Exception e) {}
        setActiveHeight(getMaxHeight() * 9 / 10);
        NativeCanvas.getInstance().repaint();
        try { Thread.sleep(50); } catch (Exception e) {}
        setActiveHeight(getMaxHeight());
        NativeCanvas.getInstance().repaint();

        try { Thread.sleep(3000); } catch (Exception e) {}
        setActiveHeight(getMaxHeight() * 9 / 10);
        NativeCanvas.getInstance().repaint();

        try { Thread.sleep(50); } catch (Exception e) {}
        setActiveHeight(getMaxHeight() * 5 / 10);
        NativeCanvas.getInstance().repaint();
        try { Thread.sleep(20); } catch (Exception e) {}
        if (this == NativeCanvas.getInstance().getPopup()) {
            NativeCanvas.getInstance().setPopup(null);
        }
    }
}
