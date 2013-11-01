/*
 * StatusView.java
 *
 * Created on 12 Август 2010 г., 21:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.ui;

import jimm.cl.SysTextList;
import jimm.comm.StringUtils;
import jimmui.view.text.TextListModel;
import jimmui.view.icons.Icon;
import jimmui.view.text.Parser;
import jimm.Jimm;
import jimm.comm.Util;
import jimmui.view.base.CanvasEx;
import jimmui.view.menu.*;
import jimmui.view.text.TextList;
import jimmui.view.text.TextListController;
import protocol.Contact;
import protocol.Protocol;
import protocol.icq.*;
import protocol.xmpp.*;

/**
 *
 * @author Vladimir Kryukov
 */
public final class StatusView extends TextListController {
    public static final int INFO_MENU_COPY     = 1;
    public static final int INFO_MENU_COPY_ALL = 2;
    public static final int INFO_MENU_GOTO_URL = 4;
    public static final int INFO_MENU_GET_X    = 5;
    private Protocol protocol;
    private Contact contact;
    // #sijapp cond.if modules_CLIENTS is "true" #
    private String clientVersion;
    // #sijapp cond.end #
    private TextListModel model = new TextListModel();


    /** Creates a new instance of StatusView */
    public StatusView() {
        list = new TextList(null);
        list.setModel(model);
        setDefaultCode(INFO_MENU_COPY);
    }
    protected void doJimmAction(int action) {
        doJimmBaseAction(action);
        switch (action) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                list.getTextContent().getController().copy(INFO_MENU_COPY_ALL == action);
                list.restore();
                break;

            case INFO_MENU_GOTO_URL:
                SysTextList.gotoURL(model.getParText(list.getTextContent().getCurrItem()));
                break;

                // #sijapp cond.if protocols_ICQ is "true" #
                // #sijapp cond.if modules_XSTATUSES is "true" #
            case INFO_MENU_GET_X:
                ((Icq)protocol).requestXStatusMessage(contact);
                list.restore();
                break;
                // #sijapp cond.end #
                // #sijapp cond.end #
        }
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void addClient() {
        addBr();
        ClientInfo info = InfoFactory.factory.getClientInfo(protocol);
        if ((ClientInfo.CLI_NONE != contact.clientIndex)
                && (null != info)) {
            addPlain(info.getIcon(contact.clientIndex),
                    (info.getName(contact.clientIndex)
                    + " " + contact.version).trim());
        }
        addPlain(null, clientVersion);
    }
    public void setClientVersion(String version) {
        clientVersion = version;
    }
    // #sijapp cond.end #

    public void addTime() {
        if (!contact.isSingleUserContact()) {
            return;
        }
        // #sijapp cond.if protocols_JABBER is "true" #
        if (contact instanceof XmppServiceContact) {
            return;
        }
        // #sijapp cond.end #
        long signonTime = contact.chaingingStatusTime;
        if (0 < signonTime) {
            long now = Jimm.getCurrentGmtTime();
            boolean today = (now - 24 * 60 * 60) < signonTime;
            if (contact.isOnline()) {
                addInfo("li_signon_time", Util.getLocalDateString(signonTime, today));
                addInfo("li_online_time", Util.longitudeToString(now - signonTime));
            } else {
                addInfo("li_signoff_time", Util.getLocalDateString(signonTime, today));
            }
        }
    }
    /////////////////////
    public void addBr() {
        Parser line = model.createNewParser(false);
        line.addText(" \n", CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
        model.addPar(line);
    }
    public void addPlain(Icon img, String str) {
        if (!StringUtils.isEmpty(str)) {
            Parser line = model.createNewParser(true);
            if (null != img) {
                line.addImage(img);
            }
            line.addText(str, CanvasEx.THEME_TEXT, CanvasEx.FONT_STYLE_PLAIN);
            model.addPar(line);
        }
    }
    public void addStatusText(String text) {
        if (!StringUtils.isEmpty(text)) {
            Parser line = model.createNewParser(true);
            line.addText(text, CanvasEx.THEME_PARAM_VALUE, CanvasEx.FONT_STYLE_PLAIN);
            model.addPar(line);
        }
    }
    public void addInfo(String key, String value) {
        model.addParam(key, value);
    }
    /////////////////////
    public void addContactStatus() {
        byte status = contact.getStatusIndex();
        StatusInfo info = InfoFactory.factory.getStatusInfo(protocol);
        addStatus(info.getIcon(status), info.getName(status));
    }
    // #sijapp cond.if modules_XSTATUSES is "true" #
    public void addXStatus() {
        XStatusInfo info = InfoFactory.factory.getXStatusInfo(protocol);
        int x = contact.getXStatusIndex();
        addStatus(info.getIcon(x), info.getName(x));
    }
    // #sijapp cond.end #
    public void addStatus(Icon icon, String name) {
        addPlain(icon, name);
    }
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     INFO_MENU_COPY);
        menu.addItem("copy_all_text", INFO_MENU_COPY_ALL);
        if ((1 < list.getTextContent().getCurrItem()) && Util.hasURL(model.getParText(list.getTextContent().getCurrItem()))) {
            menu.addItem("goto_url", INFO_MENU_GOTO_URL);
        }
        // #sijapp cond.if protocols_ICQ is "true" #
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                && (protocol instanceof Icq)) {
            menu.addItem("reqxstatmsg", INFO_MENU_GET_X);
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        menu.setActionListener(this);
        return menu;
    }
    public void init(Protocol p, Contact c) {
        contact = c;
        protocol = p;
        // #sijapp cond.if modules_CLIENTS is "true" #
        clientVersion = null;
        // #sijapp cond.end #
        list.getTextContent().setAllToTop();
    }
    public void initUI() {
        list.lock();
        model.clear();
        model.updateFontSet();
        list.setCaption(contact.getName());
        addInfo(protocol.getUserIdName(), contact.getUserId());
    }
    public Contact getContact() {
        return contact;
    }
    public void update() {
        list.updateModel();
    }
    public void showIt() {
        list.setController(this);
        list.getTextContent().setAllToTop();
        list.unlock();
        list.show();
    }
}
