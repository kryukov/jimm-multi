package org.microemu.android.device.ui;


import android.view.GestureDetector;
import android.widget.Scroller;
import jimmui.view.base.NativeCanvas;
import jimmui.view.base.TouchControl;
import jimmui.view.base.touch.TouchState;
import org.microemu.DisplayAccess;
import org.microemu.MIDletAccess;
import org.microemu.MIDletBridge;
import org.microemu.android.device.AndroidDeviceDisplay;
import org.microemu.android.device.AndroidDisplayGraphics;
import org.microemu.app.ui.DisplayRepaintListener;
import org.microemu.device.DeviceFactory;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.02.13 23:46
 *
 * @author vladimir
 */
public class CanvasView extends View implements DisplayRepaintListener {
    private AndroidDisplayGraphics graphics = null;

    private final static int FIRST_DRAG_SENSITIVITY_X = 5;

    private final static int FIRST_DRAG_SENSITIVITY_Y = 5;

    private AndroidCanvasUI ui;

    private AndroidKeyListener keyListener = null;

    private int inputType = InputType.TYPE_CLASS_TEXT;

    private GestureDetector gestureDetector;
    private Scroller scroller;
    private TouchState state = new TouchState();

    public CanvasView(final Context context, AndroidCanvasUI ui, int id) {
        super(context);
        this.ui = ui;
        setFocusable(false);
        setFocusableInTouchMode(false);
        setId(id);
        scroller = new Scroller(context);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                state.fromX = (int) e.getX();
                state.fromY = (int) e.getY();
                state.x = (int) e.getX();
                state.y = (int) e.getY();
                state.region = null;
                getNativeCanvas().androidPointerPressed(state);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                // TODO
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                state.x = (int) e.getX();
                state.y = (int) e.getY();
                state.isLong = false;
                getNativeCanvas().androidPointerTap(state);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                state.x = (int) e.getX();
                state.y = (int) e.getY();
                state.isLong = true;
                getNativeCanvas().androidPointerTap(state);
            }

            @Override
            public boolean onScroll(MotionEvent e, MotionEvent e2, float distanceX, float distanceY) {
                state.x = (int) e2.getX();
                state.y = (int) e2.getY();
                state.isLong = false;
                state.type = TouchControl.DRAGGING;
                getNativeCanvas().androidPointerMoving(state, (int) distanceX, (int) distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e, MotionEvent e2, float  vX, float vY) {
                state.x = (int) e2.getX();
                state.y = (int) e2.getY();
                state.isLong = false;
                state.type = TouchControl.DRAGGED;
                scroller.fling(state.x, state.y, 0, (int)vY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                beginScrolling();
                if (state.fromY != state.y) {
                    getNativeCanvas().androidPointerMoved(state, state.x - state.fromX, state.y - state.fromY);
                }
                return true;
            }
        });
        gestureDetector.setIsLongpressEnabled(true);
    }


    private void beginScrolling() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (scroller.computeScrollOffset()) {
                    state.x = (int) scroller.getCurrX();
                    state.y = (int) scroller.getCurrY();
                    state.type = TouchControl.KINETIC;
                    getNativeCanvas().androidPointerMoving(state, 0, 1);
                    try { Thread.sleep(50);} catch (Exception e) {}
                }
            }
        }).start();
    }

    private NativeCanvas getNativeCanvas() {
        return (NativeCanvas) ui.getDisplayable();
    }

    private void initGraphics() {
        if (graphics == null) {
            graphics = new AndroidDisplayGraphics();
        }
    }

    public void flushGraphics(int x, int y, int width, int height) {
        postInvalidate();
    }


    public void setKeyListener(AndroidKeyListener keyListener, int inputType) {
        this.keyListener = keyListener;
        this.inputType = inputType;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        outAttrs.inputType = inputType;

        return new BaseInputConnection(this, false) {

            @Override
            public boolean performEditorAction(int actionCode) {
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(CanvasView.this.getWindowToken(), 0);
                    return true;
                } else {
                    return super.performEditorAction(actionCode);
                }
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (keyListener != null) {
                    keyListener.insert(keyListener.getCaretPosition(), text);
                }

                return true;
            }

            @Override
            public boolean deleteSurroundingText(int leftLength, int rightLength) {
                if (keyListener != null) {
                    int caret = keyListener.getCaretPosition();
                    keyListener.delete(caret - leftLength, caret + rightLength);
                }

                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                return super.sendKeyEvent(event);
            }

        };
    }

    //
    // View
    //

    @Override
    public void onDraw(android.graphics.Canvas androidCanvas) {
        super.onDraw(androidCanvas);
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return;
        }
        initGraphics();
        graphics.reset(androidCanvas);
        ma.getDisplayAccess().paint(graphics);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        AndroidDeviceDisplay deviceDisplay = (AndroidDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay();
        deviceDisplay.displayRectangleWidth = w;
        deviceDisplay.displayRectangleHeight = h;
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return;
        }
        DisplayAccess da = ma.getDisplayAccess();
        if (da != null) {
            da.sizeChanged();
        }
    }
    public void resetScrolling() {
        scroller.abortAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);
        if (MotionEvent.ACTION_UP == event.getAction()) {
            getNativeCanvas().androidPointerReleased();
        }
        return result;
    }

    //
    // DisplayRepaintListener
    //

    public void repaintInvoked(Object repaintObject) {
        Rect r = (Rect) repaintObject;
        flushGraphics(r.left, r.top, r.width(), r.height());
    }
}

