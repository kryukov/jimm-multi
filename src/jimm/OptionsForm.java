/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
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
 *******************************************************************************
 * File: src/jimm/Options.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin,
 * Vladimir Kryukov
 ******************************************************************************/
package jimm;

import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.base.*;
import jimm.ui.form.*;
import jimm.ui.menu.*;
import jimm.util.*;

/* Form for editing option values */
public class OptionsForm implements FormListener, ControlStateListener, SelectListener {

    private MenuModel optionsMenu = new MenuModel();
    private GraphForm form;
    private int currentOptionsForm;
    // Static constants for menu actios
    private static final int OPTIONS_ACCOUNT = 0;
    private static final int OPTIONS_INTERFACE = 3;
    private static final int OPTIONS_HOTKEYS = 5;
    private static final int OPTIONS_SIGNALING = 6;
    // #sijapp cond.if modules_TRAFFIC is "true"#
    private static final int OPTIONS_TRAFFIC = 7;
    // #sijapp cond.end#
    private static final int OPTIONS_TIMEZONE = 8;
    private static final int OPTIONS_ANTISPAM = 9;
    private static final int OPTIONS_ABSENCE = 10;
    final private String[] hotkeyActionNames = Util.explode(
            "ext_hotkey_action_none"
            + "|" + "info"
            + "|" + "open_chats"
            // #sijapp cond.if modules_HISTORY is "true"#
            + "|" + "history"
            // #sijapp cond.end#
            + "|" + "ext_hotkey_action_onoff"
            + "|" + "keylock"
            // #sijapp cond.if target is "MIDP2" #
            + "|" + "minimize"
            // #sijapp cond.end#
            // #sijapp cond.if modules_SOUND is "true" #
            + "|" + "#sound_off"
            // #sijapp cond.end#
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            + "|" + "magic eye"
            // #sijapp cond.end#
            // #sijapp cond.if modules_FILES is "true"#
            + "|" + "ft_cam"
            // #sijapp cond.end#
            + "|" + "user_statuses"
            + "|" + "collapse_all_groups",
            '|');
    final private int[] hotkeyActions = {
            Options.HOTKEY_NONE,
            Options.HOTKEY_INFO,
            Options.HOTKEY_OPEN_CHATS,
            // #sijapp cond.if modules_HISTORY is "true"#
            Options.HOTKEY_HISTORY,
            // #sijapp cond.end#
            Options.HOTKEY_ONOFF,
            Options.HOTKEY_LOCK,
            // #sijapp cond.if target is "MIDP2" #
            Options.HOTKEY_MINIMIZE,
            // #sijapp cond.end#
            // #sijapp cond.if modules_SOUND is "true" #
            Options.HOTKEY_SOUNDOFF,
            // #sijapp cond.end#
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            Options.HOTKEY_MAGIC_EYE,
            // #sijapp cond.end#
            // #sijapp cond.if modules_FILES is "true"#
            Options.HOTKEY_SEND_PHOTO,
            // #sijapp cond.end#
            Options.HOTKEY_STATUSES,
            Options.HOTKEY_COLLAPSE_ALL
    };

    public OptionsForm() {
    }

    // Initialize the kist for the Options menu
    private void initOptionsList() {
        optionsMenu.clean();

        optionsMenu.addItem("options_account", OPTIONS_ACCOUNT);
        optionsMenu.addItem("options_interface", OPTIONS_INTERFACE);

        // #sijapp cond.if modules_ANDROID isnot "true" #
        if (3 != Options.getInt(Options.OPTION_KEYBOARD)) {
            optionsMenu.addItem("options_hotkeys", OPTIONS_HOTKEYS);
        }
        // #sijapp cond.end#
        optionsMenu.addItem("options_signaling", OPTIONS_SIGNALING);
        // #sijapp cond.if modules_ANTISPAM is "true"#
        optionsMenu.addItem("antispam", OPTIONS_ANTISPAM);
        // #sijapp cond.end#
        // #sijapp cond.if modules_ABSENCE is "true" #
        optionsMenu.addItem("absence", OPTIONS_ABSENCE);
        // #sijapp cond.end#
        // #sijapp cond.if modules_TRAFFIC is "true"#
        optionsMenu.addItem("traffic_lng", OPTIONS_TRAFFIC);
        // #sijapp cond.end#
        // #sijapp cond.if modules_ANDROID isnot "true" #
        optionsMenu.addItem("time_zone", OPTIONS_TIMEZONE);
        // #sijapp cond.end#
        optionsMenu.setActionListener(this);
        optionsMenu.setDefaultItemCode(currentOptionsForm);
    }

