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

import java.io.*;
import javax.microedition.io.*;
import jimm.chat.ChatHistory;
import jimm.cl.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.search.Search;
import jimmui.view.*;
import jimmui.view.base.*;
// #sijapp cond.if modules_ACTIVITYUI is "true"#
import jimmui.view.notify.*;
// #sijapp cond.end#
import jimm.util.JLocale;
import protocol.Protocol;
import protocol.ui.MessageEditor;
import protocol.ui.StatusView;


public class Jimm {
    private boolean locked = false;
    private long lastLockTime = 0;
    private boolean paused = true;
    private Display display;
    public JimmModel jimmModel;
    public SplashCanvas splash;
    private ContactList cl;
    private MessageEditor editor;
    private StatusView statusView;
    public UIUpdater uiUpdater;

    // #sijapp cond.if modules_ACTIVITYUI is "true"#
    private ActivityUI activity;
    // #sijapp cond.end#

    public PhoneInfo phone = new PhoneInfo();

    public final String VERSION = "###VERSION###";
    public String lastDate;

    // Application main object
    private static final Jimm instance = new Jimm();
    /****************************************************************************/

    private void platformRequestUrl(String url) throws ConnectionNotFoundException {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (-1 == url.indexOf(':')) {
            url = "xmpp:" + url;
        }
        if (url.startsWith("xmpp:")) {
            Search search = cl.getManager().getCurrentProtocol().getSearchForm();
            search.show(Util.getUrlWithoutProtocol(url));
            return;
        }
        // #sijapp cond.end #
        if (url.equals("jimm:update")) {
            url = "http://jimm.net.ru/go.xhtml?act=update&lang="
                    + JLocale.getCurrUiLanguage()
                    + "&protocols=###PROTOCOLS###&cdata="
                    + Config.loadResource("build.dat");
        }
        JimmMidlet.getMidlet().platformRequest(url.trim());
    }
    public void openUrl(String url, boolean exit) {
        try {
            platformRequestUrl(url);
            // #sijapp cond.if modules_ANDROID isnot "true" #
            if (exit) quit();
            // #sijapp cond.end #
        } catch (Exception e) {
            /* Do nothing */
        }
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
        if (phone.isS60v5()) {
            display.hide();
        }
        if (phone.isPhone(PhoneInfo.PHONE_SE)) {
            display.hideIfNeed();
        }
        // #sijapp cond.end#
        display.restore(screen);
    }

    public Display getDisplay() {
        return display;
    }


    public void restoreJimm() {
        if (!paused) {
            return;
        }
        restore(display.getCurrentDisplay());
    }
    // Start Jimm
    public void startJimm() {
        if (!paused) {
            return;
        }
        display = new Display();
        locked = false;
        wakeUp();
        initDaemonPhase1();
        initUiPhase2();
        try {
            splash = new SplashCanvas();
            initDaemonPhase3();
            initUiPhase4();
            // phase 5
            cl.startUp();
            // phase 6
            if (0 < Options.getAccountCount()) {
                autoConnect();
            }
            uiUpdater.refreshClock();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.panic("init", e);
            DebugLog.activate();
            // #sijapp cond.end#
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
        boolean wait;
        try {
            wait = jimmModel.disconnect();
        } catch (Exception e) {
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            /* Do nothing */
        }
        jimmModel.safeSave();
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                /* Do nothing */
            }
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.quit();
        // #sijapp cond.end #
        try {
            JimmMidlet.getMidlet().destroyApp(true);
        } catch (Exception e) {
            /* Do nothing */
        }
    }
    /**
     * Destroy Jimm
     */
    public void destroyJimm() {
        ChatHistory.instance.saveUnreadMessages();
        // #sijapp cond.if modules_TRAFFIC is "true" #
        // Save traffic
        Traffic.getInstance().safeSave();
        // #sijapp cond.end#
        display.hide();
    }

    public boolean isPaused() {
        return paused || display.isPaused();
    }

    public boolean isLocked() {
        return locked;
    }

    public void lockJimm() {
        final long now = Jimm.getCurrentGmtTime();
        final int WAITING_INTERVAL = 3; // sec
        if (lastLockTime + WAITING_INTERVAL  < now) {
            locked = true;
            splash.lockJimm();
            // #sijapp cond.if modules_ABSENCE is "true" #
            AutoAbsence.instance.away();
            // #sijapp cond.end #
        }
    }
    public void unlockJimm() {
        lastLockTime = Jimm.getCurrentGmtTime();
        locked = false;
        cl.activate();
        uiUpdater.refreshClock();
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.online();
        // #sijapp cond.end #
    }

    public void maximize() {
        restore(display.getCurrentDisplay());
        wakeUp();
    }
    public void maximize(CanvasEx screen) {
        restore(screen);
        wakeUp();
    }
    // Set the minimize state of midlet
    public void minimize() {
        hideApp();
        display.hide();
    }
    public void wakeUp() {
        paused = false;
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

    public static Jimm getJimm() {
        return instance;
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    public static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (Exception ignored) {
        }
    }

    public ContactList getCL() {
        return cl;
    }

    public MessageEditor getMessageEditor() {
        return editor;
    }

    public StatusView getStatusView() {
        return statusView;
    }

    public void autoConnect() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (!ru.net.jimm.JimmActivity.getInstance().isNetworkAvailable()) {
            return;
        }
        // #sijapp cond.end#
        int count = Jimm.getJimm().jimmModel.protocols.size();
        for (int i = 0; i < count; ++i) {
            Protocol p = (Protocol) Jimm.getJimm().jimmModel.protocols.elementAt(i);
            if (!"".equals(p.getPassword()) && p.getProfile().isConnected()) {
                p.connect();
            }
        }
    }

    private void initDaemonPhase1() {
        // phase #1: daemon
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.config.HomeDirectory.init();
        // #sijapp cond.end#
        JLocale.loadLanguageList();
        Options.loadOptions();
        // #sijapp cond.if modules_ANDROID is "true" #
        new ru.net.jimm.config.Options().load();
        // #sijapp cond.end#
    }
    private void initUiPhase2() {
        // phase #2; ui
        Scheme.load();
        JLocale.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
        CanvasEx.updateUI();
    }
    private void initDaemonPhase3() {
        // phase #3: daemon
        Options.loadAccounts();
        jimmModel = new JimmModel();
        jimmModel.updateAccounts();
    }
    private void initUiPhase4() {
        splash.setMessage(JLocale.getString("loading"));
        // #sijapp cond.if modules_ANDROID isnot "true" #
        splash.show();
        // #sijapp cond.end #
        splash.setProgress(10);

        cl = new ContactList();
        statusView = new StatusView();
        // back loading (traffic, sounds and light)
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
        editor = new MessageEditor();
        Jimm.gc();

        // #sijapp cond.if modules_DEBUGLOG is "true"#
        DebugLog.startTests();
        // #sijapp cond.end#

        // init contact list
        splash.setProgress(20);
        splash.setProgress(50);
        cl.initUI();
        splash.setProgress(60);
        cl.updateCl();

        if (null != uiUpdater) uiUpdater.stop();
        uiUpdater = new UIUpdater();
        uiUpdater.startUIUpdater();
        // #sijapp cond.if modules_ACTIVITYUI is "true"#
        if (phone.isPhone(PhoneInfo.PHONE_SE) && (750 <= phone.getSeVersion())) {
            activity = new ActivityUI();
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.started();
        // #sijapp cond.end #
    }
}