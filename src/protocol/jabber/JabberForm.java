/*
 * JabberForm.java
 *
 * Created on 12 Март 2009 г., 0:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.ui.form.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
final class JabberForm implements FormListener {
    private XForm form;
    private Jabber jabber;
    private String jid;
    private String id;

    public final static byte TYPE_REGISTER = 0;
    public final static byte TYPE_CAPTCHA = 1;
    public final static byte TYPE_OWNER = 2;
    public final static byte TYPE_NONE = 3;
    private byte type = TYPE_CAPTCHA;

    /** Creates a new instance of JabberForm */
    public JabberForm(byte formType, Jabber protocol, String resourceJid) {
        jabber = protocol;
        jid = resourceJid;
        type = formType;
        id = newId();
        form = new XForm();
    }

    private String typeToTitle(byte type) {
        switch (type) {
            case TYPE_CAPTCHA: return "captcha";
            case TYPE_REGISTER: return "registration";
            case TYPE_OWNER: return "options";
        }
        return null;
    }
    private String newId() {
        return "forms" + Util.uniqueValue();
    }
    private String getJid() {
        return jid;
    }
    public boolean isWaiting() {
        return form.isWaiting();
    }
    public void loadFromXml(XmlNode xml, XmlNode baseXml) {
        id = newId();
        form.loadFromXml(xml, baseXml);
    }
    public void show() {
        form.init(typeToTitle(type), this);
        form.setWainting();
        form.getForm().show();
    }
    public void showCaptcha(XmlNode baseNode) {
        form.init(typeToTitle(type), this);
        final String S_CAPTCHA = "c" + "aptcha";
        XmlNode captcha = baseNode.getFirstNode(S_CAPTCHA);
        id = baseNode.getAttribute("i" + "d");
        form.loadXFromXml(captcha, baseNode);
        form.getForm().showTop();
    }

    public String getId() {
        return id;
    }

    public void formAction(GraphForm uiForm, boolean apply) {
        if (apply) {
            if (0 < form.getSize()) {
                doAction();
            }

        } else {
            switch (type) {
                case TYPE_CAPTCHA:
                    Contact c = jabber.getItemByUIN(jid);
                    ContactList.getInstance().activate(c);
                    return;
            }
            form.back();
        }
    }

    void error(String description) {
        type = TYPE_NONE;
        form.setErrorMessage(description);
    }

    void success() {
        ContactList.getInstance().activate(jabber.getItemByUIN(jid));
    }

    private String getCaptchaXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><captcha xmlns='urn:xmpp:captcha'>"
                + form.getXmlForm()
                + "</captcha></iq>";
    }
    private String getRegisterXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='jabber:iq:register'>"
                + form.getXmlForm()
                + "</query></iq>";
    }
    private String getOwnerXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='http://jabber.org/protocol/muc#owner'>"
                + form.getXmlForm()
                + "</query></iq>";
    }

    private void doAction() {
        switch (type) {
            case TYPE_REGISTER:
                jabber.getConnection().register2(this, getRegisterXml(), getJid());
                break;

            case TYPE_CAPTCHA:
                jabber.getConnection().requestRawXml(getCaptchaXml());
                ContactList.getInstance().activate(jabber.getItemByUIN(jid));
                break;

            case TYPE_OWNER:
                jabber.getConnection().requestRawXml(getOwnerXml());
                ContactList.getInstance().activate(jabber.getItemByUIN(jid));
                break;

            case TYPE_NONE:
                form.back();
                break;
        }
    }
}
// #sijapp cond.end #
