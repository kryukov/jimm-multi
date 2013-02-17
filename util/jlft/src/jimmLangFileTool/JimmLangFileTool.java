/*******************************************************************************
 * JimmLangFileTool - Simple Java GUI for editing/comparing Jimm language files
 * Copyright (C) 2005  Jimm Project
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
 * File: src/jimmLangFileTool/JimmLangFileTool.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Vladimir Krukov
 *******************************************************************************/

package jimmLangFileTool;

import javax.swing.JOptionPane;


public class JimmLangFileTool {
    
    // The both files we wound like to compare
    private LGFile base;
    private LGFile compare;
    
    public JimmLangFileTool() {
        base = new LGFile(baseLANGFile);
        compare = new LGFile(compareLANGFile);
    }
    
    
    /**
     * @return Returns the base.
     */
    public LGFile getBase() {
        return base;
    }
    
    
    /**
     * @return Returns the compare.
     */
    public LGFile getCompare() {
        return compare;
    }
    
    
    /**
     * @param base The base to set.
     */
    public void setBase(LGFile base) {
        this.base = base;
    }
    
    
    /**
     * @param compare The compare to set.
     */
    public void setCompare(LGFile compare) {
        this.compare = compare;
    }
    
    public void compare() {
        for (int i = 0; i < base.size(); i++) {
            // A hole baseSubset is missing in the compare file, add baseSubset from base file
            LGFileSubset baseSubset = (LGFileSubset)base.get(i);
            LGFileSubset compSubset = compare.containsGroup(baseSubset.getId());

            if (compSubset == null) {
                LGFileSubset temp = baseSubset.getClone();
                for (int j = 0; j < temp.size() ; j++) {
                    ((LGString)temp.get(j)).setTranslated(LGString.NOT_TRANSLATED);
                }
                compare.add(i,temp);
            // Only a few items are missing, find out which, add and tag them
            } else {
                System.out.println(baseSubset.getId());
                for (int k = 0; k < baseSubset.size(); k++) {
                    LGString lgs_base = (LGString)baseSubset.get(k);
                    if (lgs_base.isTranslated() == LGString.NOT_IN_BASE_FILE) continue;
                    LGString lgs_compare = compSubset.containsKey(lgs_base.getKey());
                    lgs_base.setTranslated(LGString.TRANSLATED);
                    if (lgs_compare == null) {
                        lgs_compare = lgs_base.getClone();
                        lgs_compare.setTranslated(LGString.NOT_TRANSLATED);
                        compSubset.add(k,lgs_compare);
                    } else if (lgs_compare.getTranslated() != LGString.NOT_TRANSLATED) {
                        lgs_compare.setTranslated(LGString.TRANSLATED);
                    }
                }
            }
            compSubset = compare.containsGroup(baseSubset.getId());
            for (int k = 0; k < compSubset.size(); k++) {
                LGString lgs_compare = (LGString)compSubset.get(k);
                LGString lgs_base = baseSubset.containsKey(lgs_compare.getKey());
                if (lgs_base == null) {
                    lgs_base = lgs_compare.getClone();
                    lgs_compare.setTranslated(LGString.NOT_IN_BASE_FILE);
                    lgs_base.setTranslated(LGString.NOT_IN_BASE_FILE);
                    baseSubset.add(k, lgs_base);
                }
            }
            
            Object[] a = baseSubset.toArray();
            for (int j = 0; j < a.length; j++) {
                int idx = compSubset.indexOf(((LGString)a[j]).getKey());
                if (idx >= 0) {
                    baseSubset.setElementAt(a[j], idx);
                } else {
                    System.out.println(((LGString)a[j]).getKey());
                }
            }
        }
    }
    
    
    // Variables
    public static String baseLANGFile = "src/lng/EN.lang";
    public static String compareLANGFile = "src/lng/RU.lang";
    
    static public void main(String[] argv) {
        JimmLangFileTool tool = new JimmLangFileTool();
        GUI ui = new GUI(tool);
        boolean loaded = false;
        try {
            tool.setBase(LGFile.load(baseLANGFile));
            tool.setCompare(LGFile.load(compareLANGFile));
            loaded = true;
        } catch (Exception e) {
        }
         if (!loaded) {
            JOptionPane.showMessageDialog(ui, "Error loading the file", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            tool.compare();
        }
        ui.initialize();
    }
    
}
