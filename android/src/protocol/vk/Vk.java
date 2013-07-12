package protocol.vk;

import jimm.chat.message.PlainMessage;
import jimm.search.Search;
import jimm.search.UserInfo;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.01.13 22:02
 *
 * @author vladimir
 */
public class Vk extends Protocol {
    private VkConnection connection = null;
    @Override
    public String getUserIdName() {
        return "Id";
    }


    @Override
    protected void requestAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void grandAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void denyAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_setPrivateStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_renameContact(Contact contact, String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_moveContact(Contact contact, Group to) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_removeGroup(Group group) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_renameGroup(Group group, String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_addGroup(Group group) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isConnected() {
        VkConnection c = connection;
        return (null != c) && c.isConnected();
    }

    @Override
    protected void startConnection() {
        connection = new VkConnection(this);
        connection.login();
    }

    @Override
    protected void closeConnection() {
        VkConnection c = connection;
        connection = null;
        if (null != c) {
            c.logout();
        }
    }

    @Override
    public Group createGroup(String name) {
        return new Group(name);
    }

    @Override
    protected Contact createContact(String uin, String name) {
        VkContact c = new VkContact(Integer.parseInt(uin));
        c.setName(name);
        return c;
    }

    @Override
    protected void s_searchUsers(Search cont) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_updateOnlineStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_updateXStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void saveUserInfo(UserInfo info) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }
}
