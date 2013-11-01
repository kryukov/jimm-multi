/*
 * StringConvertor.java
 *
 * Created on 6 Февраль 2007 г., 19:49
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.comm;

import java.io.*;
import java.util.Vector;

/**
 *
 * @author vladiеmir
 */
public final class StringUtils {
    private final String name;
    private final String[] from;
    private final String[] to;

    private static StringUtils[] converters = new StringUtils[0];

    public static final String DETRANSLITERATE = "detrans" + "literate";
    public static final String MRIM2JIMM = "mrim2jimm";
    public static final String JIMM2MRIM = "jimm2mrim";
    public static final String JABBER2JIMM = "jabber2jimm";
    public static final char DELIMITER = ' ';

    private static boolean systemWin1251 = true;

    static public String bytesToSizeString(int v, boolean force) {
        int size = v;
        if (v < 1024 || force) {
            return size + " B";
        }
        size = (v + 512) / 1024;
        v /= 1024;
        if (v < 1024 * 10) {
            return size + " KiB";
        }
        size = (v + 512) / 1024;
        v /= 1024;
        return size + " MiB";
    }

    // Converts a byte array to a hex string
    public static String byteArrayToHexString(byte[] buf) {
        StringBuilder hexString = new StringBuilder(buf.length * 2);
        for (int i = 0; i < buf.length; ++i) {
            String hex = Integer.toHexString(buf[i] & 0x00FF);
            if (hex.length() < 2) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Check is data array ucs2 string
    public static boolean isDataUCS2(byte[] array, int start, int lenght) {
        if ((lenght & 1) != 0) {
            return false;
        }
        int end = start + lenght;
        byte b;
        boolean result = true;
        for (int i = start; i < end; i += 2) {
            b = array[i];
            // b == 0x04 - russian
            if (0 < b && b < 0x09) {
                return true;
            }
            // b == 0x00 - latin
            if (0x00 == b && array[i + 1] != 0) {
                return true;
            }
            if (0x20 == b && array[i + 1] >= 0x20) {
                result = false;
            }
            if (0x20 < b || b < 0x00) {
                result = false;
            }
        }
        return result;
    }

    // Check is data array utf-8 string
    public static boolean isDataUTF8(byte[] array, int start, int length) {
        if (0 == length) {
            return false;
        }

        for (int i = start, len = length; len > 0;) {
            byte bt = array[i++];
            len--;

            int seqLen = 0;
            if      ((bt & 0xE0) == 0xC0) seqLen = 1;
            else if ((bt & 0xF0) == 0xE0) seqLen = 2;
            else if ((bt & 0xF8) == 0xF0) seqLen = 3;
            else if ((bt & 0xFC) == 0xF8) seqLen = 4;
            else if ((bt & 0xFE) == 0xFC) seqLen = 5;

            if (0 == seqLen) {
                if ((bt & 0x80) == 0x80) {
                    return false;
                }
                continue;
            }

            for (int j = 0; j < seqLen; ++j) {
                if (len == 0) return false;
                bt = array[i++];
                if ((bt & 0xC0) != 0x80) return false;
                len--;
            }
        }
        return true;
    }

    // Converts an Unicode string into CP1251 byte array
    public static byte[] stringToByteArray1251(String s) {
        if (null == s) {
            return new byte[0];
        }
        if (systemWin1251) {
            try {
                return s.getBytes("Windows-1251");
            } catch (Exception e) {
                systemWin1251 = false;
            }
        }
        byte buf[] = new byte[s.length()];
        int size = s.length();
        for(int i = 0; i < size; ++i) {
            char ch = s.charAt(i);
            switch (ch) {
                case 1025:
                    buf[i] = -88;
                    break;
                case 1105:
                    buf[i] = -72;
                    break;

                    /* Ukrainian CP1251 chars section */
                case 1168:
                    buf[i] = -91;
                    break;
                case 1028:
                    buf[i] = -86;
                    break;
                case 1031:
                    buf[i] = -81;
                    break;
                case 1030:
                    buf[i] = -78;
                    break;
                case 1110:
                    buf[i] = -77;
                    break;
                case 1169:
                    buf[i] = -76;
                    break;
                case 1108:
                    buf[i] = -70;
                    break;
                case 1111:
                    buf[i] = -65;
                    break;
                    /* end of section */

                default:
                    if (ch >= '\u0410' && ch <= '\u044F') {
                        buf[i] = (byte) ((ch - 1040) + 192);
                    } else {
                        buf[i] = (byte)((int)ch & 0xFF);
                    }
                    break;
            }
        }
        return buf;
    }

    // Converts an CP1251 byte array into an Unicode string
    public static String byteArray1251ToString(byte buf[], int pos, int len) {
        if (systemWin1251) {
            try {
                return new String(buf, pos, len, "Windows-1251");
            } catch (Exception e) {
                systemWin1251 = false;
            }
        }
        int end = pos + len;
        StringBuilder StringBuilder = new StringBuilder(len);
        for(int i = pos; i < end; ++i) {
            int ch = buf[i] & 0xff;
            switch (ch) {
                case 168:
                    StringBuilder.append('\u0401');
                    break;
                case 184:
                    StringBuilder.append('\u0451');
                    break;

                    /* Ukrainian CP1251 chars section */
                case 165:
                    StringBuilder.append('\u0490');
                    break;
                case 170:
                    StringBuilder.append('\u0404');
                    break;
                case 175:
                    StringBuilder.append('\u0407');
                    break;
                case 178:
                    StringBuilder.append('\u0406');
                    break;
                case 179:
                    StringBuilder.append('\u0456');
                    break;
                case 180:
                    StringBuilder.append('\u0491');
                    break;
                case 186:
                    StringBuilder.append('\u0454');
                    break;
                case 191:
                    StringBuilder.append('\u0457');
                    break;
                    /* end of section */

                default:
                    try {
                        if (ch >= 192 && ch <= 255) {
                            StringBuilder.append((char) ((1040 + ch) - 192));
                        } else {
                            StringBuilder.append((char)ch);
                        }
                    } catch (Exception e) {
                    }
                    break;
            }
        }
        return removeCr(StringBuilder.toString());
    }

    public static String utf8beByteArrayToString(byte[] buf, int off, int len) {
        try {
            // Remove \0's at the end
            if ((0 < len) && (0x00 == buf[off + len - 1])) {
                len--;
            }
            if (0 == len) {
                return "";
            }
            byte[] buf2 = new byte[len + 2];
            Util.putWordBE(buf2, 0, len);
            System.arraycopy(buf, off, buf2, 2, len);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf2);
            DataInputStream dis = new DataInputStream(bais);
            return removeCr(dis.readUTF());
        } catch (Exception e) {
            // do nothing
        }
        return "";
    }
    /**
     * Converts the specified string to UCS-2BE
     */
    public static byte[] stringToUcs2beByteArray(String val) {
        byte[] ucs2be = new byte[val.length() * 2];
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            Util.putWordBE(ucs2be, i * 2, (int) val.charAt(i));
        }
        return ucs2be;
    }


