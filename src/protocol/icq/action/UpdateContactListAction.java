/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/comm/UpdateContactListAction.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.icq.action;

import java.util.Vector;
import jimm.*;
import jimm.comm.*;
import protocol.*;
import protocol.icq.*;
import protocol.icq.packet.*;
import jimm.modules.*;

public class UpdateContactListAction extends IcqAction {

    /* Action states */
    private static final int STATE_ERROR = -1;
    private static final int STATE_RENAME = 1;
    private static final int STATE_COMPLETED = 3;
    private static final int STATE_MOVE1 = 4;
    private static final int STATE_MOVE2 = 5;
    private static final int STATE_MOVE3 = 6;
    private static final int STATE_ADD = 18;
    private static final int STATE_DELETE_CONTACT = 7;
    private static final int STATE_DELETE_GROUP = 9;
    private static final int STATE_ADD_GROUP     = 11;
    private static final int STATE_COMMIT     = 13;

    /* Action types */
    public static final int ACTION_ADD    = 1;
    public static final int ACTION_DEL    = 2;
    public static final int ACTION_RENAME = 3;
    public static final int ACTION_MOVE   = 4;
    private static final int ACTION_ADD_REQ_AUTH  = 5;
    public static final int ACTION_MOVE_REQ_AUTH = 6;
    private static final int ACTION_MOVE_FROM_NIL = 7;

    /* Timeout */
    public static final int TIMEOUT = 10; // seconds

    /** ************************************************************************* */

    /* Contact item */
    private Contact contact;
    private int contactFromId;
    private int contactToId;

    /* Group item */
    private Group gItem;
    private Group newGItem;

    private int action;
    private int state;

    private int errorCode;

    /**
     * Removes or adds given contact
     */
    public UpdateContactListAction(Icq icq, Contact cItem, int _action) {
        this.action = _action;
        this.contact = cItem;
        this.contactFromId = ((IcqContact)cItem).getContactId();
        this.gItem = icq.getGroup(cItem);
    }
    /**
     * Removes or adds given group
     */
    public UpdateContactListAction(Group cItem, int _action) {
        this.action = _action;
        this.contact = null;
        this.gItem = cItem;
    }
    /**
     * Move contact
     */
    public UpdateContactListAction(Contact cItem, Group oldGroup, Group newGroup) {
        this.contact = cItem;
        this.contactFromId = ((IcqContact)cItem).getContactId();
        this.gItem = oldGroup;
        this.newGItem = newGroup;
        action = ACTION_MOVE;
        if ((null != gItem) && ("Not In List".equals(gItem.getName()))) {
            action = ACTION_MOVE_FROM_NIL;
        }
    }

