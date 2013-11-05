package jimm.cl;

import jimmui.view.UIBuilder;
import jimmui.view.form.Menu;
import jimmui.view.icons.Icon;
import jimm.Jimm;
import jimm.Options;
import jimm.OptionsForm;
import jimm.forms.*;
import jimm.modules.*;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;
import jimmui.view.menu.SelectListener;
import protocol.Protocol;
import protocol.ui.StatusInfo;
import protocol.icq.*;
import protocol.xmpp.*;
import protocol.mrim.*;
import protocol.ui.InfoFactory;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 30.03.13 15:21
 *
 * @author vladimir
 */
public class ProtocolMenu implements SelectListener {
    /* Static constants for menu actios */
    private static final int MENU_DISCONNECT = 2;
    private static final int MENU_DISCO = 3;
    private static final int MENU_OPTIONS = 4;
    private static final int MENU_KEYLOCK = 5;
    private static final int MENU_GLOBAL_STATUS = 6;
    private static final int MENU_ACCOUNTS = 7;
    public static final int MENU_STATUS = 8;
    private static final int MENU_XSTATUS = 9;
    private static final int MENU_PRIVATE_STATUS = 10;
    private static final int MENU_GROUPS = 11;
    private static final int MENU_SEND_SMS = 12;
    private static final int MENU_ABOUT = 13;
    private static final int MENU_SOUND = 14;
    private static final int MENU_MYSELF = 15;
    private static final int MENU_MICROBLOG = 19;
    private static final int MENU_NON = 20;
    private static final int MENU_EXIT = 21;

    private static final int MENU_FIRST_ACCOUNT = 100;
    private Protocol activeProtocol;
    private boolean isMain;
    private final MenuModel menu = new MenuModel();
    private final Menu menuView;

    public ProtocolMenu(Protocol p, boolean main) {
        activeProtocol = p;
        menuView = (Select) UIBuilder.createMenu(menu);
        this.isMain = main;
    }

    public void updateMenu() {
        int currentCommand = menuView.getSelectedItemCode();
        if (isMain) {
            updateMainMenu();
        } else {
            menu.clean();
            protocolMenu();
        }
        menu.setDefaultItemCode(currentCommand);
        menuView.update();
    }
    private MenuModel updateMainMenu() {
        Protocol p = activeProtocol;
        menu.clean();
        // #sijapp cond.if modules_ANDROID isnot "true" #
        menu.addItem("keylock_enable", MENU_KEYLOCK);
        // #sijapp cond.end #
        if (null == p) {
            menu.addItem("set_status", GlobalStatusForm.getGlobalStatusIcon(), MENU_GLOBAL_STATUS);
            menu.addItem("accounts", null, MENU_ACCOUNTS);
        } else if (0 < Jimm.getJimm().getCL().getManager().getModel().getProtocolCount()) {
            protocolMenu();
        }
        if (isSmsSupported()) {
            menu.addItem("send_sms", MENU_SEND_SMS);
        }
        menu.addItem("options_lng", MENU_OPTIONS);

        // #sijapp cond.if modules_SOUND is "true" #
        boolean isSilent = Options.getBoolean(Options.OPTION_SILENT_MODE);
        menu.addItem(isSilent ? "#sound_on" : "#sound_off", MENU_SOUND);
        // #sijapp cond.end#

        menu.addItem("about", MENU_ABOUT);
        menu.addItem("exit", MENU_EXIT);
        return menu;
    }

