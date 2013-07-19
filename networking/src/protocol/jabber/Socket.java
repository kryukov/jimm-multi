/*
 * Socket.java
 *
 * Created on 4 Февраль 2009 г., 15:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.jabber;

import com.jcraft.jzlib.*;
import jimm.JimmException;
import jimm.modules.*;
import protocol.net.TcpSocket;

/**
 *
 * @author Vladimir Krukov
 */
final class Socket {
    private TcpSocket socket = new TcpSocket();
    private boolean connected;
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    // #sijapp cond.if modules_ZLIB is "true" #
    private ZInputStream zin;
    private ZOutputStream zout;
    private boolean compressed;
    // #sijapp cond.end #
    
    /**
     * Creates a new instance of Socket
     */
    public Socket() {
    }
    // #sijapp cond.if modules_ZLIB is "true" #
    public void activateStreamCompression() {
        zin = new ZInputStream(socket);
        zout = new ZOutputStream(socket, JZlib.Z_DEFAULT_COMPRESSION);
        zout.setFlushMode(JZlib.Z_SYNC_FLUSH);
        compressed = true;
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.println("zlib is working");
        // #sijapp cond.end #
    }
    // #sijapp cond.end #

    public boolean isConnected() {
        return connected;
    }
    
    public void connectTo(String url) throws JimmException {
        System.out.println("url: " + url);
        socket.connectTo(url);
        connected = true;
    }

    private int read(byte[] data) throws JimmException {
        // #sijapp cond.if modules_ZLIB is "true" #
        if (compressed) {
            int bRead = zin.read(data);
            if (-1 == bRead) {
                throw new JimmException(120, 13);
            }
            return bRead;
        }
        // #sijapp cond.end #
        int length = Math.min(data.length, socket.available());
        if (0 == length) {
            return 0;
        }
        int bRead = socket.read(data, 0, length);
        if (-1 == bRead) {
            throw new JimmException(120, 12);
        }
        return bRead;
    }

    public void write(byte[] data) throws JimmException {
        // #sijapp cond.if modules_ZLIB is "true" #
        if (compressed) {
            zout.write(data);
            zout.flush();
            return;
        }
        // #sijapp cond.end #
        socket.write(data, 0, data.length);
        socket.flush();
    }
    public void close() {
        connected = false;
        // #sijapp cond.if modules_ZLIB is "true" #
        try {
            zin.close();
            zout.close();
        } catch (Exception ex) {
        }
        // #sijapp cond.end #
        socket.close();
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    private byte readByte() throws JimmException {
        if (inputBufferIndex >= inputBufferLength) {
            inputBufferIndex = 0;
            inputBufferLength = read(inputBuffer);
            while (0 == inputBufferLength) {
                sleep(100);
                inputBufferLength = read(inputBuffer);
            }
        }
        return inputBuffer[inputBufferIndex++];
    }
    public int available() throws JimmException {
        if (inputBufferIndex < inputBufferLength) {
            return (inputBufferLength - inputBufferIndex);
        }
        return socket.available();
    }


    char readChar() throws JimmException {
        try {
            byte bt = readByte();
            if (0 <= bt) {
                return (char)bt;
            }
            if ((bt & 0xE0) == 0xC0) {
                byte bt2 = readByte();
                return (char)(((bt & 0x3F) << 6) | (bt2 & 0x3F));

            } else if ((bt & 0xF0) == 0xE0) {
                byte bt2 = readByte();
                byte bt3 = readByte();
                return (char)(((bt & 0x1F) << 12) | ((bt2 & 0x3F) << 6) | (bt3 & 0x3F));

            } else {
                int seqLen = 0;
                if ((bt & 0xF8) == 0xF0) seqLen = 3;
                else if ((bt & 0xFC) == 0xF8) seqLen = 4;
                else if ((bt & 0xFE) == 0xFC) seqLen = 5;
                for (; 0 < seqLen; --seqLen) {
                    bt = readByte();
                }
                return '?';
            }
        } catch (JimmException e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("readChar je ", e);
            // #sijapp cond.end #
            throw e;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("readChar e ", e);
            // #sijapp cond.end #
            throw new JimmException(120, 7);
        }
    }
}
