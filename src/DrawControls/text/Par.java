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
Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/
package DrawControls.text;

import DrawControls.icons.Icon;
import javax.microedition.lcdui.*;
import jimm.modules.*;
import jimm.ui.base.CanvasEx;
import jimm.ui.base.GraphicsEx;

/**
 *
 * @author Vladimir Kryukov
 */
public class Par {

    static final short TEXT = 0;
    static final short SMILE = 1;
    static final short IMAGE = 2;
    static final short EOL = 3;
    static final short BR = 4;
    static final short PROGRESS = 5;
    public boolean selectable = true;
    private Object[] objects;
    private short[] lines;

    public Par() {
    }

    void setLines(short[] lines, Object[] objects) {
        this.lines = lines;
        this.objects = objects;
    }

    public void replaceFirstIcon(Icon icon) {
        for (int i = 0; i < objects.length; ++i) {
            if (objects[i] instanceof Icon) {
                objects[i] = icon;
                return;
            }
        }
    }

    public void setProgress(byte progress) {
        lines[lines.length - 1] = progress;
    }

    public int getHeight() {
        return lines[0];
    }

    private String getText(short id) {
        return (String) objects[id];
    }

    private Icon getIcon(short id) {
        return (Icon) objects[id];
    }

    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int from, int size) {
        int ip = 1;
        while (ip < lines.length) {
            int lineHeight = lines[ip];
            if (from < lineHeight) {
                break;
            }
            from -= lineHeight;
            y += lineHeight;
            ip = getLine(1, ip);
        }
        if (lines.length <= ip) {
            return;
        }
        int offset = 0;
        size += from;
        String text = null;
        Icon icon = null;
        int xpos = x;
        int lineHeight = lines[ip++];
        while (ip < lines.length) {
            switch (lines[ip++]) {
                case TEXT:
                    text = getText(lines[ip++]);
                    short tfrom = lines[ip++];
                    short len = lines[ip++];
                    byte fontStyle = (byte) lines[ip++];
                    byte colorType = (byte) lines[ip++];
                    int drawYPos = y + offset + lineHeight - fontSet[fontStyle].getHeight();
                    g.setThemeColor(colorType);
                    g.setFont(fontSet[fontStyle]);
                    g.getGraphics().drawSubstring(text, tfrom, len,
                            xpos, drawYPos, Graphics.TOP | Graphics.LEFT);
                    xpos += lines[ip++];
                    break;

                case SMILE:
                    icon = Emotions.instance.getSmileIcon(lines[ip++]);
                    g.drawByLeftTop(icon, xpos, y + offset + lineHeight - icon.getHeight());
                    xpos += icon.getWidth();
                    break;

                case IMAGE:
                    icon = getIcon(lines[ip++]);
                    g.drawByLeftTop(icon, xpos, y + offset + (lineHeight - icon.getHeight()) / 2);
                    xpos += icon.getWidth() + 2;
                    break;

                case EOL:
                case BR:
                    xpos = x;
                    offset += lineHeight;
                    if ((size <= offset) || (ip == lines.length)) {
                        return;
                    }
                    lineHeight = lines[ip++];
                    break;

                case PROGRESS:
                    g.setThemeColor((byte) lines[ip++]);
                    int width = lines[ip++];
                    width = width * lines[ip++] / 100;
                    int height = fontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight();
                    g.fillRect(xpos, y + offset, width, height);
                    break;
            }
        }
    }

    private int getLine(int index, int from) {
        from++;
        int ip = from;
        while (ip < lines.length) {
            if (0 == index) {
                return ip - 1;
            }
            switch (lines[ip++]) {
                case TEXT:
                    ip += 6;
                    break;

                case SMILE:
                    ip++;
                    break;

                case IMAGE:
                    ip++;
                    break;

                case EOL:
                case BR:
                    index--;
                    ip++;
                    break;

                case PROGRESS:
                    ip += 3;
                    break;
            }
        }
        return lines.length;
    }

    public int getLineCount() {
        int count = 1;
        int ip = 2;
        while (ip < lines.length) {
            switch (lines[ip++]) {
                case TEXT:
                    ip += 6;
                    break;

                case SMILE:
                    ip++;
                    break;

                case IMAGE:
                    ip++;
                    break;

                case EOL:
                case BR:
                    count++;
                    ip++;
                    break;

                case PROGRESS:
                    ip += 3;
                    break;
            }
        }
        return count;
    }

    public String getText() {
        if (null == lines) {
            return null;
        }

        StringBuffer result = new StringBuffer();
        String str;
        int ip = 2;
        while (ip < lines.length) {
            switch (lines[ip++]) {
                case TEXT:
                    str = getText(lines[ip++]);
                    short from = lines[ip++];
                    short len = lines[ip++];
                    ip += 3;
                    result.append(str.substring(from, from + len));
                    break;

                case SMILE:
                    // #sijapp cond.if modules_SMILES is "true" #
                    result.append(Emotions.instance.getSmileText(lines[ip++]));
                    // #sijapp cond.end #
                    break;

                case IMAGE:
                    ip++;
                    break;

                case EOL:
                    ip++;
                    break;

                case BR:
                    result.append("\n");
                    ip++;
                    break;

                case PROGRESS:
                    ip += 3;
                    break;
            }
        }
        String retval = result.toString().trim();
        return (retval.length() == 0) ? null : retval;
    }

    public String getFirstTextAt(int line) {
        int l = getLine(line, 1) + 1;
        if ((l + 3 < lines.length) && (0 == lines[l + 0])) {
            return getText(lines[l + 1]).substring(lines[l + 2], lines[l + 2] + lines[l + 3]);
        }
        return "";
    }

    public boolean isBrAt(int line) {
        int ip = getLine(line, 1) + 1;
        while (ip < lines.length) {
            switch (lines[ip++]) {
                case TEXT:
                    ip += 6;
                    break;

                case SMILE:
                    ip++;
                    break;

                case IMAGE:
                    ip++;
                    break;

                case EOL:
                    return false;

                case BR:
                    return true;

                case PROGRESS:
                    ip += 3;
                    break;
            }
        }
        return false;
    }
}
