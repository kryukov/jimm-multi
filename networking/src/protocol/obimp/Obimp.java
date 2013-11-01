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

import jimm.Jimm;
import jimm.chat.message.PlainMessage;
import jimm.comm.StringUtils;
import jimm.comm.Util;
import jimm.search.*;
import protocol.*;
import protocol.ui.StatusView;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public class Obimp extends Protocol {
    private ObimpConnection connection;
    private String server = "";

    public Obimp() {
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || StringUtils.isEmpty(server);
    }

    @Override
    public String getUserIdName() {
        return "ObimpID";
    }

    @Override
    protected String processUin(String uin) {
        String[] userId = Util.explode(uin, '@');
        if (2 == userId.length) {
            server = StringUtils.toLowerCase(userId[1]);
        }
        return StringUtils.toLowerCase(userId[0]);
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
        c.start();
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

    protected void requestAuth(String userId) {
        connection.sendAuthRequest(userId);
    }

    @Override
    public void grandAuth(String userId) {
        connection.sendAuthReply(userId, true);
    }

    @Override
    public void denyAuth(String userId) {
        connection.sendAuthReply(userId, false);
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    @Override
    protected void s_updateXStatus() {
        connection.sendStatus();
    }
    // #sijapp cond.end #

    @Override
    public void showUserInfo(Contact contact) {
        UserInfo data;
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

    @Override
    public void showStatus(Contact contact) {
        StatusView statusView = Jimm.getJimm().getStatusView();
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