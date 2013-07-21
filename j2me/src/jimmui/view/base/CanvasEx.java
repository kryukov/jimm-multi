/*
 * CanvasEx.java
 *
 * @author Vladimir Krukov
 */

package jimmui.view.base;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.modules.*;
import jimmui.SplashCanvas;
import jimmui.view.menu.Select;

/**
 * Basic class for UI-controls.
 *
 * @author Vladimir Kryukov
 */
abstract public class CanvasEx extends DisplayableEx {
    public static final byte FONT_STYLE_PLAIN = 0;
    public static final byte FONT_STYLE_BOLD  = 1;

    public static final byte THEME_BACKGROUND           = 0;
    public static final byte THEME_TEXT                 = 1;
    public static final byte THEME_CAP_BACKGROUND       = 2;
    public static final byte THEME_CAP_TEXT             = 3;
    public static final byte THEME_PARAM_VALUE          = 4;

    public static final byte THEME_CHAT_INMSG           = 5;
    public static final byte THEME_CHAT_OUTMSG          = 6;
    //public static final byte THEME_CHAT_FROM_HISTORY    = 7;

    public static final byte THEME_CONTACT_ONLINE       = 8;
    public static final byte THEME_CONTACT_WITH_CHAT    = 9;
    public static final byte THEME_CONTACT_OFFLINE      = 10;
    public static final byte THEME_CONTACT_TEMP         = 11;

    public static final byte THEME_SCROLL_BACK          = 12;

    public static final byte THEME_SELECTION_RECT       = 13;
    //public static final byte THEME_BACK                 = 14;

    public static final byte THEME_SPLASH_BACKGROUND    = 15;
    public static final byte THEME_SPLASH_LOGO_TEXT     = 16;
    public static final byte THEME_SPLASH_MESSAGES      = 17;
    public static final byte THEME_SPLASH_DATE          = 18;///
    public static final byte THEME_SPLASH_PROGRESS_BACK = 19;
    public static final byte THEME_SPLASH_PROGRESS_TEXT = 20;
    public static final byte THEME_SPLASH_LOCK_BACK     = 21;
    public static final byte THEME_SPLASH_LOCK_TEXT     = 22;

    public static final byte THEME_MAGIC_EYE_NUMBER     = 23;
    public static final byte THEME_MAGIC_EYE_ACTION     = 24;
    public static final byte THEME_MAGIC_EYE_NL_USER    = 25;
    public static final byte THEME_MAGIC_EYE_USER       = 26;
    public static final byte THEME_MAGIC_EYE_TEXT       = 27;

    //public static final byte THEME_MENU_SHADOW          = 28;
    public static final byte THEME_MENU_BACK            = 29;
    public static final byte THEME_MENU_BORDER          = 30;
    public static final byte THEME_MENU_TEXT            = 31;
    public static final byte THEME_MENU_SEL_BACK        = 32;
    //public static final byte THEME_MENU_SEL_BORDER      = 33;
    public static final byte THEME_MENU_SEL_TEXT        = 34;

    //public static final byte THEME_POPUP_SHADOW         = 35;
    public static final byte THEME_POPUP_BORDER         = 36;
    public static final byte THEME_POPUP_BACK           = 37;
    public static final byte THEME_POPUP_TEXT           = 38;

    public static final byte THEME_GROUP                = 39;
    public static final byte THEME_CHAT_HIGHLIGHT_MSG   = 40;
    public static final byte THEME_SELECTION_BACK       = 41;
    public static final byte THEME_CONTACT_STATUS       = 42;
    public static final byte THEME_PROTOCOL             = 43;
    public static final byte THEME_PROTOCOL_BACK        = 44;

    public static final byte THEME_FORM_EDIT            = 45;
    public static final byte THEME_FORM_TEXT            = 46;
    public static final byte THEME_FORM_BORDER          = 47;
    public static final byte THEME_FORM_BACK            = 48;

    public static final byte THEME_CHAT_BG_IN           = 49;
    public static final byte THEME_CHAT_BG_OUT          = 50;
    public static final byte THEME_CHAT_BG_IN_ODD       = 51;
    public static final byte THEME_CHAT_BG_OUT_ODD      = 52;
    public static final byte THEME_CHAT_BG_MARKED       = 53;
    public static final byte THEME_CHAT_BG_SYSTEM       = 54;

    // Width of scroller line
    public static int scrollerWidth;
    public static int minItemHeight;
    public static int minItemWidth;

    // Key event type
    public final static int KEY_PRESSED = 1;
    public final static int KEY_REPEATED = 2;
    public final static int KEY_RELEASED = 3;

