package jimmui.view.base.touch;

import jimmui.view.base.*;

// #sijapp cond.if modules_TOUCH is "true"#
public class KineticScrolling {
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
// #sijapp cond.end#
