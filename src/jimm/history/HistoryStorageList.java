/*
 * HistoryStorageList.java
 *
 * Created on 1 Май 2007 г., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.history;

// #sijapp cond.if modules_HISTORY is "true" #

import jimm.ui.text.TextList;
import jimm.ui.text.TextListModel;
import DrawControls.text.*;
import java.util.*;
import java.io.*;
import javax.microedition.rms.*;
import jimm.*;
import jimm.cl.*;
// #sijapp cond.if modules_FILES="true"#
import jimm.modules.fs.*;
// #sijapp cond.end#
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.form.*;
import jimm.ui.menu.*;
import jimm.modules.*;
import jimm.util.JLocale;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import jimm.comm.*;
import jimm.ui.text.TextListController;
import protocol.Contact;

// Visual messages history list
public final class HistoryStorageList extends ScrollableArea implements
        Runnable, FormListener, TextListExCommands {

    // list UIN
    private HistoryStorage history;

    // Controls for finding text
    private GraphForm frmFind;
    private static final int tfldFind = 1000;
    private static final int chsFind = 1010;
    private static final int NOT_FOUND = 1;

    private static final int CACHE_SIZE = 50;
    private Hashtable cachedRecords = new Hashtable();
    private Thread searching = null;

    private MenuModel msgMenu = new MenuModel();
    private TextList msg = new TextList(null);

    // #sijapp cond.if modules_FILES="true"#
    private HistoryExport export = null;
    // #sijapp cond.end#

    // Constructor
    public HistoryStorageList(HistoryStorage storage) {
        super(JLocale.getString("history"));
        history = storage;
        history.openHistory();

        int size = getSize();
        if (0 != size) {
            setCurrentItemIndex(size - 1);
        }
    }

    void closeHistoryView() {
        clearCache();
        history.closeHistoryView();
        searching = null;
    }

    public int getHistorySize() {
        return history.getHistorySize();
    }
    private CachedRecord getCachedRecord(int num) {
        Integer key = new Integer(num);
        CachedRecord cachedRec = (CachedRecord)cachedRecords.get(key);
        if (null == cachedRec) {
            trimCache();
            cachedRec = history.getRecord(num);
            if (null != cachedRec) {
                cachedRecords.put(key, cachedRec);
            }
        }
        return cachedRec;
    }
    private void trimCache() {
        if (cachedRecords.size() > CACHE_SIZE) {
            cachedRecords.clear();
        }
    }
    // Clears cache before hiding history list
    private void clearCache() {
        cachedRecords.clear();
        Jimm.gc();
    }

    protected void onCursorMove() {
        CachedRecord record = getCachedRecord(getCurrItem());
        if (null != record) {
            setCaption(record.from + " " + record.date);
        }
    }
    protected void doJimmAction(int action) {
        switch (action) {
            case NativeCanvas.JIMM_SELECT:
                showMessText().show();
                return;

            case NativeCanvas.JIMM_BACK:
                back();
                closeHistoryView();
                return;

            case NativeCanvas.JIMM_MENU:
                showMenu(getMenu());
                return;
        }
        switch (action) {
            case MENU_GOTO_URL:
                ContactList.getInstance().gotoUrl(getCachedRecord(getCurrItem()).text);
                break;

            case MENU_SELECT:
                showMessText().show();
                break;

            case MENU_FIND:
                if (null == frmFind) {
                    frmFind = new GraphForm("find", "find", "back", this);
                    frmFind.addTextField(tfldFind, "text_to_find", "", 64);
                    frmFind.addCheckBox(chsFind + 0, "find_backwards", true);
                    frmFind.addCheckBox(chsFind + 1, "find_case_sensitiv", false);
                }
                frmFind.remove(NOT_FOUND);
                frmFind.show();
                break;

            case MENU_CLEAR:
                MenuModel menu = new MenuModel();
                menu.addItem("currect_contact",         MENU_DEL_CURRENT);
                menu.addItem("all_contact_except_this", MENU_DEL_ALL_EXCEPT_CUR);
                menu.addItem("all_contacts",            MENU_DEL_ALL);
                menu.setActionListener(new Binder(this));
                showMenu(menu);
                break;

            case MENU_DEL_CURRENT:
                history.removeHistory();
                clearCache();
                restore();
                break;

            case MENU_DEL_ALL_EXCEPT_CUR:
                history.clearAll(true);
                restore();
                break;

            case MENU_DEL_ALL:
                history.clearAll(false);
                clearCache();
                restore();
                break;

            case MENU_COPY_TEXT:
                int index = getCurrItem();
                if (-1 == index) return;
                CachedRecord record = getCachedRecord(index);
                if (null == record) return;
                JimmUI.setClipBoardText((record.type == 0),
                        record.from, record.date, record.text);
                restore();
                break;

            case MENU_INFO:
                RecordStore rs = history.getRS();
                try {
                    String sb = JLocale.getString("hist_cur") + ": " + getSize()  + "\n"
                            + JLocale.getString("hist_size") + ": " + (rs.getSize() / 1024) + "\n"
                            + JLocale.getString("hist_avail") + ": " + (rs.getSizeAvailable() / 1024) + "\n";
                    new Popup(this, sb).show();
                } catch (Exception e) {
                }
                break;

            // #sijapp cond.if modules_FILES="true"#
            case MENU_EXPORT:
                export = new HistoryExport(this);
                export.export(history);
                break;

            case MENU_EXPORT_ALL:
                export = new HistoryExport(this);
                export.export(null);
                break;
            // #sijapp cond.end#
        }
    }

    public void onContentMove(TextListModel sender, int direction) {
        moveInList(direction);
    }

    private static final int MENU_SELECT     = 0;
    private static final int MENU_FIND       = 1;
    private static final int MENU_CLEAR      = 2;
    private static final int MENU_DEL_CURRENT        = 40;
    private static final int MENU_DEL_ALL_EXCEPT_CUR = 41;
    private static final int MENU_DEL_ALL            = 42;
    private static final int MENU_COPY_TEXT  = 3;
    private static final int MENU_INFO       = 4;
    private static final int MENU_EXPORT     = 5;
    private static final int MENU_EXPORT_ALL = 6;
    private static final int MENU_GOTO_URL   = 7;

    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (getSize() > 0) {
            menu.addItem("select",       MENU_SELECT);
            menu.addEllipsisItem("find",  MENU_FIND);
            menu.addEllipsisItem("clear", MENU_CLEAR);
            menu.addItem("copy_text",    MENU_COPY_TEXT);
            menu.addItem("history_info", MENU_INFO);
            // #sijapp cond.if modules_FILES="true"#
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addItem("export",       MENU_EXPORT);
                //menu.addItem("exportall",    MENU_EXPORT_ALL);
            }
            // #sijapp cond.end#
        }
        menu.setActionListener(new Binder(this));
        return menu;
    }

    // Moves on next/previous message in list and shows message text
    private void moveInList(int offset) {
        setCurrentItemIndex(getCurrItem() + offset);
        showMessText().restore();
    }
    public void run() {
        Thread it = Thread.currentThread();
        searching = it;
        // search
        String text = frmFind.getTextFieldValue(tfldFind);
        int textIndex = find(text, getCurrItem(),
                frmFind.getCheckBoxValue(chsFind + 1),
                frmFind.getCheckBoxValue(chsFind + 0));

        if (0 <= textIndex) {
            setCurrentItemIndex(textIndex);
            restore();

        } else if (searching == it) {
            frmFind.addString(NOT_FOUND, text + "\n" + JLocale.getString("not_found"));
        }
    }

    private int find(String text, int fromIndex, boolean caseSens, boolean back) {
        Thread it = Thread.currentThread();
        int size = history.getHistorySize();
        if ((fromIndex < 0) || (fromIndex >= size)) return -1;
        if (!caseSens) text = StringConvertor.toLowerCase(text);

        int step = back ? -1 : +1;
        int updater = 100;
        for (int index = fromIndex; ; index += step) {
            if ((index < 0) || (index >= size)) break;
            CachedRecord record = history.getRecord(index);
            String searchText = caseSens
                    ? record.text
                    : StringConvertor.toLowerCase(record.text);
            if (searchText.indexOf(text) != -1) {
                return index;
            }

            if (0 != updater) {
                updater--;
            } else {
                updater = 100;
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                if (it != searching) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void formAction(GraphForm form, boolean apply) {
        if (apply) {
            frmFind.remove(NOT_FOUND);
            new Thread(this).start();

        } else {
            searching = null;
            restore();
        }
    }

    // Show text message of current message of messages list
    private TextList showMessText() {
        if (getCurrItem() >= getSize()) return null;
        CachedRecord record = history.getRecord(getCurrItem());
//        // #sijapp cond.if modules_TOUCH is "true"#
//        if ((null != current) && (getCurrItem() == current.index)) {
//            current = null;
//            invalidate();
//            return;
//        }
//        final int width = NativeCanvas.getInstance().getMinScreenMetrics()
//                - scrollerWidth - 3;
//        Parser parser = new Parser(getCurrItem(), getFontSet(), width);
//        parser.addText(record.from + " ", THEME_TEXT, FONT_STYLE_BOLD);
//        parser.addText(record.date + ":", THEME_TEXT, FONT_STYLE_BOLD);
//        parser.doCRLF();
//        parser.addTextWithSmiles(record.text, THEME_TEXT, FONT_STYLE_PLAIN);
//        current = parser.getPar();
//        invalidate();
//        if (true) return;
//        // #sijapp cond.end#

        msg.setCaption(record.from);
        msg.setUpdateListener(this);

        msgMenu.clean();
        msgMenu.addItem("copy_text", MENU_COPY_TEXT);
        if (record.containsUrl()) {
            msgMenu.addItem("goto_url",  MENU_GOTO_URL);
        }
        msgMenu.setActionListener(new Binder(this));


        msg.lock();
        msg.setAllToTop();
        TextListModel msgText = new TextListModel();
        Parser parser = msgText.createNewParser(false);
        parser.addText(record.date + ":", THEME_TEXT, FONT_STYLE_BOLD);
        parser.doCRLF();
        parser.addTextWithSmiles(record.text, THEME_TEXT, FONT_STYLE_PLAIN);
        msgText.addPar(parser);

        msg.setController(new TextListController(msgMenu, -1));
        msg.setModel(msgText);
        return msg;
    }

    // returns size of messages history list
    protected final int getSize() {
        return getHistorySize();
    }

    protected void paint(GraphicsEx g) {
        super.paint(g);
        // #sijapp cond.if modules_FILES="true"#
        HistoryExport he = export;
        if (null != he) {
            int progressHeight = getDefaultFont().getHeight();
            int y = (getClientHeight() - 2 * progressHeight) / 2;
            int w = getScreenWidth();
            g.setClip(0, 0, getScreenWidth(), getScreenHeight());
            g.setThemeColor(CanvasEx.THEME_BACKGROUND);
            g.fillRect(0, y - 1, w, progressHeight * 2 + 1);
            g.setThemeColor(CanvasEx.THEME_TEXT);
            g.drawRect(0, y - 1, w, progressHeight * 2 + 1);
            g.setFont(getDefaultFont());
            g.drawString(he.contact, 2, y, w - 4, progressHeight);
            g.fillRect(0, y + progressHeight, w * he.currentMessage / he.messageCount, progressHeight);
        }
        // #sijapp cond.end#
    }
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        CachedRecord record = getCachedRecord(index);
        if ((null == record) || (null == record.getShortText())) return;
//        if ((null != current) && (index == current.index)) {
//            current.paint(getFontSet(), g, x1, y1, skip, to);
//            return;
//        }
        Font font = getDefaultFont();
        g.setFont(font);
        g.setThemeColor((record.type == 0) ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG);
        g.drawString(record.getShortText(), x1, y1 + (h - font.getHeight()) / 2,
                Graphics.TOP | Graphics.LEFT);
    }

    protected int getItemHeight(int itemIndex) {
//        if ((null != current) && (itemIndex == current.index)) {
//            return Math.max(CanvasEx.minItemHeight, current.getHeight());
//        }
        return Math.max(CanvasEx.minItemHeight, getDefaultFont().getHeight());
    }


    public void select(Select select, MenuModel model, int cmd) {
    }
    // #sijapp cond.if modules_FILES="true"#
    void exportDone() {
        export = null;
        invalidate();
    }
    // #sijapp cond.end#
}
// #sijapp cond.end#