    /**
     * Extract a UCS-2BE string from the specified buffer (buf)
     * starting at position off, ending at position off+len
     */
    public static String ucs2beByteArrayToString(byte[] buf, int off, int len) {

        // Length check
        if ((off + len > buf.length) || (len % 2 != 0)) {
            return "";
        }

        // Convert
        StringBuilder sb = new StringBuilder();
        int end = off + len;
        for (int i = off; i < end; i += 2) {
            sb.append((char)Util.getWordBE(buf, i));
        }
        return removeCr(sb.toString());

    }

    // Removes all CR occurences
    public static String removeCr(String val) {
        if (val.indexOf('\r') < 0) {
            return val;
        }
        if (-1 == val.indexOf('\n')) {
            return val.replace('\r', '\n');
        }

        StringBuilder result = new StringBuilder();
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            char chr = val.charAt(i);
            if ((chr == 0) || (chr == '\r')) continue;
            result.append(chr);
        }
        return result.toString();
    }

    // Restores CRLF sequense from LF
    public static String restoreCrLf(String val) {
        StringBuilder result = new StringBuilder();
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            char chr = val.charAt(i);
            if (chr == '\r') continue;
            if (chr == '\n') {
                result.append("\r\n");
            } else {
                result.append(chr);
            }
        }
        return result.toString();
    }

    // Converts the specified string (val) to a byte array
    public static byte[] stringToByteArrayUtf8(String val) {
        // Write string in UTF-8 format
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            char ch;
            for (int i = 0; i < val.length(); ++i) {
                ch = val.charAt(i);
                if ((ch & 0x7F) == ch) {
                    if ((0x20 <= ch) || ('\r' == ch) || ('\n' == ch) || ('\t' == ch)) {
                        baos.write(ch);
                    }

                } else if ((ch & 0x7FF) == ch) {
                    baos.write(0xC0 | ((ch >>> 6) & 0x1F));
                    baos.write(0x80 | ((ch) & 0x3F));

                } else if ((ch & 0xFFFF) == ch) {
                    baos.write(0xE0 | ((ch >>> 12) & 0x0F));
                    baos.write(0x80 | ((ch >>> 6) & 0x3F));
                    baos.write(0x80 | ((ch) & 0x3F));
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            // Do nothing
        }
        return null;
    }

    // Converts the specified string (val) to a byte array
    public static byte[] stringToByteArray(String val) {
        return isEmpty(val) ? new byte[0] : val.getBytes();
    }

    /**
     * Extracts a string from the buffer (buf) starting at position off,
     * ending at position off+len
     */
    public static String byteArrayToString(byte[] buf, int off, int len) {

        // Length check
        if (buf.length < off + len) {
            return "";
        }
        // Remove \0's at the end
        while ((0 < len) && (buf[off + len - 1] == 0x00)) {
            len--;
        }
        if (0 == len) {
            return "";
        }

        // Scaning chars for detecting encoding.
        if (isDataUCS2(buf, off, len)) {
            return ucs2beByteArrayToString(buf, off, len);
        }

        // Read string in UTF-8 format
        if (isDataUTF8(buf, off, len)) {
            return utf8beByteArrayToString(buf, off, len);
        }

        return byteArrayToWinString(buf, off, len);
    }


    public static String byteArrayToWinString(byte[] buf, int off, int len) {
        // Length check
        if (buf.length < off + len) {
            return "";
        }
        // Remove \0's at the end
        if ((0 < len) && (0x00 == buf[off + len - 1])) {
            len--;
        }
        if (0 == len) {
            return "";
        }
        return byteArray1251ToString(buf, off, len);
    }
    public static String byteArrayToAsciiString(byte[] buf) {
        return new String(buf, 0, buf.length);
    }
    public static String byteArrayToAsciiString(byte[] buf, int offset, int length) {
        return new String(buf, offset, length);
    }

    public static boolean stringEquals(String s1, String s2) {
        if (s1.length() != s2.length()) {
            return false;
        }
        int size = s1.length();
        for (int i = 0; i < size; ++i) {
            if (toLowerCase(s1.charAt(i)) != toLowerCase(s2.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int stringCompare(String s1, String s2) {
        int size = Math.min(s1.length(), s2.length());
        int result = 0;
        for (int i = 0; i < size; ++i) {
            result = toLowerCase(s1.charAt(i)) - toLowerCase(s2.charAt(i));
            if (result != 0) {
                return result;
            }
        }
        return s1.length() - s2.length();
    }

    public static String toLowerCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toLowerCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }

    public static String toUpperCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toUpperCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }

    public static char toLowerCase(char c) {
        if (c >= 'A' && c <= 'Z' || c >= '\300'
                && c <= '\326' || c >= '\330'
                && c <= '\336' || c >= '\u0400'
                && c <= '\u042F') {
            if (c <= 'Z' || c >= '\u0410' && c <= '\u042F') {
                return (char)(c + 32);
            }
            if(c < '\u0410') {
                return (char)(c + 80);
            }
            return (char)(c + 32);
        }
        return Character.toLowerCase(c);
    }

    public static char toUpperCase(char c) {
        if (c >= 'a' && c <= 'z' || c >= '\337'
                && c <= '\366' || c >= '\370'
                && c <= '\377' || c >= '\u0430'
                && c <= '\u045F') {
            if (c <= 'z' || c >= '\u0430' && c <= '\u044F') {
                return (char)(c - 32);
            }
            if (c > '\u042F') {
                return (char)(c - 80);
            }
            return (char)(c - 32);
        }
        return Character.toUpperCase(c);
    }
    private boolean equalsSubstring(String s1, int pos, String s2) {
        if (s1.length() - pos < s2.length()) {
            return false;
        }
        int length = s2.length();
        for (int i = 0; i < length; ++i) {
            if (toLowerCase(s1.charAt(i + pos)) != toLowerCase(s2.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    private int convertCharIndex(String str, int pos) {
        for (int i = 0; i < from.length; ++i) {
            if (equalsSubstring(str, pos, from[i])) {
                return i;
            }
        }
        return -1;
    }
    private String convertChar(String str, int pos, int wordIndex) {

        char ch = str.charAt(pos);
        if (toLowerCase(ch) == ch) {
            return to[wordIndex];
        }
        if ((1 != from[wordIndex].length()) && (1 != to[wordIndex].length())) {
            ch = str.charAt(pos + 1);
            if (toLowerCase(ch) == ch) {
                return toUpperCase(to[wordIndex].substring(0, 1))
                        + to[wordIndex].substring(1);
            }
        }
        return toUpperCase(to[wordIndex]);
    }

    private String convertTextCaseInsensitive(String str) {
        StringBuilder buf = new StringBuilder();
        int pos = 0;
        int skipLength = -1;
        if (str.startsWith("/")) {
            skipLength = str.indexOf(' ');
            if (-1 == skipLength) {
                return str;
            }
        }
        if (-1 == skipLength) {
            for (int i = 0; i < Math.min(str.length(), 32); ++i) {
                char ch = str.charAt(i);
                if (',' == ch) {
                    skipLength = i;
                    break;
                }
                if (' ' == ch) {
                    break;
                }
            }
        }
        if (-1 < skipLength) {
            while (pos <= skipLength) {
                buf.append(str.charAt(pos));
                pos++;
            }
        }
        boolean convert = true;
        while (pos < str.length()) {
            if (convert) {
                int wordIndex = convertCharIndex(str, pos);
                if (-1 != wordIndex) {
                    buf.append(convertChar(str, pos, wordIndex));
                    pos += from[wordIndex].length();
                    continue;
                }
            }
            if (str.startsWith("  ", pos)) {
                convert = !convert;
                buf.append(' ');
                pos += 2;
                continue;
            }
            buf.append(str.charAt(pos));
            pos++;
        }
        return buf.toString();
    }

    private String convertText(String text) {
        String result = text;
        for (int i = 0; i < from.length; ++i) {
            result = Util.replace(result, from[i], to[i]);
        }
        return result;
    }


    private StringUtils(String name, String[] from, String[] to) {
        this.name = name;

        this.from = from;
        this.to = to;
        for (int i = 0; i < from.length; ++i) {
            if (from[i].equals(to[i])) {
                from[i] = to[i];
            }
        }
    }

    public static void load() {
        Vector<Config> configs = new Vector<Config>();
        String content = Config.loadResource("/replaces.txt");
        Config.parseIniConfig(content, configs);
        // #sijapp cond.if protocols_MRIM is "true" or protocols_JABBER is "true" #
        String mrimContent = Config.loadResource("/mrim-replaces.txt");
        Config.parseIniConfig(mrimContent, configs);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        String jabberContent = Config.loadResource("/jabber-replaces.txt");
        Config.parseIniConfig(jabberContent, configs);
        // #sijapp cond.end #
        StringUtils.converters = new StringUtils[configs.size()];
        for (int i = 0; i < configs.size(); ++i) {
            Config config = (Config) configs.elementAt(i);
            StringUtils.converters[i] = new StringUtils(config.getName(),
                    config.getKeys(), config.getValues());
        }
    }

    private static StringUtils getConverter(String scheme) {
        for (int i = 0; i < converters.length; ++i) {
            if (scheme.equals(converters[i].name)) {
                return converters[i];
            }
        }
        return null;
    }
    public static String convert(String scheme, String str) {
        StringUtils convertor = getConverter(scheme);
        return (null == convertor) ? str : convertor.convertText(str);
    }

    public static String detransliterate(String str) {
        StringUtils convertor = getConverter(DETRANSLITERATE);
        return (null == convertor) ? str : convertor.convertTextCaseInsensitive(str);
    }

    public static boolean isEmpty(String value) {
        return (null == value) || (0 == value.length());
    }
    public static String notNull(String value) {
        return (null == value) ? "" : value;
    }
    public static String trim(String msg) {
        if (StringUtils.isEmpty(msg)) {
            return "";
        }
        if (-1 == "\n ".indexOf(msg.charAt(msg.length() - 1))) {
            return msg;
        }

        for (int i = msg.length() - 1; 0 <= i; --i) {
            char ch = msg.charAt(i);
            if (('\n' != ch) && (' ' != ch)) {
                return msg.substring(0, i + 1);
            }
        }
        return "";
    }
    public static boolean contains(String[] base, String value) {
        for (int i = 0; i < base.length; ++i) {
            if (base[i].equals(value)) return true;
        }
        return false;
    }


    private static int cutIndex(String str, char ch, int cutIndex, int length) {
        int chIndex = str.lastIndexOf(ch, length);
        if (-1 != chIndex) {
            return Math.max(cutIndex, chIndex);
        }
        return cutIndex;
    }
    public static String cut(String str, int length) {
        if (str.length() < length) {
            return str;
        }
        int cutIndex = 0;
        final char[] cutChars = {' ', '\n'};
        for (int i = 0; i < cutChars.length; ++i) {
            cutIndex = cutIndex(str, cutChars[i], cutIndex, length);
        }
        if (0 == cutIndex) {
            cutIndex = length;
        }
        return str.substring(0, cutIndex) + "...";
    }
}