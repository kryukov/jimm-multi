package ru.net.jimm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import jimm.Jimm;
import jimm.cl.ContactList;
import org.microemu.MIDletBridge;
import protocol.Protocol;

public class NetworkStateReceiver extends BroadcastReceiver {
    private String previousNetworkType = null;
    private boolean isNetworkAvailable = false;

    private boolean modeNotChanged(String networkType) {
        return  (null == previousNetworkType)
                ? (null == networkType)
                : previousNetworkType.equals(networkType);
    }

    public IntentFilter getFilter() {
        return new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public boolean updateNetworkState(Context context) {
        String networkType = getConnectionType(context);
        if (modeNotChanged(networkType)) return false;
        previousNetworkType = networkType;
        isNetworkAvailable = (null != networkType);
        return true;
    }

    @Override
    public void onReceive(Context context, Intent networkIntent) {
        try {
            if (updateNetworkState(context)) {
                if (null == MIDletBridge.getCurrentMIDlet()) return;
                resetConnections();
                if (isNetworkAvailable) {
                    restoreConnections();
                }
            }
        } catch (Exception ignored) {
        }
    }


    private String getConnectionType(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if ((null != activeNetwork) && activeNetwork.isConnected()) {
                return activeNetwork.getTypeName();
            }

            return null;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void resetConnections() {
        for (Protocol p : Jimm.getJimm().jimmModel.getProtocols()) {
            p.disconnect(false);
        }
    }

    private void restoreConnections() {
        Jimm.getJimm().getCL().autoConnect();
    }
}