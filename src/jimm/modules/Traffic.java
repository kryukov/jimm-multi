/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
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
 * File: src/jimm/modules/traffic/Traffic.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author: Andreas Rossbacher
 *******************************************************************************/

// #sijapp cond.if modules_TRAFFIC is "true" #

package jimm.modules;

import java.io.*;
import java.util.*;
import javax.microedition.rms.RecordStoreException;

import jimm.*;
import jimm.cl.ContactList;
import jimm.io.Storage;
import jimm.comm.*;


public final class Traffic {
    // Traffic read form file
    private int allInTraffic;
    private int allOutTraffic;

    // Traffic for this session
    private int sessionInTraffic;
    private int sessionOutTraffic;

    // Date of last reset of allInTraffic
    private Date savedSince;

    // Amount of money for all
    private int savedCost;

    private volatile int trafficBlockSize = 0;
    private static final int UPDATE_CL_BYTES = 2048;
    private static final int MTU = 576;
    private static final int TCP_HEADER_SIZE = 24;

    // Constructor
    private Traffic() {
    }

    private static Traffic instance = new Traffic();
    public static Traffic getInstance() {
        return instance;
    }

    //Loads traffic from file
    public final void load() {
        sessionInTraffic  = 0;
        sessionOutTraffic = 0;
        savedCost = 0;
        savedSince = new Date();
        Storage traffic = new Storage("traffic");
        try {
            traffic.open(false);

            byte[] buf = traffic.getRecord(2);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);

            allInTraffic  = dis.readInt();
            allOutTraffic = dis.readInt();
            savedSince.setTime(dis.readLong());
            long yesterday = dis.readLong();
            savedCost = dis.readInt();
        } catch (Exception e) {
            savedSince.setTime(new Date().getTime());
            allInTraffic = 0;
            sessionOutTraffic = 0;
            savedCost = 0;
        }
        traffic.close();
    }

    // Saves traffic from file
    private synchronized void save() throws IOException, RecordStoreException {
        // Open record store
        Storage traffic = new Storage("traffic");
        traffic.open(true);
        // Add empty records if necessary
        traffic.initRecords(2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Add traffic amount and savedSince to record store
        dos.writeInt(allInTraffic + sessionInTraffic);
        dos.writeInt(allOutTraffic + sessionOutTraffic);
        dos.writeLong(savedSince.getTime());
        dos.writeLong(0);
        generateCostSum(0, 0, false);
        dos.writeInt(savedCost);
        traffic.setRecord(2, baos.toByteArray());

        traffic.close();
    }

    public void safeSave() {
        try {
            save();
        } catch (Exception e) {
        }
    }

    public int getAllInTraffic() {
        return allInTraffic + sessionInTraffic;
    }
    public int getAllOutTraffic() {
        return allOutTraffic + sessionOutTraffic;
    }
    public int getSessionInTraffic() {
        return sessionInTraffic;
    }
    public int getSessionOutTraffic() {
        return sessionOutTraffic;
    }

    // Generates String for Traffic Info Screen
    public String getTrafficString() {
        Calendar time = Calendar.getInstance();
        time.setTime(savedSince);
        return (Util.makeTwo(time.get(Calendar.DAY_OF_MONTH)) + "." +
                Util.makeTwo(time.get(Calendar.MONTH) + 1) + "." +
                time.get(Calendar.YEAR));
    }

    // Returns String value of cost value
    public static String costToString(int value) {
        String costString = "";
        try{
            if (value != 0) {
                costString = Integer.toString(value / 100000);
                String afterDot = Integer.toString(value % 100000);
                while (afterDot.length() != 5) {
                    afterDot = "0" + afterDot;
                }
                costString = costString + "." + afterDot.substring(0, 2);

            } else {
                costString = "0.00";
            }

        } catch (Exception e) {
            costString = "0.00";
        }
        return costString + Options.getString(Options.OPTION_CURRENCY);
    }

    // Generates int of money amount spent on connection
    public static int calcCost(int size) {
        int costOf1M = Options.getInt(Options.OPTION_COST_OF_1M) * 100;
        int costPacketLength = Math.max(Options.getInt(Options.OPTION_COST_PACKET_LENGTH), 1);
        long packets = 0;
        if (0 != size) {
            packets = (size + costPacketLength - 1) / costPacketLength;
        }
        return (int)(packets * costPacketLength * costOf1M / (1024 * 1024));
    }
    public int generateCostSum(int in, int out, boolean thisSession) {
        int cost = 0;
        int costOf1M = Options.getInt(Options.OPTION_COST_OF_1M) * 100;
        int costPacketLength = Math.max(Options.getInt(Options.OPTION_COST_PACKET_LENGTH), 1);

        long packets = 0;
        if (0 != in) {
            packets += (in + costPacketLength - 1) / costPacketLength;
        }
        if (0 != out) {
            packets += (out + costPacketLength - 1) / costPacketLength;
        }
        cost += (int)(packets * costPacketLength * costOf1M / (1024 * 1024));
        return cost;
    }

    //Returns value of  traffic
    public int getSessionTraffic() {
        return sessionInTraffic + sessionOutTraffic;
    }

    private void addTraffic(int size) {
        trafficBlockSize += size;
        if (trafficBlockSize > UPDATE_CL_BYTES) {
    	    trafficBlockSize = 0;
            ContactList.getInstance().getManager().updateTitle();
            ContactList.getInstance().getManager().invalidate();
        }
    }

    public void addInTraffic(int bytes) {
        final int size = bytes + TCP_HEADER_SIZE * (bytes + MTU - 1) / MTU;
        sessionInTraffic += size;
        addTraffic(size);
    }
    public void addOutTraffic(int bytes) {
        final int size = bytes + TCP_HEADER_SIZE * (bytes + MTU - 1) / MTU;
        sessionOutTraffic += size;
        addTraffic(size);
    }

    // Reset the saved value
    public void reset() {
        allInTraffic  = 0;
        allOutTraffic = 0;
        savedCost     = 0;
        savedSince.setTime(new Date().getTime());
        try {
            safeSave();
        } catch (Exception e) { // Do nothing
        }
    }
}
// #sijapp cond.end#
