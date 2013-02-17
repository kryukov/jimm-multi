/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-05  Jimm Project

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
File: src/jimm/SplashCanvas.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/
package jimm;

import DrawControls.icons.*;
import DrawControls.text.*;
import javax.microedition.lcdui.*;
import jimm.chat.ChatHistory;
import jimm.comm.*;
import jimm.chat.message.Message;
import jimm.cl.*;
import jimm.ui.base.*;
import jimm.util.*;

public final class SplashCanvas extends CanvasEx {

    // True if keylock has been enabled
    static private final short KEY_LOCK_MSG_TIME = 2000 / NativeCanvas.UIUPDATE_TIME;
    private final Image splash = ImageList.loadImage("/logo.png");
    // Font used to display the logo (if image is not available)
    private final Font logoFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    // Font used to display informational messages
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    /*****************************************************************************/
    private short keyLock = -1;
    private short resetTime = -1;
    private long lastTime = 0;
    // Message to display beneath the splash image
    private String message;
    // Progress in percent
    private volatile int progress;
    // Number of available messages
    private int availableMessages;
    private Icon iconOfMessages;
    // Time since last key # pressed
    private long poundPressTime;

    // Sets the informational message
    public void setMessage(String message) {
        this.message = message;
        this.progress = 0;
        this.invalidate();
    }

    // Sets the current progress in percent (and request screen refresh)
    public void setProgress(int progress) {
        if (progress == this.progress) {
            return;
        }
        this.progress = progress;
        this.invalidate();
    }

    private void setLockMessage() {
        setMessage(JLocale.getString("keylock_enabled"));
    }

    public void lockJimm() {
        keyLock = 0;
        setLockMessage();
        messageAvailable();
        show();
    }

    // Called when message has been received
    public void messageAvailable() {
        availableMessages = ChatHistory.instance.getUnreadMessageCount();
        iconOfMessages = ChatHistory.instance.getUnreadMessageIcon();
        invalidate();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(int x, int y, boolean isLongTap) {
    }

    protected void stylusXMoving(int fromX, int fromY, int toX, int toY) {
        if (Jimm.isLocked()) {
            int region = Math.max(getProgressHeight(), minItemHeight);
            int minY = getH() - region;
            if ((fromY < minY) || (toY < minY)) {
                poundPressTime = 0;
                keyLock = KEY_LOCK_MSG_TIME;
                setProgress(0);
                invalidate();
                return;
            }
            setProgress(Math.max(fromX, toX) * 100 / getW());
        }
    }

    protected void stylusXMoved(int fromX, int fromY, int toX, int toY) {
        int region = Math.max(getProgressHeight(), minItemHeight);
        int minY = getH() - region;
        if ((fromY < minY) || (toY < minY)) {
            poundPressTime = 0;
            keyLock = KEY_LOCK_MSG_TIME;
            setProgress(0);
            invalidate();
            return;
        }
        int x1 = Math.min(fromX, toX);
        int x2 = Math.max(fromX, toX);
        if ((x1 < region) && (getW() - region < x2)) {
            if (Jimm.isLocked()) {
                Jimm.unlockJimm();
                return;
            }
            ContactList.getInstance().activate();
        } else {
            setProgress(0);
        }
    }
    // #sijapp cond.end#

    // Called when a key is pressed
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (!Jimm.isLocked()) return;
        if (Canvas.KEY_POUND == keyCode) {
            long now = System.currentTimeMillis();

            if ((KEY_PRESSED == type) && (0 == poundPressTime)) {
                poundPressTime = now;

            } else if (0 < poundPressTime) {
                long delta = now - poundPressTime;
                long max = 1000;
                if (delta > max * 2) {
                    delta = 0;
                    poundPressTime = 0;
                }
                setProgress((int) Math.min(delta * 100 / max, 100));
                if (delta > max) {
                    Jimm.unlockJimm();
                    poundPressTime = 0;
                }
            }
            return;
        }
        if (KEY_PRESSED == type) {
            setProgress(0);
            poundPressTime = 0;
            keyLock = KEY_LOCK_MSG_TIME;
            invalidate();
        }
    }

    protected void doJimmAction(int keyCode) {
    }

    protected void updateTask(long microTime) {
        boolean repaintIt = false;
        if (0 <= resetTime) {
            if (0 == resetTime) {
                setLockMessage();
                repaintIt = true;
            }

            resetTime--;
        }

        // key lock
        if (0 <= keyLock) {
            if (0 == keyLock) {
                repaintIt = true;
            }
            keyLock--;
        }

        // clock
        long last = lastTime;
        long now = microTime / (60 * 1000);
        lastTime = now;
        if (last != now) {
            repaintIt = true;
        }

        // locking
        if (0 < poundPressTime) {
            long delta = microTime - poundPressTime;
            long max = 1000;
            if (delta > max * 2) {
                delta = 0;
                poundPressTime = 0;
            }
            progress = (int) Math.min(delta * 100 / max, 100);
            repaintIt = true;
        }

        if (repaintIt) {
            invalidate();
        }
    }

