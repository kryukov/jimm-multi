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
*******************************************************************************
File: src/jimm/Options.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
           Vladimir Kryukov
******************************************************************************/




package jimm;

import jimm.comm.*;
import jimm.forms.*;
import jimm.io.Storage;
import protocol.*;
import protocol.icq.*;
import jimm.modules.*;
import jimm.util.*;
import java.io.*;
import java.util.*;
import jimm.ui.base.NativeCanvas;


/**
 * Current record store format:
 *
 * Record #1: VERSION               (UTF8)
 * Record #2: OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            OPTION KEY            (BYTE)
 *            OPTION VALUE          (Type depends on key)
 *            ...
 *
 * Option key            Option value
 *   0 -  63 (00XXXXXX)  UTF8
 *  64 - 127 (01XXXXXX)  INTEGER
 * 128 - 191 (10XXXXXX)  BOOLEAN
 * 192 - 224 (110XXXXX)  LONG
 * 225 - 255 (111XXXXX)  SHORT, BYTE-ARRAY (scrambled String)
 */
public class Options {

    /* Option keys */
//    static final int OPTION_NICK1                      =  21;   /* String */
//    static final int OPTION_UIN1                       =   0;   /* String */
//    static final int OPTION_PASSWORD1                  = 228;   /* String  */
//    static final int OPTION_NICK2                      =  22;   /* String */
//    static final int OPTION_UIN2                       =  14;   /* String  */
//    static final int OPTION_PASSWORD2                  = 229;   /* String  */
//    static final int OPTION_NICK3                      =  23;   /* String */
//    static final int OPTION_UIN3                       =  15;   /* String  */
//    static final int OPTION_PASSWORD3                  = 230;   /* String  */
    static final int OPTIONS_CURR_ACCOUNT              =  86;   /* int     */

//    public static final int OPTION_SRV_HOST            =   1;   /* String  */
//    public static final int OPTION_SRV_PORT            =   2;   /* String  */
//    public static final int OPTION_KEEP_CONN_ALIVE     = 128;   /* boolean */
//    public static final int OPTION_CONN_ALIVE_INVTERV  =  13;   /* String  */
//    public static final int OPTION_ASYNC               = 166;   /* boolean */
//    public static final int OPTION_CONN_ALIVE_INVTERV  = 108;   /* int     */
//    public static final int OPTION_CONN_TYPE           =  83;   /* int     */
//    public static final int OPTION_AUTO_CONNECT        = 138;   /* boolean */
//    public static final int OPTION_SHADOW_CON          = 139;   /* boolean */
//    public static final int OPTION_UPDATE_CHECK_TIME  =  64;   /* int     */
//    public static final int OPTION_LAST_VERSION       =  27;   /* String  */
//    public static final int OPTION_CHECK_UPDATES      = 174;   /* boolean */

    public static final int OPTION_AA_BLOCK           = 175;   /* boolean */
    public static final int OPTION_AA_TIME            = 106;   /* int     */
    //public static final int OPTION_AUTOANSWER         =  28;   /* String  */

