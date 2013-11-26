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
    private long keepAliveInterval;
    private boolean usePong;
    protected boolean connect;
    private Vector<PlainMessage> messages = new Vector<PlainMessage>();

    // ping only
    private long nextPingTime;
    private long pongTime;

    private static final int PING_INTERVAL = 90 /* sec */;
    private static final int PONG_TIMEOUT = 4 * 60 /* sec */;


    protected final void setPingInterval(long interval) {
        keepAliveInterval = Math.min(keepAliveInterval, interval);
        nextPingTime = Jimm.getCurrentGmtTime() + keepAliveInterval;
    }
    protected final long getPingInterval() {
        return keepAliveInterval;
    }
    protected final void usePong() {
        if (Jimm.getJimm().phone.isCedar()) {
            return;
        }
        usePong = true;
        updateTimeout();
    }

    private void initPingValues() {
        usePong = false;
        keepAliveInterval = PING_INTERVAL;
        nextPingTime = Jimm.getCurrentGmtTime() + keepAliveInterval;
    }
    public final void start() {
        new Thread(this).start();
    }
    public final void run() {
        initPingValues();
        JimmException exception = null;
        try {
            getProtocol().setConnectingProgress(0);
            connect();

            while (isConnected()) {
                boolean doing = processPacket();
                if (!doing) {
                    sleep(250);
                    doPingIfNeeded();
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

    private void doPingIfNeeded() throws JimmException {
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
            nextPingTime = now + keepAliveInterval;
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
        markMessageSent(-1, -1);
    }
    private PlainMessage getMessage(long msgId) {
        if (-1 < msgId) {
            for (int i = 0; i < messages.size(); ++i) {
                PlainMessage m = (PlainMessage)messages.elementAt(i);
                if (m.getMessageId() == msgId) {
                    return m;
                }
            }
        }
        return null;
    }
    public final boolean isMessageExist(long msgId) {
        return null != getMessage(msgId);
    }
    public final void markMessageSent(long msgId, int status) {
        PlainMessage msg = getMessage(msgId);
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
