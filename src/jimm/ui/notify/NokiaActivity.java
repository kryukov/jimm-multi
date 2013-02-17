// #sijapp cond.if modules_ACTIVITYUI_off is "true"#
package jimm.ui.notify;

import com.nokia.mid.ui.*;
import javax.microedition.lcdui.Image;
import jimm.Jimm;
import jimm.util.JLocale;

/**
 *
 * @author Vladimir Kryukov
 */
public class NokiaActivity implements SoftNotificationListener, PhoneActivity {

    private SoftNotification iSoftNotification;

    public NokiaActivity() {
    }
    public PhoneActivity init() {
        try {
            iSoftNotification = SoftNotification.newInstance();
            iSoftNotification.setListener(this);
            return this;
        } catch (Exception e) {
            return null;
        }
    }

    public void addEvent(String title, String desc, Image icon) {
        try {
            iSoftNotification.setText(title, desc);
            iSoftNotification.setSoftkeyLabels(JLocale.getString("ok"),
                    JLocale.getString("back"));
            iSoftNotification.post();
        } catch (Exception e) {
        }
    }

    public void notificationSelected(SoftNotification notification) {
        //Handle the event...
        Jimm.maximize();
    }

    public void notificationDismissed(SoftNotification notification) {
        try {
            iSoftNotification.remove();
        } catch (Exception e) {
        }
    }
}
// #sijapp cond.end#
