/*
 * UIUpadter.java
 *
 * Created on 22 ���� 2007 �., 23:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import java.util.*;
import jimm.Jimm;
import jimm.Options;
import jimm.cl.ContactList;
import jimm.ui.*;

/**
 *
 * @author vladimir
 */
public class UIUpdater extends TimerTask {
    private static final int FLASH_COUNTER = 16;
    private static final int SYS_FLASH_COUNTER = 16*3;

    private static UIUpdater uiUpdater;
    private static Timer uiTimer;

    private Object displ = null;
    private String text = null;
    private int counter;
    private long timestamp = 0;

    private static final int FLASH_CAPTION_INTERVAL = 250;
    private int flashCaptionInterval;

    public static void startUIUpdater() {
        if (null != uiTimer) {
            uiTimer.cancel();
        }
        uiTimer = new Timer();
        uiUpdater = new UIUpdater();
        refreshClock();
        uiTimer.schedule(uiUpdater, 0, NativeCanvas.UIUPDATE_TIME);
    }

    public static void startFlashCaption(Object disp, String text) {
        if (null == disp) return;
        if (null == text) return;
        Object prevDisplay = uiUpdater.displ;
        uiUpdater.displ = null;
        if (null != prevDisplay) {
            uiUpdater.setTicker(prevDisplay, null);
        }
        uiUpdater.setTicker(disp, text);
        uiUpdater.text  = text;
        uiUpdater.counter = (disp instanceof InputTextBox) ? SYS_FLASH_COUNTER : FLASH_COUNTER;
        uiUpdater.flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        uiUpdater.displ = disp;
    }

    private void taskFlashCaption(Object curDispay) {
        flashCaptionInterval -= NativeCanvas.UIUPDATE_TIME;
        if (0 < flashCaptionInterval) {
            return;
        }
        flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        if (0 < counter) {
            if (curDispay instanceof VirtualList) {
                setTicker(curDispay, ((counter & 1) == 0) ? text : " ");
            }
            counter--;

        } else {
            setTicker(curDispay, null);
            displ = null;
        }
    }
    private void setTicker(Object displ, String text) {
        if (displ instanceof InputTextBox) {
            ((InputTextBox)displ).setTicker(text);
        } else if (displ instanceof VirtualList) {
            ((VirtualList)displ).setTicker(text);
        }
    }

    public void run() {
        try {
            update();
        } catch (OutOfMemoryError out) {
            // nothing
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("UIUpdater", e);
            // #sijapp cond.end #
        }
    }

    public static void refreshClock() {
        uiUpdater.timestamp = Jimm.getCurrentGmtTime() / 60;
        NativeCanvas.getInstance().getProtoCanvas().refreshClock();
    }
    private void updateClock() {
        if (0 == NativeCanvas.getInstance().getProtoCanvas().getSoftBarHeight()) {
            return;
        }
        CanvasEx c = NativeCanvas.getInstance().getCanvas();
        if ((null == c) || !c.isSoftBarShown()) {
            return;
        }
        long newTime = Jimm.getCurrentGmtTime() / 60;
        if (timestamp != newTime) {
            timestamp = newTime;
            NativeCanvas.getInstance().getProtoCanvas().refreshClock();
        }
    }
    private void update() {
        // flash caption task
        Object curDispay = displ;
        if (null != curDispay) {
            taskFlashCaption(curDispay);
        }
        long microTime = System.currentTimeMillis();

        ContactList.getInstance().timerAction();

        NativeCanvas canvas = NativeCanvas.getInstance();
        if (!canvas.isShown()) {
            return;
        }

        // UI update task
        CanvasEx current = canvas.getCanvas();
        if (null != current) {
            try {
                current.updateTask(microTime);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("updateTask", e);
                // #sijapp cond.end #
            }
            if (0 < GraphicsEx.showScroll) {
                GraphicsEx.showScroll--;
                if (0 == GraphicsEx.showScroll) {
                    current.invalidate();
                }
            }
        }

        // Time update task
        updateClock();
    }
}
