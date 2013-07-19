/*
 * PhotoListener.java
 *
 * Created on 4 Октябрь 2009 г., 6:28
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_FILES="true"#

package jimm.modules.photo;

/**
 *
 * @author Vladimir Kryukov
 */
public interface PhotoListener {
    void processPhoto(byte[] photo);
}
// #sijapp cond.end#
