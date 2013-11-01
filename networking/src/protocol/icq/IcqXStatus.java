/*
 * IcqXStatus.java
 *
 * Created on 14 ������ 20, 07 �., 22:01
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_ICQ is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package protocol.icq;

import jimm.comm.*;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class IcqXStatus {
    private byte[][] guids;
    private short[] ids;

    public IcqXStatus() {
        Config config = new Config().loadLocale("/icq-xstatus.txt");
        String[] keys = config.getKeys();
        guids = new byte[keys.length][];
        ids = new short[keys.length];
        for (int i = 0; i < keys.length; ++i) {
            String[] codes = Util.explode(keys[i], StringUtils.DELIMITER);
            ids[i] = -1;
            for (int j = 0; j < codes.length; ++j) {
                if (codes[j].length() != 32) {
                    ids[i] = (short)Util.strToIntDef(codes[j], -1);
                } else {
                    guids[i] = getGuid(codes[j]);
                }
            }
        }
    }
    private byte[] getGuid(String line) {
        byte[] data = new byte[16];
        for (int i = 0; i < data.length; ++i) {
            data[i] =(byte)((Character.digit(line.charAt(i * 2), 16) << 4)
                    | (Character.digit(line.charAt(i * 2 + 1), 16)));

        }
        return data;
    }

    private int getGuid(byte[] myguids) {
        byte[] guid;
        for (int i = 0; i < guids.length; ++i) {
            guid = guids[i];
            if (null == guid) {
                continue;
            }
            for (int j = 0; j < myguids.length; j += 16) {
                for (int k = 0; k < 16; ++k) {
                    if (guid[k] != myguids[j + k]) {
                        break;
                    }
                    if (15 == k) {
                        return i;
                    }
                }
            }
        }
        return XStatusInfo.XSTATUS_NONE;
    }
    
    public int createXStatus(byte[] myguids, String mood) {
        if ((null != mood) && mood.startsWith("0icqmood")) {
            int id = Util.strToIntDef(mood.substring(8), -1);
            for (int i = 0; i < ids.length; ++i) {
                if (ids[i] == id) {
                    return (byte)i;
                }
            }
        }
        if (null != myguids) {
            return (byte)getGuid(myguids);
        }
        return XStatusInfo.XSTATUS_NONE;
    }
    

    public int getIcqMood(int xstatusIndex) {
        int index = xstatusIndex;
        if ((0 <= index) && (index < ids.length)) {
            return ids[index];
        }
        return -1;
    }
    public GUID getIcqGuid(int xstatusIndex) {
        int index = xstatusIndex;
        if ((0 <= index) && (index < ids.length)) {
            return new GUID(guids[index]);
        }
        return null;
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
