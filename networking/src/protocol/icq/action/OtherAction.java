/*
 * OtherAction.java
 *
 * Created on 16 ������ 2007 �., 8:43
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;

import jimm.*;
import protocol.icq.packet.*;

/**
 *
 * @author vladimir
 */
public class OtherAction extends IcqAction {

    /** Creates a new instance of OtherAction */
    private Packet packet;
    public OtherAction(Packet sp) {
        packet = sp;
    }

    public void init() throws JimmException {
        sendPacket(packet);
    }

    public boolean forward(Packet packet) throws JimmException {
        return false;
    }

    public boolean isCompleted() {
        return true;
    }

    public boolean isError() {
        return false;
    }
}
// #sijapp cond.end #
