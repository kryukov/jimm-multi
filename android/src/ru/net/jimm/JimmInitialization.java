package ru.net.jimm;

import org.microemu.microedition.ImplementationInitialization;

/**
 * @author Totktonada
 */

public class JimmInitialization implements ImplementationInitialization {

    /*
     * (non-Javadoc)
     *
     * @see org.microemu.microedition.ImplementationInitialization#registerImplementation()
     */
    public void registerImplementation() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.microemu.microedition.ImplementationInitialization#notifyMIDletStart()
     */
    public void notifyMIDletStart() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.microemu.microedition.ImplementationInitialization#notifyMIDletDestroyed()
     */
    public void notifyMIDletDestroyed() {
        JimmActivity.getInstance().notifyMIDletDestroyed();
        JimmActivity.getInstance().finish();
    }

}
