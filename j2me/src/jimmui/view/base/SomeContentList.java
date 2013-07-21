package jimmui.view.base;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 19:36
 *
 * @author vladimir
 */
public class SomeContentList extends VirtualList {
    protected SomeContent content;

    public SomeContentList(String capt) {
        super(capt);
    }

    @Override
    protected final int getSize() {
        return content.getSize();
    }

    @Override
    protected final int getItemHeight(int itemIndex) {
        return content.getItemHeight(itemIndex);
    }

    @Override
    protected final void drawItemBack(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        content.drawItemBack(g, index, getCurrItem(), x1, y1, w, h, skip, to);
    }
    @Override
    protected final void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        content.drawItemData(g, index, x1, y1, w, h, skip, to);
    }

    @Override
    protected void doJimmAction(int keyCode) {
        content.doJimmAction(keyCode);
    }
}
