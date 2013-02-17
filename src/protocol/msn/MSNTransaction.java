/* JIMMY - Instant Mobile Messenger
   Copyright (C) 2006  JIMMY Project
 
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
 **********************************************************************
 File: jimmy/MSNTransaction.java
 
 Author(s): Zoran Mesec, Matevz Jekovec
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import java.util.Vector;

/**
 * This class represents a single request (line) to be sent to the MSN server.
 *
 * Because the statements are usually long and can get complicated, MSNTransaction is a bridge between MSNProtocol and raw ServerHandler.sendRequest().
 * MSNTransaction automatically stores the serial number of the transaction in trID_.
 * Typical use:
 * 1) MSNTransaction myTransaction = new MSNTransaction();
 * 2) myTransaction.newTransaction();
 * 3) myTransaction.setType("VER");
 * 4) myTransaction.addElement("MSNP11"); myTransaction.addElement("CVR0");
 * 5) serverHandler.sendRequest(myTransaction.toString());
 *
 * This will send "VER 1 MSNP8 CVR0\r\n" to the server.
 *
 * @author Zoran Mesec
 * @author Matevz Jekovec
 */
public class MSNTransaction {
    private String messageType_;	//the first 3 characters which mark the message type
    private int trID_;   //serial number of the transaction
    private Vector arguments_;	//the arguments list
    private static final String NEWLINE_ = "\r\n";	//newline definition
    
    /**
     * Creates a new instance of MSNTransaction.
     * Transaction ID is by default 0! Call newTransaction() before making a transaction.
     */
    public MSNTransaction() {
        this.trID_ = 0;
        this.arguments_ = new Vector();
    }
    
    /**
     * Sets the type - the first three characters of the transaction (eg. VER, USR, CHG etc.)
     *
     * @param type 3 characters long message type
     */
    public void setType(String type) {
        this.messageType_ = type;
    }
    
    /**
     * Adds another argument to the message and automatically append " ".
     *
     * @param arg New argument
     */
    public void addArgument(String arg) {
        this.arguments_.addElement(arg);
    }
    
    /**
     * Blanks the arguments list and message type. Increase the serial number of the transaction by 1.
     * Call this method when sending a new request to the server.
     */
    public void newTransaction() {
        this.trID_++;
        this.arguments_.removeAllElements();
        this.messageType_ = null;
    }
    
    /**
     * Concatenates the message type, serial number, a list of arguments separated by a blank space and adds new line characters.
     * @return Message in String ready to be sent directly to MSN server.
     */
    public String toString() {
        StringBuffer rMessage = new StringBuffer();
        rMessage.append(this.messageType_);
        rMessage.append(" ").append(this.trID_);
        
        for(int i=0; i<this.arguments_.size(); ++i) {
            rMessage.append(" ").append(this.arguments_.elementAt(i));
        }
        rMessage.append(NEWLINE_);
        
        return rMessage.toString();
    }
    
    /**
     * Concatenates the message type, serial number, a list of arguments separated by a blank space.
     * @return Message in String ready to be sent directly to MSN server.
     */
    public String toStringNN() {
        StringBuffer rMessage = new StringBuffer();
        rMessage.append(this.messageType_);
        rMessage.append(" ").append(this.trID_);
        
        for(int i=0; i<this.arguments_.size(); ++i) {
            rMessage.append(" ").append(this.arguments_.elementAt(i));
        }
        //rMessage.append(NEWLINE_);
        
        return rMessage.toString();
    }
    
    /**
     * Logout is an exception among all the requests. It doesn't need the transaction ID beside.
     * @return Logout request ready to be sent directly to the MSN Server.
     */
    public String getLogoutString() {
        return "OUT" + NEWLINE_;
    }
    
    /**
     * MSNTransaction stores the current transaction ID locally. If you need it in any case externally, call this function.
     * @return Current transaction ID
     */
    public int getTransactionID() {
        return this.trID_;
    }
}
// #sijapp cond.end #