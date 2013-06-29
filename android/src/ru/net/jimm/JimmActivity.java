/**
 *  MicroEmulator
 *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 */

package ru.net.jimm;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.os.*;
import android.view.*;
import jimm.Jimm;
import ui.base.KeyEmulator;
import ui.base.NativeCanvas;
import ui.menu.Select;
import org.microemu.DisplayAccess;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.android.device.AndroidDevice;
import org.microemu.android.device.AndroidInputMethod;
import org.microemu.android.device.ui.*;
import org.microemu.android.util.AndroidRecordStoreManager;
import org.microemu.app.Common;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.ui.CommandUI;

import android.media.AudioManager;
import android.app.NotificationManager;
import org.microemu.android.MicroEmulatorActivity;
import android.content.Intent;
import android.util.Log;
import org.microemu.cldc.file.FileSystem;

public class JimmActivity extends MicroEmulatorActivity {

    public static final String LOG_TAG = "JimmActivity";
    public static String PACKAGE_NAME = null;

    public Common common;

    private boolean isVisible = false;

    private static JimmActivity instance;

    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    private boolean ignoreBackKeyUp = false;

//    private boolean isFirstBack = true;

    public static JimmActivity getInstance() {
        return instance;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        clipboard.setActivity(this);
        externalApi.setActivity(this);
        addActivityResultListener(externalApi);

        instance = this;
        PACKAGE_NAME = getApplicationContext().getPackageName();

        Environment.initLogger();

        MIDletInit();
    }

