/*
 * TouchControl.java
 *
 * Created on 18 Январь 2010 г., 18:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

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

    private int pressLimit;

    private ProtoCanvas canvas = null;

    public synchronized void setCanvas(ProtoCanvas c) {
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

    private synchronized void _pointerPressed(ProtoCanvas c, int x, int y) {
        stopKinetic();

        kineticOn = true;

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
    }

    private synchronized void _pointerReleased(ProtoCanvas c, int x, int y) {
        isDragged |= isDragged(x, y);

        curX = x;
        curY = y;

        if (isDragged) {
            updateDirection();
            if (horizontalDirection) {
                kineticOn = false;
            }
            c.stylusMoved(startX, startY, x, y, horizontalDirection, TouchControl.DRAGGED);
            if (kineticOn) {
                kinetic.release(y);
                startKinetic();
            }

        } else {
            c.stylusTap(x, y, Display.isLongAction(pressTime));
        }
        c.stylusReleased();
        pressTime = 0;
    }

    private synchronized void _pointerDragged(ProtoCanvas c, int x, int y) {
        if (!isDragged) {
            isDragged = isDragged(x, y);
            if (!isDragged) {
                return;
            }
        }

        if (kineticOn) {
            kinetic.drag(y);
        }

        if (isDragged && (0 < pressTime)) {
            curX = x;
            curY = y;
            updateDirection();
            c.stylusMoved(startX, startY, x, y, horizontalDirection, TouchControl.DRAGGING);
        }
    }

    synchronized void kineticMoving(int y) {
        ProtoCanvas c = canvas;
        if (kineticOn && (null != c) && (null != kineticTask)) {
            try {
                c.stylusMoved(startX, startY, startX, y, horizontalDirection, TouchControl.KINETIC);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("stylusKineticMoving", e);
                // #sijapp cond.end #
            }
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

class KineticScrolling {
    private int fromY;
    private long startTime;

    private int toY;
    private int period;

    public void press(int y, long now) {
        reset(y, now);
    }

    public void release(int y) {
        addPoint(y, System.currentTimeMillis());
    }

    public void drag(int y) {
        addPoint(y, System.currentTimeMillis());
    }

    public KineticScrollingTask create(TouchControl tc) {
        final int movingAfter = CanvasEx.minItemHeight / 2;
        final int time = period;
        final int way = toY - fromY;
        if ((Math.abs(way) < movingAfter) || (time < 2)) {
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
    private void addPoint(int y, long now) {
        if (toY == y) {
            return;
        }
        boolean sameDirection = ((fromY < y) && (toY < y)) || ((y < fromY) && (y < toY));
        int interval = (int)(now - startTime);
        if (sameDirection) {
            sameDirection = (interval < 100) || (2 < Math.abs(y - toY));
        }
        if (sameDirection) {
            toY = y;
            period = interval;
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
// #sijapp cond.end#