    private void addItem() throws JimmException {
        byte[] buf = null;
        if (null == contact) {
            buf = packGroup(gItem);
            state = STATE_ADD_GROUP;

        } else {
            gItem = getIcq().getGroup(contact);
            contactToId = getIcq().createRandomId();
            buf = packContact(contact, contactToId, gItem.getId(), action == ACTION_ADD_REQ_AUTH);
            state = STATE_ADD;
        }
        sendSsiPacket(SnacPacket.CLI_ROSTERADD_COMMAND, buf);
    }
    private void sendSsiPacket(int cmd, byte[] buf) throws JimmException {
        sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY,
                cmd, getConnection().getNextCounter(), buf));
    }

    public void init() throws JimmException {
        byte[] buf = null;

        if (ACTION_RENAME != action) {
            /* Send a CLI_ADDSTART packet */
            transactionStart();
        }

        switch (action) {
            /* Send a CLI_ROSTERUPDATE packet */
            case ACTION_RENAME:
                buf = (null != contact) ? packContact(contact, contactFromId, contact.getGroupId(), false) : packGroup(gItem);
                sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, buf);
                state = STATE_RENAME;
                break;

                /* Send CLI_ROSTERADD packet */
            case ACTION_ADD:
            case ACTION_ADD_REQ_AUTH:
                addItem();
                break;

                /* Send a CLI_ROSTERDELETE packet */
            case ACTION_MOVE_FROM_NIL:
            case ACTION_DEL:
                if (null != contact) {
                    buf = packContact(contact, contactFromId, gItem.getId(), false);
                    this.state = STATE_DELETE_CONTACT;
                } else {
                    buf = packGroup(gItem);
                    this.state = STATE_DELETE_GROUP;
                }

                sendSsiPacket(SnacPacket.CLI_ROSTERDELETE_COMMAND, buf);
                break;

                /* Move contact between groups (like Miranda does) */
            case ACTION_MOVE:
                sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND,
                        packContact(contact, contactFromId, gItem.getId(), false));

                this.state = STATE_MOVE1;
                break;
        }
        active();
    }

    private boolean processPaket(Packet packet) throws JimmException {
        /* Watch out for SRV_UPDATEACK packet type */
        SnacPacket snacPacket = null;
        if (packet instanceof SnacPacket) {
            snacPacket = (SnacPacket) packet;
        } else {
            return false;
        }

        if (SnacPacket.SSI_FAMILY != snacPacket.getFamily()) {
            return false;
        }
        if (SnacPacket.CLI_ROSTERDELETE_COMMAND == snacPacket.getCommand()) {
            ArrayReader buf = snacPacket.getReader();
            int length = buf.getWordBE();
            String uin = StringConvertor.byteArrayToAsciiString(
                    buf.getArray(length), 0, length);
            return (null != contact) && uin.equals(contact.getUserId());
        }
        if (SnacPacket.SRV_UPDATEACK_COMMAND != snacPacket.getCommand()) {
            return false;
        }

        // Check error code, see ICQv8 specification
        int retCode = snacPacket.getReader().getWordBE();
        switch (retCode) {
            case 0x002: errorCode = 154; break;
            case 0x003: errorCode = 155; break;
            case 0x00A: errorCode = 156; break;
            case 0x00C: errorCode = 157; break;
            case 0x00D: errorCode = 158; break;
        }
        if ((0x00A == retCode) && (ACTION_MOVE_FROM_NIL == action)) {
            errorCode = 0;
            state = STATE_COMPLETED;
            contact.setGroup(newGItem);
            contact.setTempFlag(false);
            getIcq().addContact(contact);
            return true;
        }
        if ((0x00A == retCode) && (ACTION_DEL == action)) {
            errorCode = 0;
            state = STATE_COMPLETED;
            return true;
        }
        if (0 != errorCode) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.println("update action = " + action
                    + " state = " + state
                    + " ret code = " + retCode);
            // #sijapp cond.end #
            // non critical
            getIcq().showException(new JimmException(errorCode, 0));
            state = STATE_ERROR;
            return true;
        }

        switch (state) {
            case STATE_ADD_GROUP:
                if (0 < getIcq().getGroupItems().size()) {
                    sendGroupsList();
                    this.state = STATE_COMMIT;
                } else {
                    transactionCommit();
                    this.state = STATE_COMPLETED;
                }
                break;

            case STATE_ADD:
                if (0 == retCode) {
                    sendGroup(gItem);
                    ((IcqContact)contact).setContactId(contactToId);
                    contact.setBooleanValue(IcqContact.CONTACT_NO_AUTH, action == ACTION_ADD_REQ_AUTH);
                    this.state = STATE_COMPLETED;
                }
                transactionCommit();
                if ((0 != retCode) && (action == ACTION_ADD)) {
                    action = ACTION_ADD_REQ_AUTH;
                    transactionStart();
                    addItem();
                }
                break;

            case STATE_RENAME:
                this.state = STATE_COMPLETED;
                break;

                /* STATE_MOVE */
            case STATE_MOVE1:
                sendSsiPacket(SnacPacket.CLI_ROSTERDELETE_COMMAND,
                        packContact(contact, contactFromId, gItem.getId(), false));

                this.state = STATE_MOVE2;
                break;

            case STATE_MOVE2:
                contactToId = getIcq().createRandomId();
                sendSsiPacket(SnacPacket.CLI_ROSTERADD_COMMAND,
                        packContact(contact, contactToId, newGItem.getId(), action == ACTION_MOVE_REQ_AUTH));
                this.state = STATE_MOVE3;
                break;

            case STATE_MOVE3:
                ((IcqContact)contact).setContactId(contactToId);
                sendGroup(newGItem);
                this.state = STATE_COMMIT;
                break;

            case STATE_DELETE_CONTACT:
                sendGroup(gItem);
                this.state = STATE_COMMIT;
                break;

            case STATE_DELETE_GROUP:
                sendGroupsList();
                this.state = STATE_COMMIT;
                break;


            case STATE_COMMIT:
                transactionCommit();
                this.state = STATE_COMPLETED;
                break;
        }

        active();
        return true;
    }

    private void transactionStart() throws JimmException {
        sendSsiPacket(SnacPacket.CLI_ADDSTART_COMMAND, new byte[0]);
    }

    private void transactionCommit() throws JimmException {
        sendSsiPacket(SnacPacket.CLI_ADDEND_COMMAND, new byte[0]);
    }

    private void sendGroupsList() throws JimmException {
        sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, packGroups());
    }

    private void sendGroup(Group group) throws JimmException {
        sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, packGroup(group));
    }

    /* Forwards received packet, returns true if packet was consumed */
    public boolean forward(Packet packet) throws JimmException {
        boolean result = processPaket(packet);

        if (result && (0 != errorCode)) {
            if ((ACTION_MOVE != action) && (ACTION_RENAME != action)) {
                transactionCommit();
            }
            active();
        }

        return result;
    }


    public boolean isCompleted() {
        return (this.state == UpdateContactListAction.STATE_COMPLETED);
    }

    public boolean isError() {
        if (this.state == ConnectAction.STATE_ERROR) return true;
        if (isNotActive(TIMEOUT) || (errorCode != 0)) {
            this.state = ConnectAction.STATE_ERROR;
        }
        return (this.state == ConnectAction.STATE_ERROR);
    }

    private byte[] packContact(Contact cItem, int contactId, int groupId, boolean auth) {
        Util stream = new Util();

        stream.writeLenAndUtf8String(cItem.getUserId());
        stream.writeWordBE(groupId);
        stream.writeWordBE(contactId);//getContactId(cItem));
        stream.writeWordBE(0); // Type (Buddy record)

        /* Additional data */
        Util addData = new Util();

        /* TLV(0x0131) - name */
        if ((ACTION_DEL != action) && (ACTION_MOVE_FROM_NIL != action)) {
            addData.writeWordBE(0x0131);
            addData.writeLenAndUtf8String(cItem.getName());
        }

        // /* Server-side additional data */
        //if (contact.ssData != null) {
        //    Util.writeByteArray(addData, contact.ssData);
        //}

        /* TLV(0x0066) - you are awaiting authorization for this buddy */
        if (auth) {
            addData.writeTLV(0x0066, null);
        }

        /* Append additional data to stream */
        stream.writeWordBE(addData.size());
        stream.writeByteArray(addData.toByteArray());

        // Util.showBytes(stream.toByteArray());

        return stream.toByteArray();
    }

    private byte[] packGroup(Group gItem) {
        Util stream = new Util();

        stream.writeLenAndUtf8String(gItem.getName());
        stream.writeWordBE(gItem.getId()); // Group Id
        stream.writeWordBE(0); // Id
        stream.writeWordBE(1); // Type (Group)

        /* Contact items */
        Vector items = gItem.getContacts();
        int size = items.size();
        if (size != 0) {
            /* Length of the additional data */
            stream.writeWordBE(size * 2 + 4);

            /* TLV(0x00C8) */
            stream.writeWordBE(0x00c8);
            stream.writeWordBE(size * 2);
            for (int i = 0; i < size; ++i) {
                IcqContact item = (IcqContact)items.elementAt(i);
                stream.writeWordBE(item.getContactId());
            }
        } else {
            stream.writeWordBE(0);
        }

        return stream.toByteArray();
    }

    private byte[] packGroups() {
        Util stream = new Util();

        Vector gItems = getIcq().getGroupItems();
        int size = gItems.size();
        stream.writeLenAndUtf8String("");// Master Group Name
        stream.writeWordBE(0); // Group Id
        stream.writeWordBE(0); // Id
        stream.writeWordBE(1);
        stream.writeWordBE(size * 2 + 4);
        stream.writeWordBE(0xc8);
        stream.writeWordBE(size * 2);

        for (int i = 0; i < size; ++i) {
            stream.writeWordBE(((Group)gItems.elementAt(i)).getId());
        }

        return stream.toByteArray();
    }
}
// #sijapp cond.end #
