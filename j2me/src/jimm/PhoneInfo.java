package jimm;

import jimm.comm.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.07.13 23:19
 *
 * @author vladimir
 */
public class PhoneInfo {
    public static final byte PHONE_SE             = 0;
    public static final byte PHONE_SE_SYMBIAN     = 1;
    public static final byte PHONE_NOKIA          = 2;
    public static final byte PHONE_NOKIA_S40      = 3;
    public static final byte PHONE_NOKIA_S60      = 4;
    public static final byte PHONE_NOKIA_S60v8    = 5;
    public static final byte PHONE_NOKIA_N80      = 6;
    public static final byte PHONE_INTENT_JTE     = 7;
    public static final byte PHONE_JBED           = 8;
    public static final byte PHONE_SAMSUNG        = 9;
    public static final byte PHONE_ANDROID        = 10;

    public final String microeditionProfiles;
    public final String microeditionPlatform;
    public final byte generalPhoneType;

    public PhoneInfo() {
        microeditionProfiles = getSystemProperty("microedition.profiles", null);
        microeditionPlatform = getPhone();
        generalPhoneType = getGeneralPhone();
    }
    // #sijapp cond.if target is "MIDP2"#
    public boolean isS60v5() {
        String platform = StringUtils.notNull(Jimm.getJimm().phone.microeditionPlatform);
        return hasSubStr(platform, "sw_platform_version=5.");
    }
    // #sijapp cond.end#

    private String getPhone() {
        final String platform = getSystemProperty("microedition.platform", null);
        // #sijapp cond.if target is "MIDP2" #
        if (null == platform) {
            try {
                Class.forName("com.nokia.mid.ui.DeviceControl");
                return "Nokia";
            } catch (Exception ignored) {
            }
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_ANDROID is "true" #
        String android = getSystemProperty("device.model", "")
                + "/" + getSystemProperty("device.software.version", "")
                + "/" +  getSystemProperty("device.id", "");
        if (2 < android.length()) {
            return "android/" + android;
        }
        // #sijapp cond.end #
        return platform;
    }

    private byte getGeneralPhone() {
        String device = getPhone();
        if (null == device) {
            return -1;
        }
        device = device.toLowerCase();
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_ANDROID is "true" #
        if (hasSubStr(device, "android")) {
            return PHONE_ANDROID;
        }
        // #sijapp cond.end#
        if (hasSubStr(device, "ericsson")) {
            if (hasSubStr(getSystemProperty("com.sonyericsson.java.platform", "").toLowerCase(), "sjp")) {
                return PHONE_SE_SYMBIAN;
            }
            return PHONE_SE;
        }
        if (hasSubStr(device, "platform=s60")) {
            return PHONE_NOKIA_S60;
        }
        if (hasSubStr(device, "nokia")) {
            if (hasSubStr(device, "nokian80")) {
                return PHONE_NOKIA_N80;
            }
            if (null != getSystemProperty("com.nokia.memoryramfree", null)) {
                // S60 3rd Edition
                return PHONE_NOKIA_S60;
            }
            String dir = getSystemProperty("fileconn.dir.private", "");
            // s40 (6233) does not have this property
            if (hasSubStr(dir, "/private/")) {
                // it is s60 v3 fp1
                return PHONE_NOKIA_S60;
            }
            if (-1 != device.indexOf(';')) {
                return PHONE_NOKIA_S60;
            }
            return PHONE_NOKIA_S40;
        }
        if (hasSubStr(device, "samsung")) {
            return PHONE_SAMSUNG;
        }
        if (hasSubStr(device, "jbed")) {
            return PHONE_JBED;
        }
        if (hasSubStr(device, "intent")) {
            return PHONE_INTENT_JTE;
        }
        // #sijapp cond.end #
        return -1;
    }

    private boolean hasSubStr(String str, String subStr) {
        int index = str.indexOf(subStr);
        return -1 != index;
    }

    private String getSystemProperty(String key, String defVal) {
        String res = null;
        try {
            res = System.getProperty(key);
        } catch (Exception ignored) {
        }
        return StringUtils.isEmpty(res) ? defVal : res;
    }

    public boolean isPhone(final byte phone) {
        // #sijapp cond.if target is "MIDP2" #
        if (PHONE_NOKIA_S60v8 == phone) {
            return (PHONE_NOKIA_S60 == generalPhoneType)
                    && (-1 == microeditionPlatform.indexOf(';'));
        }
        if (PHONE_NOKIA == phone) {
            return (PHONE_NOKIA_S40 == generalPhoneType)
                    || (PHONE_NOKIA_S60 == generalPhoneType)
                    || (PHONE_NOKIA_N80 == generalPhoneType);
        }
        if (PHONE_SE == phone) {
            return (PHONE_SE_SYMBIAN == generalPhoneType)
                    || (PHONE_SE == generalPhoneType);
        }
        // #sijapp cond.end #
        return phone == generalPhoneType;
    }
    public int getSeVersion() {
        String sJava = getSystemProperty("com.sonyericsson.java.platform", "");
        // sJava has format "JP-x.x" or "JP-x.x.x", e.g. "JP-8.5" or "JP-8.5.2".
        // The next code also correct parse string with format "JP-x".
        // On all uncorrect strings, sonyJava set to 0.
        if ((null != sJava) && sJava.startsWith("JP-")) {
            int major = 0;
            int minor = 0;
            int micro = 0;


            if (sJava.length() >= 4) {
                major = sJava.charAt(3) - '0';
            }
            if (sJava.length() >= 6) {
                minor = sJava.charAt(5) - '0';
            }
            if (sJava.length() >= 8) {
                micro = sJava.charAt(7) - '0';
            }


            if ((0 <= major) && (major <= 9)
                    && (0 <= minor) && (minor <= 9)
                    && (0 <= micro) && (micro <= 9)) {
                return major * 100 + minor * 10 + micro;
            }
        }
        return 0;
    }

    public boolean hasMemory(int requared) {
        // #sijapp cond.if target is "MIDP2" #
        if (isPhone(PHONE_SE)) {
            return true;
        }
        if (isPhone(PHONE_NOKIA_S60)) {
            return true;
        }
        if (isPhone(PHONE_JBED)) {
            return true;
        }
        if (isPhone(PHONE_INTENT_JTE)) {
            return true;
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        if (isPhone(PHONE_ANDROID)) {
            return true;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        Jimm.gc();
        long free = Runtime.getRuntime().freeMemory();
        return (requared < free);
    }

    public boolean isCedar() {
        return hasSubStr(StringUtils.notNull(microeditionPlatform), "EricssonJ108i");
    }

    public boolean isCollapsible() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (true) return true;
        // #sijapp cond.end #
        return isPhone(PhoneInfo.PHONE_SE) || isPhone(PhoneInfo.PHONE_NOKIA_S60);
    }
}
