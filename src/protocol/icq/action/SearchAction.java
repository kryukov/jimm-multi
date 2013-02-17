/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-04  Jimm Project

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
File: src/jimm/comm/SearchAction.java
 Version: ###VERSION###  Date: ###DATE###
Author(s): Andreas Rossbacher
*******************************************************************************/

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;

import jimm.*;
import jimm.comm.*;
import protocol.icq.packet.*;
import jimm.search.*;


public class SearchAction extends IcqAction {

    // States of search mission
    private static final int STATE_ERROR         = -1;
    private static final int STATE_SEARCH_SENT   = 1;
    private static final int STATE_NEXT_RECEIVED = 2;
    private static final int STATE_LAST_RECEIVED = 3;
    private static final int STATE_FINISHED      = 4;

    // TLVs used in LE format
    public static final int TLV_TYPE_UIN                 = 0x0136; // long (4 bytes)
    public static final int TLV_TYPE_NICK                = 0x0154; // String (2 bytes length + string)
    public static final int TLV_TYPE_FIRSTNAME           = 0x0140; // String (2 bytes length + string)
    public static final int TLV_TYPE_LASTNAME            = 0x014A; // String (2 bytes length + string)
    public static final int TLV_TYPE_EMAIL               = 0x015E; // String (2 bytes length + string + 1 byte email code)
    public static final int TLV_TYPE_CITY                = 0x0190; // String (2 bytes length + string)
    public static final int TLV_TYPE_GENDER              = 0x017C; // UINT8 (1 byte: 1 - female, 2 - male)
    public static final int TLV_TYPE_ONLYONLINE          = 0x0230; // UINT8 (1 byte:  1 - search online, 0 - search all)
    public static final int TLV_TYPE_AGE                 = 0x0168; // ages (2 bytes from, 2 bytes to)


    // Timeout
    public static final int TIMEOUT = 60; // seconds

    /****************************************************************************/

    // Action state
    private int state;

    // Search object as container for request and results
    private Search cont;
    private int searchId;

    public SearchAction(Search cont) {
    	super();
    	this.cont = cont;
        searchId = cont.getSearchId();
    }
    private boolean isCanceled() {
        return searchId != cont.getSearchId();
    }

    private void addStr(Util buffer, int type, int param) {
        String str = cont.getSearchParam(param);
        if (null != str) {
            buffer.writeProfileAsciizTLV(type, str);
        }
    }
    private void addByte(Util buffer, int type, int value) {
        buffer.writeWordLE(type);
        buffer.writeWordLE(1);
        buffer.writeByte(value);
    }
    // Init action
    public void init() throws JimmException {
        Util buffer = new Util();

        buffer.writeWordLE(0x055f);

        // UIN
        int uin = Util.strToIntDef(cont.getSearchParam(Search.UIN), 0);
        if (0 != uin) {
            buffer.writeWordLE(TLV_TYPE_UIN);
            buffer.writeWordLE(0x0004);
            buffer.writeDWordLE(uin);
        }

        addStr(buffer, TLV_TYPE_NICK, Search.NICK);
        addStr(buffer, TLV_TYPE_FIRSTNAME, Search.FIRST_NAME);
        addStr(buffer, TLV_TYPE_LASTNAME, Search.LAST_NAME);
        addStr(buffer, TLV_TYPE_EMAIL, Search.EMAIL);
        addStr(buffer, TLV_TYPE_CITY, Search.CITY);

        // Age (user enter age as "minAge-maxAge", "-maxAge", "minAge-")
        String[] age = Util.explode(cont.getSearchParam(Search.AGE), '-');
        if ((age.length == 2) && ((age[0].length() > 0) || (age[1].length() > 0))) {
            buffer.writeWordLE(TLV_TYPE_AGE);
            buffer.writeWordLE(4);
            buffer.writeWordLE(Util.strToIntDef(age[0], 0));
            buffer.writeWordLE(Util.strToIntDef(age[1], 99));
        }


        // Gender
        int gender = Util.strToIntDef(cont.getSearchParam(Search.GENDER), 0);
        if (0 != gender) {
            addByte(buffer, TLV_TYPE_GENDER, gender);
        }

        // Only online
        if ("1".equals(cont.getSearchParam(Search.ONLY_ONLINE))) {
            addByte(buffer, TLV_TYPE_ONLYONLINE, 1);
        }

        sendPacket(new ToIcqSrvPacket(SnacPacket.CLI_TOICQSRV_COMMAND,
                0x0002, getIcq().getUserId(), 0x07D0, new byte[0], buffer.toByteArray()));

        active();
        this.state = STATE_SEARCH_SENT;
    }


    // Forwards received packet, returns true if packet was consumed
    public boolean forward(Packet packet) throws JimmException {
        if (isCanceled() || !(packet instanceof FromIcqSrvPacket)) {
            return false;
        }

        // Watch out for SRV_FROMICQSRV packet type
        FromIcqSrvPacket fromIcqServerPacket = (FromIcqSrvPacket) packet;
        UserInfo info = new UserInfo(getIcq());

        ArrayReader reader = fromIcqServerPacket.getReader();

        int dataSubType = reader.getWordBE();
        if (0xa401 == dataSubType) {
            this.state = STATE_NEXT_RECEIVED;
        } else if (0xae01 == dataSubType) {
            this.state = STATE_LAST_RECEIVED;
        } else {
            return false;
        }

        if (0x0A != reader.getByte()) {
            this.state = STATE_ERROR;
            return true;
        }

        reader.getWordLE(); // data size
        // Get UIN
        info.uin = String.valueOf(reader.getDWordLE());

        // Get nick
        String[] strings = new String[4];

        // Get the strings
        // #0 Nick
        // #1 Firstname
        // #2 Lastname
        // #3 EMail
        for (int i = 0; i < 4; ++i) {
            int len = reader.getWordLE();
            byte[] str = reader.getArray(len);
            strings[i] = StringConvertor.byteArrayToWinString(str, 0, len);
        }
        info.nick = strings[0];
        info.firstName = strings[1];
        info.lastName = strings[2];
        info.email = strings[3];

        // Get auth flag
        info.auth = (0 == reader.getByte());

        // Get status
        int status = reader.getWordLE();
        info.status = Integer.toString(status);

        // Get gender
        info.gender = (byte) reader.getByte();

        // Get age
        info.age = reader.getWordLE();
        cont.addResult(info);
        if (this.state == STATE_LAST_RECEIVED) {
            long foundleft = reader.getDWordLE();
            this.state = STATE_FINISHED;
            cont.finished();
        }
        // Update activity timestamp
        active();

        return true;
    }

    // Returns true if the action is completed
    public boolean isCompleted() {
        return (state == STATE_FINISHED) || isCanceled();
    }

    // Returns true if an error has occured
    public boolean isError() {
        if (!isCompleted() && isNotActive(TIMEOUT)) {
            cont.finished();
            state = STATE_ERROR;
        }
        return (STATE_ERROR == state);
    }
}
// #sijapp cond.end #
