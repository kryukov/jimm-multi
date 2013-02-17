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
 File: jimmy/ServerHandler.java
 
 Author(s): Zoran Mesec, Matevz Jekovec
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import jimm.comm.StringConvertor;

/**
 * This class is used to connect to a remote server using SocketConnection class.
 * This is the way how all the communication between the mobile phone and IM servers should be done.
 * The class always creates read/write type of the connection automatically.
 *
 * Usage:
 * 1) Class should be created passing the server URL without the "protocol://" prefix and a port which to connect to.
 *    ServerHandler sh_ = new ServerHandler("messenger.hotmail.com","1863");
 * 2) sh_.setTimeout(10000); //set timeout to 10 seconds - we have a really bad connection :)
 * 2) sh_.connect();
 * 3) sendRequest("VER 1 MSNP8 CVR0\r\n")
 * 4) String myReply = getReply();
 *
 * @author Zoran Mesec
 * @author Matevz Jekovec
 */
public class ServerHandler {
    private String url_;	//server URL without the leading "protocol://"
    private int outPort_;	//output port we connect to
    private int inPort_;	//input port the connection is made to
    private SocketConnection sc_;	//main connection
    private DataOutputStream os_;	//output stream linked with sc_
    private DataInputStream is_;	//input stream linked with sc_
    private final int SLEEPTIME_ = 50;	//sleep time per iteration in miliseconds
    private int timeout_ = 5000;	//timeout of getting the reply
    private boolean connected_ = false;	//connection status
    private boolean isNokia_ = false;   //if app runs on a Nokia phone
    
    /**
     * The constructor method.
     *
     * @param url URL of the server without the leading "protocol://" - No protocol type specified here! eg. messenger.hotmail.com
     * @param port Server port which to connect to. eg. 1863 for MSN.
     */
    public ServerHandler(String url, int port) {
        url_ = url;
        outPort_ = port;
        connected_ = false;
        if(System.getProperty("microedition.platform").toLowerCase().indexOf("nokia")!=-1) this.isNokia_=true;
    }
    
    /**
     * The constructor method. If no port is used (ie. use the default port - 80 http), use this constructor method.
     *
     * @param URL URL of the server.
     */
    public ServerHandler(String url) {
        this(url,0);
    }
    
    /**
     * Set the timeout before stop checking the read buffer.
     *
     * @param timeout Timeout value in miliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout_ = timeout;
    }
    
    /**
     * Open the connection to the specified URL and port passed in the constructor using the SocketConnection class.
     *
     * @param useSSL Connect using Secure Socket Layer connection
     */
    public void connect(boolean useSSL) {
        try {
            this.sc_ = (SocketConnection)Connector.open( (useSSL?"ssl://":"socket://") + url_ +
                    ((outPort_ == 0) ? "" : (":" + String.valueOf(outPort_))) );
//        	this.sc_ = (SocketConnection)Connector.open("socket://" + this.url_ + ":" + String.valueOf(this.outPort_));
            this.inPort_ = this.sc_.getLocalPort();
            this.os_ = this.sc_.openDataOutputStream();
            this.is_ = this.sc_.openDataInputStream();
            this.connected_ = true;
        }catch(SecurityException e){
            //in case NO is pressed on overair question
        }catch (IOException e){
            this.connected_ = false;
        }
    }
    /**
     * This method is provided for convenience.
     * It triggers connect(false).
     */
    public void connect() {
        connect(false);
    }
    
    /**
     * Disconnect the SocketConnection and IO streams.
     */
    public void disconnect() {
        try {
            is_.close();
            os_.flush();	//flush the output stream before closing it!
            os_.close();
            sc_.close();
            connected_ = false;
        } catch (IOException ex) {
        }
    }
    
