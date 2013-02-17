/*
 * ClientConnection.java
 *
 * Created on 13 Февраль 2011 г., 16:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.net;

import java.util.Vector;
import jimm.Jimm;
import jimm.JimmException;
import jimm.chat.message.PlainMessage;
import jimm.modules.*;
import protocol.Protocol;

/**
 *
 * @author Vladimir Kryukov
 */
public abstract class ClientConnection implements Runnable {
    private long keepAliveInterv;
    private boolean usePong;
    protected boolean connect;
    private Vector messages = new Vector();

    // ping only
    private long nextPingTime;
    private long pongTime;

    private static final int PING_INTERVAL = 2 * 60 /* sec */;
    private static final int PONG_TIMEOUT = 3 * 60 /* sec */;


    protected final void setPingInterval(long interval) {
        keepAliveInterv = Math.min(keepAliveInterv, interval);
        nextPingTime = Jimm.getCurrentGmtTime() + keepAliveInterv;
    }
    protected final long getPingInterval() {
        return keepAliveInterv;
    }
    protected final void usePong() {
        usePong = true;
        updateTimeout();
    }

    private void initPingValues() {
        usePong = false;
        keepAliveInterv = PING_INTERVAL;
        nextPingTime = Jimm.getCurrentGmtTime() + keepAliveInterv;
    }
    public final void start() {
        new Thread(this).start();
    }
    public final void run() {
        initPingValues();
        JimmException exception = null;
        try {
            connect();

            while (isConnected()) {
                boolean doing = processPacket();
                if (!doing) {
                    sleep(250);
                    doPingIfNeeeded();
                }
            }

        } catch (JimmException e) {
            exception = e;

        } catch (OutOfMemoryError err) {
            exception = new JimmException(100, 2);

        } catch (Exception ex) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            if (null != getProtocol()) {
                DebugLog.panic("die " + getId(), ex);
            }
            // #sijapp cond.end#
            exception = new JimmException(100, 1);
        }
        if (null != exception) {
            try {
                Protocol p = getProtocol();
                if (null != p) {
                    p.processException(exception);
                }
            } catch (Exception ex) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("die2 " + getId(), ex);
                // #sijapp cond.end#
            }
        }
        disconnect();
        try {
            closeSocket();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("die3 " + getId(), e);
            // #sijapp cond.end#
        }
        connect = false;
    }
    // #sijapp cond.if modules_DEBUGLOG is "true" #
    private String getId() {
        Protocol p = getProtocol();
        if (null != p) {
            return p.getUserId();
        }
        return "" + this;
    }
    // #sijapp cond.end#
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    private void doPingIfNeeeded() throws JimmException {
        long now = Jimm.getCurrentGmtTime();
        if (usePong && (pongTime + PONG_TIMEOUT < now)) {
            throw new JimmException(120, 9);
        }
        if (nextPingTime <= now) {
            if (usePong) {
                pingForPong();

            } else {
                ping();
            }
            nextPingTime = now + keepAliveInterv;
        }
    }
    protected final void updateTimeout() {
        pongTime = Jimm.getCurrentGmtTime();
    }
    public final boolean isConnected() {
        return connect;
    }

    public final void addMessage(PlainMessage msg) {
        messages.addElement(msg);
        markMessageSended(-1, -1);
    }
    public final boolean isMessageExist(long msgId) {
        if (-1 < msgId) {
            PlainMessage msg = null;
            for (int i = 0; i < messages.size(); ++i) {
                PlainMessage m = (PlainMessage)messages.elementAt(i);
                if (m.getMessageId() == msgId) {
                    return true;
                }
            }
        }
        return false;
    }
    public final void markMessageSended(long msgId, int status) {
        PlainMessage msg = null;
        for (int i = 0; i < messages.size(); ++i) {
            PlainMessage m = (PlainMessage)messages.elementAt(i);
            if (m.getMessageId() == msgId) {
                msg = m;
                break;
            }
        }
        if (null != msg) {
            msg.setSendingState(status);
            if (PlainMessage.NOTIFY_FROM_CLIENT == status) {
                messages.removeElement(msg);
            }
        }

        long date = Jimm.getCurrentGmtTime() - 5 * 60;
        for (int i = messages.size() - 1; i >= 0; --i) {
            PlainMessage m = (PlainMessage)messages.elementAt(i);
            if (date > m.getNewDate()) {
                messages.removeElement(m);
            }
        }
    }

    protected void pingForPong() throws JimmException {
    }
    public abstract void disconnect();
    protected abstract Protocol getProtocol();
    protected abstract void closeSocket();
    protected abstract void connect() throws JimmException;
    protected abstract void ping() throws JimmException;
    protected abstract boolean processPacket() throws JimmException;
}
