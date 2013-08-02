package jimmui.view;

import jimmui.view.form.Form;
import jimmui.view.form.FormListener;
import jimmui.view.form.GraphForm;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 02.08.13 22:03
 *
 * @author vladimir
 */
public class UIBuilder {
    public static Form createForm(String caption, String ok, String cancel, FormListener l) {
        return new GraphForm(caption, ok, cancel, l);
    }
}
