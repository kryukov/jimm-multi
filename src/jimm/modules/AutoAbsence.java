/*
 * AutoAbsence.java
 *
 * Created on 4 Июль 2010 г., 22:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_ABSENCE is "true" #
package jimm.modules;

import jimm.Jimm;
import jimm.Options;
import jimm.cl.ContactList;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public final class AutoAbsence {
    public static final AutoAbsence instance = new AutoAbsence();

    public AutoAbsence() {
        absence = false;
        userActivity();
    }

    private Protocol[] protos;
    private Profile[] profiles;
    private long activityOutTime;
    private boolean absence;

    private void doAway() {
        if (absence) {
            return;
        }
        int count = ContactList.getInstance().getManager().getModel().getProtocolCount();
        protos = new Protocol[count];
        profiles = new Profile[count];
        for (int i = 0; i < count; ++i) {
            Protocol p = ContactList.getInstance().getManager().getModel().getProtocol(i);
            if (isSupported(p)) {
                Profile pr = new Profile();
                protos[i] = p;
                profiles[i] = pr;
                pr.statusIndex = p.getProfile().statusIndex;
                pr.statusMessage = p.getProfile().statusMessage;
                pr.xstatusIndex = p.getProfile().xstatusIndex;
                pr.xstatusTitle = p.getProfile().xstatusTitle;
                pr.xstatusDescription = p.getProfile().xstatusDescription;
                // #sijapp cond.if protocols_MRIM is "true" #
                if (protos[i] instanceof protocol.mrim.Mrim) {
                    p.getProfile().xstatusIndex = XStatusInfo.XSTATUS_NONE;
                    p.getProfile().xstatusTitle = "";
                    p.getProfile().xstatusDescription = "";
                }
                // #sijapp cond.end #
                p.setOnlineStatus(StatusInfo.STATUS_AWAY, pr.statusMessage);
            } else {
                protos[i] = null;
            }
        }
        absence = true;
    }
    private boolean isSupported(Protocol p) {
        if ((null == p) || !p.isConnected() || p.getStatusInfo().isAway(p.getProfile().statusIndex)) {
            return false;
        }
        // #sijapp cond.if protocols_MSN is "true" #
        if (p instanceof protocol.msn.Msn) {
            return false;
        }
        // #sijapp cond.end #
        return true;
    }
    private void doRestore() {
        if (!absence || (null == protos)) {
            return;
        }
        absence = false;
        for (int i = 0; i < protos.length; ++i) {
            if (null != protos[i]) {
                Profile pr = profiles[i];
                // #sijapp cond.if protocols_MRIM is "true" #
                if (protos[i] instanceof protocol.mrim.Mrim) {
                    Profile p = protos[i].getProfile();
                    p.xstatusIndex = pr.xstatusIndex;
                    p.xstatusTitle = pr.xstatusTitle;
                    p.xstatusDescription = pr.xstatusDescription;
                }
                // #sijapp cond.end #
                protos[i].setOnlineStatus(pr.statusIndex, pr.statusMessage);
            }
        }
    }

    private boolean isBlockOn() {
        return Options.getBoolean(Options.OPTION_AA_BLOCK);
    }
    public final void updateTime() {
        if (!absence) {
            try {
                if (0 < activityOutTime) {
                    if (activityOutTime < Jimm.getCurrentGmtTime()) {
                        doAway();
                        activityOutTime = -1;
                    }
                } else if (Jimm.isPaused()) {
                    away();
                }
            } catch (Exception e) {
            }
        }
    }
    public final void away() {
        if (isBlockOn()) {
            doAway();
        }
    }
    public final void online() {
        if (isBlockOn()) {
            doRestore();
        }
    }

    public final void updateOptions() {
    }
    public final void userActivity() {
        try {
            if (!Jimm.isLocked() && !Jimm.isPaused()) {
                int init = Options.getInt(Options.OPTION_AA_TIME) * 60; // seconds
                if (0 < init) {
                    activityOutTime = Jimm.getCurrentGmtTime() + init;
                } else {
                    activityOutTime = -1;
                }
                if (absence) {
                    doRestore();
                }
            }
        } catch (Exception e) {
        }
    }
}
// #sijapp cond.end#