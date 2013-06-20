package ru.net.jimm.service;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import jimm.chat.ChatHistory;
import jimm.cl.ContactList;
import org.bombusmod.scrobbler.MusicReceiver;
import ru.net.jimm.JimmActivity;
import ru.net.jimm.R;
import ru.net.jimm.Tray;

public class JimmService extends Service {
    public static final String ACTION_FOREGROUND = "FOREGROUND";

    private static final String LOG_TAG = "JimmService";

    private final Binder localBinder = new LocalBinder();
    private MusicReceiver musicReceiver;
    private Tray tray = null;
    private WakeControl wakeLock;

    public static final int UPDATE_APP_ICON = 1;
    public static final int UPDATE_CONNECTION_STATUS = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onStart();");
        wakeLock = new WakeControl(this);

        tray = new Tray(this);
        //Foreground Service
        //tray.startForegroundCompat(R.string.app_name, getNotification());

        //musicReceiver = new MusicReceiver(this);
        //this.registerReceiver(musicReceiver, musicReceiver.getIntentFilter());
        //scrobbling finished
    }



    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy();");
        wakeLock.release();
        //this.unregisterReceiver(musicReceiver);
        tray.stopForegroundCompat(R.string.app_name);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return localBinder;
    }

    private Notification getNotification() {
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        CharSequence stateMsg = "";

        boolean version2 = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB);
        final int icon;
        if (0 < allUnread) {
            icon = version2 ? R.drawable.ic2_tray_msg : R.drawable.ic3_tray_msg;
        } else if (ContactList.getInstance().isConnected()) {
            icon = version2 ? R.drawable.ic2_tray_on : R.drawable.ic3_tray_on;
            stateMsg = getText(R.string.online);
        } else {
            icon = version2 ? R.drawable.ic2_tray_off : R.drawable.ic3_tray_off;
            if (ContactList.getInstance().isConnecting()) {
                stateMsg = getText(R.string.connecting);
            } else {
                stateMsg = getText(R.string.offline);
            }
        }

        final Notification notification = new Notification(icon, getText(R.string.app_name), 0);
        if (0 < unread) {
            notification.ledARGB = 0xff00ff00;
            notification.ledOnMS = 300;
            notification.ledOffMS = 1000;
            notification.flags |= android.app.Notification.FLAG_SHOW_LIGHTS;
        }
        if (0 < allUnread) {
            notification.number = allUnread;
            stateMsg = String.format((String) getText(R.string.unreadMessages), allUnread);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, JimmActivity.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name), stateMsg, contentIntent);
        return notification;
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_CONNECTION_STATUS:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                wakeLock.updateLock();
                break;
            case UPDATE_APP_ICON:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public JimmService getService() {
            return JimmService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
}
