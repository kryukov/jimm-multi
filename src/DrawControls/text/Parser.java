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
import java.util.Vector;
import javax.microedition.lcdui.Font;
import jimm.modules.*;
import jimm.ui.base.CanvasEx;

/**
 *
 * @author Vladimir Kryukov
 */
public final class Parser {

    private Par par;
    private short maxWidth;
    private int minHeight = 0;
    private Vector objects = new Vector();
    private Vector items = new Vector();
    private int lineWidth = 0;
    private Font[] fontSet;

    public Parser(Par par, Font[] fontSet, int width) {
        this.par = par;
        maxWidth = (short) width;
        this.fontSet = fontSet;
    }

    public Parser(Font[] fontSet, int width) {
        par = new Par();
        maxWidth = (short) width;
        this.fontSet = fontSet;
    }
    public void setSelectable(boolean selectable) {
        par.selectable = selectable;
    }

    public void useMinHeight() {
        minHeight = CanvasEx.minItemHeight;
    }

    public void destroy() {
        objects.removeAllElements();
        objects = null;
        items.removeAllElements();
        items = null;
    }

    public Par getPar() {
        commit();
        return par;
    }

    private int getWidth() {
        return maxWidth;
    }

    private int getLastLineWidth() {
        return lineWidth;
    }

    private short addObject(Object obj) {
        objects.addElement(obj);
        return (short) (objects.size() - 1);
    }

    private Icon getIcon(short index) {
        return (Icon) objects.elementAt(index);
    }

    private String getText(short index) {
        return (String) objects.elementAt(index);
    }

    private int getGlyphHeight(short[] item) {
        if (Par.TEXT == item[0]) {
            return fontSet[item[4]].getHeight();
        }
        if (Par.SMILE == item[0]) {
            return Emotions.instance.getSmileIcon(item[1]).getHeight();
        }
        if (Par.IMAGE == item[0]) {
            return getIcon(item[1]).getHeight();
        }
        return fontSet[CanvasEx.FONT_STYLE_PLAIN].getHeight();
    }

    private short[] getLines(Vector glyphs) {
        int size = 1;
        boolean newLine = true;
        final int glyphCount = glyphs.size();

        for (int i = 0; i < glyphCount; ++i) {
            if (newLine) {
                newLine = false;
                size += 1;
            }
            short[] glyph = (short[]) glyphs.elementAt(i);
            size += glyph.length;
            if ((Par.EOL == glyph[0]) || (Par.BR == glyph[0])) {
                newLine = true;
            }
        }

        int height = 0;
        int lineHeight = 0;
        int lineHeightOffset = 1;

        newLine = false;
        short[] data = new short[size];
        data[0] = (short) height;
        int ip = 2;
        for (int i = 0; i < glyphCount; ++i) {
            if (newLine) {
                data[lineHeightOffset] = (short) lineHeight;
                height += lineHeight;
                lineHeight = 0;
                newLine = false;
                lineHeightOffset = ip++;
            }
            short[] glyph = (short[]) glyphs.elementAt(i);
            lineHeight = Math.max(lineHeight, getGlyphHeight(glyph));
            System.arraycopy(glyph, 0, data, ip, glyph.length);
            ip += glyph.length;
            if ((Par.EOL == glyph[0]) || (Par.BR == glyph[0])) {
                newLine = true;
            }
        }
        if (lineHeightOffset < data.length) {
            data[lineHeightOffset] = (short) lineHeight;
            height += lineHeight;
        }
        data[0] = (short) Math.max(height, minHeight);
        return data;
    }

    public void commit() {
        Object[] objs = new Object[objects.size()];
        objects.copyInto(objs);
        par.setLines(getLines(items), objs);
    }
    private static final short[] PAR_BR = new short[]{Par.BR};
    private static final short[] PAR_EOL = new short[]{Par.EOL};

    private void internNewLine(boolean br) {
        items.addElement(br ? PAR_BR : PAR_EOL);
        lineWidth = 0;
    }

    private short[] createText(short text, int from, int to, byte colorType, byte fontStyle, int width) {
        short[] data = new short[7];
        data[0] = Par.TEXT;
        data[1] = text;
        data[2] = (short) from;
        data[3] = (short) (to - from);
        data[4] = fontStyle;
        data[5] = colorType;
        data[6] = (short) width;
        return data;
    }

    private short[] createSmile(short smileIndex) {
        short[] data = new short[2];
        data[0] = Par.SMILE;
        data[1] = smileIndex;
        return data;
    }

    private void addText(short textIndex, int from, int to, byte colorType, byte fontStyle, int width) {
        internAdd(createText(textIndex, from, to, colorType, fontStyle, width), width);
    }

