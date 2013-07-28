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
 File: src/jimm/Emotions.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis
 *******************************************************************************/

package jimm.modules;

// #sijapp cond.if (modules_SMILES is "true") #

import jimmui.ContentActionListener;
import jimmui.view.icons.*;
import java.util.*;
import java.io.*;
import jimm.comm.*;
import jimmui.view.*;
import jimmui.view.smiles.Selector;
import jimmui.view.smiles.SmilesContent;
import protocol.net.TcpSocket;

public final class Emotions {
    private ImageList images;

    private int[] selEmotionsIndexes;
    private String[] selEmotionsWord;
    private String[] selEmotionsSmileNames;

    private String smileChars;
    private int[] textCorrIndexes;
    private String[] textCorrWords;
//    private Glyphs[] smileListItems;

    private Emotions() {}
    public static final Emotions instance = new Emotions();

    private static final int PARSER_NONE        = 0;
    private static final int PARSER_NUMBER      = 1;
    private static final int PARSER_MNAME       = 2;
    private static final int PARSER_NAME        = 3;
    private static final int PARSER_LONG_NAME   = 4;
    private static final int PARSER_FIRST_SMILE = 5;
    private static final int PARSER_SMILE       = 6;

    private void smileParser(String content, Vector textCorr, Vector selEmotions) {
        Integer curIndex = new Integer(0);
        String smileName = "";
        String word = "";
        int beginPos = 0;
        int state = PARSER_NONE;
        int len = content.length();
        for (int i = 0; i <= len; ++i) {
            char ch = (i < len) ? content.charAt(i) : '\n';
            if ('\r' == ch) continue;
            switch (state) {
                case PARSER_NONE:
                    if ('"' == ch) {
                        state = PARSER_LONG_NAME;
                        beginPos = i + 1;
                    } else if ((' ' == ch) || ('\n' == ch)) {
                    } else {
                        state = PARSER_NUMBER;
                        beginPos = i;
                    }
                    break;

                case PARSER_NUMBER:
                    if (' ' == ch) {
                        state = PARSER_MNAME;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                        try {
                            Integer.parseInt(smileName);
                        } catch (Exception e) {
                            state = PARSER_FIRST_SMILE;
                        }
                    }
                    break;

                case PARSER_MNAME:
                    if ('"' == ch) {
                        state = PARSER_LONG_NAME;
                        beginPos = i + 1;
                    } else if (' ' == ch) {
                    } else {
                        state = PARSER_NAME;
                        beginPos = i;
                    }
                    break;

                case PARSER_NAME:
                    if (' ' == ch) {
                        state = PARSER_FIRST_SMILE;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                    }
                    break;

                case PARSER_LONG_NAME:
                    if ('"' == ch) {
                        state = PARSER_FIRST_SMILE;
                        smileName = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                    }
                    break;


                case PARSER_FIRST_SMILE:
                    switch (ch) {
                        case ',':
                            word = content.substring(beginPos, i).trim();
                            if (word.length() != 0) {
                                state = PARSER_SMILE;
                                selEmotions.addElement(new Object[] {curIndex, word, smileName});
                                textCorr.addElement(new Object[] {word, curIndex});
                                if (smileName.length() == 0) {
                                    smileName = word;
                                }
                            }
                            beginPos = i + 1;
                            break;
                        case '\n':
                            state = PARSER_NONE;
                            word = content.substring(beginPos, i).trim();
                            if (word.length() != 0) {
                                selEmotions.addElement(new Object[] {curIndex, word, smileName});
                                textCorr.addElement(new Object[] {word, curIndex});
                                if (smileName.length() == 0) {
                                    smileName = word;
                                }
                            }
                            curIndex = new Integer(curIndex.intValue() + 1);
                            break;
                    }
                    break;

                case PARSER_SMILE:
                    switch (ch) {
                        case ',':
                            word = content.substring(beginPos, i).trim();
                            if ((0 < word.length()) && (word.length() < 30)) {
                                textCorr.addElement(new Object[] {word, curIndex});
                            }
                            beginPos = i + 1;
                            break;
                        case '\n':
                            state = PARSER_NONE;
                            word = content.substring(beginPos, i).trim();
                            if ((0 < word.length()) && (word.length() < 30)) {
                                textCorr.addElement(new Object[] {word, curIndex});
                            }
                            curIndex = new Integer(curIndex.intValue() + 1);
                            break;
                    }
                    break;
            }
        }
    }
    public void load() {
        boolean loaded = false;
        try {
            loaded = loadAll();
        } catch (Exception ignored) {
        }
        if (!loaded) {
            selEmotionsIndexes    = null;
            selEmotionsWord       = null;
            selEmotionsSmileNames = null;
            images = null;
        }
    }
    private ImageList loadIcons(int iconsSize) throws IOException {
        ImageList emoImages = null;
        // #sijapp cond.if modules_ANISMILES is "true" #
        emoImages = new AniImageList();
        emoImages.load("/smiles", iconsSize, iconsSize);
        if (0 < emoImages.size()) {
            return emoImages;
        }
        // #sijapp cond.end #
        emoImages = new ImageList();
        emoImages.load("/smiles.png", iconsSize, iconsSize);
        return emoImages;
    }
    private boolean loadAll() {
        images = null;
        Vector textCorr = new Vector();
        Vector selEmotions = new Vector();

        // #sijapp cond.if modules_DEBUGLOG is "true"#
        jimm.Jimm.gc();
        long mem = Runtime.getRuntime().freeMemory();
        // #sijapp cond.end#

        // Load file "smiles.txt"
        InputStream stream = null;
        // #sijapp cond.if modules_ANISMILES is "true" #
        stream = jimm.Jimm.getResourceAsStream("/smiles/smiles.txt");
        // #sijapp cond.end #
        if (null == stream) {
            stream = jimm.Jimm.getResourceAsStream("/smiles.txt");
        }
        if (null == stream) {
            return false;
        }
        ImageList emoImages = null;
        try {
            DataInputStream dos = new DataInputStream(stream);
            // Read icon size
            int iconsSize = readIntFromStream(dos);
            emoImages = loadIcons(iconsSize);
            byte[] str = new byte[dos.available()];
            dos.read(str);
            String content = StringConvertor.utf8beByteArrayToString(str, 0, str.length);
            smileParser(content, textCorr, selEmotions);
            TcpSocket.close(dos);
        } catch (Exception e) {
        }
        TcpSocket.close(stream);
        if (0 == emoImages.size()) {
            return false;
        }

        // Write emotions data from vectors to arrays
        int size = selEmotions.size();
        selEmotionsIndexes    = new int[size];
        selEmotionsWord       = new String[size];
        selEmotionsSmileNames = new String[size];
        for (int i = 0; i < size; ++i) {
            Object[] data            = (Object[])selEmotions.elementAt(i);
            selEmotionsIndexes[i]    = ((Integer)data[0]).intValue();
            selEmotionsWord[i]       = (String)data[1];
            selEmotionsSmileNames[i] = (String)data[2];
        }

        size = textCorr.size();
        textCorrWords   = new String[size];
        textCorrIndexes = new int[size];
//        smileListItems = new Glyphs[size];
        StringBuffer fisrtChars = new StringBuffer(textCorr.size());
        for (int i = 0; i < size; ++i) {
            Object[] data = (Object[])textCorr.elementAt(i);
            textCorrWords[i]   = (String)data[0];
            textCorrIndexes[i] = ((Integer)data[1]).intValue();

            fisrtChars.append(textCorrWords[i].charAt(0));
        }
        this.smileChars = fisrtChars.toString();

        // #sijapp cond.if modules_DEBUGLOG is "true"#
        DebugLog.println("Emotions used (full): "+(mem - Runtime.getRuntime().freeMemory()));
        selEmotions.removeAllElements();
        selEmotions = null;
        textCorr.removeAllElements();
        textCorr = null;
        jimm.Jimm.gc();
        DebugLog.println("Emotions used: "+(mem - Runtime.getRuntime().freeMemory()));
        // #sijapp cond.end#
        jimm.Jimm.gc();
        images = emoImages;
        return true;
    }

