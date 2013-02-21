package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.net.jimm.R;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.02.13 17:08
 *
 * @author vladimir
 */
public class Input extends LinearLayout implements View.OnClickListener {
    private TextView messageEditor;
    private UserMessageListener userMessageListener;
    public Input(Context context, AttributeSet attrs) {
        super(context, attrs);
        ((Activity)getContext())
                .getLayoutInflater()
                .inflate(R.layout.input, this, true);
        messageEditor = (TextView) findViewById(R.id.messageText);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (null != userMessageListener) {
            userMessageListener.sendMessage(messageEditor.getText());
        }
    }

    public void setUserMessageListener(UserMessageListener l) {
        userMessageListener = l;
    }

    public static interface UserMessageListener {
        void sendMessage(CharSequence message);
    }
}