    //public static final int OPTION_RECONNECT           = 149;   /* boolean */
    //public static final int OPTION_RECONNECT_NUMBER    =  91;   /* int */
    //public static final int OPTION_HTTP_USER_AGENT     =  17;   /* String  */
    //public static final int OPTION_HTTP_WAP_PROFILE    =  18;   /* String  */
    public static final int OPTION_UI_LANGUAGE         =   3;   /* String  */
    //public static final int OPTION_DISPLAY_DATE        = 129;   /* boolean */
    public static final int OPTION_CL_SORT_BY          =  65;   /* int     */
    public static final int OPTION_CL_HIDE_OFFLINE     = 130;   /* boolean */
    public static final int OPTION_MESS_NOTIF_MODE     =  66;   /* int     */
    public static final int OPTION_NOTIFY_VOLUME      =  67;   /* int     */
    public static final int OPTION_ONLINE_NOTIF_MODE   =  68;   /* int     */
//    public static final int OPTION_ONLINE_NOTIF_VOL    =  69;   /* int     */
    public static final int OPTION_VIBRATOR            =  75;   /* integer */
    public static final int OPTION_TYPING_MODE         =  88;   /* integer */
//    public static final int OPTION_TYPING_VOL          =  89;
//    public static final int OPTION_MESS_NOTIF_FILE     =   4;   /* String  */
//    public static final int OPTION_ONLINE_NOTIF_FILE   =   5;   /* String  */
//    public static final int OPTION_TYPING_FILE         =  16;   /* String  */
//    public static final int OPTION_VOLUME_BUGFIX       = 155; /* boolean */
//    public static final int OPTION_CP1251_HACK         = 133;   /* boolean */
    // #sijapp cond.if modules_TRAFFIC is "true" #
    public static final int OPTION_COST_OF_1M     =  70;   /* int     */
//    public static final int OPTION_COST_PER_DAY        =  71;   /* int     */
    public static final int OPTION_COST_PACKET_LENGTH  =  72;   /* int     */
    public static final int OPTION_CURRENCY            =   6;   /* String  */
    // #sijapp cond.end #
    public static final int OPTION_ONLINE_STATUS       = 192;   /* long    */
//    public static final int OPTION_DETECT_ENCODING     = 153;   /* boolean */
//    public static final int OPTION_DELIVERY_NOTIFICATION      = 173;   /* boolean */
//    public static final int OPTION_REPLACE_STATUS_ICON = 152;   /* boolean */
    //public static final int OPTION_SHOW_LISTS_ICON     = 154;   /* boolean */
    public static final int OPTION_TF_FLAGS            = 169;   /* boolean */
    public static final int OPTION_MAX_MSG_COUNT       =  94;   /* int     */
    //public static final int OPTION_MSGSEND_MODE        =  95;   /* int     */
    //public static final int OPTION_CLIENT              =  96;   /* int     */
    public static final int OPTION_DETRANSLITERATE     = 178;   /* boolean */

    public static final int OPTION_PRIVATE_STATUS      =  93;   /* int     */
    //public static final int OPTION_CHAT_SMALL_FONT     = 135;   /* boolean */
    //public static final int OPTION_SMALL_FONT          = 157;   /* boolean */
    public static final int OPTION_USER_GROUPS         = 136;   /* boolean */
    public static final int OPTION_HISTORY             = 137;   /* boolean */
    //public static final int OPTION_SHOW_LAST_MESS      = 142;   /* boolean */
    public static final int OPTION_CLASSIC_CHAT        = 143;   /* boolean */
    public static final int OPTION_COLOR_SCHEME        =  73;   /* int     */
    public static final int OPTION_FONT_SCHEME         = 107;   /* int     */
    public static final int OPTION_STATUS_MESSAGE      =   7;   /* String  */
    public static final int OPTION_KEYBOARD            = 109;   /* int     */
    public static final int OPTION_MIN_ITEM_SIZE       = 110;   /* int     */

//    public static final int OPTION_XSTATUS             =  92;   /* int     */
//    public static final int OPTION_XTRAZ_ENABLE        = 156;   /* boolean */
//    public static final int OPTION_XTRAZ_TITLE         =  19;   /* String  */
//    public static final int OPTION_XTRAZ_DESC          =  20;   /* String  */
//    public static final int OPTION_AUTO_STATUS         = 161;   /* boolean */
//    public static final int OPTION_AUTO_XTRAZ          = 162;   /* boolean */

    public static final int OPTION_ANTISPAM_MSG        =  24;   /* String  */
    public static final int OPTION_ANTISPAM_HELLO      =  25;   /* String  */
    public static final int OPTION_ANTISPAM_ANSWER     =  26;   /* String  */
    public static final int OPTION_ANTISPAM_ENABLE     = 158;   /* boolean */
    //public static final int OPTION_ANTISPAM_OFFLINE    = 159;   /* boolean */
    public static final int OPTION_ANTISPAM_KEYWORDS   =  29;   /* String  */

    public static final int OPTION_SAVE_TEMP_CONTACT   = 147;   /* boolean */

