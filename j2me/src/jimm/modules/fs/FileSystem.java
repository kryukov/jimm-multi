// #sijapp cond.if modules_FILES="true"#
package jimm.modules.fs;

import java.util.*;
import java.io.*;
import jimm.*;
import protocol.net.TcpSocket;

public abstract class FileSystem {

    public static final String ROOT_DIRECTORY = "/";
    public static final String PARENT_DIRECTORY = "../";
    static private final boolean supports_JSR75 = supportJSR75();
    public static final String HISTORY = "h" + "istory";
    public static final String RES = "res";

    static private boolean supportJSR75() {
        try {
            return Class.forName("javax.microedition.io.file.FileConnection") != null;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    public static boolean isSupported() {
        return supports_JSR75;
    }

    public static JSR75FileSystem getInstance() {
        return new JSR75FileSystem();
    }

    public static String getJimmHome() {
        return "/e:/jimm-multi/";
    }

    public static InputStream openJimmFile(String file) {
        JSR75FileSystem fs = FileSystem.getInstance();
        byte[] buffer = null;
        try {
            fs.openFile(getJimmHome() + RES + "/" + file);
            InputStream in = fs.openInputStream();
            buffer = new byte[in.available()];
            in.read(buffer);
            TcpSocket.close(in);
        } catch (Exception e) {
            buffer = null;
        }

        fs.close();
        if (null != buffer) {
            return new ByteArrayInputStream(buffer);
        }
        return null;
    }

}
// #sijapp cond.end#
