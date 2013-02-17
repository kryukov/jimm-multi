/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/Jimm.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;

import DrawControls.icons.Icon;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import jimm.chat.ChatHistory;
import jimm.cl.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.search.Search;
import jimm.ui.*;
import jimm.ui.base.*;
// #sijapp cond.if modules_ACTIVITYUI is "true"#
import jimm.ui.notify.*;
// #sijapp cond.end#
import jimm.util.JLocale;


public class Jimm extends MIDlet implements Runnable {
    public static final String VERSION = "###VERSION###";
    public static String lastDate;
    private Display display;

    private boolean locked = false;
    private long lastLockTime = 0;

    // Application main object
    private static Jimm instance = null;
    public static Jimm getJimm() {
        return instance;
    }

    public static final String microeditionPlatform = getPhone();
    public static final String microeditionProfiles = getSystemProperty("microedition.profiles", null);
    public static final byte generalPhoneType = getGeneralPhone();
    public SplashCanvas splash;
    private boolean paused = true;
    // #sijapp cond.if modules_ACTIVITYUI is "true"#
    private ActivityUI activity;
    // #sijapp cond.end#

    /****************************************************************************/

    public static final byte PHONE_SE             = 0;
    public static final byte PHONE_SE_SYMBIAN     = 1;
    public static final byte PHONE_NOKIA          = 2;
    public static final byte PHONE_NOKIA_S40      = 3;
    public static final byte PHONE_NOKIA_S60      = 4;
    public static final byte PHONE_NOKIA_S60v8    = 5;
    public static final byte PHONE_NOKIA_N80      = 6;
    public static final byte PHONE_INTENT_JTE     = 7;
    public static final byte PHONE_JBED           = 8;
    public static final byte PHONE_SAMSUNG        = 9;
    public static final byte PHONE_ANDROID        = 10;

