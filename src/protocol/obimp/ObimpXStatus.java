/*
 * ObimpXStatus.java
 *
 * Created on 13 Январь 2011 г., 20:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_OBIMP is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package protocol.obimp;

import DrawControls.icons.ImageList;
import jimm.comm.Config;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ObimpXStatus {
    private final XStatusInfo info;
    public ObimpXStatus() {
        Config cfg = new Config().loadLocale("/obimp-xstatus.txt");
        String[] xstatusNames = cfg.getValues();
        ImageList xstatusIcons = ImageList.createImageList("/obimp-xstatus.png");
        info = new XStatusInfo(xstatusIcons, xstatusNames);
    }
    public XStatusInfo getInfo() {
        return info;
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
