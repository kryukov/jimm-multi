/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/DrawControls/VirtualTree.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package jimmui.view.roster;

import jimm.util.JLocale;
import jimmui.updater.RosterUpdater;
import jimmui.view.base.touch.*;

import jimm.*;
import jimmui.view.base.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class VirtualContactList extends SomeContentList {

    public VirtualContactList() {
        super("");
        RosterContent rc = new RosterContent(this);
        rc.setModel(rc.getUpdater().createModel());
        content = rc;
        // #sijapp cond.if modules_TOUCH is "true"#
        softBar = new RosterToolBar();
        // #sijapp cond.end #
        softBar.setSoftBarLabels("menu", "context_menu", "context_menu", false);
        updateOption();
    }
    public ContactListModel getModel() {
        return ((RosterContent)content).getModel();
    }
    public void setCLListener(ContactListListener listener) {
        ((RosterContent)content).setCLListener(listener);
    }

    public void setModel(ContactListModel model) {
        ((RosterContent)content).setAllToTop();
        ((RosterContent)content).setModel(model);
        updateTitle();
        invalidate();
    }

    // #sijapp cond.if modules_TOUCH is "true"#
    protected void stylusXMoved(TouchState state) {
        if (getWidth() / 2 < Math.abs(state.fromX - state.x)) {
            boolean isTrue = state.fromX < state.x;
            int currentModel = 0;
            if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) currentModel = 1;
            if (((RosterContent)content).getModel() == getUpdater().getChatModel()) currentModel = 2;
            currentModel = (currentModel + 3 + (isTrue ? -1 : +1)) % 3;
            switch (currentModel) {
                case 0:
                    Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
                    updateOfflineStatus();
                    Options.safeSave();
                    break;
                case 1:
                    Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, true);
                    updateOfflineStatus();
                    Options.safeSave();
                    break;
                case 2:
                    ((RosterContent)content).setModel(getUpdater().getChatModel());
                    break;
            }
            updateTitle();
            Jimm.getJimm().getCL().activate();
        }
    }
    // #sijapp cond.end#

    public void updateOfflineStatus() {
        getUpdater().getModel().hideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
        ((RosterContent)content).setModel(getUpdater().getModel());
    }

    protected void restoring() {
        content.setTopByOffset(content.getTopOffset());
        ((RosterContent)content).update();
    }
    public final void update() {
        ((RosterContent)content).update();
    }

    public void updateOption() {
        ((RosterContent)content).updateOption();
    }


    public final void setActiveContact(Contact cItem) {
        ((RosterContent)content).setActiveContact(cItem);
    }

    //Updates the title of the list
    public void updateTitle() {
        String text = "Jimm Multi";
        if (((RosterContent)content).getModel() == getUpdater().getChatModel()) {
            text = JLocale.getString("chats");
        }
        bar.setCaption(text);
    }

    public final Protocol getCurrentProtocol() {
        return ((RosterContent)content).getCurrentProtocol();
    }

    public RosterUpdater getUpdater() {
        return ((RosterContent)content).getUpdater();
    }
}
