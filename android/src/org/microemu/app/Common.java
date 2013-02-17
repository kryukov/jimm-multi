/**
 *  MicroEmulator
 *  Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@barteo.net>
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
 *  @version $Id: Common.java 2517 2011-11-10 12:30:37Z barteo@gmail.com $
 */
package org.microemu.app;

import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;

import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.MIDletContext;
import org.microemu.MicroEmulator;
import org.microemu.RecordStoreManager;
import org.microemu.app.util.MIDletSystemProperties;
import org.microemu.app.util.MIDletThread;
import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.EmulatorContext;
import org.microemu.log.Logger;
import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.ImplementationInitialization;
import org.microemu.microedition.io.ConnectorImpl;

public class Common implements MicroEmulator {

    protected EmulatorContext emulatorContext;

    private RecordStoreManager recordStoreManager;

    public Vector<ImplementationInitialization> extensions = new Vector<ImplementationInitialization>();
    
    private final Object destroyNotify = new Object();

    public Common(EmulatorContext context) {
        this.emulatorContext = context;

        /*
         * Initialize secutity context for implemenations, May be there are better place
         * for this call
         */
        ImplFactory.instance();
        MIDletSystemProperties.initContext();
        // TODO integrate with ImplementationInitialization
        ImplFactory.registerGCF(ImplFactory.DEFAULT, new ConnectorImpl());
    }

    public RecordStoreManager getRecordStoreManager() {
        return recordStoreManager;
    }

    public void setRecordStoreManager(RecordStoreManager manager) {
        this.recordStoreManager = manager;
    }

    public String getAppProperty(String key) {
        if (key.equals("microedition.platform")) {
            return "Android";
        } else if (key.equals("microedition.profiles")) {
            return "MIDP-2.0";
        } else if (key.equals("microedition.configuration")) {
            return "CLDC-1.0";
        } else if (key.equals("microedition.locale")) {
            return Locale.getDefault().getLanguage();
        } else if (key.equals("microedition.encoding")) {
            return System.getProperty("file.encoding");
        }
        return null;
    }

    public InputStream getResourceAsStream(Class origClass, String name) {
        return emulatorContext.getResourceAsStream(origClass, name);
    }

    public void notifyDestroyed(MIDletContext midletContext) {
        Logger.debug("notifyDestroyed");
        notifyImplementationMIDletDestroyed();
        startLauncher(midletContext);
    }

    public void destroyMIDletContext(MIDletContext midletContext) {
        if ((midletContext != null) && (MIDletBridge.getMIDletContext() == midletContext)) {
            Logger.debug("destroyMIDletContext");
        }
        MIDletThread.contextDestroyed(midletContext);
        synchronized (destroyNotify) {
            destroyNotify.notifyAll();
        }
    }

    protected void startLauncher(MIDletContext midletContext) {
       if (midletContext != null) {
            try {
                MIDletAccess previousMidletAccess = midletContext.getMIDletAccess();
                if (previousMidletAccess != null) {
                    previousMidletAccess.destroyApp(true);
                }
            } catch (Throwable e) {
                Logger.error("destroyApp error", e);
            }

            System.exit(0);            
        }        
    }

    public int checkPermission(String permission) {
        return MIDletSystemProperties.getPermission(permission);
    }

    public boolean platformRequest(final String URL) throws ConnectionNotFoundException {
        return emulatorContext.platformRequest(URL);
    }

    public Device getDevice() {
        return DeviceFactory.getDevice();
    }

    public void setDevice(Device device) {
        MIDletSystemProperties.setDevice(device);
        DeviceFactory.setDevice(device);
    }


    public void notifyImplementationMIDletStart() {
        for (ImplementationInitialization o : extensions) {
            o.notifyMIDletStart();
        }
    }

    public void notifyImplementationMIDletDestroyed() {
        for (ImplementationInitialization o : extensions) {
            o.notifyMIDletDestroyed();
        }
    }

    public void initMIDlet() {
        MIDletBridge.getRecordStoreManager().init(MIDletBridge.getMicroEmulator());
    }
}
