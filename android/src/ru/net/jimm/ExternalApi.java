package ru.net.jimm;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import jimm.FileTransfer;
import jimm.history.HistoryStorage;
import jimm.modules.photo.PhotoListener;
import ru.net.jimm.photo.CameraActivity;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 03.04.13 15:30
 *
 * @author vladimir
 */
public class ExternalApi {
    private JimmActivity activity;

    public void setActivity(JimmActivity activity) {
        this.activity = activity;
    }

    private PhotoListener photoListener = null;
    private FileTransfer fileTransferListener = null;
    private static final int RESULT_PHOTO = JimmActivity.RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = JimmActivity.RESULT_FIRST_USER + 2;
    private static final int RESULT_EXTERNAL_FILE = JimmActivity.RESULT_FIRST_USER + 3;
    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        jimm.modules.DebugLog.println("result " + requestCode + " " + resultCode + " " + data);
        if (null == data) return;
        if (JimmActivity.RESULT_OK != resultCode) return;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return;
                photoListener.processPhoto(data.getByteArrayExtra("photo"));
                photoListener = null;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                if (null == photoListener) return;
                Uri uriImage = data.getData();
                InputStream in = activity.getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                in.read(img);
                photoListener.processPhoto(img);
                photoListener = null;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                Uri fileUri = data.getData();
                InputStream is = activity.getContentResolver().openInputStream(fileUri);
                fileTransferListener.onFileSelect(is, getFileName(fileUri));
                fileTransferListener = null;
            }
        } catch (Throwable ignored) {
            jimm.modules.DebugLog.panic("activity", ignored);
        }
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
}
