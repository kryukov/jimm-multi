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
 File: src/jimm/Search.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher
 *******************************************************************************/

package jimm.search;

import jimm.ui.text.TextList;
import jimm.ui.text.TextListModel;
import java.util.Vector;

import jimm.cl.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.ui.base.Binder;
import jimm.ui.base.CanvasEx;
import jimm.ui.form.*;
import jimm.ui.menu.*;
import jimm.ui.text.TextListController;
import jimm.util.*;
import protocol.*;
import protocol.icq.*;
import protocol.mrim.*;

public final class Search implements FormListener, TextListExCommands, ActionListener, ControlStateListener {
    final public static int UIN         = 0;
    final public static int NICK        = 1;
    final public static int FIRST_NAME  = 2;
    final public static int LAST_NAME   = 3;
    final public static int EMAIL       = 4;
    final public static int CITY        = 5;
    final public static int GENDER      = 6;
    final public static int ONLY_ONLINE = 7;
    final public static int AGE         = 8;
    final public static int LAST_INDEX  = 9;

    /* Textboxes for adding */
    private static final int USERID = 1000;
    private static final int GROUP = 1001;
    private static final int PROFILE = 1002;
    private static final int REQ_AUTH = 1020;

    private static final int MENU_ADD     = 0;
    private static final int MENU_MESSAGE = 1;

    /* Forms for results and query */
    private GraphForm searchForm;

    private TextList screen;

    private Group group;
    private boolean waitResults = false;
    private String preferredNick;

    private Vector results = new Vector();

    private Protocol protocol;
    private boolean icqFields;
    private byte type;
    private String[] searchParams = new String[Search.LAST_INDEX];
    // #sijapp cond.if protocols_JABBER is "true" #
    private String jabberGate = null;
    // #sijapp cond.end #

    private int currentResultIndex;
    private static final String ageList = "-|13-17|18-22|23-29|30-39|40-49|50-59|60-";
    private static final String[] ages = Util.explode(ageList, '|');

    private static final byte TYPE_FULL = 0;
    private static final byte TYPE_LITE = 1;

    private int searchId;

    public int getSearchId() {
        return searchId;
    }

