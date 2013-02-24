package jimm.ui.base;

import jimm.Jimm;
import jimm.comm.Util;
import jimm.util.JLocale;

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

    private static int softBarHeight = 0;
    private static String time = "";
    public MySoftBar() {
        softBarHeight = GraphicsEx.getSoftBarSize();
    }
    static void refreshClock() {
        time = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        int h = softBarHeight;
        int screenHeight = NativeCanvas.getScreenHeight();
        NativeCanvas.getInstance().repaint(0, screenHeight - h, NativeCanvas.getScreenWidth(), h);
    }
    public int getHeight() {
        return softBarHeight;
    }

    public void paint(GraphicsEx graphicsEx, CanvasEx canvas, int bottom) {
        graphicsEx.setStrokeStyle(Graphics.SOLID);
        String[] labels = getSoftLabels();
        int w = canvas.getWidth();
        if (NativeCanvas.isOldSeLike()) {
            graphicsEx.drawSoftBar(labels[1], time,
                    hasRightSoft() ? labels[0] : null,
                    softBarHeight, w, bottom);
        } else {
            graphicsEx.drawSoftBar(labels[0], time, labels[2],
                    softBarHeight, w, bottom);
        }
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(CanvasEx canvas, int x, int y, boolean longTap) {
        int w = canvas.getWidth();
        int lsoftWidth = w / 2 - (w * 10 / 100);
        int rsoftWidth = w - lsoftWidth;
        NativeCanvas nat = NativeCanvas.getInstance();
        if (x < lsoftWidth) {
            nat.emulateKey(canvas, NativeCanvas.LEFT_SOFT);

        } else if (rsoftWidth < x) {
            nat.emulateKey(canvas, NativeCanvas.RIGHT_SOFT);

        } else {
            nat.emulateKey(canvas, NativeCanvas.NAVIKEY_FIRE);
        }
    }
    // #sijapp cond.end#


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
