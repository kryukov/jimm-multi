package jimm.cl;

import DrawControls.icons.ImageList;
import jimm.ui.menu.MenuModel;
import jimm.ui.menu.Select;
import jimm.ui.menu.SelectListener;
import protocol.Protocol;
import protocol.StatusInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 10.03.13 21:57
 *
 * @author vladimir
 */
public class GlobalStatusForm implements SelectListener {
    private StatusInfo getStatusInfo() {
        ImageList icons = ImageList.createImageList("/global-status.png");
        final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1};
        return new StatusInfo(icons, statusIconIndex, statuses);
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY};

    public void show() {
        MenuModel menu = new MenuModel();
        StatusInfo info = getStatusInfo();

        byte[] statuses = info.applicableStatuses;
        final byte offline = StatusInfo.STATUS_OFFLINE;
        menu.addItem(info.getName(offline), info.getIcon(offline), offline);
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }

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
