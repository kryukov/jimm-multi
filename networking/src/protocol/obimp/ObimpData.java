/*
 * ObimpData.java
 *
 * Created on 9 Декабрь 2010 г., 20:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_OBIMP is "true" #
package protocol.obimp;

import jimm.comm.StringConvertor;
import jimm.comm.Util;

/**
 *
 * @author Vladimir Kryukov
 */
public class ObimpData {
    private byte[] inData;
    private int inCursor;
    /** Creates a new instance of ObimpData */
    public ObimpData(byte[] data) {
        inData = data;
        inCursor = 0;
    }
    public byte[] getWtld(int tlvNum) {
        int ip = 0;
        while (ip < inData.length) {
            int id = (int)Util.getDWordBE(inData, ip);
            int len = (int)Util.getDWordBE(inData, ip + 4);
            if (id == tlvNum) {
                byte[] data = new byte[len];
                if (0 < len) {
                    System.arraycopy(inData, ip + 8, data, 0, len);
                }
                return data;
            }
            ip += 4 + 4 + len;
        }
        return null;
    }
    public byte getWtld_byte(int tlvNum) {
        byte[] buf = getWtld(tlvNum);
        return (null == buf) ? 0 : buf[0];
    }
    public int getWtld_word(int tlvNum) {
        byte[] buf = getWtld(tlvNum);
        return (null == buf) ? 0 : Util.getWordBE(buf, 0);
    }
    public long getWtld_dword(int tlvNum) {
        byte[] buf = getWtld(tlvNum);
        return (null == buf) ? 0 : Util.getDWordBE(buf, 0);
    }
    public String getWtld_str(int tlvNum) {
        byte[] buf = getWtld(tlvNum);
        return (null == buf) ? null : StringConvertor.utf8beByteArrayToString(buf, 0, buf.length);
    }
    public int getWtldType() {
        return (int)Util.getDWordBE(inData, inCursor);
    }
    public byte[] getWtldData() {
        int len = (int)Util.getDWordBE(inData, inCursor + 4);
        byte[] data = new byte[len];
        if (0 < len) {
            System.arraycopy(inData, inCursor + 8, data, 0, len);
        }
        return data;
    }
    public void skipWtld() {
        inCursor += 8 + (int)Util.getDWordBE(inData, inCursor + 4);
    }
    public int getStldType() {
        return (int)Util.getWordBE(inData, inCursor);
    }
    public byte[] getStldData() {
        int len = (int)Util.getWordBE(inData, inCursor + 2);
        byte[] data = new byte[len];
        if (0 < len) {
            System.arraycopy(inData, inCursor + 4, data, 0, len);
        }
        return data;
    }
    public void skipStld() {
        inCursor += 4 + (int)Util.getWordBE(inData, inCursor + 2);
    }

    public long getDWordBE() {
        long value = Util.getDWordBE(inData, inCursor);
        inCursor += 4;
        return value;
    }
    public int getWordBE() {
        int value = Util.getWordBE(inData, inCursor);
        inCursor += 2;
        return value;
    }
    public boolean isEof() {
        return inData.length <= inCursor;
    }
    public byte[] getData(int len) {
        byte[] data = new byte[len];
        if (0 < len) {
            System.arraycopy(inData, inCursor, data, 0, len);
        }
        inCursor += len;
        return data;
    }

    public byte[] b() {
        return inData;
    }
}
// #sijapp cond.end #