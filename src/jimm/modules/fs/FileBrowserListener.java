/*
 * FileBrowserListener.java
 *
 * Created on 1 Май 2007 г., 14:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_FILES="true"#
package jimm.modules.fs;

import jimm.JimmException;

/**
 *
 * @author vladimir
 */
public interface FileBrowserListener {
	public void onFileSelect(String file) throws JimmException;
	public void onDirectorySelect(String directory);
}
// #sijapp cond.end#