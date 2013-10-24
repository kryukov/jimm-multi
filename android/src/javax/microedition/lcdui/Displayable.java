/*
 *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
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
 */

package javax.microedition.lcdui;

import org.microemu.device.Device;
import org.microemu.device.DeviceFactory;
import org.microemu.device.ui.DisplayableUI;


public abstract class Displayable {
    Device device;

    Display currentDisplay = null;

    int width;

    int height;

    boolean fullScreenMode;

    Ticker ticker;

    private DisplayableUI ui;

    private String title;

    private CommandListener listener = null;


    Displayable(String title) {
        this.device = DeviceFactory.getDevice();
        this.width = -1;
        this.height = -1;
        this.fullScreenMode = false;
        this.title = title;
    }


    public int getWidth() {
        if (width == -1) {
            updateWidthAndHeight();
        }

        return width;
    }


    public int getHeight() {
        if (height == -1) {
            updateWidthAndHeight();
        }

        return height;
    }


    public boolean isShown() {
        return (null != currentDisplay) && currentDisplay.isShown(this);
    }


    public Ticker getTicker() {
        return ticker;
    }


    public void setTicker(Ticker ticker) {
        this.ticker = ticker;

        repaint();
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String s) {
        this.title = s;

        // TODO move to the native UI component
        getUi().invalidate();
    }


    public void setCommandListener(CommandListener l) {
        listener = l;

        getUi().setCommandListener(l);
    }


    CommandListener getCommandListener() {
        return listener;
    }

    void hideNotify() {
    }


    final void hideNotify(Display d) {
        getUi().hideNotify();

        hideNotify();
    }


    void keyPressed(int keyCode) {
    }


    void keyRepeated(int keyCode) {
    }


    void keyReleased(int keyCode) {
    }


    void pointerPressed(int x, int y) {
    }


    void pointerReleased(int x, int y) {
    }


    void pointerDragged(int x, int y) {
    }


    abstract void paint(Graphics g);


    void repaint() {
        if (currentDisplay != null) {
            repaint(0, 0, getWidth(), getHeight());
        }
    }


    void repaint(int x, int y, int width, int height) {
        if (currentDisplay != null) {
            currentDisplay.repaint(this, x, y, width, height);
        }
    }

    protected void sizeChanged(int w, int h) {
    }


    final void sizeChanged(Display d) {
        updateWidthAndHeight();
        sizeChanged(width, height);
    }


    void showNotify() {
    }


    final void showNotify(Display d) {
        currentDisplay = d;

        int w = device.getDeviceDisplay().getFullWidth();
        int h = device.getDeviceDisplay().getFullHeight();

        if (width != w || height != h) {
            sizeChanged(d);
        }

        showNotify();

        getUi().showNotify();
    }

    private void updateWidthAndHeight() {
        width = device.getDeviceDisplay().getFullWidth();
        height = device.getDeviceDisplay().getFullHeight();
    }

    protected DisplayableUI lazyLoad() {
        return null;
    }

    public final DisplayableUI getUi() {
        if (null == ui) {
            ui = lazyLoad();
        }
        return ui;
    }
}