    //public static final int OPTION_USE_SMILES          = 141;   /* boolean */
    //public static final int OPTION_MD5_LOGIN           = 144;   /* boolean */

//    public static final int OPTION_PRX_TYPE            =  76;   /* int     */
//    public static final int OPTION_PRX_SERV            =   8;   /* String  */
//    public static final int OPTION_PRX_PORT            =   9;   /* String  */
//    public static final int OPTION_AUTORETRY_COUNT     =  10;   /* String  */
//    public static final int OPTION_PRX_NAME            =  11;   /* String  */
//    public static final int OPTION_PRX_PASS            =  12;   /* String  */

    public static final int OPTION_GMT_OFFSET           =  87;   /* int     */
    public static final int OPTION_LOCAL_OFFSET         =  90;   /* int     */

    //public static final int OPTION_FULL_SCREEN         = 145;   /* boolean */
    public static final int OPTION_SILENT_MODE         = 150;   /* boolean */
    public static final int OPTION_BRING_UP            = 151;   /* boolean */

//    protected static final int OPTIONS_LANG_CHANGED    = 148;

//    public static final int OPTION_POPUP_WIN2          =  84;   /* int     */
    public static final int OPTION_EXT_CLKEY0          =  77;   /* int     */
    public static final int OPTION_EXT_CLKEYSTAR       =  78;   /* int     */
    public static final int OPTION_EXT_CLKEY4          =  79;   /* int     */
    public static final int OPTION_EXT_CLKEY6          =  80;   /* int     */
    public static final int OPTION_EXT_CLKEYCALL       =  81;   /* int     */
    public static final int OPTION_EXT_CLKEYPOUND      =  82;   /* int     */
    public static final int OPTION_VISIBILITY_ID       =  85;   /* int     */

    public static final int OPTION_UNTITLED_INPUT      = 160;   /* boolean */

    //public static final int OPTION_LIGHT               = 163;   /* boolean */
    public static final int OPTION_LIGHT_THEME         =  97;   /* int     */
    //public static final int OPTION_LIGHT_ONLINE        =  98;   /* int     */
    //public static final int OPTION_LIGHT_KEY_PRESS     =  99;   /* int     */
    //public static final int OPTION_LIGHT_CONNECT       = 100;   /* int     */
    //public static final int OPTION_LIGHT_MESSAGE       = 101;   /* int     */
    //public static final int OPTION_LIGHT_ERROR         = 102;   /* int     */
    //public static final int OPTION_LIGHT_SYSTEM        = 103;   /* int     */
    //public static final int OPTION_LIGHT_TICK          = 104;   /* int     */

    public static final int OPTION_INPUT_MODE          = 105;   /* int     */

    //public static final int OPTION_SOFTS_LIKE_OLDSE    = 164;   /* boolean */
    //public static final int OPTION_SHOW_AUTH_ICON      = 165;   /* boolean */
    public static final int OPTION_SHOW_SOFTBAR        = 167;   /* boolean */
//    public static final int OPTION_CUSTOM_GC           = 168;   /* boolean */
//    public static final int OPTION_POPUP_OVER_SYSTEM   = 170;   /* boolean */
    public static final int OPTION_SORT_UP_WITH_MSG    = 171;   /* boolean */
    public static final int OPTION_SWAP_SEND_AND_BACK  = 172;   /* boolean */
    public static final int OPTION_SHOW_STATUS_LINE    = 177;   /* boolean */

    public static final int OPTION_NOTIFY_IN_AWAY      = 179;   /* boolean */
    public static final int OPTION_ALARM               = 176;   /* boolean */
    public static final int OPTION_BLOG_NOTIFY         = 180;   /* boolean */
    public static final int OPTION_RECREATE_TEXTBOX    = 181;   /* boolean */

    //Hotkey Actions
    public static final int HOTKEY_NONE      =  0;
    public static final int HOTKEY_INFO      =  2;
    public static final int HOTKEY_ONOFF     =  4;
    public static final int HOTKEY_LOCK      =  7;
    public static final int HOTKEY_HISTORY   =  8;
    public static final int HOTKEY_MINIMIZE  =  9;
    public static final int HOTKEY_SOUNDOFF  = 12;
    public static final int HOTKEY_STATUSES  = 13;
    public static final int HOTKEY_MAGIC_EYE = 14;
    public static final int HOTKEY_OPEN_CHATS = 16;
    public static final int HOTKEY_COLLAPSE_ALL = 17;
    public static final int HOTKEY_SEND_PHOTO = 18;

