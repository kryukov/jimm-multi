package jimmui.view.form;

import javax.microedition.lcdui.Image;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 02.08.13 21:52
 *
 * @author vladimir
 */
public interface Form {
    void setControlStateListener(ControlStateListener l);

    void destroy();

    void clearForm();

    void addSelector(int controlId, String label, String items, int index);

    void addSelector(int controlId, String label, String[] items, int index);

    void addVolumeControl(int controlId, String label, int current);

    void addCheckBox(int controlId, String label, boolean selected);

    void addHeader(String label);

    void addString(String label, String text);

    void addString(String text);

    void addString(int controlId, String text);

    void addLink(int controlId, String text);

    void addLatinTextField(int controlId, String label, String text, int size);

    void addTextField(int controlId, String label, String text, int size);

    void addPasswordField(int controlId, String label, String text, int size);

    void addTextField(int controlId, String label, String text, int size, int type);

    void addImage(Image img);

    void remove(int controlId);

    boolean hasControl(int controlId);

    void setTextFieldLabel(int controlId, String desc);

    int getGaugeValue(int controlId);

    int getVolumeValue(int controlId);

    String getTextFieldValue(int controlId);

    int getSelectorValue(int controlId);

    String getSelectorString(int controlId);

    boolean getCheckBoxValue(int controlId);

    void addThumbnailImage(Image img);


    void show();
    void showTop();
    void restore();
    void back();
    void invalidate();
}
