package jimm.comm;

import jimm.util.JLocale;
import protocol.net.TcpSocket;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 04.11.13 18:05
 *
 * @author vladimir
 */
public class Tokenizer {
    private String stream;
    private int index;
    private boolean isNotEof = false;
    public Tokenizer(String path, boolean locale) {
        try {
            stream = locale ? loadLocateResource(path) : loadResource(path);
            index = 0;
        } catch (Exception ignored) {
        }
    }
    public char nextChat() {
        if (index < stream.length()) {
            return stream.charAt(index++);
        } else {
            isNotEof = true;
            return '\n';
        }
    }

    public boolean isNotEof() {
        return isNotEof;
    }

    public static String loadResource(String path) {
        String res = "";
        InputStream stream = null;
        try {
            stream = jimm.Jimm.getResourceAsStream(path);
            byte[] str = new byte[stream.available()];
            TcpSocket.readFully(stream, str, 0, str.length);
            res = StringUtils.utf8beByteArrayToString(str, 0, str.length);
        } catch (Exception ignored) {
        }
        TcpSocket.close(stream);
        return res;
    }

    private String loadLocateResource(String path) {
        String lang = JLocale.getCurrUiLanguage();
        int index = path.lastIndexOf('.');
        if (-1 == index) {
            index = path.length();
        }
        String localPath = path.substring(0, index) + "." + lang + path.substring(index);
        String config = loadResource(localPath);
        if (0 == config.length()) {
            return loadResource(path);
        }
        return config;
    }
}