    private void internAdd(short[] item, int width) {
        items.addElement(item);
        lineWidth += width;
    }

    public void doCRLF() {
        internNewLine(true);
    }

    public void addImage(Icon image) {
        short[] img = new short[2];
        img[0] = Par.IMAGE;
        img[1] = addObject(image);
        int width = image.getWidth() + 2;
        if ((getLastLineWidth() + width) > getWidth()) {
            internNewLine(false);
        }
        internAdd(img, width);
    }

    public void addProgress(byte colorType) {
        internNewLine(false);
        short[] progress = new short[4];
        progress[0] = Par.PROGRESS;
        progress[1] = colorType;
        progress[2] = maxWidth;
        progress[3] = 0;
        internAdd(progress, maxWidth);
    }

    public void addText(String text, byte colorType, byte fontStyle) {
        addBigText(text, colorType, fontStyle, false);
    }

    public void addTextWithSmiles(String text, byte colorType, byte fontStyle) {
        addBigText(text, colorType, fontStyle, true);
    }

    /**
     * Add big multiline textIndex.
     *
     * Text visial width can be larger then screen maxWidth.
     * Method addText automatically divides textIndex to short lines
     * and adds lines to textIndex list
     */
    private void addBigText(String text, byte colorType,
            byte fontStyle, boolean withEmotions) {
        Font font = fontSet[fontStyle];
        short textIndex = addObject(text);

        // Width of free space in last line
        final int fullWidth = getWidth();
        int width = fullWidth - getLastLineWidth();

        int curLineWidth = 0;
        // #sijapp cond.if modules_SMILES is "true" #
        int smileCount = 100;
        // #sijapp cond.end #
        int lineStart = 0;
        int wordStart = 0;
        int wordWidth = 0;
        int textLen = text.length();
        // #sijapp cond.if modules_SMILES is "true" #
        Emotions smiles = Emotions.instance;
        withEmotions &= smiles.isEnabled();
        // #sijapp cond.end #
        for (int i = 0; i < textLen; ++i) {
            char ch = text.charAt(i);
            if ('\n' == ch) {
                addText(textIndex, lineStart, i, colorType, fontStyle, curLineWidth);
                internNewLine(true);
                lineStart = i + 1;
                width = fullWidth;
                wordStart = lineStart;
                wordWidth = 0;
                curLineWidth = 0;
                continue;
            }

            // #sijapp cond.if modules_SMILES is "true" #
            int smileIndex = withEmotions ? smiles.getSmile(text, i) : -1;
            if (-1 != smileIndex) {
                wordStart = i;
                if (lineStart < wordStart) {
                    addText(textIndex, lineStart, wordStart, colorType, fontStyle, curLineWidth);
                    if (width <= 0) {
                        internNewLine(false);
                        width = fullWidth;
                    }
                }

                short[] smileItem = createSmile((short) smileIndex);
                int smileWidth = smiles.getSmileIcon(smileIndex).getWidth();
                width -= smileWidth;
                if (width <= 0) {
                    internNewLine(false);
                    width = fullWidth - smileWidth;
                }
                internAdd(smileItem, smileWidth);

                i += smiles.getSmileText(smileIndex).length() - 1;
                lineStart = i + 1;
                wordStart = lineStart;
                wordWidth = 0;

                smileCount--;
                if (0 == smileCount) {
                    withEmotions = false;
                }
                curLineWidth = 0;
                continue;
            }
            // #sijapp cond.end #

            int charWidth = font.charWidth(ch);

            wordWidth += charWidth;
            curLineWidth += charWidth;
            width -= charWidth;
            if (' ' == ch) {
                wordStart = i + 1;
                wordWidth = 0;
                continue;
            }

            if (width <= 0) {
                if (lineStart < wordStart) {
                    curLineWidth -= wordWidth;
                    addText(textIndex, lineStart, wordStart, colorType, fontStyle, curLineWidth);
                    internNewLine(false);
                    lineStart = wordStart;
                    width = fullWidth - wordWidth;
                    curLineWidth = wordWidth;
                    continue;

                } else if (wordWidth < fullWidth) {
                    if (0 < getLastLineWidth()) {
                        internNewLine(false);
                    }
                    width = fullWidth - wordWidth;
                    curLineWidth = wordWidth;
                    continue;

                } else {
                    addText(textIndex, lineStart, i, colorType, fontStyle, curLineWidth);
                    internNewLine(false);
                    lineStart = i;
                    width = fullWidth - charWidth;
                    wordStart = i;
                    wordWidth = charWidth;
                    curLineWidth = charWidth;
                    continue;
                }
            }
        }
        if (lineStart < text.length()) {
            addText(textIndex, lineStart, text.length(), colorType, fontStyle, curLineWidth);
        }
    }
}
