package jimmui.view.base;

import jimmui.view.roster.VirtualContactList;
import jimm.Jimm;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 31.08.12 17:37
 *
 * @author vladimir
 */
public class KeyEmulator {
    public static void emulateKey(int jimmKey) {
        Jimm.getJimm().getDisplay().getNativeCanvas().emulateKey(null, jimmKey);
    }
    public static boolean isMain() {
        Object current = Jimm.getJimm().getDisplay().getCurrentDisplay();

        if (current instanceof VirtualContactList) {
            return !Jimm.getJimm().getCL().isChats((CanvasEx) current);
        }
        return false;
    }
}
