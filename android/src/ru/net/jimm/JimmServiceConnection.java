package ru.net.jimm;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import ru.net.jimm.service.JimmService;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.07.12 1:01
 *
 * @author vladimir
 */
public class JimmServiceConnection implements ServiceConnection {
    private JimmService jimmService = null;

    public void onServiceConnected(ComponentName className, IBinder service) {
        jimmService = ((ru.net.jimm.service.JimmService.LocalBinder)service).getService();
    }

    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        jimmService = null;
    }

    private void send(Message msg) {
        try {
            jimmService.handleMessage(msg);
        } catch (Exception e) {
            // do nothing
        }
    }
    public void updateAppIcon() {
        send(Message.obtain(null, JimmService.UPDATE_APP_ICON));
    }
    public void updateConnectionState() {
        send(Message.obtain(null, JimmService.UPDATE_CONNECTION_STATUS));
    }
}
