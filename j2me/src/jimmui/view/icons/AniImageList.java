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
 File: src/DrawControls/AniImageList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Vladimir Kryukov
 *******************************************************************************/
/*
 * AniImageList.java
 *
 * Created on 4 Апрель 2008 г., 18:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimmui.view.icons;

import java.io.InputStream;
// #sijapp cond.if modules_ANISMILES is "true" #

/**
 *
 * @author vladimir
 */
public class AniImageList extends ImageList {

    public AniImageList() {
    }

    private String getAnimationFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".png";
    }

    public void load(String resName, int w, int h) {
        try {
            InputStream is = jimm.Jimm.getResourceAsStream(resName + "/animate.bin");
            int smileCount = is.read() + 1;

            AniIcon[] icons = new AniIcon[smileCount];
            ImageList image = new ImageList();
            for (int smileNum = 0; smileNum < smileCount; ++smileNum) {
                int imageCount = is.read();
                int frameCount = is.read();
                image.load(getAnimationFile(resName, smileNum), imageCount);
                boolean loaded = (0 < image.size());
                AniIcon icon = loaded ? new AniIcon(image.iconAt(0), frameCount) : null;
                for (int frameNum = 0; frameNum < frameCount; ++frameNum) {
                    int iconIndex = is.read();
                    int delay = is.read() * Animation.WAIT_TIME;
                    if (loaded) {
                        icon.addFrame(frameNum, image.iconAt(iconIndex), delay);
                    }
                }
                icons[smileNum] = icon;
                if (loaded) {
                    width = Math.max(width, icon.getWidth());
                    height = Math.max(height, icon.getHeight());
                }
            }
            this.icons = icons;
            if (size() > 0) {
                new Animation(icons).start();
            }
        } catch (Exception ignored) {
        }
    }

}
// #sijapp cond.end #
