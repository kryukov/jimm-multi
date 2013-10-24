/**
 *  MicroEmulator
 *  Copyright (C) 2009 Bartek Teodorczyk <barteo@barteo.net>
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
 *  @version $Id: MicroEmulatorActivity.java 1918 2009-01-21 12:56:43Z barteo $
 */

package org.microemu.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;

import android.view.KeyEvent;
import android.view.MotionEvent;
import jimm.modules.DebugLog;
import org.microemu.DisplayAccess;
import org.microemu.DisplayComponent;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.android.device.AndroidDeviceDisplay;
import org.microemu.android.device.AndroidFontManager;
import org.microemu.android.device.AndroidInputMethod;
import org.microemu.android.device.ui.AndroidCanvasUI;
import org.microemu.android.device.ui.AndroidDisplayableUI;
import org.microemu.android.device.ui.CanvasView;
import org.microemu.android.util.ActivityResultListener;
import org.microemu.device.DeviceDisplay;
import org.microemu.device.DeviceFactory;
import org.microemu.device.EmulatorContext;
import org.microemu.device.FontManager;
import org.microemu.device.InputMethod;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;

public abstract class MicroEmulatorActivity extends Activity {
		
	public static AndroidConfig config = new AndroidConfig();
	
	public boolean windowFullscreen;

	private Handler handler = new Handler();
	
	private Thread activityThread;
	
	protected View contentView;

	private Dialog dialog;
	
	private ArrayList<ActivityResultListener> activityResultListeners = new ArrayList<ActivityResultListener>();
	
	protected EmulatorContext emulatorContext;
	
	public void setConfig(AndroidConfig config) {
		MicroEmulatorActivity.config = config;
	}
    
    public EmulatorContext getEmulatorContext() {
        return emulatorContext;
    }

	public boolean post(final Runnable r) {
        Runnable nn = new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception e) {
                    DebugLog.panic("err ", e);
                }
            }
        };
		if (activityThread == Thread.currentThread()) {
			nn.run();
			return true;
		} else {
			return handler.post(nn);
		}
	}
	
	public boolean postDelayed(Runnable r, long delayMillis) {
		if (activityThread == Thread.currentThread()) {
			r.run();
			return true;
		} else {
			return handler.postDelayed(r, delayMillis);
		}
	}
	
	public boolean isActivityThread() {
		return (activityThread == Thread.currentThread());
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
        emulatorContext = new EmulatorContext() {

            private InputMethod inputMethod = new AndroidInputMethod();

            private DeviceDisplay deviceDisplay = new AndroidDeviceDisplay(MicroEmulatorActivity.this, this, 200, 200);
            
            private FontManager fontManager = new AndroidFontManager(getResources().getDisplayMetrics());

            public DisplayComponent getDisplayComponent() {
                // TODO consider removal of EmulatorContext.getDisplayComponent()
                System.out.println("MicroEmulator.emulatorContext::getDisplayComponent()");
                return null;
            }

            public InputMethod getDeviceInputMethod() {
                return inputMethod;
            }

            public DeviceDisplay getDeviceDisplay() {
                return deviceDisplay;
            }

            public FontManager getDeviceFontManager() {
                return fontManager;
            }

            public InputStream getResourceAsStream(Class origClass, String name) {
                try {
                    if (name.startsWith("/")) {
                        return MicroEmulatorActivity.this.getAssets().open(name.substring(1));
                    } else {
                        Package p = origClass.getPackage();
                        if (p == null) {
                            return MicroEmulatorActivity.this.getAssets().open(name);
                        } else {
                        	String folder = origClass.getPackage().getName().replace('.', '/');
                            return MicroEmulatorActivity.this.getAssets().open(folder + "/" + name);
                        }
                    }
                } catch (IOException e) {
                    //Logger.debug(e); // large output with BombusMod
                    return null;
                }
            }

            public boolean platformRequest(String url) throws ConnectionNotFoundException 
            {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {
                    throw new ConnectionNotFoundException();
                }

                return true;
            }
                    
        };
		
		activityThread = Thread.currentThread();
	}
	
	@Override
	public void setContentView(View view) {
Log.d("AndroidCanvasUI", "set content view: " + view);
		super.setContentView(view);
		contentView = view;
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		MIDletAccess ma = MIDletBridge.getMIDletAccess();
		if (ma == null) {
			return;
		}
		DisplayAccess da = ma.getDisplayAccess();
        if (da != null) {
            Displayable d = da.getCurrent();
            if (d instanceof Canvas) {
                CanvasView c = ((AndroidCanvasUI)d.getUi()).getCanvasView();
                AndroidDeviceDisplay deviceDisplay = (AndroidDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay();
                deviceDisplay.displayRectangleWidth = c.getWidth();
                deviceDisplay.displayRectangleHeight = c.getHeight();
                da.sizeChanged();
                deviceDisplay.repaint(0, 0, deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
            }
		}
	}
	
	public void addActivityResultListener(ActivityResultListener listener) {
		activityResultListeners.add(listener);
	}
	
	public void removeActivityResultListener(ActivityResultListener listener) {
		activityResultListeners.remove(listener);
	}
	
	@Override
	protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (ActivityResultListener activityResultListener : activityResultListeners) {
            if (activityResultListener.onActivityResult(requestCode, resultCode, data)) {
                return;
            }
        }
		
		super.onActivityResult(requestCode, resultCode, data);
	}

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

}