    private static final Vector listOfProfiles = new Vector();

    /**************************************************************************/
    public static int getMaxAccountCount() {
        return 20;
    }
    public static int getAccountCount() {
        synchronized (listOfProfiles) {
            return listOfProfiles.size();
        }
    }
    public static Profile getAccount(int num) {
        synchronized (listOfProfiles) {
            if (listOfProfiles.size() <= num) {
                return new Profile();
            }
            return (Profile)listOfProfiles.elementAt(num);
        }
    }
    public static int getAccountIndex(Profile profile) {
        synchronized (listOfProfiles) {
            return Math.max(0, Util.getIndex(listOfProfiles, profile));
        }
    }
    public static void setCurrentAccount(int num) {
        num = Math.min(num, getAccountCount());
        Options.setInt(Options.OPTIONS_CURR_ACCOUNT, num);
    }
    public static int getCurrentAccount() {
        return Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
    }
    public static void delAccount(int num) {
        synchronized (listOfProfiles) {
            listOfProfiles.removeElementAt(num);

            // correct current position
            int current = getCurrentAccount();
            if (current == num) {
                current = 0;
            }
            if (num < current) {
                current--;
            }
            if (listOfProfiles.size() < current) {
                current = 0;
            }
            setCurrentAccount(current);

            // remove profile
            Storage s = new Storage("j-accounts");
            try {
                s.open(false);
                for (; num < listOfProfiles.size(); ++num) {
                    Profile p = (Profile)listOfProfiles.elementAt(num);
                    s.setRecord(num + 1, writeAccount(p));
                }
                for (; num < s.getNumRecords(); ++num) {
                    s.setRecord(num + 1, new byte[0]);
                }
            } catch (Exception e) {
            }
            s.close();
        }
    }
    public static void setAccount(int num, Profile account) {
        int size = getAccountCount();
        synchronized (listOfProfiles) {
            if (num < size) {
                listOfProfiles.setElementAt(account, num);
            } else {
                num = listOfProfiles.size();
                listOfProfiles.addElement(account);
            }
            saveAccount(num, account);
        }
    }
    public static void saveAccount(Profile account) {
        synchronized (listOfProfiles) {
            int num = listOfProfiles.indexOf(account);
            if (0 <= num) {
                saveAccount(num, account);
            }
        }
    }
    public static void loadAccounts() {
        Storage s = new Storage("j-accounts");
        try {
            synchronized (listOfProfiles) {
                listOfProfiles.removeAllElements();
                s.open(false);
                int accountCount = s.getNumRecords();
                for (int i = 0 ; i < accountCount; ++i) {
                    byte[] data = s.getRecord(i + 1);
                    if ((null == data) || (0 == data.length)) {
                        break;
                    }
                    Profile p = readProfile(data);
                    if (!StringConvertor.isEmpty(p.userId)) {
                        listOfProfiles.addElement(p);
                    }
                }
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("load accounts", e);
            // #sijapp cond.end#
            // migrate
            final int OPTION_NICK1                      =  21;   /* String */
            final int OPTION_UIN1                       =   0;   /* String */
            final int OPTION_PASSWORD1                  = 228;   /* String  */
            final int OPTION_NICK2                      =  22;   /* String */
            final int OPTION_UIN2                       =  14;   /* String  */
            final int OPTION_PASSWORD2                  = 229;   /* String  */
            final int OPTION_NICK3                      =  23;   /* String */
            final int OPTION_UIN3                       =  15;   /* String  */
            final int OPTION_PASSWORD3                  = 230;   /* String  */
            addProfile(OPTION_UIN1, OPTION_PASSWORD1, OPTION_NICK1);
            addProfile(OPTION_UIN2, OPTION_PASSWORD2, OPTION_NICK2);
            addProfile(OPTION_UIN3, OPTION_PASSWORD3, OPTION_NICK3);
        }
        s.close();
    }

    private static void saveAccount(int num, Profile account) {
        if (StringConvertor.isEmpty(account.userId)) {
            return;
        }
        Storage s = new Storage("j-accounts");
        try {
            s.open(true);
            byte[] hash = writeAccount(account);
            if (num < s.getNumRecords()) {
                s.setRecord(num + 1, hash);
            } else {
                s.addRecord(hash);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("save account #" + num, e);
            // #sijapp cond.end#
        }
        s.close();
    }
    private static void addProfile(int uinOpt, int passOpt, int nickOpt) {
        String uin = getString(uinOpt);
        if (!StringConvertor.isEmpty(uin)) {
            Profile p = new Profile();
            p.userId = uin;
            p.password = getString(passOpt);
            p.nick = getString(nickOpt);
            setAccount(getMaxAccountCount(), p);
            setString(uinOpt, "");
        }
    }

    private static byte[] writeAccount(Profile account) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(account.protocolType);
            dos.writeUTF(StringConvertor.notNull(account.userId));
            dos.writeUTF(StringConvertor.notNull(account.password));
            dos.writeUTF(StringConvertor.notNull(account.nick));
            dos.writeByte(account.statusIndex);
            dos.writeUTF(StringConvertor.notNull(account.statusMessage));
            dos.writeByte(account.xstatusIndex);
            dos.writeUTF(StringConvertor.notNull(account.xstatusTitle));
            dos.writeUTF(StringConvertor.notNull(account.xstatusDescription));
            dos.writeBoolean(account.isActive);
            dos.writeBoolean(account.isConnected & account.isActive);

            byte[] hash = Util.decipherPassword(baos.toByteArray());
            baos.close();
            return hash;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("write account" + account.userId, e);
            // #sijapp cond.end#
            return new byte[0];
        }
    }
    private static Profile readProfile(byte[] data) {
        Profile p = new Profile();
        try {
            byte[] buf = Util.decipherPassword(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);

            p.protocolType = dis.readByte();
            p.userId = dis.readUTF();
            p.password = dis.readUTF();
            p.nick = dis.readUTF();
            p.statusIndex = dis.readByte();
            p.statusMessage = dis.readUTF();
            p.xstatusIndex = dis.readByte();
            p.xstatusTitle = dis.readUTF();
            p.xstatusDescription = dis.readUTF();
            p.isActive = true;
            if (0 < dis.available()) {
                p.isActive = dis.readBoolean();
            }
            p.isConnected = false;
            if (0 < dis.available()) {
                p.isConnected = dis.readBoolean() & p.isActive;
            }
            bais.close();
        } catch (IOException ex) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("read account", ex);
            // #sijapp cond.end#
        }
        return p;
    }
    /**************************************************************************/

    // Hashtable containing all option key-value pairs
    private static Object[] options = new Object[256];


    public static void loadOptions() {
        // Try to load option values from record store and construct options form
        try {
            setDefaults();
            initAccounts();
            load();
        // Use default values if loading option values from record store failed
        } catch (Exception e) {
            setDefaults();
        }
    }
    private static void initAccounts() {
        setInt    (Options.OPTIONS_CURR_ACCOUNT,      0);
    }
    /* Set default values
       This is done before loading because older saves may not contain all new values */
    private static void setDefaults() {
        setString (Options.OPTION_UI_LANGUAGE,        JLocale.getSystemLanguage());
        setInt    (Options.OPTION_CL_SORT_BY,         0);
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE,    false);

        setBoolean(Options.OPTION_SHOW_SOFTBAR,       true);

        setInt    (Options.OPTION_MESS_NOTIF_MODE,    0);
        setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  0);
        setInt(Options.OPTION_TYPING_MODE, 0);
        setBoolean(Options.OPTION_BLOG_NOTIFY,        true);
        setBoolean(Options.OPTION_NOTIFY_IN_AWAY, true);
        // #sijapp cond.if modules_ANDROID is "true" #
        setInt    (Options.OPTION_NOTIFY_VOLUME,     100);
        // #sijapp cond.else #
        setInt    (Options.OPTION_NOTIFY_VOLUME,     50);
        // #sijapp cond.end#

        setBoolean(Options.OPTION_TF_FLAGS,           false);
        setInt    (Options.OPTION_MAX_MSG_COUNT,      100);

        setInt    (Options.OPTION_VIBRATOR,           1);
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isS60v5()) {
            setBoolean(Options.OPTION_SWAP_SEND_AND_BACK, true);
        }
        // #sijapp cond.end #

        // #sijapp cond.if modules_ANTISPAM is "true" #
        setString (Options.OPTION_ANTISPAM_KEYWORDS,           "http sms www @conf");
        // #sijapp cond.end #
        // #sijapp cond.if modules_TRAFFIC is "true" #
        setInt    (Options.OPTION_COST_OF_1M,    0);
        setInt    (Options.OPTION_COST_PACKET_LENGTH, 1024);
        setString (Options.OPTION_CURRENCY,           "$");
        // #sijapp cond.end #
        setLong   (Options.OPTION_ONLINE_STATUS,      StatusInfo.STATUS_ONLINE);
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        setInt    (Options.OPTION_PRIVATE_STATUS,     PrivateStatusForm.PSTATUS_NOT_INVISIBLE);
        // #sijapp cond.end #

        setBoolean(Options.OPTION_USER_GROUPS,        true);
        setBoolean(Options.OPTION_HISTORY,            false);
        setInt    (Options.OPTION_COLOR_SCHEME,       1);
        // #sijapp cond.if modules_ANDROID is "true" #
        setInt    (Options.OPTION_FONT_SCHEME,        0);
        // #sijapp cond.else #
        setInt    (Options.OPTION_FONT_SCHEME,        1);
        // #sijapp cond.end#
        int minItemSize = 15;
        // #sijapp cond.if modules_TOUCH is "true"#
        if (NativeCanvas.getInstance().hasPointerEvents()) {
            minItemSize = 20;
        }
        // #sijapp cond.end#
        setInt    (Options.OPTION_MIN_ITEM_SIZE,      minItemSize);
        setBoolean(Options.OPTION_SHOW_STATUS_LINE,   false);
        setInt    (Options.OPTION_VISIBILITY_ID,      0);

        setBoolean(Options.OPTION_SILENT_MODE,        false);
        setInt    (Options.OPTION_EXT_CLKEYSTAR,      HOTKEY_OPEN_CHATS);
        setInt    (Options.OPTION_EXT_CLKEY0,         HOTKEY_INFO);
        setInt    (Options.OPTION_EXT_CLKEY4,         HOTKEY_STATUSES);
        setInt    (Options.OPTION_EXT_CLKEY6,         HOTKEY_ONOFF);
        setInt    (Options.OPTION_EXT_CLKEYCALL,      HOTKEY_HISTORY);
        setInt    (Options.OPTION_EXT_CLKEYPOUND,     HOTKEY_LOCK);

        setBoolean(Options.OPTION_CLASSIC_CHAT,       false);

        // #sijapp cond.if modules_ANDROID is "true" #
        setBoolean(Options.OPTION_BRING_UP,           false);
        // #sijapp cond.elseif target="MIDP2"#
        setBoolean(Options.OPTION_BRING_UP,           true);
        // #sijapp cond.end#

        int time = TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60);
        /* Offset (in hours) between GMT time and local zone time
           GMT_time + GMT_offset = Local_time */
        setInt    (Options.OPTION_GMT_OFFSET,        time);

        /* Offset (in hours) between GMT time and phone clock
           Phone_clock + Local_offset = GMT_time */
        setInt    (Options.OPTION_LOCAL_OFFSET,      0);

        setBoolean(OPTION_ALARM, true);
        setBoolean(OPTION_RECREATE_TEXTBOX, Jimm.isPhone(Jimm.PHONE_SE));
    }

