/*
 * DisplayableEx.java
 *
 * Created on 23 Октябрь 2008 г., 23:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import jimm.Jimm;

/**
 *
 * @author Vladimir Krukov
 */
public abstract class DisplayableEx {
    
    public final void restore() {
        Jimm.getJimm().getDisplay().restore(this);
    }
    
    public final void showTop() {
        showing();
        Jimm.getJimm().getDisplay().showTop(this);
    }
    public final void show() {
        showing();
        Jimm.getJimm().getDisplay().show(this);
    }
    
    public final void back() {
        Jimm.getJimm().getDisplay().back(this);
    }
    
    protected void restoring() {
    }
    protected void showing() {
    }
    protected void closed() {
    }
}
