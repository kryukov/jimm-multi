/*
 * MrimXStatus.java
 *
 * Created on 5 Декабрь 2008 г., 18:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MRIM is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package protocol.mrim;

import jimm.comm.Config;
import jimm.comm.StringUtils;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public class MrimXStatusInfo {
    private final String[] statusCodes;

    public MrimXStatusInfo() {
        statusCodes = new Config().loadLocale("/mrim-xstatus.txt").getKeys();
    }

    public String getNativeXStatus(byte statusIndex) {
        if (0 <= statusIndex && statusIndex < statusCodes.length) {
            return statusCodes[statusIndex];
        }
        return null;
    }

    public int createStatus(String nativeStatus) {
        if (StringUtils.isEmpty(nativeStatus)) {
            return XStatusInfo.XSTATUS_NONE;
        }
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (statusCodes[i].equals(nativeStatus)) {
                return i;
            }
        }
        return XStatusInfo.XSTATUS_NONE;
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
