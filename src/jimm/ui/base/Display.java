/*
 * Display.java
 *
 * Created on 21 Май 2011 г., 0:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Jimm;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.menu.Select;

/**
 *
 * @author Vladimir Kryukov
 */
public class Display {
    private javax.microedition.lcdui.Display display;
    private Object currentScreen = null;
    private Vector stack = new Vector();
    public static final long LONG_INTERVAL = 700;

    /** Creates a new instance of Display */
    public Display(Jimm jimm) {
        display = javax.microedition.lcdui.Display.getDisplay(jimm);
    }
    public javax.microedition.lcdui.Display getNativeDisplay() {
        return display;
    }
    private boolean isSystem(Object screen) {
        if (screen instanceof InputTextBox) {
            return true;
        }
        if (screen instanceof Displayable) {
            return true;
        }
        return false;
    }
    public boolean isPaused() {
        if (isSystem(getCurrentDisplay())) {
            return false;
        }
        for (int i = stack.size() - 1; 0 <= i; --i) {
            if (isSystem(stack.elementAt(i))) {
                return false;
            }
        }
        Displayable d = display.getCurrent();
        return (null == d) || !d.isShown();
    }
    public boolean isShown(Displayable d) {
        Displayable c = display.getCurrent();
        return (c == d) && c.isShown();
    }

    public void hide() {
        display.setCurrent(null);
    }
    public void hideIfNeed() {
        Displayable c = display.getCurrent();
        if ((null != c) && !c.isShown()) {
            hide();
        }
    }
    public Object getCurrentDisplay() {
        return currentScreen;
    }
    private void setCurrentDisplay(Object o) {
        final Object prev = currentScreen;
        currentScreen = o;
        if ((o != prev) && (null != prev)) {
            NativeCanvas.stopKeyRepeating();

            if (prev instanceof DisplayableEx) {
                ((DisplayableEx)prev).closed();
            }
            // #sijapp cond.if modules_LIGHT is "true" #
            if (!(prev instanceof CanvasEx)) {
                CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
            }
            // #sijapp cond.end#
        }
        Displayable d = null;
        if (o instanceof CanvasEx) {
            d = NativeCanvas.getInstance();

        } else if (o instanceof InputTextBox) {
            d = ((InputTextBox)o).getBox();

        } else if (o instanceof Displayable) {
            d = (Displayable)o;
        }

        if (d instanceof Canvas) {
            ((Canvas)d).setFullScreenMode(true);
        }
        if (o instanceof DisplayableEx) {
            ((DisplayableEx)o).restoring();
        }
        // #sijapp cond.if modules_LIGHT is "true" #
        if (!(o instanceof CanvasEx)) {
            CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
        }
        // #sijapp cond.end#

        if (o instanceof CanvasEx) {
            NativeCanvas instance = NativeCanvas.getInstance();
            instance.setCanvas((CanvasEx)o);

            if ((prev instanceof CanvasEx) && isShown(instance)) {
                instance.invalidate((CanvasEx)o);
                return;
            }
        }
        display.setCurrent(d);
    }

    public synchronized void back(Object o) {
        if (currentScreen != o) {
            int index = Util.getIndex(stack, o);
            if (-1 == index) {
                return;
            }
            while (pop() != o) {
            }
        }
        setCurrentDisplay(pop());
    }
    public synchronized void closeMenus() {
        Object o = currentScreen;
        while (o instanceof jimm.ui.menu.Select) {
            o = pop();
        }
        setCurrentDisplay(o);
    }
    public synchronized void restore(Object o) {
        int index = Util.getIndex(stack, o);
        if (0 <= index) {
            while (pop() != o) {
            }
        }
        setCurrentDisplay(o);
    }
    public synchronized void show(Object o) {
        if (null != currentScreen) {
            push(currentScreen);
        }
        setCurrentDisplay(o);
    }
    public synchronized void showTop(Object o) {
        stack.removeAllElements();
        setCurrentDisplay(o);
    }

    public synchronized void pushWindow(CanvasEx c) {
        Object old = currentScreen;
        if (null != old) {
            push(old);
        }
        c.showing();
        // setup without showing
        currentScreen = c;
    }
    private Object pop() {
        Object top = stack.lastElement();
        stack.removeElementAt(stack.size() - 1);
        return top;
    }
    private void push(Object o) {
        stack.addElement(o);
    }

    public Vector getStack() {
        return stack;
    }

    static boolean isLongAction(long start) {
        return start + LONG_INTERVAL < System.currentTimeMillis();
    }
}
