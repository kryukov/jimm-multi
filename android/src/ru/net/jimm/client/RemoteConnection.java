package ru.net.jimm.client;

import android.content.ComponentName;
import android.os.*;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 13.07.13 16:26
 *
 * @author vladimir
 */
class RemoteConnection implements Connection {
    private Messenger mService = null;

    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = new Messenger(service);
    }

    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        mService = null;
    }

    public void send(Message msg) {
        try {
            mService.send(msg);
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("JimmServiceConnection", e);
        }
    }
}
