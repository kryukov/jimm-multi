/*
 * GetVersion.java
 *
 * Created on 20 Июнь 2007 г., 23:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.timers;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.base.NativeCanvas;
import protocol.net.TcpSocket;

/**
 * Try to get current Jimm version from Jimm server
 *
 * @author vladimir
 */
public class GetVersion implements Runnable {
    private volatile boolean shadowConnectionActive = false;
    private volatile ContentConnection shadowConnection = null;

    public static final int TYPE_SHADOW  = 1;
    public static final int TYPE_AVATAR = 2;
    private static final int TYPE_URL = 3;

    private int type;
    private String url;
    // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
    private jimm.search.UserInfo userInfo;
    // #sijapp cond.end#


    private String getContent(String url) {
        HttpConnection httemp = null;
        InputStream istemp = null;
        String content = "";

        try {
            httemp = (HttpConnection) Connector.open(url);
            httemp.setRequestProperty("Connection", "cl" + "ose");
            if (HttpConnection.HTTP_OK != httemp.getResponseCode()) {
                throw new IOException();
            }

            istemp = httemp.openInputStream();
            int length = (int) httemp.getLength();
            if (-1 != length) {
                byte[] bytes = new byte[length];
                istemp.read(bytes);
                content = new String(bytes);

            } else {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while (true) {
                    int ch = istemp.read();
                    if (-1 == ch) break;
                    bytes.write(ch);
                }
                content = new String(bytes.toByteArray());
                bytes.close();
            }

        } catch (Exception e) {
            content = "Error: " + e.getMessage();
        }
        TcpSocket.close(httemp);
        TcpSocket.close(istemp);
        return StringConvertor.removeCr(content);
    }

    public static void updateProgram() {
        Jimm.platformRequestAndExit("jimm:update");
    }

    private static int[] getVersionDate(String str) {
        String[] svers = Util.explode(str, '.');
        int[] ivers = new int[3];
        for (int num = 0; num < ivers.length; ++num) {
            ivers[num] = Util.strToIntDef(num < svers.length ? svers[num] : "", 0);
        }
        return ivers;
    }

    private void shadowConnection() {
        // Make the shadow connection for Nokia 6230 or other devices
        // if needed
        if (shadowConnectionActive) {
            return;
        }
        ContentConnection ctemp = null;
        DataInputStream istemp = null;

        try {
            shadowConnectionActive = true;
            ctemp = (ContentConnection)Connector.open("http://http.proxy.icq.com/hello");
            istemp = ctemp.openDataInputStream();
        } catch (Exception ignored) {
        }
        try {
            if (null != shadowConnection) {
                shadowConnection.close();
            }
        } catch (Exception ignored) {
        }
        shadowConnection = ctemp;
        shadowConnectionActive = false;
    }

    // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
    private byte[] read(InputStream in, int length) throws IOException {
        if (0 == length) {
            return null;

        }
        if (0 < length) {
            byte[] bytes = new byte[length];
            int readCount = 0;
            while (readCount < bytes.length) {
                int c = in.read(bytes, readCount, bytes.length - readCount);
                if (-1 == c) break;
                readCount += c;
            }
            return bytes;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < 100*1024; ++i) {
            int ch = in.read();
            if (-1 == ch) break;
            bytes.write(ch);
        }
        byte[] content = bytes.toByteArray();
        bytes.close();
        return content;
    }
    private Image getAvatar() {
        HttpConnection httemp = null;
        InputStream istemp = null;
        Image avatar = null;
        try {
            httemp = (HttpConnection) Connector.open(url);
            if (HttpConnection.HTTP_OK != httemp.getResponseCode()) {
                throw new IOException();
            }
            istemp = httemp.openInputStream();
            byte[] avatarBytes = read(istemp, (int)httemp.getLength());
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.getInstance().addInTraffic(avatarBytes.length);
            // #sijapp cond.end#
            avatar = javax.microedition.lcdui.Image.createImage(avatarBytes, 0, avatarBytes.length);
            avatarBytes = null;
        } catch (Exception ignored) {
        }
        TcpSocket.close(httemp);
        TcpSocket.close(istemp);
        return avatar;
    }
    private String getAvatarType() {
        int h = NativeCanvas.getInstance().getMinScreenMetrics();
        if (180 < h) {
            return "/_avatar180";
        }
        if (90 < h) {
            return "/_mrimavatar";
        }
        return "/_mrimavatarsmall";
    }
    public GetVersion(jimm.search.UserInfo ui) {
        this.type = TYPE_AVATAR;
        userInfo = ui;
        String uin = userInfo.uin;
        if (uin.endsWith("uin.icq")) {
            uin = uin.substring(0, uin.indexOf('@'));
        }
        if (-1 != uin.indexOf('@')) {
            String domain = uin.substring(uin.indexOf("@") + 1);
            String secondaryDomain = domain.substring(0, domain.indexOf('.'));
            String emailName = uin.substring(0, uin.indexOf("@"));
            url = "http://avt.foto.mail.ru/" + secondaryDomain + "/" + emailName + getAvatarType();
        } else {
            url = "http://api.icq.net/expressions/get?f=native&type=buddyIcon&t=" + uin;
        }
    }
    // #sijapp cond.end#


    // Timer routine
    public void run() {
        try {
            exec();
        } catch (Exception ignored) {
        }
    }
    private void exec() {
        if (TYPE_SHADOW == type) {
            shadowConnection();
            return;
        }
        // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
        if (TYPE_AVATAR == type) {
            try {
                userInfo.setAvatar(getAvatar());
                if (null != userInfo.avatar) {
                    userInfo.updateProfileView();
                }
            } catch (OutOfMemoryError e) {
                userInfo.setAvatar(null);
            }
            return;
        }
        // #sijapp cond.end#
        if (TYPE_URL == type) {
            getContent(url);
            return;
        }
    }

    public GetVersion(int type) {
        this.type = type;
    }
    public GetVersion(String url) {
        this.type = TYPE_URL;
        this.url = url;
    }
    public void get() {
        try {
            new Thread(this).start();
            Thread.sleep(10);
        } catch (Exception ignored) {
        }
    }
}
