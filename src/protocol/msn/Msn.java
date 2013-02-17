/*
 * Msn.java
 *
 * Created on 2 Март 2010 г., 19:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import DrawControls.icons.ImageList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import jimm.chat.message.PlainMessage;
import jimm.cl.ContactList;
import jimm.comm.Util;
import jimm.search.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public class Msn extends Protocol {
    private MsnConnection connection;
    private static final ImageList statusIcons = ImageList.createImageList("/msn-status.png");
    private static final int[] statusIcon = {1, 0, 4, -1, -1, -1, -1, -1, -1, 5, -1, -1, 3, -1, 1};
    

    /** Creates a new instance of Msn */
    public Msn() {
    }
    protected void initStatusInfo() {
        info = new StatusInfo(statusIcons, statusIcon);
    }

    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    protected void s_setPrivateStatus() {
    }
    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        String uin = dis.readUTF();
        String name = dis.readUTF();
        int groupId = dis.readInt();
        byte booleanValues = dis.readByte();
        String guid = dis.readUTF();

        Contact contact = createContact(uin, name);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        ((MsnContact)contact).setUserHash(guid);
        return contact;
    }

    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeByte(0);
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
        out.writeUTF(((MsnContact)contact).getUserHash());
    }
    protected Group loadGroup(DataInputStream dis) throws Exception {
        Group group = super.loadGroup(dis);
        ((MsnGroup)group).setGuid(dis.readUTF());
        return group;
    }
    protected void saveGroup(DataOutputStream out, Group group) throws Exception {
        super.saveGroup(out, group);
        out.writeUTF(((MsnGroup)group).getGuid());
    }

    protected void s_renameContact(Contact contact, String name) {
    }

    protected void s_removeGroup(Group group) {
    }

    protected void s_renameGroup(Group group, String name) {
    }

    protected void s_moveContact(Contact contact, Group to) {
    }

    protected void s_addGroup(Group group) {
    }

    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    protected void startConnection() {
        closeConnection();
        connection = new MsnConnection(this);
        connection.login();
    }

    protected void closeConnection() {
        MsnConnection c = connection;
        connection = null;
        if (null != c) {
            c.logout();
        }
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = groups.size() - 1; i >= 0; i--) {
                Group group = (Group)groups.elementAt(i);
                if (group.getId() == id) {
                    id = -1;
                    break;
                }
            }
            if (0 <= id) {
                return id;
            }
        }
    }
    public Group createGroup(String name) {
        return new MsnGroup(name, getNextGroupId());
    }

    protected Contact createContact(String uin, String name) {
        return new MsnContact(uin, name);
    }

    protected void s_searchUsers(Search cont) {
    }

    protected void s_updateOnlineStatus() {
    }

    protected void s_updateXStatus() {
    }

    public void saveUserInfo(UserInfo info) {
    }

    void leaveConversation(MsnContact msnContact) {
        connection.leaveConversation(msnContact);
    }

    public byte[] getStatusList() {
        return new byte[0];
    }

    protected void requestAuth(String userId) {
    }

    protected void grandAuth(String userId) {
    }

    protected void denyAuth(String userId) {
    }
    
    protected void doAction(Contact contact, int cmd) {
        if (MsnContact.CONVERSATION_DISCONNECT == cmd) {
            leaveConversation((MsnContact) contact);
            ContactList.getInstance().activate();
            return;
        }
    }

    public String getUserIdName() {
        return "e-mail";
    }
}
// #sijapp cond.end #