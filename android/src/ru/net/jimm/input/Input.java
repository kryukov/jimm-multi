package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import jimm.modules.Templates;
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
public class Input extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private EditText messageEditor;
    private Runnable userMessageListener;
    private Object owner;
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
        messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageEditor.setOnEditorActionListener(enterListener);
        messageEditor.addTextChangedListener(textWatcher);

        ImageButton smileButton = (ImageButton) findViewById(R.id.smileButton);
        smileButton.setOnClickListener(this);
        smileButton.setOnLongClickListener(this);
        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        if (null != sendButton) sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                send();
            }
        });
    }

    @Override
    public void onClick(View view) {
        Emotions.selectEmotion(new ActionListener() {
            @Override
            public void action(final CanvasEx canvas, int cmd) {
                ((JimmActivity)getContext()).post(new Runnable() {
                    @Override
                    public void run() {
                        insert(" " + ((Selector)canvas).getSelectedCode() + " ");
                        showKeyboard();
                    }
                });
            }
        });
    }

    @Override
    public boolean onLongClick(View view) {
        Templates.getInstance().showTemplatesList(new ActionListener() {
            @Override
            public void action(CanvasEx canvas, int cmd) {
                ((JimmActivity) getContext()).post(new Runnable() {
                    @Override
                    public void run() {
                        final Input input = (Input) findViewById(R.id.input_line);
                        input.insert(Templates.getInstance().getSelectedTemplate());
                        input.showKeyboard();
                    }
                });
            }
        });
        return true;
    }

    private void showKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
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
                if ((0 == t.length()) || !canAdd(t)) {
                    messageEditor.setText(t);
                    messageEditor.setSelection(t.length());
                } else {
                    insert(t);
                }
                showKeyboard();
            }
        });
    }
    public boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return !text.endsWith(", ");
    }
    public void setOwner(Object owner) {
        if (this.owner != owner) {
            this.owner = owner;
            resetText();
        }
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

    public boolean hasText() {
        return 0 < messageEditor.getText().length();
    }

    public void insert(String text) {
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean isDone(int actionId) {
        return (EditorInfo.IME_NULL == actionId)
                || (EditorInfo.IME_ACTION_DONE == actionId)
                || (EditorInfo.IME_ACTION_SEND == actionId);
    }
    private final TextView.OnEditorActionListener enterListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (isDone(actionId)) {
                    send();
                    return true;
                }
            }
            return false;
        }
    };
    private TextWatcher textWatcher = new TextWatcher() {
        private String previousText;
        private int lineCount = 0;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            previousText = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if ((start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                }
            }
            if (lineCount != messageEditor.getLineCount()) {
                lineCount = messageEditor.getLineCount();
                messageEditor.requestLayout();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };
}
