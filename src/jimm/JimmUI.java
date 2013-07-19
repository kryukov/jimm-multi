/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/JimmUI.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Igor Palkin, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/

package jimm;

import javax.microedition.lcdui.*;
import jimm.chat.*;
import jimm.comm.StringConvertor;
import jimm.modules.*;
import jimmui.view.base.*;
import jimm.cl.*;
import protocol.*;
import protocol.ui.ContactMenu;

public final class JimmUI {
    //////////////////////
    //                  //
    //    Clipboard     //
    //                  //
    //////////////////////

    private static String clipBoardText;
    private static String clipBoardHeader;
    private static boolean clipBoardIncoming;

    private static void insertQuotingChars(StringBuffer out, String text, char qChars) {
        int size = text.length();
        boolean wasNewLine = true;
        for (int i = 0; i < size; ++i) {
            char chr = text.charAt(i);
            if (wasNewLine) out.append(qChars).append(' ');
            out.append(chr);
            wasNewLine = (chr == '\n');
        }
    }

    public static boolean isClipBoardEmpty() {
        return null == clipBoardText;
    }

    public static String getClipBoardText() {
        return getClipBoardText(false);
    }
    public static String getClipBoardText(boolean quote) {
        // #sijapp cond.if modules_ANDROID is "true" #
        String androidClipboard = ru.net.jimm.JimmActivity.getInstance().clipboard.get();
        if (!StringConvertor.isEmpty(androidClipboard) && !androidClipboard.equals(clipBoardText)) {
            clipBoardText = androidClipboard;
            clipBoardHeader = "mobile";
            clipBoardIncoming = true;
        }
        // #sijapp cond.end #
        if (isClipBoardEmpty()) {
            return "";
        }
        if (!quote || (null == clipBoardHeader)) {
            return clipBoardText + " ";
        }
        return serialize(clipBoardIncoming, clipBoardHeader, clipBoardText) + "\n\n";
    }

    public static String serialize(boolean incoming, String header, String text) {
        StringBuffer sb = new StringBuffer();
        sb.append('[').append(header).append(']').append('\n');
        insertQuotingChars(sb, text, incoming ? '\u00bb' : '\u00ab');//'»' : '«');
        return sb.toString();
    }

    public static void setClipBoardText(String header, String text) {
        clipBoardText     = text;
        clipBoardHeader   = header;
        clipBoardIncoming = true;
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().clipboard.put(clipBoardText);
        // #sijapp cond.end #
    }
    public static void setClipBoardText(String text) {
        clipBoardText     = text;
        clipBoardHeader   = null;
        clipBoardIncoming = true;
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().clipboard.put(clipBoardText);
        // #sijapp cond.end #
    }

    public static void setClipBoardText(boolean incoming, String from, String date, String text) {
        clipBoardText     = text;
        clipBoardHeader   = null;//from + ' ' + date;
        clipBoardIncoming = incoming;
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().clipboard.put(clipBoardText);
        // #sijapp cond.end #
    }

    public static void clearClipBoardText() {
        clipBoardText = null;
    }


    /************************************************************************/
    /************************************************************************/
    /************************************************************************/

}