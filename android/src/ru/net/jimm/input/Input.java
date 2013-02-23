package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import jimm.modules.Emotions;
import jimm.ui.ActionListener;
import jimm.ui.Selector;
import jimm.ui.base.CanvasEx;
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
    private EditText messageEditor;
    private Runnable userMessageListener;
    public Input(Context context, AttributeSet attrs, int id) {
        super(context, attrs);
        ((Activity)getContext())
                .getLayoutInflater()
                .inflate(R.layout.input, this, true);
        setId(id);

        messageEditor = (EditText) findViewById(R.id.messageText);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditor.setOnEditorActionListener(enterListener);

        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        Emotions.selectEmotion(new ActionListener() {
            @Override
            public void action(final CanvasEx canvas, int cmd) {
                ((JimmActivity)getContext()).post(new Runnable() {
                    @Override
                    public void run() {
                        String space = " ";
                        String smile = space + ((Selector)canvas).getSelectedCode() + space;
                        int start = messageEditor.getSelectionStart();
                        int end = messageEditor.getSelectionEnd();
                        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                                smile, 0, smile.length());
                        showKeyboard();
                    }
                });
            }
        });
    }
    private void showKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(view, 0);
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    private void send() {
        hideKeyboard(messageEditor);
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
                String t = null == text ? "" : text;
                messageEditor.setText(t);
                messageEditor.setSelection(t.length());
                messageEditor.requestFocus();
                showKeyboard(messageEditor);
            }
        });
    }
    public void resetText() {
        ((JimmActivity)getContext()).post(new Runnable() {
            @Override
            public void run() {
                messageEditor.setText("");
            }
        });
    }
    public String getText() {
        return messageEditor.getText().toString();
    }

    private final TextView.OnEditorActionListener enterListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                send();
            }
            return true;
        }
    };

    public boolean hasText() {
        return 0 < messageEditor.getText().length();
    }
}
