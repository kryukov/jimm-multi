package jimmui.view;

import jimmui.view.form.Form;
import jimmui.view.form.FormListener;
import jimmui.view.form.GraphForm;
import jimmui.view.form.Menu;
import jimmui.view.menu.MenuModel;
import jimmui.view.menu.Select;

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
    public static Menu createMenu(MenuModel menu) {
        return new Select(menu);
    }
}
