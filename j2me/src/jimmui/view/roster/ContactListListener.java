/*
 * ContactListListener.java
 *
 * Created on 21 Декабрь 2009 г., 19:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.roster;

import jimmui.view.menu.MenuModel;
import jimmui.view.roster.items.TreeNode;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public interface ContactListListener {
    void activateMainMenu();
    MenuModel getContextMenu(Protocol p, TreeNode node);
}
