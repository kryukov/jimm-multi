package jimm.ui.base;

import DrawControls.tree.VirtualContactList;
import jimm.Jimm;
import jimm.cl.ContactList;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 31.08.12 17:37
 *
 * @author vladimir
 */
public class KeyEmulator {
    public static void emulateKey(int jimmKey) {
        NativeCanvas.getInstance().emulateKey(null, jimmKey);
    }
    public static boolean isMain() {
        Object current = Jimm.getJimm().getDisplay().getCurrentDisplay();
        return (current instanceof VirtualContactList);
    }
}
