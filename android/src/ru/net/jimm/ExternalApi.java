package ru.net.jimm;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import jimm.FileTransfer;
import jimm.history.HistoryStorage;
import jimm.modules.photo.PhotoListener;
import org.microemu.android.util.ActivityResultListener;
import protocol.net.TcpSocket;
import ru.net.jimm.photo.CameraActivity;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 03.04.13 15:30
 *
 * @author vladimir
 */
public class ExternalApi implements ActivityResultListener {
    private JimmActivity activity;

    public void setActivity(JimmActivity activity) {
        this.activity = activity;
    }

    private PhotoListener photoListener = null;
    private FileTransfer fileTransferListener = null;
    private Uri imageUrl = null;
    private static final int RESULT_PHOTO = JimmActivity.RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = JimmActivity.RESULT_FIRST_USER + 2;
    private static final int RESULT_EXTERNAL_FILE = JimmActivity.RESULT_FIRST_USER + 3;

    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
                imageUrl = Uri.fromFile(getOutputMediaFile());
                extCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUrl);
                startActivityForResult(extCameraIntent, RESULT_EXTERNAL_PHOTO);
                return;
            } catch (Exception ignored) {
            }
        }
        Intent cameraIntent = new Intent(activity, CameraActivity.class);
        cameraIntent.putExtra("width", width);
        cameraIntent.putExtra("height", height);
        startActivityForResult(cameraIntent, RESULT_PHOTO);
    }

    public boolean pickFile(FileTransfer listener) {
        try {
            fileTransferListener = listener;
            Intent theIntent = new Intent(Intent.ACTION_GET_CONTENT);
            theIntent.setType("file/*");
            theIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (!isCallable(theIntent)) return false;
            startActivityForResult(theIntent, RESULT_EXTERNAL_FILE);
            return true;
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("pickFile", e);
            return false;
        }
    }

    private boolean isCallable(Intent intent) {
        return !activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }
    private void startActivityForResult(Intent intent, int code) {
        activity.startActivityForResult(intent, code);
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        jimm.modules.DebugLog.println("result " + requestCode + " " + resultCode + " " + data);
        if (JimmActivity.RESULT_OK != resultCode) return false;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return false;
                photoListener.processPhoto(data.getByteArrayExtra("photo"));
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                jimm.modules.DebugLog.println("photoListener " + photoListener);
                if (null == photoListener) return false;

                // remove copy of image
                if ((null != data) && (null != data.getData()) && (null != imageUrl)) {
                    Uri file = Uri.parse("file://" + getRealPathFromUri(data.getData()));
                    jimm.modules.DebugLog.println("pickFile " + imageUrl + " " + file);
                    if (!imageUrl.equals(file)) {
                        new File(file.getPath()).delete();
                    }
                }

                Uri uriImage = imageUrl;
                jimm.modules.DebugLog.println("pickFile " + uriImage);
                InputStream in = activity.getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                TcpSocket.readFully(in, img, 0, img.length);
                photoListener.processPhoto(img);

                imageUrl = null;
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                Uri fileUri = data.getData();
                InputStream is = activity.getContentResolver().openInputStream(fileUri);
                fileTransferListener.onFileSelect(is, getFileName(fileUri));
                fileTransferListener = null;
                return true;
            }
        } catch (Throwable ignored) {
            jimm.modules.DebugLog.panic("activity", ignored);
        }
        return false;
    }

    private String getFileName(Uri fileUri) {
        String file = getRealPathFromUri(fileUri);
        return file.substring(file.lastIndexOf('/') + 1);
    }

    private String getRealPathFromUri(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String[] proj = { MediaStore.MediaColumns.DATA };
                Cursor cursor = activity.managedQuery(uri, proj, null, null, null);
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception ignored) {
        }
        return uri.toString();
    }

    public void showHistory(HistoryStorage history) {
        String historyFilePath = history.getAndroidStorage().getTextFile();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + historyFilePath);
        intent.setDataAndType(uri, "text/plain");
        activity.startActivity(intent);
    }


    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES), "Jimm");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Jimm", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath(), "IMG_"+ timeStamp + ".jpg");
    }
}
