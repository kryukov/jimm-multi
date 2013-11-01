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

package jimmui;

import jimm.comm.StringUtils;

public final class Clipboard {
    //////////////////////
    //                  //
    //    Clipboard     //
    //                  //
    //////////////////////
    private static Clipboard instance = new Clipboard();

    private String text;

    private static void insertQuotingChars(StringBuilder out, String text, char qChars) {
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
        return null == instance.text;
    }

    public static String getClipBoardText() {
        // #sijapp cond.if modules_ANDROID is "true" #
        String androidClipboard = ru.net.jimm.JimmActivity.getInstance().clipboard.get();
        if (!StringUtils.isEmpty(androidClipboard) && !androidClipboard.equals(instance.text)) {
            instance.set(androidClipboard);
        }
        // #sijapp cond.end #
        return isClipBoardEmpty() ? "" : (instance.text + " ");
    }

    public static String serialize(boolean incoming, String header, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(header).append(']').append('\n');
        insertQuotingChars(sb, text, incoming ? '\u00bb' : '\u00ab');//'»' : '«');
        return sb.toString();
    }

    public static void setClipBoardText(String text) {
        instance.set(text);
    }

    public static void setClipBoardText(boolean incoming, String from, String date, String text) {
        instance.set(text);
    }
    private void set(String text) {
        this.text = text;
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().clipboard.put(text);
        // #sijapp cond.end #
    }

}