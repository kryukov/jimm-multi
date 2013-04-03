package ru.net.jimm;

import android.text.ClipboardManager;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 03.04.13 15:10
 *
 * @author vladimir
 */
public class Clipboard {
    private JimmActivity activity;
    private final Object lock = new Object();

    public Clipboard() {
    }

    void setActivity(JimmActivity activity) {
        this.activity = activity;
    }

    public String get() {
        final AtomicReference<String> text = new AtomicReference<String>();
        text.set(null);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);
                    text.set(clipboard.hasText() ? clipboard.getText().toString() : null);
                    synchronized (lock) {
                        lock.notify();
                    }
                } catch (Throwable e) {
                    jimm.modules.DebugLog.panic("get clipboard", e);
                    // do nothing
                }
            }
        });
        if (!activity.isActivityThread()) try {
            synchronized (lock) {
                lock.wait();
            }
            //Thread.sleep(100);
        } catch (Exception ignored) {
        }
        return text.get();
    }

    public void put(final String text) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);
                    clipboard.setText(text);
                } catch (Throwable e) {
                    jimm.modules.DebugLog.panic("set clipboard", e);
                    // do nothing
                }
            }
        });
    }
}
