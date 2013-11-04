/*
 * TcpSocket.java
 *
 * Created on 17 Февраль 2011 г., 22:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.net;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.Connection;

import jimm.JimmException;
import jimm.modules.*;

/**
 *
 * @author Vladimir Kryukov
 */
public final class TcpSocket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;

    public TcpSocket() {
    }

    public void connectTo(String host, int port) throws JimmException {
        connectTo("socket://" + host + ":" + port);
    }
    public void connectTo(String url) throws JimmException {
        try {
            sc = (StreamConnection)Connector.open(url, Connector.READ_WRITE);
            SocketConnection socket = (SocketConnection)sc;
            socket.setSocketOption(SocketConnection.DELAY, 0);
            //socket.setSocketOption(SocketConnection.KEEPALIVE, 2*60);
            //socket.setSocketOption(SocketConnection.LINGER, 0);
            //socket.setSocketOption(SocketConnection.RCVBUF, 10*1024);
            //socket.setSocketOption(SocketConnection.SNDBUF, 10*1024);
            os = sc.openOutputStream();
            is = sc.openInputStream();
        } catch (ConnectionNotFoundException e) {
            throw new JimmException(121, 0);
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        } catch (SecurityException e) {
            throw new JimmException(123, 9);
        } catch (IOException e) {
            throw new JimmException(120, 0);
        } catch (Exception e) {
            throw new JimmException(120, 10);
        }
    }
    public void connectForReadingTo(String url) throws JimmException {
        try {
            sc = (StreamConnection)Connector.open(url, Connector.READ);
            //SocketConnection socket = (SocketConnection)sc;
            //socket.setSocketOption(SocketConnection.DELAY, 0);
            //socket.setSocketOption(SocketConnection.KEEPALIVE, 2*60);
            //socket.setSocketOption(SocketConnection.LINGER, 0);
            //socket.setSocketOption(SocketConnection.RCVBUF, 10*1024);
            //socket.setSocketOption(SocketConnection.SNDBUF, 10*1024);
            is = sc.openInputStream();
        } catch (ConnectionNotFoundException e) {
            throw new JimmException(121, 0);
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        } catch (SecurityException e) {
            throw new JimmException(123, 9);
        } catch (IOException e) {
            throw new JimmException(120, 0);
        } catch (Exception e) {
            throw new JimmException(120, 10);
        }
    }

    public final int read() throws JimmException {
        try {
            return is.read();
        } catch (Exception e) {
            throw new JimmException(120, 4);
        }
    }
    public int read(byte[] data, int offset, int length) throws JimmException {
        try {
            // #sijapp cond.if modules_ANDROID isnot "true" #
            length = Math.min(length, is.available());
            // #sijapp cond.end #
            if (0 == length) {
                return 0;
            }
            int bRead = is.read(data, offset, length);
            if (-1 == bRead) {
                throw new IOException("EOF");
            }
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.getInstance().addInTraffic(bRead * 3 / 2);
            // #sijapp cond.end#
            return bRead;

        } catch (IOException e) {
            throw new JimmException(120, 1);
        }
    }

    public final int readFully(byte[] data) throws JimmException {
        return readFully(data, 0, data.length);
    }
    public final int readFully(byte[] data, final int offset, final int length) throws JimmException {
        if ((null == data) || (0 == data.length)) {
            return 0;
        }
        try {
            int bReadSum = 0;
            do {
                int bRead = is.read(data, offset + bReadSum, length - bReadSum);
                if (-1 == bRead) {
                    throw new IOException("EOF");
                } else if (0 == bRead) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                }
                bReadSum += bRead;
            } while (bReadSum < length);
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.getInstance().addInTraffic(bReadSum * 3 / 2);
            // #sijapp cond.end#
            return bReadSum;
        } catch (IOException e) {
            throw new JimmException(120, 1);
        }
    }
    public final void write(byte[] data) throws JimmException {
        write(data, 0, data.length);
    }
    public void write(byte[] data, int offset, int length) throws JimmException {
        try {
            os.write(data, offset, length);
        } catch (IOException e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("write", e);
            // #sijapp cond.end#
            throw new JimmException(120, 2);
        }
        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().addOutTraffic(length * 3 / 2);
        // #sijapp cond.end#
    }
    public void flush() throws JimmException {
        try {
            os.flush();
        } catch (IOException e) {
            throw new JimmException(120, 2);
        }
    }

    public int available() throws JimmException {
        try {
            return is.available();
        } catch (IOException ex) {
            throw new JimmException(120, 3);
        }
    }

    public void startTls(String host) {
        // #sijapp cond.if modules_ANDROID is "true" #
        try {
            jimm.modules.DebugLog.println("startTls start " + sc + os + is);
            ((org.microemu.cldc.socket.SocketConnection)sc).startTls(host);
            is = sc.openInputStream();
            os = sc.openOutputStream();
            jimm.modules.DebugLog.println("startTls done " + sc + os + is);
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("startTls error", e);
        }
        // #sijapp cond.end #
    }

    public void close() {
        close(is);
        close(os);
        close(sc);
    }
    public static void close(Connection c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
    public static void close(InputStream c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
    public static void close(OutputStream c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    public static int readFully(InputStream in, byte[] data, final int offset, final int length) throws IOException {
        if ((null == data) || (0 == data.length)) {
            return 0;
        }
        int bReadSum = 0;
        do {
            int bRead = in.read(data, offset + bReadSum, length - bReadSum);
            if (-1 == bRead) {
                throw new IOException("EOF");
            } else if (0 == bRead) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }
            bReadSum += bRead;
        } while (bReadSum < length);
        return bReadSum;
    }
}
