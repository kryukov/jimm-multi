/*
 * XStatusInfo.java
 *
 * Created on 28 Апрель 2011 г., 23:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import jimm.util.JLocale;

/**
 *
 * @author Vladimir Kryukov
 */
public class XStatusInfo {
    public static final int XSTATUS_NONE = -1;
    private final ImageList icons;
    private final String[] names;
    
    public XStatusInfo(ImageList icons, String[] names) {
        this.icons = icons;
        this.names = names;
    }
    
    public Icon getIcon(int index) {
        index = (index < 0) ? index : (index & 0xFF);
        return icons.iconAt(index);
    }
    public String getName(int index) {
        index = (index < 0) ? index : (index & 0xFF);
        if ((0 <= index) && (index < names.length)) {
            return names[index];
        }
        return JLocale.getString("xstatus_none");
    }
    public int getXStatusCount() {
        return names.length;
    }
}
