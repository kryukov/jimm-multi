/*
 * PrivacyItem.java
 *
 * Created on 30 Июнь 2011 г., 22:53
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.icq;

/**
 *
 * @author Vladimir Kryukov
 */
public class PrivacyItem {
    public String userId;
    public int id;
    public PrivacyItem(String userId, int id) {
        this.userId = userId;
        this.id = id;
    }
}
