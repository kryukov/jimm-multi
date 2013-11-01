/*
 * Profile.java
 *
 * Created on 23 Январь 2010 г., 15:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimm.comm.StringUtils;
import jimm.comm.Util;
import protocol.ui.StatusInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class Profile {

    public static final byte PROTOCOL_ICQ = 0;
    public static final byte PROTOCOL_MRIM = 1;
    public static final byte PROTOCOL_XMPP = 2;
    public static final byte PROTOCOL_OBIMP = 9;
    public static final byte PROTOCOL_FACEBOOK = 10;
    public static final byte PROTOCOL_LJ = 11;
    public static final byte PROTOCOL_YANDEX = 12;
    public static final byte PROTOCOL_GTALK = 14;
    public static final byte PROTOCOL_QIP = 15;
    public static final byte PROTOCOL_ODNOKLASSNIKI = 16;
    public static final byte PROTOCOL_VK_API = 20;
    public static final String[] protocolNames = Util.explode((""
            // #sijapp cond.if protocols_ICQ is "true"#
            + "|ICQ"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            + "|Mail.ru Agent"
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            + "|XMPP (Jabber)"
            + "|Facebook"
            + "|\u041e\u0434\u043d\u043e\u043a\u043b\u0430\u0441\u0441\u043d\u0438\u043a\u0438"
            + "|LiveJournal"
            + "|GTalk"
            + "|Ya.Online"
            + "|QIP"
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
            PROTOCOL_XMPP,
            PROTOCOL_FACEBOOK,
            PROTOCOL_ODNOKLASSNIKI,
            PROTOCOL_LJ,
            PROTOCOL_GTALK,
            PROTOCOL_YANDEX,
            PROTOCOL_QIP,
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
            "Login",
            "ID",
            "Login",
            "Login",
            "Login",
            "Login",
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

    public byte statusIndex = StatusInfo.STATUS_OFFLINE;
    public String statusMessage;

    public byte xstatusIndex = -1;
    public String xstatusTitle;
    public String xstatusDescription;
    public boolean isActive;

    public Profile() {
        protocolType = Profile.protocolTypes[0];
    }

    public boolean isConnected() {
        return StatusInfo.STATUS_OFFLINE != statusIndex;
    }

    public byte getEffectiveType() {
        return getEffectiveType(protocolType);
    }

    public boolean equalsTo(Profile profile) {
        return this == profile
                || ((protocolType == profile.protocolType) && userId.equals(profile.userId));
    }

    public boolean isValid() {
        if (StringUtils.isEmpty(userId)) return false;
        int exist = -1;
        for (int i = 0; i < protocolTypes.length; ++i) {
            if (protocolType == protocolTypes[i]) exist = i;
        }
        return -1 < exist;
    }

    public static byte getEffectiveType(byte protocolType) {
        // #sijapp cond.if protocols_JABBER is "true" #
        switch (protocolType) {
            case Profile.PROTOCOL_GTALK:
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_QIP:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return Profile.PROTOCOL_XMPP;
        }
        // #sijapp cond.end #
        return protocolType;
    }
}
