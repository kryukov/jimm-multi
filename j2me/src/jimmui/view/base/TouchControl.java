/*
 * TouchControl.java
 *
 * Created on 18 Январь 2010 г., 18:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.base;

import java.util.*;

/**
 *
 * @author Vladimir Kryukov
 */
// #sijapp cond.if modules_TOUCH is "true"#
public class TouchControl {
    public static final int DRAGGING = 0;
    public static final int DRAGGED = 1;
    public static final int KINETIC = 2;
    static final int discreteness = 50;

    private boolean isDragged;
    private int startY;
    private int startX;
    private int curY;
    private int curX;
    private long pressTime;


    public boolean touchUsed;

    private boolean horizontalDirection;
    private boolean directionUnlock;

    public int prevTopY;
    public boolean isSecondTap;

    boolean kineticOn;
    private volatile KineticScrollingTask kineticTask = null;
    private final KineticScrolling kinetic = new KineticScrolling();
    private LongTapTask longTapTask= null;
    private boolean lockTouch;

    private int pressLimit;

    private CanvasEx canvas = null;
    public ActiveRegion region;

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
        region = null;

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

        c.stylusPressed(x, y);

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
        if (null != region) {
            region.stylusReleased();
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

    synchronized void kineticMoving(int y) {
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
        if (null == region) {
            c.stylusTap(startX, startY, longTap);
        } else {
            region.stylusTap(c, startX, startY, longTap);
        }
    }
    private void __stylusMoved(CanvasEx c, int x, int y, boolean horizontalDirection, int type) {
        if (null == region) {
            if (horizontalDirection) {
                if (TouchControl.DRAGGING == type) {
                    c.stylusXMoving(startX, startY, x, y);
                } else if (TouchControl.DRAGGED == type) {
                    c.stylusXMoved(startX, startY, x, y);
                }
            } else {
                c.stylusGeneralYMoved(startX, startY, x, y, type);
            }
        } else {
            region.stylusMoved(c, startX, startY, x, y, horizontalDirection, type);
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

    public void setRegion(ActiveRegion region) {
        this.region = region;
        kineticOn = false;
        region.stylusPressed(canvas, startX, startY);
    }
}

class KineticScrolling {
    private int fromY;
    private long startTime;

    private int prevY;
    private long prevTime;

    private int toY;
    private int period;

    public void press(int y, long now) {
        reset(y, now);
    }

    public void release(int y) {
        addPoint(y, System.currentTimeMillis(), false);
    }

    public void drag(int y) {
        addPoint(y, System.currentTimeMillis(), true);
    }

    public KineticScrollingTask create(TouchControl tc) {
        final int movingAfter = CanvasEx.minItemHeight / 2;
        final int time = period;
        final int way = toY - fromY;
        if ((Math.abs(way) < movingAfter) || (time < 10)) {
            return null;
        }
        final int v = calcV(way, time);
        if (Math.abs(v) <= 2) {
            return null;
        }
        int a = Math.max(1, Math.max(calcAbsA(v, time) / 3, calcAbsA(v, 3000)));
        return new KineticScrollingTask(tc, toY, v, ((way < 0) ? +a : -a));
    }

    private int calcV(int way, int time) {
        // v = 2 * s / t - v0
        // v0 = 0
        return 2 * way * TouchControl.discreteness / time;
    }
    private int calcAbsA(int v, int time) {
        // a = (v1 - v0) / t
        // v0 = 0
        return Math.abs(v * TouchControl.discreteness / time);
    }

    private void reset(int y, long now) {
        fromY = y;
        startTime = now;
        toY = y;
        period = 0;
    }
    private void addPoint(int y, long now, boolean drag) {
        if ((toY == y) && !drag) {
            return;
        }
        boolean sameDirection = ((fromY < y) && (toY <= y)) || ((y < fromY) && (y <= toY));
        if (sameDirection) {
            int interval = (int)(now - prevTime);
            sameDirection = (interval < 50) || (5 < Math.abs(y - toY));
        }
        if (sameDirection) {
            if (drag) {
                prevY = y;
                prevTime = startTime;
            }
            toY = y;
            period = (int)(now - startTime);
        } else {
            reset(y, now);
        }
    }
}

class KineticScrollingTask extends TimerTask {
    private int y;
    private int a;
    private int v;
    private Timer timer;
    private TouchControl touch;

    public KineticScrollingTask(TouchControl touch, int y, int velosity, int acceleration) {
        this.y = y;
        this.a = acceleration;
        this.v = velosity;
        this.touch = touch;
        timer = new Timer();
    }
    public void start(int interval) {
        timer.schedule(this, interval, interval);
    }

    public void run() {
        y += v;
        TouchControl t = touch;
        if (null != t) {
            t.kineticMoving(y);
        }
        int prevV = v;
        v += a;

        if (Math.abs(v) <= Math.min(5, CanvasEx.minItemHeight / 4)) {
            stop();

        } else if (v * prevV < 0) {
            stop();
        }
    }
    public void stop() {
        cancel();
        touch = null;
        Timer t = timer;
        timer = null;
        if (null != t) {
            t.cancel();
        }
    }
}
class LongTapTask extends TimerTask {
    private Timer timer;
    private TouchControl touch;
    public LongTapTask(TouchControl touch) {
        this.touch = touch;
        timer = new Timer();
    }

    public void start(long interval) {
        timer.schedule(this, interval);
    }
    public void run() {
        try {
            touch.pointerLongTap();
        } catch (Exception ignored) {
        }
    }
    public void stop() {
        touch = null;
        Timer t = timer;
        timer = null;
        if (null != t) {
            t.cancel();
        }
    }
}
// #sijapp cond.end#