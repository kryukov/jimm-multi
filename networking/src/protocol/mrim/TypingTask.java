/*
 * TypingTask.java
 *
 * Created on 3 Октябрь 2009 г., 18:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MRIM is "true" #
package protocol.mrim;

/**
 *
 * @author Vladimir Krukov
 */
public class TypingTask {
    public String email;
    public long time;

    public TypingTask(String userid, long time) {
        this.email = userid;
        this.time = time;
    }
}
// #sijapp cond.end #
