/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/FileTransfer.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Dmitry Tunin
 *******************************************************************************/

// #sijapp cond.if modules_FILES="true"#
package jimm;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.Image;

import jimm.chat.Chat;
import jimm.chat.MessData;
import jimm.comm.*;
import jimm.modules.*;
import jimm.cl.*;
// #sijapp cond.if modules_FILES="true"#
import jimm.modules.fs.*;
// #sijapp cond.end#
import jimm.modules.photo.*;
import jimm.ui.form.*;
import jimm.util.JLocale;
import protocol.Contact;
import protocol.Protocol;
import protocol.net.TcpSocket;

public final class FileTransfer implements FormListener, FileBrowserListener,
        PhotoListener, Runnable {

    // Form for entering the name and description
    private GraphForm name_Desc;

    // File data
    private String filename;
    private String description;
    private int sendMode;
    private InputStream fis;
    private int fsize;

    // File path and description TextField
    private static final int descriptionField = 1000;
    private static final int  transferMode = 1001;

    private MessData progressInstance;
    private boolean canceled = false;

    private Protocol protocol;
    private Contact cItem;
    private Chat chat;

    // #sijapp cond.if modules_ANDROID isnot "true" #
    private ViewFinder vf;
    // #sijapp cond.end #
    private JSR75FileSystem file;

    // Constructor
    public FileTransfer(Protocol p, Contact _cItem) {
        protocol = p;
        cItem = _cItem;
    }

    // Return the cItem belonging to this FileTransfer
    public Contact getReceiver() {
        return cItem;
    }

    // Set the file data
    private void setData(InputStream is, int size) {
        fis = is;
        fsize = size;
    }
    public InputStream getFileIS() {
        return fis;
    }
    public int getFileSize() {
        return fsize;
    }

    // Start the file transfer procedure depening on the ft type
    public void startFileTransfer() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (ru.net.jimm.JimmActivity.getInstance().pickFile(this)) {
            return;
        }
        // #sijapp cond.end #
        FileBrowser fsBrowser = new FileBrowser(false);
        fsBrowser.setListener(this);
        fsBrowser.activate();
    }
    public static boolean isPhotoSupported() {
        String supports = System.getProperty("video.snapshot.encodings");
        return !StringConvertor.isEmpty(supports);
    }
    public void startPhotoTransfer() {
        // #sijapp cond.if modules_ANDROID isnot "true" #
        vf = new ViewFinder();
        vf.setPhotoListener(this);
        vf.show();
        // #sijapp cond.else #
        ru.net.jimm.JimmActivity.getInstance().startCamera(this, 1024, 768);
        // #sijapp cond.end #
    }

    // #sijapp cond.if modules_ANDROID is "true" #
    public void onFileSelect(InputStream in, String fileName) {
        try {
            setFileName(fileName);
            int fileSize = in.available();
            setData(in, fileSize);
            askForNameDesc();
        } catch (Exception e) {
            closeFile();
            handleException(new JimmException(191, 6));
        }
    }
        // #sijapp cond.end #
    public void onFileSelect(String filename) throws JimmException {
        file = FileSystem.getInstance();
        try {
            file.openFile(filename);
            setFileName(file.getName());
            // FIXME resource leak
            InputStream is = file.openInputStream();
            int fileSize = (int)file.fileSize();
            byte[] image = null;
            // #sijapp cond.if modules_ANDROID is "true" #
            if ((fileSize < MAX_IMAGE_SIZE) && isImageFile()) {
                image = FileSystem.getInstance().getFileContent(filename);
            }
            // #sijapp cond.end #
            setData(is, fileSize);
            askForNameDesc();
            showPreview(image);

        } catch (Exception e) {
            closeFile();
            throw new JimmException(191, 3);
        }
    }

    //
    public void onDirectorySelect(String s0) {}

    /* Helpers for options UI: */
    private void askForNameDesc() {
        name_Desc = new GraphForm("name_desc", "ok", "back", this);
        name_Desc.addString("filename", filename);
        name_Desc.addTextField(descriptionField, "description", "", 255);
        String items = "jimm.net.ru|jimm.org";
        // #sijapp cond.if protocols_JABBER is "true" #
        if (cItem instanceof protocol.jabber.JabberContact) {
            if (cItem.isSingleUserContact() && cItem.isOnline()) {
                items += "|ibb";
            }
        }
        // #sijapp cond.end #
        name_Desc.addSelector(transferMode, "send_via", items, 0);
        name_Desc.addString(JLocale.getString("size") + ": ", String.valueOf(getFileSize()/1024)+" kb");
        // #sijapp cond.if modules_TRAFFIC is "true" #
        int cost = Traffic.calcCost(getFileSize());
        if (0 < cost) {
            name_Desc.addString(JLocale.getString("cost") + ": ",
                    Traffic.costToString(cost));
        }
        // #sijapp cond.end #
        name_Desc.show();
    }

    // Command listener
    public void formAction(GraphForm form, boolean apply) {
        if (apply) {
            description = name_Desc.getTextFieldValue(descriptionField);
            sendMode = name_Desc.getSelectorValue(transferMode);
            addProgress();
            if (name_Desc.getSelectorValue(transferMode) == 2) {
                try {
                    protocol.sendFile(this, filename, description);
                } catch (Exception ignored) {
                }

            } else {
                setProgress(0);
                new Thread(this).start();
            }
        } else {
            destroy();
            ContactList.getInstance().activate();
        }
    }

    public boolean is(MessData par) {
        return (progressInstance == par);
    }
    private String getProgressText() {
        return filename + " - " + StringConvertor.bytesToSizeString(getFileSize(), false);
    }
    private void changeFileProgress(String message) {
        if (cItem.hasChat()) {
            chat.changeFileProgress(progressInstance,
                    JLocale.getEllipsisString("sending_file"),
                    getProgressText() + "\n"
                    + JLocale.getString(message));
        }
    }

    public void cancel() {
        canceled = true;
        changeFileProgress("canceled");
        if (0 < sendMode) {
            closeFile();
        }
    }
    public boolean isCanceled() {
        return canceled;
    }
    private void addProgress() {
        chat = protocol.getChat(cItem);
        progressInstance = chat.addFileProgress(JLocale.getEllipsisString("sending_file"), getProgressText());
        chat.activate();
        ContactList.getInstance().addTransfer(this);
    }
    public void setProgress(int percent) {
        try {
            _setProgress(percent);
        } catch (Exception ignored) {
        }
    }
    public void _setProgress(int percent) {
        if (isCanceled()) {
            return;
        }
        if (-1 == percent) {
            percent = 100;
        }
        if ((0 == percent) && (null == progressInstance)) {
            return;
        }
        if (100 == percent) {
            ContactList.getInstance().removeTransfer(progressInstance, false);
            changeFileProgress("complete");
            return;
        }
        progressInstance.par.setProgress((byte)percent);
        if (cItem.hasChat()) {
            chat.invalidate();
        }
    }
    private void handleException(JimmException e) {
        destroy();
        if (isCanceled()) {
            return;
        }
        if (null != progressInstance) {
            changeFileProgress(JLocale.getString("error") + "\n" + e.getMessage());
        }
    }

    private void closeFile() {
        if (null != file) {
            file.close();
            file = null;
        }
        TcpSocket.close(fis);
        fis = null;
    }
    public void destroy() {
        try {
            closeFile();
            ContactList.getInstance().removeTransfer(progressInstance, false);
            // #sijapp cond.if modules_ANDROID isnot "true" #
            if (null != vf) {
                vf.dismiss();
                vf = null;
            }
            // #sijapp cond.end #
            name_Desc.clearForm();
            Jimm.gc();
        } catch (Exception ignored) {
        }
    }

    /** ************************************************************************* */
    public void run() {
        try {
            InputStream in = getFileIS();
            int size = getFileSize();
            switch (sendMode) {
                case 0:
                    sendFileThroughServer(in, size);
                    break;
                case 1:
                    sendFileThroughWeb(in, size);
                    break;
            }

        } catch(JimmException e) {
            handleException(e);

        } catch(Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("FileTransfer.run", e);
            // #sijapp cond.end#
            handleException(new JimmException(194, 2));
        }
        destroy();
    }
    public void processPhoto(final byte[] data) {
        setData(new ByteArrayInputStream(data), data.length);
        String timestamp = Util.getLocalDateString(Jimm.getCurrentGmtTime(), false);
        String photoName = "photo-"
                + timestamp.replace('.', '-').replace(' ', '-')
                + ".jpg";
        setFileName(photoName);
        askForNameDesc();
        showPreview(data);
    }
    private void showPreview(final byte[] image) {
        // #sijapp cond.if modules_ANDROID is "true" #
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Image img = Image.createImage(image, 0, image.length);
                    img = Util.createThumbnail(img, name_Desc.getScreenWidth(),
                            name_Desc.getScreenHeight());
                    name_Desc.addImage(img);
                } catch (Throwable ignored) {
                    // IOException or OutOfMemoryError
                }
            }
        }).start();
        // #sijapp cond.end #
    }

    private void setFileName(String name) {
        // Windows fix
        name = name.replace(':', '.');
        name = name.replace('/', '_');
        name = name.replace('\\', '_');
        name = name.replace('%', '_');
        filename = name;
    }

    private String getTransferClient() {
        String client = "jimm";
        // #sijapp cond.if modules_MULTI is "true" #
        client = "jimm-multi";
        // #sijapp cond.elseif protocols_ICQ is "true" #
        client = "jimm-icq";
        // #sijapp cond.elseif protocols_MRIM is "true" #
        client = "jimm-mrim";
        // #sijapp cond.elseif protocols_JABBER is "true" #
        client = "jimm-jabber";
        // #sijapp cond.end #
        return client;
    }
    private void sendFileThroughServer(InputStream fis, int fileSize) throws JimmException {
        TcpSocket socket = new TcpSocket();
        try {
            socket.connectTo("socket://files.jimm.net.ru:2000");

            final int version = 1;
            Util header = new Util();
            header.writeWordBE(version);
            header.writeLenAndUtf8String(filename);
            header.writeLenAndUtf8String(description);
            header.writeLenAndUtf8String(getTransferClient());
            header.writeDWordBE(fileSize);
            socket.write(header.toByteArray());
            socket.flush();

            byte[] buffer = new byte[4*1024];
            int counter = fileSize;
            while (counter > 0) {
                int read = fis.read(buffer);
                socket.write(buffer, 0, read);
                counter -= read;
                if (fileSize != 0) {
                    if (isCanceled()) {
                        throw new JimmException(194, 1);
                    }
                    socket.flush();
                    setProgress((100 - 2) * (fileSize - counter) / fileSize);
                }
            }
            socket.flush();

            int length = socket.read();
            if (-1 == length) {
                throw new JimmException(120, 13);
            }
            socket.read(buffer, 0, length);
            String url = StringConvertor.utf8beByteArrayToString(buffer, 0, length);

            if (isCanceled()) {
                throw new JimmException(194, 1);
            }
            // Send info about file
            StringBuffer messText = new StringBuffer();
            if (!StringConvertor.isEmpty(description)) {
                messText.append(description).append("\n");
            }
            messText.append("File: ").append(filename).append("\n");
            messText.append("Size: ")
                    .append(StringConvertor.bytesToSizeString(fileSize, false))
                    .append("\n");
            messText.append("Link: ").append(url);

            protocol.sendMessage(cItem, messText.toString(), true);
            setProgress(100);
            socket.close();

        } catch (JimmException e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("send file", e);
            // #sijapp cond.end#
            socket.close();
            throw e;

        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("send file", e);
            // #sijapp cond.end#
            socket.close();
            throw new JimmException(194, 0);
        }
    }
    private void sendFileThroughWeb(InputStream fis, int fsize) throws JimmException {
        InputStream is = null;
        OutputStream os = null;
        HttpConnection sc = null;

        String host = "filetransfer.jimm.org";
        final String url = "http://" + host + "/__receive_file.php";
        try {
            sc = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
            sc.setRequestMethod(HttpConnection.POST);

            String boundary = "a9f843c9b8a736e53c40f598d434d283e4d9ff72";

            sc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            os = sc.openOutputStream();

            // Send post header
            StringBuffer headers = new StringBuffer();
            headers.append("--").append(boundary).append("\r\n");
            headers.append("Content-Disposition: form-data; name=\"filedesc\"\r\n");
            headers.append("\r\n");
            headers.append(description);
            headers.append("\r\n");
            headers.append("--").append(boundary).append("\r\n");
            headers.append("Content-Disposition: form-data; name=\"jimmfile\"; filename=\"");
            headers.append(filename).append("\"\r\n");
            headers.append("Content-Type: application/octet-stream\r\n");
            headers.append("Content-Transfer-Encoding: binary\r\n");
            headers.append("\r\n");
            os.write(StringConvertor.stringToByteArrayUtf8(headers.toString()));

            // Send file data and show progress
            byte[] buffer = new byte[1024*2];
            int counter = fsize;
            while (counter > 0) {
                int read = fis.read(buffer);
                os.write(buffer, 0, read);
                counter -= read;
                if (fsize != 0) {
                    if (isCanceled()) {
                        throw new JimmException(194, 1);
                    }
                    setProgress((100 - 2) * (fsize - counter) / fsize);
                }
            }

            // Send end of header
            String end = "\r\n--" + boundary + "--\r\n";
            os.write(StringConvertor.stringToByteArrayUtf8(end));

            // Read response
            is = sc.openInputStream();

            int respCode = sc.getResponseCode();
            if (HttpConnection.HTTP_OK != respCode) {
                throw new JimmException(194, respCode);
            }

            StringBuffer response = new StringBuffer();
            for (;;) {
                int read = is.read();
                if (read == -1) break;
                response.append((char)(read & 0xFF));
            }

            String respString = response.toString();

            int dataPos = respString.indexOf("http://");
            if (-1 == dataPos) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.println("server say '" + respString + "'");
                // #sijapp cond.end#
                throw new JimmException(194, 1);
            }

            if (isCanceled()) {
                throw new JimmException(194, 1);
            }
            respString = Util.replace(respString, "\r", "");
            respString = Util.replace(respString, "\n", "");

            // Send info about file
            StringBuffer messText = new StringBuffer();
            if (!StringConvertor.isEmpty(description)) {
                messText.append(description).append("\n");
            }
            messText.append("File: ").append(filename).append("\n");
            messText.append("Size: ")
                    .append(StringConvertor.bytesToSizeString(fsize, false))
                    .append("\n");
            messText.append("Link: ").append(respString);

            protocol.sendMessage(cItem, messText.toString(), true);
            setProgress(100);
            TcpSocket.close(sc);
            TcpSocket.close(os);
            TcpSocket.close(is);

        } catch (IOException e) {
            TcpSocket.close(sc);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("send file", e);
            // #sijapp cond.end#
            throw new JimmException(194, 0);
        }

    }

    private static final int MAX_IMAGE_SIZE = 2*1024*1024;
    private boolean isImageFile() {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png");
    }
}
// #sijapp cond.end#