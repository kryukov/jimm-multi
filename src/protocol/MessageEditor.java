/*
 * MessageEditor.java
 *
 * Created on 8 Июнь 2010 г., 20:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import jimm.Options;
import jimm.cl.ContactList;
import jimm.ui.*;

/**
 *
 * @author Vladimir Krukov
 */
public class MessageEditor implements TextBoxListener {
    private InputTextBox messageTextbox;
    private Protocol protocol = null;
    private Contact toContact = null;

    /** Creates a new instance of MessageEditor */
    public MessageEditor() {
        createTextBox();
    }
    private void createTextBox() {
        int size = 5000;
        // #sijapp cond.if modules_ANDROID is "true" #
        size = 10000;
        // #sijapp cond.end #
        messageTextbox = new InputTextBox().create("message", size, "send");
        messageTextbox.setCancelNotify(true);
    }
    public void writeMessage(Protocol p, Contact to, String message) {
        boolean recreate = Options.getBoolean(Options.OPTION_RECREATE_TEXTBOX);
        String prevText = null;
        if (recreate) {
            prevText = messageTextbox.getRawString();
            createTextBox();

        } else {
            messageTextbox.updateCommands();
        }

        /* If user want reply with quotation */
        if (null != message) {
            messageTextbox.setString(message);

        /* Keep old text if press "cancel" while last edit */
        } else if (toContact != to) {
            messageTextbox.setString(null);

        } else if (recreate) {
            messageTextbox.setString(prevText);
        }

        if (toContact != to) {
            protocol = p;
            toContact = to;
            /* Display textbox for entering messages */
            messageTextbox.setCaption(" " + to.getName());
        }
        protocol.sendTypingNotify(to, true);

        messageTextbox.setTextBoxListener(this);
        messageTextbox.show();
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        protocol.sendTypingNotify(toContact, false);
        if (ok) {
            String text = messageTextbox.getString();
            if (!toContact.isSingleUserContact() && text.endsWith(", ")) {
                text = "";
            }
            protocol.sendMessage(toContact, text, true);
            if (toContact.hasChat()) {
                protocol.getChat(toContact).activate();
            } else {
                ContactList.getInstance().activate();
            }
            messageTextbox.setString(null);
            return;
        }
    }
    public void insert(String text) {

    }
    public InputTextBox getTextBox() {
        return messageTextbox;
    }
    public boolean isActive(Contact c) {
        return c == toContact;
    }
}
