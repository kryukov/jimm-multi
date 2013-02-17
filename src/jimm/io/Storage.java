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
*******************************************************************************
File: src/jimm/Options.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
           Vladimir Kryukov
******************************************************************************/

/*
 * Storage.java
 *
 * Created on 22 Январь 2010 г., 23:40
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.io;

import java.io.*;
import java.util.Vector;
import javax.microedition.rms.*;
import jimm.Jimm;
import jimm.comm.StringConvertor;

/**
 * RMS wrapper
 *
 * @author Vladimir Kryukov
 */
public final class Storage {
    public static final int SLOT_VERSION  = 1;
    public static final int SLOT_OPTIONS  = 2;

    private RecordStore rs = null;
    private String name;
    /** Creates a new instance of Storage */
    public Storage(String name) {
        this.name = Storage.getStorageName(name);
    }
    public static String[] getList() {
        String[] recordStores = RecordStore.listRecordStores();
        if (null == recordStores) {
            recordStores = new String[0];
        }
        return recordStores;
    }
    public boolean exist() {
        String[] recordStores = Storage.getList();
        for (int i = 0; i < recordStores.length; ++i) {
            if (name.equals(recordStores[i])) {
                return true;
            }
        }
        return false;
    }

    public static String getStorageName(String name) {
        return (32 < name.length()) ? name.substring(0, 32) : name;
    }
    public void delete() {
        try {
            RecordStore.deleteRecordStore(name);
        } catch (Exception ignored) {
        }
    }
    public boolean isOppened() {
        return null != rs;
    }
    public void open(boolean create) throws IOException, RecordStoreException {
        if (null == rs) {
            rs = RecordStore.openRecordStore(name, create);
        }
    }
    public byte[] getRecord(int recordNum) {
        if (null == rs) {
            return null;
        }
        try {
            return rs.getRecord(recordNum);
        } catch (Exception ignored) {
        }
        return null;
    }
    public void close() {
        try {
            rs.closeRecordStore();
        } catch (Exception ignored) {
        }
        rs = null;
    }
    public void initRecords(int count) throws RecordStoreException {
        // Add empty records if necessary
        if (rs.getNumRecords() < count) {
            if ((1 < count) && (0 == rs.getNumRecords())) {
                byte[] version = StringConvertor.stringToByteArrayUtf8(Jimm.VERSION);
                rs.addRecord(version, 0, version.length);
            }
            while (rs.getNumRecords() < count) {
                rs.addRecord(new byte[0], 0, 0);
            }
        }
    }
    public void addRecord(byte[] data) throws RecordStoreException {
        rs.addRecord(data, 0, data.length);
    }
    public void setRecord(int num, byte[] data) throws RecordStoreException {
        rs.setRecord(num, data, 0, data.length);
    }
    public void deleteRecord(int num) {
        try {
            rs.deleteRecord(num);
        } catch (Exception ignored) {
        }
    }
    private void putRecord(int num, byte[] data) throws RecordStoreException {
        initRecords(num);
        setRecord(num, data);
    }
    public int getNumRecords() throws RecordStoreException {
        return rs.getNumRecords();
    }
    public RecordStore getRS() {
        return rs;
    }

    public void saveListOfString(Vector strings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < strings.size(); ++i) {
                dos.writeUTF(StringConvertor.notNull((String)strings.elementAt(i)));
                addRecord(baos.toByteArray());
                baos.reset();
            }
            baos.close();
        } catch (Exception ignored) {
        }
    }
    public Vector loadListOfString() {
        Vector strings = new Vector();
        try {
            for (int i = 0; i < getNumRecords(); ++i) {
                byte[] data = getRecord(i + 1);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                strings.addElement(dis.readUTF());
                bais.close();
            }
        } catch (Exception ignored) {
        }
        return strings;
    }

    public static byte[] loadSlot(int slotId) {
        try {
            Storage storage = new Storage("rms-options");
            storage.open(false);
            byte[] slot = storage.getRecord(slotId);
            storage.close();
            return slot;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void saveSlot(int slotId, byte[] buf) {
        try {
            Storage storage = new Storage("rms-options");
            storage.open(true);
            storage.initRecords(2);
            storage.setRecord(slotId, buf);
            storage.close();
        } catch (Exception ignored) {
        }
    }
    public void saveXStatuses(String[] titles, String[] descs) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < titles.length; ++i) {
                dos.writeUTF(StringConvertor.notNull(titles[i]));
                dos.writeUTF(StringConvertor.notNull(descs[i]));
            }
            putRecord(1, baos.toByteArray());
        } catch (Exception ignored) {
        }
    }
    public void loadXStatuses(String[] titles, String[] descs) {
        try {
            byte[] buf = getRecord(1);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);
            for (int i = 0; i < titles.length; ++i) {
                titles[i] = StringConvertor.notNull(dis.readUTF());
                descs[i]  = StringConvertor.notNull(dis.readUTF());
            }
        } catch (Exception ignored) {
        }
    }
}
