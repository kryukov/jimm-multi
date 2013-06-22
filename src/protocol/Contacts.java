package protocol;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 22.06.13 18:14
 *
 * @author vladimir
 */
public class Contacts extends Vector {
    public boolean add(Contact c) {
        super.addElement(c);
        return true;
    }
}
