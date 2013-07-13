package ru.net.jimm.service;

import android.content.Context;
import android.os.PowerManager;
import jimm.Jimm;
import jimm.cl.JimmModel;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 25.05.13 17:09
 *
 * @author vladimir
 */
public class WakeControl {
    private static final String LOCK_TAG = "JimmService";
    private final JimmService service;
    private PowerManager.WakeLock wakeLock;

    public WakeControl(JimmService jimmService) {
        service = jimmService;

    }

    public void release() {
        if (isHeld()) wakeLock.release();
        wakeLock = null;
    }
    public void updateLock() {
        JimmModel cl = Jimm.getJimm().jimmModel;
        boolean need = cl.isConnected() || cl.isConnecting();
        if (need) {
            if (!isHeld()) acquire();
        } else {
            if (isHeld()) release();
        }

    }

    private void acquire() {
        PowerManager powerManager = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, LOCK_TAG);
        if (null != wakeLock) {
            wakeLock.acquire();
        }
    }

    private boolean isHeld() {
        return (null != wakeLock) && wakeLock.isHeld();
    }
}
