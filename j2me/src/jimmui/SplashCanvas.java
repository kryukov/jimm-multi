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
package jimmui;

import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.view.base.touch.*;
import jimmui.view.icons.*;
import jimmui.view.text.*;
import javax.microedition.lcdui.*;

import jimm.comm.*;
import jimmui.view.base.*;
import jimm.util.*;
import protocol.ui.InfoFactory;

import java.util.Vector;

public final class SplashCanvas extends CanvasEx {

    // True if keylock has been enabled
    static private final short KEY_LOCK_MSG_TIME = 2000 / NativeCanvas.UIUPDATE_TIME;
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
        int count = 0;
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        for (int i = chats.size() - 1; 0 <= i; --i) {
            count += ((ChatModel) chats.elementAt(i)).getUnreadMessageCount();
        }
        availableMessages = count;
        iconOfMessages = Jimm.getJimm().getCL().getUnreadMessageIcon();
        invalidate();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusTap(TouchState state) {
    }

    protected void stylusXMoving(TouchState state) {
        if (Jimm.getJimm().isLocked()) {
            int region = Math.max(getProgressHeight(), minItemHeight);
            int minY = getHeight() - region;
            if ((state.fromY < minY) || (state.y < minY)) {
                poundPressTime = 0;
                keyLock = KEY_LOCK_MSG_TIME;
                setProgress(0);
                invalidate();
                return;
            }
            setProgress(Math.max(state.fromX, state.y) * 100 / getWidth());
        }
    }

    protected void stylusXMoved(TouchState state) {
        int region = Math.max(getProgressHeight(), minItemHeight);
        int minY = getHeight() - region;
        if ((state.fromY < minY) || (state.y < minY)) {
            poundPressTime = 0;
            keyLock = KEY_LOCK_MSG_TIME;
            setProgress(0);
            invalidate();
            return;
        }
        int x1 = Math.min(state.fromX, state.x);
        int x2 = Math.max(state.fromX, state.x);
        if ((x1 < region) && (getWidth() - region < x2)) {
            if (Jimm.getJimm().isLocked()) {
                Jimm.getJimm().unlockJimm();
                return;
            }
            Jimm.getJimm().getCL().activate();
        } else {
            setProgress(0);
        }
    }
    // #sijapp cond.end#

    // Called when a key is pressed
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (!Jimm.getJimm().isLocked()) return;
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
                    Jimm.getJimm().unlockJimm();
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

    protected void paint(GraphicsEx g) {
        final int height = getHeight();
        final int width = getWidth();
        final int progressHeight = getProgressHeight();
        final int fontHeight = font.getHeight();
        final int otherHeight = height - progressHeight;
        // Do we need to draw the splash image?
        if (g.getClipY() < otherHeight) {
            // Draw background
            g.setThemeColor(THEME_SPLASH_BACKGROUND);
            g.fillRect(0, 0, width, height);

            // Display splash image (or text)
            g.setThemeColor(THEME_SPLASH_LOGO_TEXT);
            g.setFont(logoFont);
            g.drawString("Jimm Multi", width / 2, height / 2 + 5, Graphics.HCENTER | Graphics.BASELINE);
            g.setFont(font);

            // Draw the date
            g.setThemeColor(THEME_SPLASH_DATE);
            g.setFont(font);
            long gmtTime = Jimm.getCurrentGmtTime();
            g.drawString(Util.getLocalDateString(gmtTime, false),
                    width / 2, 12, Graphics.TOP | Graphics.HCENTER);
            g.drawString(Util.getLocalDayOfWeek(gmtTime),
                    width / 2, 13 + fontHeight, Graphics.TOP | Graphics.HCENTER);

            // Display message icon, if keylock is enabled
            if (Jimm.getJimm().isLocked()) {
                if (0 < availableMessages) {
                    Icon icon = iconOfMessages;
                    if (null != icon) {
                        g.drawByLeftTop(icon, 1, otherHeight - fontHeight - 9);
                    }
                    g.setThemeColor(THEME_SPLASH_MESSAGES);
                    g.setFont(font);
                    int x = InfoFactory.msgIcons.getWidth() + 4;
                    int y = otherHeight - fontHeight - 5;
                    g.drawString("# " + availableMessages, x, y, Graphics.LEFT | Graphics.TOP);
                }

                // Display the keylock message if someone hit the wrong key
                if (0 < keyLock) {
                    // Init the dimensions
                    String unlockMsg = JLocale.getString("keylock_message");
                    // #sijapp cond.if modules_TOUCH is "true"#
                    if (Jimm.getJimm().getDisplay().hasPointerEvents()) {
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
}
