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

import java.io.*;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import jimm.FileTransfer;
import jimm.Jimm;
import jimm.modules.DebugLog;
import jimm.modules.photo.PhotoListener;
import jimm.ui.base.KeyEmulator;
import jimm.ui.base.NativeCanvas;
import jimm.ui.menu.Select;
import org.microemu.DisplayAccess;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.android.device.AndroidDevice;
import org.microemu.android.device.AndroidInputMethod;
import org.microemu.android.device.ui.*;
import org.microemu.android.util.AndroidLoggerAppender;
import org.microemu.android.util.AndroidRecordStoreManager;
import org.microemu.app.Common;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.ui.CommandUI;
import org.microemu.log.Logger;

import android.media.AudioManager;
import android.app.NotificationManager;
import org.microemu.android.MicroEmulatorActivity;
import android.content.Intent;
import android.util.Log;
import org.microemu.cldc.file.FileSystem;
import ru.net.jimm.photo.CameraActivity;

public class JimmActivity extends MicroEmulatorActivity {

    public static final String LOG_TAG = "JimmActivity";
    public static String PACKAGE_NAME = null;

    public Common common;

    private boolean isVisible = false;

    private static JimmActivity instance;

    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

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

        instance = this;
        PACKAGE_NAME = getApplicationContext().getPackageName();

        Logger.removeAllAppenders();
        Logger.setLocationEnabled(false);
        Logger.addAppender(new AndroidLoggerAppender());

        System.setOut(new PrintStream(new OutputStream() {

            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }

        }));

        System.setErr(new PrintStream(new OutputStream() {

            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }

        }));

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
        Jimm.getJimm().quit();
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

    private boolean ignoreBackKeyUp = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
        if (ui instanceof AndroidCanvasUI) {
            if (ignoreKey(keyCode)) {
                return super.onKeyDown(keyCode, event);
            }
            DebugLog.println("commands keyCode " + keyCode);
            if (!KeyEmulator.isMain() || (KeyEvent.KEYCODE_BACK != keyCode)) {
                Device device = DeviceFactory.getDevice();
                ((AndroidInputMethod) device.getInputMethod()).buttonPressed(event);
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

        if (ui instanceof AndroidCanvasUI) {
            if (ignoreKey(keyCode)) {
                return super.onKeyUp(keyCode, event);
            }

            Device device = DeviceFactory.getDevice();
            ((AndroidInputMethod) device.getInputMethod()).buttonReleased(event);

            return true;
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

    private final static KeyEvent KEY_RIGHT_DOWN_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);

    private final static KeyEvent KEY_RIGHT_UP_EVENT = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT);

    private final static KeyEvent KEY_LEFT_DOWN_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);

    private final static KeyEvent KEY_LEFT_UP_EVENT = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT);

    private final static KeyEvent KEY_DOWN_DOWN_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);

    private final static KeyEvent KEY_DOWN_UP_EVENT = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);

    private final static KeyEvent KEY_UP_DOWN_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);

    private final static KeyEvent KEY_UP_UP_EVENT = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);

    private final static float TRACKBALL_THRESHOLD = 1.0f;

    private float accumulatedTrackballX = 0;

    private float accumulatedTrackballY = 0;

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
         if (event.getAction() == MotionEvent.ACTION_MOVE) {
                 MIDletAccess ma = MIDletBridge.getMIDletAccess();
                 if (ma == null) {
                         return false;
                 }
                 final DisplayAccess da = ma.getDisplayAccess();
                 if (da == null) {
                         return false;
                 }
                 AndroidDisplayableUI ui = (AndroidDisplayableUI) da.getDisplayableUI(da.getCurrent());
            if (ui instanceof AndroidCanvasUI) {
                float x = event.getX();
                float y = event.getY();
                if ((x > 0 && accumulatedTrackballX < 0) || (x < 0 && accumulatedTrackballX > 0)) {
                    accumulatedTrackballX = 0;
                }
                if ((y > 0 && accumulatedTrackballY < 0) || (y < 0 && accumulatedTrackballY > 0)) {
                    accumulatedTrackballY = 0;
                }
                if (accumulatedTrackballX + x > TRACKBALL_THRESHOLD) {
                    accumulatedTrackballX -= TRACKBALL_THRESHOLD;
                    KEY_RIGHT_DOWN_EVENT.dispatch(this);
                    KEY_RIGHT_UP_EVENT.dispatch(this);
                } else if (accumulatedTrackballX + x < -TRACKBALL_THRESHOLD) {
                    accumulatedTrackballX += TRACKBALL_THRESHOLD;
                    KEY_LEFT_DOWN_EVENT.dispatch(this);
                    KEY_LEFT_UP_EVENT.dispatch(this);
                }
                if (accumulatedTrackballY + y > TRACKBALL_THRESHOLD) {
                    accumulatedTrackballY -= TRACKBALL_THRESHOLD;
                    KEY_DOWN_DOWN_EVENT.dispatch(this);
                    KEY_DOWN_UP_EVENT.dispatch(this);
                } else if (accumulatedTrackballY + y < -TRACKBALL_THRESHOLD) {
                    accumulatedTrackballY += TRACKBALL_THRESHOLD;
                    KEY_UP_DOWN_EVENT.dispatch(this);
                    KEY_UP_UP_EVENT.dispatch(this);
                }
                accumulatedTrackballX += x;
                accumulatedTrackballY += y;

                return true;
            }
        }

        return super.onTrackballEvent(event);
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
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return null;
        }
        final DisplayAccess da = ma.getDisplayAccess();
        if (da == null) {
            return null;
        }
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

            System.setProperty("microedition.platform", "microemu-android");
            System.setProperty("microedition.configuration", "CLDC-1.1");
            System.setProperty("microedition.profiles", "MIDP-2.0");
            System.setProperty("microedition.locale", Locale.getDefault().toString());
            System.setProperty("device.manufacturer", android.os.Build.BRAND);
            System.setProperty("device.model", android.os.Build.MODEL);
            System.setProperty("device.software.version", android.os.Build.VERSION.RELEASE);

            // photo
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                System.setProperty("video.snapshot.encodings", "yes");
            }

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
            startService(new Intent(this, JimmService.class));
            registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
            bindService(new Intent(this, JimmService.class), serviceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            error(e);
        }
    }
    public void notifyMIDletDestroyed() {
        try {
            unbindService(serviceConnection);
        } catch (Exception e) {
            // do nothing
        }
        try {
            unregisterReceiver(networkStateReceiver);
        } catch (Exception e) {
            // do nothing
        }
        stopService(new Intent(this, JimmService.class));
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


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        externalApi.onActivityResult(requestCode, resultCode, data);
    }

    public void updateAppIcon() {
        serviceConnection.send(Message.obtain(null, JimmService.UPDATE_APP_ICON));
    }

    public final ExternalApi externalApi = new ExternalApi();

    public final Clipboard clipboard = new Clipboard();

    private final JimmServiceConnection serviceConnection = new JimmServiceConnection();
}