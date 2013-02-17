/*
 * Light.java
 *
 * Light-control module.
 *
 * Usage:
 * <CODE>
 * // #sijapp cond.if modules_LIGHT is "true" #
 * CustomLight.setLightMode(CustomLight.ACTION_NONE);
 * // #sijapp cond.end#
 * </CODE>
 *
 * @author Vladimir Krukov
 */

package jimm.modules;


// #sijapp cond.if modules_LIGHT is "true" #


import jimm.*;
import java.util.*;

/**
 * Class for platform-independent light control.
 *
 * @author Vladimir Krukov
 */
public final class CustomLight extends TimerTask {
    private static CustomLight instance = new CustomLight(null);
    private Timer timer;

    public static final byte ACTION_NONE               = 0;
    public static final byte ACTION_KEY_PRESS          = 1;
    public static final byte ACTION_MESSAGE            = 2;
    public static final byte ACTION_ERROR              = 3;
    public static final byte ACTION_SYSTEM             = 4;

    public static final byte ACTION_COUNT              = 5;

    public static final byte ACTION_SYSTEM_OFF         = 11;

    private static final byte ACTION_OFF               = 100;
    private static final byte ACTION_SLEEP             = 101;
    private static final byte ACTION_SYSTEM_SLEEP        = 102;


    private static final byte LIGHT_NONE               = 0;
    private static final byte LIGHT_NOKIA              = 2;
    private static final byte LIGHT_MIDP20             = 6;

    private static int light  = detectMode();
    private byte action = ACTION_NONE;
    private int tick = 0;
    private int prevLightLevel = 0;
    private boolean checkPrevState = true;
    private boolean systemLock = false;

    private int[] lightTheme;
    private int lightThemeIndex = 0;

    private static final int INTERVAL = 1000;

    public static void setLightMode(final byte m) {
        if (null != instance) {
            instance.setMode(m);
        }
    }

    private int getMaxTickCount() {
        return 15;
    }
    private synchronized void setMode(final byte m) {
        if (0 == lightThemeIndex) {
            return;
        }
        if (systemLock && (ACTION_SYSTEM_OFF != m)) {
            return;
        }
        systemLock = (ACTION_SYSTEM == m);
        tick = getMaxTickCount();
        processAction(m);
        action = nextAction(m);
    }

    public void run() {
        if (0 == lightThemeIndex) {
            return;
        }
        final byte act = action;
        if (systemLock || (ACTION_OFF == act)) {
            return;
        }

        if (ACTION_SLEEP != act) {
            processAction(act);
        }

        if (0 < tick) {
            tick--;
            return;
        }
        tick = getMaxTickCount();
        action = nextAction(act);
    }

    private byte nextAction(byte action) {
        switch (action) {
            case ACTION_NONE:
            case ACTION_OFF:
                return ACTION_OFF;

            case ACTION_SYSTEM:
            case ACTION_SYSTEM_SLEEP:
                return ACTION_SYSTEM_SLEEP;

            case ACTION_SLEEP:
                return ACTION_NONE;

            case ACTION_SYSTEM_OFF:
            default:
                return ACTION_SLEEP;
        }
    }

    private synchronized void processAction(byte action) {
        setLight(getLightValue(action));
    }

    private void setLight(int level) {
        if ((100 < level) || (level < 0)) {
            return;
        }
        if (checkPrevState && (level == prevLightLevel)) {
            return;
        }
        prevLightLevel = level;
        // #sijapp cond.if target is "MIDP2" #
        if ((0 < level) && Jimm.isPhone(Jimm.PHONE_NOKIA_S40)) {
            setHardwareLight(0);
        }
        // #sijapp cond.end #
        setHardwareLight(level);
    }
    private void setHardwareLight(int value) {
        try {
            switch (light) {
                // #sijapp cond.if target is "MIDP2" #
                case LIGHT_NOKIA:
                    com.nokia.mid.ui.DeviceControl.setLights(0, value);
                    break;

//                case LIGHT_SAMSUNG:
//                    if (value > 0) {
//                        com.samsung.util.LCDLight.on(0x7FFFFFFF);
//                    } else {
//                        com.samsung.util.LCDLight.off();
//                    }
//                    break;
                // #sijapp cond.end #

                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
                case LIGHT_MIDP20:
                    Jimm.getJimm().getDisplay().getNativeDisplay().flashBacklight(
                            (0 < value) ? 0x7FFFFFFF : 0x00000000);
                    break;
                // #sijapp cond.end #
            }
        } catch (Exception e) {
        }
    }

    public static boolean isSupport() {
        return light != LIGHT_NONE;
    }

    public static boolean canControlBrightness() {
        return (light == LIGHT_NOKIA);
    }

    private static int detectMode() {
        // #sijapp cond.if target is "MIDP2" #
        try {
            Class.forName("com.nokia.mid.ui.DeviceControl");
            return LIGHT_NOKIA;
        } catch (Exception e) {
        }
//        try {
//            Class.forName("com.samsung.util.LCDLight");
//            return LIGHT_SAMSUNG;
//        } catch (Exception e) {
//        }
        // #sijapp cond.end #
        return LIGHT_MIDP20;
    }

    private int getLightValue(int action) {
        switch (action) {
            case ACTION_SYSTEM_OFF:
                action = ACTION_KEY_PRESS;
                break;
            case ACTION_ERROR:
                action = ACTION_MESSAGE;
                break;
        }
        if (action < ACTION_COUNT) {
            return lightTheme[action];
        }
        return -1;
    }

    /** Creates a new instance of Light */
    private CustomLight(Timer timer) {
        this.timer = timer;
    }
//    private boolean isOff() {
//        return (null != timer) && checkPrevState && (0 == prevLightLevel);
//    }
//    public static boolean isTurnedOff() {
//        return instance.isOff();
//    }
    public static void switchOn(int theme) {
        if (instance.lightThemeIndex == theme) {
            return;
        }
        final boolean on = (0 < theme);
        final boolean worked = (null != instance.timer);
        instance.lightThemeIndex = 0;
        if (worked) {
            if (!on) {
                instance.timer.cancel();
                // #sijapp cond.if target is "MIDP2" #
                instance.setLight(Jimm.isPhone(Jimm.PHONE_NOKIA_S60) ? 40 : 0);
                // #sijapp cond.else #
                instance.setLight(0);
                // #sijapp cond.end #
                instance = new CustomLight(null);
            }
        } else {
            if (on) {
                instance = new CustomLight(new Timer());
            }
        }
        if (null != instance.timer) {
            instance.setTheme(theme);
            if (!worked) {
                instance.timer.scheduleAtFixedRate(instance, 0, INTERVAL);
                setLightMode(ACTION_KEY_PRESS);
            }
        }
    }
    private void setTheme(int theme) {
        int m = 100;
        int l = 0;
        int def = 0;
        switch (theme) {
            case 1: l = 20; m = 20; break;
            case 2: l = 50; m = 50; break;
            case 3: l = 100; m = 100; break;
            case 4: l = 0; m = 100; break;
            case 5: def = 20; l = 20; m = 20; break;
        }
        lightThemeIndex = theme;
        lightTheme = new int[]{def, l, m, l, l, l};
        checkPrevState = (101 != getLightValue(ACTION_NONE));
    }
}
// #sijapp cond.end#
