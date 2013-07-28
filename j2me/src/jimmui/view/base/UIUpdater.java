/*
 * UIUpadter.java
 *
 * Created on 22 ���� 2007 �., 23:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.base;

import java.util.*;
import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.view.*;
import protocol.Contact;
import protocol.ui.MessageEditor;
import protocol.Protocol;
import protocol.ui.InfoFactory;

/**
 *
 * @author vladimir
 */
public class UIUpdater extends TimerTask {
    private static final int FLASH_COUNTER = 16;
    private static final int SYS_FLASH_COUNTER = 16*3;

    private Timer uiTimer;

    private Object displ = null;
    private String text = null;
    private int counter;
    private long timestamp = 0;

    private static final int FLASH_CAPTION_INTERVAL = 250;
    private int flashCaptionInterval;


    public void startUIUpdater() {
        uiTimer = new Timer();
        refreshClock();
        uiTimer.schedule(this, 0, NativeCanvas.UIUPDATE_TIME);
    }
    public void stop() {
        uiTimer.cancel();
    }

    public void showTopLine(Protocol protocol, Contact contact, String nick, byte statusIndex) {
        if (contact != Jimm.getJimm().getCL().getUpdater().getCurrentContact()) {
            return;
        }
        Object vis = null;
        MessageEditor editor = Jimm.getJimm().getMessageEditor();
        if ((null != editor) && editor.getTextBox().isShown()) {
            vis = editor.getTextBox();
        } else if (contact.hasChat()) {
            ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(contact);
            vis = Jimm.getJimm().getCL().getChat(chat);
        }
        if (null == vis) return;

        String text = InfoFactory.factory.getStatusInfo(protocol).getName(statusIndex);
        if (null == text) return;
        if (null != nick) text = nick + ": " + text;

        Object prevDisplay = displ;
        this.displ = null;
        if (null != prevDisplay) {
            setTicker(prevDisplay, null);
        }
        setTicker(vis, text);
        this.text  = text;
        this.counter = (vis instanceof InputTextBox) ? SYS_FLASH_COUNTER : FLASH_COUNTER;
        this.flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        this.displ = vis;
    }

    private void taskFlashCaption(Object curDispay) {
        flashCaptionInterval -= NativeCanvas.UIUPDATE_TIME;
        if (0 < flashCaptionInterval) {
            return;
        }
        flashCaptionInterval = FLASH_CAPTION_INTERVAL;
        if (0 < counter) {
            if (curDispay instanceof SomeContentList) {
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
        } else if (displ instanceof SomeContentList) {
            ((SomeContentList)displ).bar.setTicker(text);
            ((SomeContentList)displ).invalidate();
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

    private void refreshClock(long timestamp) {
        this.timestamp = timestamp;
        CanvasEx canvas = Jimm.getJimm().getDisplay().getNativeCanvas().getCanvas();
        if ((null == canvas) || canvas.isSoftBarShown()) {
            MySoftBar.refreshClock();
            MyActionBar.refreshClock();
        }
    }
    public void refreshClock() {
        refreshClock(Jimm.getCurrentGmtTime() / 60);
    }
    private void updateClock() {
        CanvasEx c = Jimm.getJimm().getDisplay().getNativeCanvas().getCanvas();
        if ((null == c) || !c.isSoftBarShown()) {
            return;
        }
        long newTime = Jimm.getCurrentGmtTime() / 60;
        if (timestamp != newTime) {
            refreshClock(newTime);
        }
    }
    private void update() {
        // flash caption task
        Object curDispay = displ;
        if (null != curDispay) {
            taskFlashCaption(curDispay);
        }
        long microTime = System.currentTimeMillis();

        Jimm.getJimm().getCL().timerAction();

        NativeCanvas canvas = Jimm.getJimm().getDisplay().getNativeCanvas();
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
            if (0 < MyScrollBar.showScroll) {
                MyScrollBar.showScroll--;
                if (0 == MyScrollBar.showScroll) {
                    current.invalidate();
                }
            }
        }

        // Time update task
        updateClock();
    }
}
