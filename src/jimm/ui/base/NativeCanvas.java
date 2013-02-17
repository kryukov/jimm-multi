/*
 * NativeCanvas.java
 *
 * Midp Canvas wrapper.
 *
 * @author Vladimir Kryukov
 */

package jimm.ui.base;

import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.modules.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class NativeCanvas extends Canvas {
    public static final int UIUPDATE_TIME = 250;
    private ProtoCanvas canvas = new ProtoCanvas();
    private Popup popup = null;
    private Image bDIimage = null;

    private static NativeCanvas instance = new NativeCanvas();
    // #sijapp cond.if modules_TOUCH is "true"#
    public TouchControl touchControl = new TouchControl();
    // #sijapp cond.end#

    private long firePressTime = 0;

    private boolean ignoreKeys = false;

    private NativeCanvas() {
    }

    protected void paint(Graphics g) {
        if (isDoubleBuffered()) {
            canvas.paintAllOnGraphics(g);

        } else {
            try {
                if ((null == bDIimage) || (bDIimage.getHeight() != getHeight())) {
                    bDIimage = Image.createImage(getWidth(), getHeight());
                }
                canvas.paintAllOnGraphics(bDIimage.getGraphics());
                g.drawImage(bDIimage, 0, 0, Graphics.TOP | Graphics.LEFT);
            } catch (Throwable e) {
                canvas.paintAllOnGraphics(g);
            }
        }
    }

    // #sijapp cond.if target="MIDP2" #
    protected void showNotify() {
        if (Jimm.isPaused()) {
            Jimm.wakeUp();
        }
        updateMetrix(getWidth(), getHeight());
        canvas.getCanvas().restoring();
    }
    // #sijapp cond.end #
//    protected void hideNotify() {
//    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void pointerReleased(int x, int y) {
        ContactList.getInstance().userActivity();
        touchControl.pointerReleased(x, y);
    }
    protected void pointerPressed(int x, int y) {
        ContactList.getInstance().userActivity();
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        touchControl.pointerPressed(x, y);
    }
    protected void pointerDragged(int x, int y) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        touchControl.pointerDragged(x, y);
    }
    // #sijapp cond.end#

    public void setCanvas(CanvasEx canvasEx) {
        stopKeyRepeating();
        canvas.setCanvas(canvasEx);
        // #sijapp cond.if modules_TOUCH is "true"#
        touchControl.setCanvas(canvas);
        // #sijapp cond.end#
    }
    CanvasEx getCanvas() {
        return canvas.getCanvas();
    }
    public void setPopup(Popup p) {
        popup = p;
        repaint();
    }
    public Popup getPopup() {
        return popup;
    }
    public static void stopKeyRepeating() {
        instance.ignoreKeys = true;
        KeyRepeatTimer.stop();
    }

    protected void sizeChanged(int w, int h) {
        updateMetrix(w, h);
        CanvasEx c = canvas.getCanvas();
        if (isShown()) {
            c.restoring();
            invalidate(c);
        }
    }

    public static final int LEFT_SOFT  = 0x00100000;

    public static final int RIGHT_SOFT = 0x00100001;
    public static final int CLEAR_KEY  = 0x00100002;
    public static final int CLOSE_KEY  = 0x00100003;
    public static final int CALL_KEY   = 0x00100004;
    public static final int CAMERA_KEY = 0x00100005;
    public static final int ABC_KEY    = 0x00100006;
    public static final int VOLPLUS_KEY  = 0x00100007;
    public static final int VOLMINUS_KEY = 0x00100008;
    public static final int NAVIKEY_RIGHT = 0x00100009;
    public static final int NAVIKEY_LEFT  = 0x0010000A;
    public static final int NAVIKEY_UP    = 0x0010000B;
    public static final int NAVIKEY_DOWN  = 0x0010000C;
    public static final int NAVIKEY_FIRE  = 0x0010000D;
    public static final int UNUSED_KEY    = 0x0010000F;
    public static final int JIMM_BACK     = 0x00100010;
    public static final int JIMM_MENU     = 0x00100011;
    public static final int JIMM_SELECT   = 0x00100012;
    public static final int JIMM_ACTIVATE = 0x00100013;
    private int getKey(int code) {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (Jimm.isPhone(Jimm.PHONE_ANDROID)) {
            if (-4 == code) {
                return CLOSE_KEY;
            }
            if (-84 == code) {
                return CALL_KEY;
            }
            if (-8 == code) {
                return CLOSE_KEY;
            }
        }
        // #sijapp cond.end #
        String strCode = null;
        try {
            strCode = instance.getKeyName(code);
            if (null != strCode) {
                strCode = strCode.replace('_', ' ').toLowerCase();
            }
        } catch (IllegalArgumentException ignored) {
        }

        if (null != strCode) {
            if ("soft1".equals(strCode)
                    || "soft 1".equals(strCode)
                    || "softkey 1".equals(strCode)
                    || strCode.startsWith("left soft")) {
                return LEFT_SOFT;
            }
            if ("soft2".equals(strCode)
                    || "soft 2".equals(strCode)
                    || "softkey 4".equals(strCode)
                    || strCode.startsWith("right soft")) {
                return RIGHT_SOFT;
            }
            if ("on/off".equals(strCode) || ("ba" + "ck").equals(strCode)) {
                return CLOSE_KEY;
            }
            if (("clea" + "r").equals(strCode)) {
                return CLEAR_KEY;
            }
//            if ("soft3".equals(strCode)) {
//                return MIDDLE_SOFT;
//            }
            if (("se" + "nd").equals(strCode)) {
                return CALL_KEY;
            }
            if (("sele" + "ct").equals(strCode) || ("o" + "k").equals(strCode)
                    || "fire".equals(strCode) || "navi-center".equals(strCode)
                    || "enter".equals(strCode)) {
                return NAVIKEY_FIRE;
            }
            if ("start".equals(strCode)) {
                return CALL_KEY;
            }
            if ("up".equals(strCode) || "navi-up".equals(strCode)
                    || "up arrow".equals(strCode)) {
                return NAVIKEY_UP;
            }
            if ("down".equals(strCode) || "navi-down".equals(strCode)
                    || "down arrow".equals(strCode)) {
                return NAVIKEY_DOWN;
            }
            if ("left".equals(strCode) || "navi-left".equals(strCode)
                    || "left arrow".equals(strCode) || "sideup".equals(strCode)) {
                return NAVIKEY_LEFT;
            }
            if ("right".equals(strCode) || "navi-right".equals(strCode)
                    || "right arrow".equals(strCode) || "sidedown".equals(strCode)) {
                return NAVIKEY_RIGHT;
            }
        }
        if(code == -6 || code == -21 || code == 21 || code == 105
                || code == -202 || code == 113 || code == 57345
                || code == 0xFFBD) {
            return LEFT_SOFT;
        }
        if (!Jimm.isPhone(Jimm.PHONE_SE)) {
            if (-22 == code) {
                return RIGHT_SOFT;
            }
        }
        if (code == -7 || code == 22 || code == 106
                || code == -203 || code == 112 || code == 57346
                || code == 0xFFBB) {
            return RIGHT_SOFT;
        }
        if (-41 == code) { // Alcatel-OT-800/1.0
            return NAVIKEY_FIRE;
        }
        if (-5 == code) {
            return NAVIKEY_FIRE;
        }
        if (63557 == code) { // nokia e63
            return NAVIKEY_FIRE;
        }
        if (code == -8) {
            return CLEAR_KEY;
        }
        if ((-11 == code) || (-12 == code)) {
            return CLOSE_KEY;
        }
        if ((-26 == code) || (-24 == code)) {
            return CAMERA_KEY;
        }
        if (code == -10) {
            return CALL_KEY;
        }
        if (code == -50 || code == 1048582) {
            return ABC_KEY;
        }
        if (code == -36) {
            return VOLPLUS_KEY;
        }
        if (code == -37) {
            return VOLMINUS_KEY;
        }
        return code;
    }

    protected void keyPressed(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        doKeyReaction(keyCode, CanvasEx.KEY_PRESSED);
    }

    protected void keyRepeated(int keyCode) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID is "true" #
        doKeyReaction(keyCode, CanvasEx.KEY_REPEATED);
        // #sijapp cond.end #
    }

    protected void keyReleased(int keyCode) {
        doKeyReaction(keyCode, CanvasEx.KEY_RELEASED);
    }

    public static int getJimmKey(int code) {
        return instance.getKey(code);
    }
    public static int getJimmAction(int key, int keyCode) {
        return instance.getAction(key, keyCode);
    }
    private int getAction(int key, int keyCode) {
        if (key != keyCode) {
            return key;
        }
        switch (keyCode) {
            case KEY_NUM2: return NAVIKEY_UP;
            case KEY_NUM3: return 0;
            case KEY_NUM4: return NAVIKEY_LEFT;
            case KEY_NUM5: return NAVIKEY_FIRE;
            case KEY_NUM6: return NAVIKEY_RIGHT;
            case KEY_NUM7: return 0;
            case KEY_NUM8: return NAVIKEY_DOWN;
            case KEY_NUM9: return 0;
            case KEY_POUND: return 0;
            case KEY_STAR: return 0;
        }
        try {// getGameAction can raise exception
            int action = instance.getGameAction(keyCode);
            switch (action) {
                case Canvas.RIGHT: return NAVIKEY_RIGHT;
                case Canvas.LEFT:  return NAVIKEY_LEFT;
                case Canvas.UP:    return NAVIKEY_UP;
                case Canvas.DOWN:  return NAVIKEY_DOWN;
                case Canvas.FIRE:  return NAVIKEY_FIRE;
            }
        } catch (Exception ignored) {
        }
        return key;
    }
    private int qwerty2phone(int key) {
        switch (key) {
            // lat
            case 'm': return KEY_NUM0;
            case 'r': return KEY_NUM1;
            case 't': return KEY_NUM2;
            case 'z': return KEY_NUM3;
            case 'f': return KEY_NUM4;
            case 'g': return KEY_NUM5;
            case 'h': return KEY_NUM6;
            case 'v': return KEY_NUM7;
            case 'b': return KEY_NUM8;
            case 'n': return KEY_NUM9;
            case 'j': return KEY_POUND;
            case 'u': return KEY_STAR;
            // rus
            case 1100: return KEY_NUM0;
            case 1082: return KEY_NUM1;
            case 1077: return KEY_NUM2;
            case 1103: return KEY_NUM3;
            case 1072: return KEY_NUM4;
            case 1087: return KEY_NUM5;
            case 1088: return KEY_NUM6;
            case 1084: return KEY_NUM7;
            case 1080: return KEY_NUM8;
            case 1090: return KEY_NUM9;
            case 1086: return KEY_POUND;
            case 1075: return KEY_STAR;
        }
        return key;
    }
    private int qwerty2action(int key, int keyCode) {
        switch (key) {
            case KEY_NUM0: return 0;
            case KEY_NUM1: return 0;
            case KEY_NUM2: return NAVIKEY_UP;
            case KEY_NUM3: return 0;
            case KEY_NUM4: return NAVIKEY_LEFT;
            case KEY_NUM5: return NAVIKEY_FIRE;
            case KEY_NUM6: return NAVIKEY_RIGHT;
            case KEY_NUM7: return 0;
            case KEY_NUM8: return NAVIKEY_DOWN;
            case KEY_NUM9: return 0;
            case KEY_POUND: return 0;
            case KEY_STAR: return 0;
        }
        return getAction(key, keyCode);
    }
    static boolean isOldSeLike() {
        return (2 == Options.getInt(Options.OPTION_KEYBOARD));
    }

    private void doKeyReaction(int keyCode, int type) {
        doKeyReaction(canvas.getCanvas(), keyCode, type);
    }
    private void doKeyReaction(CanvasEx c, int keyCode, int type) {
        int key = getKey(keyCode);
        int action;
        if (1 == Options.getInt(Options.OPTION_KEYBOARD)) {
            boolean executed = c.qwertyKey(keyCode, type);
            if (executed) {
                return;
            }
            int qwertyKeyCode = qwerty2phone(keyCode);
            if (qwertyKeyCode != keyCode) {
                key = qwertyKeyCode;
            }
            action = qwerty2action(key, keyCode);
        } else {
            if (isOldSeLike()) {
                switch (key) {
                    case LEFT_SOFT:
                        key = c.isSwapped() ? RIGHT_SOFT : NAVIKEY_FIRE;
                        if (c.isNotSwappable()) {
                            key = LEFT_SOFT;
                        }
                        break;
                    case RIGHT_SOFT:
                        if (c.hasRightSoft()) {
                            key = c.isNotSwappable() ? RIGHT_SOFT : LEFT_SOFT;
                        } else {
                            key = UNUSED_KEY;
                        }
                        break;
                    case CLOSE_KEY:
                        key = RIGHT_SOFT;
                        break;
                }
            }
            action = getAction(key, keyCode);
        }

        ContactList.getInstance().userActivity();
        doKeyReaction(c, key, action, type);
        // #sijapp cond.if modules_ANDROID isnot "true" #
        if (CanvasEx.KEY_PRESSED == type) { // navigation keys only
            switch (action) {
                case NAVIKEY_RIGHT:
                case NAVIKEY_LEFT:
                case NAVIKEY_UP:
                case NAVIKEY_DOWN:
                case NAVIKEY_FIRE:
                case KEY_NUM1:
                case KEY_NUM3:
                case KEY_NUM7:
                case KEY_NUM9:
                    KeyRepeatTimer.start(key, action, c);
                    break;
            }

        } else {
            KeyRepeatTimer.stop();
        }
        // #sijapp cond.end #
    }
    private int mapToJimmAction(CanvasEx c, int keyCode) {
        if ((NativeCanvas.RIGHT_SOFT == keyCode) || (NativeCanvas.CLOSE_KEY == keyCode)) {
            return NativeCanvas.JIMM_BACK;
        }
        if (NativeCanvas.LEFT_SOFT == keyCode) {
            return c.hasMenu() ? NativeCanvas.JIMM_MENU : NativeCanvas.JIMM_SELECT;
        }
        return 0;
    }
    private void doKeyReaction(CanvasEx c, int keyCode, int action, int type) {
        try {
            if (ignoreKeys) {
                if (CanvasEx.KEY_PRESSED != type) {
                    return;
                }
                ignoreKeys = false;
            }
            int jimmAction = mapToJimmAction(c, keyCode);
            if (0 < jimmAction) {
                if ((NativeCanvas.CLOSE_KEY == keyCode)
                        && Jimm.isPhone(Jimm.PHONE_NOKIA_S60)
                        && hasPointerEvents()) {
                    return;
                }
                if (CanvasEx.KEY_PRESSED == type) {
                    c.execJimmAction(jimmAction);
                }
                return;
            }
            if ((NativeCanvas.NAVIKEY_FIRE == action) && (CanvasEx.KEY_PRESSED == type)) {
                firePressTime = System.currentTimeMillis();
            }
            c.doKeyReaction(keyCode, action, type);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Key error", e);
            // #sijapp cond.end #
        }
    }

    void emulateKey(CanvasEx c, int key) {
        if (null == c) c = canvas.getCanvas();
        doKeyReaction(c, key, CanvasEx.KEY_PRESSED);
        doKeyReaction(c, key, CanvasEx.KEY_RELEASED);
    }

    public void invalidate(CanvasEx canvasEx) {
        if (canvas.is(canvasEx)) {
            repaint();
        }
    }

    public static NativeCanvas getInstance() {
        return instance;
    }

    private void updateMetrix(int w, int h) {
        if ((0 == w) || (0 == h)) return;
        canvas.setSize(w, h);
    }
    public static int getScreenWidth() {
        return instance.getWidth();
    }
    public static int getScreenHeight() {
        return instance.getHeight();
    }
    public int getMinScreenMetrics() {
        return Math.min(getWidth(), getHeight());
    }

    public static boolean isLongFirePress() {
        return Display.isLongAction(instance.firePressTime);
    }

    public ProtoCanvas getProtoCanvas() {
        return canvas;
    }


    private static class KeyRepeatTimer extends TimerTask {
        private static Timer timer = new Timer();
        private int key;
        private int action;
        private CanvasEx canvas;
        private NativeCanvas nativeCanvas;
        private int slowlyIterations = 8;


        public static void start(int key, int action, CanvasEx c) {
            try {
                stop();
                timer = new Timer();
                KeyRepeatTimer repeater = new KeyRepeatTimer(key, action, c);
                timer.schedule(repeater, 300, 80);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        public static void stop() {
            Timer t = timer;
            if (null != t) {
                t.cancel();
                t = null;
            }
        }

        private KeyRepeatTimer(int keyCode, int actionCode, CanvasEx c) {
            key = keyCode;
            action = actionCode;
            canvas = c;
            nativeCanvas = NativeCanvas.getInstance();
        }

        public void run() {
            if (0 < slowlyIterations) {
                slowlyIterations--;
                if (0 != slowlyIterations % 2) {
                    return;
                }
            }
            if (!Jimm.getJimm().getDisplay().isShown(nativeCanvas)
                        || !nativeCanvas.canvas.is(canvas)) {
                KeyRepeatTimer.stop();
                return;
            }
            nativeCanvas.doKeyReaction(canvas, key, action, CanvasEx.KEY_REPEATED);
        }
    }
}
