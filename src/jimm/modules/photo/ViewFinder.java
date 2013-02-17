/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *getName
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/modules/photo/ViewFinder.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

// #sijapp cond.if modules_FILES="true"#
package jimm.modules.photo;

// #sijapp cond.if modules_ANDROID isnot "true" #
import DrawControls.text.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.base.*;
import jimm.util.JLocale;

/** ************************************************************************* */
/** ************************************************************************* */
// Class for viewfinder
public class ViewFinder extends Canvas implements Runnable {
    // Variables
    private Player player = null;
    private VideoControl videoControl = null;
    private Par errorMessage = null;
    private byte[] data;

    private static final byte STATE_CAPTURE = 0;
    private static final byte STATE_PREEVIEW = 1;
    private static final byte STATE_DONE = 2;
    private byte state = STATE_CAPTURE;
    private static int takePhotoMethod = 0;
    private Image thumbnailImage = null;

    private PhotoListener listener;
    private final GraphicsEx gx = new GraphicsEx();
    // #sijapp cond.if modules_TOUCH is "true"#
    private int touchKey = 0;
    private int touchStartX = 0;
    // #sijapp cond.end#

    private static int useSize = getPhotoSize();
    private static final String[] sizes = {"", "320x240",
            "480x640", "640x480",
            "1280x960", "2048x1536"};
    private static final String[] keys = {"", "width=320&height=240",
            "width=480&height=640", "width=640&height=480",
            "width=1280&height=960", "width=2048&height=1536"};

