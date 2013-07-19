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
 File: src/jimm/comm/RequestInfoAction.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer
 *******************************************************************************/


// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;


import jimm.comm.*;
import jimm.*;
import protocol.icq.IcqContact;
import protocol.icq.packet.*;
import jimm.modules.*;
import jimm.search.*;

public class RequestInfoAction extends IcqAction {

    // Receive timeout
    private static final int TIMEOUT = 10; // seconds

    /****************************************************************************/

    private UserInfo strData;

    // Date of init
    private int packetCounter;
    private IcqContact contact;
    private boolean done = false;

    // Constructor
    public RequestInfoAction(UserInfo data, IcqContact item) {
        super();
        packetCounter = 0;
        contact = item;
        strData = data;
        strData.uin = contact.getUserId();
    }
    public UserInfo getUserInfo() {
        return strData;
    }

    // Init action
    public void init() throws JimmException {

        // Send a CLI_METAREQINFO packet
        Util stream = new Util();
        try {
            stream.writeWordLE(ToIcqSrvPacket.CLI_META_REQMOREINFO_TYPE);
            stream.writeDWordLE(Long.parseLong(strData.uin));
            sendPacket(new ToIcqSrvPacket(0, getIcq().getUserId(), ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], stream.toByteArray()));
        } catch (Exception ignored) {
            requestNew();
        }

