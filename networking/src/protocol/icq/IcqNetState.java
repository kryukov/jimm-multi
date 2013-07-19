/*
 * IcqNetState.java
 *
 * Created on 29 Февраль 2008 г., 19:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq;

import java.util.Vector;
import jimm.*;
import protocol.icq.action.*;
import protocol.icq.packet.*;
import jimm.modules.*;

/**
 *
 * @author vladimir
 */
class IcqNetState {

    private final Vector actActions = new Vector();
    private final Vector reqAction = new Vector();
    private IcqNetDefActions defActionListener;
    private IcqNetWorking connection;

    public IcqNetState() {
    }

    public void login(IcqNetWorking con) {
        connection = con;
        defActionListener = new IcqNetDefActions(connection);
    }

    public void disconnect() {
        connection = null;
    }

    // Request an action
    public void requestAction(IcqAction act) {
        // Look whether action is executable at the moment
        act.setConnection(connection);
        synchronized (reqAction) {
            cleanActions();
            reqAction.addElement(act);
        }
    }

    void processPacket(Packet packet) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null == packet) {
            DebugLog.println("packet is null");
            return;
        }
        // #sijapp cond.end #
        // Forward received packet to all active actions and to the
        // action listener
        for (int i = 0; i < actActions.size(); ++i) {
                IcqAction act = (IcqAction) actActions.elementAt(i);
                if (act.isError() || act.isCompleted()) {
                    continue;
                }
                try {
                    if (act.forward(packet)) {
                        if (act.isCompleted() || act.isError()) {
                            actActions.removeElement(act);
                        }
                        return;
                    }
                } catch (JimmException e) {
                    throw e;
                } catch (Exception e) {
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    DebugLog.panic("Icq action error", e);
                    if (packet instanceof SnacPacket) {
                        SnacPacket snacPacket = (SnacPacket) packet;
                        DebugLog.println(actActions.elementAt(i).getClass().toString());
                        DebugLog.println("family = 0x" + Integer.toHexString(snacPacket.getFamily())
                                + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));
                    }
                    // #sijapp cond.end #
                }
        }
        try {
            defActionListener.forward(packet);
        } catch (JimmException e) {
            throw e;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Icq listener error", e);
            if (packet instanceof SnacPacket) {
                SnacPacket snacPacket = (SnacPacket) packet;
                DebugLog.println("family = 0x" + Integer.toHexString(snacPacket.getFamily())
                        + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));
            }
            // #sijapp cond.end #
        }

    }

    private IcqAction getNewAction() {
        IcqAction newAction = null;
        synchronized (reqAction) {
            if (0 < reqAction.size()) {
                newAction = (IcqAction) reqAction.elementAt(0);
                reqAction.removeElementAt(0);
            }
        }
        return newAction;
    }
    public boolean processActions() {
        IcqAction newAction = getNewAction();
        if (null == newAction) {
            return false;
        }

        // Initialize action
        try {
            newAction.init();
        } catch (JimmException e) {
            //TODO: Is it not critical exception?
            connection.getIcq().processException(e);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("newAction.init()", e);
            // #sijapp cond.end #
        }
        if (!newAction.isCompleted() && !newAction.isError()) {
            actActions.addElement(newAction);
        }
        return true;
    }
    private boolean cleanActions() {
        // Remove completed actions
        for (int i = actActions.size() - 1; i >= 0; --i) {
            IcqAction act = (IcqAction)actActions.elementAt(i);
            if (act.isCompleted() || act.isError()) {
                actActions.removeElementAt(i);
            }
        }
        return false;
    }
}
// #sijapp cond.end #
