package jimmui.view.base;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 19:37
 *
 * @author vladimir
 */
public interface SomeContent {
    public int getSize();

    public int getItemHeight(int itemIndex);

    public void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to);

    public void doJimmAction(int keyCode);

    public void drawItemBack(GraphicsEx g, int index, int selected, int x, int y, int w, int h, int skip, int to);
}
