package jimmui.view.base.touch;

import jimmui.view.base.ActiveRegion;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 04.08.13 13:52
 *
 * @author vladimir
 */
public class TouchState {
    public int fromX;
    public int fromY;
    public int x;
    public int y;
    public boolean isSecondTap;
    public boolean isLong;
    public int prevTopY;
    public int type;
    public ActiveRegion region;
}
