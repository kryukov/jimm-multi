package jimmui;

import javax.microedition.lcdui.*;
import jimm.modules.*;
import jimmui.view.base.*;
import jimm.cl.*;
import jimm.*;
import protocol.*;
import protocol.ui.ContactMenu;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 19.07.13 17:32
 *
 * @author vladimir
 */
public class HotKeys {
    ///////////////////
    //               //
    //    Hotkeys    //
    //               //
    ///////////////////

    private static int getHotKeyOpCode(int keyCode) {
        int action = Options.HOTKEY_NONE;
        // #sijapp cond.if modules_ANDROID isnot "true" #
        switch (keyCode) {
            case Canvas.KEY_NUM0:
                action = Options.getInt(Options.OPTION_EXT_CLKEY0);
                break;
            case Canvas.KEY_NUM4:
                action = Options.getInt(Options.OPTION_EXT_CLKEY4);
                break;

            case Canvas.KEY_NUM6:
                action = Options.getInt(Options.OPTION_EXT_CLKEY6);
                break;

            case Canvas.KEY_STAR:
                action = Options.getInt(Options.OPTION_EXT_CLKEYSTAR);
                break;

            case Canvas.KEY_POUND:
                action = Options.getInt(Options.OPTION_EXT_CLKEYPOUND);
                break;

            case NativeCanvas.CAMERA_KEY:
            case NativeCanvas.CALL_KEY:
                action = Options.getInt(Options.OPTION_EXT_CLKEYCALL);
                break;
        }
        // #sijapp cond.end #
        return action;
    }
    public static boolean isHotKey(int keyCode) {
        return (Options.HOTKEY_NONE != getHotKeyOpCode(keyCode));
    }
    public static boolean execHotKey(Protocol p, Contact contact, int keyCode, int type) {
        int action = getHotKeyOpCode(keyCode);
        return (Options.HOTKEY_NONE != action) && execHotKeyAction(p, contact, action, type);
    }

    private static boolean execHotKeyAction(Protocol p, Contact contact, int actionNum, int keyType) {
        if ((CanvasEx.KEY_REPEATED == keyType)
                || (CanvasEx.KEY_RELEASED == keyType)) {
            return false;
        }
        if (Options.HOTKEY_LOCK == actionNum) {
            Jimm.getJimm().lockJimm();
            return true;
        }
        ContactList cl = Jimm.getJimm().getCL();
        if (null != contact) {
            switch (actionNum) {
                // #sijapp cond.if modules_HISTORY is "true" #
                case Options.HOTKEY_HISTORY:
                    new ContactMenu(p, contact).doAction(Contact.USER_MENU_HISTORY);
                    return true;
                // #sijapp cond.end#

                case Options.HOTKEY_INFO:
                    p.showUserInfo(contact);
                    return true;

                case Options.HOTKEY_STATUSES:
                    p.showStatus(contact);
                    return true;

                // #sijapp cond.if modules_FILES is "true"#
                case Options.HOTKEY_SEND_PHOTO:
                    if (FileTransfer.isPhotoSupported()) {
                        new FileTransfer(p, contact).startPhotoTransfer();
                    }
                    return true;
                // #sijapp cond.end#
            }
        }
        Object currentDisplay = Jimm.getJimm().getDisplay().getCurrentDisplay();
        switch (actionNum) {
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case Options.HOTKEY_MAGIC_EYE:
                MagicEye.activate();
                return true;
            // #sijapp cond.end#

            case Options.HOTKEY_OPEN_CHATS:
                if (Jimm.getJimm().getCL().isChats(currentDisplay)) {
                    Jimm.getJimm().getCL().backFromChats();
                } else {
                    Jimm.getJimm().getCL().showChatList(false);
                }
                return true;

            case Options.HOTKEY_ONOFF:
                if (currentDisplay != cl.getManager()) {
                    return true;
                }
                boolean hide = !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, hide);
                Options.safeSave();
                cl.getManager().updateOfflineStatus();
                cl.activate();
                return true;

            case Options.HOTKEY_MINIMIZE:
                Jimm.getJimm().minimize();
                return true;

            // #sijapp cond.if modules_SOUND is "true" #
            case Options.HOTKEY_SOUNDOFF:
                Notify.getSound().changeSoundMode(true);
                return true;
            // #sijapp cond.end#

            case Options.HOTKEY_COLLAPSE_ALL:
                if (currentDisplay == cl.getManager()) {
                    cl.getUpdater().collapseAll();
                }
                return true;
        }
        return false;
    }
}
