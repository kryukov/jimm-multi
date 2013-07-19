/*
 * IBBFileTransfer.java
 *
 * Created on 20 Январь 2010 г., 13:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.jabber;

// #sijapp cond.if protocols_JABBER is "true" #
// #sijapp cond.if modules_FILES is "true"#
import jimm.*;
import jimm.comm.*;


/**
 *
 * @author Vladimir Krukov
 */
public class IBBFileTransfer {
    private String to;
    private String sid;

    private int blockIndex;
    private static final int blockSize = 4096;

    private String fileName;
    private String fileDesc;
    private FileTransfer ft;

    public IBBFileTransfer(String name, String desc, FileTransfer ft) {
        this.fileName = name;
        this.fileDesc = desc;
        this.ft = ft;
        JabberContact c = (JabberContact)ft.getReceiver();
        this.to = c.getUserId();
        if (!(c instanceof JabberServiceContact)) {
            String resource = c.getCurrentSubContact().resource;
            this.to += '/' + resource;
        }
        this.sid = Util.xmlEscape("Jimm" + Util.uniqueValue());
    }
    public void setProgress(int percent) {
        ft.setProgress(percent);
    }
    public boolean isCanceled() {
        return ft.isCanceled();
    }
    public void destroy() {
        ft.destroy();
        ft = null;
    }

    public String initTransfer() {
        return "<iq id='jimmibb_open' to='"
                + Util.xmlEscape(to)
                + "' type='set'><open xmlns='http://jabber.org/protocol/ibb' block-size='"
                + blockSize + "' sid='" + sid + "' stanza='iq'/></iq>";
    }

    private byte[] readNextBlock() {
        int size = Math.min(blockSize, ft.getFileSize() - blockIndex * blockSize);
        if (size < 0) {
            return null;
        }
        try {
            byte[] data = new byte[size];
            ft.getFileIS().read(data);
            return data;
        } catch (Exception ex) {
            return null;
        }
    }
    public String nextBlock() {
        byte[] data = readNextBlock();
        if (null == data) {
            return null;
        }
        String xml = "<iq id='jimmibb_" + blockIndex + "' to='" + Util.xmlEscape(to)
                + "' type='set'><data xmlns='http://jabber.org/protocol/ibb' seq='"
                + blockIndex + "' sid='" + sid + "'>"
                + Util.xmlEscape(Util.base64encode(data)) + "</data></iq>";
        blockIndex++;
        return xml;
    }
    public int getPercent() {
        return 10 + (blockIndex * blockSize * 90 / ft.getFileSize());
    }
    public String close() {
        return "<iq id='jimmibb_close' to='" + Util.xmlEscape(to)
                + "' type='set'><close xmlns='http://jabber.org/protocol/ibb' sid='"
                + sid + "'/></iq>";
    }

    public String getRequest() {
        return"<iq type='set' id='jimmibb_si' to='"
                + Util.xmlEscape(to) + "'><si xmlns='http://jabber.org/protocol/si' id='"
                + sid + "' "
                + "mime-type='application/octet-stream' "
                + "profile='http://jabber.org/protocol/si/profile/file-transfer'>"
                + "<file xmlns='http://jabber.org/protocol/si/profile/file-transfer' "
                + "name='" + Util.xmlEscape(fileName) + "' "
                + "size='" + ft.getFileSize() + "' >"
                + "<desc>" + Util.xmlEscape(fileDesc) + "</desc>"
                + "</file>"
                + "<feature xmlns='http://jabber.org/protocol/feature-neg'>"
                + "<x xmlns='jabber:x:data' type='form'>"
                + "<field var='stream-method' type='list-single'>"
                + "<option><value>http://jabber.org/protocol/ibb</value></option>"
                + "</field></x></feature></si></iq>";
    }
}
// #sijapp cond.end#
// #sijapp cond.end#
