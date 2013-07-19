package jimm;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 14.07.13 15:51
 *
 * @author vladimir
 */
public class JimmMidlet extends MIDlet {
    private Jimm jimm;
    private static JimmMidlet midlet;

    public static JimmMidlet getMidlet() {
        return midlet;
    }

    @Override
    protected void startApp() throws MIDletStateChangeException {
        if (null == midlet) {
            midlet = this;
            jimm = Jimm.getJimm();
            jimm.startJimm();
        } else {
            jimm.restoreJimm();
        }
    }

    @Override
    protected void pauseApp() {
        try {
            jimm.hideApp();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        if (null != midlet) {
            midlet = null;
            jimm.destroyJimm();
            notifyDestroyed();
        }
    }
}
