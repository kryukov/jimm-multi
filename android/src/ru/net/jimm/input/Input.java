package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import jimm.Options;
import jimm.chat.Chat;
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
    private volatile Chat owner;
    private int layout = 0;
    private boolean sendByEnter;

    public Input(Context context, AttributeSet attrs, int id) {
        super(context, attrs);
        updateInput();
        setId(id);
    }
    public void updateInput() {
        boolean simple = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        final int newLayout = simple ? R.layout.input_simple : R.layout.input;
        if (layout != newLayout) {
            JimmActivity activity = (JimmActivity) getContext();
            activity.post(new Runnable() {
                @Override
                public void run() {
                    layout = newLayout;
                    init();
                    requestLayout();
                }
            });
        }
    }

    private void init() {
        removeAllViewsInLayout();
        ((Activity)getContext())
                .getLayoutInflater()
                .inflate(layout, this, true);

        messageEditor = (EditText) findViewById(R.id.messageText);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        ImageButton smileButton = (ImageButton) findViewById(R.id.input_smile_button);
        smileButton.setOnClickListener(this);
        smileButton.setOnLongClickListener(this);
        ImageButton sendButton = (ImageButton) findViewById(R.id.input_send_button);
        sendByEnter = (null == sendButton);
        if (null != sendButton) {
            sendButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                }
            });
        }
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        messageEditor.addTextChangedListener(textWatcher);
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
                        insert(Templates.getInstance().getSelectedTemplate());
                        showKeyboard();
                    }
                });
            }
        });
        return true;
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
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
        if (null != owner) {
            owner.sendMessage(getText());
        }
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
    public void setOwner(Chat owner) {
        if (this.owner != owner) {
            this.owner = owner;
            String name = (null != owner) ? owner.getContact().getName() : null;
            final String hint = (null == name)
                    ? getContext().getString(R.string.hint_message)
                    : getContext().getString(R.string.hint_message_to, name);
            ((JimmActivity)getContext()).post(new Runnable() {
                @Override
                public void run() {
                    messageEditor.setHint(hint);
                }
            });
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
            if (isDone(actionId)) {
                if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
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
            if (sendByEnter) {
                previousText = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
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
