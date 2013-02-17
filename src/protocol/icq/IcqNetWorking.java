/*
 * IcqNetWorking.java
 *
 * Created on 23 ������ 2007 �., 18:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq;

import jimm.comm.Util;
import jimm.ui.timers.*;
import protocol.Protocol;
import protocol.icq.action.IcqAction;
import protocol.net.*;
import protocol.icq.packet.*;
import jimm.*;

/**
 *
 * @author vladimir
 */
public final class IcqNetWorking extends ClientConnection {

    private byte[] flapHeader = new byte[6];
    private int nextIcqSequence; // ICQ sequence number counter
    private TcpSocket socket;
    private Icq icq;
    private IcqNetState queue;
    private boolean icqConnected = false;
    // FLAP sequence number
    // Set starting point for seq numbers (not bigger then 0x8000)
    private int flapSEQ = Util.nextRandInt() % 0x8000;
    private byte[] pingPacket = null;
    // Counter variable
    private int counter = 0;

    // Channel constants
    private static final int CHANNEL_CONNECT = 0x01;
    private static final int CHANNEL_SNAC = 0x02;
    private static final int CHANNEL_ERROR = 0x03;
    private static final int CHANNEL_DISCONNECT = 0x04;
    private static final int CHANNEL_PING = 0x05;

    public IcqNetWorking() {
    }

    public int getNextCounter() {
        return ++counter;
    }

    public final void processIcqException(JimmException e) {
        icq.processException(e);
    }

    public void initNet(Icq icq) {
        this.icq = icq;
        queue = new IcqNetState();
        queue.login(this);
    }

    public boolean isIcqConnected() {
        return icqConnected;
    }

    public void setIcqConnected() {
        icqConnected = true;
    }

    public void requestAction(IcqAction act) {
        queue.requestAction(act);
    }

    public Icq getIcq() {
        return icq;
    }

    protected Protocol getProtocol() {
        return icq;
    }

    public final void connectTo(String server) throws JimmException {
        // Open connection
        if (null != socket) {
            socket.close();
        }
        if (!isConnected()) {
            return;
        }
        socket = new TcpSocket();
        socket.connectTo("socket://" + server);
    }

    // Returns and updates sequence nr
    private int getFlapSequence() {
        flapSEQ = (++flapSEQ) & 0x7FFF;
        return flapSEQ;
    }

    public void sendPacket(Packet packet) throws JimmException {
        if (packet instanceof ToIcqSrvPacket) {
            ((ToIcqSrvPacket) packet).setIcqSequence(nextIcqSequence++);
        }
        write(packet.toByteArray());
    }

    private void write(byte[] out) throws JimmException {
        Util.putWordBE(out, 2, getFlapSequence());
        socket.write(out);
        socket.flush();
    }

    private void readPacket(TcpSocket socket) throws JimmException {
        socket.readFully(flapHeader);

        // Verify flap header
        if (0x2A != flapHeader[0]) {
            throw new JimmException(124, 0);
        }

        byte[] flapData = new byte[Util.getWordBE(flapHeader, 4)];
        socket.readFully(flapData);

        Packet packet = parse(Util.getByte(flapHeader, 1), flapData);
        flapData = null;
        if (null != packet) {
            // Get FLAP sequence number
            int flapSequence = Util.getWordBE(flapHeader, 2);
            queue.processPacket(packet);
        }
    }

    protected void ping() throws JimmException {
        if (null != pingPacket) {
            write(pingPacket);
        }
    }

    public void initPing() {
        pingPacket = new byte[6];
        Util.putByte(pingPacket, 0, 0x2a);
        Util.putByte(pingPacket, 1, CHANNEL_PING);
        Util.putWordBE(pingPacket, 2, 0 /* stub */);
        Util.putWordBE(pingPacket, 4, 0);
    }

    private boolean isShadowNeeded() {
        return Jimm.isPhone(Jimm.PHONE_NOKIA_S40);
    }

    protected void connect() throws JimmException {
        connect = true;
        nextIcqSequence = 0;
        if (isShadowNeeded()) {
            new GetVersion(GetVersion.TYPE_SHADOW).get();
        }
        // login
        queue.processActions();
    }

    protected boolean processPacket() throws JimmException {
        boolean action = queue.processActions();
        if ((null != socket) && (0 < socket.available())) {
            readPacket(socket);
            return true;
        }
        return action;
    }

    protected void closeSocket() {
        if (null != socket) {
            socket.close();
        }
    }

    // Parses given byte array and returns a Packet object
    private Packet parse(int channel, byte[] flapData) throws JimmException {
        try {
            switch (channel) {
                case CHANNEL_SNAC:
                    int family = Util.getWordBE(flapData, 0);
                    int command = Util.getWordBE(flapData, 2);
                    if (SnacPacket.OLD_ICQ_FAMILY == family) {
                        return (SnacPacket.SRV_FROMICQSRV_COMMAND == command)
                                ? FromIcqSrvPacket.parse(flapData) : null;
                    }
                    return SnacPacket.parse(family, command, flapData);

                case CHANNEL_CONNECT:
                    return ConnectPacket.parse(flapData);
                case CHANNEL_DISCONNECT:
                    return DisconnectPacket.parse(flapData);
            }
        } catch (JimmException e) {
            throw e;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.dump("broken packet " + channel, flapData);
            // #sijapp cond.end #
        }
        return null;
    }

    public void disconnect() {
        icq = null;
        IcqNetState l = queue;
        queue = null;
        if (null != l) {
            l.disconnect();
        }
        connect = false;
    }
}
// #sijapp cond.end #
