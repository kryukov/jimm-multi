package jimmui.view.chat;

import jimm.*;
import jimm.cl.SysTextList;
import jimm.comm.StringUtils;
import jimm.history.*;
import jimmui.Clipboard;
import jimmui.HotKeys;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
import jimmui.view.base.*;
import jimmui.view.base.touch.*;
import jimmui.view.icons.Icon;
import jimmui.view.menu.MenuModel;
import protocol.Contact;
import protocol.Protocol;
import protocol.ui.*;

import javax.microedition.lcdui.Font;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 21:44
 *
 * @author vladimir
 */
public class ChatContent extends SomeContent {
    private ChatModel model;
    private boolean selectMode;
    public static final String ADDRESS = ", ";

    public ChatContent(SomeContentList view, ChatModel model) {
        super(view);
        this.model = model;
    }

    @Override
    public void setTopByOffset(int offset) {
        super.setTopByOffset(offset);
        model.bottomOffset = getTopOffset() + view.getContentHeight();
    }

    @Override
    protected void setCurrItem(int cItem) {
        super.setCurrItem(cItem);
        model.current = getCurrItem();
    }

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public int getItemHeight(int itemIndex) {
        return model.getItemHeight(model.getMessage(itemIndex));
    }

    @Override
    public void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        int header = model.getMessageHeaderHeight(mData);
        if (0 < header) {
            int visHeader = Math.min(Math.max(0, header - skip), to);
            drawMessageHeader(g, mData, x, y, w, header, skip, visHeader);
            y += header;
            h -= header;
            skip -= header;
        }
        model.getMessage(index).par.paint(model.fontSet, g, 1, y, skip, to);
    }

    public void drawItemBack(GraphicsEx g, int index, int selected, int x, int y, int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        byte bg;
        if (mData.isMarked()) {
            bg = CanvasEx.THEME_CHAT_BG_MARKED;
        } else if (mData.isService()) {
            bg = CanvasEx.THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = mData.isIncoming() ? CanvasEx.THEME_CHAT_BG_IN : CanvasEx.THEME_CHAT_BG_OUT;
        } else {
            bg = mData.isIncoming() ? CanvasEx.THEME_CHAT_BG_IN_ODD : CanvasEx.THEME_CHAT_BG_OUT_ODD;
        }
        if (g.notEqualsColor(CanvasEx.THEME_BACKGROUND, bg)) {
            if (selected == index) {
                g.setThemeColor(CanvasEx.THEME_SELECTION_BACK, bg, 0xA0);
            } else {
                g.setThemeColor(bg);
            }
            g.fillRect(x, y + skip, w, to);
        }
    }

    private void drawMessageHeader(GraphicsEx g, MessData mData, int x1, int y1, int w, int baseLine, int skip, int visHeight) {
        Icon icon = InfoFactory.msgIcons.iconAt(mData.iconIndex);
        if (null != icon) {
            int iconWidth = g.drawImage(icon, x1, y1, baseLine) + 1;
            x1 += iconWidth;
            w -= iconWidth;
        }

        Font[] set = model.fontSet;
        Font boldFont = set[CanvasEx.FONT_STYLE_BOLD];
        g.setFont(boldFont);
        g.setThemeColor(getInOutColor(mData.isIncoming()));

        Font plainFont = set[CanvasEx.FONT_STYLE_PLAIN];
        String time = mData.isMarked() ? "  v  " : mData.strTime;
        int timeWidth = plainFont.stringWidth(time);

        g.drawString(mData.getNick(), x1, y1, w - timeWidth, baseLine, skip, visHeight);

        g.setFont(plainFont);
        g.drawString(time, x1 + w - timeWidth, y1, timeWidth, baseLine, skip, visHeight);
    }

    private byte getInOutColor(boolean incoming) {
        return incoming ? CanvasEx.THEME_CHAT_INMSG : CanvasEx.THEME_CHAT_OUTMSG;
    }

    @Override
    public void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_MENU:
                view.showMenu(getMenu());
                return;

            case NativeCanvas.JIMM_BACK:
                if (0 == model.size()) {
                    Jimm.getJimm().jimmModel.unregisterChat(model);
                }
                Jimm.getJimm().getCL().activate(getContact());
                return;

            case NativeCanvas.JIMM_SELECT:
                onMessageSelected();
                return;
        }
        if (!model.writable && ((ACTION_REPLY == action)
                || (Contact.USER_MENU_MESSAGE == action))) {
            return;
        }
        switch (action) {
            case ACTION_REPLY:
                execJimmAction(NativeCanvas.JIMM_SELECT);
                break;

            case ACTION_COPY_TEXT:
                copyText();
                break;

            case ACTION_GOTO_URL:
                SysTextList.gotoURL(getCurrentText());
                break;

            case ACTION_SELECT:
                markItem(getCurrItem());
                break;

            // #sijapp cond.if modules_HISTORY is "true" #
            case ACTION_ADD_TO_HISTORY:
                addTextToHistory();
                break;
            // #sijapp cond.end#

            case ACTION_DEL_CHAT:
                Jimm.getJimm().getChatUpdater().removeMessagesAtCursor(model);
                if (0 < getSize()) {
                    view.restore();
                } else {
                    Jimm.getJimm().jimmModel.unregisterChat(model);
                    Jimm.getJimm().getCL().activate(null);
                }
                break;

            // #sijapp cond.if modules_FILES="true"#
            case ACTION_FT_CANCEL:
                Jimm.getJimm().jimmModel.removeTransfer(getCurrentMsgData(), true);
                break;
            // #sijapp cond.end#

            default:
                new ContactMenu(getProtocol(), getContact()).doAction(action);
        }
    }
    private static final int ACTION_FT_CANCEL = 900;
    private static final int ACTION_REPLY = 901;
    private static final int ACTION_ADD_TO_HISTORY = 902;
    private static final int ACTION_COPY_TEXT = 903;
    private static final int ACTION_GOTO_URL = 904;
    private static final int ACTION_DEL_CHAT = 905;
    private static final int ACTION_SELECT = 906;


    private MenuModel getContextMenu() {
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_FILES="true"#
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        } else {
            menu.addItem("select", ACTION_SELECT);
        }
        // #sijapp cond.else#
        menu.addItem("select", ACTION_SELECT);
        // #sijapp cond.end#
        if (!selectMode) {
            if ((null != md) && md.isURL()) {
                menu.addItem("goto_url", ACTION_GOTO_URL);
            }
        }
        menu.addItem("copy_text", ACTION_COPY_TEXT);
        if (!selectMode) {
            // #sijapp cond.if modules_HISTORY is "true" #
            if (!Options.getBoolean(Options.OPTION_HISTORY) && getContact().hasHistory()) {
                menu.addItem("add_to_history", ACTION_ADD_TO_HISTORY);
            }
            // #sijapp cond.end#
            if (getContact().isSingleUserContact()) {
                if (model.isBlogBot()) {
                    menu.addItem("reply", ACTION_REPLY);
                }
            } else {
                if (model.writable) {
                    menu.addItem("reply", ACTION_REPLY);
                }
            }
        }
        menu.setActionListener(new Binder(this));
        return menu;
    }
    protected MenuModel getMenu() {
        if (selectMode) {
            MenuModel menu = new MenuModel();
            menu.addItem("copy_text", ACTION_COPY_TEXT);
            menu.setActionListener(new Binder(this));
            return menu;
        }
        boolean accessible = model.writable && (getContact().isSingleUserContact() || getContact().isOnline());
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        // #sijapp cond.if modules_ANDROID isnot "true" #
        // #sijapp cond.if modules_FILES="true"#
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
        if (model.hasAuthRequests()) {
            menu.addItem("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.addItem("deny", Contact.USER_MENU_DENY_AUTH);
        }

        // #sijapp cond.if modules_ANDROID isnot "true" #
        // not in touch
        if (getContact().isSingleUserContact()) {
            if (model.isBlogBot()) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            } else {
                menu.addItem("reply", Contact.USER_MENU_MESSAGE);
            }
        } else {
            if (model.writable) {
                menu.addItem("message", Contact.USER_MENU_MESSAGE);
                menu.addItem("reply", ACTION_REPLY);
            }
        }
        // #sijapp cond.end#
        if (!getContact().isSingleUserContact()) {
            menu.addItem("list_of_users", Contact.USER_MENU_USERS_LIST);
        }
        // #sijapp cond.if modules_ANDROID isnot "true" #
        if ((null != md) && md.isURL()) {
            menu.addItem("goto_url", ACTION_GOTO_URL);
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_FILES is "true"#
        if (accessible) {
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addItem("ft_name", Contact.USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.addItem("ft_cam", Contact.USER_MENU_CAM_TRANS);
            }
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_ANDROID isnot "true" #
        menu.addItem("copy_text", ACTION_COPY_TEXT);
        // #sijapp cond.end#
        if (accessible) {
            if (!Clipboard.isClipBoardEmpty()) {
                menu.addItem("paste", Contact.USER_MENU_PASTE);
            }
        }
        getContact().addChatMenuItems(menu);


        // #sijapp cond.if modules_ANDROID isnot "true" #
        // #sijapp cond.if modules_HISTORY is "true" #
        if (!Options.getBoolean(Options.OPTION_HISTORY) && getContact().hasHistory()) {
            menu.addItem("add_to_history", ACTION_ADD_TO_HISTORY);
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
        if (!getContact().isAuth()) {
            menu.addItem("requauth", Contact.USER_MENU_REQU_AUTH);
        }
        //menu.addItem("user_menu",   USER_MENU_SHOW);
        //if (!getContact().isSingleUserContact() && getContact().isOnline()) {
        //    menu.addItem("leave_chat", Contact.CONFERENCE_DISCONNECT);
        //}
        menu.addItem("delete_chat", ACTION_DEL_CHAT);
        menu.setActionListener(new Binder(this));
        return menu;
    }

    void onMessageSelected() {
        if (selectMode) {
            markItem(getCurrItem());
            return;
        }
        if (getContact().isSingleUserContact()) {
            if (model.isBlogBot()) {
                writeMessage(getBlogPostId(getCurrentText()));
                return;
            }
            writeMessage(null);
            return;
        }
        MessData md = getCurrentMsgData();
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        writeMessageTo(model.getMyName().equals(nick) ? null : nick);
    }

    private Contact getContact() {
        return model.contact;
    }
    private Protocol getProtocol() {
        return model.protocol;
    }


    public final void writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += ADDRESS;

        } else {
            nick = "";
        }
        writeMessage(nick);
    }

    public final void writeMessage(String initText) {
        if (model.writable) {
            // #sijapp cond.if modules_ANDROID is "true" #
            if (true) {
                ((Chat)view).activate();
                Jimm.getJimm().getDisplay().getNativeCanvas().getInput().setText(initText);
                return;
            }
            // #sijapp cond.end #
            MessageEditor editor = Jimm.getJimm().getMessageEditor();
            if (null != editor) {
                editor.writeMessage(getProtocol(), getContact(), initText);
            }
        }
    }

    private void markItem(int item) {
        MessData mData = model.getMessage(item);
        mData.setMarked(!mData.isMarked());
        selectMode = hasSelectedItems();
        invalidate();
    }
    private boolean hasSelectedItems() {
        for (int i = 0; i < model.size(); ++i) {
            MessData md = model.getMessage(i);
            if (md.isMarked()) {
                return true;
            }
        }
        return false;
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    private void addTextToHistory() {
        MessData md = getCurrentMsgData();
        if ((null == md) || (null == md.getText())) {
            return;
        }
        if (getContact().hasHistory()) {
            HistoryStorage.getHistory(getContact()).addText(md.getText(), md.isIncoming(), md.getNick(), md.getTime());
        }
    }
    // #sijapp cond.end#

    private MessData getCurrentMsgData() {
        try {
            int messIndex = getCurrItem();
            return (messIndex < 0) ? null : model.getMessage(messIndex);
        } catch (Exception e) {
            return null;
        }
    }

    protected String getCurrentText() {
        MessData md = getCurrentMsgData();
        return (null == md) ? "" : md.getText();
    }

    protected void resetSelected() {
        selectMode = false;
        for (int i = 0; i < model.size(); ++i) {
            model.getMessage(i).setMarked(false);
        }
    }
    private String copySelected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < model.size(); ++i) {
            MessData md = model.getMessage(i);
            if (md.isMarked()) {
                String msg = md.getText();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n");
            }
        }
        return 0 == sb.length() ? null : sb.toString();
    }
    private void copyText() {
        String all = copySelected();
        if (null != all) {
            resetSelected();
            Clipboard.setClipBoardText(all);
            return;
        }
        MessData md = getCurrentMsgData();
        if (null == md) {
            return;
        }
        String msg = md.getText();
        if (md.isMe()) {
            msg = "*" + md.getNick() + " " + msg;
        }
        Clipboard.setClipBoardText(md.isIncoming(), md.getNick(), md.strTime, msg);
    }

    private String getBlogPostId(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        String lastLine = text.substring(text.lastIndexOf('\n') + 1);
        if (0 == lastLine.length()) {
            return null;
        }
        if ('#' != lastLine.charAt(0)) {
            return null;
        }
        int numEnd = lastLine.indexOf(' ');
        if (-1 != numEnd) {
            lastLine = lastLine.substring(0, numEnd);
        }
        return lastLine + " ";
    }

    protected void beforePaint() {
        model.resetUnreadMessages();
        ((Chat)view).updateStatusIcons();
    }
    // #sijapp cond.if modules_TOUCH is "true"#
    protected void touchItemTaped(int item, int x, TouchState state) {
        if (state.isLong || (view.getWidth() - view.minItemHeight < x)) {
            view.showMenu(getContextMenu());
        } else if (selectMode) {
            markItem(item);
        } else if (state.isLong) {
            view.showMenu(getMenu());
        } else if (state.isSecondTap) {
            execJimmAction(NativeCanvas.JIMM_SELECT);
        }

    }
    // #sijapp cond.end#

    protected final boolean doKeyReaction(int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_PRESSED == type) {
            switch (keyCode) {
                case NativeCanvas.CALL_KEY:
                    actionCode = 0;
                    break;

                case NativeCanvas.CLEAR_KEY:
                    execJimmAction(ACTION_DEL_CHAT);
                    return true;
            }
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_LEFT:
                case NativeCanvas.NAVIKEY_RIGHT:
                    Jimm.getJimm().getCL().showNextPrevChat(model, NativeCanvas.NAVIKEY_RIGHT == actionCode);
                    return true;
            }
        }
        if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
            if (CanvasEx.KEY_RELEASED != type) {
                return true;
            }
            if ('5' == keyCode) {
                execJimmAction(NativeCanvas.JIMM_SELECT);
            } else {
                if (Jimm.getJimm().getDisplay().getNativeCanvas().isLongFirePress()) {
                    markItem(getCurrItem());
                } else {
                    writeMessage(null);
                }
            }
            return true;
        }
        if (HotKeys.execHotKey(getProtocol(), getContact(), keyCode, type)) {
            return true;
        }
        return super.doKeyReaction(keyCode, actionCode, type);
    }

}
