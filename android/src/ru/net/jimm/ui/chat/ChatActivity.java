package ru.net.jimm.ui.chat;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import jimmui.model.chat.ChatModel;
import ru.net.jimm.R;
import ru.net.jimm.input.Input;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 12:53
 *
 * @author vladimir
 */
public class ChatActivity extends Activity {
    private ChatAdapter chatAdapter;
    private Input input;
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.chat);
        chatAdapter = new ChatAdapter(this, new ChatModel());

        input = new Input(this, null, R.id.input_line);
        ((LinearLayout)findViewById(R.id.chat_input)).addView(input);
        final ListView messages = (ListView) findViewById(R.id.chat_messages);
    }
}
