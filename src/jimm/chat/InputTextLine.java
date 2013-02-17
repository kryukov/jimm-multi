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
 File: this
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Vladimir Kryukov
 *******************************************************************************/

package jimm.chat;
import DrawControls.text.*;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.comm.Config;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class InputTextLine implements ActionListener {
    private boolean qwerty = false;
    private int height = 0;
    private int width = 0;
    private int y = 0;
    private int topLine = 0;
    private final int LINES = 2;
    private StringBuffer content = new StringBuffer();
    private Par text = new Par();
    private boolean visible = false;
    private boolean upperCase = false;

    private String inMap;
    private String[] outMap;

    // qwerty keyboard: shift en ru
    private int qwertyUseMapKey = 0;
    private int upperCaseKey = NativeCanvas.ABC_KEY;
    private int keyMenu = 0;
    private boolean qwertyUseMap = false;

    // phone keyboard: keys
    private int keySmile = Canvas.KEY_STAR;
    private int keyDelete = NativeCanvas.RIGHT_SOFT;

    private static final byte MODE_L = 0; // abc
    private static final byte MODE_CAPS = 1; // Abc
    private byte mode = MODE_L;
    private int charNum = 0;
    private final int SKIP_KEYS = 5;
    private static final String[] modeStrings = {"abc", "Abc"};


    public InputTextLine() {
    }
    private String arrayToString(String[] arr) {
        StringBuffer out = new StringBuffer();
        for (int i = SKIP_KEYS; i < arr.length; ++i) {
            out.append(arr[i]);
        }
        return out.toString();
    }
    private int getCharCount(char ch) {
        if (!qwerty || qwertyUseMap) {
            int index = inMap.indexOf(ch);
            if (-1 < index) {
                return outMap[index + SKIP_KEYS].length();
            }
        }
        return 1;
    }
    private char getChar(char ch, int numChar) {
        int index = inMap.indexOf(ch);
        if (-1 < index) {
            ch = outMap[index + SKIP_KEYS].charAt(numChar);
        }
        return ch;
    }

    public void setVisible(boolean v) {
        visible = v;
        boolean oldQwerty = qwerty;
        qwerty = (1 == Options.getInt(Options.OPTION_KEYBOARD));
        if (visible && ((qwerty != oldQwerty) || (null == inMap))) {
            try {
                Config keyMap = new Config();
                keyMap.loadLocale(qwerty ? "/qwerty-map.txt" : "/phone-map.txt");
                String[] values = keyMap.getValues();
                inMap  = arrayToString(keyMap.getKeys());
                outMap = keyMap.getValues();
                if (qwerty) {
                    qwertyUseMapKey = Util.strToIntDef(values[0], 0);
                    upperCaseKey = Util.strToIntDef(values[1], NativeCanvas.ABC_KEY);
                    keyDelete = Util.strToIntDef(values[2], 8);
                    keySmile = Util.strToIntDef(values[3], 0);
                    keyMenu = Util.strToIntDef(values[4], 12);
                } else {
                    keyDelete = Util.strToIntDef(values[2], NativeCanvas.RIGHT_SOFT);
                    keySmile = Util.strToIntDef(values[3], Canvas.KEY_STAR);
                }
                System.out.println("in " + inMap);
            } catch (Exception e) {
                visible = false;
            }
        }
    }
    public boolean isVisible() {
        return visible;
    }

    public int getHeight() {
        return visible ? height : 0;
    }

    public int getRealHeight() {
        Font font = GraphicsEx.chatFontSet[CanvasEx.FONT_STYLE_PLAIN];
        return font.getHeight() * LINES + 2;
    }
    public void setSize(int y, int width, int height) {
        this.y = y;
        this.width = width;
        this.height = getRealHeight();
    }

    private String getTextLine(int num) {
        if (num < text.getLineCount()) {
            return text.getFirstTextAt(num);
        }
        return "";
    }
    public void paint(Graphics g) {
        if (!visible) {
            return;
        }
        Font font = GraphicsEx.chatFontSet[CanvasEx.FONT_STYLE_PLAIN];
        g.setStrokeStyle(Graphics.SOLID);
        g.setFont(font);

        final int modeWidth = font.charWidth('#') * 4;
        g.setColor(0xFFFFFF);
        g.setClip(width - modeWidth, y - font.getHeight(), modeWidth, font.getHeight());
        int d = Math.max(4, font.getHeight() * 30 / 100);
        g.fillRoundRect(width - modeWidth, y - font.getHeight(), modeWidth + d, font.getHeight() + d, d, d);
        g.setColor(0x000000);
        g.drawRoundRect(width - modeWidth, y - font.getHeight(), modeWidth + d, font.getHeight() + d, d, d);
        g.setColor(0x404040);
        g.drawString(modeStrings[mode], width - modeWidth + d, y - font.getHeight(), Graphics.TOP | Graphics.LEFT);

        g.setClip(0, y, width, height);
        g.setColor(0xFFFFFF);
        g.fillRect(0, y, width, height);
        g.setColor(0x000000);
        g.drawRect(0, y, width, height);
        g.setColor(0xD0D0D0);
        g.drawLine(3, y + font.getHeight() * 1 + 1, width - 6, y + font.getHeight() * 1 + 1);
        g.drawLine(3, y + font.getHeight() * 2 + 1, width - 6, y + font.getHeight() * 2 + 1);
        g.setColor(0x000000);

        final int OFFSET = 5;
        g.setColor(0x000000);
        boolean editableLetter = false && (System.currentTimeMillis() <= lastPressTime + KEY_TIMEOUT);
        for (int lineIndex = 0; lineIndex < LINES; ++lineIndex) {
            String line = getTextLine(topLine + lineIndex);
            int offset = OFFSET;
            int lineY = this.y + font.getHeight() * lineIndex;
            int stopPosition = line.length();
            int cursorPos = ((topLine + lineIndex == cursorLine) ? cursorChar : -1) - 1;
            int cursorOffset = offset;
            for (int i = 0; i < stopPosition; ++i) {
                char ch = line.charAt(i);
                int chWidth = font.charWidth(ch);
                if ((i == cursorPos) && editableLetter) {
                    g.setColor(0x0000FF);
                }
                g.drawChar(ch, offset, lineY + 1, Graphics.TOP | Graphics.LEFT);
                offset += chWidth;
                if (i == cursorPos) {
                    cursorOffset = offset;
                    g.setColor(0x000000);
                }

            }
            if (stopPosition <= cursorPos) {
                cursorOffset = offset + font.charWidth(' ');
            }
            if (-1 <= cursorPos) {
                g.setColor(0x0000FF);
                g.fillRect(cursorOffset + 1, lineY + 2, 2, font.getHeight() - 2);
                g.setColor(0x000000);
            }
        }
    }

    private int key = -1;

    private int pressCount = 0;
    private long lastPressTime = 0;
    private int cursor = 0;
    private int cursorLine = 0;
    private int cursorChar = 0;
    private void setCursor(int pos) {
        cursor = pos;
        cursorLine = 0;
        cursorChar = 0;
        int cursorPos = cursor;
        for (int lineIndex = 0; lineIndex < text.getLineCount(); ++lineIndex) {
            int len = getTextLine(lineIndex).length();
            if (text.isBrAt(lineIndex)) {
                len++;
            }
            if (cursorPos < len + 1) {
                cursorChar = cursorPos;
                if ((cursorPos == len) && text.isBrAt(lineIndex)) {
                    cursorLine++;
                    cursorChar = 0;
                }
                break;
            }
            cursorPos -= len;
            cursorLine++;
        }
        topLine = Math.max(topLine, Math.max(cursorLine - LINES + 1, 0));
        topLine = Math.min(topLine, Math.min(cursorLine,
                Math.max(0, text.getLineCount() - LINES)));
    }


    public void action(CanvasEx canvas, int cmd) {
        // #sijapp cond.if modules_SMILES is "true" #
        if (canvas instanceof Selector) {
            insert(" " + ((Selector)canvas).getSelectedCode() + " ");
        }
        // #sijapp cond.end#
    }

    private void resetCurrentChar() {
        lastPressTime = 0;
        key = -1;
        pressCount = 0;
    }
    private void insert(String text) {
        resetCurrentChar();
        if (cursor < content.length()) {
            content.insert(cursor, text);
        } else {
            setCursor(content.length());
            content.append(text);
        }
        updateUI();
        setCursor(cursor + text.length());
    }
    private void addChar(char ch) {
        if (cursor < content.length()) {
            content.insert(cursor, ch);
        } else {
            setCursor(content.length());
            content.append(ch);
        }
        updateUI();
        setCursor(cursor + 1);
    }
    private void updateUI() {
        Parser parser = new Parser(GraphicsEx.chatFontSet, width - 5 * 2);
        parser.addText(content.toString(), (byte)0, (byte)0);
        text = parser.getPar();
    }


    private char getCurrentChar() {
        char ch = (char)key;
        if (qwerty) {
            if (qwertyUseMap) {
                ch = getChar(ch, pressCount);
            }
            if (upperCase) {
                ch = StringConvertor.toUpperCase(ch);
            }
        } else {
            ch = getChar(ch, pressCount);
            if (MODE_CAPS == mode) {
                ch = StringConvertor.toUpperCase(ch);
            }
        }
        return ch;
    }
    private void setMode(byte mode) {
        this.mode = mode;
        charNum = 0;
    }
    private final static long KEY_TIMEOUT = 600;
    private void autoMode() {
        if (0 == content.length()) {
            setMode(MODE_CAPS);
            return;
        }
        if ((1 < cursor) && (cursor <= content.length())
                && (-1 != " ".indexOf(content.charAt(cursor - 1)))
                && (-1 != ".!?".indexOf(content.charAt(cursor - 2)))) {
            setMode(MODE_CAPS);

        } else if (1 == charNum) {
            setMode(MODE_L);
        }
    }
    private boolean naviKeys(int actionCode) {
        if (NativeCanvas.NAVIKEY_LEFT == actionCode) {
            setCursor(cursor - 1);
            if (-1 == cursor) {
                setCursor(content.length());
            }
            resetCurrentChar();
            return true;
        }
        if (NativeCanvas.NAVIKEY_RIGHT == actionCode) {
            setCursor(cursor + 1);
            if (content.length() < cursor) {
                setCursor(0);
            }
            resetCurrentChar();
            return true;
        }
        return false;
    }


    private boolean __qwertyKey(Chat chat, int keyCode, int type) {
        final long currentTime = System.currentTimeMillis();
        boolean nextChar = (0 == lastPressTime) || (lastPressTime + KEY_TIMEOUT < currentTime);

        int actionCode = ('\n' == keyCode) ? NativeCanvas.NAVIKEY_FIRE : 0;
        if (sysKeys(chat, keyCode, actionCode, type)) {
            return true;
        }
        if ((upperCaseKey == keyCode) && (CanvasEx.KEY_REPEATED != type)) {// shift (-50)
            upperCase = (CanvasEx.KEY_PRESSED == type);
        }
        if (CanvasEx.KEY_PRESSED != type) {
            return false;
        }

        if (qwertyUseMapKey == keyCode) {
            qwertyUseMap = !qwertyUseMap;

        } else if (keyMenu == keyCode) {// main menu
            setVisible(false);

        } else if (keyDelete == keyCode) { // backspace
            if ((0 < content.length()) && (0 < cursor)) {
                setCursor(cursor - 1);
                content.deleteCharAt(cursor);
                updateUI();
            }
            autoMode();

        } else if (keySmile == keyCode) {
            // #sijapp cond.if modules_SMILES is "true" #
            Emotions.selectEmotion(this);
            // #sijapp cond.end #

        } else if (((32 <= keyCode) && (keyCode < 256))
                || (61441 == keyCode) || ('\n' == keyCode)) {
            lastPressTime = currentTime;
            processKey((char)keyCode, nextChar);
            return true;

        } else {
            lastPressTime = 0;
            if (!nextChar) {
                autoMode();
                return true;
            }
            return naviKeys(actionCode);
        }
        lastPressTime = 0;
        return true;
    }

    private void processKey(char keyCode, boolean nextChar) {
        if ((key == keyCode) && !nextChar && (0 < cursor)) {
            int chars = getCharCount(keyCode);
            if (1 < chars) {
                pressCount = (pressCount + 1) % chars;
                content.setCharAt(cursor - 1, getCurrentChar());
                updateUI();
                return;
            }
        }
        autoMode();
        charNum++;

        key = keyCode;
        pressCount = 0;
        addChar(getCurrentChar());
    }
    private boolean phoneKeyPressed(int keyCode, int actionCode) {
        final long currentTime = System.currentTimeMillis();
        boolean nextChar = (0 == lastPressTime) || (lastPressTime + KEY_TIMEOUT < currentTime);

        if ((Canvas.KEY_NUM0 <= keyCode) && (keyCode <= Canvas.KEY_NUM9)) {
            lastPressTime = currentTime;
            processKey((char)keyCode, nextChar);
            return true;
        }
        lastPressTime = 0;
        if (keySmile == keyCode) {
            // #sijapp cond.if modules_SMILES is "true" #
            Emotions.selectEmotion(this);
            // #sijapp cond.end #
            return true;
        }

        if (keyDelete == keyCode) {
            keyCode = NativeCanvas.CLEAR_KEY;
        }
        if (NativeCanvas.CLEAR_KEY == keyCode) {
            resetCurrentChar();
            if ((0 < content.length()) && (0 < cursor)) {
                setCursor(cursor - 1);
                content.deleteCharAt(cursor);
                updateUI();
            }
            autoMode();
            return true;
        }
        if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
            return false;
        }
        if (!nextChar) {
            autoMode();
            return true;
        }
        return naviKeys(actionCode);
    }
    private boolean phoneKey(Chat chat, int keyCode, int actionCode, int type) {
        if (Canvas.KEY_POUND == keyCode) {
            if (CanvasEx.KEY_PRESSED == type) {
                setMode((byte) ((mode + 1) % modeStrings.length));
                chat.invalidate();
            }
            return true;
        }
        if ((CanvasEx.KEY_PRESSED == type) || (CanvasEx.KEY_REPEATED == type)) {
            if (phoneKeyPressed(keyCode, actionCode)) {
                chat.invalidate();
                return true;
            }
        }
        return sysKeys(chat, keyCode, actionCode, type);
    }


    private boolean sysKeys(Chat chat, int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_PRESSED == type) {
            switch (keyCode) {
                case NativeCanvas.CAMERA_KEY:
                case NativeCanvas.CALL_KEY:
                    chat.onMessageSelected();
                    return true;
            }
        }
        if (CanvasEx.KEY_RELEASED == type) {
            if (NativeCanvas.NAVIKEY_FIRE == keyCode) {
                final String data = getString();
                final String address = Chat.ADDRESS + " ";
                if (!data.endsWith(address)) {
                    chat.getProtocol().sendMessage(chat.getContact(), data, true);
                }
                setString("");
                chat.invalidate();
                setVisible(false);
            }
            if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
                return true;
            }
        }
        return false;
    }
    public boolean doKeyReaction(Chat chat, int keyCode, int actionCode, int type) {
        if (!visible) {
            return false;
        }
        return phoneKey(chat, keyCode, actionCode, type);
    }
    boolean qwertyKey(Chat chat, int keyCode, int type) {
        boolean result = __qwertyKey(chat, keyCode, type);
        if (result) {
            chat.invalidate();
        }
        return result;
    }

    public String getString() {
        return content.toString();
    }
    public void setString(String str) {
        content = new StringBuffer(StringConvertor.notNull(str));
        autoMode();
        updateUI();
        setCursor(content.length());
    }

}
