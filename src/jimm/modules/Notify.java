/*
 * Notify.java
 *
 * Created on 22 ������ 2007 �., 17:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package jimm.modules;

// #sijapp cond.if modules_ANDROID is "true" #
import ru.net.jimm.sound.SoundPlayer;
// #sijapp cond.else #
import javax.microedition.media.*;
import javax.microedition.media.control.*;
// #sijapp cond.end #
import java.io.InputStream;
import jimm.*;
import jimm.comm.Util;
import jimm.ui.base.Popup;
import jimm.util.*;

/**
 *
 * @author vladimir
 */
// #sijapp cond.if modules_SOUND is "true" #
public class Notify implements Runnable
        // #sijapp cond.if modules_ANDROID isnot "true" #
        , PlayerListener
        // #sijapp cond.end #
        {

    private static Notify _this = new Notify();

    public static Notify getSound() {
        return _this;
    }
    /* Notify notification typs */
    public static final int NOTIFY_MESSAGE = 0;
    public static final int NOTIFY_ONLINE = 1;
    public static final int NOTIFY_TYPING = 2;
    public static final int NOTIFY_MULTIMESSAGE = 3;
    public static final int NOTIFY_ALARM = 4;
    public static final int NOTIFY_RECONNECT = 5;
    public static final int NOTIFY_BLOG = 6;

    private static final int VIBRA_OFF = 0;
    private static final int VIBRA_ON = 1;
    private static final int VIBRA_LOCKED_ONLY = 2;

    private String nextMelody = null;
    private long nextPlayTime = 0;
    private int playingType = 0;

    private static String[] files = {null, null, null, null, null, null, null};
    private static String fileVibrate = null;

    /**
     * Creates a new instance of Notify
     */
    private Notify() {
    }

    private String getMimeType(String ext) {
        if ("mp3".equals(ext)) {
            return "audio/mpeg";
        }
        if ("mid".equals(ext) || "midi".equals(ext)) {
            return "audio/midi";
        }
        if ("amr".equals(ext)) {
            return "audio/amr";
        }
        if ("mmf".equals(ext)) {
            return "audio/mmf";
        }
        if ("imy".equals(ext)) {
            return "audio/iMelody";
        }
        if ("aac".equals(ext)) {
            return "audio/aac";
        }
        if ("m4a".equals(ext)) {
            return "audio/m4a";
        }
        return "audio/X-wav"; // wav
    }

    private void vibrate(int duration) {
        Jimm.getJimm().getDisplay().getNativeDisplay().vibrate(duration);
    }

    private int getVolume() {
        return Options.getInt(Options.OPTION_NOTIFY_VOLUME);
    }

    // Play a sound notification
    private boolean isSoundNotification(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
                return 0 < Options.getInt(Options.OPTION_MESS_NOTIF_MODE);

            case NOTIFY_ONLINE:
                return 0 < Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);

            case NOTIFY_BLOG:
                return Options.getBoolean(Options.OPTION_BLOG_NOTIFY);

            case NOTIFY_MULTIMESSAGE:
                return false;
        }
        return true;
    }

    private boolean isCompulsory(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
            case NOTIFY_MULTIMESSAGE:
            case NOTIFY_ALARM:
            case NOTIFY_BLOG:
                return true;
        }
        return false;
    }

    private void playNotify() {
        int notType = playingType;
        int vibrate = 0;
        if (NOTIFY_ALARM == notType) {
            vibrate = 1500;
        } else {

            int vibraKind = Options.getInt(Options.OPTION_VIBRATOR);
            if ((VIBRA_LOCKED_ONLY == vibraKind) && !Jimm.isLocked()) {
                vibraKind = VIBRA_OFF;
            }
            if ((VIBRA_OFF != vibraKind)
                    && ((NOTIFY_MESSAGE == notType) || (NOTIFY_MULTIMESSAGE == notType))) {
                vibrate = Jimm.isPaused() ? 700 : 200;
            }
        }

        boolean play = !Options.getBoolean(Options.OPTION_SILENT_MODE)
                && isSoundNotification(notType);
        String file = play ? files[notType] : null;

        if (0 < vibrate) {
            // #sijapp cond.if modules_ANDROID isnot "true" #
            if (null != fileVibrate) {
                if (null != file) {
                    nextMelody = file;
                }
                safePlay(fileVibrate);
                return;
            }
            // #sijapp cond.end#
            vibrate(vibrate);
        }
        if (null != file) {
            safePlay(file);

        // #sijapp cond.if modules_ANDROID isnot "true" #
        } else if (play && (NOTIFY_MESSAGE == notType)) {
            try {
                Manager.playTone(ToneControl.C4, 750, getVolume());
            } catch (Exception e) {
            }
        // #sijapp cond.end#
        }

    }

    private void closePlayer() {
        // #sijapp cond.if modules_ANDROID is "true" #
        if (null != androidPlayer) {
            androidPlayer.close();
            androidPlayer = null;
        }
        // #sijapp cond.else #
        closePlayer(j2mePlayer);
        // #sijapp cond.end #
    }
    public void run() {
        try {
            playNotify();
        } catch (OutOfMemoryError err) {
        }
    }
    private void playNotification(int notType) {
        final long now = System.currentTimeMillis();
        long next = nextPlayTime;
        if (!isCompulsory(playingType) && isCompulsory(notType)) {
            next = 0;
        }
        if (now < next) {
            return;
        }
        if (NOTIFY_ALARM == notType) {
            if (!Options.getBoolean(Options.OPTION_ALARM)) {
                return;
            }
            next = now + 3000;

        } else {
            next = now + 2000;
        }
        nextPlayTime = next;
        playingType = notType;
        new Thread(this).start();
    }

    public void playSoundNotification(int notType) {
        synchronized (syncObject) {
            playNotification(notType);
        }
    }

    private final Object syncObject = new Object();
    // #sijapp cond.if modules_ANDROID isnot "true" #
    private Player j2mePlayer;
    // #sijapp cond.else#
    private SoundPlayer androidPlayer;
    // #sijapp cond.end#

    // #sijapp cond.if modules_ANDROID isnot "true" #
    public void playerUpdate(final Player player, final String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event)) {
            closePlayer(player);
            String next = nextMelody;
            if (null != next) {
                nextMelody = null;
                safePlay(next);
            }
        }
    }
    private void closePlayer(final Player p) {
        if (p == j2mePlayer) {
            j2mePlayer = null;
        }
        if (null != p) {
            try {
                p.stop();
            } catch (Exception e) {
            }
            try {
                p.close();
            } catch (Exception e) {
            }
        }
    }
    // #sijapp cond.end #
    private void safePlay(String file) {
        try {
            closePlayer();
            createPlayer(file);
            play();

        } catch (Exception e) {
            closePlayer();

        } catch (OutOfMemoryError err) {
            closePlayer();
        }
    }

    private boolean testSoundFile(String source) {
        try {
            createPlayer(source);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            closePlayer();
        }
    }

    /* Creates player for file 'source' */
    private void createPlayer(String source) throws Exception {
        // #sijapp cond.if modules_ANDROID isnot "true" #
            /* What is file extention? */
        String ext = source.substring(source.lastIndexOf('.') + 1);

        InputStream is = jimm.Jimm.getResourceAsStream(source);
        if (null == is) throw new Exception();
        j2mePlayer = Manager.createPlayer(is, getMimeType(ext));
        j2mePlayer.addPlayerListener(this);
        // #sijapp cond.else #

        androidPlayer = new SoundPlayer();
        androidPlayer.play(source, getVolume());
        // #sijapp cond.end #
    }

    private void play() throws Exception {
        // #sijapp cond.if modules_ANDROID isnot "true" #
        j2mePlayer.realize();
        try {
            int volume = getVolume();
            VolumeControl c = (VolumeControl) j2mePlayer.getControl("VolumeControl");
            if ((null != c) && (0 < volume)) {
                c.setLevel(volume);
            }
        } catch (Exception e) {
        }
        j2mePlayer.prefetch();
        j2mePlayer.start();
        // #sijapp cond.else #
        androidPlayer.start();
        // #sijapp cond.end #
    }

    private String selectSoundType(String name) {
        /* Test other extensions */
        String[] exts = Util.explode("mp3|wav|mid|midi|mmf|amr|imy|aac|m4a", '|');
        for (int i = 0; i < exts.length; ++i) {
            String testFile = name + exts[i];
            if (testSoundFile(testFile)) {
                return testFile;
            }
        }
        return null;
    }

    public void changeSoundMode(boolean showPopup) {
        boolean newValue = !Options.getBoolean(Options.OPTION_SILENT_MODE);
        closePlayer();
        Options.setBoolean(Options.OPTION_SILENT_MODE, newValue);
        Options.safeSave();
        if (showPopup) {
            String text = newValue ? "#sound_is_off" : "#sound_is_on";
            new Popup(JLocale.getString(text)).show();
        }
        vibrate(newValue ? 0 : 100);
    }

    public void initSounds() {
        files[NOTIFY_ONLINE] = selectSoundType("/online.");
        files[NOTIFY_MESSAGE] = selectSoundType("/message.");
        files[NOTIFY_TYPING] = selectSoundType("/typing.");
        files[NOTIFY_ALARM] = selectSoundType("/alarm.");
        files[NOTIFY_RECONNECT] = selectSoundType("/reconnect.");
        files[NOTIFY_BLOG] = selectSoundType("/blog.");
        if (testSoundFile("/vibrate.imy")) {
            fileVibrate = "/vibrate.imy";
        }
    }

    public boolean hasAnySound() {
        for (int i = 0; i < files.length; ++i) {
            if (null != files[i]) {
                return true;
            }
        }
        return false;
    }
}
// #sijapp cond.end #
