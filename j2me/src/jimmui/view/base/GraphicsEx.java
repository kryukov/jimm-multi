/*
 * GraphicsEx.java
 *
 * Created on 15 Ноябрь 2007 г., 0:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.base;

import jimm.Jimm;
import jimmui.view.icons.*;

import javax.microedition.lcdui.*;

/**
 *
 * @author vladimir
 */
public final class GraphicsEx {
    private Graphics gr;
    private static int[] theme = Scheme.getScheme();

    public final int softbarOffset;

    public  static Font[] chatFontSet;
    public  static Font[] contactListFontSet;
    public  static Font[] popupFontSet;
    public  static Font menuFont;
    public  static Font statusLineFont;
    public static Font captionFont;
    public static Font softBarFont;

    public static final int captionOffset;
    public static final int captionWidthFix;
    public static int captionHeight;

    public GraphicsEx() {
        if (null != Scheme.softbarImage) {
            softbarOffset = Scheme.softbarImage.getHeight() / 3;
        } else {
            softbarOffset = 1;
        }
    }
    public void setGraphics(Graphics graphics) {
        gr = graphics;
    }



    public void setThemeColor(byte object) {
        gr.setColor(theme[object]);
    }

    public int getThemeColor(byte object) {
        return theme[object];
    }
    public boolean notEqualsColor(byte object1, byte object2) {
        return theme[object1] != theme[object2];
    }