    public static boolean isSupported() {
        return (null != instance.images);
    }

    public final boolean isEnabled() {
        return (null != images);
    }

    private int readIntFromStream(DataInputStream stream) throws IOException {
        int value = 0;
        byte digit = stream.readByte();
        while (digit >= '0' && digit <= '9') {
            value = 10 * value + (digit - '0');
            digit = stream.readByte();
        }
        while (digit != '\n') {
            digit = stream.readByte();
        }
        return value;
    }

    public int getSmile(String text, int pos) {
        int smileIndex = smileChars.indexOf(text.charAt(pos));
        while (-1 != smileIndex) {
            if (text.startsWith(textCorrWords[smileIndex], pos)) {
                return smileIndex;
            }
            smileIndex = smileChars.indexOf(text.charAt(pos), smileIndex + 1);
        }
        return -1;
    }
    public Icon getSmileIcon(int smileIndex) {
        return images.iconAt(textCorrIndexes[smileIndex]);
    }
    public String getSmileText(int smileIndex) {
        return textCorrWords[smileIndex];
    }

    ///////////////////////////////////
    //                               //
    //   UI for emotion selection    //
    //                               //
    ///////////////////////////////////

    static private Selector sl;

    static public void selectEmotion(ContentActionListener listener) {
        if (!isSupported()) {
            return;
        }
        if (null == sl) {
            sl = new Selector(instance.images, instance.selEmotionsSmileNames,
                    instance.selEmotionsWord);
        }
        ((SmilesContent)sl.getContent()).setSelectionListener(listener);
        sl.show();
    }
}

// #sijapp cond.end#