    private int width = 0;
    private int height = 0;
    protected void sizeChanged(int w, int h) {
        if ((0 == w) || (0 == h)) return;
        boolean prev = width < height;
        boolean next = w < h;
        if (prev != next) {
            width = w;
            height = h;
            if (STATE_CAPTURE == state) {
                start();
                repaint();
            }
        }
    }
    private static int getPhotoSize() {
        if (Jimm.isPhone(Jimm.PHONE_NOKIA)) {
            return 1;
        }
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            return 3;
        }
        return 0;
    }
    public void show() {
        Jimm.getJimm().getDisplay().show(this);
        width = getWidth();
        height = getHeight();
        start();
    }
    public void setPhotoListener(PhotoListener l) {
        listener = l;
    }
    private void back() {
        dismiss();
        Jimm.getJimm().getDisplay().back(this);
    }

    public ViewFinder() {
    }
    private void setError(JimmException err) {
        state = STATE_PREEVIEW;
        Parser parser = new Parser(GraphicsEx.contactListFontSet, getWidth() * 8 / 10);
        byte color = CanvasEx.THEME_CAP_TEXT;
        if (0 < (gx.getThemeColor(CanvasEx.THEME_CAP_TEXT) & 0x808080)) {
            color = CanvasEx.THEME_CAP_BACKGROUND;
        }
        parser.addText(err.getMessage(), color, CanvasEx.FONT_STYLE_PLAIN);
        errorMessage = parser.getPar();
        repaint();
    }

    public void paint(Graphics g) {
        gx.setGraphics(g);
        int captionHeight = GraphicsEx.calcCaptionHeight(null, "");
        int softBarHeight = gx.getSoftBarSize();
        int scrWidth = getWidth();
        int scrHeight = getHeight() - softBarHeight - captionHeight;
        String caption = (STATE_CAPTURE == state) ? "viewfinder" : "send_img";
        caption = JLocale.getString(caption);
        if (0 < useSize) {
            caption += " " + sizes[useSize];
        }

        g.setColor(0xffffffff);
        g.fillRect(0, captionHeight, scrWidth, scrHeight);

        g.setColor(0x00000000);
        if (STATE_PREEVIEW == state) {
            if (null != errorMessage) {
                errorMessage.paint(GraphicsEx.contactListFontSet, gx, scrWidth / 10,
                        (scrHeight - errorMessage.getHeight()) / 2,
                        0, errorMessage.getHeight());

            } else if (null != thumbnailImage) {
                g.drawImage(thumbnailImage, scrWidth / 2, scrHeight / 2, Graphics.VCENTER | Graphics.HCENTER);

            } else {
                g.drawString("...", scrWidth / 2 - 5, scrHeight / 2, Graphics.TOP | Graphics.LEFT);
            }
        }

        gx.drawBarBack(0, captionHeight, Scheme.captionImage, getWidth());
        gx.drawCaption(null, caption, null, captionHeight, getWidth());

        gx.drawSoftBar((null == errorMessage) ? JLocale.getString("ok") : "",
                "", JLocale.getString("back"),
                softBarHeight, getWidth(), getHeight() - gx.getSoftBarSize());
    }

    private VideoControl createPlayer(String url) throws IOException, MediaException {
        player = Manager.createPlayer(url);
        player.realize();
        return (VideoControl) player.getControl("VideoControl");
    }

    private void updateVideoLocation(VideoControl vControl) {
        if (null == vControl) return;

        vControl.setVisible(false);
        int captionHeight = GraphicsEx.calcCaptionHeight(null, "");
        int softBarHeight = gx.getSoftBarSize();
        int canvasWidth = getWidth();
        int canvasHeight = getHeight() - softBarHeight - captionHeight;
        try {
            vControl.setDisplayLocation(0, captionHeight);
            vControl.setDisplaySize(canvasWidth, canvasHeight);

            int displayWidth = vControl.getDisplayWidth();
            int displayHeight = vControl.getDisplayHeight();
            int x = (canvasWidth - displayWidth) / 2;
            int y = captionHeight + (canvasHeight - displayHeight) / 2;

            vControl.setDisplayLocation(x, y);
        } catch (MediaException me) {
            try {
                vControl.setDisplayFullScreen(true);
            } catch (MediaException me2) {
            }
        }
        vControl.setVisible(true);
    }
    private void initVideo() throws JimmException {
        reset();
        try {
            VideoControl vControl = null;
            // Create the player
            // #sijapp cond.if target is "MIDP2" #
            try {
                if (Jimm.isPhone(Jimm.PHONE_NOKIA_S40)) {
                    vControl = createPlayer("capture://image");
                }
            } catch (Exception e) {
            }
            // #sijapp cond.end #
            if (null == vControl) {
                vControl = createPlayer("capture://video");
            }
            if (null == vControl) {
                throw new JimmException(180, 0);
            }
            vControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
            updateVideoLocation(vControl);
            player.start();
            videoControl = vControl;
        } catch (IOException ioe) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (ioe)", ioe);
            // #sijapp cond.end#
            throw new JimmException(181, 0);
        }  catch (MediaException me) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (me)", me);
            // #sijapp cond.end#
            throw new JimmException(181, 1);
        }  catch (SecurityException se) {
            throw new JimmException(181, 2);
        }  catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (e)", e);
            // #sijapp cond.end#
            throw new JimmException(181, 3);
        }
    }
    // start the viewfinder
    private synchronized void start() {
        state = STATE_CAPTURE;
        stop();
        try {
            initVideo();
        }  catch (JimmException e) {
            stop();
            setError(e);
        }
    }
    private synchronized void reset() {
        thumbnailImage = null;
        data = null;
    }

    // stop the viewfinder
    private synchronized void stop() {
        if (null != videoControl) {
            try {
                videoControl.setVisible(false);
                // Remove video control at SE phones placing it beyond screen border
                // #sijapp cond.if target is "MIDP2" #
                if (Jimm.isPhone(Jimm.PHONE_SE)) {
                    videoControl.setDisplayLocation(1000, 1000);
                }
                // #sijapp cond.end #
            } catch (Exception e) {
            }
        }
        videoControl = null;

        if (null != player) {
            try {
                player.stop();
            } catch (Exception e) {
            }
            try {
                player.close();
            } catch (Exception e) {
            }
        }
        player = null;
        Jimm.gc();
    }

    private byte[] getSnapshot(String type) {
        try {
            return videoControl.getSnapshot(type);
        } catch (SecurityException e) {
            return null;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            //DebugLog.panic("getSnapshot(" + type + ")", e);
            // #sijapp cond.end#
            return null;
        }
    }

    // take a snapshot form the viewfinder
    private byte[] takeSnapshot() {
        final String[] urls = {"encoding=jpeg&width=320&height=240",
            "encoding=jpeg&width=480&height=640",
            "encoding=jpeg&width=640&height=480",
            "encoding=jpeg",
            "JPEG", null};
        byte[] photo = null;
        if (0 < useSize) {
            return getSnapshot("encoding=jpeg&" + keys[useSize]);
        }
        for (int i = takePhotoMethod; i < urls.length; ++i) {
            if (null != photo) {
                break;
            }
            photo = getSnapshot(urls[i]);
            if (null != photo) {
                takePhotoMethod = i;
                return photo;
            }
        }
        takePhotoMethod = 0;
        return null;
    }
    public void takePhoto() {
        if (null != player) {
            data = null;
            try {
                data = takeSnapshot();
            } catch (OutOfMemoryError e) {
                data = null;
            }

            stop();
            repaint();
            if (null == data) {
                setError(new JimmException(183, 0));

            } else {
                try {
                    thumbnailImage = createImage();
                    repaint();
                } catch (OutOfMemoryError e) {
                    thumbnailImage = null;
                    data = null;
                    setError(new JimmException(183, 1));
                }
            }
        }
    }
    private Image createImage()  {
        Image img = Image.createImage(data, 0, data.length);
        int captionHeight = GraphicsEx.calcCaptionHeight(null, "");
        int softBarHeight = gx.getSoftBarSize();
        int scrWidth = getWidth();
        int scrHeight = getHeight() - softBarHeight - captionHeight;
        return Util.createThumbnail(img, scrWidth, scrHeight);
    }
    public void dismiss() {
        state = STATE_DONE;
        stop();
        data = null;
        thumbnailImage = null;
        listener = null;
    }

    public synchronized void run() {
        takePhoto();
    }

    protected void keyPressed(int key) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        int keyCode = NativeCanvas.getJimmKey(key);
        int action = NativeCanvas.getJimmAction(keyCode, key);
        doKeyPressed(keyCode, action);
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    private int getTouchKey(int x) {
        switch (x / (getWidth() / 3)) {
            case 0: return NativeCanvas.LEFT_SOFT;
            case 1: return NativeCanvas.NAVIKEY_FIRE;
            case 2: return NativeCanvas.RIGHT_SOFT;
        }
        return 0;
    }
    protected void pointerReleased(int x, int y) {
        ContactList.getInstance().userActivity();
        if (y > getClientHeight()) {
            final int key = getTouchKey(x);
            if (key == touchKey) {
                doKeyPressed(key, key);
            }
        } else {
            int delta = x - touchStartX;
            if (getWidth() / 3 < Math.abs(delta)) {
                final int key = (delta < 0) ? NativeCanvas.NAVIKEY_LEFT
                        : NativeCanvas.NAVIKEY_RIGHT;
                doKeyPressed(key, key);
            }
        }

        touchKey = 0;
    }
    protected void pointerPressed(int x, int y) {
        ContactList.getInstance().userActivity();
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        touchKey = (y < getClientHeight()) ? 0 : getTouchKey(x);
        touchStartX = x;
    }
    protected void pointerDragged(int x, int y) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
    }
    private int getClientHeight() {
        return getHeight() - gx.getSoftBarSize();
    }
    // #sijapp cond.end#

    // Key pressed
    public void doKeyPressed(int keyCode, int actionCode) {
        if ((STATE_CAPTURE == state) && (NativeCanvas.CAMERA_KEY == actionCode)) {
            actionCode = NativeCanvas.NAVIKEY_FIRE;
        }
        switch (actionCode) {
            case NativeCanvas.RIGHT_SOFT:
            case NativeCanvas.CLOSE_KEY:
                if ((STATE_CAPTURE == state) || (null != errorMessage)) {
                    back();

                } else if (STATE_PREEVIEW == state) {
                    start();
                }
                break;

            case NativeCanvas.LEFT_SOFT:
            case NativeCanvas.NAVIKEY_FIRE:
                if (STATE_CAPTURE == state) {
                    state = STATE_PREEVIEW;
                    new Thread(this).start();

                } else if (null != thumbnailImage) {
                    stop();
                    try {
                        listener.processPhoto(data);
                        dismiss();
                    } catch (Exception e) {
                        setError(new JimmException(191, 4));
                    }
                }
                break;

            case NativeCanvas.NAVIKEY_LEFT:
                useSize = (useSize - 1 + sizes.length) % sizes.length;
                repaint();
                break;

            case NativeCanvas.NAVIKEY_RIGHT:
                useSize = (useSize + 1) % sizes.length;
                repaint();
                break;
        }
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