        // Save date
        active();
    }
    private void requestNew() throws JimmException {
        Util stream = new Util();
        byte[] uin = strData.uin.getBytes();

        stream.writeWordLE(ToIcqSrvPacket.CLI_META_REQUEST_FULL_INFO);
        stream.writeWordLE(30 + uin.length);
        stream.writeWordBE(0x05b9);
        stream.writeWordBE(ToIcqSrvPacket.CLI_META_REQUEST_FULL_INFO);
        stream.writeDWordBE(0x00000000);
        stream.writeDWordBE(0x00000000);
        stream.writeDWordBE(0x04e30000);
        stream.writeDWordBE(0x00020003);
        stream.writeDWordBE(0x00000001);
        stream.writeWordBE(4 + uin.length);
        stream.writeWordBE(0x0032);
        stream.writeWordBE(uin.length);
        stream.writeByteArray(uin);
        sendPacket(new ToIcqSrvPacket(0, getIcq().getUserId(), ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], stream.toByteArray()));
    }


    private String readAsciiz(ArrayReader stream) {
        int len = stream.getWordLE();
        if (len == 0) {
            return "";
        }
        byte[] buffer = stream.getArray(len);
        // TODO: check it
        return StringConvertor.byteArrayToWinString(buffer, 0, buffer.length).trim();
    }


    // Forwards received packet, returns true if packet was consumed
    public boolean forward(Packet packet) throws JimmException {
        boolean consumed = false;

        // Watch out for SRV_FROMICQSRV packet
        if (packet instanceof FromIcqSrvPacket) {
            FromIcqSrvPacket fromIcqSrvPacket = (FromIcqSrvPacket) packet;

            // Watch out for SRV_META packet
            if (fromIcqSrvPacket.getSubcommand() != FromIcqSrvPacket.SRV_META_SUBCMD) {
                return false;
            }

            // Get packet data
            ArrayReader stream = fromIcqSrvPacket.getReader();

            // Watch out for SRV_METAGENERAL packet
            try {

                int type = stream.getWordLE();
                stream.getByte(); // Success byte
                if (FromIcqSrvPacket.SRV_META_FULL_INFO == type) {
                    stream.skip(5*2 + 21);
                    processFillInfo(stream);
                    return true;
                }
                switch (type) {
                case FromIcqSrvPacket.SRV_META_GENERAL_TYPE: //  basic user information
                    {
                        strData.nick        = readAsciiz(stream); // nickname
                        strData.firstName   = readAsciiz(stream);
                        strData.lastName    = readAsciiz(stream);
                        strData.email       = readAsciiz(stream); // email
                        strData.homeCity    = readAsciiz(stream); // home city
                        strData.homeState   = readAsciiz(stream); // home state
                        strData.homePhones  = readAsciiz(stream); // home phone
                        strData.homeFax     = readAsciiz(stream); // home fax
                        strData.homeAddress = readAsciiz(stream); // home address
                        strData.cellPhone   = readAsciiz(stream); // cell phone
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                case 0x00DC: // more user information
                    {
                        strData.age = stream.getWordLE();
                        strData.gender = (byte)stream.getByte();
                        strData.homePage = readAsciiz(stream);
                        int year = stream.getWordLE();
                        int mon  = stream.getByte();
                        int day  = stream.getByte();
                        strData.birthDay = (year != 0)
                                ? (day + "." + (mon < 10 ? "0" : "") + mon + "." + year)
                                : null;
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                case 0x00D2: // work user information
                    {
                        strData.workCity    = readAsciiz(stream);
                        strData.workState   = readAsciiz(stream);
                        strData.workPhone   = readAsciiz(stream);
                        strData.workFax     = readAsciiz(stream);
                        strData.workAddress = readAsciiz(stream);

                        readAsciiz(stream);                          // work zip code
                        stream.getWordLE();                      // work country code
                        strData.workCompany    = readAsciiz(stream); // work company
                        strData.workDepartment = readAsciiz(stream); // work department
                        strData.workPosition   = readAsciiz(stream); // work position
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                case 0x00E6: // user about information
                    {
                        strData.about = readAsciiz(stream); // notes string
                        packetCounter++;
                        consumed = true;
                        break;
                    }

                case 0x00F0: // user interests information
                    {
                        StringBuffer sb = new StringBuffer();
                        int counter = stream.getByte();
                        for (int i = 0; i < counter; ++i) {
                            stream.getWordLE();
                            String item = readAsciiz(stream);
                            if (item.length() == 0) continue;
                            if (sb.length() != 0) sb.append(", ");
                            sb.append(item);
                        }
                        strData.interests = sb.toString();
                        packetCounter++;
                        consumed = true;
                        break;
                    }
                }

            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.panic("Request Info action", e);
                // #sijapp cond.end#
            }
            if (packetCounter >= 5) {
                requestNew();
            }

            // is completed?
            strData.setOptimalName();
            strData.updateProfileView();
        }

        return consumed;
    }
    private void processFillInfo(ArrayReader stream) {
        done = true;
        int len = stream.getWordBE();
        int offset = stream.getOffset();
        strData.nick = str(stream.getTlvData(0x0046, offset, len));
        strData.firstName = str(stream.getTlvData(0x0064, offset, len));
        strData.lastName = str(stream.getTlvData(0x006e, offset, len));
        strData.gender = stream.getTlvData(0x0082, offset, len)[0];
        strData.homePage = str(stream.getTlvData(0x00fa, offset, len));
        strData.about = str(stream.getTlvData(0x0186, offset, len));
        strData.homeState = getTvlData(0x0096, 0x0078, stream, offset, len);
        strData.homeCity = getTvlData(0x0096, 0x0064, stream, offset, len);
        strData.homeAddress = getTvlData(0x0096, 0x006e, stream, offset, len);
        if (StringConvertor.isEmpty(strData.homeCity) && StringConvertor.isEmpty(strData.homeAddress)) {
            strData.homeCity = getTvlData(0x00a0, 0x0064, stream, offset, len);
            strData.homeAddress = getTvlData(0x00a0, 0x006e, stream, offset, len);
        }
        strData.workState = getTvlData(0x0118, 0x00be, stream, offset, len);
        strData.workCity = getTvlData(0x0118, 0x00b4, stream, offset, len);
        strData.workDepartment = getTvlData(0x0118, 0x007D, stream, offset, len);
        strData.workCompany = getTvlData(0x0118, 0x006e, stream, offset, len);
        strData.workPosition = getTvlData(0x0118, 0x0064, stream, offset, len);

        strData.setOptimalName();
        strData.updateProfileView();

        /*
        stream.setOffset(offset);
        while (stream.isNotEnd()) {
            int type = stream.getTlvType();
            byte[] data = stream.getTlv();
            DebugLog.dump("type " + Integer.toHexString(type) + " " + StringConvertor.byteArray1251ToString(data, 0, data.length), data);
        }
        */
    }
    private String getTvlData(int type, int subtype, ArrayReader stream, int offset, int len) {
        byte[] data = stream.getTlvData(type, offset, len);
        if (null == data) return null;
        if (0 == subtype) return str(data);
        ArrayReader sub = new ArrayReader(data, 2);
        int subLen = sub.getWordBE();
        data = sub.getTlvData(subtype, 4, subLen);
        if (null == data) return null;
        return str(data);
    }
    private String str(byte[] data) {
        if (null == data) return null;
        return StringConvertor.utf8beByteArrayToString(data, 0, data.length).trim();
    }

    // Returns true if the action is completed
    public boolean isCompleted() {
        return done;
    }


    // Returns true if an error has occured
    public boolean isError() {
        return isNotActive(TIMEOUT);
    }


}
// #sijapp cond.end #
