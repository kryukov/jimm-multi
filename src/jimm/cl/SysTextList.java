/*
 * SysTextListEx.java
 *
 * Created on 17 Июнь 2007 г., 21:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package jimm.cl;

import jimm.modules.*;
import DrawControls.text.Parser;
import java.util.*;
import jimm.*;
import jimm.comm.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.ui.text.*;
import jimm.ui.timers.*;
import jimm.util.*;

/**
 *
 * @author vladimir
 */
public final class SysTextList extends TextListController {
    private TextListModel model = new TextListModel();

    private void copy() {
        String text = getCurrText();
        if (null != text) {
            JimmUI.setClipBoardText(list.getCaption(), text);
        }
    }

    private String getCurrText() {
        return list.getModel().getParText(list.getCurrItem());
    }
    ///////////////////////////////////////////////////////////////////////////
    protected void doJimmAction(int action) {
        if ((NativeCanvas.JIMM_SELECT == action) && (MENU_OPENURL == defaultCode)) {
            list.restore();
            String str = getCurrText();
            if (null != str) {
                if (-1 != str.indexOf("http://")) {
                    Jimm.openUrl(str);
                } else {
                    GetVersion.updateProgram();
                }
            }
            return;
        }
        doJimmBaseAction(action);
        switch (action) {
            case MENU_UPDATE:
                GetVersion.updateProgram();
                break;

                // #sijapp cond.if modules_TRAFFIC is "true" #
            case MENU_RESET:
                Traffic.getInstance().reset();
                updateAbout();
                list.restore();
                break;
                // #sijapp cond.end#

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            case MENU_DEBUGLOG:
                DebugLog.activate();
                break;
            // #sijapp cond.end#

            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case MENU_MAGIC_EYE:
                MagicEye.activate();
                break;
            // #sijapp cond.end#

            case URL_MENU_GOTO:
                list.back();
                Jimm.openUrl(getCurrText());
                break;

            case URL_MENU_COPY:
                copy();
                list.restore();
                break;

            case URL_MENU_ADD:
                list.restore();
                Jimm.openUrl("xmpp:" + Util.getUrlWithoutProtocol(getCurrText()));
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    //                                                                          //
    // About                                                                    //
    //                                                                          //
    //////////////////////////////////////////////////////////////////////////////
    // String for recent version
    private static final int MENU_UPDATE = 0;
    private static final int MENU_OPENURL = -2;
    private static final int MENU_RESET = 3;

    private static final int MENU_DEBUGLOG = 20;
    private static final int MENU_MAGIC_EYE = 21;

    private static final int URL_MENU_GOTO = 10;
    private static final int URL_MENU_ADD = 11;
    private static final int URL_MENU_COPY = 12;

    public TextList makeAbout() {
        TextList aboutScreen = new TextList(JLocale.getString("about"));
        list = aboutScreen;

        updateAbout();

        MenuModel m = new MenuModel();
        // #sijapp cond.if modules_ANDROID isnot "true" #
        m.addItem("update_jimm", SysTextList.MENU_UPDATE);
        // #sijapp cond.end #
        // #sijapp cond.if modules_TRAFFIC is "true" #
        m.addItem("reset", SysTextList.MENU_RESET);
        // #sijapp cond.end #
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        m.addItem("debug log", MENU_DEBUGLOG);
        // #sijapp cond.end#
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        m.addItem("magic eye", MENU_MAGIC_EYE);
        // #sijapp cond.end#
        m.setActionListener(this);
        setMenu(m, MENU_OPENURL);
        aboutScreen.setController(this);

        return aboutScreen;
    }
    public void updateAbout() {
        model = new TextListModel();
        Jimm.gc();
        Jimm.gc();
        long freeMem = Runtime.getRuntime().freeMemory() / 1024;

        final String commaAndSpace = ", ";

        String[] params = Util.explode(JLocale.getString("about_info"), '\n');
        addPlainText("\n " + params[0] + "\n\n", CanvasEx.THEME_TEXT);
        addPlainText("" + params[2] + "\n", CanvasEx.THEME_TEXT);
        for (int i = 3; i < params.length; ++i) {
            int end = params[i].indexOf(':');
            if (-1 == end) {
                addPlainText("\n", CanvasEx.THEME_TEXT);
            } else {
                String key = params[i].substring(0, end);
                String value = params[i].substring(end + 1).trim();
                if (value.startsWith("http://")) {
                    addUrl(key, value);
                } else {
                    addAboutParam(key, value);
                }
            }
        }

        String partner = Jimm.getAppProperty("Jimm-Partner", null);
        if (!StringConvertor.isEmpty(partner)) {
            addUrl("Partner", StringConvertor.cut(partner, 50));
        }
        addPlainText("\n", CanvasEx.THEME_TEXT);

        String midpInfo = Jimm.microeditionPlatform;
        if (null != Jimm.microeditionProfiles) {
            midpInfo += commaAndSpace + Jimm.microeditionProfiles;
        }
        String locale = System.getProperty("microedition.locale");
        if (null != locale) {
            midpInfo += commaAndSpace + locale;
        }
        addAboutParam("midp_info", midpInfo);

        addPlainText("\n", CanvasEx.THEME_TEXT);
        addAboutParam("free_heap", freeMem + JLocale.getString("kb"));
        addAboutParam("total_mem", (Runtime.getRuntime().totalMemory() / 1024)
                + JLocale.getString("kb"));

        if (null != Jimm.lastDate) {
            addAboutParam("latest_ver", Jimm.lastDate);
        }

        // #sijapp cond.if modules_TRAFFIC is "true" #
        addPlainText("\n", CanvasEx.THEME_TEXT);
        Traffic t = Traffic.getInstance();
        // Traffic for a session
        int sessionIn  = t.getSessionInTraffic();
        int sessionOut = t.getSessionOutTraffic();
        int session    = sessionIn + sessionOut;
        int sessionCost = t.generateCostSum(sessionIn, sessionOut, false);
        addTrafficSection(JLocale.getString("session"),
                session,
                sessionCost);

        // Traffic since date
        int totalIn  = t.getAllInTraffic();
        int totalOut = t.getAllOutTraffic();
        int total    = totalIn + totalOut;
        int totalCost = t.generateCostSum(totalIn, totalOut, true);
        addTrafficSection(JLocale.getString("traffic_since") + " "
                + t.getTrafficString(),
                total,
                totalCost);
        // #sijapp cond.end #
        commit();

        list.setModel(model);
    }

    // #sijapp cond.if modules_TRAFFIC is "true" #
    private void addTrafficSection(String title, int total, int cost) {
        addPlainText(title, CanvasEx.THEME_TEXT);
        addPlainText(": ", CanvasEx.THEME_TEXT);
        addPlainText(StringConvertor.bytesToSizeString(total, false), CanvasEx.THEME_PARAM_VALUE);
        // The cost of the traffic
        if (0 < cost) {
            addPlainText(" (", CanvasEx.THEME_TEXT);
            addPlainText(Traffic.costToString(cost), CanvasEx.THEME_PARAM_VALUE);
            addPlainText(")", CanvasEx.THEME_TEXT);
        }
        addPlainText("\n", CanvasEx.THEME_TEXT);
    }
    // #sijapp cond.end #
    private void addAboutParam(String langStr, String str) {
        addPlainText(JLocale.getString(langStr) + ": ", CanvasEx.THEME_TEXT);
        addPlainText(str, CanvasEx.THEME_PARAM_VALUE);
        addPlainText("\n", CanvasEx.THEME_TEXT);
    }

    private void addUrl(String langStr, String url) {
        addPlainText(JLocale.getString(langStr) + ":\n", CanvasEx.THEME_TEXT);
        commit();
        Parser line = model.createNewParser(true);
        line.addText(url, CanvasEx.THEME_PARAM_VALUE, CanvasEx.FONT_STYLE_PLAIN);
        model.addPar(line);
    }

    private void addPlainText(String text, byte colorType) {
        getDefaultParser().addText(text, colorType, CanvasEx.FONT_STYLE_PLAIN);
    }

    ///////////////////////////////////////////////////////////////////////////
    private Parser defaultParser;

    private void commit() {
        if (null != defaultParser) {
            model.addPar(defaultParser);
        }
        defaultParser = null;
    }
    private Parser getDefaultParser() {
        if (null == defaultParser) {
            defaultParser = model.createNewParser(false);
        }
        return defaultParser;
    }
    ///////////////////////////////////////////////////////////////////////////
    public static void gotoURL(String text) {
        Vector urls = Util.parseMessageForURL(text);
        if (null == urls) {
            return;
        }

        TextList list = new TextList(JLocale.getString("goto_url"));

        MenuModel m = new MenuModel();
        m.addItem("select", URL_MENU_GOTO);
        // #sijapp cond.if protocols_JABBER is "true" #
        m.addItem("add_user", URL_MENU_ADD);
        // #sijapp cond.end #
        m.addItem("copy_text", URL_MENU_COPY);
        SysTextList c = new SysTextList();
        m.setActionListener(c);
        c.setMenu(m, URL_MENU_GOTO);
        list.setController(c);

        TextListModel urlModels = new TextListModel();
        for (int i = 0; i < urls.size(); ++i) {
            urlModels.addItem((String) urls.elementAt(i), false);
        }
        list.setModel(urlModels);

        Jimm.getJimm().getDisplay().closeMenus();
        list.show();
    }
}
