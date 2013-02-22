package jimm.ui.base;


import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import DrawControls.tree.VirtualContactList;

import javax.microedition.lcdui.Graphics;

/**
 *
 * @author vladimir
 */
public class MyActionBar {

    private Icon[] images;
    private String caption;
    private String ticker;
    private static Icon messageIcon;

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

    public void paint(GraphicsEx g, VirtualList view, int width) {
        final int height = getHeight();
        g.setStrokeStyle(Graphics.SOLID);
        g.setClip(0, 0, width, height + 1);
        g.drawBarBack(0, height, Scheme.captionImage, width);

        if (null != view) {
            view.drawProgress(g, width, height);
        }

        int x = 0;
        // #sijapp cond.if target is "MIDP2"#
        x += GraphicsEx.captionOffset;
        width -= GraphicsEx.captionOffset;
        width -= GraphicsEx.captionWidthFix;
        // #sijapp cond.end#

        g.setThemeColor(CanvasEx.THEME_BACKGROUND);
        // #sijapp cond.if modules_TOUCH is "true"#
        if (null != view) {
            final int itemWidth = getHeight();
            if (hasMenu(view)) {
                width -= drawRight(g, menu, x + width, itemWidth, height);
            }

            if (!isMainView(view)) {
                int iconWidth = drawLeft(g, back, x, itemWidth, height);
                x += iconWidth;
                width -= iconWidth;
            }
        }
        // #sijapp cond.end#
        x += 2;

        Icon ic = messageIcon;
        if (null != ic) {
            width -= drawRight(g, ic, x + width, getHeight(), height);
            width -= 1;
        }

        g.setFont(GraphicsEx.captionFont);
        g.setThemeColor(CanvasEx.THEME_CAP_TEXT);
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
        // #sijapp cond.if modules_TOUCH is "true"#
        g.drawLine(x - defWidth, 0, x - defWidth, height);
        // #sijapp cond.end#
        return defWidth;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    private static final Icon back = ImageList.createImageList("/back_button.png").iconAt(0);
    private static final Icon menu = ImageList.createImageList("/menu_button.png").iconAt(0);
    public void touchTapped(VirtualList view, int x, int width) {
        view.touchCaptionTapped(getCaptionRegion(view, x, width));
    }
    public static final int CAPTION_REGION_BACK = -1;
    public static final int CAPTION_REGION_MENU = -2;
    public static final int CAPTION_REGION_NEW_MESSAGE = -3;
    public static final int CAPTION_REGION_GENERAL = 1;
    protected int getCaptionRegion(VirtualList view, int x, int width) {
        // #sijapp cond.if target is "MIDP2"#
        x += GraphicsEx.captionOffset;
        width -= GraphicsEx.captionOffset;
        width -= GraphicsEx.captionWidthFix;
        // #sijapp cond.end#
        int itemWidth = getHeight();
        if ((x < itemWidth) && !isMainView(view)) {
            return CAPTION_REGION_BACK;
        }
        width -= itemWidth;
        if ((width < x) && hasMenu(view)) {
            return CAPTION_REGION_MENU;
        }
        width -= itemWidth;
        if (width < x) {
            return CAPTION_REGION_NEW_MESSAGE;
        }
        return CAPTION_REGION_GENERAL;
    }
    // #sijapp cond.end#
    private boolean hasMenu(VirtualList view) {
        return (null != view.getMenu()) || isMainView(view);
    }
    private boolean isMainView(VirtualList view) {
        return view instanceof VirtualContactList;
    }
}
