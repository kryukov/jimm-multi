/*
 * ObimpGroup.java
 *
 * Created on 5 Декабрь 2010 г., 13:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_OBIMP is "true" #
package protocol.obimp;

import protocol.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class ObimpGroup extends Group {
    
    /** Creates a new instance of ObimpGroup */
    public ObimpGroup(String name) {
        super(name);
    }
    public void setGroupId(int groupId) {
        super.setGroupId(groupId);
        if (0 == groupId) {
            setMode(Group.MODE_BOTTOM | Group.MODE_NEW_CONTACTS);
        }
    }
}
// #sijapp cond.end #