    private void addHotKey(String keyName, int option) {
        int optionValue = Options.getInt(option);
        int def = 0;
        for (int i = 0; i < hotkeyActionNames.length; ++i) {
            if (hotkeyActions[i] == optionValue) {
                def = i;
                break;
            }
        }
        form.addSelector(option, keyName, Util.implode(hotkeyActionNames, "|"), def);
    }

    private void saveHotKey(int option) {
        Options.setInt(option, hotkeyActions[form.getSelectorValue(option)]);
    }

    private void initHotkeyMenuUI() {
        addHotKey("ext_clhotkey0", Options.OPTION_EXT_CLKEY0);
        addHotKey("ext_clhotkey4", Options.OPTION_EXT_CLKEY4);
        addHotKey("ext_clhotkey6", Options.OPTION_EXT_CLKEY6);
        addHotKey("ext_clhotkeystar", Options.OPTION_EXT_CLKEYSTAR);
        addHotKey("ext_clhotkeypound", Options.OPTION_EXT_CLKEYPOUND);
        // #sijapp cond.if target is "MIDP2" #
        String label = JLocale.getString("camera") + " / " + JLocale.getString("ext_clhotkeycall");
        addHotKey(label, Options.OPTION_EXT_CLKEYCALL);
        // #sijapp cond.end#
    }

    ///////////////////////////////////////////////////////////////////////////

    /* Activate options menu */
    public void show() {
        initOptionsList();
        new Select(optionsMenu).show();
    }

