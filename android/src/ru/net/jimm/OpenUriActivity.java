package ru.net.jimm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import jimm.Jimm;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import protocol.xmpp.Jid;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 30.12.12 3:15
 *
 * @author vladimir
 */
public class OpenUriActivity extends Activity {
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final boolean delay = null == JimmActivity.getInstance();
            final Uri uri = intent.getData();
            startActivity(new Intent(this, JimmActivity.class));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (delay) try {
                        // has not started yet
                        Thread.sleep(5000);
                    } catch (Exception ignored) {
                    }
                    process(uri);
                }
            }).start();
        }
    }


    public boolean process(Uri uri) {
        try {
            String path = uri.toString();
            if (path.startsWith("xmpp")) {
                processXmpp(path.substring("xmpp:".length()));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
    public void processXmpp(String path) {
        String jid = path;
        if (-1 < path.indexOf('?')) {
            jid = path.substring(0, path.indexOf('?'));
        }
        jimm.modules.DebugLog.println("open xmpp " + path + " " + jid);
        Xmpp xmpp = getFirstJabber();
        if (null == xmpp) {
            alert();
            return;
        }
        try {
            Contact c = xmpp.createTempContact(Jid.getBareJid(jid));
            while (xmpp.isConnecting()) {
                try {
                    Thread.sleep(2000);
                } catch (Exception ignored) {
                }
            }
            xmpp.addTempContact(c);
            Jimm.getJimm().getCL().activate(c);
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("uri", e);
        }
    }

    private Xmpp getFirstJabber() {
        for (Protocol p : Jimm.getJimm().jimmModel.getProtocols()) {
            if (p instanceof Xmpp) return (Xmpp) p;
        }
        return null;
    }

    private void alert() {
        AlertDialog alertDialog = new AlertDialog.Builder(JimmActivity.getInstance()).create();
        alertDialog.setTitle(getText(R.string.app_name));
        alertDialog.setMessage(getText(R.string.xmppAccountDontFound));
        alertDialog.show();
    }
}