    private boolean repaintLocked = false;

    private int _width;
    private int _height;

    public static void updateUI() {
        scrollerWidth = getScrollWidth();
        minItemHeight = getMinItemHeight();
        minItemWidth = getMinItemWidth();
    }
    private static int getMinItemHeight() {
        int multiplier = Options.getInt(Options.OPTION_MIN_ITEM_SIZE);
        Font smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  Font.SIZE_SMALL);
        return smallFont.getHeight() * multiplier / 10;
    }
    private static int getMinItemWidth() {
        return Jimm.getJimm().getDisplay().getNativeCanvas().getMinScreenMetrics() * 30 / 100;
    }
    private static int getScrollWidth() {
        int zoom = 2;//nc.hasPointerEvents() ? 5 : 2;
        return Math.max(Jimm.getJimm().getDisplay().getNativeCanvas().getMinScreenMetrics() * zoom / 100, 6);
    }

    public int getHeight() {
        return _height - (isSoftBarShown() ? getSoftBar().getHeight() : 0);
    }

    public int getWidth() {
        return _width;
    }
    protected final void setSize(int w, int h) {
        _width = w;
        _height = h;
    }
    public final boolean isSoftBarShown() {
        // #sijapp cond.if modules_ANDROID is "true"#
        if (this instanceof jimm.modules.fs.FileBrowser) return true;
        if (this instanceof jimmui.view.form.GraphForm) return true;
        if (this instanceof jimmui.view.roster.VirtualContactList) {
            return true;//!Options.getBoolean(Options.OPTION_USER_ACCOUNTS);
        }
        if (true) return false;
        // #sijapp cond.end#
        if (this instanceof SplashCanvas) return false;
        // #sijapp cond.if modules_TOUCH isnot "true"#
        if (!Options.getBoolean(Options.OPTION_SHOW_SOFTBAR)) {
            return false;
        }
        // #sijapp cond.end#
        return (null != getSoftBar());
    }
    public final MySoftBar getSoftBar() {
        if (this instanceof VirtualList) return ((VirtualList) this).softBar;
        if (this instanceof Select) return ((Select) this).softBar;
        return null;
    }

    /**
     * UI dinamic update
     */
    protected void updateTask(long microTime) {
    }


    protected int[] getScroll() {
        return null;
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void setScrollTop(int top) {
    }
    protected int getScrollTop() {
        return 0;
    }
    protected void stylusPressed(int x, int y) {
    }
    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
    }
    protected void stylusXMoving(int fromX, int fromY, int toX, int toY) {
    }
    protected void stylusTap(int x, int y, boolean longTap) {
    }
    protected void stylusGeneralYMoved(int fromX, int fromY, int toX, int toY, int type) {
    }
    // #sijapp cond.end#

    /**
     * paint procedure
     */
    protected abstract void paint(GraphicsEx g);

    protected final void paintBack(GraphicsEx g) {
        if (this != Jimm.getJimm().getDisplay().getCurrentDisplay()) {
            return;
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setThemeColor(THEME_BACKGROUND);
        g.fillRect(clipX, clipY, clipWidth, clipHeight);
        try {
            Vector v = jimm.Jimm.getJimm().getDisplay().getStack();
            int from = 0;
            for (int i = v.size() - 1; 0 <= i; --i) {
                if (v.elementAt(i) instanceof VirtualList) {
                    from = i;
                    break;
                } else if (!(v.elementAt(i) instanceof CanvasEx)) {
                    from = i;
                    break;
                }
            }
            for (int i = from; i < v.size(); ++i) {
                Object o = v.elementAt(i);
                if (o instanceof CanvasEx) {
                    ((CanvasEx)o).paint(g);
                }
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("paint back", e);
            // #sijapp cond.end #
        }
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }



    // protected void invalidate()
    public final void invalidate() {
        if (repaintLocked) return;
        Jimm.getJimm().getDisplay().getNativeCanvas().invalidate(this);
    }

    public final void lock() {
        repaintLocked = true;
    }

    public final void unlock() {
        repaintLocked = false;
        invalidate();
    }

    protected final boolean isLocked() {
        return repaintLocked;
    }

    public final void execJimmAction(int keyCode) {
        doJimmAction(keyCode);
    }
    protected abstract void doJimmAction(int keyCode);
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
    }
    protected boolean qwertyKey(int keyCode, int type) {
        return false;
    }

    protected boolean hasMenu() {
        return true;
    }

    protected void sizeChanged(int prevW, int prevH, int w, int h) {
    }
}