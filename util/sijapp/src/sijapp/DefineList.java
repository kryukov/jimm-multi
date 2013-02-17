/*******************************************************************************
 * SiJaPP - Simple Java PreProcessor
 * Copyright (C) 2003  Manuel Linsmayer
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
 *******************************************************************************/


package sijapp;

import java.io.*;
import java.util.*;


public class DefineList {
    private String prefix = "";    
    private String list = "";
    
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getList() {
        return list;
    }
    
    public void setList(String list) {
        this.list = list;
    }
    

    private boolean empty(String str) {
        return (null == str) || (0 == str.length());
    }
    private String[] explode(String value) {
        if (empty(value)) {
            return new String[0];
        }
        Vector values = new Vector();
        StringTokenizer strTok = new StringTokenizer(value, ",");
        while (strTok.hasMoreTokens()) {
            String langName = strTok.nextToken().trim();
            values.add(langName);
        }
        String[] result = new String[values.size()];
        values.copyInto(result);
        return result;
    }

    public Define[] getDefines() {
        String[] valueList = explode(list);
        Define[] results = new Define[valueList.length];
        for (int valueNum = 0; valueNum < valueList.length; valueNum++) {
            results[valueNum] = new Define();
            results[valueNum].setName(prefix + "_" + valueList[valueNum]);
            results[valueNum].setValue("true");
        }
        return results;
    }
}
