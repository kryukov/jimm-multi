package ru.net.jimm;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import org.microemu.android.util.AndroidLoggerAppender;
import org.microemu.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 27.04.13 12:44
 *
 * @author vladimir
 */
public class Environment {
    public static void initLogger() {
        Logger.removeAllAppenders();
        Logger.setLocationEnabled(false);
        Logger.addAppender(new AndroidLoggerAppender());

        System.setOut(new PrintStream(new OutputStream() {

            StringBuilder line = new StringBuilder();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    if (line.length() > 0) {
                        Logger.debug(line.toString());
                        line.setLength(0);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }

        }));

        System.setErr(new PrintStream(new OutputStream() {

            StringBuilder line = new StringBuilder();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    if (line.length() > 0) {
                        Logger.debug(line.toString());
                        line.setLength(0);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }

        }));
    }
    public static void setup(Activity activity) {
        System.setProperty("microedition.platform", "microemu-android");
        System.setProperty("microedition.configuration", "CLDC-1.1");
        System.setProperty("microedition.profiles", "MIDP-2.0");
        System.setProperty("microedition.locale", Locale.getDefault().toString());
        System.setProperty("device.manufacturer", android.os.Build.BRAND);
        System.setProperty("device.model", android.os.Build.MODEL);
        System.setProperty("device.software.version", android.os.Build.VERSION.RELEASE);

        // photo
        if (hasCamera(activity)) {
            System.setProperty("video.snapshot.encodings", "yes");
        }
        if (hasAccelerometer(activity)) {
            System.setProperty("device.accelerometer", "yes");
        }
    }

    private static boolean hasAccelerometer(Activity activity) {
        boolean defValue = false;
        try {
            SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
            if (null == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
                return false;
            }
            defValue = true;
            return  1 == Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
        } catch (Throwable e) {
            return defValue;
        }
    }
    private static boolean hasCamera(Activity activity) {
        try {
            return (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
        } catch (Throwable e) {
            return false;
        }
    }
}