    private void showMessage(GraphicsEx g, String msg, int width, int height) {
        final int size_x = width / 10 * 8;
        final int textWidth = size_x - 8;

        Font[] fontSet = GraphicsEx.chatFontSet;
        Parser parser = new Parser(fontSet, textWidth);
        parser.addText(msg, THEME_SPLASH_LOCK_TEXT, FONT_STYLE_PLAIN);
        Par par = parser.getPar();

        final int textHeight = par.getHeight();
        final int size_y = textHeight + 8;
        final int x = width / 2 - (width / 10 * 4);
        final int y = height / 2 - (size_y / 2);
        g.setThemeColor(THEME_SPLASH_LOCK_BACK);
        g.fillRect(x, y, size_x, size_y);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        g.drawRect(x + 2, y + 2, size_x - 5, size_y - 5);
        g.setThemeColor(THEME_SPLASH_LOCK_TEXT);
        par.paint(fontSet, g, x + 4, y + 4, 0, textHeight);
    }

    private int getH() {
        return getScreenHeight();
    }

    private int getW() {
        return getScreenWidth();
    }

    protected void paint(GraphicsEx g) {
        final int height = getH();
        final int width = getW();
        final int progressHeight = getProgressHeight();
        final int fontHeight = font.getHeight();
        final int otherHeight = height - progressHeight;
        // Do we need to draw the splash image?
        if (g.getClipY() < otherHeight) {
            // Draw background
            g.setThemeColor(THEME_SPLASH_BACKGROUND);
            g.fillRect(0, 0, width, height);

            // Display splash image (or text)
            if (null != splash) {
                g.drawImage(splash, width / 2, height / 2, Graphics.HCENTER | Graphics.VCENTER);
            } else {
                g.setThemeColor(THEME_SPLASH_LOGO_TEXT);
                g.setFont(logoFont);
                g.drawString("jimm", width / 2, height / 2 + 5, Graphics.HCENTER | Graphics.BASELINE);
                g.setFont(font);
            }

            // Draw the date
            g.setThemeColor(THEME_SPLASH_DATE);
            g.setFont(font);
            long gmtTime = Jimm.getCurrentGmtTime();
            g.drawString(Util.getLocalDateString(gmtTime, false),
                    width / 2, 12, Graphics.TOP | Graphics.HCENTER);
            g.drawString(Util.getLocalDayOfWeek(gmtTime),
                    width / 2, 13 + fontHeight, Graphics.TOP | Graphics.HCENTER);

            // Display message icon, if keylock is enabled
            if (Jimm.isLocked()) {
                if (0 < availableMessages) {
                    Icon icon = iconOfMessages;
                    if (null != icon) {
                        g.drawByLeftTop(icon, 1, otherHeight - fontHeight - 9);
                    }
                    g.setThemeColor(THEME_SPLASH_MESSAGES);
                    g.setFont(font);
                    int x = Message.msgIcons.getWidth() + 4;
                    int y = otherHeight - fontHeight - 5;
                    g.drawString("# " + availableMessages, x, y, Graphics.LEFT | Graphics.TOP);
                }

                // Display the keylock message if someone hit the wrong key
                if (0 < keyLock) {
                    // Init the dimensions
                    String unlockMsg = JLocale.getString("keylock_message");
                    // #sijapp cond.if modules_TOUCH is "true"#
                    if (NativeCanvas.getInstance().hasPointerEvents()) {
                        unlockMsg = JLocale.getString("touchlock_message");
                    }
                    // #sijapp cond.end#
                    showMessage(g, unlockMsg, width, height);
                }
            }
        }

        g.setFont(font);
        g.setClip(0, otherHeight, width, progressHeight);

        // Draw white bottom bar
        g.setStrokeStyle(Graphics.SOLID);
        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.drawLine(0, otherHeight, width, otherHeight);

        g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
        g.drawString(message, (width / 2),
                otherHeight + (progressHeight - fontHeight) / 2,
                Graphics.TOP | Graphics.HCENTER);

        // Draw current progress
        int progressPx = width * progress / 100;
        if (1 < progressPx) {
            g.setClip(0, otherHeight, progressPx, progressHeight);

            g.setThemeColor(THEME_SPLASH_PROGRESS_BACK);
            g.fillRect(0, otherHeight, progressPx, progressHeight);

            g.setThemeColor(THEME_SPLASH_PROGRESS_TEXT);
            // Draw the progressbar message
            g.drawString(message, (width / 2),
                    otherHeight + (progressHeight - fontHeight) / 2,
                    Graphics.TOP | Graphics.HCENTER);
        }
    }

    private int getProgressHeight() {
        return Math.max(font.getHeight() * 3 / 2, minItemHeight);
    }

    public boolean isSoftBarShown() {
        return false;
    }
}