    public Search(Protocol protocol) {
        this.protocol = protocol;

        // #sijapp cond.if protocols_ICQ is "true" #
        icqFields = (protocol instanceof Icq);
        // #sijapp cond.else #
        icqFields = false;
        // #sijapp cond.end #

        preferredNick = null;

    }
    public void controlStateChanged(GraphForm form, int id) {
        if (PROFILE == id) {
            String userid = searchForm.getTextFieldValue(USERID);
            if (StringConvertor.isEmpty(userid)) {
                return;
            }
            // #sijapp cond.if protocols_JABBER is "true" #
            if ((null != jabberGate) && !userid.endsWith(jabberGate)) {
                userid = userid.replace('@', '%') + '@' + jabberGate;
            }
            // #sijapp cond.end #
            Contact contact = protocol.createTempContact(userid);
            if (null != contact) {
                protocol.showUserInfo(contact);
            }
        }
    }
    public void show() {
        type = TYPE_FULL;
        createSearchForm();
        searchForm.show();
    }
    public void show(String uin) {
        type = TYPE_LITE;
        setSearchParam(Search.UIN, uin);
        createSearchForm();
        searchForm.show();
    }
    private void showResults() {
        results.removeAllElements();
        searchId = Util.uniqueValue();
        waitResults = true;
        showWaitScreen();
        protocol.searchUsers(this);
    }
    public final void putToGroup(Group group) {
        this.group = group;
    }
    private Vector getGroups() {
        Vector all = protocol.getGroupItems();
        Vector groups = new Vector();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group)all.elementAt(i);
            if (g.hasMode(Group.MODE_NEW_CONTACTS)) {
                groups.addElement(g);
            }
        }
        return groups;
    }

    /* Add a result to the results vector */
    public void addResult(UserInfo info) {
        results.addElement(info);
    }

    private UserInfo getCurrentResult() {
        return (UserInfo) results.elementAt(currentResultIndex);
    }
    private int getResultCount() {
        return results.size();
    }

    // #sijapp cond.if protocols_JABBER is "true" #
    public void setJabberGate(String gate) {
        jabberGate = gate;
    }
    // #sijapp cond.end #

    public String getSearchParam(int param) {
        return searchParams[param];
    }
    public void setSearchParam(int param, String value) {
        searchParams[param] = StringConvertor.isEmpty(value) ? null : value;
    }
    public String[] getSearchParams() {
        return searchParams;
    }

    public void finished() {
        if (waitResults) {
            activate();
        }
        waitResults = false;
    }
    public void canceled() {
        if (waitResults) {
            searchId = -1;
            searchForm.restore();
        }
        waitResults = false;
    }

    private void addUserIdItem() {
        String userid = StringConvertor.notNull(getSearchParam(UIN));
        searchForm.addTextField(USERID, protocol.getUserIdName(), userid, 64);
    }
    private void createSearchForm() {
        /* Result Screen */
        screen = new TextList("");
        screen.setUpdateListener(this);

        /* Form */
        searchForm = new GraphForm((TYPE_LITE == type) ? "add_user" : "search_user",
                "ok", "back", this);
        if (TYPE_LITE == type) {
            addUserIdItem();
            // #sijapp cond.if protocols_JABBER is "true" #
            if (null != jabberGate) {
                searchForm.addString("transport", jabberGate);
            }
            // #sijapp cond.end #

            Vector groups = getGroups();
            if (!groups.isEmpty()) {
                String[] list = new String[groups.size()];
                int def = 0;
                for (int i = 0; i < groups.size(); ++i) {
                    Group g = (Group) groups.elementAt(i);
                    list[i] = g.getName();
                    if (g == group) {
                        def = i;
                    }
                }
                searchForm.addSelector(GROUP, "group", list, def);
            }
            boolean request_auth = true;
            // #sijapp cond.if protocols_MRIM is "true" #
            if (protocol instanceof Mrim) {
                request_auth = false;
            }
            // #sijapp cond.end #
            if (request_auth) {
                searchForm.addCheckBox(REQ_AUTH, "requauth", true);
            }
            searchForm.addLink(PROFILE, JLocale.getString("info"));
            searchForm.setControlStateListener(this);
            return;
        }
        searchForm.addCheckBox(Search.ONLY_ONLINE, "only_online", false);
        addUserIdItem();
        searchForm.addTextField(Search.NICK, "nick", "", 64);
        searchForm.addTextField(Search.FIRST_NAME, "firstname", "", 64);
        searchForm.addTextField(Search.FIRST_NAME, "lastname", "", 64);
        searchForm.addTextField(Search.CITY, "city", "", 64);
        searchForm.addSelector(Search.GENDER, "gender", "female_male" + "|" + "female" + "|" + "male", 0);
        // #sijapp cond.if protocols_ICQ is "true" #
        if (icqFields) {
            searchForm.addTextField(Search.EMAIL, "email", "", 64);
        }
        // #sijapp cond.end #
        searchForm.addSelector(Search.AGE, "age", ageList, 0);
    }

    /* Activate search form */
    private void activate() {
        drawResultScreen();
        screen.restore();
    }

    private void showWaitScreen() {
        screen.lock();
        screen.setCaption(JLocale.getString("search_user"));
        screen.setAllToTop();
        TextListModel model = new TextListModel();
        model.setInfoMessage(JLocale.getString("wait"));
        screen.setController(new TextListController(null, MENU_ADD));
        screen.setModel(model);
        screen.show();
    }
    private void drawResultScreen() {
        int resultCount = getResultCount();

        if (0 < resultCount) {
            screen.setCaption(JLocale.getString("results")
                    + " " + (currentResultIndex + 1) + "/" + resultCount);
            screen.setAllToTop();
            UserInfo userInfo = getCurrentResult();
            userInfo.setSeachResultFlag();
            userInfo.setProfileView(screen);
            userInfo.updateProfileView();

        } else {
            /* Show a result entry */
            screen.lock();
            screen.setCaption(JLocale.getString("results") + " 0/0");
            screen.setAllToTop();
            TextListModel model = new TextListModel();
            model.setInfoMessage(JLocale.getString("no_results"));
            screen.setModel(model);
        }

        MenuModel menu = new MenuModel();
        if (0 < resultCount) {
            menu.addItem("add_to_list", MENU_ADD);
            menu.addItem("send_message", MENU_MESSAGE);
        }
        menu.setActionListener(new Binder(this));
        screen.setController(new TextListController(menu, MENU_ADD));
    }

    private void nextOrPrev(boolean next) {
        int size = getResultCount();
        if (0 < size) {
            if (1 < size) {
                getCurrentResult().setProfileView(null);
                getCurrentResult().removeAvatar();
            }
            currentResultIndex = ((next ? 1 : size - 1) + currentResultIndex) % size;
        }
        activate();
    }

    public void onContentMove(TextListModel sender, int direction) {
        nextOrPrev(1 == direction);
    }

    public void formAction(GraphForm form, boolean apply) {
        if (apply) {
            if (TYPE_FULL == type) {
                currentResultIndex = 0;
                setSearchParam(Search.UIN, searchForm.getTextFieldValue(USERID).trim());
                setSearchParam(Search.NICK,        searchForm.getTextFieldValue(Search.NICK));
                setSearchParam(Search.FIRST_NAME,  searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.LAST_NAME,   searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.CITY,        searchForm.getTextFieldValue(Search.CITY));
                setSearchParam(Search.GENDER,      Integer.toString(searchForm.getSelectorValue(Search.GENDER)));
                setSearchParam(Search.ONLY_ONLINE, searchForm.getCheckBoxValue(Search.ONLY_ONLINE) ? "1" : "0");
                setSearchParam(Search.AGE,         ages[searchForm.getSelectorValue(Search.AGE)]);
                // #sijapp cond.if protocols_ICQ is "true" #
                if (icqFields) {
                    setSearchParam(Search.EMAIL, searchForm.getTextFieldValue(Search.EMAIL));
                }
                // #sijapp cond.end #
                showResults();

            } else if (TYPE_LITE == type) {
                String userid = searchForm.getTextFieldValue(USERID).trim();
                userid = StringConvertor.toLowerCase(userid);
                if (StringConvertor.isEmpty(userid)) {
                    return;
                }
                // #sijapp cond.if protocols_JABBER is "true" #
                if ((null != jabberGate) && !userid.endsWith(jabberGate)) {
                    userid = userid.replace('@', '%') + '@' + jabberGate;
                }
                // #sijapp cond.end #

                Contact contact = protocol.createTempContact(userid);
                if (null != contact) {
                    if (contact.isTemp()) {
                        String g = null;
                        if (!contact.isSingleUserContact()) {
                            g = contact.getDefaultGroupName();
                        }
                        if (null == g) {
                            g = searchForm.getSelectorString(GROUP);
                        }
                        contact.setName(preferredNick);
                        contact.setGroup(protocol.getGroup(g));
                        protocol.addContact(contact);
                        if (searchForm.getCheckBoxValue(REQ_AUTH)
                                && contact.isSingleUserContact()) {
                            protocol.requestAuth(contact);
                        }
                    }
                    ContactList.getInstance().activate(contact);
                }
            }

        } else {
            form.back();
        }
    }
    private Contact createContact(UserInfo resultData) {
        String uin = StringConvertor.toLowerCase(resultData.uin.trim());
        // #sijapp cond.if protocols_JABBER is "true" #
        if ((null != jabberGate) && !uin.endsWith(jabberGate)) {
            uin = uin.replace('@', '%') + '@' + jabberGate;
        }
        // #sijapp cond.end #
        Contact contact = protocol.getItemByUIN(uin);
        if (null == contact) {
            contact = protocol.createTempContact(uin);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            protocol.addTempContact(contact);
            contact.setOfflineStatus();
            contact.setName(resultData.getOptimalName());
        }
        return contact;
    }

    public void action(CanvasEx canvas, int cmd) {
        switch (cmd) {
            case MENU_ADD:
                UserInfo info = getCurrentResult();
                Search s = new Search(protocol);
                s.preferredNick = info.getOptimalName();
                s.show(info.uin);
                break;

            case MENU_MESSAGE:
                UserInfo temp = getCurrentResult();
                createContact(temp).activate(protocol);
                break;
        }
    }
}