//    /* Experimental */
//    private void loadDefault() {
//        Options config = new Options().load("/config.txt");
//        String[] keys = config.getKeys();
//        String[] values = config.getValues();
//        for (int i = 0; i < keys.length; ++i) {
//            int key = Util.strToIntDef(keys[i], -1);
//            if (key < 0) {
//            } else if (key < 64) {  /* 0-63 = String */
//                setString(key, values[i]);
//            } else if (key < 128) {  /* 64-127 = int */
//                setInt(key, Util.strToIntDef(values[i], 0));
//            } else if (key < 192) {  /* 128-191 = boolean */
//                setBoolean(key, 0 != Util.strToIntDef(values[i], 0));
//            } else if (key < 224) {  /* 192-223 = long */
//                setLong(key, Util.strToIntDef(values[i], 0));
//            }
//        }
//    }

    /* Load option values from record store */
    private static void load() throws IOException {
        /* Read all option key-value pairs */
        byte[] buf = Storage.loadSlot(Storage.SLOT_OPTIONS);
        if (buf == null) {
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        while (dis.available() > 0) {
            int optionKey = dis.readUnsignedByte();
            if (optionKey < 64) {  /* 0-63 = String */
                setString(optionKey, dis.readUTF());
            } else if (optionKey < 128) {  /* 64-127 = int */
                setInt(optionKey, dis.readInt());
            } else if (optionKey < 192) {  /* 128-191 = boolean */
                setBoolean(optionKey, dis.readBoolean());
            } else if (optionKey < 224) {  /* 192-223 = long */
                setLong(optionKey, dis.readLong());
            } else {  /* 226-255 = Scrambled String */
                byte[] optionValue = new byte[dis.readUnsignedShort()];
                dis.readFully(optionValue);
                optionValue = Util.decipherPassword(optionValue);
                setString(optionKey, StringConvertor.utf8beByteArrayToString(optionValue, 0, optionValue.length));
            }
        }
    }


    /* Save option values to record store */
    private static void save() throws IOException {
        /* Temporary variables */

        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStart();
        // #sijapp cond.end #

        /* Save all option key-value pairs */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int key = 0; key < options.length; ++key) {
            if (null == options[key]) {
                continue;
            }
            dos.writeByte(key);
            if (key < 64) {  /* 0-63 = String */
                dos.writeUTF((String)options[key]);
            } else if (key < 128) {  /* 64-127 = int */
                dos.writeInt(((Integer)options[key]).intValue());
            } else if (key < 192) {  /* 128-191 = boolean */
                dos.writeBoolean(((Boolean)options[key]).booleanValue());
            } else if (key < 224) {  /* 192-223 = long */
                dos.writeLong(((Long)options[key]).longValue());
            } else if (key < 256) {  /* 226-255 = Scrambled String */
                String str = (String)options[key];
                byte[] optionValue = StringConvertor.stringToByteArrayUtf8(str);
                optionValue = Util.decipherPassword(optionValue);
                dos.writeShort(optionValue.length);
                dos.write(optionValue);
            }
        }
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("make options");
        // #sijapp cond.end #

        /* Close record store */
        Storage.saveSlot(Storage.SLOT_OPTIONS, baos.toByteArray());
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSlot(OPTIONS)");
        // #sijapp cond.end #
    }

    public static synchronized void safeSave() {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        long profiler = DebugLog.profilerStart();
        // #sijapp cond.end #
        try {
            save();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("options: " + e.toString());
            // #sijapp cond.end #
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        try {
            new ru.net.jimm.config.Options().store();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("options: " + e.toString());
            // #sijapp cond.end #
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.profilerStep("safeSave", profiler);
        // #sijapp cond.end #
    }

    /* Option retrieval methods (no type checking!) */
    public static String getString(int key) {
        String value = (String)options[key];
        return (null == value) ? "" : value;
    }

    public static int getInt(int key) {
        Integer value = (Integer) options[key];
        return (null == value) ? 0 : value.intValue();
    }

    public static boolean getBoolean(int key) {
        Boolean value = (Boolean) options[key];
        return (null == value) ? false : value.booleanValue();
    }

    public static long getLong(int key) {
        Long value = (Long) options[key];
        return (null == value) ? 0 : value.longValue();
    }


    /* Option setting methods (no type checking!) */
    public static void setString(int key, String value) {
        options[key] = value;
    }
    public static void setInt(int key, int value) {
        options[key] = new Integer(value);
    }

    public static void setBoolean(int key, boolean value) {
        options[key] = new Boolean(value);
    }

    public static void setLong(int key, long value) {
        options[key] = new Long(value);
    }
}
