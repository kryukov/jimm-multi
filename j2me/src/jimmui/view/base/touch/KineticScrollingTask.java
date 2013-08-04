package jimmui.view.base.touch;

import jimmui.view.base.*;

import java.util.*;

// #sijapp cond.if modules_TOUCH is "true"#
public class KineticScrollingTask extends TimerTask {
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
