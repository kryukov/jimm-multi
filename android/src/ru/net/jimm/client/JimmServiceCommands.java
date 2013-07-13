package ru.net.jimm.client;

import android.os.Message;
import jimm.Jimm;
import protocol.Protocol;
import ru.net.jimm.service.JimmService;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.07.12 1:01
 *
 * @author vladimir
 */
public class JimmServiceCommands {

    public Connection connection = new LocalConnection();

    public void updateAppIcon() {
        connection.send(Message.obtain(null, JimmService.UPDATE_APP_ICON));
    }
    public void updateConnectionState() {
        connection.send(Message.obtain(null, JimmService.UPDATE_CONNECTION_STATUS));
    }
    public void connect(Protocol p) {
        int protocolIndex = Jimm.getJimm().jimmModel.protocols.indexOf(p);
        jimm.modules.DebugLog.println("connect to " + p.getUserId() + " " + protocolIndex);
        if (0 <= protocolIndex) {
            connection.send(Message.obtain(null, JimmService.CONNECT, protocolIndex, 0));
        }
    }

    public void started() {
        connection.send(Message.obtain(null, JimmService.STARTED));
    }
    public void quit() {
        connection.send(Message.obtain(null, JimmService.QUIT));
    }
}
