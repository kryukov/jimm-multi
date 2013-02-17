/*
 * MsnGroup.java
 *
 * Created on 3 Март 2010 г., 17:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import protocol.Group;

/**
 *
 * @author Vladimir Krukov
 */
public class MsnGroup extends Group {
    
    /** Creates a new instance of MsnGroup */
    public MsnGroup(String name, int id) {
        super(name);
        setGroupId(id);
    }

    private String guid;
    void setGuid(String guid) {
        this.guid = guid;
    }
    String getGuid() {
        return guid;
    }
    
}
// #sijapp cond.end #