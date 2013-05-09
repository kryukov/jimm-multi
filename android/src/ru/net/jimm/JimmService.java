package ru.net.jimm;


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

public class JimmService extends Service {
    public static final String ACTION_FOREGROUND = "FOREGROUND";

    private static final String LOG_TAG = "JimmService";

    private final Messenger messenger = new Messenger(new Handler(new IncomingMessageHandler()));
    private MusicReceiver musicReceiver;
    private Tray tray = null;
    private PowerManager.WakeLock wakeLock;

    public static final int UPDATE_APP_ICON = 1;
    public static final int UPDATE_CONNECTION_STATUS = 2;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onStart();");
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
        if (null != wakeLock) {
            wakeLock.acquire();
        }

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
        if (wakeLock.isHeld()) wakeLock.release();
        //this.unregisterReceiver(musicReceiver);
        tray.stopForegroundCompat(R.string.app_name);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return messenger.getBinder();
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

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_CONNECTION_STATUS:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                break;
            case UPDATE_APP_ICON:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                break;
            default:
                return false;
        }
        return true;
    }

    private class IncomingMessageHandler implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            try {
                return JimmService.this.handleMessage(msg);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
