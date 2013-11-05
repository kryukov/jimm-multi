package jimmui.view.icons;

import jimmui.view.base.CanvasEx;

import java.util.Timer;
import java.util.TimerTask;
// #sijapp cond.if modules_ANISMILES is "true" #

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 06.11.13 0:18
 *
 * @author vladimir
 */
public class Animation extends TimerTask {
    public static final int WAIT_TIME = 100;
    private AniIcon[] icons;

    public Animation(AniIcon[] icons) {
        this.icons = icons;
    }
    public void run() {
        iteration();
    }
    private void iteration() {
        boolean update = false;
        for (AniIcon icon : icons) {
            if (null != icon) {
                update |= icon.nextFrame(WAIT_TIME);
            }
        }
        if (update) {
            Object screen = jimm.Jimm.getJimm().getDisplay().getCurrentDisplay();
            if (screen instanceof CanvasEx) {
                ((CanvasEx) screen).invalidate();
            }
        }
    }
    public void start() {
        Timer timer = new Timer();
        timer.schedule(this, WAIT_TIME, WAIT_TIME);
    }
}
// #sijapp cond.end #
