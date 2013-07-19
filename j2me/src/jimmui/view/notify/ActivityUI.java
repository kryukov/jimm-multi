/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
// #sijapp cond.if modules_ACTIVITYUI is "true"#
package jimmui.view.notify;

import jimmui.view.icons.Icon;
import jimm.Jimm;

/**
 *
 * @author vladimir
 */
public class ActivityUI {
    private PhoneActivity activity;

    public ActivityUI() {
        if (Jimm.getJimm().phone.isPhone(PhoneInfo.PHONE_SE)) {
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
