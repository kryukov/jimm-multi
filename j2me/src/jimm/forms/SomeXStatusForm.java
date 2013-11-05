/*
 * SomeXStatusForm.java
 *
 * Created on 7 Август 2011 г., 13:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

// #sijapp cond.if modules_XSTATUSES is "true" #
import jimm.Jimm;
import jimm.comm.StringUtils;
import jimm.io.Storage;
import jimmui.view.InputTextBox;
import jimmui.view.TextBoxListener;
import jimmui.view.UIBuilder;
import jimmui.view.form.Form;
import jimmui.view.form.FormListener;
import jimmui.view.menu.*;
import protocol.*;
import protocol.icq.*;
import protocol.xmpp.*;
import protocol.mrim.*;
import protocol.ui.InfoFactory;
import protocol.ui.XStatusInfo;

/**
 *
 * @author Vladimir Kryukov
 */
public final class SomeXStatusForm implements SelectListener, TextBoxListener, FormListener {
    private MenuModel menu = new MenuModel();
    private Protocol protocol;
    private int xstatus;
    private InputTextBox message;
    private static final int OPTION_XTRAZ_TITLE = 10;
    private static final int OPTION_XTRAZ_DESC  = 11;

    protected String[] xst_titles = new String[100];
    protected String[] xst_descs  = new String[100];

    public SomeXStatusForm(Protocol protocol) {
        this.protocol = protocol;
        load();
    }
    public final void select(Select select, MenuModel model, int statusIndex) {
        xstatus = statusIndex;
        if (-1 == statusIndex) {
            setXStatus("", "");
            back();

        } else {
            onStatusSelected();
        }
    }
    private String getProtocolId() {
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            return "icq";
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            return "mrim";
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_JABBER is "true" #
        if (protocol instanceof Xmpp) {
            return "jabber";
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_OBIMP is "true" #
        if (protocol instanceof protocol.obimp.Obimp) {
            return "obimp";
        }
        // #sijapp cond.end#
        return "";
    }

    private void load() {
        try {
            Storage storage = new Storage(getProtocolId() + "-xstatus");
            storage.open(false);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception ignored) {
        }
    }

    public final void show() {
        menu.clean();
        XStatusInfo info = InfoFactory.factory.getXStatusInfo(protocol);
        for (byte i = -1; i < info.getXStatusCount(); ++i) {
            menu.addItem(info.getName(i), info.getIcon(i), i);
        }
        menu.setDefaultItemCode(protocol.getProfile().xstatusIndex);
        menu.setActionListener(this);
        UIBuilder.createMenu(menu).show();
    }
    public final void back() {
        Jimm.getJimm().getCL().activate();
    }


    protected final void setXStatus(String title, String desc) {
        if (0 <= xstatus) {
            xst_titles[xstatus] = StringUtils.notNull(title);
            xst_descs[xstatus]  = StringUtils.notNull(desc);
            try {
                Storage storage = new Storage(getProtocolId() + "-xstatus");
                storage.open(true);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception ignored) {
            }
        }
        protocol.setXStatus(xstatus, title, desc);
        Jimm.getJimm().getCL().updateMainMenu();
    }

    public void textboxAction(InputTextBox box, boolean ok) {
        if ((message == box) && ok) {
            setXStatus(message.getString(), "");
            back();
        }
    }

    public void formAction(Form form, boolean apply) {
        if (apply) {
            setXStatus(form.getTextFieldValue(OPTION_XTRAZ_TITLE),
                    form.getTextFieldValue(OPTION_XTRAZ_DESC));
        }
        back();
    }

    protected void onStatusSelected() {
        int title = 512;
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            title = 32;
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            title = 128;
        }
        // #sijapp cond.end #

        int id = xstatus;
        message = new InputTextBox().create("status_message", title);
        message.setString(xst_titles[id]);
        message.setTextBoxListener(this);
        message.show();
    }
}
// #sijapp cond.end #