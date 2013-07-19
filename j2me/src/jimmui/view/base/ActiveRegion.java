package jimmui.view.base;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 24.02.13 22:10
 *
 * @author vladimir
 */
public class ActiveRegion {
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusPressed(CanvasEx c, int x, int y) {
    }
    protected void stylusTap(CanvasEx c, int x, int y, boolean longTap) {
    }
    protected void stylusMoved(CanvasEx c, int fromX, int fromY, int toX, int toY, boolean horizontalDirection, int type) {
    }
    public void stylusReleased() {
    }
    // #sijapp cond.end#
}
