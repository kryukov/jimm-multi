package protocol.vk.api;

import jimm.comm.Util;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

import android.content.Context;

import org.json.*;

import java.io.IOException;
import java.util.Collection;

public class VkApp {
    private static final String APP_ID = "3373173";
    private static final String SCOPE = "friends,messages,notifications";
    //constants for OAUTH AUTHORIZE in Vkontakte
    public static final String CALLBACK_URL = "https://oauth.vk.com/blank.html";
    private static final String OAUTH_AUTHORIZE_URL = "https://oauth.vk.com/authorize?client_id=" + APP_ID + "&scope=" + SCOPE + "&redirect_uri=" + CALLBACK_URL + "&display=touch&response_type=token";

    private Context _context;
    private VkDialogListener _listener;
    private VkSession _vkSess;

    private static final String VK_API_URL = "https://api.vk.com/method/";
    private boolean error;

    public VkApp(Context context) {
        _context = context;
        _vkSess = new VkSession(_context);
        setListener(new VkDialogListener() {
            @Override
            public void onComplete(String url) {
                jimm.modules.DebugLog.println("token " + url);
                _vkSess.deserialize(url.substring(url.indexOf("#") + 1));
            }

            @Override
            public void onError(String description) {
                jimm.modules.DebugLog.println("error " + description);
                error = true;
            }
        });
    }
    public boolean isLogged() {
        return !"".equals(_vkSess.getAccessToken()[0]);
    }

    public void setListener(VkDialogListener listener) {
        _listener = listener;
    }

    public void showLoginDialog() {
        new VkDialog(_context, OAUTH_AUTHORIZE_URL, _listener).show();
    }

    public JSONObject getFriends() {
        return request(uri("friends.get", "fields=first_name,last_name,nickname"));
    }
    public JSONObject getOnlineFriends() {
        return request(uri("friends.getOnline", ""));
    }

    public void getDialogs() {
        request(uri("messages.getDialogs", ""));
    }
    public void sendMessage(String uid, String message) {
        request(uri("messages.send", "uid=" + Util.xmlEscape(uid) + "&message=" + Util.xmlEscape(message)));
    }
    public JSONObject getMessages() {
        return request(uri("messages.get", "filters=1"));
    }
    public void markAsRead(Collection<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (Integer id : ids) {
            sb.append(id).append(",");
        }
        request(uri("messages.markAsRead", "mids=" + Util.xmlEscape(sb.toString())));
    }

    private JSONObject request(String uri) {
        //send request to vkontakte api
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uri);

        try {
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            String responseText = EntityUtils.toString(entity);

            jimm.modules.DebugLog.println("result " + responseText);
            //parse response for error code or not
            if (parseResponse(responseText)) throw new IOException("error");
            return new JSONObject(responseText);

            //Log.d(Constants.DEBUG_TAG,"response text="+responseText);
        } catch (Exception ioex) {
            jimm.modules.DebugLog.panic("uri" + uri, ioex);
            return null;
        }
    }
    private String uri(String method, String args) {
        String[] params = _vkSess.getAccessToken();
        String accessToken = params[0];
        return VK_API_URL + method + "?" + args + "&access_token=" + accessToken;
    }

    public boolean hasAccessToken() {
        String[] params = _vkSess.getAccessToken();
        try {
            long accessTime = Long.parseLong(params[3]);
            long currentTime = System.currentTimeMillis();
            long expireTime = (currentTime - accessTime) / 1000;

            //Log.d(Constants.DEBUG_TAG,"expires time="+expireTime);

            if (params[0].equals("") || params[1].equals("") || params[2].equals("") || Long.parseLong(params[3]) == 0) {
                //Log.d(Constants.DEBUG_TAG,"access token empty");
                return false;
            } else if (expireTime >= Long.parseLong(params[1])) {
                //Log.d(Constants.DEBUG_TAG,"access token time expires out");
                return false;
            } else {
                //Log.d(Constants.DEBUG_TAG,"access token ok");
                return true;
            }
        } catch (Exception e) {
            jimm.modules.DebugLog.panic("hasAccessToken", e);
        }
        return false;
    }

    public boolean isError() {
        return error;
    }

    public interface VkDialogListener {
        void onComplete(String url);

        void onError(String description);
    }

    //parse vkontakte JSON response
    private boolean parseResponse(String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);

            if (jsonObj.has("error")) {
                JSONObject errorObj = jsonObj.getJSONObject("error");
                int errCode = errorObj.getInt("error_code");
                return errCode != 14;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
            //Log.d(Constants.DEBUG_TAG,"exception when creating json object");
        }

        return false;
    }

}
