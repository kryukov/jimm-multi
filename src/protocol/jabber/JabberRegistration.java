package protocol.jabber;
// #sijapp cond.if protocols_JABBER is "true" #

import jimm.*;
import jimm.comm.*;
import jimm.ui.form.FormListener;
import jimm.ui.form.GraphForm;
import protocol.Profile;

/**
 *
 * @author Vladimir Kryukov
 */
public class JabberRegistration implements Runnable, FormListener {
    private XForm form;
    private JabberXml connection;
    private AccountsForm opts;
    private byte type;
    private String id;

    private String domain = "";
    private String xml = null;
    private String username;
    private String password;

    public final static byte TYPE_NEW_ACCOUNT_DOMAIN = 0;
    public final static byte TYPE_NEW_ACCOUNT_CREATE = 1;
    public final static byte TYPE_NONE = 2;
    private final static int FORM_SERVER = 0;


    public JabberRegistration(AccountsForm of) {
        opts = of;
    }
    public void show() {
        form = new XForm();
        type = TYPE_NEW_ACCOUNT_DOMAIN;
        id = "reg0";
        form = new XForm();
        form.init("registration", this);
        form.getForm().addTextField(FORM_SERVER, "domain", "", 50);
        form.getForm().show();
    }
    private String getServer(String domain) {
        protocol.net.SrvResolver r = new protocol.net.SrvResolver();
        String server = r.getXmpp(domain);
        r.close();
        return StringConvertor.isEmpty(server) ? (domain + ":5222") : server;
    }

    public void run() {
        String error = null;
        try {
            connection = new JabberXml();
            XmlNode xform = connection.newAccountConnect(domain, "socket://" + getServer(domain));
            id = "reg1";
            form.loadFromXml(xform.childAt(0), xform);
            while (null == xml) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            if (0 == xml.length()) {
                throw new JimmException(0, 0);
            }
            XmlNode n = connection.newAccountRegister(xml);
            if (("r" + "esult").equals(n.getAttribute("t" + "ype"))) {
                Profile account = new Profile();
                account.protocolType = Profile.PROTOCOL_JABBER;
                account.userId = username + "@" + domain;
                account.password = password;
                account.nick = "";
                account.isActive = true;
                opts.addAccount(Options.getMaxAccountCount(), account);

            } else {
                error = connection.getError(n.getFirstNode("e" + "rror"));
            }

        } catch (JimmException ex) {
        } catch (Exception ex) {
        }
        try {
            connection.disconnect();
        } catch (Exception ex) {
        }

        if (null == error) {
            form.back();
            opts = null;

        } else {
            type = TYPE_NONE;
            form.setErrorMessage(error);
        }
    }

    private void register(String form) {
        xml = form;
    }
    private void cancel() {
        xml = "";
    }

    private void requestForm(String domain) {
        this.domain = domain;
        new Thread(this).start();
    }

    public void formAction(GraphForm uiForm, boolean apply) {
        if (apply) {
            if ((0 < form.getSize()) || (TYPE_NEW_ACCOUNT_DOMAIN == type)) {
                doAction();
            }

        } else {
            cancel();
            form.back();
        }
    }
    private String getRegisterXml() {
        return "<iq type='set' to='" + Util.xmlEscape(domain) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='jabber:iq:register'>"
                + form.getXmlForm()
                + "</query></iq>";
    }
    private void doAction() {
        switch (type) {
            case TYPE_NEW_ACCOUNT_DOMAIN:
                String jid = form.getForm().getTextFieldValue(FORM_SERVER);
                if (!StringConvertor.isEmpty(jid)) {
                    form.setWainting();
                    requestForm(jid);
                    type = TYPE_NEW_ACCOUNT_CREATE;
                }
                break;

            case TYPE_NEW_ACCOUNT_CREATE:
                username = StringConvertor.notNull(form.getField(XForm.S_USERNAME));
                password = StringConvertor.notNull(form.getField(XForm.S_PASSWORD));
                register(getRegisterXml());
                break;
        }
    }
}
// #sijapp cond.end#