    /**
     * Send a message to the remote server using the OutputStream.
     *
     * @param message Message to be sent using the OutputStream to the remote server using SocketConnection.
     */
    public void sendRequest(String message) {
        sendRequest(StringConvertor.stringToByteArrayUtf8(message));
    }
    public void sendRequest(String message, byte[] body) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        jimm.modules.DebugLog.println("MSN out: " + message + StringConvertor.utf8beByteArrayToString(body, 0, body.length));
        // #sijapp cond.end #
        try {
            os_.write(StringConvertor.stringToByteArrayUtf8(message));
            os_.write(body);
            os_.flush();
            
        } catch (IOException ex) {
        }
    }
    
    /**
     * Send a message to the remote server using the OutputStream.
     *
     * @param message Message to be sent using the OutputStream to the remote server using SocketConnection.
     */
    public void sendRequest(byte[] message) {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        jimm.modules.DebugLog.println("MSN out: " + StringConvertor.utf8beByteArrayToString(message, 0, message.length));
        // #sijapp cond.end #
//    	System.out.println("[DEBUG] OUT:\n" + Utils.byteArrayToHexString(message));
        
        try {
            os_.write(message);
            os_.flush();
            
        } catch (IOException ex) {
        }
    }
    
    /**
     * Read a message from the remote server reading the waiting buffer. If waiting buffer is empty, wait until it gets filled or if timeout occurs.
     * This method reads and empties the WHOLE buffer (ie. doesn't stop at new line)!
     *
     * @return Message from the remote server as a String in UTF-8 encoding, null if timeout has occured.
     */
    public String getReply() {
        return getReply(null);
    }
    
    /**
     * Read a message from the remote server reading the waiting buffer. If waiting buffer is empty, wait until it gets filled or if timeout occurs.
     * This method reads and empties the WHOLE buffer (ie. doesn't stop at new line)!
     *
     * @param enc Return String in the given encoding. See http://java.sun.com/j2se/1.5.0/docs/api/java/nio/charset/Charset.html for additional information on String encodings.
     * @return Message from the remote server as a String in UTF-8 encoding, null if timeout has occured.
     */
    public String getReply(String enc) {
        if (enc == null) enc = "UTF-8";
        byte[] buffer = getReplyLine();
        if ((null != buffer) && (0 != buffer.length)) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.println("MSN in: " + StringConvertor.utf8beByteArrayToString(buffer, 0, buffer.length));
            // #sijapp cond.end #
            return StringConvertor.utf8beByteArrayToString(buffer, 0, buffer.length);
        }
        return null;
    }
    public boolean available() {
        try {
            return 0 < is_.available();
        } catch (IOException ex) {
            connected_ = false;
            return false;
        }
    }
    public byte[] getReplyBytes(int count) {
        byte[] data = new byte[count];
        try {
            int readed = 0;
            while (true) {
                int read = is_.read(data, readed, count - readed);
                if (read < 0) break;
                readed += read;
                if (readed == count) break;
                if (!available()) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                    }
                }
            }
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.println("___ in: " + StringConvertor.utf8beByteArrayToString(data, 0, data.length));
            // #sijapp cond.end #
        } catch (Exception ex) {
            connected_ = false;
            return null;
        }
        return data;
    }
    /**
     * Read a message from the remote server reading the waiting buffer. If waiting buffer is empty, wait until it gets filled or if timeout occurs.
     * This method reads and empties the WHOLE buffer (ie. doesn't stop at new line)!
     *
     * @return Message from the remote server as a byte[]. null if timeout has occured
     */
    public byte[] getReplyLine() {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            while (0 == is_.available()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            while (true) {
                int read = is_.available();
                while (0 < read--) {
                    int ch = is_.read();
                    if (read < 0) return null;
                    if ('\r' == ch) continue;
                    if ('\n' == ch) {
                        return (0 == result.size()) ? null : result.toByteArray();
                    }
                    result.write(ch);
                }
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                }
            }
        }catch (InterruptedIOException ex){
//        		ex.printStackTrace();
            connected_ = false;
            return null;
        }catch (IOException ex){
            connected_ = false;
        }
        return null;
    }
    
    /**
     * Returns the created input (local) port when an outgoing connection was established.
     *
     * @return Incoming port
     */
    public int getInputPort() {return inPort_;}
    
    /**
     * Returns the connection outgoing port passed in the constructor.
     *
     * @return Outgoing port
     */
    public int getOutputPort() {return outPort_;}
    
    /**
     * Returns true if the connection is established, false otherwise.
     */
    public boolean isConnected() {return connected_;}
    
    /**
     * Sets a new url to connect to
     * @param u the url String
     */
    public void setURL(String u) {this.url_ = u;}
    
    /**
     * Sets tne new port to connect to
     * @param p the port int number
     */
    public void setPort(int p) {this.outPort_ = p;}
}

// #sijapp cond.end #