    private static String getPhone() {
        final String platform = getSystemProperty("microedition.platform", null);
        // #sijapp cond.if target is "MIDP2" #
        if (null == platform) {
            try {
                Class.forName("com.nokia.mid.ui.DeviceControl");
                return "Nokia";
            } catch (Exception e) {
            }
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_ANDROID is "true" #
        String android = getSystemProperty("device.model", "")
                + "/" + getSystemProperty("device.software.version", "")
                + "/" +  getSystemProperty("device.id", "");
        if (2 < android.length()) {
            return "android/" + android;
        }
        // #sijapp cond.end #
        return platform;
    }

    private static byte getGeneralPhone() {
        String device = getPhone();
        if (null == device) {
            return -1;
        }
        device = device.toLowerCase();
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_ANDROID is "true" #
        if (-1 != device.indexOf("android")) {
            return PHONE_ANDROID;
        }
        // #sijapp cond.end#
        if (device.indexOf("ericsson") != -1) {
            if ((-1 != getSystemProperty("com.sonyericsson.java.platform", "")
                        .toLowerCase().indexOf("sjp"))) {
                return PHONE_SE_SYMBIAN;
            }
            return PHONE_SE;
        }
        if (-1 != device.indexOf("platform=s60")) {
            return PHONE_NOKIA_S60;
        }
        if (device.indexOf("nokia") != -1) {
            if (device.indexOf("nokian80") != -1) {
                return PHONE_NOKIA_N80;
            }
            if (null != getSystemProperty("com.nokia.memoryramfree", null)) {
                // S60 3rd Edition
                return PHONE_NOKIA_S60;
            }
            String dir = getSystemProperty("fileconn.dir.private", "");
            // s40 (6233) does not have this property
            if (-1 != dir.indexOf("/private/")) {
                // it is s60 v3 fp1
                return PHONE_NOKIA_S60;
            }
            if (-1 != device.indexOf(';')) {
                return PHONE_NOKIA_S60;
            }
            return PHONE_NOKIA_S40;
        }
        if (device.indexOf("samsung") != -1) {
            return PHONE_SAMSUNG;
        }
        if (device.indexOf("jbed") != -1) {
            return PHONE_JBED;
        }
        if (device.indexOf("intent") != -1) {
            return PHONE_INTENT_JTE;
        }
        // #sijapp cond.end #
        return -1;
    }
    public static boolean isPhone(final byte phone) {
        // #sijapp cond.if target is "MIDP2" #
        if (PHONE_NOKIA_S60v8 == phone) {
            return (PHONE_NOKIA_S60 == generalPhoneType)
                    && (-1 == microeditionPlatform.indexOf(';'));
        }
        if (PHONE_NOKIA == phone) {
            return (PHONE_NOKIA_S40 == generalPhoneType)
                    || (PHONE_NOKIA_S60 == generalPhoneType)
                    || (PHONE_NOKIA_N80 == generalPhoneType);
        }
        if (PHONE_SE == phone) {
            return (PHONE_SE_SYMBIAN == generalPhoneType)
                    || (PHONE_SE == generalPhoneType);
        }
        // #sijapp cond.end #
        return phone == generalPhoneType;
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    private int getSeVersion() {
        String sJava = getSystemProperty("com.sonyericsson.java.platform", "");
        // sJava has format "JP-x.x" or "JP-x.x.x", e.g. "JP-8.5" or "JP-8.5.2".
        // The next code also correct parse string with format "JP-x".
        // On all uncorrect strings, sonyJava set to 0.
        if ((null != sJava) && sJava.startsWith("JP-")) {
            int major = 0;
            int minor = 0;
            int micro = 0;


            if (sJava.length() >= 4) {
                major = sJava.charAt(3) - '0';
            }
            if (sJava.length() >= 6) {
                minor = sJava.charAt(5) - '0';
            }
            if (sJava.length() >= 8) {
                micro = sJava.charAt(7) - '0';
            }


            if ((0 <= major) && (major <= 9)
                    && (0 <= minor) && (minor <= 9)
                    && (0 <= micro) && (micro <= 9)) {
                return major * 100 + minor * 10 + micro;
            }
        }
        return 0;
    }
    public static boolean hasMemory(int requared) {
        // #sijapp cond.if target is "MIDP2" #
        if (isPhone(PHONE_SE)) {
            return true;
        }
        if (isPhone(PHONE_NOKIA_S60)) {
            return true;
        }
        if (isPhone(PHONE_JBED)) {
            return true;
        }
        if (isPhone(PHONE_INTENT_JTE)) {
            return true;
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        if (isPhone(PHONE_ANDROID)) {
            return true;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        Jimm.gc();
        long free = Runtime.getRuntime().freeMemory();
        return (requared < free);
    }

    public static String getAppProperty(String key, String defval) {
        String res = null;
        try {
            res = instance.getAppProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }
    public static boolean isSetAppProperty(String key) {
        String res = getAppProperty(key, "");
        return "yes".equals(res) || "true".equals(res);
    }
    private static String getSystemProperty(String key, String defval) {
        String res = null;
        try {
            res = System.getProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }

    // #sijapp cond.if target is "MIDP2"#
    public static boolean isS60v5() {
        String platform = StringConvertor.notNull(Jimm.microeditionPlatform);
        return -1 != platform.indexOf("sw_platform_version=5.");
    }
    // #sijapp cond.end#
    private static void platformRequestUrl(String url) throws ConnectionNotFoundException {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (-1 == url.indexOf(':')) {
            url = "xmpp:" + url;
        }
        if (url.startsWith("xmpp:")) {
            Search search = ContactList.getInstance().getManager().getCurrentProtocol().getSearchForm();
            search.show(Util.getUrlWithoutProtocol(url));
            return;
        }
        // #sijapp cond.end #
        if (url.equals("jimm:update")) {
            StringBuffer url_ = new StringBuffer();
            url_.append("http://jimm.net.ru/go.xhtml?act=update&lang=");
            url_.append(JLocale.getCurrUiLanguage());
            url_.append("&protocols=###PROTOCOLS###&cdata=");
            url_.append(Config.loadResource("build.dat"));
            url = url_.toString();
        }
        Jimm.getJimm().platformRequest(url.trim());
    }
    public static void openUrl(String url) {
        try {
            platformRequestUrl(url);
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    public static void platformRequestAndExit(String url) {
        try {
            platformRequestUrl(url);
            // #sijapp cond.if modules_ANDROID isnot "true" #
            Jimm.getJimm().quit();
            // #sijapp cond.end #
        } catch (Exception e) {
            /* Do nothing */
        }
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in = null;
        // #sijapp cond.if modules_ANDROID is "true" #
        in = jimm.modules.fs.FileSystem.openJimmFile(name);
        if (null == in) {
            try {
                in = ru.net.jimm.JimmActivity.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        // #sijapp cond.else #
        in = new Object().getClass().getResourceAsStream(name);
        // #sijapp cond.end #

        return in;
    }

    public void addEvent(String title, String desc, Icon icon) {
        // #sijapp cond.if modules_ACTIVITYUI is "true"#
        if (null != activity) {
            activity.addEvent(title, desc, null);
        }
        // #sijapp cond.end#
    }


    public void run() {
        try {
            backgroundLoading();
        } catch (Exception ignored) {
        }
    }
    private void backgroundLoading() {
        // #sijapp cond.if modules_TRAFFIC is "true" #
        // Create traffic Object (and update progress indicator)
        Traffic.getInstance().load();
        // #sijapp cond.end#

        // #sijapp cond.if modules_SOUND is "true"#
        Notify.getSound().initSounds();
        // #sijapp cond.end#

        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.switchOn(Options.getInt(Options.OPTION_LIGHT_THEME));
        // #sijapp cond.end#
        Jimm.gc();

        // init message editor
        // #sijapp cond.if modules_SMILES is "true" #
        //splash.setProgress(10);
        Emotions.instance.load();
        // #sijapp cond.end#
        //splash.setProgress(25);
        StringConvertor.load();
        //splash.setProgress(35);
        Templates.getInstance().load();
        ContactList.getInstance().initMessageEditor();
        Jimm.gc();

        // #sijapp cond.if modules_DEBUGLOG is "true"#
        DebugLog.startTests();
        // #sijapp cond.end#
    }

    private void initBasic() {
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.config.HomeDirectory.init();
        // #sijapp cond.end#
        JLocale.loadLanguageList();
        Scheme.load();

        Options.loadOptions();
        // #sijapp cond.if modules_ANDROID is "true" #
        new ru.net.jimm.config.Options().load();
        // #sijapp cond.end#
        JLocale.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
        CanvasEx.updateUI();
        UIUpdater.startUIUpdater();
    }
    private void initialize() {
        splash = new SplashCanvas();

        splash.setMessage(JLocale.getString("loading"));
        // #sijapp cond.if modules_ANDROID isnot "true" #
        splash.show();
        // #sijapp cond.end #
        splash.setProgress(5);

        // back loading (traffic, sounds and light)
        //new Thread(this).start();
        backgroundLoading();

        // init contact list
        splash.setProgress(10);
        Options.loadAccounts();
        splash.setProgress(50);
        ContactList.getInstance().initUI();
        splash.setProgress(60);
        ContactList.getInstance().initAccounts();
        splash.setProgress(80);
        ContactList.getInstance().loadAccounts();

        // #sijapp cond.if modules_ACTIVITYUI is "true"#
        if (isPhone(PHONE_SE) && (750 <= getSeVersion())) {
            activity = new ActivityUI();
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().updateAppIcon();
        // #sijapp cond.end #
    }

    private void restore(Object screen) {
        if (null == screen) {
            return;
        }
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.online();
        // #sijapp cond.end #
        wakeUp();
        // #sijapp cond.if target is "MIDP2"#
        if (isS60v5()) {
            display.hide();
        }
        if (isPhone(PHONE_SE)) {
            display.hideIfNeed();
        }
        // #sijapp cond.end#
        display.restore(screen);
    }

    public Display getDisplay() {
        return display;
    }


    // Start Jimm
    public void startApp() throws MIDletStateChangeException {
        if (!paused && (null != Jimm.instance)) {
            return;
        }
        if (null == display) {
            display = new Display(this);
        }
        // Return if MIDlet has already been initialized
        if (null != Jimm.instance) {
            restore(display.getCurrentDisplay());
            return;
        }
        // Save MIDlet reference
        Jimm.instance = this;
        locked = false;
        wakeUp();
        initBasic();
        try {
            initialize();
            ContactList.getInstance().startUp();
            UIUpdater.refreshClock();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.panic("init", e);
            DebugLog.activate();
            // #sijapp cond.end#
        }
    }

    // Pause
    public void pauseApp() {
        try {
            hideApp();
        } catch (Exception e) {
        }
    }
    public void hideApp() {
        Object currentScreen = display.getCurrentDisplay();
        if (currentScreen instanceof InputTextBox) {
            return;
        }
        paused = true;
        //currentScreen = null;
        locked = false;
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.away();
        // #sijapp cond.end #
    }

    public void quit() {
        ContactList cl = ContactList.getInstance();
        boolean wait;
        try {
            wait = cl.disconnect();
        } catch (Exception e) {
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            /* Do nothing */
        }
        cl.safeSave();
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                /* Do nothing */
            }
        }
        try {
            Jimm.getJimm().destroyApp(true);
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    /**
     * Destroy Jimm
     */
    public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        if (null != instance) {
            ChatHistory.instance.saveUnreadMessages();
            // #sijapp cond.if modules_TRAFFIC is "true" #
            // Save traffic
            Traffic.getInstance().safeSave();
            // #sijapp cond.end#
        }
        instance.display.hide();
        instance = null;
        notifyDestroyed();
    }

    public static boolean isPaused() {
        if (instance.paused) {
            return true;
        }
        return instance.display.isPaused();
    }

    public static boolean isLocked() {
        return instance.locked;
    }

    public static void lockJimm() {
        final long now = Jimm.getCurrentGmtTime();
        final int WAITING_INTERVAL = 3; // sec
        if (instance.lastLockTime + WAITING_INTERVAL  < now) {
            instance.locked = true;
            instance.splash.lockJimm();
            // #sijapp cond.if modules_ABSENCE is "true" #
            AutoAbsence.instance.away();
            // #sijapp cond.end #
        }
    }
    public static void unlockJimm() {
        instance.lastLockTime = Jimm.getCurrentGmtTime();
        instance.locked = false;
        ContactList.getInstance().activate();
        UIUpdater.refreshClock();
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.online();
        // #sijapp cond.end #
    }

    public static void maximize() {
        instance.restore(instance.display.getCurrentDisplay());
        wakeUp();
    }
    public static void maximize(CanvasEx screen) {
        instance.restore(screen);
        wakeUp();
    }
    // Set the minimize state of midlet
    public static void minimize() {
        instance.hideApp();
        instance.display.hide();
    }
    public static void wakeUp() {
        instance.paused = false;
    }
    public static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }
}