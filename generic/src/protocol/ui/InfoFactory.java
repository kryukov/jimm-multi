package protocol.ui;

import jimm.chat.message.Message;
import jimm.comm.Config;
import jimmui.view.icons.Icon;
import jimmui.view.icons.ImageList;
import protocol.Contact;
import protocol.Profile;
import protocol.Protocol;
import protocol.icq.ClientDetector;
import protocol.xmpp.XmppClient;
import protocol.mrim.MrimClient;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 07.07.13 15:24
 *
 * @author vladimir
 */
public class InfoFactory {
    public static final ImageList msgIcons = ImageList.createImageList("/msgs.png");
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private final ImageList serverListsIcons = ImageList.createImageList("/serverlists.png");
    // #sijapp cond.end #
    // #sijapp cond.if protocols_ICQ is "true" #
    public static final Icon happyIcon = ImageList.createImageList("/happy.png").iconAt(0);
    // #sijapp cond.end #
    public static final Icon authIcon = ImageList.createImageList("/auth.png").iconAt(0);
    protected StatusInfo[] info = new StatusInfo[21];
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private XStatusInfo[] xStatuses = new XStatusInfo[21];
    // #sijapp cond.end #
    // #sijapp cond.if modules_CLIENTS is "true" #
    private ClientInfo[] clientInfo = new ClientInfo[21];
    // #sijapp cond.end #
    public final StatusInfo global = createGlobalStatusInfo();

    // #sijapp cond.if modules_SERVERLISTS is "true" #
    public Icon getServerListIcon(Contact contact) {
        int privacyList = -1;
        if (contact.inIgnoreList()) {
            privacyList = 0;
        } else if (contact.inInvisibleList()) {
            privacyList = 1;
        } else if (contact.inVisibleList()) {
            privacyList = 2;
        }
        return serverListsIcons.iconAt(privacyList);
    }
    // #sijapp cond.end #

    public StatusInfo getStatusInfo(Protocol protocol) {
        byte protocolType = protocol.getProfile().protocolType;
        if (null == info[protocolType]) {
            info[protocolType] = createStatusInfo(protocolType);
        }
        return info[protocolType];
    }

    public StatusInfo getStatusInfo(byte protocolType) {
        if (null == info[protocolType]) {
            info[protocolType] = createStatusInfo(protocolType);
        }
        return info[protocolType];
    }

