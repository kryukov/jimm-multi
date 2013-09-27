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
 *  @version $Id: AndroidCanvasUI.java 2507 2011-07-12 07:52:03Z barteo@gmail.com $
 */

package org.microemu.android.device.ui;

import javax.microedition.lcdui.Canvas;

import android.view.View;
import android.widget.RelativeLayout;
import jimmui.view.chat.Chat;
import org.microemu.android.MicroEmulatorActivity;
import org.microemu.android.device.AndroidDeviceDisplay;
import org.microemu.device.ui.CanvasUI;
import ru.net.jimm.R;
import ru.net.jimm.input.Input;

public class AndroidCanvasUI extends AndroidDisplayableUI<Canvas> implements CanvasUI {
    private CanvasView canvasView;
    private Input input;
    private View view;

    public AndroidCanvasUI(final MicroEmulatorActivity activity, Canvas canvas) {
        super(activity, canvas);
        activity.post(new Runnable() {
            public void run() {
                canvasView = new CanvasView(activity, AndroidCanvasUI.this, 666);
                input = new Input(activity, null, R.id.input_line);
                input.setVisibility(View.INVISIBLE);
                view = createView(canvasView, input);
            }
        });
    }
    private View createView(CanvasView canvas, Input input) {
        RelativeLayout all = new RelativeLayout(activity);
        all.addView(canvas, set(create(RelativeLayout.LayoutParams.FILL_PARENT),
                RelativeLayout.ABOVE, input.getId()));
        all.addView(input, set(create(RelativeLayout.LayoutParams.WRAP_CONTENT),
                RelativeLayout.ALIGN_PARENT_BOTTOM));
        return all;
    }
    private RelativeLayout.LayoutParams create(int h) {
        return new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, h);
    }
    private RelativeLayout.LayoutParams set(RelativeLayout.LayoutParams params, int verb, int anchor) {
        params.addRule(verb, anchor);
        return params;
    }
    private RelativeLayout.LayoutParams set(RelativeLayout.LayoutParams params, int verb) {
        params.addRule(verb);
        return params;
    }

    @Override
    public void hideNotify() {
        ((AndroidDeviceDisplay) activity.getEmulatorContext().getDeviceDisplay()).removeDisplayRepaintListener(canvasView);
    }

    @Override
    public void showNotify() {

        activity.post(new Runnable() {
            public void run() {
                activity.setContentView(view);
                canvasView.requestFocus();
                ((AndroidDeviceDisplay) activity.getEmulatorContext().getDeviceDisplay()).addDisplayRepaintListener(canvasView);
                displayable.repaint();
            }
        });
    }

    public Input getInput() {
        return input;
    }

    public void setInputVisibility(final boolean v, final Chat chat) {
        if ((null == chat) && (null != input)) {
            input.resetOwner();
        }
        activity.post(new Runnable() {
            public void run() {
                boolean prevV = (input.getVisibility() == View.VISIBLE);
                input.setVisibility(v ? View.VISIBLE : View.GONE);
                if (null != chat) {
                    input.setOwner(chat);
                }
                view.requestLayout();
                if (v && input.hasText()) {
                    input.showKeyboard();
                } else {
                    canvasView.requestFocus();
                    if ((prevV && !v) || !input.hasText()) {
                        input.hideKeyboard(view);
                    }
                }

            }
        });
    }
    public void layout() {
        view.requestLayout();
    }
    public CanvasView getCanvasView() {
        return canvasView;
    }
}
