package jimmui.view.smiles;

import jimmui.view.ActionListener;
import jimmui.view.icons.*;
import javax.microedition.lcdui.*;
import jimmui.view.base.*;


public final class Selector extends SomeContentList {
    public Selector(ImageList icons, String[] names, String[] codes) {
        super(null);
        content = new SmilesContent(this, icons, names, codes);
        softBar.setSoftBarLabels("select", "select", "cancel", false);
    }
    protected void restoring() {
        ((SmilesContent)content).restoring();
    }

    public void setCaption(String name) {
        bar.setCaption(name);
    }
}