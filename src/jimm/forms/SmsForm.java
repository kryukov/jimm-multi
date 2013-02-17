/*
 * SmsForm.java
 *
 * Created on 12 Август 2008 г., 14:41
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import java.util.Vector;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
// #sijapp cond.if target is "MIDP2" #
// #sijapp cond.if modules_FILES="true"#
// #sijapp cond.if modules_ANDROID isnot "true" #
import javax.wireless.messaging.*;
// #sijapp cond.end#
// #sijapp cond.end #
// #sijapp cond.end #
import jimm.cl.ContactList;
import jimm.ui.form.*;
import jimm.util.JLocale;
import protocol.mrim.*;
import protocol.Protocol;

/**
 *
 * @author Vladimir Kryukov
 */
public class SmsForm implements FormListener {

    /** Creates a new instance of SmsForm */
    public SmsForm(Protocol protocol, String phones) {
        this.phones = phones;

        Protocol[] protos;
        if (null == protocol) {
            protos = ContactList.getInstance().getProtocols();

        } else {
            protos = new Protocol[] {protocol};
        }

        protocols = new Vector();
        agents = "";
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_FILES="true"#
        // #sijapp cond.if modules_ANDROID isnot "true" #
        agents += "phone";
        protocols.addElement(null);
        // #sijapp cond.end#
        // #sijapp cond.end#
        // #sijapp cond.end#

        // #sijapp cond.if protocols_MRIM is "true" #
        for (int i = 0; i < protos.length; ++i) {
            if ((protos[i] instanceof Mrim) && protos[i].isConnected()) {
                agents += "|" + protos[i].getUserId();
                protocols.addElement(protos[i]);
            }
        }
        if (agents.startsWith("|")) {
            agents = agents.substring(1);
        }
        // #sijapp cond.end #
    }
    private String phones;
    private GraphForm form;
    private String agents;
    private Vector protocols;


    private static final int PHONE = 0;
    private static final int TEXT = 1;
    private static final int AGENT = 2;

    private static final int MAX_SMS_LENGTH = 156;

    public void show() {
        if (0 == agents.length()) {
            return;
        }
        form = new GraphForm("send_sms", "send", "cancel", this);
        if (null == phones) {
            form.addTextField(PHONE, "phone", "", 20, TextField.PHONENUMBER);

        } else {
            form.addSelector(PHONE, "phone", phones.replace(',', '|'), 0);
        }

        if (0 < agents.indexOf('|')) {
            form.addSelector(AGENT, "send_via", agents, 0);
        } else {
            form.addString("send_via", JLocale.getString(agents));
        }
        form.addTextField(TEXT, "message", "", MAX_SMS_LENGTH);
        form.show();
    }

    private void sendSms(Protocol p, String phone, String text) {
        // #sijapp cond.if protocols_MRIM is "true" #
        if (p instanceof Mrim) {
            ((Mrim)p).sendSms(phone, text);
            return;
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_FILES="true"#
        // #sijapp cond.if modules_ANDROID isnot "true" #
        try {
            if (phone.length() < 6) {
                return;
            }
            final MessageConnection conn = (MessageConnection)Connector.open("sms://" + phone + ":5151");
            final TextMessage msg = (TextMessage)conn.newMessage(MessageConnection.TEXT_MESSAGE);
            msg.setPayloadText(text);
            conn.send(msg);
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        // #sijapp cond.end #
    }
    private String getPhone() {
        // #sijapp cond.if protocols_MRIM is "true" #
        if (null != phones) {
            return form.getSelectorString(PHONE);
        }
        // #sijapp cond.end #
        return form.getTextFieldValue(PHONE);
    }
    public void formAction(GraphForm form, boolean apply) {
        if (apply) {
            final String text = form.getTextFieldValue(TEXT);
            final String phone = getPhone();
            if ((0 < text.length()) && (0 < phone.length())) {
                int agent = (0 < agents.indexOf('|')) ? form.getSelectorValue(AGENT) : 0;
                sendSms((Protocol) protocols.elementAt(agent), phone, text);
            }
        }
        form.back();
    }
}
