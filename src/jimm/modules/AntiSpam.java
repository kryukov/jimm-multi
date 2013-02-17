/*
 * AntiSpam.java
 *
 * Created on 24 Апрель 2007 г., 13:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.modules;

import java.util.*;
import jimm.*;
import jimm.chat.message.*;
import jimm.comm.*;
import protocol.*;

// #sijapp cond.if modules_ANTISPAM is "true" #
/**
 *
 * @author vladimir
 */
public class AntiSpam {
    private static AntiSpam antiSpam = new AntiSpam();

    private Vector validUins = new Vector();
    private Vector uncheckedUins = new Vector();
    private AntiSpam() {
    }
    
    private void sendHelloMessage(Protocol protocol, Contact contact) {
        validUins.addElement(contact.getUserId());
        uncheckedUins.removeElement(contact.getUserId());
        if (protocol.isMeVisible(contact)) {
            protocol.sendMessage(contact, Options.getString(Options.OPTION_ANTISPAM_HELLO), false);
        }
    }

    private void sendQuestion(Protocol protocol, Contact contact) {
        if (uncheckedUins.contains(contact.getUserId())) {
            uncheckedUins.removeElement(contact.getUserId());
            return;
        }
        String message = Options.getString(Options.OPTION_ANTISPAM_MSG);
        if (protocol.isMeVisible(contact) && !StringConvertor.isEmpty(message)) {
            protocol.sendMessage(contact, "I don't like spam!\n" + message, false);
            uncheckedUins.addElement(contact.getUserId());
        }
    }

    private boolean isChecked(String uin) {
        if (validUins.contains(uin)) {
            validUins.removeElement(uin);
            return true;
        }
        return false;
    }
    private void denyAuth(Protocol protocol, Message message) {
        if (message instanceof SystemNotice) {
    	    SystemNotice notice = (SystemNotice)message;
    	    if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                protocol.autoDenyAuth(message.getSndrUin());
    	    }
        }
    }
    private boolean containsKeywords(String msg) {
        String opt = Options.getString(Options.OPTION_ANTISPAM_KEYWORDS);
        if (0 == opt.length()) return false;
        if (5000 < msg.length()) {
            return true;
        }
        String[] keywords = Util.explode(StringConvertor.toLowerCase(opt), ' ');
        msg = StringConvertor.toLowerCase(msg);
        for (int i = 0; i < keywords.length; ++i) {
            if (-1 != msg.indexOf(keywords[i])) {
                return true;
            }
        }
        return false;
    }
    public boolean isSpamMessage(Protocol protocol, Message message) {
        if (!Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE)) {
            return false;
        }
        String uin = message.getSndrUin();
        if (isChecked(uin)) {
            return false;
        }
        denyAuth(protocol, message);
        if (!(message instanceof PlainMessage)) {
            return true;
        }

        String msg = message.getText();
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        if (msg.length() < 256) {
    	    MagicEye.addAction(protocol, uin, "antispam", msg);
        }
        // #sijapp cond.end #
        if (message.isOffline()) {
            return true;
        }
        Contact contact = protocol.createTempContact(uin);

        String[] msgs = Util.explode(Options.getString(Options.OPTION_ANTISPAM_ANSWER), '\n');
        for (int i = 0; i < msgs.length; ++i) {
            if (StringConvertor.stringEquals(msg, msgs[i])) {
                sendHelloMessage(protocol, contact);
                return true;
            }
        }
        sendQuestion(protocol, contact);
        return true;
    }
    
    public static boolean isSpam(Protocol protocol, Message message) {
        if (antiSpam.containsKeywords(message.getText())) {
            antiSpam.denyAuth(protocol, message);
            return true;
        }
        return antiSpam.isSpamMessage(protocol, message);
    }
}
// #sijapp cond.end #