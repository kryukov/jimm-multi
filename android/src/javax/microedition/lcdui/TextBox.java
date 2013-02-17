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

import org.microemu.android.device.ui.AndroidTextBoxUI;
import org.microemu.device.DeviceFactory;
import org.microemu.device.InputMethod;
import org.microemu.device.InputMethodEvent;
import org.microemu.device.InputMethodListener;
import org.microemu.device.ui.DisplayableUI;
import org.microemu.device.ui.TextBoxUI;

//TODO implement pointer events
public class TextBox extends Screen {
    private int maxSize;
    private int constraints;
    private String initText;

    public TextBox(String title, String text, int maxSize, int constraints) {
        super(title);
        initText = (null == text) ? "" : text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }
    protected DisplayableUI lazyLoad() {
        return DeviceFactory.getDevice().getUIFactory().createTextBoxUI(this, initText);
    }


    public void delete(int offset, int length) {
        ((AndroidTextBoxUI) getUi()).delete(offset, length);
    }

    public int getCaretPosition() {
        return ((AndroidTextBoxUI) getUi()).getCaretPosition();
    }

    public int getChars(char[] data) {
        String field = getString();
        if (data.length < field.length()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        field.getChars(0, field.length(), data, 0);

        return field.length();
    }

    public void setConstraints(int constraints) {
        if ((constraints & TextField.CONSTRAINT_MASK) < TextField.ANY
                || (constraints & TextField.CONSTRAINT_MASK) > TextField.DECIMAL) {
            throw new IllegalArgumentException("constraints " + constraints + " is an illegal value");
        }
        this.constraints = constraints;
        if (!InputMethod.validate(getString(), constraints)) {
            setString("");
        }
        // TODO
    }
    public int getConstraints() {
        return constraints;
    }

    public int setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException();
        }
        this.maxSize = maxSize;
        // TODO
        return maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public String getString() {
        return ((TextBoxUI) getUi()).getString();
    }

    public void insert(char[] data, int offset, int length, int position) {
        insert(new String(data, offset, length), position);
    }

    public void insert(String src, int position) {
        ((TextBoxUI) getUi()).insert(src, position);
    }

    public void setChars(char[] data, int offset, int length) {
        setString(new String(data, offset, length));
    }

    public void setInitialInputMode(String characterSubset) {
        // TODO implement
    }

    public final void setString(String text) {
        if (!InputMethod.validate(text, constraints)) {
            throw new IllegalArgumentException();
        }
        ((AndroidTextBoxUI) getUi()).setString(text);
    }

    @Override
    public void setTicker(Ticker ticker) {
        // TODO implement
    }

    @Override
    public void setTitle(String s) {
        super.setTitle(s);
    }

    public int size() {
        return ((TextBoxUI) getUi()).getString().length();
    }

    @Override
    void hideNotify() {
        super.hideNotify();
    }

    @Override
    void showNotify() {
        super.showNotify();
    }
}