    private void showAccounts() {
        MenuModel m = new MenuModel();
        m.setActionListener(this);
        int count = Jimm.getJimm().getCL().getManager().getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = Jimm.getJimm().getCL().getManager().getModel().getProtocol(i);
            m.addRawItem(p.getUserId(), InfoFactory.factory.getStatusInfo(p).getIcon(p.getProfile().statusIndex),
                    MENU_FIRST_ACCOUNT + i);
        }
        Jimm.getJimm().getCL().getManager().showMenu(m);
    }

    private void protocolMenu() {
        Protocol protocol = activeProtocol;
        int id = protocol.isConnected() && protocol.hasVCardEditor() ? MENU_MYSELF : MENU_NON;
        menu.addRawItem(protocol.getUserId(), null, id);
        if (protocol.isConnecting()) {
            menu.addItem("disconnect", MENU_DISCONNECT);
            return;
        }
        /*
        if (protocol.isConnected()) {
            menu.addItem("disconnect", MENU_DISCONNECT);

        } else {
            menu.addItem("connect", MENU_CONNECT);
        }
        */
        menu.addItem("set_status",
                InfoFactory.factory.getStatusInfo(protocol).getIcon(protocol.getProfile().statusIndex),
                MENU_STATUS);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        if (hasXStatus(protocol)) {
            Icon icon = InfoFactory.factory.getXStatusInfo(protocol).getIcon(protocol.getProfile().xstatusIndex);
            menu.addItem("set_xstatus", icon, MENU_XSTATUS);
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            menu.addItem("private_status", PrivateStatusForm.getIcon(protocol),
                    MENU_PRIVATE_STATUS);
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        if (protocol.isConnected()) {
            // #sijapp cond.if protocols_JABBER is "true" #
            if (protocol instanceof Xmpp) {
                if (((Xmpp)protocol).hasS2S()) {
                    menu.addItem("service_discovery", MENU_DISCO);
                }
            }
            // #sijapp cond.end #
            menu.addItem("manage_contact_list", MENU_GROUPS);
            if (protocol.hasVCardEditor()) {
                menu.addItem("myself", MENU_MYSELF);
            }
            // #sijapp cond.if protocols_MRIM is "true" #
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            if (protocol instanceof Mrim) {
                menu.addItem("microblog",
                        ((Mrim) protocol).getMicroBlog().getIcon(), MENU_MICROBLOG);
            }
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
    }
    // #sijapp cond.if modules_XSTATUSES is "true" #
    private boolean hasXStatus(Protocol protocol) {
        return null != InfoFactory.factory.getXStatusInfo(protocol)
                && 0 < InfoFactory.factory.getXStatusInfo(protocol).getXStatusCount();
    }
    // #sijapp cond.end #

    private boolean isSmsSupported() {
        // #sijapp cond.if protocols_MRIM is "true" #
        int count = Jimm.getJimm().getCL().getManager().getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = Jimm.getJimm().getCL().getManager().getModel().getProtocol(i);
            if ((p instanceof Mrim) && p.isConnected()) {
                return true;
            }
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" #
        // #sijapp cond.if modules_FILES="true"#
        if (!Jimm.getJimm().phone.isCollapsible()) {
            return true;
        }
        // #sijapp cond.end #
        // #sijapp cond.end #
        return false;
    }

    private void execCommand(int cmd) {
        if (MENU_FIRST_ACCOUNT <= cmd) {
            Protocol p = Jimm.getJimm().getCL().getManager().getModel().getProtocol(cmd - MENU_FIRST_ACCOUNT);
            MenuModel model = Jimm.getJimm().getCL().getContextMenu(p, null);
            Jimm.getJimm().getCL().getManager().showMenu(model);
            return;
        }
        final Protocol proto = activeProtocol;
        switch (cmd) {
            case MENU_DISCONNECT:
                proto.setStatus(StatusInfo.STATUS_OFFLINE, "");
                Thread.yield();
                Jimm.getJimm().getCL().activate();
                break;

            case MENU_KEYLOCK:
                Jimm.getJimm().lockJimm();
                break;

            case MENU_GLOBAL_STATUS:
                new GlobalStatusForm().show();
                break;

            case MENU_ACCOUNTS:
                showAccounts();
                break;

            case MENU_STATUS:
                new SomeStatusForm(proto).show();
                break;

            // #sijapp cond.if modules_XSTATUSES is "true" #
            case MENU_XSTATUS:
                new SomeXStatusForm(proto).show();
                break;
            // #sijapp cond.end #

            // #sijapp cond.if protocols_ICQ is "true" #
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            case MENU_PRIVATE_STATUS:
                new PrivateStatusForm(proto).show();
                break;
            // #sijapp cond.end #
            // #sijapp cond.end #

            // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_DISCO:
                ((Xmpp)proto).getServiceDiscovery().showIt();
                break;
            // #sijapp cond.end #

            // #sijapp cond.if protocols_MRIM is "true" #
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case MENU_MICROBLOG:
                ((Mrim)proto).getMicroBlog().activate();
                updateMenu();
                menuView.update();
                break;
            // #sijapp cond.end #
            // #sijapp cond.end #

            case MENU_OPTIONS:
                new OptionsForm().show();
                break;

            case MENU_ABOUT:
                new SysTextList().makeAbout().show();
                break;

            case MENU_GROUPS:
                new ManageContactListForm(proto).show();
                break;

            // #sijapp cond.if modules_SOUND is "true" #
            case MENU_SOUND:
                Notify.getSound().changeSoundMode(false);
                updateMenu();
                menuView.update();
                break;
            // #sijapp cond.end#

            case MENU_MYSELF:
                proto.showUserInfo(proto.createTempContact(proto.getUserId(), proto.getNick()));
                break;

            case MENU_SEND_SMS:
                new SmsForm(null, null).show();
                break;

            case MENU_EXIT:
                Jimm.getJimm().quit();
                break;
        }
    }

    public void select(Select select, MenuModel model, int cmd) {
        execCommand(cmd);
    }

    public void setDefaultItemCode(int item) {
        menu.setDefaultItemCode(item);
    }

    public Menu getView() {
        menu.setActionListener(this);
        return menuView;
    }
    public MenuModel getModel() {
        menu.setActionListener(this);
        return menu;
    }

    public void setProtocol(Protocol p) {
        activeProtocol = p;
    }
}
