/*
 * MrimClient.java
 *
 * Created on 26 Апрель 2009 г., 19:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MRIM is "true" #
// #sijapp cond.if modules_CLIENTS is "true" #
package protocol.mrim;

import DrawControls.icons.*;
import jimm.comm.Config;
import jimm.comm.StringConvertor;
import jimm.modules.*;
import protocol.ClientInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class MrimClient {
    private static final ImageList clientIcons = ImageList.createImageList("/mrim-clients.png");
    private static final String[] clientIds;
    private static final String[] clientNames;
    static {
        Config cfg = new Config().load("/mrim-clients.txt");
        clientIds = cfg.getKeys();
        clientNames = cfg.getValues();
    }

    static ClientInfo get() {
        return new ClientInfo(clientIcons, clientNames);
    }

    /** Creates a new instance of MrimClient */
    private MrimClient() {
    }

    private static String getValue(String str, String key) {
        String fullKey = key + "=\"";
        int keyIndex = str.indexOf(fullKey);
        int valueIndex = keyIndex + fullKey.length();
        int endIndex = str.indexOf('"', valueIndex);
        if ((-1 == keyIndex) || (-1 == endIndex)) {
            return "";
        }
        return str.substring(valueIndex, endIndex);
    }
    static public void createClient(MrimContact contact, String caps) {
        if (StringConvertor.isEmpty(caps)) {
            contact.setClient(ClientInfo.CLI_NONE, null);
            return;
        }

        String clientClient = getValue(caps, "cl" + "ient");
        String clientName = getValue(caps, "n" + "ame");
        short clientIndex = ClientInfo.CLI_NONE;
        for (short idIndex = 0; idIndex < clientIds.length; ++idIndex) {
            if (-1 != clientName.indexOf(clientIds[idIndex])) {
                clientIndex = idIndex;
                break;
            }
            if (-1 != clientClient.indexOf(clientIds[idIndex])) {
                clientIndex = idIndex;
                break;
            }
        }
        if (ClientInfo.CLI_NONE == clientIndex) {
            // #sijapp cond.if modules_DEBUGLOG is "true"#
            DebugLog.println("Unknown client: " + caps);
            // #sijapp cond.end#
            contact.setClient(ClientInfo.CLI_NONE, null);
            return;
        }
        contact.setClient(clientIndex, getValue(caps, "v" + "ersion"));
    }
}
// #sijapp cond.end #
// #sijapp cond.end #