    @Override
    protected void onPause() {
        isVisible = false;
        super.onPause();
        try {
            if (isFinishing()) {
                Log.i(LOG_TAG, "onPause(); with isFinishing() == true.");
                Log.i(LOG_TAG, "Stopping service...");
                // ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
                // stopService(new Intent(this, JimmService.class));
                return;
            }

            Log.i(LOG_TAG, "onPause(); with isFinishing() == false.");

            MIDletAccess ma = MIDletBridge.getMIDletAccess();
            if (null != ma) {
                ma.pauseApp();
                DisplayAccess da = ma.getDisplayAccess();
                if (null != da) {
                    da.hideNotify();
                }
            }
        } catch (Exception e) {
            error(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;

        new Thread(new Runnable() {

            public void run()
            {
                MIDletStart();
            }

        }).start();
    }

    public final boolean isVisible() {
        if (isVisible) {
            KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            return !keyguardManager.inKeyguardRestrictedInputMode();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (null != Jimm.getJimm()) Jimm.getJimm().quit();
        Log.i(LOG_TAG, "onDestroy();");
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isVisible = true;
    }

    @Override
    protected void onStop() {
        isVisible = false;
        Log.i(LOG_TAG, "onStop();");
        super.onStop();
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getDisplayable() instanceof AndroidCanvasUI) {
            if (ignoreKey(keyCode)) {
                return super.onKeyDown(keyCode, event);
            }
            if (!KeyEmulator.isMain() || (KeyEvent.KEYCODE_BACK != keyCode)) {
                Device device = DeviceFactory.getDevice();
                ((AndroidInputMethod) device.getInputMethod()).buttonPressed(event);
//                isFirstBack = true;
                return true;
            }
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            // Take care of calling this method on earlier versions of 
            // the platform where it doesn't exist. 
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && ignoreBackKeyUp) {
            ignoreBackKeyUp = false;
            return true;
        }
        if (getDisplayable() instanceof AndroidCanvasUI) {
            if (ignoreKey(keyCode)) {
                return super.onKeyUp(keyCode, event);
            }

            if (!KeyEmulator.isMain() || (KeyEvent.KEYCODE_BACK != keyCode)) {
                Device device = DeviceFactory.getDevice();
                ((AndroidInputMethod) device.getInputMethod()).buttonReleased(event);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    private boolean ignoreKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                return true;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BACK:
                return false;
            default:
                return true;
        }
    }

    private AndroidDisplayableUI getDisplayable() {
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return null;
        }
        final DisplayAccess da = ma.getDisplayAccess();
        if (da == null) {
            return null;
        }
        return (AndroidDisplayableUI) da.getDisplayableUI(da.getCurrent());
    }
    private Commands getCommandsUI() {
        AndroidDisplayableUI ui = getDisplayable();
        if (ui == null) {
            return null;
        }
        if (ui instanceof AndroidTextBoxUI) {
            return ((AndroidTextBoxUI) ui).getCommandsUI();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (KeyEmulator.isMain()) {
//            if (isFirstBack) {
//                isFirstBack = false;
//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        isFirstBack = true;
//                    }
//                }, 1000);
//                return;
//            }
            minimizeApp();
            return;
        }

        Commands commands = getCommandsUI();
        if (null == commands) {
            return;
        }
        CommandUI cmd = commands.getBackCommand();
        if (cmd == null) {
            return;
        }
        AndroidDisplayableUI ui = getDisplayable();
        if (ui.getCommandListener() != null) {
            ignoreBackKeyUp = true;
            MIDletBridge.getMIDletAccess().getDisplayAccess().commandAction(cmd.getCommand(), ui.getDisplayable());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Commands commands = getCommandsUI();
        if (null == commands) {
            if (Jimm.getJimm().getDisplay().getCurrentDisplay() instanceof Select) {
                KeyEmulator.emulateKey(NativeCanvas.JIMM_BACK);
            } else {
                KeyEmulator.emulateKey(NativeCanvas.LEFT_SOFT);
            }
            return false;
        }

        menu.clear();
        boolean result = false;

        CommandUI back = commands.getBackCommand();
        for (int i = 0; i < commands.size(); i++) {
            AndroidCommandUI cmd = commands.get(i);
            if (back == cmd) continue;
            result = true;
            SubMenu item = menu.addSubMenu(Menu.NONE, i + MENU_TEXTBOX_FIRST, Menu.NONE, cmd.getCommand().getLabel());
            item.setIcon(cmd.getDrawable());
        }
        return result;
    }

    private static final int MENU_TEXTBOX_FIRST = 10 + Menu.FIRST;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            final DisplayAccess da = MIDletBridge.getMIDletAccess().getDisplayAccess();
            CommandUI c = getCommandsUI().get(item.getItemId() - MENU_TEXTBOX_FIRST);
            da.commandAction(c.getCommand(), da.getCurrent());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void error(Exception e) {
        final StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append(String.format("%s.%s %d\n", ste.getClassName(), ste.getMethodName(), ste.getLineNumber()));
        }
        post(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(JimmActivity.this).create();
                alertDialog.setTitle("Jimm Error");
                alertDialog.setMessage(sb.toString());
                alertDialog.show();
            }
        });
    }


    private void MIDletInit() {
        try {
            common = new Common(emulatorContext);

            MIDletBridge.setMicroEmulator(common);
            common.setRecordStoreManager(new AndroidRecordStoreManager(this));
            common.setDevice(new AndroidDevice(emulatorContext, this));

            Environment.setup(this);

            /* JSR-75 */
            FileSystem fs = new FileSystem();
            fs.registerImplementation();
            common.extensions.add(fs);

            /* JimmInitialization and Service */
            JimmInitialization init = new JimmInitialization();
            init.registerImplementation();
            common.extensions.add(init);

            startService();
            networkStateReceiver.updateNetworkState(this);

            common.initMIDlet();
            new Jimm().initMidlet();
            common.notifyImplementationMIDletStart();
        } catch (Exception e) {
            error(e);
        }
    }

    private void MIDletStart() {
        try {
            MIDletBridge.getMIDletAccess().startApp();
            if (contentView != null) {
                post(new Runnable() {
                    public void run() {
                        contentView.invalidate();
                    }
                });
            }
        } catch (Exception e) {
            error(e);
        }
    }
    private void startService() {
        try {
            startService(new Intent(this, ru.net.jimm.service.JimmService.class));
            registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
            bindService(new Intent(this, ru.net.jimm.service.JimmService.class), service, BIND_AUTO_CREATE);
        } catch (Exception e) {
            error(e);
        }
    }
    public void notifyMIDletDestroyed() {
        try {
            unbindService(service);
        } catch (Exception e) {
            // do nothing
        }
        try {
            unregisterReceiver(networkStateReceiver);
        } catch (Exception e) {
            // do nothing
        }
        stopService(new Intent(this, ru.net.jimm.service.JimmService.class));
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
    }

    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public boolean isNetworkAvailable() {
        return networkStateReceiver.isNetworkAvailable();
    }

    public final ExternalApi externalApi = new ExternalApi();

    public final Clipboard clipboard = new Clipboard();

    public final JimmServiceConnection service = new JimmServiceConnection();
}