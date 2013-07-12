package jimm.cl;

import jimmui.view.icons.Icon;
import jimmui.view.icons.ImageList;
import jimmui.view.roster.ContactListModel;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimmui.view.menu.SelectListener;
import protocol.Protocol;
import protocol.ui.StatusInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 10.03.13 21:57
 *
 * @author vladimir
 */
public class GlobalStatusForm implements SelectListener {
    public static final StatusInfo global = getStatusInfo();

    private static StatusInfo getStatusInfo() {
        final ImageList icons = ImageList.createImageList("/global-status.png");
        final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1};
        final byte[] statuses = {
                StatusInfo.STATUS_CHAT,
                StatusInfo.STATUS_ONLINE,
                StatusInfo.STATUS_AWAY};
        return new StatusInfo(icons, statusIconIndex, statuses);
    }

    private static byte getGlobalStatus() {
        ContactListModel model = ContactList.getInstance().getManager().getModel();
        byte globalStatus = StatusInfo.STATUS_OFFLINE;
        int globalStatusWidth = StatusInfo.getWidth(globalStatus);
        for (int i = 0; i < model.getProtocolCount(); ++i) {
            byte status = model.getProtocol(i).getProfile().statusIndex;
            if (StatusInfo.getWidth(status) < globalStatusWidth) {
                globalStatus = status;
                globalStatusWidth = StatusInfo.getWidth(globalStatus);
            }
        }
        if (null == global.getIcon(globalStatus)) {
            globalStatus = StatusInfo.STATUS_ONLINE;
        }
        return globalStatus;
    }
    public static Icon getGlobalStatusIcon() {
        return global.getIcon(getGlobalStatus());
    }

    public void show() {
        MenuModel menu = new MenuModel();
        StatusInfo info = global;

        byte[] statuses = info.applicableStatuses;
        final byte offline = StatusInfo.STATUS_OFFLINE;
        menu.addItem(info.getName(offline), info.getIcon(offline), offline);
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(getGlobalStatus());
        menu.setActionListener(this);
        new Select(menu).show();
    }

    public void select(Select select, MenuModel menu, int cmd) {
        ContactList.getInstance().activate();
        setGlobalStatus(cmd);
    }

    private void setGlobalStatus(int status) {
        Protocol[] all = ContactList.getInstance().getProtocols();
        for (int i = 0; i < all.length; ++i) {
            all[i].setStatus(status, "");
        }
    }
}
