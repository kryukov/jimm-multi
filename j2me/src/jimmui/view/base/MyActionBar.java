package jimmui.view.base;


import jimmui.view.icons.Icon;
import jimmui.view.roster.ContactListModel;
import jimmui.view.roster.VirtualContactList;
import jimm.Jimm;
import jimm.comm.Util;
import protocol.Protocol;

import javax.microedition.lcdui.Graphics;

/**
 *
 * @author vladimir
 */
public class MyActionBar extends ActiveRegion {

    private Icon[] images;
    private String caption;
    private String ticker;
    private static Icon messageIcon;

    private static String time = "";

    static void refreshClock() {
        // #sijapp cond.if modules_ANDROID isnot "true"#
        // #sijapp cond.if modules_TOUCH is "true"#
        time = Util.getLocalDateString(Jimm.getCurrentGmtTime(), true);
        int h = GraphicsEx.getSoftBarSize();
        Jimm.getJimm().getDisplay().getNativeCanvas().repaint(0, 0, Jimm.getJimm().getDisplay().getScreenWidth(), h);
        // #sijapp cond.end#
        // #sijapp cond.end#
    }

    public static void setMessageIcon(Icon messageIcon) {
        MyActionBar.messageIcon = messageIcon;
    }

    public static Icon getMessageIcon() {
        return messageIcon;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
    public int getHeight() {
        return GraphicsEx.calcCaptionHeight(images, caption);
    }

    public void setImages(Icon[] images) {
        this.images = images;
    }

    public String getCaption() {
        return caption;
    }

    public void paint(GraphicsEx g, CanvasEx view, int width) {
        final int height = getHeight();
        g.setStrokeStyle(Graphics.SOLID);
        g.setClip(0, 0, width, height + 1);
        if (null == Scheme.captionImage) {
            g.setThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            g.fillRect(0, 0, width, height);
            g.setThemeColor(CanvasEx.THEME_CAP_LINE);
            g.drawLine(0, height - 1, width, height - 1);
            g.drawLine(0, height, width, height);
        } else {
            g.drawBarBack(0, height, Scheme.captionImage, width);
        }

        int x = 0;
        // #sijapp cond.if target is "MIDP2"#
        x += GraphicsEx.captionOffset;
        width -= GraphicsEx.captionOffset;
        width -= GraphicsEx.captionWidthFix;
        // #sijapp cond.end#
        x += 2;

        // #sijapp cond.if modules_ANDROID isnot "true"#
        // #sijapp cond.if modules_TOUCH is "true"#
        g.setFont(GraphicsEx.softBarFont);
        g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
        int timeWidth = GraphicsEx.softBarFont.stringWidth(time);
        width -= (timeWidth + 5);
        g.drawString(null, time, null, x + width + 3, 1, timeWidth, height - 2);
        // #sijapp cond.end#
        // #sijapp cond.end#

        Icon ic = messageIcon;
        if (null != ic) {
            width -= drawRight(g, ic, x + width, getHeight(), height);
            width -= 1;
        }

        g.setFont(GraphicsEx.captionFont);
        g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
        // #sijapp cond.if modules_TOUCH isnot "true"#
        if (view instanceof VirtualContactList) {
            int progress = 0;
            int maxProgress = 0;
            ContactListModel m = ((VirtualContactList)view).getModel();
            for (int i = 0; i < m.getProtocolCount(); ++i) {
                Protocol p = m.getProtocol(i);
                if (p.isConnecting()) {
                    progress += p.getConnectingProgress();
                    maxProgress += 100;
                }
            }
            if (0 < maxProgress) {
                progress = width * progress / maxProgress;
                g.fillRect(0, height - CanvasEx.scrollerWidth / 2, progress, CanvasEx.scrollerWidth / 2);
            }
        }
        // #sijapp cond.end#
        String label = (null == ticker) ? caption : ticker;
        g.drawString(images, label, null, x, 1, width, height - 2);
    }
    private int drawLeft(GraphicsEx g, Icon  icon, int x, int defWidth, int height) {
        // #sijapp cond.if modules_TOUCH isnot "true"#
        defWidth = icon.getWidth();
        // #sijapp cond.end#
        g.drawImage(icon, x + (defWidth - icon.getWidth()) / 2, 0, height);
        // #sijapp cond.if modules_TOUCH is "true"#
        g.drawLine(x + defWidth, 0, x + defWidth, height);
        // #sijapp cond.end#
        return defWidth;
    }
    private int drawRight(GraphicsEx g, Icon  icon, int x, int defWidth, int height) {
        if (null == icon) {
            return 0;
        }
        // #sijapp cond.if modules_TOUCH isnot "true"#
        defWidth = icon.getWidth();
        // #sijapp cond.end#
        g.drawImage(icon, x - defWidth + (defWidth - icon.getWidth()) / 2, 0, height);
        return defWidth;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(CanvasEx canvas, int x, int y, boolean longTap) {
        int region = getCaptionRegion(canvas, x, canvas.getWidth());

        if (CAPTION_REGION_NEW_MESSAGE == region) {
            if (Jimm.getJimm().getCL().isChats(canvas)) {
                Jimm.getJimm().getCL().backFromChats();
            } else {
                Jimm.getJimm().getCL().showChatList(true);
            }

        } else if (CAPTION_REGION_GENERAL == region) {

            if (Jimm.getJimm().getCL().isChats(canvas)) {
                Jimm.getJimm().getCL().backFromChats();
            } else {
                Jimm.getJimm().getCL().showChatList(false);
            }
        }
    }
    public static final int CAPTION_REGION_NEW_MESSAGE = -3;
    public static final int CAPTION_REGION_GENERAL = 1;
    protected int getCaptionRegion(CanvasEx view, int x, int width) {
        // #sijapp cond.if target is "MIDP2"#
        x += GraphicsEx.captionOffset;
        width -= GraphicsEx.captionOffset;
        width -= GraphicsEx.captionWidthFix;
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID isnot "true"#
        // #sijapp cond.if modules_TOUCH is "true"#
        int timeWidth = GraphicsEx.softBarFont.stringWidth(time);
        width -= (timeWidth + 4);
        // #sijapp cond.end#
        // #sijapp cond.end#
        int itemWidth = getHeight();
        width -= itemWidth;
        if (width < x) {
            return CAPTION_REGION_NEW_MESSAGE;
        }
        return CAPTION_REGION_GENERAL;
    }
    // #sijapp cond.end#
    private boolean hasMenu(CanvasEx view) {
        return (null != view.getMenu()) || isMainView(view);
    }
    private boolean isMainView(CanvasEx view) {
        return view instanceof VirtualContactList;
    }
}
