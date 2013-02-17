/*
 * JabberClient.java
 *
 * Created on 26 Апрель 2009 г., 19:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
// #sijapp cond.if modules_CLIENTS is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import jimm.comm.Config;
import protocol.ClientInfo;

/**
 *
 * @author Vladimir Krukov
 */
public final class JabberClient {
    private static final ImageList clientIcons = ImageList.createImageList("/jabber-clients.png");
    private static final String[] clientCaps;
    private static final String[] clientNames;
    static {
        Config cfg = new Config().load("/jabber-clients.txt");
        clientCaps = cfg.getKeys();
        clientNames = cfg.getValues();
    }
    public static ClientInfo get() {
        return new ClientInfo(clientIcons, clientNames);
    }
    public static final byte CLIENT_NONE = -1;

    public static short createClient(String caps) {
        if (null == caps) {
            return CLIENT_NONE;
        }
        caps = caps.toLowerCase();
        for (short capsIndex = 0; capsIndex < clientCaps.length; ++capsIndex) {
            if (-1 != caps.indexOf(clientCaps[capsIndex])) {
                return capsIndex;
            }
        }
        return CLIENT_NONE;
    }

}
// #sijapp cond.end #
// #sijapp cond.end #