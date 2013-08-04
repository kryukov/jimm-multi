/*
 * TouchControl.java
 *
 * Created on 18 Январь 2010 г., 18:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.base;

import jimmui.view.base.touch.*;

/**
 *
 * @author Vladimir Kryukov
 */
// #sijapp cond.if modules_TOUCH is "true"#
public class TouchControl {
    public static final int DRAGGING = 0;
    public static final int DRAGGED = 1;
    public static final int KINETIC = 2;
    public static final int discreteness = 50;

    private boolean isDragged;
    private int startY;
    private int startX;
    private int curY;
    private int curX;
    private long pressTime;


    private boolean horizontalDirection;
    private boolean directionUnlock;

    private TouchState state = new TouchState();

    private boolean kineticOn;
    private volatile KineticScrollingTask kineticTask = null;
    private final KineticScrolling kinetic = new KineticScrolling();
    private LongTapTask longTapTask = null;
    private boolean lockTouch;

    private int pressLimit;

    private CanvasEx canvas = null;

    public synchronized void setCanvas(CanvasEx c) {
        stopKinetic();
        pressLimit = CanvasEx.minItemHeight / 2;
        canvas = c;
    }

    public void pointerDragged(int x, int y) {
        try {
            _pointerDragged(canvas, x, y);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("stylusMoving", e);
            // #sijapp cond.end #
        }
    }
    public void pointerReleased(int x, int y) {
        try {
            _pointerReleased(canvas, x, y);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("stylusReleased", e);
            // #sijapp cond.end #
        }
    }
    public void pointerPressed(int x, int y) {
        try {
            _pointerPressed(canvas, x, y);
        } catch (Exception e) {
            kineticOn = false;
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("stylusPressed", e);
            // #sijapp cond.end #
        }
    }

    public void pointerLongTap() {
        try {
            _pointerReleased(canvas, startX, startY);
            lockTouch = true;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("pointerLongTap", e);
            // #sijapp cond.end #
        }
    }


    private synchronized void _pointerPressed(CanvasEx c, int x, int y) {
        stopKinetic();
        lockTouch = false;

        kineticOn = true;
        state.region = null;

        horizontalDirection = false;
        directionUnlock = true;
        isDragged = false;
        startX = x;
        startY = y;
        curX = x;
        curY = y;
        long now = System.currentTimeMillis();
        pressTime = now;
        kinetic.press(y, now);

        state.fromX = x;
        state.fromY = y;
        state.x = x;
        state.y = y;
        c.stylusPressed(state);
        if (null != state.region) {
            kineticOn = false;
            state.region.stylusPressed(canvas, startX, startY);
        }

        longTapTask = new LongTapTask(this);
        longTapTask.start(Display.LONG_INTERVAL);
    }

    private synchronized void _pointerReleased(CanvasEx c, int x, int y) {
        if (lockTouch) return;
        longTapTask.stop();
        isDragged |= isDragged(x, y);

        curX = x;
        curY = y;

        if (isDragged) {
            updateDirection();
            if (horizontalDirection) {
                kineticOn = false;
            }
            __stylusMoved(c, x, y, horizontalDirection, TouchControl.DRAGGED);
            if (kineticOn) {
                kinetic.release(y);
                startKinetic();
            }

        } else {
            __stylusTap(c, x, y, Display.isLongAction(pressTime));
        }
        if (null != state.region) {
            state.region.stylusReleased();
        }
        pressTime = 0;
    }

    private synchronized void _pointerDragged(CanvasEx c, int x, int y) {
        if (lockTouch) return;
        if (!isDragged) {
            isDragged = isDragged(x, y);
            if (!isDragged) {
                return;
            }
        }

        longTapTask.stop();
        if (kineticOn) {
            kinetic.drag(y);
        }

        if (isDragged && (0 < pressTime)) {
            curX = x;
            curY = y;
            updateDirection();
            __stylusMoved(c, x, y, horizontalDirection, TouchControl.DRAGGING);

        }
    }

    public synchronized void kineticMoving(int y) {
        if (lockTouch) return;
        CanvasEx c = canvas;
        if (kineticOn && (null != c) && (null != kineticTask)) {
            try {
                __stylusMoved(c, startX, y, horizontalDirection, TouchControl.KINETIC);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("stylusKineticMoving", e);
                // #sijapp cond.end #
            }
        }
    }


    private void __stylusTap(CanvasEx c, int x, int y, boolean longTap) {
        state.fromX = startX;
        state.fromY = startY;
        state.x = curX;
        state.y = curY;
        state.isLong = longTap;
        if (null == state.region) {
            c.stylusTap(state);
        } else {
            state.region.stylusTap(c, startX, startY, longTap);
        }
    }
    private void __stylusMoved(CanvasEx c, int x, int y, boolean horizontalDirection, int type) {
        state.fromX = startX;
        state.fromY = startY;
        state.x = x;
        state.y = y;
        state.type = type;
        if (null == state.region) {
            if (horizontalDirection) {
                if (TouchControl.DRAGGING == type) {
                    c.stylusXMoving(state);
                } else if (TouchControl.DRAGGED == type) {
                    c.stylusXMoved(state);
                }
            } else {
                c.stylusGeneralYMoved(state);
            }
        } else {
            state.region.stylusMoved(c, startX, startY, x, y, horizontalDirection, type);
        }
    }


    private void startKinetic() {
        KineticScrollingTask it = kinetic.create(this);
        if (null != it) {
            it.start(discreteness);
            kineticTask = it;
        }
    }
    private void stopKinetic() {
        KineticScrollingTask k = kineticTask;
        kineticTask = null;
        if (null != k) {
            k.stop();
        }
    }

    private boolean isDragged(int x, int y) {
        return (pressLimit < Math.abs(x - startX))
                || (pressLimit < Math.abs(y - startY));
    }
    private void updateDirection() {
        if (directionUnlock) {
            horizontalDirection = (Math.abs(startY - curY) * 2 < Math.abs(startX - curX));
            directionUnlock = false;
        }
    }
}
// #sijapp cond.end#