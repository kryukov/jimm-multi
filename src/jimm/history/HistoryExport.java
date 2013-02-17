package jimm.history;

// #sijapp cond.if modules_HISTORY is "true" #
// #sijapp cond.if modules_FILES="true"#
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.modules.Notify;
import jimm.modules.fs.*;
import jimm.util.JLocale;
import protocol.*;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 12.08.12 19:08
 *
 * @author vladimir
 */
class HistoryExport implements Runnable, FileBrowserListener {
    private HistoryStorage exportHistory;
    private String directory;

    private HistoryStorageList screen;
    String contact;
    int currentMessage;
    int messageCount;

    public HistoryExport(HistoryStorageList screen) {
        this.screen = screen;
    }

    public void export(HistoryStorage storage) {
        exportHistory = storage;
        FileBrowser fsBrowser = new FileBrowser(true);
        fsBrowser.setListener(this);
        fsBrowser.activate();
    }

    public void onFileSelect(String s0) throws JimmException {
    }

    public void onDirectorySelect(String dir) {
        directory = dir;
        screen.restore();
        new Thread(this).start();
    }
    private void setProgress(int messageNum) {
        currentMessage = messageNum;
        screen.invalidate();
    }

    public void run() {
        try {
            exportContact(exportHistory);
            // #sijapp cond.if modules_SOUND is "true" #
            Notify.getSound().playSoundNotification(Notify.NOTIFY_MESSAGE);
            // #sijapp cond.end #
            screen.exportDone();
        } catch (Exception ex) {
            JimmException e = new JimmException(191, 5);
            if (ex instanceof JimmException) {
                e = (JimmException) ex;
            }
            ContactList.getInstance().activate(e.getMessage());
        }
    }

    private void write(OutputStream os, String val) throws IOException {
        os.write(StringConvertor.stringToByteArrayUtf8(val));
    }
    private void exportUinToStream(HistoryStorage storage, OutputStream os) throws IOException {
        messageCount = storage.getHistorySize();
        if (0 == messageCount) {
            return;
        }
        os.write(new byte[] { (byte) 0xef, (byte) 0xbb, (byte) 0xbf });

        Contact c = storage.getContact();
        String userId = c.getUserId();
        String nick = (c.getName().length() > 0) ? c.getName() : userId;
        contact = nick;
        setProgress(0);
        write(os, "\r\n\t" + JLocale.getString("message_history_with")
                + nick + " (" + userId + ")\r\n\t"
                + JLocale.getString("export_date")
                + Util.getLocalDateString(Jimm.getCurrentGmtTime(), false)
                + "\r\n\r\n");

        String me = JLocale.getString("me");
        int guiStep = Math.max(messageCount / 100, 1) * 5;
        for (int i = 0, curStep = 0; i < messageCount; ++i) {
            CachedRecord record = storage.getRecord(i);
            write(os, " " + ((record.type == 0) ? nick : me)
                    + " (" + record.date + "):\r\n");
            write(os, StringConvertor.restoreCrLf(record.text) + "\r\n");
            curStep++;
            if (curStep > guiStep) {
                os.flush();
                setProgress(i);
                curStep = 0;
            }
        }
        setProgress(messageCount);
        os.flush();
    }

    private JSR75FileSystem openFile(String userId) throws JimmException {
        String timemark = Util.getDate("_%y%m", Util.createCurrentLocalTime());
        for (int counter = 0; counter < 1000; ++counter) {
            StringBuffer sb = new StringBuffer();
            sb.append(directory).append("hist_").append(userId).append(timemark);
            if (0 < counter) {
                sb.append("_").append(counter);
            }
            sb.append(".txt");
            JSR75FileSystem file = FileSystem.getInstance();
            file.openFile(sb.toString());
            if (!file.exists()) {
                return file;
            }
            file.close();
        }
        return null;
    }
    private void exportContact(HistoryStorage storage) throws Exception {

        storage.openHistory();
        try {
            if (0 < storage.getHistorySize()) {
                JSR75FileSystem file = openFile(storage.getUniqueUserId());
                OutputStream out = file.openOutputStream();
                try {
                    exportUinToStream(storage, out);
                } finally {
                    out.close();
                    file.close();
                }
            }
        } finally {
            storage.closeHistory();
        }
    }
}
// #sijapp cond.end#
// #sijapp cond.end#
