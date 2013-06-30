/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package protocol.jabber;

// #sijapp cond.if protocols_JABBER is "true" #
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimmui.view.base.Popup;
import jimmui.view.form.*;
import jimm.util.JLocale;

/**
 *
 * @author Vladimir Krukov
 */
public final class AdHoc implements FormListener, ControlStateListener {
    private JabberContact contact;
    private Jabber protocol;
    private String jid;
    private String[] nodes;
    private String[] names;
    private GraphForm commandsListForm;
    private XForm commandForm;
    private static final int FORM_RESOURCE = 1;
    private static final int FORM_COMMAND = 2;

    public AdHoc(Jabber protocol, JabberContact contact) {
        this.protocol = protocol;
        this.contact = contact;
        this.jid = contact.getUserId() + "/" + contact.currentResource;
        nodes = new String[0];
        names = new String[0];
    }
    public String getJid() {
        return jid;
    }

    public void controlStateChanged(GraphForm form, int id) {
        if (FORM_RESOURCE == id) {
            requestCommandsForCurrentResource();
            updateForm(false);
        }
    }

    void show() {
        commandsListForm = new GraphForm("adhoc", "ok", "cancel", this);
        updateForm(false);
        commandsListForm.setControlStateListener(this);
        commandsListForm.show();
        requestCommandsForCurrentResource();
    }
    private String[] getResources() {
        String[] resources = new String[contact.subcontacts.size()];
        for (int i = resources.length - 1; 0 <= i; --i) {
            JabberContact.SubContact sub = (JabberContact.SubContact) contact.subcontacts.elementAt(i);
            resources[i] = sub.resource;
        }
        return resources;
    }
    private void updateForm(boolean loaded) {
        String[] resources = getResources();
        int selectedResource = 0;
        if (commandsListForm.hasControl(FORM_RESOURCE)) {
            selectedResource = commandsListForm.getSelectorValue(FORM_RESOURCE);

        } else {
            for (int i = resources.length - 1; 0 <= i; --i) {
                if (resources[i].equals(contact.currentResource)) {
                    selectedResource = i;
                }
            }
        }

        commandsListForm.clearForm();
        if (1 < resources.length) {
            commandsListForm.addSelector(FORM_RESOURCE, "resource", resources, selectedResource);
        }
        if (0 < names.length) {
            commandsListForm.addSelector(FORM_COMMAND, "commands", names, 0);
        } else {
            String label = loaded ? "commands_not_found" : "receiving_commands";
            commandsListForm.addString(JLocale.getString(label));
        }
        commandsListForm.invalidate();
    }
    private void requestCommandsForCurrentResource() {
        nodes = new String[0];
        names = new String[0];
        if (null != Jid.getResource(contact.getUserId(), null)) {
            jid = contact.getUserId();

        } else if (1 < contact.subcontacts.size()) {
            String resource = commandsListForm.getSelectorString(FORM_RESOURCE);
            jid = contact.getUserId() + "/" + resource;

        } else if (1 == contact.subcontacts.size()) {
            JabberContact.SubContact sub = (JabberContact.SubContact) contact.subcontacts.elementAt(0);
            if (StringConvertor.isEmpty(sub.resource)) {
                jid = contact.getUserId();
            } else {
                jid = contact.getUserId() + "/" + sub.resource;
            }

        } else {
            jid = contact.getUserId();
        }
        protocol.getConnection().requestCommandList(this);
    }

    void addItems(XmlNode query) {
        int count = (null == query) ? 0 : query.childrenCount();
        nodes = new String[count];
        names = new String[count];
        for (int i = 0; i < count; ++i) {
            XmlNode item = query.childAt(i);
            nodes[i] = StringConvertor.notNull(item.getAttribute("n" + "ode"));
            names[i] = StringConvertor.notNull(item.getAttribute(XmlNode.S_NAME));
        }
        updateForm(true);
    }

    private int commandIndex;
    private String commandSessionId;
    private String commandId;
    public void formAction(GraphForm form, boolean apply) {
        if (!apply) {
            form.back();
            return;
        }
        if (commandsListForm == form) {
            if (0 == nodes.length) {
                return;
            }
            commandIndex = form.getSelectorValue(FORM_COMMAND);
            protocol.getConnection().requestCommand(this, nodes[commandIndex]);

        } else {
            execForm();
            ContactList.getInstance().activate(contact);
        }
    }
    private void execForm() {
        String xml = "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(commandId)
                + "'><command xmlns='http://jabber.org/protocol/commands'"
                + " node='" + nodes[commandIndex] + "'"
                + (null != commandSessionId ? " sessionid='" + commandSessionId + "'": "")
                + ">"
                + commandForm.getXmlForm()
                + "</command></iq>";
        protocol.getConnection().requestRawXml(xml);
    }
    private String getCurrentNode() {
        return ((0 <= commandIndex) && (commandIndex < nodes.length))
                ? nodes[commandIndex] : "";
    }
    void loadCommandXml(XmlNode iqXml, String id) {
        XmlNode commandXml = iqXml.getFirstNode("c" + "ommand");
        if (null == commandXml) {
            return;
        }
        String xmlns = commandXml.getXmlns();
        if (!"http://jabber.org/protocol/commands".equals(xmlns)) {
            return;
        }
        if (!getCurrentNode().equals(commandXml.getAttribute("n" + "ode"))) {
            return;
        }
        commandId = id;
        commandSessionId = commandXml.getAttribute("sessionid");
        XForm form = new XForm();
        form.init(names[commandIndex], this);
        form.loadXFromXml(commandXml, iqXml);
        commandForm = form;

        boolean showForm = (0 < commandForm.getForm().getSize());
        String status = commandXml.getAttribute("s" + "tatus");
        if (("c" + "anceled").equals(status) || ("co" + "mpleted").equals(status)) {
            String text = commandXml.getFirstNodeValue("n" + "ote");
            protocol.getConnection().resetAdhoc();
            commandForm = null;
            if (!StringConvertor.isEmpty(text)) {
                new Popup(text).show();
                showForm = false;
            }
        }

        if (showForm) {
            form.getForm().show();
        }
    }
}
// #sijapp cond.end #
