package protocol.vk;

import jimmui.view.menu.MenuModel;
import protocol.Contact;
import protocol.Protocol;
import protocol.ui.StatusInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.01.13 22:02
 *
 * @author vladimir
 */
public class VkContact extends Contact {
    private int uid;
    VkContact(int uid) {
        this.uid = uid;
        this.userId = "" + uid;
    }
    int getUid() {
        return uid;
    }
    @Override
    public void initManageContactMenu(Protocol protocol, MenuModel menu) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setOnlineStatus() {
        setStatus(StatusInfo.STATUS_ONLINE, null);
    }
}
