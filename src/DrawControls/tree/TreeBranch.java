/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/DrawControls/TreeBranch.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Vladimir Kryukov
 *******************************************************************************/
/*
 * TreeBranch.java
 *
 * Created on 7 Февраль 2008 г., 16:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.tree;

/**
 *
 * @author vladimir
 */
public abstract class TreeBranch implements TreeNode {
	
    public TreeBranch() {
    }

    private boolean expanded = false;

    public final boolean isExpanded() {
        return expanded;
    }
    /**
     * Expand or collapse tree node.
     * 
     * NOTE: this is not recursive operation!
     */
    public final void setExpandFlag(boolean value) {
        expanded = value;
        sort();
    }

    public void sort() {
    }
    public abstract boolean isEmpty();
}
