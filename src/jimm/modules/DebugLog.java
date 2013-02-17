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
File: src/jimm/DebugLog.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Artyomov Denis
*******************************************************************************/

// #sijapp cond.if modules_DEBUGLOG is "true" #
package jimm.modules;

import jimm.ui.text.TextListModel;
import jimm.ui.text.TextList;
import DrawControls.text.Parser;
import jimm.Jimm;
import jimm.comm.MD5;
import jimm.comm.Util;
import jimm.ui.base.CanvasEx;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.util.*;

public final class DebugLog implements SelectListener {
    private static final DebugLog instance = new DebugLog();
    private TextListModel model = new TextListModel();
    private TextList list = null;

    private DebugLog() {
    }

    public static void activate() {
        if (null == instance.list) {
            instance.list = new TextList(JLocale.getString("debug log"));
            instance.list.setModel(instance.model);
        }
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     MENU_COPY);
        menu.addItem("copy_all_text", MENU_COPY_ALL);
        menu.addItem("clear",         MENU_CLEAN);
        menu.addItem("Properties",    MENU_PROPERTIES);
        menu.setActionListener(instance);
        instance.list.setController(new TextListController(menu, -1));

        instance.list.setAllToBottom();
        instance.list.show();
    }

    private static final int MENU_COPY       = 0;
    private static final int MENU_COPY_ALL   = 1;
    private static final int MENU_CLEAN      = 2;
    private static final int MENU_PROPERTIES = 3;


    public void select(Select select, MenuModel menu, int action) {
        switch (action) {
            case MENU_COPY:
            case MENU_COPY_ALL:
                list.getController().copy(action == MENU_COPY_ALL);
                list.restore();
                break;

            case MENU_CLEAN:
                synchronized (instance) {
                    model.clear();
                    list.updateModel();
                }
                list.restore();
                break;

            case MENU_PROPERTIES:
                dumpProperties();
                list.restore();
                break;
        }
    }

    private void removeOldRecords() {
        final int maxRecordCount = 200;
        while (maxRecordCount < model.getSize()) {
            if (null == list) {
                model.removeFirst();
            } else {
                list.removeFirstText();
            }
        }
    }

    public static void memoryUsage(String str) {
        long size = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        size = (size + 512) / 1024;
        println(str + " = " + size + "kb.");
    }

    private static String _(String text) {
        if (null == text) {
            return "";
        }
        try {
            String text1 = JLocale.getString(text);
            if (!text1.equals(text)) {
                return "[l] " + text1;
            }
        } catch (Exception ignored) {
        }
        return text;
    }
    public static void systemPrintln(String text) {
        System.out.println(text);
    }
    private synchronized void print(String text) {
        Parser record = model.createNewParser(true);
        String date = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        record.addText(date + ": ", CanvasEx.THEME_MAGIC_EYE_NUMBER,
                CanvasEx.FONT_STYLE_PLAIN);
        record.addText(_(text), CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);

        model.addPar(record);
        removeOldRecords();
        if (null != list) {
            list.updateModel();
        }
    }
    public static void println(String text) {
        System.out.println(text);
        instance.print(text);
    }

    public static void panic(String str) {
        try {
            // make stack trace...
            throw new Exception();
        } catch (Exception e) {
            panic(str, e);
        }
    }
    public static void assert0(String str, String result, String heed) {
        //println(str + " " + result + " " + heed);
        assert0(str, (result != heed) && !result.equals(heed));
    }
    public static void assert0(String str, boolean result) {
        if (result) {
            try {
                // make stack trace...
                throw new Exception();
            } catch (Exception e) {
                println("assert: " + _(str));
                e.printStackTrace();
            }
        }
    }

    private long freeMemory() {
        for (int i = 0; i < 10; ++i) {
            jimm.Jimm.gc();
        }
        return Runtime.getRuntime().freeMemory();
    }

    public static void panic(String str, Throwable e) {
        System.err.println("panic: " + _(str));
        String text = "panic: " + _(str) + " "  + e.getMessage()
                + " (" + e.getClass().getName() + ")";
        // #sijapp cond.if modules_ANDROID is "true"#
        for (StackTraceElement ste : e.getStackTrace()) {
            text += String.format("\n%s.%s() %d", ste.getClassName(), ste.getMethodName(), ste.getLineNumber());
        }
        // #sijapp cond.end#
        println(text);
        e.printStackTrace();
    }

    private static long profilerTime;
    public static long profilerStart() {
        profilerTime = System.currentTimeMillis();
        return profilerTime;
    }
    public static long profilerStep(String str, long startTime) {
        long now = System.currentTimeMillis();
        println("profiler: " + _(str) + ": " + (now - startTime));
        return now;
    }
    public static void profilerStep(String str) {
        long now = System.currentTimeMillis();
        println("profiler: " + _(str) + ": " + (now - profilerTime));
        profilerTime = now;
    }

    private void testOAuth() {

    }
    public static void startTests() {
        println("1329958015 " + Util.createGmtTime(2012, 02, 23, 4, 46, 55));

        println("TimeZone info");
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        println("TimeZone offset: " + tz.getRawOffset());
        println("Daylight: " + tz.useDaylightTime());
        println("ID: " + tz.getID());

        MD5 md5 = new MD5();
        md5.init();
        md5.updateASCII("\u0422\u0435\u0441\u0442");
        md5.finish();
        assert0("md5 (ru): failed", !md5.getDigestHex().equals("16497fa0c8e13ce8fab874d959db91b9"));

        md5 = new MD5();
        md5.init();
        md5.updateASCII("Test");
        md5.finish();
        assert0("md5 (en): failed", !md5.getDigestHex().equals("0cbc6611f5540bd0809a388dc95a615b"));

        assert0("bs64decode (0): failed", MD5.decodeBase64(" eg=="), "z");
        assert0("bs64decode (1): failed", MD5.decodeBase64("eg=="), "z");
        assert0("bs64decode (2): failed", MD5.decodeBase64("eno="), "zz");
        assert0("bs64decode (3): failed", MD5.decodeBase64("enp6"), "zzz");
        assert0("bs64decode (4): failed", MD5.decodeBase64(" eg==\n"), "z");
        assert0("bs64decode (5): failed", MD5.decodeBase64("eg==\n"), "z");
        assert0("bs64decode (6): failed", MD5.decodeBase64("eno=\n"), "zz");
        assert0("bs64decode (7): failed", MD5.decodeBase64("enp6\n"), "zzz");
        assert0("bs64 (1): failed", MD5.toBase64(new byte[]{'z'}), "eg==");
        assert0("bs64 (2): failed", MD5.toBase64(new byte[]{'z', 'z'}), "eno=");
        assert0("bs64 (3): failed", MD5.toBase64(new byte[]{'z', 'z', 'z'}), "enp6");

        assert0("replace (1): failed", Util.replace("text2text23", "2", "3"), "text3text33");
        assert0("replace (2): failed", Util.replace("text22text2223", "22", "3"), "text3text323");
        assert0("replace (3): failed", Util.replace("text22text22", "22", "3"), "text3text3");
        assert0("replace (4): failed", Util.replace("text3text33", "22", "3"), "text3text33");
        //protocol.icq.ClientDetector.g();
    }
    public static void dumpProperties() {
        println("RamFree: "   + System.getProperty("com.nokia.memoryramfree"));
        println("Network: "   + System.getProperty("com.nokia.mid.networkid"));
        //println("Avaliable: " + System.getProperty("com.nokia.mid.networkavailability"));
        //println("Status: "    + System.getProperty("com.nokia.mid.networkstatus"));
        println("Signal: "    + System.getProperty("com.nokia.mid.networksignal"));
        println("Indicator: " + System.getProperty("com.nokia.canvas.net.indicator.location"));
        //println("SellId: "    + System.getProperty("com.nokia.mid.cellid"));
        println("Point: "     + System.getProperty("com.nokia.network.access"));

        println("Battery: " + batteryLevel());
        println("Params: "     + System.getProperty("com.nokia.mid.cmdline"));
        //println("Dir: "     + System.getProperty("fileconn.dir.private"));
        //println("CameraActivity: "  + System.getProperty("camera.orientations"));

        //println("Soft1 "  + System.getProperty("com.nokia.softkey1.label.location"));
        //println("Soft2 "  + System.getProperty("com.nokia.softkey2.label.location"));
        //println("Soft3 "  + System.getProperty("com.nokia.softkey3.label.location"));
    }
    private static String batteryLevel() {
        String level = System.getProperty("com.nokia.mid.batterylevel");
        if (null == level) {
            level = System.getProperty("batterylevel");
        }
        return level;
    }


    public static void dump(String comment, byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("dump: ").append(comment).append(":\n");
        for (int i = 0; i < data.length; ++i) {
            String hex = Integer.toHexString(((int)data[i]) & 0xFF);
            if (1 == hex.length()) sb.append(0);
            sb.append(hex);
            sb.append(" ");
            if (i % 16 == 15) sb.append("\n");
        }
        println(sb.toString());
    }
}
// #sijapp cond.end#