package jimm.cl;

import jimm.Jimm;
import jimmui.view.icons.Icon;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimmui.view.menu.SelectListener;
import protocol.Protocol;
import protocol.ui.InfoFactory;
import protocol.ui.StatusInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 10.03.13 21:57
 *
 * @author vladimir
 */
public class GlobalStatusForm implements SelectListener {

    public static Icon getGlobalStatusIcon() {
        return InfoFactory.factory.global.getIcon(Jimm.getJimm().jimmModel.getGlobalStatus());
    }

    public void show() {
        MenuModel menu = new MenuModel();
        StatusInfo info = InfoFactory.factory.global;

        byte[] statuses = info.applicableStatuses;
        final byte offline = StatusInfo.STATUS_OFFLINE;
        menu.addItem(info.getName(offline), info.getIcon(offline), offline);
        for (int i = 0; i < statuses.length; ++i) {
            menu.addItem(info.getName(statuses[i]), info.getIcon(statuses[i]), statuses[i]);
        }
        menu.setDefaultItemCode(Jimm.getJimm().jimmModel.getGlobalStatus());
        menu.setActionListener(this);
        new Select(menu).show();
    }

    public void select(Select select, MenuModel menu, int cmd) {
        ContactList.getInstance().activate();
        setGlobalStatus(cmd);
    }

    private void setGlobalStatus(int status) {
        Protocol[] all = Jimm.getJimm().jimmModel.getProtocols();
        for (int i = 0; i < all.length; ++i) {
            all[i].setStatus(status, "");
        }
    }
}
