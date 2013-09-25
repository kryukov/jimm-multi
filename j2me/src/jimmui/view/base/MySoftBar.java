package jimmui.view.base;

import jimm.Jimm;
import jimm.comm.Util;
import jimm.util.JLocale;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 24.02.13 20:51
 *
 * @author vladimir
 */
public class MySoftBar extends ActiveRegion {
    private String[] softLabels = new String[3];
    private boolean directKey = false;

    private static String time = "";
    public MySoftBar() {
    }
    static void refreshClock() {
        // #sijapp cond.if (modules_ANDROID is "true") or (modules_TOUCH isnot "true")#
        time = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        int h = GraphicsEx.getSoftBarSize();
        int screenHeight = Jimm.getJimm().getDisplay().getScreenHeight();
        Jimm.getJimm().getDisplay().getNativeCanvas().repaint(0, screenHeight - h, Jimm.getJimm().getDisplay().getScreenWidth(), h);
        // #sijapp cond.end#
    }
    public int getHeight() {
        return GraphicsEx.getSoftBarSize();
    }

    public void paint(GraphicsEx graphicsEx, CanvasEx canvas, int bottom) {
        graphicsEx.setStrokeStyle(Graphics.SOLID);
        String[] labels = getSoftLabels();
        int w = canvas.getWidth();
        if (NativeCanvas.isOldSeLike()) {
            drawSoftBar(graphicsEx, labels[1], time,
                    hasRightSoft() ? labels[0] : null,
                    getHeight(), w, bottom);
        } else {
            drawSoftBar(graphicsEx, labels[0], time, labels[2],
                    getHeight(), w, bottom);
        }
    }
    public final void paint(GraphicsEx graphicsEx, Canvas canvas) {
        graphicsEx.setStrokeStyle(Graphics.SOLID);
        String[] labels = getSoftLabels();
        int bottom = canvas.getHeight() - getHeight();
        int w = canvas.getWidth();
        if (NativeCanvas.isOldSeLike()) {
            drawSoftBar(graphicsEx, labels[1], time,
                    hasRightSoft() ? labels[0] : null,
                    getHeight(), w, bottom);
        } else {
            drawSoftBar(graphicsEx, labels[0], time, labels[2],
                    getHeight(), w, bottom);
        }
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(CanvasEx canvas, int x, int y, boolean longTap) {
        int w = canvas.getWidth();
        int lsoftWidth = w / 2 - (w * 10 / 100);
        int rsoftWidth = w - lsoftWidth;
        NativeCanvas nat = Jimm.getJimm().getDisplay().getNativeCanvas();
        if (x < lsoftWidth) {
            nat.emulateKey(canvas, NativeCanvas.LEFT_SOFT);

        } else if (rsoftWidth < x) {
            nat.emulateKey(canvas, NativeCanvas.RIGHT_SOFT);

        } else {
            nat.emulateKey(canvas, NativeCanvas.NAVIKEY_FIRE);
        }
    }
    // #sijapp cond.end#

    private void drawSoftBar(GraphicsEx gr, String left, String middle, String right,
                             int height, int w, int y) {
        int h = height;

        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(0, y, w, h);

        if (null == Scheme.softbarImage) {
            gr.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            gr.fillRect(0, y, w, height);
            gr.setThemeColor(CanvasEx.THEME_CAP_LINE);
            gr.drawLine(0, y, w, y);
            gr.drawLine(0, y + 1, w, y + 1);
        } else {
            gr.drawBarBack(y, height, Scheme.softbarImage, w);
        }

        int halfSoftWidth = w / 2 - (2 + 2 * gr.softbarOffset);
        h -= 2;
        y++;
        gr.setThemeColor(CanvasEx.THEME_CAP_TEXT);
        gr.setFont(gr.softBarFont);

        int leftWidth = 0;
        h -= (h - gr.softBarFont.getHeight()) / 2;
        if (null != left) {
            leftWidth = Math.min(gr.softBarFont.stringWidth(left),  halfSoftWidth);
            gr.drawString(left, gr.softbarOffset, y, leftWidth, h);
        }

        int rightWidth = 0;
        if (null != right) {
            rightWidth = Math.min(gr.softBarFont.stringWidth(right), halfSoftWidth);
            gr.drawString(right, w - rightWidth - gr.softbarOffset, y, rightWidth, h);
        }

        int criticalWidth = halfSoftWidth - 5;
        // #sijapp cond.if (modules_ANDROID is "true") or (modules_TOUCH isnot "true")#
        if ((rightWidth < criticalWidth) && (leftWidth < criticalWidth)) {
            int middleWidth = gr.softBarFont.stringWidth(middle) + 2;
            int start = (w - middleWidth) / 2;
            if ((leftWidth < start) && (rightWidth < start)) {
                gr.drawString(middle, start, y, middleWidth, h);
            }
        }
        // #sijapp cond.end#
        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void setSoftBarLabels(String more, String ok, String back, boolean direct) {
        softLabels[0] = JLocale.getString(more); // menu
        softLabels[1] = JLocale.getString(ok);   // default
        softLabels[2] = JLocale.getString(back); // back
        directKey = direct;
    }
    final boolean isNotSwappable() {
        return directKey;
    }
    final boolean hasRightSoft() {
        return softLabels[0] != softLabels[1];
    }
    final boolean isSwapped() {
        return softLabels[1] == softLabels[2];
    }
    final String[] getSoftLabels() {
        return softLabels;
    }

}
