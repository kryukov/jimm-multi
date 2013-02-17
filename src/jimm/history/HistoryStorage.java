/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/HistoryStorage.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Igor Palkin
 *******************************************************************************/

// #sijapp cond.if modules_HISTORY is "true" #

package jimm.history;

// #sijapp cond.if modules_HISTORY is "true" #
import javax.microedition.rms.*;
import jimm.cl.ContactList;
import jimm.io.Storage;
import java.io.*;
import jimm.comm.*;
import protocol.Contact;



// History storage implementation
public class HistoryStorage {
    //===================================//
    //                                   //
    //    Data storage implementation    //
    //                                   //
    //===================================//

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private String storageName;
    private Storage historyStore;
    private int currRecordCount = -1;
    // #sijapp cond.if modules_ANDROID is "true" #
    private AndroidHistoryStorage androidStorage;
    // #sijapp cond.end #

    public HistoryStorage(Contact contact) {
        this.contact = contact;
        uniqueUserId = ContactList.getInstance().getProtocol(contact).getUniqueUserId(contact);
        storageName = getRSName();
        // #sijapp cond.if modules_ANDROID is "true" #
        androidStorage = new AndroidHistoryStorage(this);
        // #sijapp cond.end #
    }
    public Contact getContact() {
        return contact;
    }

    public static HistoryStorage getHistory(Contact contact) {
        return new HistoryStorage(contact);
    }

    private boolean openHistory(boolean create) {
        if (null == historyStore) {
            try {
                historyStore = new Storage(storageName);
                historyStore.open(create);
            } catch (Exception e) {
                historyStore = null;
                return false;
            }
        }
        return true;
    }
    public void openHistory() {
        openHistory(false);
    }
    public void closeHistory() {
        if (null != historyStore) {
            historyStore.close();
        }
        historyStore = null;
        currRecordCount = -1;
    }

    synchronized void closeHistoryView() {
        closeHistory();
    }
    /**
     *  Add message text to contact history
     *
     * @param text text to save
     * @param incoming type of message 0 - incoming, 1 - outgouing
     * @param from sender
     * @param gmtTime date of message
     */
    public synchronized void addText(String text, boolean incoming,
            String from, long gmtTime) {
        // #sijapp cond.if modules_ANDROID is "true" #
        androidStorage.addText(text, incoming, from, gmtTime);
        // #sijapp cond.else #
        boolean isOpened = openHistory(true);
        if (!isOpened) {
            return;
        }
        byte type = (byte) (incoming ? 0 : 1);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream das = new DataOutputStream(baos);
            das.writeByte(type);
            das.writeUTF(from);
            das.writeUTF(text);
            das.writeUTF(Util.getLocalDateString(gmtTime, false));
            byte[] buffer = baos.toByteArray();
            historyStore.addRecord(buffer);
        } catch (Exception e) {
            // do nothing
        }
        closeHistory();
        currRecordCount = -1;
        // #sijapp cond.end #
    }

    // Returns reference for record store
    RecordStore getRS() {
        return historyStore.getRS();
    }

    // Returns record store name for Contact
    private String getRSName() {
        return Storage.getStorageName(PREFIX + getUniqueUserId());
    }
    String getUniqueUserId() {
        return uniqueUserId;
    }

    // Returns record count for Contact
    public int getHistorySize() {
        // #sijapp cond.if modules_ANDROID is "true" #
        currRecordCount = androidStorage.getHistorySize();
        // #sijapp cond.else #
        if (currRecordCount < 0) {
            openHistory(false);
            currRecordCount = 0;
            try {
                if (null != historyStore) {
                    currRecordCount = historyStore.getNumRecords();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        // #sijapp cond.end #
        return currRecordCount;
    }

    // Returns full data of stored message
    public CachedRecord getRecord(int recNo) {
        // #sijapp cond.if modules_ANDROID is "true" #
        return androidStorage.getRecord(recNo);
        // #sijapp cond.else #
        if (null == historyStore) {
            openHistory(false);
        }
        CachedRecord result = new CachedRecord();
        try {
            byte[] data = historyStore.getRecord(recNo + 1);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            result.type = dis.readByte();
            result.from = dis.readUTF();
            result.text = dis.readUTF();
            result.date = dis.readUTF();

        } catch (Exception e) {
            result.type = 0;
            result.from = "";
            result.text = "";
            result.date = "";
        }
        return result;
        // #sijapp cond.end #
    }

    // Clears messages history for Contact
    public void removeHistory() {
        closeHistory();
        removeRMS(storageName);
    }

    private void removeRMS(String rms) {
        new Storage(rms).delete();
    }
    // Clears all records for all uins
    public void clearAll(boolean except) {
        closeHistory();
        String exceptRMS = (except ? storageName : null);
        String[] stores = Storage.getList();

        for (int i = 0; i < stores.length; ++i) {
            String store = stores[i];
            if (!store.startsWith(PREFIX)) {
                continue;
            }
            if (store.equals(exceptRMS)) {
                continue;
            }
            removeRMS(store);
        }
    }
}
// #sijapp cond.end#