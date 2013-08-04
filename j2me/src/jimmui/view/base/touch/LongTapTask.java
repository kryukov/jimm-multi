package jimmui.view.base.touch;

// #sijapp cond.if modules_TOUCH is "true"#

import jimmui.view.base.*;

import java.util.Timer;
import java.util.TimerTask;

public class LongTapTask extends TimerTask {
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