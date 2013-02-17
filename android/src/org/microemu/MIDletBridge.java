/*
 *  MicroEmulator
 *  Copyright (C) 2001-2007 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2007-2007 Vlad Skarzhevskyy
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
 *  Contributor(s):
 *    3GLab
 *    
 *  @version $Id: MIDletBridge.java 2341 2010-03-26 11:30:06Z barteo@gmail.com $    
 */
package org.microemu;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.midlet.MIDlet;

/**
 * 
 * Enables access to MIDlet and MIDletAccess by threadLocal
 *
 */
public class MIDletBridge {

    private static final MIDletContext midletContext = new MIDletContext();
    private static MicroEmulator emulator = null;

    public static void setMicroEmulator(MicroEmulator emulator) {
        MIDletBridge.emulator = emulator;
    }

    public static MicroEmulator getMicroEmulator() {
        return emulator;
    }

    public static void registerMIDletAccess(MIDletAccess accessor) {
        getMIDletContext().setMIDletAccess(accessor);
    }

    public static MIDletContext getMIDletContext() {
        return midletContext;
    }

    public static MIDlet getCurrentMIDlet() {
        return getMIDletContext().getMIDlet();
    }

    public static MIDletAccess getMIDletAccess() {
        return getMIDletContext().getMIDletAccess();
    }

    public static RecordStoreManager getRecordStoreManager() {
        return emulator.getRecordStoreManager();
    }

    public static String getAppProperty(String key) {
        return emulator.getAppProperty(key);
    }

    public static InputStream getResourceAsStream(Class origClass, String name) {
        return emulator.getResourceAsStream(origClass, name);
    }

    public static void notifyDestroyed() {
        emulator.notifyDestroyed(getMIDletContext());
        destroyMIDletContext();
    }

    public static void destroyMIDletContext() {
        emulator.destroyMIDletContext(midletContext);
    }

    public static int checkPermission(String permission) {
        return emulator.checkPermission(permission);
    }

    public static boolean platformRequest(String URL) throws ConnectionNotFoundException {
        return emulator.platformRequest(URL);
    }
}
