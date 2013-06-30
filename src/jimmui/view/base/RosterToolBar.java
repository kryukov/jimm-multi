package jimmui.view.base;

// #sijapp cond.if modules_TOUCH is "true"#

import jimmui.view.icons.Icon;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.VirtualContactList;
import jimm.cl.ContactList;
import jimm.cl.GlobalStatusForm;
import jimmui.view.menu.MenuModel;
import protocol.Protocol;

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
            ContactList.getInstance().activateMainMenu();
            return;
        }
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            _x += defWidth;
            if (x < _x) {
                MenuModel model = ContactList.getInstance().getContextMenu(p, null);
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
        // #sijapp cond.if modules_MULTI is "true" #
        // general
        x += drawLeft(g, GlobalStatusForm.getGlobalStatusIcon(), x, y, height);
        // accounts
        ContactListModel m = ((VirtualContactList)c).getModel();
        for (int i = 0; i < m.getProtocolCount(); ++i) {
            Protocol p = m.getProtocol(i);
            Icon icon = p.getStatusInfo().getIcon(p.getProfile().statusIndex);
            if (p.isConnecting()) {
                int progress = Math.min(2, getIconWidth() * p.getConnectingProgress() / 100);
                g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
                g.fillRect(x, y + height - CanvasEx.scrollerWidth, progress, CanvasEx.scrollerWidth);
            }
            x += drawLeft(g, icon, x, y, height);
        }
        // #sijapp cond.else #
        Protocol p = ((VirtualContactList)c).getModel().getProtocol(0);
        Icon icon = p.getStatusInfo().getIcon(p.getProfile().statusIndex);
        x += drawLeft(g, icon, x, y, height);
        // #sijapp cond.end #
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
        drawSeparator(g, x + defWidth, y + 1, height);
        return defWidth + getSeparatorWidth();
    }
    private void drawSeparator(GraphicsEx g, int x, int y, int height) {
        g.setThemeColor(CanvasEx.THEME_BACKGROUND);
        g.drawLine(x, y, x, y + height);
        g.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
        g.drawLine(x + 1, y, x + 1, y + height);
    }
}
// #sijapp cond.end#
