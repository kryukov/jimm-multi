package jimm.ui.base;

// #sijapp cond.if modules_TOUCH is "true"#

import DrawControls.icons.Icon;
import DrawControls.tree.ContactListModel;
import DrawControls.tree.VirtualContactList;
import jimm.cl.ContactList;
import jimm.ui.menu.MenuModel;
import protocol.Protocol;

import javax.microedition.lcdui.Graphics;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 09.03.13 2:45
 *
 * @author vladimir
 */
public class RosterSoftBar extends MySoftBar {
    public int getHeight() {
        return GraphicsEx.getSoftBarSize();
    }
    protected void stylusTap(CanvasEx c, int x, int y, boolean longTap) {
        int _x = 0;
        int defWidth = getHeight();
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            _x += defWidth;
            if (x < _x) {
                MenuModel model = ContactList.getInstance().getContextMenu(p, p.getProtocolBranch());
                ((VirtualContactList) c).showMenu(model);
                return;
            }
        }
    }
    public void paint(GraphicsEx g, CanvasEx c, int y) {
        int width = c.getWidth();

        g.setStrokeStyle(Graphics.SOLID);
        int height = getHeight();
        g.setClip(0, y, width, height);
        g.drawBarBack(y, height, Scheme.softbarImage, width);

        int x = 0;
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            Icon icon = p.getStatusInfo().getIcon(p.getProfile().statusIndex);
            x += drawLeft(g, icon, x, y, height);
        }
    }
    private int drawLeft(GraphicsEx g, Icon  icon, int x, int y, int height) {
        int defWidth = getHeight();
        g.drawImage(icon, x + (defWidth - icon.getWidth()) / 2, y, height);
        drawSeparator(g, x + defWidth, y, height);
        return defWidth;
    }
    private void drawSeparator(GraphicsEx g, int x, int y, int height) {
        g.setThemeColor(CanvasEx.THEME_BACKGROUND);
        g.drawLine(x, y, x, y + height);
        g.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
        g.drawLine(x + 1, y, x + 1, y + height);
    }
}
// #sijapp cond.end#
