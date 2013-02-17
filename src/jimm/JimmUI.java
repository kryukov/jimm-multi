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
import jimm.ui.base.*;
import jimm.cl.*;
import protocol.*;

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

    public static String getClipBoardText(boolean quote) {
        // #sijapp cond.if modules_ANDROID is "true" #
        String androidClipboard = ru.net.jimm.JimmActivity.getInstance().getFromClipboard();
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
        ru.net.jimm.JimmActivity.getInstance().putToClipboard(clipBoardHeader, clipBoardText);
        // #sijapp cond.end #
    }

    public static void setClipBoardText(boolean incoming, String from, String date, String text) {
        clipBoardText     = text;
        clipBoardHeader   = from + ' ' + date;
        clipBoardIncoming = incoming;
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().putToClipboard(clipBoardHeader, clipBoardText);
        // #sijapp cond.end #
    }

    public static void clearClipBoardText() {
        clipBoardText = null;
    }


    /************************************************************************/
    /************************************************************************/
    /************************************************************************/

    ///////////////////
    //               //
    //    Hotkeys    //
    //               //
    ///////////////////

    private static int getHotKeyOpCode(int keyCode, int type) {
        int action = Options.HOTKEY_NONE;
        switch (keyCode) {
            case Canvas.KEY_NUM0:
                action = Options.getInt(Options.OPTION_EXT_CLKEY0);
                break;
            case Canvas.KEY_NUM4:
                action = Options.getInt(Options.OPTION_EXT_CLKEY4);
                break;

            case Canvas.KEY_NUM6:
                action = Options.getInt(Options.OPTION_EXT_CLKEY6);
                break;

            case Canvas.KEY_STAR:
                action = Options.getInt(Options.OPTION_EXT_CLKEYSTAR);
                break;

            case Canvas.KEY_POUND:
                action = Options.getInt(Options.OPTION_EXT_CLKEYPOUND);
                break;

            case NativeCanvas.CAMERA_KEY:
            case NativeCanvas.CALL_KEY:
                action = Options.getInt(Options.OPTION_EXT_CLKEYCALL);
                break;
        }
        return action;
    }
    public static boolean isHotKey(int keyCode, int type) {
        return (Options.HOTKEY_NONE != getHotKeyOpCode(keyCode, type));
    }
    public static boolean execHotKey(Protocol p, Contact contact, int keyCode, int type) {
        int action = getHotKeyOpCode(keyCode, type);
        return (Options.HOTKEY_NONE != action) && execHotKeyAction(p, contact, action, type);
    }

    private static boolean execHotKeyAction(Protocol p, Contact contact, int actionNum, int keyType) {
        if ((CanvasEx.KEY_REPEATED == keyType)
                || (CanvasEx.KEY_RELEASED == keyType)) {
            return false;
        }
        if (Options.HOTKEY_LOCK == actionNum) {
            Jimm.lockJimm();
            return true;
        }
        ContactList cl = ContactList.getInstance();
        if (null != contact) {
            switch (actionNum) {
                // #sijapp cond.if modules_HISTORY is "true" #
                case Options.HOTKEY_HISTORY:
                    new ContactMenu(p, contact).doAction(Contact.USER_MENU_HISTORY);
                    return true;
                // #sijapp cond.end#

                case Options.HOTKEY_INFO:
                    p.showUserInfo(contact);
                    return true;

                case Options.HOTKEY_STATUSES:
                    p.showStatus(contact);
                    return true;

                // #sijapp cond.if modules_FILES is "true"#
                case Options.HOTKEY_SEND_PHOTO:
                    if (FileTransfer.isPhotoSupported()) {
                        new FileTransfer(p, contact).startPhotoTransfer();
                    }
                    return true;
                // #sijapp cond.end#
            }
        }
        Object currentDisplay = Jimm.getJimm().getDisplay().getCurrentDisplay();
        switch (actionNum) {
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case Options.HOTKEY_MAGIC_EYE:
                MagicEye.activate();
                return true;
            // #sijapp cond.end#

            case Options.HOTKEY_OPEN_CHATS:
                if (currentDisplay == ChatHistory.instance) {
                    ChatHistory.instance.goBack();

                } else {
                    ChatHistory.instance.showChatList(false);
                }
                return true;

            case Options.HOTKEY_ONOFF:
                if (currentDisplay != cl.getManager()) {
                    return true;
                }
                boolean hide = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, hide);
                Options.safeSave();
                ContactList.getInstance().activate();
                return true;

            case Options.HOTKEY_MINIMIZE:
                Jimm.minimize();
                return true;

                // #sijapp cond.if modules_SOUND is "true" #
            case Options.HOTKEY_SOUNDOFF:
                Notify.getSound().changeSoundMode(true);
                return true;
                // #sijapp cond.end#

            case Options.HOTKEY_COLLAPSE_ALL:
                if (currentDisplay == cl.getManager()) {
                    ContactList.getInstance().collapseAll();
                }
                return true;
        }
        return false;
    }
}