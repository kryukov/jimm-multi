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
 File: src/DrawControls/AniIcon.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Vladimir Kryukov
 *******************************************************************************/
/*
 * GifIcon.java
 *
 * Created on 4 Апрель 2008 г., 19:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.icons;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
// #sijapp cond.if modules_ANISMILES is "true" #

/**
 *
 * @author vladimir
 */
public class AniIcon extends Icon {
    private Icon[] frames;
    private int[] delays;
    private int currentFrame = 0;
    /** Creates a new instance of GifIcon */

    public AniIcon(Icon icon, int frameCount) {
        super(icon.getImage(), 0, 0, icon.getWidth(), icon.getHeight());
        frames = new Icon[frameCount];
        delays = new int[frameCount];
    }
    protected Image getImage() {
        return frames[currentFrame].getImage();
    }
    void addFrame(int num, Icon icon, int dalay) {
        frames[num] = icon;
        delays[num] = dalay;
    }
    public void drawByLeftTop(Graphics g, int x, int y) {
        frames[currentFrame].drawByLeftTop(g, x, y);
        painted = true;
    }
    private boolean painted = false;
    private long sleepTime = 0;
    boolean nextFrame(long deltaTime) {
        sleepTime -= deltaTime;
        if (sleepTime <= 0) {
            currentFrame = (currentFrame + 1) % frames.length;
            sleepTime = delays[currentFrame];
            boolean needReepaint = painted;
            painted = false;
            return needReepaint;
        }
        return false;
    }
    
}
// #sijapp cond.end #
