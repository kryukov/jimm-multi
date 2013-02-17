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
 * File: src/jimmLangFileTool/GUI.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Vladimir Krukov
 *******************************************************************************/

package jimmLangFileTool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.JLabel;
import java.awt.GridLayout;

public class GUI extends JFrame implements ActionListener {
    
    // Variables
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JScrollPane compareScrollPane = null;
    private JTable compareTable = null;
    private JlftTableModel jlftTableModel = null;  //  @jve:decl-index=0:visual-constraint=""
    private TableCellRenderer renderer;
    
    private JimmLangFileTool jlft;
    private JToolBar compareToolBar = null;
    private JButton openBase = null;
    private JButton openCompare = null;
    private JButton saveCompare = null;
    private JButton remove = null;
    private JButton optimize = null;
    private JButton about = null;
    private JButton removeGroup = null;
    
    /**
     * This is the default constructor
     */
    public GUI(JimmLangFileTool _jlft) {
        jlft = _jlft;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    /**
     * This method initializes this
     *
     * @return void
     */
    public void initialize() {
        renderer = new JlftTableRenderer();
        setContentPane(getJContentPane(false));
        setTitle("Jimm Lang File Tool");
        setSize(new java.awt.Dimension(850,500));
        setLocation(new java.awt.Point(20,20));
        fillInValues();
        pack();
        setVisible(true);
    }
    
    private int getSizeForTable() {
        LGFile base = jlft.getBase();
        LGFile compare = jlft.getCompare();
        return (base.getEntrysize() > compare.getEntrysize())
                ? base.getEntrysize()
                : compare.getEntrysize();
    }
    
    private void fillInValues() {
        int j=0;
        int k=0;
        int l=0;
        int m=0;
        
        // Delete all old stuff
        compareTable.removeAll();
        compareTable.setModel(getDefaultTableModel(true));
        setTableProps();
        
        // Set base values
        LGFile base = jlft.getBase();
        for (int i = 0; i < base.size(); i++) {
            LGFileSubset subset = (LGFileSubset) base.get(i);
            compareTable.setValueAt(subset, i + k + l, 0);
            for (j = 0; j < subset.size(); j++) {
                if (((LGString)subset.get(j)).getTranslated() != LGString.REMOVED) {
                    compareTable.setValueAt(subset.get(j), i + k + j + 1 + l, 0);
                    compareTable.setValueAt(subset.get(j), i + k + j + 1 + l, 1);
                } else {
                    l--;
                }
            }
            k += j;
        }
        
        k =0;
        l =0;
        
        // Set compare values
        LGFile compare = jlft.getCompare();
        for(int i = 0; i < compare.size(); i++) {
            if (!((LGFileSubset) compare.get(i)).isRemoved()) {
                LGFileSubset subset = (LGFileSubset)compare.get(i);
                compareTable.setValueAt(subset, i + k + l + m, 2);
                for (j = 0; j < subset.size(); j++) {
                    if (((LGString)subset.get(j)).getTranslated() != LGString.REMOVED) {
                        compareTable.setValueAt(subset.get(j), i + k + j + 1 + l + m, 2);
                        compareTable.setValueAt(subset.get(j), i + k + j + 1 + l + m, 3);
                    } else {
                        l--;
                    }
                }
                k += j;
            } else {
                m--;
            }
        }
    }
    
    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane(boolean refresh) {
        if (jContentPane == null || refresh) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints1.gridy = 1;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.weighty = 1.0;
            gridBagConstraints1.gridx = 0;
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridx = 0;
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.setPreferredSize(new java.awt.Dimension(850,500));
            jContentPane.setMinimumSize(new java.awt.Dimension(810,66));
            jContentPane.add(getCompareToolBar(), gridBagConstraints);
            jContentPane.add(getCompareScrollPane(), gridBagConstraints1);
        }
        return jContentPane;
    }
    