    private static Font[] createFontSet(int fontSize) {
        Font[] fontSet = new Font[2];
        fontSet[CanvasEx.FONT_STYLE_PLAIN] = createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  fontSize);
        fontSet[CanvasEx.FONT_STYLE_BOLD]  = createFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,   fontSize);
        return fontSet;
    }

    private static Font createPlainFont(int size) {
        return createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, size);
    }
    private static Font createFont(int face, int style, int size) {
        return Font.getFont(face, style, size);
    }

    private static int size2font(int size) {
        switch (Math.max(0, Math.min(size, 2))) {
            case 0: return Font.SIZE_SMALL;
            case 1: return Font.SIZE_MEDIUM;
            case 2: return Font.SIZE_LARGE;
        }
        return Font.SIZE_SMALL;
    }
    private static int num2size(int num, boolean chat) {
        switch (num) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 2;
        }
        return 1;
    }
    public static void setFontScheme(int num) {
        int[] sizes = new int[7];
        sizes[0] = size2font(num2size(num, true));
        sizes[1] = size2font(num2size(num, false));
        sizes[2] = size2font(num2size(num, true));
        sizes[3] = size2font(num2size(num, false) - 1);
        int systemSize = (num < 2) ? 0 : 1;
        // #sijapp cond.if modules_TOUCH is "true"#
        systemSize = num2size(num, true);
        // #sijapp cond.end#
        sizes[4] = size2font(systemSize);
        sizes[5] = size2font(systemSize);
        sizes[6] = size2font(systemSize);

        chatFontSet        = createFontSet(sizes[0]);
        contactListFontSet = createFontSet(sizes[1]);
        popupFontSet       = createFontSet(sizes[2]);

        statusLineFont = createPlainFont(sizes[3]);
        menuFont       = createPlainFont(sizes[4]);
        captionFont    = createFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, sizes[5]);
        softBarFont    = createFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, sizes[6]);
    }
    ////////////////////////////////////////////////////////////////////////////
    public void setThemeColor(byte colorObject, byte bgObject, int transparency) {
        setColor(getThemeColor(colorObject), getThemeColor(bgObject), transparency);
    }
    public void setColor(int color, int bg, int transparency) {
        if (0 != (color & 0xFF000000)) transparency = color >>> 24;
        gr.setColor(transparentPart(color, transparency) + transparentPart(bg, 256 - transparency));
    }
    private int transparentPart(int color, int transparency) {
        int b = (color & 0xFF) * transparency / 256;
        int g = ((color & 0xFF00) >> 8) * transparency / 256;
        int r = ((color & 0xFF0000) >> 16) * transparency / 256;
        int a = ((color & 0xFF0000) >> 24) * transparency / 256;
        return b | (g << 8) | (r << 16) | (a << 24);
    }

    public boolean isDarkColor(int color) {
        int r = ((color & 0xFF0000) >> 16) * 1000 / 256;
        int g = ((color & 0x00FF00) >> 8)  * 1000 / 256;
        int b =  (color & 0x0000FF)        * 1000 / 256;

        int y = (222 * r + 707 * g +  71 * b) / 1000;
        return y < 500;
    }

    public final int lite(int color, int deltaY) {
        int r = ((color & 0xFF0000) >> 16) * 1000 / 256;
        int g = ((color & 0x00FF00) >> 8)  * 1000 / 256;
        int b =  (color & 0x0000FF)        * 1000 / 256;

        int x = (431 * r + 342 * g + 178 * b) / 1000;
        int y = (222 * r + 707 * g +  71 * b) / 1000;
        int z = ( 20 * r + 130 * g + 939 * b) / 1000;

        y = Math.max(0, Math.min(y + deltaY, 1000));

        r = 3063 * x - 1393 * y -  476 * z;
        g = -969 * x + 1876 * y +   42 * z;
        b =   68 * x -  229 * y + 1069 * z;

        r = component(r * 256 / 1000000);
        g = component(g * 256 / 1000000);
        b = component(b * 256 / 1000000);
        return b | (g << 8) | (r << 16);
    }

    private int component(int c) {
        return Math.max(0, Math.min(c, 255));
    }

    // change light of color
    public int transformColorLight(int color, int light) {
        int b = (color & 0xFF) + light;
        int g = ((color & 0xFF00) >> 8) + light;
        int r = ((color & 0xFF0000) >> 16) + light;
        b = Math.min(Math.max(0, b), 255);
        g = Math.min(Math.max(0, g), 255);
        r = Math.min(Math.max(0, r), 255);
        return b | (g << 8) | (r << 16);
    }

    public void drawString(String str, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);
        gr.drawString(str, x, y + height, Graphics.BOTTOM + Graphics.LEFT);
        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawString(String str, int x, int y, int width, int base, int skip, int height) {
        if (null == str) {
            return;
        }
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height - skip);
        gr.drawString(str, x, y + base - skip, Graphics.BOTTOM + Graphics.LEFT);
        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawImage(Image image, int x, int y, int width, int height) {
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);
        int imgWidth = image.getWidth();
        if (width < imgWidth) {
            gr.drawImage(image, x + (width - imgWidth) / 2, y, Graphics.TOP | Graphics.LEFT);
        } else {
            for (int offsetX = 0; offsetX < width; offsetX += imgWidth) {
                gr.drawImage(image, x + offsetX, y, Graphics.TOP | Graphics.LEFT);
            }
        }
        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawImageInCenter(Image image, int x, int y, int width, int height) {
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);
        gr.drawImage(image, x + (width - image.getWidth()) / 2, y, Graphics.TOP | Graphics.LEFT);
        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawString(Icon[] lIcons, String str, Icon[] rIcons, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }

        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);

        int lWidth = drawImages(lIcons, x, y, height);
        if (lWidth > 0) {
            lWidth++;
        }
        int rWidth = getImagesWidth(rIcons);
        if (rWidth > 0) {
            rWidth++;
        }
        drawImages(rIcons, x + width - rWidth, y, height);
        gr.setClip(x + lWidth, y, width - lWidth - rWidth, height);
        gr.drawString(str, x + lWidth, y + (height - gr.getFont().getHeight()) / 2,
                Graphics.LEFT + Graphics.TOP);

        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public int drawImage(Icon icon, int x, int y, int height) {
        if (null != icon) {
            icon.drawByLeftTop(gr, x, y + (height - icon.getHeight()) / 2);
            return icon.getWidth();
        }
        return 0;
    }
    public int drawImages(Icon[] icons, int x, int y, int height) {
        int width = 0;
		if (null != icons) {
            for (int i = 0; i < icons.length; ++i) {
                if (null != icons[i]) {
                    icons[i].drawByLeftTop(gr, x + width, y + (height - icons[i].getHeight()) / 2);
                    width += icons[i].getWidth() + 1;
                }
            }
		}
        return width - 1;
    }

    public int getImagesWidth(Icon[] icons) {
        int width = 0;
        int correction = 0;
		if (null != icons) {
            for (int i = 0; i < icons.length; ++i) {
                if (null != icons[i]) {
                    width += icons[i].getWidth() + 1;
                    correction = -1;
                }
            }
		}
        return width + correction;
    }
    public static int getMaxImagesHeight(Icon[] images) {
        if (null == images) {
            return 0;
        }
        int height = 0;
        for (int i = 0; i < images.length; ++i) {
            if (null != images[i]) {
                height = Math.max(height, images[i].getHeight());
            }
        }
        return height;
    }

    public void fillGradRect(int color1, int color2, int x, int y, int width, int height) {
        if (true) {
            // #sijapp cond.if modules_ANDROID is "true"#
            gr.fillGradient(x, y, width, height, color1, color2);
            return;
            // // #sijapp cond.else#
            //setColor(color1);
            //fillRect(x, y, width, height);
            // #sijapp cond.end#
        }
        int r1 = ((color1 & 0xFF0000) >> 16);
        int g1 = ((color1 & 0x00FF00) >> 8);
        int b1 =  (color1 & 0x0000FF);
        int dr2 = ((color2 & 0xFF0000) >> 16) - r1;
        int dg2 = ((color2 & 0x00FF00) >> 8) - g1;
        int db2 =  (color2 & 0x0000FF) - b1;

        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);

        int x2 = x + width;
        for (int i = 0; i < height; ++i) {
            gr.setColor(i * dr2 / height + r1, i * dg2 / height + g1, i * db2 / height + b1);
            gr.drawLine(x, y + i, x2, y + i);
        }

        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public void drawBarBack(int y, int height, Image back, int width) {
        if (height <= 0) return;
        if (null == back) {
            int capBkColor = getThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            fillGradRect(capBkColor, transformColorLight(capBkColor, -32), 0, y, width, height);
            setColor(transformColorLight(capBkColor, -128));
            int lineY = (0 == y) ? (height - 1) : y;
            drawLine(0, lineY, width, lineY);
        } else {
            drawImage(back, 0, y, width, height);
        }
    }
    public static int getSoftBarSize() {
        if (null != Scheme.softbarImage) {
            return Scheme.softbarImage.getHeight();
        }
        return Math.max(CanvasEx.minItemHeight, softBarFont.getHeight() + 2);
    }

    public void fillRect(int x, int y, int width, int height, byte sback) {
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x, y, width, height);

        setThemeColor(sback);
        gr.fillRect(x, y, width, height);

        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    private static final int SHADOW_SIZE = 2;
    public void drawDoubleBorder(int x, int y, int width, int height, byte sborder) {
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(x - SHADOW_SIZE, y - SHADOW_SIZE, width + SHADOW_SIZE * 2, height + SHADOW_SIZE * 2);

        setThemeColor(sborder);
        gr.drawLine(x + width, y - 1, x + width, y + height);
        gr.drawLine(x - 1, y - 1, x - 1, y + height);
        gr.fillRect(x - SHADOW_SIZE, y, SHADOW_SIZE, height);
        gr.fillRect(x + width, y, SHADOW_SIZE, height);
        gr.fillRect(x, y - SHADOW_SIZE, width, SHADOW_SIZE);
        gr.fillRect(x, y + height, width, SHADOW_SIZE);

        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void setStrokeStyle(int style) {
        gr.setStrokeStyle(style);
    }

    public final void fillRect(int x, int y, int width, int height) {
        gr.fillRect(x, y, width, height);
    }

    public final void fillRect(int left, int top, int width, int height, Image img) {
        int clipX = gr.getClipX();
        int clipY = gr.getClipY();
        int clipHeight = gr.getClipHeight();
        int clipWidth = gr.getClipWidth();
        gr.setClip(left, top, width, height);

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        for (int x = 0; x < width; x += imgWidth) {
            for (int y = 0; y < height; y += imgHeight) {
                gr.drawImage(img, x + left, y + top, Graphics.TOP | Graphics.LEFT);
            }
        }

        gr.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void drawImage(Image image, int x, int y, int anchor) {
        gr.drawImage(image, x, y, anchor);
    }

    public final void setColor(int color) {
        gr.setColor(color);
    }


    public final void drawLine(int x1, int y1, int x2, int y2) {
        gr.drawLine(x1, y1, x2, y2);
    }

    public final int getColor() {
        return gr.getColor();
    }

    public final void drawRect(int x, int y, int width, int height) {
        gr.drawRect(x, y, width, height);
    }
    public final void drawSimpleRect(int x, int y, int width, int height) {
        gr.drawLine(x,         y + 1, x,         y + height - 1);
        gr.drawLine(x + width, y + 1, x + width, y + height - 1);
        gr.drawLine(x + 1, y,          x + width - 1, y);
        gr.drawLine(x + 1, y + height, x + width - 1, y + height);
    }

    public final void drawString(String str, int x, int y, int anchor) {
        gr.drawString(str, x, y, anchor);
    }

    public final void drawByLeftTop(Icon icon, int x, int y) {
        if (null != icon) {
            icon.drawByLeftTop(gr, x, y);
        }
    }

    public final void drawInCenter(Icon icon, int x, int y) {
        if (null != icon) {
            icon.drawInCenter(gr, x, y);
        }
    }


    public final int getClipY() {
        return gr.getClipY();
    }

    public final int getClipX() {
        return gr.getClipX();
    }

    public final int getClipWidth() {
        return gr.getClipWidth();
    }

    public final int getClipHeight() {
        return gr.getClipHeight();
    }

    public final void setClip(int x, int y, int width, int height) {
        gr.setClip(x, y, width, height);
    }
    public final void clipRect(int x, int y, int width, int height) {
        gr.clipRect(x, y, width, height);
    }

    public final Graphics getGraphics() {
        return gr;
    }

    public final void setFont(Font font) {
        gr.setFont(font);
    }

    // #sijapp cond.if target is "MIDP2"#
    static {
        if (jimm.Jimm.getJimm().phone.isPhone(jimm.PhoneInfo.PHONE_NOKIA_N80)) {
            captionOffset = 30;
        } else if (jimm.Jimm.getJimm().phone.isPhone(jimm.PhoneInfo.PHONE_NOKIA)) {
            captionOffset = 20;
        } else {
            captionOffset = 0;
        }
        if (jimm.Jimm.getJimm().phone.isPhone(jimm.PhoneInfo.PHONE_SE)) {
            captionWidthFix = 25;
        } else if (jimm.Jimm.getJimm().phone.isPhone(jimm.PhoneInfo.PHONE_SAMSUNG)) {
            captionWidthFix = 20;
        } else {
            captionWidthFix = 0;
        }
    }
    // #sijapp cond.end#
    public static int calcCaptionHeight(Icon[] icons, String text) {
        if (null != Scheme.captionImage) {
            return Scheme.captionImage.getHeight();
        }

        int height = captionFont.getHeight();
        if (null != icons) {
            for (int i = 0; i < icons.length; ++i) {
                if (null != icons[i]) {
                    height = Math.max(height, icons[i].getHeight());
                }
            }
        }
        height = Math.max(CanvasEx.minItemHeight, height + 2 + 1);
        GraphicsEx.captionHeight = Math.max(GraphicsEx.captionHeight, height);
        return height;
    }
    public void reset() {
        gr = null;
    }

    public int drawPopup(CanvasEx screen, int captionHeight) {
        Popup p = Jimm.getJimm().getDisplay().getNativeCanvas().getPopup();
        if ((null == p) || (screen != p.getCanvas())) {
            return 0;
        }
        return p.paint(this, captionHeight);
    }
}