package jimmui.view.roster.items;

import jimm.Jimm;
import jimm.Options;
import jimm.chat.message.Message;
import jimm.cl.ContactList;
import jimm.comm.Sortable;
import jimmui.view.icons.Icon;
import protocol.Contact;
import protocol.Protocol;
import protocol.ui.ClientInfo;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;
import protocol.ui.XStatusInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 03.08.13 13:12
 *
 * @author vladimir
 */
public class ContactNode implements TreeNode, Sortable {
    private Contact c;
    @Override
    public String getText() {
        return c.getName();
    }

    @Override
    public void getLeftIcons(Icon[] lIcons) {
        if (c.isTyping()) {
            lIcons[0] = InfoFactory.msgIcons.iconAt(Message.ICON_TYPE);
        } else {
            lIcons[0] = InfoFactory.msgIcons.iconAt(c.getUnreadMessageIcon());
        }
        Protocol protocol = getProtocol();
        if (null != protocol) {
            if (null == lIcons[0]) {
                lIcons[0] = InfoFactory.factory.getStatusInfo(protocol).getIcon(c.getStatusIndex());
            }
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (XStatusInfo.XSTATUS_NONE != c.getXStatusIndex()) {
                lIcons[1] = InfoFactory.factory.getXStatusInfo(protocol).getIcon(c.getXStatusIndex());
            }
            // #sijapp cond.end #
        }

        if (!c.isTemp() && !c.isAuth()) {
            lIcons[3] = InfoFactory.authIcon;
        }
        // #sijapp cond.if modules_SERVERLISTS is "true" #
        lIcons[4] = InfoFactory.factory.getServerListIcon(c);
        // #sijapp cond.end #
    }

    @Override
    public void getRightIcons(Icon[] icons) {
        // #sijapp cond.if modules_CLIENTS is "true" #
        ClientInfo info = InfoFactory.factory.getClientInfo(getProtocol());
        icons[0] = (null != info) ? info.getIcon(c.clientIndex) : null;
        // #sijapp cond.end #
    }


    private Protocol getProtocol() {
        return Jimm.getJimm().jimmModel.getProtocol(c);
    }

    ////
    // Node weight declaration.
    // -3       - normal group
    // -2       - non editable group
    // -1       - non removable group
    //  9       - chat group (online)
    // 10       - contact with message
    // 20 - 49  - normal-contact (status)
    // 50       - chat group (offline)
    // 51       - offline-contact
    // 60       - temp-contact
    public final int getNodeWeight() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!c.isSingleUserContact()) {
            return c.isOnline() ? 9 : 50;
        }
        // #sijapp cond.end #
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && c.hasUnreadMessage()) {
            return 10;
        }
        int sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
        if (ContactList.SORT_BY_NAME == sortType) {
            return 20;
        }
        if (c.isOnline()) {
            switch (sortType) {
                case ContactList.SORT_BY_STATUS:
                    // 29 = 49 - 20 last normal status
                    return 20 + StatusInfo.getWidth(c.getStatusIndex());
                case ContactList.SORT_BY_ONLINE:
                    return 20;
            }
        }

        if (c.isTemp()) {
            return 60;
        }
        return 51;
    }

}
