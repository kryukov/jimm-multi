package ru.net.jimm.client;

import android.content.ServiceConnection;
import android.os.Message;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 13.07.13 16:28
 *
 * @author vladimir
 */
interface Connection extends ServiceConnection {
    public void send(Message msg);
}
