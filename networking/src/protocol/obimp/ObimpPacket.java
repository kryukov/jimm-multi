/*
 * ObimpPacket.java
 *
 * Created on 5 Декабрь 2010 г., 14:05
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
public class ObimpPacket {
    private Util outPacket = new Util();
    private int type;
    private int subtype;
    private ObimpData data;

    public ObimpPacket(int type, int subtype, ObimpData data) {
        this.type = type;
        this.subtype = subtype;
        this.data = data;
    }
    public int getType() {
        return type;
    }
    public int getSubType() {
        return subtype;
    }
    public ObimpData getData() {
        return data;
    }


    public ObimpPacket(int type, int subtype) {
        this.type = type;
        this.subtype = subtype;
        outPacket.writeByte('#');
        outPacket.writeDWordBE(0);
        outPacket.writeWordBE(type);
        outPacket.writeWordBE(subtype);
        outPacket.writeDWordBE(0);
        outPacket.writeDWordBE(0);
    }
    
    public Util raw() {
        return outPacket;
    }
    public void writeWtld(int type, byte[] data) {
        outPacket.writeDWordBE(type);
        outPacket.writeDWordBE(data.length);
        outPacket.writeByteArray(data);
    }
    public void writeWtld_long(int type, long data) {
        outPacket.writeDWordBE(type);
        outPacket.writeDWordBE(4);
        outPacket.writeDWordBE(data);
    }
    public void writeWtld_word(int type, int data) {
        outPacket.writeDWordBE(type);
        outPacket.writeDWordBE(2);
        outPacket.writeWordBE(data);
    }
    public void writeWtld_byte(int type, int data) {
        outPacket.writeDWordBE(type);
        outPacket.writeDWordBE(1);
        outPacket.writeByte(data);
    }
    public void writeWtld_str(int type, String data) {
        data = StringConvertor.notNull(data);
        writeWtld(type, StringConvertor.stringToByteArrayUtf8(data));
    }
    public void writeWtld_flag(int type) {
        outPacket.writeDWordBE(type);
        outPacket.writeDWordBE(0);
    }
    public void writeWtld_notNullStr(int type, String data) {
        if (!StringConvertor.isEmpty(data)) {
            writeWtld(type, StringConvertor.stringToByteArrayUtf8(data));
        }
    }
    public void writeStld(int type, byte[] data) {
        outPacket.writeWordBE(type);
        outPacket.writeWordBE(data.length);
        outPacket.writeByteArray(data);
    }
    public byte[] toByteArray(int seq) {
        final int headerLength = (1 + 4 + 2 + 2 + 4 + 4);
        byte[] buf = outPacket.toByteArray();
        Util.putDWordBE(buf, 1, seq);
        Util.putDWordBE(buf, 9, seq);
        Util.putDWordBE(buf, 13, buf.length - headerLength);
        return buf;
    }
}
// #sijapp cond.end #