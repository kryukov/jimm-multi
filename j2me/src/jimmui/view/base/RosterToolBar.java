package jimmui.view.base;

// #sijapp cond.if modules_TOUCH is "true"#

import jimm.Jimm;
import jimmui.view.icons.Icon;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.VirtualContactList;
import jimm.cl.GlobalStatusForm;
import jimmui.view.menu.MenuModel;
import protocol.Protocol;
import protocol.ui.InfoFactory;

import javax.microedition.lcdui.Graphics;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 09.03.13 2:45
 *
 * @author vladimir
 */
public class RosterToolBar extends MySoftBar {
    public int getHeight() {
        return GraphicsEx.getSoftBarSize();
    }
    protected void stylusTap(CanvasEx c, int x, int y, boolean longTap) {
        int _x = 0;
        int defWidth = getHeight();
        _x += defWidth;
        if (x < _x) {
            Jimm.getJimm().getCL().activateMainMenu();
            return;
        }
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            _x += defWidth;
            if (x < _x) {
                MenuModel model = Jimm.getJimm().getCL().getContextMenu(p, null);
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
        if (null == Scheme.softbarImage) {
            g.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            g.fillRect(0, y, width, height);
            g.setThemeColor(CanvasEx.THEME_CAP_LINE);
            g.drawLine(0, y, width, y);
            g.drawLine(0, y + 1, width, y + 1);
        } else {
            g.drawBarBack(y, height, Scheme.softbarImage, width);
        }

        int x = 0;
        // general
        x += drawLeft(g, GlobalStatusForm.getGlobalStatusIcon(), x, y, height);
        // accounts
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            Icon icon = InfoFactory.factory.getStatusInfo(p).getIcon(p.getProfile().statusIndex);
            if (p.isConnecting()) {
                int progress = Math.min(2, getIconWidth() * p.getConnectingProgress() / 100);
                g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
                g.fillRect(x, y + height - CanvasEx.scrollerWidth, progress, CanvasEx.scrollerWidth);
            }
            x += drawLeft(g, icon, x, y, height);
        }
    }

    private int getIconWidth() {
        return getHeight();
    }

    private int getSeparatorWidth() {
        return 2;
    }

    private int drawLeft(GraphicsEx g, Icon  icon, int x, int y, int height) {
        int defWidth = getIconWidth();
        if (null != icon) {
            g.drawImage(icon, x + (defWidth - icon.getWidth()) / 2, y, height);
        }
        return defWidth + getSeparatorWidth();
    }
}
// #sijapp cond.end#
