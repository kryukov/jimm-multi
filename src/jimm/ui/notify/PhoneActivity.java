/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
// #sijapp cond.if modules_ACTIVITYUI is "true"#
package jimm.ui.notify;

import javax.microedition.lcdui.Image;

/**
 *
 * @author Vladimir Kruykov
 */
public interface PhoneActivity {
    PhoneActivity init();
    void addEvent(String title, String desc, Image icon);
}
// #sijapp cond.end#