    private void setChecked(String lngStr, int optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void createNotifyControls(int modeOpt, String title) {
        // #sijapp cond.if modules_SOUND is "true" #
        form.addCheckBox(modeOpt, title, 0 < Options.getInt(modeOpt));
        // #sijapp cond.end#
    }

    private void saveNotifyControls(int opt) {
        // #sijapp cond.if modules_SOUND is "true" #
        Options.setInt(opt, form.getCheckBoxValue(opt) ? 2 : 0);
        // #sijapp cond.end#
    }

    /* Helpers for options UI: */
    private void createSelector(String cap, String items, int opt) {
        form.addSelector(opt, cap, items, Options.getInt(opt));
    }

    private void loadOptionString(int opt, String label, int size) {
        form.addTextField(opt, label, Options.getString(opt), size);
    }

    private void saveOptionString(int opt) {
        Options.setString(opt, form.getTextFieldValue(opt));
    }

    private void saveOptionBoolean(int opt) {
        Options.setBoolean(opt, form.getCheckBoxValue(opt));
    }

    private void saveOptionSelector(int opt) {
        Options.setInt(opt, form.getSelectorValue(opt));
    }
    // #sijapp cond.if modules_SOUND is "true" #

    private void loadOptionGauge(int opt, String label) {
        form.addVolumeControl(opt, label, Options.getInt(opt));
    }

    private void saveOptionGauge(int opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
    }
    // #sijapp cond.end#
    // #sijapp cond.if modules_TRAFFIC is "true"#

    private void loadOptionDecimal(int opt, String label) {
        form.addTextField(opt, label, Util.intToDecimal(Options.getInt(opt)),
                6);
    }

    private void saveOptionDecimal(int opt) {
        Options.setInt(opt, Util.decimalToInt(form.getTextFieldValue(opt)));
    }
    // #sijapp cond.end#

    private void loadOptionInt(int opt, String label, String variants) {
        String current = String.valueOf(Options.getInt(opt));
        String[] alts = Util.explode(variants, '|');
        int selected = 0;
        for (int i = 0; i < alts.length; ++i) {
            if (alts[i].equals(current)) {
                selected = i;
            }
        }
        form.addSelector(opt, label, alts, selected);
    }

    private void saveOptionInt(int opt) {
        int val = Util.strToIntDef(form.getSelectorString(opt).trim(), 0);
        Options.setInt(opt, val);
    }

    private void loadOptionInt(int opt, String label, String[] variants, short[] alts) {
        int current = Options.getInt(opt);
        int selected = 0;
        for (int i = 0; i < alts.length; ++i) {
            if (alts[i] == current) {
                selected = i;
            }
        }
        form.addSelector(opt, label, variants, selected);
    }

    private void saveOptionInt(int opt, short[] alts) {
        Options.setInt(opt, alts[form.getSelectorValue(opt)]);
    }

    private static final short[] minItemMultipliers = new short[]{10, 15, 20, 30};
    private static final String[] minItems = {"x1", "x1.5", "x2", "x3"};

    /* Command listener */
    public void formAction(GraphForm form, boolean apply) {
        /* Look for back command */
        if (!apply) {
            back();

            // Look for save command
        } else {
            // Save values, depending on selected option menu item
            switch (currentOptionsForm) {
                case OPTIONS_INTERFACE:
                    if (JLocale.langAvailable.length > 1) {
                        int lang = form.getSelectorValue(Options.OPTION_UI_LANGUAGE);
                        Options.setString(Options.OPTION_UI_LANGUAGE, JLocale.langAvailable[lang]);
                    }
                    String[] colorSchemes = Scheme.getSchemeNames();
                    if (1 < colorSchemes.length) {
                        saveOptionSelector(Options.OPTION_COLOR_SCHEME);
                        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
                    }

                    saveOptionSelector(Options.OPTION_FONT_SCHEME);
                    GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
                    saveOptionInt(Options.OPTION_MIN_ITEM_SIZE, minItemMultipliers);
                    CanvasEx.updateUI();

                    // #sijapp cond.if modules_TOUCH isnot "true"#
                    saveOptionBoolean(Options.OPTION_SHOW_SOFTBAR);
                    // #sijapp cond.end#

                    saveOptionBoolean(Options.OPTION_USER_GROUPS);
                    saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                    saveOptionBoolean(Options.OPTION_SAVE_TEMP_CONTACT);
                    saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG);
                    saveOptionBoolean(Options.OPTION_SHOW_STATUS_LINE);

                    saveOptionSelector(Options.OPTION_CL_SORT_BY);

                    // #sijapp cond.if modules_HISTORY is "true"#
                    saveOptionBoolean(Options.OPTION_HISTORY);
                    // #sijapp cond.end#
                    // #sijapp cond.if modules_ANDROID isnot "true" #
                    saveOptionBoolean(Options.OPTION_CLASSIC_CHAT);
                    saveOptionBoolean(Options.OPTION_SWAP_SEND_AND_BACK);
                    saveOptionBoolean(Options.OPTION_TF_FLAGS);
                    saveOptionBoolean(Options.OPTION_UNTITLED_INPUT);
                    saveOptionBoolean(Options.OPTION_RECREATE_TEXTBOX);
                    saveOptionBoolean(Options.OPTION_DETRANSLITERATE);
                    saveOptionSelector(Options.OPTION_INPUT_MODE);
                    saveOptionSelector(Options.OPTION_KEYBOARD);
                    // #sijapp cond.end#

                    saveOptionInt(Options.OPTION_MAX_MSG_COUNT);

                    // #sijapp cond.if modules_LIGHT is "true" #
                    if (CustomLight.isSupport()) {
                        saveOptionSelector(Options.OPTION_LIGHT_THEME);
                        CustomLight.switchOn(Options.getInt(Options.OPTION_LIGHT_THEME));
                    }
                    // #sijapp cond.end#

                    ContactList.getInstance().getManager().update();
                    break;

                case OPTIONS_HOTKEYS:
                    saveHotKey(Options.OPTION_EXT_CLKEY0);
                    saveHotKey(Options.OPTION_EXT_CLKEY4);
                    saveHotKey(Options.OPTION_EXT_CLKEY6);
                    saveHotKey(Options.OPTION_EXT_CLKEYSTAR);
                    saveHotKey(Options.OPTION_EXT_CLKEYPOUND);
                    // #sijapp cond.if target is "MIDP2" #
                    saveHotKey(Options.OPTION_EXT_CLKEYCALL);
                    // #sijapp cond.end#
                    break;

                case OPTIONS_SIGNALING:
                    // #sijapp cond.if modules_SOUND is "true" #
                    saveOptionGauge(Options.OPTION_NOTIFY_VOLUME);
                    // #sijapp cond.end#
                    saveOptionSelector(Options.OPTION_VIBRATOR);
                    // #sijapp cond.if modules_SOUND is "true" #
                    saveNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE);
                    saveNotifyControls(Options.OPTION_MESS_NOTIF_MODE);
                    // #sijapp cond.end#
                    saveOptionBoolean(Options.OPTION_NOTIFY_IN_AWAY);
                    saveOptionBoolean(Options.OPTION_ALARM);
                    saveOptionBoolean(Options.OPTION_BLOG_NOTIFY);
                    saveOptionSelector(Options.OPTION_TYPING_MODE);

                    // #sijapp cond.if modules_ANDROID isnot "true" #
                    // #sijapp cond.if target="MIDP2"#
                    saveOptionBoolean(Options.OPTION_BRING_UP);
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    break;

                // #sijapp cond.if modules_ANTISPAM is "true"#
                case OPTIONS_ANTISPAM:
                    saveOptionString(Options.OPTION_ANTISPAM_MSG);
                    saveOptionString(Options.OPTION_ANTISPAM_ANSWER);
                    saveOptionString(Options.OPTION_ANTISPAM_HELLO);
                    saveOptionString(Options.OPTION_ANTISPAM_KEYWORDS);
                    saveOptionBoolean(Options.OPTION_ANTISPAM_ENABLE);
                    break;
                // #sijapp cond.end#

                // #sijapp cond.if modules_ABSENCE is "true" #
                case OPTIONS_ABSENCE:
                    saveOptionBoolean(Options.OPTION_AA_BLOCK);
                    Options.setInt(Options.OPTION_AA_TIME, form.getSelectorValue(Options.OPTION_AA_TIME) * 5);
                    jimm.modules.AutoAbsence.instance.updateOptions();
                    break;
                // #sijapp cond.end#

                // #sijapp cond.if modules_TRAFFIC is "true"#
                case OPTIONS_TRAFFIC:
                    saveOptionDecimal(Options.OPTION_COST_OF_1M);
                    Options.setInt(Options.OPTION_COST_PACKET_LENGTH,
                            Util.strToIntDef(form.getTextFieldValue(Options.OPTION_COST_PACKET_LENGTH), 0) * 1024);
                    saveOptionString(Options.OPTION_CURRENCY);
                    break;
                // #sijapp cond.end#

                case OPTIONS_TIMEZONE: {
                    /* Set up time zone*/
                    int timeZone = form.getSelectorValue(Options.OPTION_GMT_OFFSET) - 12;
                    Options.setInt(Options.OPTION_GMT_OFFSET, timeZone);

                    /* Translate selected time to GMT */
                    int selHour = form.getSelectorValue(Options.OPTION_LOCAL_OFFSET) - timeZone;
                    selHour = selHour - 12;

                    /* Calculate diff. between selected GMT time and phone time */
                    int localOffset = (selHour + 12 + 24) % 24 - 12;
                    Options.setInt(Options.OPTION_LOCAL_OFFSET, localOffset);
                    break;
                }

            }

            /* Save options */
            Options.safeSave();
            back();
        }

    }

    public void select(Select select, MenuModel model, int cmd) {
        // Add elements, depending on selected option menu item
        currentOptionsForm = cmd;
        // Delete all items
        form = new GraphForm("options_lng", "save", "back", this);
        form.setCaption(model.getItemText(currentOptionsForm));
        switch (currentOptionsForm) {
            case OPTIONS_ACCOUNT:
                new AccountsForm().show();
                return;

            case OPTIONS_INTERFACE:
                // Initialize elements (interface section)
                if (JLocale.langAvailable.length > 1) {
                    int cur = 0;
                    String curLang = Options.getString(Options.OPTION_UI_LANGUAGE);
                    for (int j = 0; j < JLocale.langAvailable.length; ++j) {
                        if (JLocale.langAvailable[j].equals(curLang)) {
                            cur = j;
                        }
                    }
                    form.addSelector(Options.OPTION_UI_LANGUAGE, "language", JLocale.langAvailableName, cur);
                }
                String[] colorSchemes = Scheme.getSchemeNames();
                if (colorSchemes.length > 1) {
                    form.addSelector(Options.OPTION_COLOR_SCHEME, "color_scheme", colorSchemes, Options.getInt(Options.OPTION_COLOR_SCHEME));
                }

                createSelector("fonts",
                        "fonts_smallest" + "|" + "fonts_small" + "|" + "fonts_normal" + "|" + "fonts_large" + "|" + "fonts_largest",
                        Options.OPTION_FONT_SCHEME);
                loadOptionInt(Options.OPTION_MIN_ITEM_SIZE, "item_height_multiplier", minItems, minItemMultipliers);


                form.addString("contact_list", null);
                setChecked("show_user_groups", Options.OPTION_USER_GROUPS);
                setChecked("hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                setChecked("save_temp_contacts", Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked("show_status_line", Options.OPTION_SHOW_STATUS_LINE);
                setChecked("contacts_with_msg_at_top", Options.OPTION_SORT_UP_WITH_MSG);

                createSelector("sort_by",
                        "sort_by_status" + "|" + "sort_by_online" + "|" + "sort_by_name",
                        Options.OPTION_CL_SORT_BY);

                form.addString("chat", null);
                // #sijapp cond.if modules_HISTORY is "true"#
                setChecked("use_history", Options.OPTION_HISTORY);
                // #sijapp cond.end#
                // #sijapp cond.if modules_ANDROID isnot "true" #
                setChecked("cl_chat", Options.OPTION_CLASSIC_CHAT);
                // #sijapp cond.end#
                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, "max_message_count", "10|50|100|250|500|1000");

                // #sijapp cond.if modules_ANDROID isnot "true" #
                form.addString("textbox", null);
                setChecked("swap_send_and_back", Options.OPTION_SWAP_SEND_AND_BACK);
                setChecked("auto_case", Options.OPTION_TF_FLAGS);
                setChecked("untitled_input", Options.OPTION_UNTITLED_INPUT);
                setChecked("recreate_textbox", Options.OPTION_RECREATE_TEXTBOX);
                setChecked("detransliterate", Options.OPTION_DETRANSLITERATE);
                createSelector("input_mode",
                        "default" + "|" + "latin" + "|" + "cyrillic",
                        Options.OPTION_INPUT_MODE);
                createSelector("keyboard_type",
                        "default" + "|" + "QWERTY" + "|" + "old_se_keys" + "|" + "no",
                        Options.OPTION_KEYBOARD);
                // #sijapp cond.end #
                // #sijapp cond.if modules_TOUCH isnot "true"#
                setChecked("show_softbar", Options.OPTION_SHOW_SOFTBAR);
                // #sijapp cond.end#

                // #sijapp cond.if modules_LIGHT is "true" #
                if (CustomLight.isSupport()) {
                    createSelector("light_theme",
                            "off" + "|" + "light_min" + "|" + "light_middle"
                            + "|" + "light_max" + "|" + "light_message_only"
                            + "|" + "light_always_min",
                            Options.OPTION_LIGHT_THEME);
                }
                // #sijapp cond.end#
                break;

            case OPTIONS_HOTKEYS:
                initHotkeyMenuUI();
                break;

            /* Initialize elements (Signaling section) */
            case OPTIONS_SIGNALING:
                /* Vibrator notification controls */

                // #sijapp cond.if modules_SOUND is "true" #
                if (Notify.getSound().hasAnySound()) {
                    loadOptionGauge(Options.OPTION_NOTIFY_VOLUME, "volume");
                }
                // #sijapp cond.end#

                // #sijapp cond.if modules_SOUND is "true" #
                createNotifyControls(Options.OPTION_MESS_NOTIF_MODE,
                        "message_notification");
                createNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE,
                        "onl_notification");
                setChecked("alarm", Options.OPTION_ALARM);
                setChecked("blog_notify", Options.OPTION_BLOG_NOTIFY);
                // #sijapp cond.end#
                createSelector("typing_notify",
                        "no" + "|" + "typing_incoming" + "|" + "typing_both",
                        Options.OPTION_TYPING_MODE);
                createSelector(
                        "vibration",
                        "no" + "|" + "yes" + "|" + "when_locked",
                        Options.OPTION_VIBRATOR);
                // #sijapp cond.if modules_SOUND is "true" #
                setChecked("notify_in_away", Options.OPTION_NOTIFY_IN_AWAY);
                // #sijapp cond.end#


                // #sijapp cond.if modules_ANDROID isnot "true" #
                // #sijapp cond.if target="MIDP2"#
                /* Midlet auto bring up controls on MIDP2 */
                setChecked("bring_up", Options.OPTION_BRING_UP);
                // #sijapp cond.end#
                // #sijapp cond.end#
                break;

            // #sijapp cond.if modules_ANTISPAM is "true"#
            case OPTIONS_ANTISPAM:
                setChecked("on", Options.OPTION_ANTISPAM_ENABLE);
                loadOptionString(Options.OPTION_ANTISPAM_MSG, "antispam_msg", 256);
                loadOptionString(Options.OPTION_ANTISPAM_ANSWER, "antispam_answer", 256);
                loadOptionString(Options.OPTION_ANTISPAM_HELLO, "antispam_hello", 256);
                loadOptionString(Options.OPTION_ANTISPAM_KEYWORDS, "antispam_keywords", 512);
                break;
            // #sijapp cond.end#

            // #sijapp cond.if modules_ABSENCE is "true" #
            case OPTIONS_ABSENCE:
                setChecked("after_block", Options.OPTION_AA_BLOCK);
                form.addSelector(Options.OPTION_AA_TIME, "after_time", "off" + "|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);
                //form.addChoiceGroup(Options.OPTION_AUTOABSENCE, null, Choice.MULTIPLE);
                //setChecked(Options.OPTION_AUTOABSENCE, "autoanswer", Options.OPTION_AUTOABSENCE);
                //loadOptionString(Options.OPTION_AUTOANSWER, "answer", 256);
                break;
            // #sijapp cond.end#

            /* Initialize elements (cost section) */
            // #sijapp cond.if modules_TRAFFIC is "true"#
            case OPTIONS_TRAFFIC:
                loadOptionDecimal(Options.OPTION_COST_OF_1M, "cp1m");
                form.addTextField(Options.OPTION_COST_PACKET_LENGTH,
                        "plength",
                        String.valueOf(Options.getInt(Options.OPTION_COST_PACKET_LENGTH) / 1024),
                        4, TextField.NUMERIC);
                loadOptionString(Options.OPTION_CURRENCY, "currency", 4);
                break;
            // #sijapp cond.end#

            case OPTIONS_TIMEZONE: {
                int gmtOffset = Options.getInt(Options.OPTION_GMT_OFFSET);

                String[] timezones = new String[26];
                for (int i = -12; i <= 13; ++i) {
                    timezones[i + 12] = "GMT" + (i < 0 ? "" : "+") + i + ":00";
                }
                form.addSelector(Options.OPTION_GMT_OFFSET, "time_zone",
                        timezones, gmtOffset + 12);

                int cur = 0;
                long now = Util.createCurrentLocalTime();
                int minutes = (int)((now / 60) % 60);
                int hour = (int)((now / (60 * 60)) % 24);
                int startHour = hour - Options.getInt(Options.OPTION_LOCAL_OFFSET)
                        - Options.getInt(Options.OPTION_GMT_OFFSET) - 12;
                String[] localHours = new String[24];
                for (int i = 0; i < localHours.length; ++i) {
                    int h = ((startHour + i + 24) % 24);
                    localHours[i] = h + ":" + Util.makeTwo(minutes);
                    if (hour == h) {
                        cur = i;
                    }
                }
                form.addSelector(Options.OPTION_LOCAL_OFFSET, "local_time",
                        localHours, cur);
                break;
            }
        }
        form.setControlStateListener(this);
        form.show();
    }

    private void back() {
        form.back();
    }

    public void controlStateChanged(GraphForm form, int id) {
        switch (id) {
            case Options.OPTION_COLOR_SCHEME:
                saveOptionSelector(Options.OPTION_COLOR_SCHEME);
                Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
                CanvasEx.updateUI();
                break;
            case Options.OPTION_FONT_SCHEME:
                saveOptionSelector(Options.OPTION_FONT_SCHEME);
                GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
                CanvasEx.updateUI();
                break;
            case Options.OPTION_MIN_ITEM_SIZE:
                saveOptionInt(Options.OPTION_MIN_ITEM_SIZE, minItemMultipliers);
                CanvasEx.updateUI();
                break;
        }
    }
}