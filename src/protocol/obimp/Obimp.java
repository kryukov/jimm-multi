/*
 * Obimp.java
 *
 * Created on 5 Декабрь 2010 г., 13:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_OBIMP is "true" #
package protocol.obimp;

import DrawControls.icons.*;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.search.*;
import jimm.ui.menu.MenuModel;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class Obimp extends Protocol {
    private static final ImageList statusIcons = ImageList.createImageList("/obimp-status.png");
    private static final int[] statusIcon = {2, 0, 6, 5, 10, 11, -1, -1, 12, 7, 8, 9, 4, 3, 1};

    private ObimpConnection connection;
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public static ObimpXStatus xStatus = new ObimpXStatus();
    // #sijapp cond.end #
    private String server = "";

    public Obimp() {
    }
    protected void initStatusInfo() {
        info = new StatusInfo(statusIcons, statusIcon);

        // #sijapp cond.if modules_XSTATUSES is "true" #
        xstatusInfo = Obimp.xStatus.getInfo();
        // #sijapp cond.end #
    }
    public boolean isEmpty() {
        return super.isEmpty() || StringConvertor.isEmpty(server);
    }
    public String getUserIdName() {
        return "ObimpID";
    }
    protected String processUin(String uin) {
        String[] userId = Util.explode(uin, '@');
        if (2 == userId.length) {
            server = StringConvertor.toLowerCase(userId[1]);
        }
        return StringConvertor.toLowerCase(userId[0]);
    }
    String getServer() {
        return server;
    }

    protected void s_setPrivateStatus() {
    }

    protected void sendSomeMessage(PlainMessage msg) {
        getConnection().sendMessage(msg);
    }

    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((ObimpContact)contact, contact.getGroupId());
    }
    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateGroup(group);
    }


    protected void s_removeContact(Contact c) {
        connection.removeItem(((ObimpContact)c).getId());
    }
    protected void s_removeGroup(Group group) {
        connection.removeItem(group.getId());
    }

    protected void s_moveContact(Contact contact, Group to) {
        connection.updateContact((ObimpContact) contact, to.getId());
    }
    protected void s_addContact(Contact contact) {
        connection.addContact((ObimpContact) contact);
    }

    protected void s_addGroup(Group group) {
        connection.addGroup(group);
    }

    public boolean isConnected() {
        return null != connection;
    }
    ObimpConnection getConnection() {
        return connection;
    }

    protected void startConnection() {
        ObimpConnection c = new ObimpConnection(this);
        connection = c;
        new Thread(c).start();
    }

    protected void closeConnection() {
        ObimpConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    public Group createGroup(String name) {
        return new ObimpGroup(name);
    }

    protected Contact createContact(String uin, String name) {
        return new ObimpContact(uin, name);
    }

    protected void s_searchUsers(Search cont) {
        if (null != cont.getSearchParam(Search.UIN)) {
            UserInfo userInfo = new UserInfo(this);
            userInfo.uin = cont.getSearchParam(Search.UIN);
            cont.addResult(userInfo);
            cont.finished();
            return;
        }
        connection.searchUsers(cont);
    }

    protected void s_updateOnlineStatus() {
        connection.sendStatus();
    }

    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }

    private static final byte[] statuses = {
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
    public byte[] getStatusList() {
        return statuses;
    }

    protected void requestAuth(String userId) {
        connection.sendAuthRequest(userId);
    }

    protected void grandAuth(String userId) {
        connection.sendAuthReply(userId, true);
    }

    protected void denyAuth(String userId) {
        connection.sendAuthReply(userId, false);
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    protected void s_updateXStatus() {
        connection.sendStatus();
    }
    // #sijapp cond.end #

    public void showUserInfo(Contact contact) {
        UserInfo data = null;
        if (isConnected()) {
            data = getConnection().getUserInfo((ObimpContact) contact);
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this, contact.getUserId());
            data.uin = contact.getUserId();
            data.nick = contact.getName();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile();
    }
    public void showStatus(Contact contact) {
        StatusView statusView = ContactList.getInstance().getStatusView();
        MenuModel menu = new MenuModel();

        statusView.init(this, contact);
        statusView.initUI();
        statusView.addContactStatus();
        statusView.addStatusText(contact.getStatusText());
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
    	    statusView.addXStatus();
    	    statusView.addStatusText(contact.getXStatusText());
        }
        // #sijapp cond.end #
//        // #sijapp cond.if modules_CLIENTS is "true" #
//        statusView.addBr();
//        statusView.addClient(client.getIcon(), client.getName());
//        // #sijapp cond.end #
        //statusView.addTime(contact);

        statusView.showIt();
    }

}
// #sijapp cond.end #