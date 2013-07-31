package jimmui.view.form;

import jimmui.view.text.Par;

import javax.microedition.lcdui.Image;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 01.08.13 0:37
 *
 * @author vladimir
 */
class Control {
    public int id;
    public Par description;
    public boolean disabled;
    public byte type;
    public int height;

    // input
    public int inputType;
    public String text;
    public int size;
    // checkbox
    public boolean selected;
    // select
    public String[] items;
    public int current;
    // gauge
    public int level;
    // image
    public Image image;
}
