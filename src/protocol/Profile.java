/*
 * Profile.java
 *
 * Created on 23 Январь 2010 г., 15:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimm.comm.Util;

/**
 *
 * @author Vladimir Krukov
 */
public final class Profile {

    public static final int PROTOCOL_ICQ = 0;
    public static final int PROTOCOL_MRIM = 1;
    public static final int PROTOCOL_JABBER = 2;
    public static final int PROTOCOL_MSN = 4;
    public static final int PROTOCOL_OBIMP = 9;
    public static final int PROTOCOL_FACEBOOK = 10;
    public static final int PROTOCOL_LJ = 11;
    public static final int PROTOCOL_YANDEX = 12;
    public static final int PROTOCOL_VK = 13;
    public static final int PROTOCOL_GTALK = 14;
    public static final int PROTOCOL_QIP = 15;
    public static final int PROTOCOL_ODNOKLASSNIKI = 16;
    public static final int PROTOCOL_VK_API = 20;
    public static final String[] protocolNames = Util.explode((""
            // #sijapp cond.if protocols_ICQ is "true"#
            + "|ICQ"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            + "|Mail.ru Agent"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            + "|Jabber"
            // #sijapp cond.if modules_MULTI is "true" #
            + "|Facebook"
            + "|\u041e\u0434\u043d\u043e\u043a\u043b\u0430\u0441\u0441\u043d\u0438\u043a\u0438"
            + "|VKontakte"
            + "|LiveJournal"
            + "|GTalk"
            + "|Ya.Online"
            + "|QIP"
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            + "|MSN"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_OBIMP is "true" #
            + "|OBIMP"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_VKAPI is "true" #
            + "|vk.com (api)"
            // #sijapp cond.end #
    ).substring(1), '|');
    public static final byte[] protocolTypes = new byte[] {
            // #sijapp cond.if protocols_ICQ is "true"#
            PROTOCOL_ICQ,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            PROTOCOL_MRIM,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            PROTOCOL_JABBER,
            // #sijapp cond.if modules_MULTI is "true" #
            PROTOCOL_FACEBOOK,
            PROTOCOL_ODNOKLASSNIKI,
            PROTOCOL_VK,
            PROTOCOL_LJ,
            PROTOCOL_GTALK,
            PROTOCOL_YANDEX,
            PROTOCOL_QIP,
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            PROTOCOL_MSN,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_OBIMP is "true" #
            PROTOCOL_OBIMP,
            // #sijapp cond.end #
            // #sijapp cond.if protocols_VKAPI is "true" #
            PROTOCOL_VK_API,
            // #sijapp cond.end #
    };
    public static final String[] protocolIds = new String[] {
            // #sijapp cond.if protocols_ICQ is "true"#
            "UIN/E-mail",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            "e-mail",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            "jid",
            // #sijapp cond.if modules_MULTI is "true" #
            "Login",
            "ID",
            "ID",
            "Login",
            "Login",
            "Login",
            "Login",
            // #sijapp cond.end #
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            "LiveID/E-mail",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_OBIMP is "true" #
            "ObimpID",
            // #sijapp cond.end #
            // #sijapp cond.if protocols_VKAPI is "true" #
            "E-mail/phone",
            // #sijapp cond.end #
    };

    public byte protocolType;
    public String userId = "";
    public String password = "";
    public String nick = "";

    public byte statusIndex = 1;
    public String statusMessage;

    public byte xstatusIndex = -1;
    public String xstatusTitle;
    public String xstatusDescription;
    public boolean isActive;
    public boolean isConnected;

    public Profile() {
        protocolType = Profile.protocolTypes[0];
    }
}
