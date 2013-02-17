/*
 * InputTextBox.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.base.*;
import jimm.util.JLocale;

/**
 * Extended TestBox.
 * Now realized:
 * 1) long text editor;
 * 2) smiles;
 * 3) templates;
 * 4) text buffer;
 * 5) transliteration (cyrilic);
 * 6) restoring previous windows.
 *
 * @author Vladimir Kryukov
 */
public final class InputTextBox extends DisplayableEx implements CommandListener, ActionListener {
    // #sijapp cond.if modules_SMILES is "true" #
    private Command insertEmotionCommand;
    // #sijapp cond.end#
    private Command insertTemplateCommand;
    private Command pasteCommand;
    private Command quoteCommand;
    private Command clearCommand;

    private Command cancelCommand;
    private Command okCommand;

    private int caretPos = 0;
    private boolean cancelNotify = false;
    private int textLimit;
    private String caption;
    private TextBox textBox;

    private int inputType;
    private int inputKeySet;
    private TextBoxListener listener;

    private int getBackType() {
        return Jimm.isPhone(Jimm.PHONE_SE_SYMBIAN) ? Command.CANCEL : Command.BACK;
    }

    private int getItemType() {
        return Jimm.isPhone(jimm.Jimm.PHONE_NOKIA) ? Command.SCREEN : Command.ITEM;
    }
    private TextBox createTextBox() {
        TextBox box = null;
        final int MAX_CHAR_PER_PAGE = Jimm.isPhone(Jimm.PHONE_SE) ? 9000 : 3000;
        final int MIN_CHAR_PER_PAGE = 1000;
        try {
            box = new TextBox(caption, "", Math.min(MAX_CHAR_PER_PAGE, textLimit), inputType);
        } catch (Exception e) {
            box = new TextBox(caption, "", Math.min(MIN_CHAR_PER_PAGE, textLimit), inputType);
        }
        setCaption(caption);

        int commandType = getItemType();
        // #sijapp cond.if modules_ANDROID is "true" #
        commandType = Command.SCREEN;
        // #sijapp cond.end#
        int editType = getItemType();

        if (TextField.ANY == inputType) {
            // #sijapp cond.if modules_SMILES is "true" #
            insertEmotionCommand  = initCommand("insert_emotion", commandType, 1);
            // #sijapp cond.end#
            insertTemplateCommand = initCommand("templates", commandType, 2);
            pasteCommand          = initCommand("paste", editType, 3);
            quoteCommand          = initCommand("quote", editType, 4);
            clearCommand          = initCommand("clear", editType, 5);
        }
        return box;
    }
    private void setOkCommandCaption(String title) {
        int okType = Command.OK;
        int cancelType = getBackType();
        int cancelIndex = 15;
        if (Options.getBoolean(Options.OPTION_SWAP_SEND_AND_BACK)) {
            okType = getBackType();
            cancelType = getItemType();
        }
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isS60v5()) {
            cancelIndex = 7;
        }
        // #sijapp cond.end #
        okCommand = initCommand(title, okType, 6);
        cancelCommand = initCommand("cancel", cancelType, cancelIndex);
    }

    public InputTextBox() {
    }
    private InputTextBox create(String title, int len, int type, String okCaption) {
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_NOKIA_S60v8)) {
            type = TextField.ANY;
        }
        // #sijapp cond.end#
        setCaption(JLocale.getString(title));
        cancelNotify = false;
        inputType = type;
        textLimit = len;
        inputKeySet = 0;
        textBox = createTextBox();
        setOkCommandCaption(okCaption);
        addDefaultCommands();
        textBox.setCommandListener(this);
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            Jimm.gc();
        }
        // #sijapp cond.end#
        return this;
    }
    public InputTextBox create(String title, int len, int type) {
        return create(title, len, type, "ok");
    }
    public InputTextBox create(String title, int len, String okCaption) {
        return create(title, len, TextField.ANY, okCaption);
    }
    public InputTextBox create(String title, int len) {
        return create(title, len, TextField.ANY);
    }

    public void setCancelNotify(boolean notify) {
        cancelNotify = notify;
    }

    public final boolean isOkCommand(Command cmd) {
        return okCommand == cmd;
    }

    private Command initCommand(String title, int type, int pos) {
        return new Command(JLocale.getString(title), type, pos);
    }
    private void addCommand(Command cmd) {
        if (null != cmd) {
            textBox.addCommand(cmd);
        }
    }

    private void addDefaultCommands() {
        addCommand(okCommand);

        // #sijapp cond.if modules_SMILES is "true" #
        if (Emotions.isSupported()) {
            addCommand(insertEmotionCommand);
        }
        // #sijapp cond.end#
        addCommand(insertTemplateCommand);
        addCommand(pasteCommand);
        addCommand(quoteCommand);
        addCommand(clearCommand);
        addCommand(cancelCommand);

        updateConstraints();
        updateInitialInputMode();
    }
    public void updateCommands() {
        updateConstraints();
        updateInitialInputMode();
    }

    private void updateInitialInputMode() {
        int mode = Options.getInt(Options.OPTION_INPUT_MODE);
        if (inputKeySet == mode) {
            return;
        }
        inputKeySet = mode;
        try {
            String[] modes = {null, "UCB_BASIC_LATIN", "UCB_CYRILLIC"};
            textBox.setInitialInputMode(modes[mode]);
        } catch (Exception e) {
        }
    }

    private void updateConstraints() {
        final int caps = TextField.INITIAL_CAPS_SENTENCE;
        final int realMode = textBox.getConstraints();
        boolean hasCaps = Options.getBoolean(Options.OPTION_TF_FLAGS);

        try {
            if ((0 != (realMode & caps)) != hasCaps) {
                int mode = inputType;
                if (hasCaps) {
                    mode |= caps;
                }
                textBox.setConstraints(mode);
            }
        } catch (Exception e) {
        }
    }
    protected void closed() {
    }
    public void showing() {
    }
    protected void restoring() {
    }

    public final Displayable getBox() {
        return textBox;
    }

    public void setTextBoxListener(TextBoxListener cl) {
        listener = cl;
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (cancelCommand == c) {
                if (cancelNotify) {
                    //listener.commandAction(c, null);
                    listener.textboxAction(this, false);
                }
                back();

            } else if (clearCommand == c) {
                setString(null);

            } else if ((pasteCommand == c) || (quoteCommand == c)) {
                boolean quote = (quoteCommand == c);
                int pos = getCaretPosition();
                String clip = JimmUI.getClipBoardText(quote);
                if (quote && (2 < pos)) {
                    String text = textBox.getString();
                    if (('\n' == text.charAt(pos - 2)) && ('\n' == text.charAt(pos - 1))) {
                        pos--;
                        clip = clip.substring(0, clip.length() - 1);
                    }
                }
                insert(clip, pos);

            } else if (insertTemplateCommand == c) {
                caretPos = getCaretPosition();
                Templates.getInstance().showTemplatesList(this);

                // #sijapp cond.if modules_SMILES is "true" #
            } else if (insertEmotionCommand == c) {
                caretPos = getCaretPosition();
                Emotions.selectEmotion(this);
                // #sijapp cond.end #

            } else {
                listener.textboxAction(this, true);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Text box", e);
            // #sijapp cond.end #
            if (isOkCommand(c)) {
                back();
            }
        }
  }
    private int getCaretPosition() {
        return textBox.getCaretPosition();
    }

    public void action(CanvasEx canvas, int cmd) {
        // #sijapp cond.if modules_SMILES is "true" #
        if (canvas instanceof Selector) {
            String space = getSpace();
            insert(space + ((Selector)canvas).getSelectedCode() + space, caretPos);
        }
        // #sijapp cond.end#
        if (Templates.getInstance().is(canvas)) {
            String s = Templates.getInstance().getSelectedTemplate();
            if (null != s) {
                insert(s, caretPos);
            }
        }
    }
    public final String getSpace() {
        return Options.getBoolean(Options.OPTION_DETRANSLITERATE) ? "  " : " ";
    }

    public boolean isCancelCommand(Command cmd) {
        return cancelCommand == cmd;
    }
    public String getRawString() {
        String text = StringConvertor.notNull(textBox.getString());
        return StringConvertor.removeCr(text);
    }
    public String getString() {
        String messText = getRawString();
        if (Options.getBoolean(Options.OPTION_DETRANSLITERATE)) {
            return StringConvertor.detransliterate(messText);
        }
        return messText;
    }

    private void insert(String str, int pos) {
        try {
            int max = textBox.getMaxSize() - textBox.size() - 1;
            if (max < str.length()) {
                str = str.substring(0, max);
            }
            textBox.insert(str, pos);
        } catch (Exception e) {
        }
    }

    public void setTicker(String text) {
        boolean hasTicker = Jimm.isPhone(Jimm.PHONE_ANDROID)
                || Jimm.isPhone(Jimm.PHONE_NOKIA);
        if (hasTicker) {
            textBox.setTicker((null == text) ? null : new Ticker(text));
        }
//        if (Options.getBoolean(Options.OPTION_POPUP_OVER_SYSTEM)) {
//            String boxText = textBox.getString();
//            textBox.setTicker((null == text) ? null : new Ticker(text));
//            if ((0 != boxText.length()) && (0 == textBox.getString().length())) {
//                textBox.setString(boxText);
//            }
//        }
    }
    public final void setCaption(String title) {
        caption = (null == title) ? "" : title;
        title = Options.getBoolean(Options.OPTION_UNTITLED_INPUT) ? null : caption;
        if (null != textBox) {
            String boxText = textBox.getString();
            textBox.setTitle(title);
            if (!boxText.equals(textBox.getString()) && (0 == textBox.getString().length())) {
                textBox.setString(boxText);
            }
        }
    }
    private String getCaption() {
        return caption;
    }

    private void setTextToBox(String text) {
        if (null != text) {
            // #sijapp cond.if target is "MIDP2"#
            try {
                if (Jimm.isPhone(Jimm.PHONE_NOKIA)) {
                    textBox.setString("");
                    textBox.insert(text, getCaretPosition());
                    return;
                }
            } catch (Exception e) {
            }
            // #sijapp cond.end #
            try {
                if (textBox.getMaxSize() < text.length()) {
                    text = text.substring(0, textBox.getMaxSize());
                }
                textBox.setString(text);
                return;
            } catch (Exception e) {
            }
        }
        textBox.setString("");
    }

    public void setString(String initText) {
        setTextToBox(initText);
    }
    public boolean isShown() {
        return textBox.isShown();
    }
}
