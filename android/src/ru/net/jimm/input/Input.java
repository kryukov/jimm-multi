package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.net.jimm.JimmActivity;
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
    private Runnable userMessageListener;
    public Input(Context context, AttributeSet attrs, int id) {
        super(context, attrs);
        ((Activity)getContext())
                .getLayoutInflater()
                .inflate(R.layout.input, this, true);
        messageEditor = (TextView) findViewById(R.id.messageText);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
        setId(id);
    }

    @Override
    public void onClick(View view) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if (null != userMessageListener) {
            userMessageListener.run();
        }
    }

    public void setUserMessageListener(Runnable l) {
        userMessageListener = l;
    }

    public void setText(final String text) {
        ((JimmActivity)getContext()).post(new Runnable() {
            @Override
            public void run() {
                messageEditor.setText(text);
            }
        });
    }
    public String getText() {
        return messageEditor.getText().toString();
    }
}
