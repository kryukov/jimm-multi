/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/comm/Util.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Sergey Chernov, Andrey B. Ivlev
            Artyomov Denis, Igor Palkin, Vladimir Kryukov
 *******************************************************************************/


package jimm.comm;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;

import jimm.*;
import jimm.util.JLocale;


public class Util {
    private ByteArrayOutputStream stream = new ByteArrayOutputStream();
    public Util() {
    }
    public byte[] toByteArray() {
        return stream.toByteArray();
    }
    public int size() {
        return stream.size();
    }
    public void reset() {
        try {
    	    stream.reset();
        } catch (Exception ignored) {
        }
    }


    public void writeZeroes(int count) {
        for (int i = 0; i < count; ++i) {
            writeByte(0);
        }
    }
    public void writeWordBE(int value) {
        try {
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
            stream.write(value & 0xFF);
        } catch (Exception ignored) {
        }
    }
    public void writeWordLE(int value) {
        try {
            stream.write(value & 0xFF);
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array) {
        try {
            stream.write(array);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array, int offset, int length) {
        try {
            stream.write(array, offset, length);
        } catch (Exception ignored) {
        }
    }

    public void writeDWordBE(long longValue) {
        try {
            int value = (int)longValue;
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0x0000FF00) >> 8)  & 0xFF);
            stream.write(  value & 0x000000FF);
        } catch (Exception ignored) {
        }
    }
    public void writeDWordLE(long longValue) {
        try {
            int value = (int)longValue;
            stream.write(  value & 0x000000FF);
            stream.write(((value & 0x0000FF00) >> 8)  & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByte(int value) {
        try {
            stream.write(value);
        } catch (Exception ignored) {
        }
    }

    public void writeShortLenAndUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        writeByte(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }
    public void writeLenAndUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        writeWordBE(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }
    public void writeUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }
    public void writeProfileAsciizTLV(int type, String value) {
        value = StringConvertor.notNull(value);

        byte[] raw = StringConvertor.stringToByteArray1251(value);
        writeWordLE(type);
        writeWordLE(raw.length + 3);
        writeWordLE(raw.length + 1);
        writeByteArray(raw);
        writeByte(0);
    }
    public void writeTlvECombo(int type, String value, int code) {
        value = StringConvertor.notNull(value);
        writeWordLE(type);
        byte[] raw = StringConvertor.stringToByteArray(value);
        writeWordLE(raw.length + 4);
        writeWordLE(raw.length + 1);
        try {
            stream.write(raw, 0, raw.length);
            stream.write(0);
            stream.write(code);
        } catch (Exception ignored) {
        }
    }
    public void writeTLV(int type, byte[] data) {
        writeWordBE(type);
        int length = (null == data) ? 0 : data.length;
        writeWordBE(length);
        if (length > 0) {
            try {
                stream.write(data, 0, data.length);
            } catch (Exception ignored) {
            }
        }
    }
    public void writeTLVWord(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(2);
        writeWordBE(wordValue);
    }
    public void writeTLVDWord(int type, long wordValue) {
        writeWordBE(type);
        writeWordBE(4);
        writeDWordBE(wordValue);
    }
    public void writeTLVByte(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(1);
        writeByte(wordValue);
    }

    // Password encryption key
    private static final byte[] PASSENC_KEY = {(byte)0xF3, (byte)0x26, (byte)0x81, (byte)0xC4,
                                              (byte)0x39, (byte)0x86, (byte)0xDB, (byte)0x92,
                                              (byte)0x71, (byte)0xA3, (byte)0xB9, (byte)0xE6,
                                              (byte)0x53, (byte)0x7A, (byte)0x95, (byte)0x7C};

    // Extracts the byte from the buffer (buf) at position off
    public static int getByte(byte[] buf, int off) {
        return ((int) buf[off]) & 0x000000FF;
    }


    // Puts the specified byte (val) into the buffer (buf) at position off
    public static void putByte(byte[] buf, int off, int val) {
        buf[off] = (byte) (val & 0x000000FF);
    }


    // Extracts the word from the buffer (buf) at position off using the specified byte ordering (bigEndian)
    /** @deprecated */
    public static int getWordLE(byte[] buf, int off) {
        int val = (((int) buf[off])) & 0x000000FF;
        return val | (((int) buf[++off]) << 8) & 0x0000FF00;
    }
    public static int getWordBE(byte[] buf, int off) {
        int val = (((int) buf[off]) << 8) & 0x0000FF00;
        return val | (((int) buf[++off])) & 0x000000FF;
    }


    /** @deprecated */
    public static void putWordLE(byte[] buf, int off, int val) {
        buf[off]   = (byte) ((val)      & 0x000000FF);
        buf[++off] = (byte) ((val >> 8) & 0x000000FF);
    }


    // Puts the specified word (val) into the buffer (buf) at position off using big endian byte ordering
    public static void putWordBE(byte[] buf, int off, int val) {
        buf[off]   = (byte) ((val >> 8) & 0x000000FF);
        buf[++off] = (byte) ((val)      & 0x000000FF);
    }

    public static long getDWordLE(byte[] buf, int off) {
        long val;
        // Little endian
        val  = (((long) buf[off]))         & 0x000000FF;
        val |= (((long) buf[++off]) << 8)  & 0x0000FF00;
        val |= (((long) buf[++off]) << 16) & 0x00FF0000;
        val |= (((long) buf[++off]) << 24) & 0xFF000000;
        return val;
    }
    // Extracts the double from the buffer (buf) at position off using big endian byte ordering
    public static long getDWordBE(byte[] buf, int off) {
        long val;
        val  = (((long) buf[off]) << 24)   & 0xFF000000;
        val |= (((long) buf[++off]) << 16) & 0x00FF0000;
        val |= (((long) buf[++off]) << 8)  & 0x0000FF00;
        val |= (((long) buf[++off]))       & 0x000000FF;
        return val;
    }



    // Puts the specified double (val) into the buffer (buf) at position off using the specified byte ordering (bigEndian)
    public static void putDWordLE(byte[] buf, int off, long val) {
        // Little endian
        buf[off]   = (byte) ((val)       & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8)  & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 24) & 0x00000000000000FF);
    }
    // Puts the specified double (val) into the buffer (buf) at position off using big endian byte ordering
    public static void putDWordBE(byte[] buf, int off, long val) {
        buf[off]   = (byte) ((val >> 24) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8)  & 0x00000000000000FF);
        buf[++off] = (byte) ((val)       & 0x00000000000000FF);
    }

    // DeScramble password
    public static byte[] decipherPassword(byte[] buf) {
        byte[] ret = new byte[buf.length];
        for (int i = 0; i < buf.length; ++i) {
            ret[i] = (byte) (buf[i] ^ PASSENC_KEY[i % PASSENC_KEY.length]);
        }
        return ret;
    }

    //  If the numer has only one digit add a 0
    public static String makeTwo(int number) {
        if (number < 10) {
            return "0" + String.valueOf(number);
        }
        return String.valueOf(number);
    }

    private static Random rand = new Random(System.currentTimeMillis());
    public static int nextRandInt() {
        return Math.abs(Math.max(Integer.MIN_VALUE + 1, rand.nextInt()));
    }

    // #sijapp cond.if modules_TRAFFIC is "true" #
    // Returns String value of cost value
    public static String intToDecimal(int value) {
        try {
            if (value != 0) {
                String costString = Integer.toString(value / 1000) + ".";
                String afterDot = Integer.toString(value % 1000);
                while (afterDot.length() != 3) {
                    afterDot = "0" + afterDot;
                }
                while ((afterDot.endsWith("0")) && (afterDot.length() > 2)) {
                    afterDot = afterDot.substring(0, afterDot.length() - 1);
                }
                return costString + afterDot;
            }
        } catch (Exception ignored) {
        }
        return "0.0";
    }

    // Extracts the number value form String
    public static int decimalToInt(String string) {
        try {
            int i = string.indexOf('.');
            if (i < 0) {
                return Integer.parseInt(string) * 1000;

            } else {
                int value = Integer.parseInt(string.substring(0, i)) * 1000;
                string = string.substring(i + 1, Math.min(string.length(), i + 1 + 3));
                while (string.length() < 3) {
                    string = string + "0";
                }
                return value + Integer.parseInt(string);
            }
        } catch (Exception ignored) {
            return 0;
        }
    }
    // #sijapp cond.end#

    /*/////////////////////////////////////////////////////////////////////////
    //                                                                       //
    //                 METHODS FOR DATE AND TIME PROCESSING                  //
    //                                                                       //
    /////////////////////////////////////////////////////////////////////////*/

    private final static int TIME_SECOND = 0;
    private final static int TIME_MINUTE = 1;
    private final static int TIME_HOUR   = 2;
    private final static int TIME_DAY    = 3;
    private final static int TIME_MON    = 4;
    private final static int TIME_YEAR   = 5;

    final private static byte[] dayCounts = {
        31,28,31,30,31,30,31,31,30,31,30,31
    };

    private final static int[] calFields = {
            Calendar.YEAR,         Calendar.MONTH,     Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY,  Calendar.MINUTE,    Calendar.SECOND};

    private final static int[] ofsFieldsA = { 0, 4, 6, 9, 12, 15 } ; //XEP-0091 - DEPRECATED
    private final static int[] ofsFieldsB = { 0, 5, 8, 11, 14, 17 } ;//XEP-0203
    private final static String[] months = {"Jan", "Feb", "Mar", "Apr",
                        "May", "Jun", "Jul", "Aug",
                        "Sep", "Oct", "Nov", "Dec"};
    public static long createGmtDate(String sdate) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try {
            sdate = sdate.trim();
            int[] ofs = sdate.endsWith("Z") ? ofsFieldsB : ofsFieldsA;
            long result;
            if (Character.isDigit(sdate.charAt(0))) {
                int fieldLength = 4;    // yearlen
                for (int i = 0; i < calFields.length; ++i) {
                    int begIndex = ofs[i];
                    int field = strToIntDef(sdate.substring(begIndex, begIndex + fieldLength), 0);
                    if (1 == i) {
                        field += Calendar.JANUARY - 1;
                    }
                    fieldLength = 2;
                    c.set(calFields[i], field);
                }
                result = Math.max(0, c.getTime().getTime() / 1000);

            } else {
                String[] rfcDate = Util.explode(sdate, ' ');
                c.set(Calendar.YEAR, strToIntDef(rfcDate[3], 0));

                for (int i = 0; i < months.length; ++i) {
                    if (months[i].equals(rfcDate[2])) {
                        c.set(Calendar.MONTH, i);
                        break;
                    }
                }
                c.set(Calendar.DAY_OF_MONTH, strToIntDef(rfcDate[1], 0));
                c.set(Calendar.HOUR_OF_DAY, strToIntDef(rfcDate[4].substring(0, 2), 0));
                c.set(Calendar.MINUTE,      strToIntDef(rfcDate[4].substring(3, 5), 0));
                c.set(Calendar.SECOND,      strToIntDef(rfcDate[4].substring(6), 0));

                long delta = strToIntDef(rfcDate[5].substring(1, 3), 0) * 60 * 60
                        + strToIntDef(rfcDate[5].substring(3, 5), 0) * 60;
                if ('+' == rfcDate[5].charAt(0)) {
                    delta = -delta;
                }
                result = Math.max(0, c.getTime().getTime() / 1000 + delta);
            }
            return result;
        } catch (Exception ignored) {
        }
        return 0;
    }
    public static long createLocalDate(String date) {
        try {
            date = date.replace('.', ' ').replace(':', ' ');
            String[] values = Util.explode(date, ' ');
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.YEAR, Util.strToIntDef(values[2], 0));
            c.set(Calendar.MONTH, Util.strToIntDef(values[1], 0) - 1);
            c.set(Calendar.DAY_OF_MONTH, Util.strToIntDef(values[0], 0));
            c.set(Calendar.HOUR_OF_DAY, Util.strToIntDef(values[3], 0));
            c.set(Calendar.MINUTE,      Util.strToIntDef(values[4], 0));
            c.set(Calendar.SECOND,      0);
            return localTimeToGmtTime(c.getTime().getTime() / 1000);
        } catch (Exception ignored) {
            return 0;
        }
    }


    /* Creates current date (local) */
    public static long createCurrentLocalTime() {
        return gmtTimeToLocalTime(Jimm.getCurrentGmtTime());
    }

    public static String getLocalDayOfWeek(long gmtTime) {
        // local
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(new Date(Util.gmtTimeToLocalTime(gmtTime) * 1000));
        String[] days = {"", "sunday", "monday", "tuesday", "wednesday",
                "thursday", "friday", "saturday"};
        return JLocale.getString(days[cal.get(Calendar.DAY_OF_WEEK)]);
    }
    public static String getLocalDateString(long gmtDate, boolean onlyTime) {
        if (0 == gmtDate) return "***error***";
        int[] localDate = createDate(gmtTimeToLocalTime(gmtDate));

        StringBuffer sb = new StringBuffer(16);

        if (!onlyTime) {
            sb.append(Util.makeTwo(localDate[TIME_DAY]))
              .append('.')
              .append(Util.makeTwo(localDate[TIME_MON]))
              .append('.')
              .append(localDate[TIME_YEAR])
              .append(' ');
        }

        sb.append(Util.makeTwo(localDate[TIME_HOUR]))
          .append(':')
          .append(Util.makeTwo(localDate[TIME_MINUTE]));

        return sb.toString();
    }
    public static String getDate(String format, long anyDate) {
        if (0 == anyDate) return "error";
        int[] localDate = createDate(anyDate);
        format = Util.replace(format, "%H", Util.makeTwo(localDate[TIME_HOUR]));
        format = Util.replace(format, "%M", Util.makeTwo(localDate[TIME_MINUTE]));
        format = Util.replace(format, "%S", Util.makeTwo(localDate[TIME_SECOND]));
        format = Util.replace(format, "%Y", "" + localDate[TIME_YEAR]);
        format = Util.replace(format, "%y", Util.makeTwo(localDate[TIME_YEAR] % 100));
        format = Util.replace(format, "%m", Util.makeTwo(localDate[TIME_MON]));
        format = Util.replace(format, "%d", Util.makeTwo(localDate[TIME_DAY]));
        return format;
    }

    /* Show date string */
    public static String getUtcDateString(long gmtTime) {
        return getDate("%Y-%m-%dT%H:%M:%SZ", gmtTime);
    }
    /* Generates seconds count from 1st Jan 1970 till mentioned date */
    public static long createGmtTime(int year, int mon, int day,
            int hour, int min, int sec) {
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, mon - 1);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            c.set(Calendar.SECOND, sec);
            return c.getTime().getTime() / 1000;
        } catch (Exception ignored) {
            return 0;
        }
    }

    // Creates array of calendar values form value of seconds since 1st jan 1970 (GMT)
    private static int[] createDate(long value) {
        int total_days, last_days, i;
        int sec, min, hour, day, mon, year;

        sec = (int) (value % 60);

        min = (int) ((value / 60) % 60); // min
        value -= 60 * min;

        hour = (int) ((value / 3600) % 24); // hour
        value -= 3600 * hour;

        total_days = (int) (value / (3600 * 24));

        year = 1970;
        for (;;) {
            last_days = total_days - ((year % 4 == 0) && (year != 2000) ? 366 : 365);
            if (last_days <= 0) break;
            total_days = last_days;
            year++;
        } // year

        int febrDays = ((year % 4 == 0) && (year != 2000)) ? 29 : 28;

        mon = 1;
        for (i = 0; i < 12; ++i) {
            last_days = total_days - ((i == 1) ? febrDays : dayCounts[i]);
            if (last_days <= 0) break;
            mon++;
            total_days = last_days;
        } // mon

        day = total_days; // day

        return new int[] { sec, min, hour, day, mon, year };
    }

    public static long gmtTimeToLocalTime(long gmtTime) {
        return gmtTime + Options.getInt(Options.OPTION_GMT_OFFSET) * 3600L;
    }
    public static long localTimeToGmtTime(long localTime) {
        return localTime - Options.getInt(Options.OPTION_GMT_OFFSET) * 3600L;
    }

    public static String longitudeToString(long seconds) {
        int days = (int)(seconds / 86400);
        seconds %= 86400;
        int hours = (int)(seconds / 3600);
        seconds %= 3600;
        int minutes = (int)(seconds / 60);

        StringBuffer buf = new StringBuffer();
        if (days != 0) {
            buf.append(days).append(' ').append( JLocale.getString("days") ).append(' ');
        }
        if (hours != 0) {
            buf.append(hours).append(' ').append( JLocale.getString("hours") ).append(' ');
        }
        if (minutes != 0) {
            buf.append(minutes).append(' ').append( JLocale.getString("minutes") );
        }

        return buf.toString();
    }


    //////////////////////////////////////////////////////////////////////////////////
    public static int uniqueValue() {
        int time = (int)(Jimm.getCurrentGmtTime() & 0x7FFF);
        return (time << 16) | (rand.nextInt() & 0xFFFF);
    }
    //////////////////////////////////////////////////////////////////////////////////
    private static final int URL_CHAR_PROTOCOL = 0;
    private static final int URL_CHAR_PREV    = 1;
    private static final int URL_CHAR_OTHER   = 2;
    private static final int URL_CHAR_DIGIT   = 3;
    private static final int URL_CHAR_NONE    = 4;

    private static boolean isURLChar(char chr, int mode) {
        if (mode == URL_CHAR_PROTOCOL) {
            return ((chr >= 'A') && (chr <= 'Z')) ||
                    ((chr >= 'a') && (chr <= 'z'));
        }

        if (mode == URL_CHAR_PREV) {
            return ((chr >= 'A') && (chr <= 'Z'))
                    || ((chr >= 'a') && (chr <= 'z'))
                    || ((chr >= '0') && (chr <= '9'))
                    || ('@' == chr) || ('-' == chr)
                    || ('_' == chr)|| ('%' == chr);
        }
        if (URL_CHAR_DIGIT == mode) return Character.isDigit(chr);
        if (URL_CHAR_NONE == mode) return (' ' == chr) || ('\n' == chr);

        if ((chr <= ' ') || (chr == '\n')) return false;
        return true;
    }

    private static void putUrl(Vector urls, String url) {
        final String skip = "?!;:,.";
        final String openDelemiters = "{[(«";
        final String delemiters = "}])»";
        int cutIndex = url.length() - 1;
        for (; cutIndex >= 0; --cutIndex) {
            char lastChar = url.charAt(cutIndex);
            if (-1 != skip.indexOf(lastChar)) {
                continue;
            }
            int delemiterIndex = delemiters.indexOf(lastChar);
            if (-1 != delemiterIndex) {
                if (-1 == url.indexOf(openDelemiters.charAt(delemiterIndex))) {
                    continue;
                }
            }
            break;
        }

        if (cutIndex <= 0) {
            return;

        } else if (cutIndex != url.length() - 1) {
            url = url.substring(0, cutIndex + 1);
        }

        if (-1 == url.indexOf(':')) {
            boolean isPhone = ('+' == url.charAt(0));
            boolean hasDot = false;
            boolean nonDigit = false;
            for (int i = isPhone ? 1 : 0; i < url.length(); ++i) {
                char ch = url.charAt(i);
                if ('.' == ch) {
                    hasDot = true;
                } else if (!Character.isDigit(ch)) {
                    nonDigit = true;
                    break;
                }
            }
            if (isPhone) {
                if (!nonDigit && !hasDot && (7 <= url.length())) {
                    url = "tel:" + url;
                } else {
                    return;
                }
            } else {
                if (nonDigit) {
                    if (-1 == url.indexOf('/')) {
                        if (-1 == url.indexOf('@')) return;
                        // jid or email
                    } else {
                        url = "http:\57\57" + url;
                    }
                } else {
                    return;
                }
            }
        }
        int protoEnd = url.indexOf(':');
        if (-1 != protoEnd) {
            if (url.length() <= protoEnd + 5) {
                return;
            }
            for (int i = 0; i < protoEnd; ++i) {
                if (!isURLChar(url.charAt(i), URL_CHAR_PROTOCOL)) {
                    return;
                }
            }
        }
        if (!urls.contains(url)) {
            urls.addElement(url);
        }
    }
    private static void parseForUrl(Vector result, String msg, char ch, int before, int after, int limit) {
        if (limit <= result.size()) {
            return;
        }
        int size = msg.length();
        int findIndex = 0;
        int beginIdx;
        int endIdx;
        for (;;) {
            if (findIndex >= size) break;
            int ptIndex = msg.indexOf(ch, findIndex);
            if (ptIndex == -1) break;

            for (endIdx = ptIndex + 1; endIdx < size; ++endIdx) {
                if (!isURLChar(msg.charAt(endIdx), after)) {
                    break;
                }
            }

            findIndex = endIdx;
            if (endIdx - ptIndex < 2) continue;

            if  (URL_CHAR_NONE != before) {
                for (beginIdx = ptIndex - 1; beginIdx >= 0; --beginIdx) {
                    if (!isURLChar(msg.charAt(beginIdx), before)) {
                        break;
                    }
                }
                if ((beginIdx == -1) || !isURLChar(msg.charAt(beginIdx), before)) {
                    beginIdx++;
                }
                if (ptIndex == beginIdx) continue;

            } else {
                beginIdx = ptIndex;
                if ((0 < beginIdx) && !isURLChar(msg.charAt(beginIdx - 1), before)) {
                    continue;
                }
            }
            if (endIdx - beginIdx < 5) continue;
            putUrl(result, msg.substring(beginIdx, endIdx));
            if (limit < result.size()) {
                return;
            }
        }
    }
    public static String getUrlWithoutProtocol(String url) {
        int index = url.indexOf(':');
        if (-1 != index) {
            url = url.substring(index + 1);
            if (url.startsWith("\57\57")) {
                url = url.substring(2);
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
        }
        return url;
    }
    public static String notUrls(String str) {
        str = StringConvertor.notNull(str);
        return (-1 != str.indexOf("http://")) ? "" : str;
    }
    public static boolean hasURL(String msg) {
        if (null == msg) return false;
        Vector result = new Vector();
        parseForUrl(result, msg, '.', URL_CHAR_PREV, URL_CHAR_OTHER, 1);
        parseForUrl(result, msg, ':', URL_CHAR_PROTOCOL, URL_CHAR_OTHER, 1);
        parseForUrl(result, msg, '+', URL_CHAR_NONE, URL_CHAR_DIGIT, 1);
        parseForUrl(result, msg, '@', URL_CHAR_PREV, URL_CHAR_OTHER, 1);
        return !result.isEmpty();
    }
    public static Vector parseMessageForURL(String msg) {
        if (null == msg) return null;
        // we are parsing 100 links only
        final int MAX_LINK_COUNT = 100;
        Vector result = new Vector();
        parseForUrl(result, msg, '.', URL_CHAR_PREV, URL_CHAR_OTHER, MAX_LINK_COUNT);
        parseForUrl(result, msg, ':', URL_CHAR_PROTOCOL, URL_CHAR_OTHER, MAX_LINK_COUNT);
        parseForUrl(result, msg, '+', URL_CHAR_NONE, URL_CHAR_DIGIT, MAX_LINK_COUNT);
        parseForUrl(result, msg, '@', URL_CHAR_PREV, URL_CHAR_OTHER, MAX_LINK_COUNT);
        return result.isEmpty() ? null : result;
    }

    public static int strToIntDef(String str, int defValue) {
        if (null == str) {
            return defValue;
        }
        try {
            while ((1 < str.length()) && ('0' == str.charAt(0))) {
                str = str.substring(1);
            }
            return Integer.parseInt(str);
        } catch (Exception ignored) {
        }
        return defValue;
    }


    public static String replace(String text, String from, String to) {
        int fromSize = from.length();
        int start = 0;
        int pos = 0;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            pos = text.indexOf(from, pos);
            if (-1 == pos) break;
            sb.append(text.substring(start, pos)).append(to);
            pos += fromSize;
            start = pos;
        }
        if (start < text.length()) {
            sb.append(text.substring(start));
        }
        return sb.toString();
    }

    public static String replace(String text, String[] from, String[] to, String keys) {
        // keys - is first chars of from
        StringBuffer result = new StringBuffer();
        int pos = 0;
        while (pos < text.length()) {
            char ch = text.charAt(pos);

            int index = keys.indexOf(ch);
            while (-1 != index) {
                if (text.startsWith(from[index], pos)) {
                    pos += from[index].length();
                    result.append(to[index]);
                    break;
                }
                index = keys.indexOf(text.charAt(pos), index + 1);
            }

            if (-1 == index) {
                result.append(ch);
                pos++;
            }
        }

        return result.toString();
    }

    /* Divide text to array of parts using serparator charaster */
    static public String[] explode(String text, char separator) {
        if (StringConvertor.isEmpty(text)) {
            return new String[0];
        }
        Vector tmp = new Vector();
        int start = 0;
        int end = text.indexOf(separator, start);
        while (end >= start) {
            tmp.addElement(text.substring(start, end));
            start = end + 1;
            end = text.indexOf(separator, start);
        }
        tmp.addElement(text.substring(start));
        String[] result = new String[tmp.size()];
        tmp.copyInto(result);
        return result;
    }
    static public String implode(String[] text, String separator) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < text.length; ++i) {
            if (null != text[i]) {
                if (0 != result.length()) {
                    result.append(separator);
                }
                result.append(text[i]);
            }
        }
        return result.toString();
    }

    private static final String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    private static int base64GetNextChar(String str, int index) {
        if (-1 == index) return -2;
        char ch = str.charAt(index);
        if ('=' == ch) {
            return -1;
        }
        return base64.indexOf(ch);
    }
    private static int base64GetNextIndex(String str, int index) {
        for (; index < str.length(); ++index) {
            char ch = str.charAt(index);
            if ('=' == ch) {
                return index;
            }
            int code = base64.indexOf(ch);
            if (-1 != code) {
                return index;
            }
        }
        return -1;
    }

    public static byte[] base64decode(String str) {
        if (null == str) str = "";
        Util out = new Util();
        for (int strIndex = 0; strIndex < str.length(); ++strIndex) {
    	    strIndex = base64GetNextIndex(str, strIndex);
            if (-1 == strIndex) break;
            int ch1 = base64GetNextChar(str, strIndex);
            if (-1 == ch1) break;

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch2 = base64GetNextChar(str, strIndex);
            if (-1 == ch2) break;
            out.writeByte((byte)(0xFF & ((ch1 << 2) | (ch2 >>> 4))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch3 = base64GetNextChar(str, strIndex);
            if (-1 == ch3) break;
            out.writeByte((byte)(0xFF & ((ch2 << 4) | (ch3 >>> 2))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch4 = base64GetNextChar(str, strIndex);
            if (-1 == ch4) break;
            out.writeByte((byte)(0xFF & ((ch3 << 6) | (ch4))));
        }
        return out.toByteArray();
    }
    public static String base64encode( final byte[] data ) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & data[i + 2]);
                quad = true;
            }
            out[index+3] = base64.charAt(quad ? (val & 0x3F) : 64);
            val >>= 6;
            out[index+2] = base64.charAt(trip ? (val & 0x3F) : 64);
            val >>= 6;
            out[index+1] = base64.charAt(val & 0x3F);
            val >>= 6;
            out[index+0] = base64.charAt(val & 0x3F);
        }
        return new String(out);
    }

    private static final String[] escapedChars = {"&quot;", "&apos;", "&gt;", "&lt;", "&amp;"};
    private static final String[] unescapedChars = {"\"", "'", ">", "<", "&"};
    public static String xmlEscape(String text) {
        text = StringConvertor.notNull(text);
        return Util.replace(text, unescapedChars, escapedChars, "\"'><&");
    }

    public static String xmlUnescape(String text) {
        if (-1 == text.indexOf('&')) {
            return text;
        }
        return Util.replace(text, escapedChars, unescapedChars, "&&&&&");
    }

    public static Image createThumbnail(Image image, int width, int height) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();

        if ((width > sourceWidth) && (height > sourceHeight)) {
            return image;
        }
        int thumbWidth = width;
        int thumbHeight = thumbWidth * sourceHeight / sourceWidth;
        if (thumbHeight > height) {
            thumbHeight = height;
            thumbWidth = thumbHeight * sourceWidth / sourceHeight;
        }

        Image thumb = Image.createImage(thumbWidth, thumbHeight);
        Graphics g = thumb.getGraphics();

        for (int y = 0; y < thumbHeight; ++y) {
            for (int x = 0; x < thumbWidth; ++x) {
                g.setClip(x, y, 1, 1);
                int dx = x * sourceWidth / thumbWidth;
                int dy = y * sourceHeight / thumbHeight;
                g.drawImage(image, x - dx, y - dy, Graphics.LEFT | Graphics.TOP);
            }
        }
        return thumb;
    }


    //////////////////////////////////////////////////////////////////////////////////
    private static int compareNodes(Sortable node1, Sortable node2) {
        int result = node1.getNodeWeight() - node2.getNodeWeight();
        if (0 == result) {
            result = StringConvertor.stringCompare(node1.getText(), node2.getText());
        }
        return result;
    }

    public static void sort(Vector subnodes) {
        for (int i = 1; i < subnodes.size(); ++i) {
            Sortable currNode = (Sortable)subnodes.elementAt(i);
            int j = i - 1;
            for (; j >= 0; --j) {
                Sortable itemJ = (Sortable)subnodes.elementAt(j);
                if (compareNodes(itemJ, currNode) <= 0) {
                    break;
                }
                subnodes.setElementAt(itemJ, j + 1);
            }
            if (j + 1 != i) {
                subnodes.setElementAt(currNode, j + 1);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    private static void putCh(StringBuffer sb, int ch) {
        String s = Integer.toHexString(ch);
        sb.append("%");
        if (1 == s.length()) sb.append('0');
        sb.append(s);
    }
    public static String urlEscape(String param) {
        String urlOK = "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < param.length(); ++i) {
            char ch = param.charAt(i);
            char lowerCh = Character.toLowerCase(ch);
            if (Character.isDigit(ch) || (-1 != "qwertyuiopasdfghjklzxcvbnm@.-".indexOf(lowerCh))) {
                sb.append(ch);

            } else if (' ' == ch) {
                sb.append('+');

            } else if ((0x7F & ch) == ch) {
                putCh(sb, ch);

            } else if ((0xFFF & ch) == ch) {
                putCh(sb, 0xD0 | (ch >> 6));
                putCh(sb, 0x80 | (0x3F & ch));

            } else {
                putCh(sb, 0xE8 | (ch >> 12));
                putCh(sb, 0x80 | (0x3F & (ch >> 6)));
                putCh(sb, 0x80 | (0x3F & ch));
            }
        }
        return sb.toString();
    }
    //////////////////////////////////////////////////////////////////////////////////
    public static int getIndex(Vector v, Object o) {
        int size = v.size();
        for (int i = 0; i < size; ++i) {
            if (v.elementAt(i) == o) {
                return i;
            }
        }
        return -1;
    }
}
