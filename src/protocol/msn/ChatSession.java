/*
 * ChatSession.java
 *
 * Created on 2 Март 2010 г., 22:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import java.util.Vector;
import jimm.chat.message.PlainMessage;

/**
 *
 * @author Vladimir Krukov
 */
public class ChatSession {
    public MsnContact c;
    public int id;
    public ServerHandler sh;

    public int initId;
    public PlainMessage msg = null;
    public final Vector outgoingPackets = new Vector();
    /** Creates a new instance of ChatSession */
    public ChatSession(MsnContact c) {
        this.c = c;
    }
    
}
// #sijapp cond.end #