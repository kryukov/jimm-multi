/**
 *  MicroEmulator
 *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id: AndroidDisplayableUI.java 2365 2010-04-12 20:18:01Z barteo@gmail.com $
 */

package org.microemu.android.device.ui;

import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.microemu.android.MicroEmulatorActivity;
import org.microemu.device.ui.DisplayableUI;

public abstract class AndroidDisplayableUI<T extends Displayable> implements DisplayableUI {
	
	protected MicroEmulatorActivity activity;
	
	protected T displayable;


	private CommandListener commandListener = null;
	
	protected AndroidDisplayableUI(MicroEmulatorActivity activity, T displayable) {
		this.activity = activity;
		this.displayable = displayable;
		
	}

	public CommandListener getCommandListener() {
		return commandListener;
	}
	
	//
	// DisplayableUI
	//

	public void setCommandListener(CommandListener l) {
		this.commandListener = l;
	}

	public void invalidate() {
	}

	public abstract void showNotify();

	public void hideNotify() {
	}

    public Displayable getDisplayable() {
        return displayable;
    }

}
