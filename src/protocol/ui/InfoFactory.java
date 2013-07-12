package protocol.ui;

import jimm.comm.Config;
import jimmui.view.icons.ImageList;
import protocol.Profile;
import protocol.Protocol;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 07.07.13 15:24
 *
 * @author vladimir
 */
public class InfoFactory {
    public static InfoFactory factory = new InfoFactory();
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private XStatusInfo[] xStatuses = new XStatusInfo[21];
    // #sijapp cond.end #

    // #sijapp cond.if modules_XSTATUSES is "true" #
    public XStatusInfo getXStatusInfo(Protocol protocol) {
        int protocolType = protocol.getProfile().getEffectiveType();
        if (null == xStatuses[protocolType]) {
            xStatuses[protocolType] = createXStatusInfo(protocolType);
        }
        return xStatuses[protocolType];
    }

    private XStatusInfo createXStatusInfo(int protocolType) {
        String[] names;
        ImageList icons = null;
        Config config;
        switch (protocolType) {
                // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                config = new Config().loadLocale("/icq-xstatus.txt");
                names = config.getValues();
                icons = ImageList.createImageList("/icq-xstatus.png");
                break;
                // #sijapp cond.end #

                // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                config = new Config().loadLocale("/mrim-xstatus.txt");
                icons = ImageList.createImageList("/mrim-xstatus.png");
                names = config.getValues();
                break;
                // #sijapp cond.end #

            // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_JABBER:
                config = new Config().loadLocale("/jabber-xstatus.txt");
                names = config.getValues();
                icons = ImageList.createImageList("/jabber-xstatus.png");
                break;
            // #sijapp cond.end #

            // #sijapp cond.if protocols_OBIMP is "true" #
            case Profile.PROTOCOL_OBIMP:
                config = new Config().loadLocale("/obimp-xstatus.txt");
                names = config.getValues();
                icons = ImageList.createImageList("/obimp-xstatus.png");
                break;
            // #sijapp cond.end #

            default:
                names = new String[0];
        }
        return new XStatusInfo(icons, names);
    }
    // #sijapp cond.end #
}
