/*
 * ServiceDiscovery.java
 *
 * Created on 9 Февраль 2009 г., 15:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import jimm.ui.text.TextList;
import DrawControls.text.*;
import java.util.Vector;
import jimm.cl.ContactList;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.JLocale;
import jimm.comm.*;
import jimm.ui.base.CanvasEx;
import jimm.ui.text.TextListController;
import jimm.ui.text.TextListModel;
import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public final class ServiceDiscovery extends TextListController implements TextBoxListener {

    private boolean isConferenceList = false;
    private int totalCount = 0;

    private Jabber jabber;
    private String serverJid;
    private InputTextBox serverBox;
    private InputTextBox searchBox;
    private boolean shortView;
    private Vector jids = new Vector();

    private TextList screen = new TextList(JLocale.getString("service_discovery"));
    private TextListModel model = new TextListModel();


    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_SET = 1;
    private static final int COMMAND_REGISTER = 2;
    private static final int COMMAND_SEARCH = 3;
    private static final int COMMAND_SET_SERVER = 4;
    private static final int COMMAND_HOME = 5;

    public ServiceDiscovery() {
        serverBox = new InputTextBox().create("service_discovery_server", 64);
        searchBox = new InputTextBox().create("service_discovery_search", 64);
        screen.setModel(model);
    }
    public void setProtocol(Jabber protocol) {
        jabber = protocol;
    }



    protected void doJimmAction(int action) {
        doJimmBaseAction(action);
        String jid = getCurrentJid();
        if (!StringConvertor.isEmpty(jid)) {
            switch (action) {
                case COMMAND_ADD:
                    Contact c = jabber.createTempContact(jid);
                    jabber.addContact(c);
                    ContactList.getInstance().activate(c);
                    break;

                case COMMAND_SET:
                    setServer(jid);
                    screen.restore();
                    break;

                case COMMAND_REGISTER:
                    jabber.getConnection().register(jid);
                    break;
            }
        }
        switch (action) {
            case COMMAND_SEARCH:
                searchBox.setTextBoxListener(this);
                searchBox.show();
                break;

            case COMMAND_SET_SERVER:
                serverBox.setString(serverJid);
                serverBox.setTextBoxListener(this);
                serverBox.show();
                break;

            case COMMAND_HOME:
                setServer("");
                screen.restore();
                break;
        }
    }
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        String jid = getCurrentJid();
        if (Jid.isConference(jid)) {
            menu.addItem("service_discovery_add", COMMAND_ADD);
            setDefaultCode(COMMAND_ADD);

        } else if (Jid.isKnownGate(jid)) {
            menu.addItem("register", COMMAND_REGISTER);
            setDefaultCode(COMMAND_REGISTER);

        } else {
            menu.addItem("select", COMMAND_SET);
            if (Jid.isGate(jid)) {
                menu.addItem("register", COMMAND_REGISTER);
            }
            setDefaultCode(COMMAND_SET);
        }
        menu.addItem("service_discovery_search", COMMAND_SEARCH);
        menu.addItem("service_discovery_server", COMMAND_SET_SERVER);
        menu.addItem("service_discovery_home", COMMAND_HOME);
        menu.setActionListener(this);
        return menu;
    }
    private String getJid(int num) {
        if (num < jids.size()) {
            String rawJid = (String)jids.elementAt(num);
            if (rawJid.endsWith("@")) {
                return rawJid + serverJid;
            }
            return rawJid;
        }
        return "";
    }
    private int getJidIndex(int textIndex) {
        if (!model.isSelectable(textIndex)) return -1;
        int index = -1;
        for (int i = 0; i <= textIndex; ++i) {
            if (model.isSelectable(i)) index++;
        }
        return index;
    }
    private String getCurrentJid() {
        int currentIndex = getJidIndex(screen.getCurrItem());
        return (-1 == currentIndex) ? "" : getJid(currentIndex);
    }

    private void addServer(boolean active) {
        if (0 < serverJid.length()) {
            Parser item = model.createNewParser(active);
            if (active) {
                item.useMinHeight();
            }
            item.addText(serverJid, CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_BOLD);
            model.addPar(item);
            if (active) {
                jids.addElement(serverJid);
            }
        }
    }
    private void clear() {
        model.clear();
        jids.removeAllElements();
        addServer(false);
        screen.setAllToTop();
    }
    public void setTotalCount(int count) {
        model.clear();
        jids.removeAllElements();
        addServer(true);
        totalCount = count;
        shortView |= (totalCount > 400);
        screen.updateModel();
    }

    private String makeShortJid(String jid) {
        if (isConferenceList) {
            return jid.substring(0, jid.indexOf('@') + 1);
        }
        return jid;
    }
    private String makeReadableJid(String jid) {
        if (isConferenceList) {
            return jid;
        }
        if (Jid.isConference(serverJid)) {
            return Jid.getResource(jid, jid);
        }
        jid = Util.replace(jid, "@conference.jabber.ru", "@c.j.ru");
        return Util.replace(jid, "@conference.", "@c.");
    }

    public void addItem(String name, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        String shortJid = makeShortJid(jid);
        String visibleJid = makeReadableJid(shortJid);
        Parser item = model.createNewParser(true);
        item.useMinHeight();
        item.addText(visibleJid, CanvasEx.THEME_TEXT,
                shortView ?  CanvasEx.FONT_STYLE_PLAIN :  CanvasEx.FONT_STYLE_BOLD);
        if (!shortView) {
            if (StringConvertor.isEmpty(name)) {
                name = shortJid;
            }
            item.doCRLF();
            item.addText(name, CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_PLAIN);
        }

        model.addPar(item);
        jids.addElement(shortJid);
        if (0 == (jids.size() % 50)) {
            screen.updateModel();
        }
    }

    public void showIt() {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("");
        }
        screen.setController(this);
        screen.show();
    }
    public void update() {
        screen.updateModel();
    }

    private void addUnique(String text, String jid) {
        if (-1 == jids.indexOf(jid)) {
            addItem(text, jid);
        }
    }

    private void addBookmarks() {
        Vector all = jabber.getContactItems();
        boolean notEmpty = false;
        for (int i = 0; i < all.size(); ++i) {
            JabberContact contact = (JabberContact)all.elementAt(i);
            if (contact.isConference()) {
                addUnique(contact.getName(), contact.getUserId());
                notEmpty = true;
            }
        }
        if (notEmpty) {
            Parser br = model.createNewParser(false);
            br.addText("\n", CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_PLAIN);
            model.addPar(br);
        }
    }
    private void addBuildInList() {
        addUnique("Jimm aspro", "jimm-aspro@conference.jabber.ru");
        Parser br = model.createNewParser(false);
        br.addText("\n", CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_PLAIN);
        model.addPar(br);

        String domain = Jid.getDomain(jabber.getUserId());
        addUnique(JLocale.getString("my_server"), domain);
        addUnique(JLocale.getString("conferences_on_") + domain, "conference." + domain);
    }

    public void setServer(String jid) {
        jid = Jid.getNormalJid(jid);
        totalCount = 0;
        shortView = false;
        serverJid = jid;
        isConferenceList = (-1 == jid.indexOf('@')) && Jid.isConference('@' + jid);
        clear();
        if (0 == jid.length()) {
            Config conf = new Config().loadLocale("/jabber-services.txt");
            boolean conferences = true;
            addBookmarks();
            for (int i = 0; i < conf.getKeys().length; ++i) {
                if (conferences && !Jid.isConference(conf.getKeys()[i])) {
                    conferences = false;
                    addBuildInList();
                }
                addUnique(conf.getValues()[i], conf.getKeys()[i]);
            }
            if (conferences) {
                addBuildInList();
            }
            screen.updateModel();
            return;
        }
        if (Jid.isConference(serverJid)) {
            shortView = true;
        }
        Parser wait = model.createNewParser(false);
        wait.addText(JLocale.getString("wait"),
                CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_PLAIN);
        model.addPar(wait);
        screen.updateModel();
        jabber.getConnection().requestDiscoItems(serverJid);
    }

    void setError(String description) {
        clear();
        Parser error = model.createNewParser(false);
        error.addText(description, CanvasEx.THEME_TEXT,  CanvasEx.FONT_STYLE_PLAIN);
        model.addPar(error);
        screen.updateModel();
    }


    private void setCurrTextIndex(int textIndex) {
        int index = 0;
        int currIndex = 0;
        for (int i = 0; i < model.getSize(); ++i) {
            if (model.isSelectable(i)) {
                if (textIndex == currIndex) {
                    index = i;
                    break;
                }
                currIndex++;
            }
        }
        screen.setCurrentItemIndex(index);
    }
    public void textboxAction(InputTextBox box, boolean ok) {
        if (!ok) {
            return;
        }
        if (serverBox == box) {
            setServer(serverBox.getString());
            screen.restore();

        } else if (searchBox == box) {
            String text = searchBox.getString();
            if (isConferenceList) {
                text = StringConvertor.toLowerCase(text);
            }
            int currentIndex = getJidIndex(screen.getCurrItem()) + 1;
            for (int i = currentIndex; i < jids.size(); ++i) {
                String jid = (String)jids.elementAt(i);
                if (-1 != jid.indexOf(text)) {
                    setCurrTextIndex(i);
                    break;
                }
            }
            screen.restore();
        }
    }
}
// #sijapp cond.end #
