package org.microemu.android.device.ui;

import org.microemu.device.ui.CommandUI;
import javax.microedition.lcdui.Command;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 12.08.12 20:39
 *
 * @author vladimir
 */
public class Commands extends Vector<AndroidCommandUI> {
    public Commands() {
    }
    public Commands(Commands c) {
        super(c);
    }

    public CommandUI getBackCommand() {
        CommandUI cmd = getFirstCommandOfType(Command.BACK);
        if (null == cmd) {
            cmd = getFirstCommandOfType(Command.EXIT);
        }
        if (null == cmd) {
            cmd = getFirstCommandOfType(Command.CANCEL);
        }
        return cmd;
    }

    private CommandUI getFirstCommandOfType(int commandType) {
        for (CommandUI cmd : this) {
            if (cmd.getCommand().getCommandType() == commandType) {
                return cmd;
            }
        }
        return null;
    }
}
