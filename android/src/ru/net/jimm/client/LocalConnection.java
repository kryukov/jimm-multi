package ru.net.jimm.client;

import android.content.*;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import ru.net.jimm.service.JimmService;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 13.07.13 16:27
 *
 * @author vladimir
 */
class LocalConnection implements Connection {
    private Messenger mService = null;
    private JimmService jimmService = null;

    public void onServiceConnected(ComponentName className, IBinder service) {
        if (service instanceof JimmService.LocalBinder) {
            jimmService = ((JimmService.LocalBinder)service).getService();
        } else {
            mService = new Messenger(service);
        }
    }

    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        jimmService = null;
        mService = null;
    }

    public void send(Message msg) {
        try {
            if (null != jimmService) {
                jimmService.handleMessage(msg);
            } else {
                mService.send(msg);
            }
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("JimmServiceConnection", e);
        }
    }
}
