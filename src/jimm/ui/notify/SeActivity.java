// #sijapp cond.if modules_ACTIVITYUI is "true"#
package jimm.ui.notify;

import javax.microedition.lcdui.Image;

import jimm.Jimm;

/**
 *
 * @author Rad1st
 */
public class SeActivity implements com.sonyericsson.ui.UIEventListener, PhoneActivity {
    public SeActivity() {
    }

    public PhoneActivity init() {
        try {
            Class.forName("com.sonyericsson.ui.UIActivityMenu");
            com.sonyericsson.ui.UIActivityMenu.getInstance(Jimm.getJimm())
                    .setEventListener(this);
            return this;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Handles callbacks when the user selects the event from the list in the Activity Menu
     */
    public void eventAction(int eventId) {
        //Handle the event...
        Jimm.maximize();
    }

    /**
     * Pushes a new event to the Activity Menu
     */
    public void addEvent(String title, String desc, Image icon) {
        com.sonyericsson.ui.UIActivityMenu.getInstance(Jimm.getJimm())
                .addEvent(title, desc, icon, null);
    }
}
// #sijapp cond.end#

