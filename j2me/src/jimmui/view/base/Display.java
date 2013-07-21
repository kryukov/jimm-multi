/*
 * Display.java
 *
 * Created on 21 Май 2011 г., 0:05
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.base;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.JimmMidlet;
import jimmui.view.chat.Chat;
import jimm.comm.Util;
import jimm.modules.*;
import jimmui.view.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class Display {
    private javax.microedition.lcdui.Display display;
    private NativeCanvas nativeCanvas;

    private Object currentScreen = null;
    private Vector<Object> stack = new Vector<Object>();
    public static final long LONG_INTERVAL = 700;
    private DisplayableEx main;

    /** Creates a new instance of Display */
    public Display() {
    }
    public void updateDisplay() {
        display = javax.microedition.lcdui.Display.getDisplay(JimmMidlet.getMidlet());
        nativeCanvas = new NativeCanvas();
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
            nativeCanvas.stopKeyRepeating();

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
            d = nativeCanvas;

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
            nativeCanvas.setCanvas((CanvasEx)o);

            // #sijapp cond.if modules_ANDROID isnot "true" #
            if ((prev instanceof CanvasEx) && isShown(nativeCanvas)) {
                nativeCanvas.invalidate((CanvasEx)o);
                return;
            }
            // #sijapp cond.end#
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
        if (stack.isEmpty()) {
            setCurrentDisplay(main);
        } else {
            setCurrentDisplay(pop());
        }
    }
    public synchronized void closeMenus() {
        Object o = currentScreen;
        while (o instanceof jimmui.view.menu.Select) {
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
    public synchronized void showMain(DisplayableEx o) {
        stack.removeAllElements();
        main = o;
        setCurrentDisplay(o);
    }
    public DisplayableEx getMain() {
        return main;
    }
    public synchronized void showTop(DisplayableEx o) {
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

    public boolean remove(Chat chat) {
        if (!stack.isEmpty() && (chat == stack.firstElement())) {
            stack.removeElementAt(0);
            while (!stack.isEmpty()) {
                Object o = stack.firstElement();
                if (o instanceof jimmui.view.menu.Select) {
                    stack.removeElementAt(0);
                } else {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    public NativeCanvas getNativeCanvas() {
        return nativeCanvas;
    }

    public boolean hasPointerEvents() {
        // #sijapp cond.if modules_ANDROID is "true"#
        if (true) return true;
        // #sijapp cond.elseif modules_TOUCH is "true"#
        if (true) return nativeCanvas.hasPointerEvents();
        // #sijapp cond.end#
        return false;
    }

    public int getMinScreenMetrics() {
        return nativeCanvas.getMinScreenMetrics();
    }

    public int getScreenWidth() {
        return nativeCanvas.getWidth();
    }
    public int getScreenHeight() {
        return nativeCanvas.getHeight();
    }

}
