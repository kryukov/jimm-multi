package ru.net.jimm.sound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import jimm.modules.fs.FileSystem;
import jimm.modules.fs.JSR75FileSystem;
import protocol.net.TcpSocket;
import ru.net.jimm.JimmActivity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author vladimir
 */
public class SoundPlayer implements MediaPlayer.OnCompletionListener {
    private MediaPlayer androidPlayer;
    private FileInputStream in;

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.release();
    }

    public void close() {
        if (null != androidPlayer) {
            androidPlayer.release();
            androidPlayer = null;
        }
        TcpSocket.close(in);
    }
    public void play(String source, int volume) throws IOException {
        AudioManager audioManager = (AudioManager)JimmActivity.getInstance().getSystemService(Context.AUDIO_SERVICE);

        if (AudioManager.RINGER_MODE_NORMAL == audioManager.getRingerMode()) {
            playIt(source.substring(1), volume);
        } else {
            close();
        }
    }
    private void playIt(String source, int volume) throws IOException {
        androidPlayer = new MediaPlayer();
        try {
            in = (FileInputStream) openFile(source);
            if (null == in) {
                AssetFileDescriptor afd = JimmActivity.getInstance().getAssets().openFd(source);
                androidPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } else {
                androidPlayer.setDataSource(in.getFD());
                TcpSocket.close(in);
            }
            androidPlayer.prepare();
            androidPlayer.setVolume(volume / 100f, volume / 100f);
        } catch (IOException e) {
            close();
            throw e;
        }
    }
    public static InputStream openFile(String file) {
        JSR75FileSystem fs = FileSystem.getInstance();
        InputStream in = null;
        try {
            fs.openFile(FileSystem.getJimmHome() + FileSystem.RES + "/" + file);
            in = fs.openInputStream();
        } catch (Exception ignored) {
        }
        fs.close();
        return in;
    }

    public void start() {
        if (null != androidPlayer) {
            androidPlayer.setOnCompletionListener(this);
            androidPlayer.start();
        }
    }
}