    /**
     * This method initializes compareScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getCompareScrollPane() {
        if (compareScrollPane == null) {
            compareScrollPane = new JScrollPane();
            compareScrollPane.setName("compareScrollPane");
            compareScrollPane.setViewportView(getCompareTable());
        }
        return compareScrollPane;
    }
    
    /**
     * This method initializes compareTable
     *
     * @return javax.swing.JTable
     */
    private JTable getCompareTable() {
        if (compareTable == null) {
            compareTable = new JTable();
            compareTable.setBackground(java.awt.Color.white);
            compareTable.setFont(new Font("DialogInput",Font.PLAIN, 12));
            compareTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            compareTable.setModel(getDefaultTableModel(false));
            setTableProps();
        }
        return compareTable;
    }
    
    private void setTableProps() {
        setColumnWidth(0, 170, 170, 170);
        setColumnWidth(1, 220,  -1, 220);
        setColumnWidth(2,   1, 170,   1);
        setColumnWidth(3, 220,  -1, 220);
        JTextField field = new JTextField();
        field.setFont(new Font("DialogInput",Font.PLAIN, 12));
        TableCellEditor editor = new DefaultCellEditor(field);
        try {
            compareTable.setDefaultEditor(Class.forName("java.lang.Object"), editor);
            compareTable.setDefaultRenderer(Class.forName("java.lang.Object"), renderer);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void setColumnWidth(int num, int minWidth, int maxWidth, int prefWidth) {
        TableColumn colum = compareTable.getColumnModel().getColumn(num);
        if (minWidth > 0) {
            colum.setMinWidth(minWidth);
        }
        if (maxWidth > 0) {
            colum.setMaxWidth(maxWidth);
        }
        if (prefWidth > 0) {
            colum.setPreferredWidth(prefWidth);
        }
    }

    /**
     * This method initializes defaultTableModel
     *
     * @return javax.swing.table.DefaultTableModel
     */
    private DefaultTableModel getDefaultTableModel(boolean refresh) {
        if (jlftTableModel == null || refresh) {
            jlftTableModel = new JlftTableModel();
            Vector columIdent = new Vector();
            columIdent.add("Base key "+jlft.getBase().getName());
            columIdent.add("Base value "+jlft.getBase().getName());
            columIdent.add("Compare key "+jlft.getCompare().getName());
            columIdent.add("Compare value "+jlft.getCompare().getName());
            jlftTableModel.setColumnIdentifiers(columIdent);
            jlftTableModel.setColumnCount(4);
            jlftTableModel.setRowCount(getSizeForTable());
        }
        return jlftTableModel;
    }
    
    
    
    /**
     * This method initializes compareToolBar
     *
     * @return javax.swing.JToolBar
     */
    private JToolBar getCompareToolBar() {
        if (compareToolBar == null) {
            compareToolBar = new JToolBar();
            compareToolBar.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
            compareToolBar.setFloatable(false);
            compareToolBar.setName("compareToolBar");
            compareToolBar.add(getOpenBase());
            compareToolBar.add(getOpenCompare());
            compareToolBar.add(getSaveCompare());
            compareToolBar.add(getRemove());
            compareToolBar.add(getRemoveGroup());
            compareToolBar.add(getOptimize());
            compareToolBar.add(getAbout());
            compareToolBar.add(getLegendPanel());
        }
        return compareToolBar;
    }
    
    /**
     * This method initializes openBase
     *
     * @return javax.swing.JButton
     */
    private JButton getOpenBase() {
        if (openBase == null) {
            openBase = new JButton();
            openBase.setText("Open base");
            openBase.setToolTipText("Open a base langauge file (the left one)");
            openBase.addActionListener(this);
        }
        return openBase;
    }
    
    private JButton getOptimize() {
        if (optimize == null) {
            optimize = new JButton();
            optimize.setText("Optimize");
            optimize.setToolTipText("Optimize all langauge file (remove duplicate nodes)");
            optimize.addActionListener(this);
        }
        return optimize;
    }
    
    /**
     * This method initializes openCompare
     *
     * @return javax.swing.JButton
     */
    private JButton getOpenCompare() {
        if (openCompare == null) {
            openCompare = new JButton();
            openCompare.setText("Open compare");
            openCompare.setToolTipText("Open a compare langauge file (the left one)");
            openCompare.addActionListener(this);
        }
        return openCompare;
    }
    
    /**
     * This method initializes saveCompare
     *
     * @return javax.swing.JButton
     */
    private JButton getSaveCompare() {
        if (saveCompare == null) {
            saveCompare = new JButton();
            saveCompare.setText("Save compare");
            saveCompare.setToolTipText("Save the compare file (the right one)");
            saveCompare.addActionListener(this);
        }
        return saveCompare;
    }
    
    /**
     * This method initializes remove
     *
     * @return javax.swing.JButton
     */
    private JButton getRemove() {
        if (remove == null) {
            remove = new JButton();
            remove.setText("Remove");
            remove.setToolTipText("Remove the currently selected langauge String");
            remove.addActionListener(this);
        }
        return remove;
    }
    
    /**
     * This method initializes about
     *
     * @return javax.swing.JButton
     */
    private JButton getAbout() {
        if (about == null) {
            about = new JButton();
            about.setText("About");
            about.setToolTipText("Display info about this application");
            about.addActionListener(this);
        }
        return about;
    }
    
    public void actionPerformed(ActionEvent act) {
        int answer = -1;
        if (act.getSource() == openBase) {
            JFileChooser chooser = new JFileChooser("src/lng");
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    jlft.setBase(LGFile.load(file.getCanonicalPath()));
                } catch (Exception e) {
                    System.out.println("Error loading file");
                }
                jlft.compare();
                fillInValues();
            }
        }
        if (act.getSource() == openCompare) {
            JFileChooser chooser = new JFileChooser("src/lng/");
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    jlft.setCompare(LGFile.load(file.getCanonicalPath()));
                } catch (Exception e) {
            	    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading the file", "Error", JOptionPane.ERROR_MESSAGE);
                }
                jlft.compare();
                fillInValues();
            }
        }
        if (act.getSource() == saveCompare) {
            JFileChooser chooser = new JFileChooser("src/lng/");
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    jlft.getCompare().save(file.getCanonicalPath());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error saving the file", "Error", JOptionPane.ERROR_MESSAGE);
                }
                jlft.compare();
                fillInValues();
            }
        }
        if (act.getSource() == remove) if ((compareTable.getSelectedColumn() == -1) || (compareTable.getSelectedRow() == -1))
            JOptionPane.showMessageDialog(this, "You have to select an item first", "Select first", JOptionPane.ERROR_MESSAGE);
        else
            if (((compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn()) instanceof LGString))) {
            if ((((LGString) compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn())).isTranslated() != LGString.NOT_IN_BASE_FILE))
                JOptionPane.showMessageDialog(this, "You can only delete items which are not in the base file(red).", "Cannot delete item", JOptionPane.ERROR_MESSAGE);
            else {
                answer = JOptionPane.showConfirmDialog(this, "Delete \"" + ((LGString) compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn())).getKey() + "\"?", "Delete?", JOptionPane.YES_NO_OPTION);
                switch (answer) {
                    case JOptionPane.YES_OPTION:
                        System.out.println("YES");
                        ((LGString) compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn())).setTranslated(LGString.REMOVED);
                        fillInValues();
                        break;
                    default: // do nothing
                }
            }
            }
        
        if (act.getSource() == removeGroup) if (compareTable.getSelectedColumn() > 1) if ((compareTable.getSelectedColumn() == -1) || (compareTable.getSelectedRow() == -1))
            JOptionPane.showMessageDialog(this, "You have to select a group item first", "Select first", JOptionPane.ERROR_MESSAGE);
        else
            if ((compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn()) instanceof LGFileSubset)) {
            answer = JOptionPane.showConfirmDialog(this, "Delete \"" + ((LGFileSubset) compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn())).getId() + "\"?", "Delete?", JOptionPane.YES_NO_OPTION);
            switch (answer) {
                case JOptionPane.YES_OPTION:
                    System.out.println("YES");
                    System.out.println("Delete it");
                    ((LGFileSubset) compareTable.getValueAt(compareTable.getSelectedRow(), compareTable.getSelectedColumn())).setRemoved(true);
                    fillInValues();
                    break;
                default: // do nothing
            }
            }
        
        if (act.getSource() == about) {
            JOptionPane.showMessageDialog(this, "Jimm Lang File Tool - Tool for editing Jimm language Files\n\n (C) Jimm project 2005\n\nwww.jimm.org\n\n", "About Jimm Lang File Tool", JOptionPane.PLAIN_MESSAGE);
        }
        if (act.getSource() == optimize) {
            jlft.getBase().optimize();
            jlft.getCompare().optimize();
            jlft.compare();
            fillInValues();
        }
        
    }
    
    
    
    public class JlftTableRenderer extends DefaultTableCellRenderer {
        
        private static final long serialVersionUID = 1L;
        LGString lgs;
        
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof LGString) {
                lgs = (LGString) value;
                
                switch (lgs.isTranslated()) {
                    case LGString.TRANSLATED:
                        cell.setBackground(Color.white);
                        break;
                    case LGString.NEWLY_TRANSLATED:
                        cell.setBackground(Color.orange);
                        break;
                    case LGString.NOT_IN_BASE_FILE:
                        cell.setBackground(Color.red);
                        break;
                    case LGString.NOT_TRANSLATED:
                        cell.setBackground(Color.yellow);
                        break;
                    default:
                        cell.setBackground(Color.white);
                }
            } else {
                cell.setFocusable(false);
                cell.setBackground(Color.lightGray);
            }
            
            return cell;
            
        }
    }
    
    
    public class JlftTableModel extends DefaultTableModel {
        
        private static final long serialVersionUID = 1L;
        LGString lgs;
        
        public boolean isCellEditable(int row, int colum) {
            if(!(super.getValueAt(row,colum) instanceof LGString) || (colum  < 2)
                    || ((LGString)super.getValueAt(row,colum)).isTranslated() == LGString.NOT_IN_BASE_FILE)
                return false;
            else
                return true;
            
        }
        
        public Object getValueAt(int row, int colum) {
            Object o = super.getValueAt(row,colum);
            if(o instanceof LGString) {
                lgs = (LGString)o;
                if(colum % 2 == 0) {
                    lgs.setReturnKey(true);
                } else {
                    lgs.setReturnKey(false);
                }
                return lgs;
            } else {
                return o;
            }
        }
        
        public void setValueAt(Object value, int row, int colum) {
            Object o = getValueAt(row, colum);
            if (value instanceof String && !(((String)value).startsWith("MOD_")
                    || ((String)value).startsWith("TAR_")
                    || ((String)value).startsWith("GENERAL"))
                    && o instanceof LGString) {
                lgs = (LGString)o;
                if (!((String) value).equals(lgs.getValue())) {
                    lgs.setTranslated(LGString.NEWLY_TRANSLATED);
                    lgs.setValue((String)value);
                    super.setValueAt(lgs.getClone(), row, colum);
                }
            } else {
                super.setValueAt(value, row, colum);
            }
            
        }
    }
    
    
    /**
     * This method initializes legendPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getLegendPanel() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setRows(2);
        gridLayout.setHgap(3);
        gridLayout.setVgap(3);
        gridLayout.setColumns(2);
        JPanel legendPanel = new JPanel();
        legendPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3,3,3,3));
        legendPanel.setLayout(gridLayout);
        addToLegend(legendPanel, "Key was found in base and compare file", Color.white);
        addToLegend(legendPanel, "Key was found in compare but not in base", Color.red);
        addToLegend(legendPanel, "Text for this key was changed but not saved yet", Color.orange);
        addToLegend(legendPanel, "Key was not found in compare file", Color.yellow);
        return legendPanel;
    }
    
    private void addToLegend(JPanel lp, String cap, java.awt.Color color) {
        JLabel label = new JLabel();
        label.setText(cap);
        label.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        label.setFont(new java.awt.Font("MS Sans Serif", java.awt.Font.PLAIN, 10));
        label.setOpaque(true);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,3,0,3));
        label.setBackground(color);
        lp.add(label, null);
    }
    
    /**
     * This method initializes removeGroup
     *
     * @return javax.swing.JButton
     */
    private JButton getRemoveGroup() {
        if (removeGroup == null) {
            removeGroup = new JButton();
            removeGroup.setText("Remove group");
            removeGroup.setToolTipText("Remove a whole group of Strings at once");
            removeGroup.addActionListener(this);
        }
        return removeGroup;
    }
    
}
