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
File: src/DrawControls/ImageList.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Artyomov Denis, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/
package DrawControls.icons;

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;

//! Class for dividing one big image to several small with equal size
/*!
This class allow you to reduce images number, stored at res folder. 
It can be uses only if all images have equal height and width. 
For example, if you want use 10 images with size 16 x 16, you can
store one 160 x 16 image and divide it with help of this class

\par Example
\code
ImageList images = new ImageList();
images.createImageList("/big160x16.png", 16);

// now you can retrive second image: 
Image img1 = images.iconAt(1);

\endcode
 */
public class ImageList {

    private Icon[] icons;
    protected int width = 0;
    protected int height = 0;
    // #sijapp cond.if modules_MULTI is "true" #
    private static Hashtable files = new Hashtable();
    // #sijapp cond.end #

    //! Return image by index
    public Icon iconAt(int index) { //!< Index of requested image in the list
        if (0 <= index && index < size()) {
            return icons[index];
        }
        return null;
    }

    //! Return number of stored images
    public int size() {
        return (null == icons) ? 0 : icons.length;
    }

    //! Return width of each image
    public int getWidth() {
        return width;
    }

    //! Return hright of each image
    public int getHeight() {
        return height;
    }

    // #sijapp cond.if modules_ANISMILES is "true" #
    public void load(String resName, int count) throws IOException {
        Image resImage = ImageList.loadImage(resName);
        if (null == resImage) {
            return;
        }
        int imgHeight = resImage.getHeight();
        int imgWidth = resImage.getWidth();
        width = imgWidth / count;
        height = imgHeight;

        Vector tmpIcons = new Vector();
        for (int y = 0; y < imgHeight; y += height) {
            for (int x = 0; x < imgWidth; x += width) {
                Icon icon = new Icon(resImage, x, y, width, height);
                tmpIcons.addElement(icon);
            }
        }
        icons = new Icon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
    }
    // #sijapp cond.end #
    //! Load and divide big image to several small and store it in object

    public void load(String resName, int width, int height) throws IOException {
        Image resImage = ImageList.loadImage(resName);
        if (null == resImage) {
            return;
        }
        int imgHeight = resImage.getHeight();
        int imgWidth = resImage.getWidth();

        if (width == -1) {
            width = Math.min(imgHeight, imgWidth);
        }
        if (height == -1) {
            height = imgHeight;
        }

        this.width = width;
        this.height = height;

        Vector tmpIcons = new Vector();
        for (int y = 0; y < imgHeight; y += height) {
            for (int x = 0; x < imgWidth; x += width) {
                Icon icon = new Icon(resImage, x, y, width, height);
                tmpIcons.addElement(icon);
            }
        }
        icons = new Icon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
    }

    static public ImageList createImageList(String resName) {
        // #sijapp cond.if modules_MULTI is "true" #
        ImageList imgs = (ImageList) files.get(resName);
        if (null != imgs) {
            return imgs;
        }
        // #sijapp cond.end #
        ImageList icons = new ImageList();
        try {
            icons.load(resName, -1, -1);
        } catch (Exception e) {
        }
        // #sijapp cond.if modules_MULTI is "true" #
        files.put(resName, icons);
        // #sijapp cond.end #
        return icons;
    }

    static public Image loadImage(String resName) {
        try {
            return Image.createImage(resName);
        } catch (Exception e) {
        } catch (OutOfMemoryError out) {
        }
        return null;
    }
}