    private StatusInfo createStatusInfo(byte protocolType) {
        int[] statusIconIndex = new int[]{1, 0, 3, 4, -1, -1, -1, -1, -1, 6, -1, 5, -1, -1, 1};
        byte[] applicableStatuses = xmppStatuses;
        String file = "jabber";
        switch (protocolType) {
            case Profile.PROTOCOL_GTALK:
                file = "gtalk";
                break;
            case Profile.PROTOCOL_FACEBOOK:
                file = "facebook";
                break;
            case Profile.PROTOCOL_LJ:
                file = "livejournal";
                break;
            case Profile.PROTOCOL_YANDEX:
                file = "ya";
                break;
            case Profile.PROTOCOL_QIP:
                file = "qip";
                break;
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                file = "o" + "k";
                break;
            // #sijapp cond.if protocols_VKAPI is "true" #
            case Profile.PROTOCOL_VK_API:
                file = "v" + "k";
                statusIconIndex = new int[]{1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1};
                applicableStatuses = vkApiStatuses;
                break;
            // #sijapp cond.end #
            case Profile.PROTOCOL_OBIMP:
                file = "obimp";
                statusIconIndex = new int[]{2, 0, 6, 5, 10, 11, -1, -1, 12, 7, 8, 9, 4, 3, 1};
                applicableStatuses = obimpStatuses;
                break;
            // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                file = "mrim";
                statusIconIndex = new int[]{1, 0, 3, 4, -1, -1, -1, -1, -1, -1, 5, -1, 2, -1, 1};
                applicableStatuses = mrimStatuses;
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                file = "icq";
                statusIconIndex = new int[]{1, 0, 4, 3, 10, 11, 8, 9, 12, 5, 6, 7, 2, 2, 1};
                applicableStatuses = icqStatuses;
                break;
            // #sijapp cond.end #
        }
        ImageList statusIcons = ImageList.createImageList("/" + file + "-status.png");
        if (0 == statusIcons.size()) {
            statusIcons = ImageList.createImageList("/jabber-status.png");
        }
        return new StatusInfo(statusIcons, statusIconIndex, applicableStatuses);
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public ClientInfo getClientInfo(Protocol protocol) {
        if (null == protocol) return null;
        int protocolType = protocol.getProfile().getEffectiveType();
        if (null == clientInfo[protocolType]) {
            clientInfo[protocolType] = createClientInfo(protocolType);
        }
        return clientInfo[protocolType];
    }

    private ClientInfo createClientInfo(int protocolType) {
        switch (protocolType) {
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                return ClientDetector.instance.get();
            // #sijapp cond.end #

            // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                return MrimClient.get();
            // #sijapp cond.end #

            // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_XMPP:
                return XmppClient.get();
            // #sijapp cond.end #

            // #sijapp cond.if protocols_OBIMP is "true" #
            case Profile.PROTOCOL_OBIMP:
                return null;
            // #sijapp cond.end #

            default:
        }
        return null;
    }
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
        ImageList icons;
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
            case Profile.PROTOCOL_XMPP:
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
                icons = ImageList.createImageList("/unk-xstatus.unk");
        }
        return new XStatusInfo(icons, names);
    }
    public boolean onlyOneIcon(Protocol protocol) {
        // #sijapp cond.if protocols_MRIM is "true" #
        switch (protocol.getProfile().protocolType) {
            case Profile.PROTOCOL_MRIM: return true;
        }
        // #sijapp cond.end #
        return false;
    }
    // #sijapp cond.end #
    private static StatusInfo createGlobalStatusInfo() {
        final ImageList icons = ImageList.createImageList("/global-status.png");
        final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1};
        return new StatusInfo(icons, statusIconIndex, globalStatuses);
    }

    public int getMoreImportant(int v1, int v2) {
        if ((Message.ICON_IN_MSG_HI == v1) || (Message.ICON_IN_MSG_HI == v2)) {
            return Message.ICON_IN_MSG_HI;
        }
        if ((Message.ICON_SYSREQ == v1) || (Message.ICON_SYSREQ == v2)) {
            return Message.ICON_SYSREQ;
        }
        if ((Message.ICON_IN_MSG == v1) || (Message.ICON_IN_MSG == v2)) {
            return Message.ICON_IN_MSG;
        }
        if ((Message.ICON_SYS_OK == v1) || (Message.ICON_SYS_OK == v2)) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }



    private static final byte[] icqStatuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_NA,
            StatusInfo.STATUS_OCCUPIED,
            StatusInfo.STATUS_DND,
            StatusInfo.STATUS_EVIL,
            StatusInfo.STATUS_DEPRESSION,
            StatusInfo.STATUS_LUNCH,
            StatusInfo.STATUS_HOME,
            StatusInfo.STATUS_WORK,
            StatusInfo.STATUS_INVISIBLE,
            StatusInfo.STATUS_INVIS_ALL};

    private static final byte[] xmppStatuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_XA,
            StatusInfo.STATUS_DND};

    private static final byte[] mrimStatuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_UNDETERMINATED,
            StatusInfo.STATUS_INVISIBLE};

    private static final byte[] obimpStatuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_NA,
            StatusInfo.STATUS_OCCUPIED,
            StatusInfo.STATUS_DND,
            StatusInfo.STATUS_LUNCH,
            StatusInfo.STATUS_HOME,
            StatusInfo.STATUS_WORK,
            StatusInfo.STATUS_INVISIBLE,
            StatusInfo.STATUS_INVIS_ALL};

    private static final byte[] globalStatuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY};

    private static final byte[] vkApiStatuses = {StatusInfo.STATUS_ONLINE};

    public static InfoFactory factory = new InfoFactory();
}
