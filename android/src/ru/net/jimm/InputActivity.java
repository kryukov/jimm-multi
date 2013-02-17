package ru.net.jimm;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.SubMenu;
import jimm.ui.base.KeyEmulator;
import org.microemu.DisplayAccess;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.android.device.ui.AndroidCommandUI;
import org.microemu.android.device.ui.AndroidDisplayableUI;
import org.microemu.device.ui.CommandUI;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 31.08.12 17:17
 *
 * @author vladimir
 */
public class InputActivity extends Activity {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return false;
        }
        final DisplayAccess da = ma.getDisplayAccess();
        if (da == null) {
            return false;
        }
        AndroidDisplayableUI ui = (AndroidDisplayableUI) da.getDisplayableUI(da.getCurrent());
        if (ui == null) {
            return false;
        }

        menu.clear();
        boolean result = false;

        CommandUI back = ui.getCommandsUI().getBackCommand();
        for (int i = 0; i < ui.getCommandsUI().size(); i++) {
            AndroidCommandUI cmd = ui.getCommandsUI().get(i);
            if (back == cmd) continue;
            result = true;
            SubMenu item = menu.addSubMenu(Menu.NONE, i + Menu.FIRST, Menu.NONE, cmd.getCommand().getLabel());
            item.setIcon(cmd.getDrawable());
        }
        return result;
    }

}
