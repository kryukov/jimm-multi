/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
// #sijapp cond.if modules_ACTIVITYUI is "true"#
package jimm.ui.notify;

import DrawControls.icons.Icon;
import jimm.Jimm;

/**
 *
 * @author vladimir
 */
public class ActivityUI {
    private PhoneActivity activity;

    public ActivityUI() {
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            activity = new SeActivity().init();
        }
    }
    public void addEvent(String title, String desc, Icon icon) {
        if (null != activity) {
            activity.addEvent(title, desc, null);
        }
    }

}
// #sijapp cond.end#
