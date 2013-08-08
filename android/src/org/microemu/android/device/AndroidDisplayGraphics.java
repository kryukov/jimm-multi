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
 *  @version $Id: AndroidDisplayGraphics.java 2522 2011-11-28 13:44:59Z barteo@gmail.com $
 */

package org.microemu.android.device;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

import android.graphics.*;
import org.microemu.log.Logger;

public class AndroidDisplayGraphics extends javax.microedition.lcdui.Graphics {
	
    public final Paint strokePaint = new Paint();
    
    public final Paint fillPaint = new Paint();
    
    public AndroidFont androidFont;
    
	private static final DashPathEffect dashPathEffect = new DashPathEffect(new float[] { 5, 5 }, 0);
	
	private Canvas canvas;
	
	private Rect clip;

    protected Rect display = new Rect();

	private Font font;

	private int strokeStyle = SOLID;

	public AndroidDisplayGraphics() {
		strokePaint.setAntiAlias(true);
		strokePaint.setStyle(Paint.Style.STROKE);
		fillPaint.setAntiAlias(true);
		fillPaint.setStyle(Paint.Style.FILL);
	}

    public AndroidDisplayGraphics(Bitmap bitmap) {
        this.canvas = new Canvas(bitmap);
        this.canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		strokePaint.setAntiAlias(true);
		strokePaint.setStyle(Paint.Style.STROKE);
		fillPaint.setAntiAlias(true);
		fillPaint.setStyle(Paint.Style.FILL);

        reset(this.canvas);
    }

	public final void reset(Canvas canvas) {
	    this.canvas = canvas;

		Rect tmp = this.canvas.getClipBounds();
		this.canvas.clipRect(tmp, Region.Op.REPLACE);
		clip = this.canvas.getClipBounds();
		setFont(Font.getDefaultFont());
	}

	public void clipRect(int x, int y, int width, int height) {
        canvas.clipRect(x, y, x + width, y + height);
        canvas.clipRect(x, y, Math.min(canvas.getWidth(), x + width), Math.min(canvas.getHeight(), y + height));
		clip = canvas.getClipBounds();
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
	    RectF rect = new RectF(x, y, x + width, y + height);
	    canvas.drawArc(rect, -startAngle, -arcAngle, false, strokePaint);
    }

	public void drawImage(Image img, int x, int y, int anchor) {
        int newx = x;
        int newy = y;

        if (anchor == 0) {
            anchor = javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT;
        }

        if ((anchor & javax.microedition.lcdui.Graphics.RIGHT) != 0) {
            newx -= img.getWidth();
        } else if ((anchor & javax.microedition.lcdui.Graphics.HCENTER) != 0) {
            newx -= img.getWidth() / 2;
        }
        if ((anchor & javax.microedition.lcdui.Graphics.BOTTOM) != 0) {
            newy -= img.getHeight();
        } else if ((anchor & javax.microedition.lcdui.Graphics.VCENTER) != 0) {
            newy -= img.getHeight() / 2;
        }

        if (img.isMutable()) {
            canvas.drawBitmap(((AndroidMutableImage) img).getBitmap(), newx, newy, strokePaint);
        } else {
            canvas.drawBitmap(((AndroidImmutableImage) img).getBitmap(), newx, newy, strokePaint);
        }
    }

	public void drawLine(int x1, int y1, int x2, int y2) {
        if (x1 == x2) {
            canvas.drawLine(x1, y1, x2, y2 + 1, strokePaint);
        } else if (y1 == y2) {
            canvas.drawLine(x1, y1, x2 + 1, y2, strokePaint);
        } else {
            canvas.drawLine(x1, y1, x2 + 1, y2 + 1, strokePaint);
        }
	}

	public void drawRect(int x, int y, int width, int height) {
        canvas.drawRect(x, y, x + width, y + height, strokePaint);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		canvas.drawRoundRect(new RectF(x, y, x + width, y + height), (float) arcWidth, (float) arcHeight, strokePaint);
   }

	public void drawString(String str, int x, int y, int anchor) {
		drawSubstring(str, 0, str.length(), x, y, anchor);
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        int newx = x;
        int newy = y;

        if (anchor == 0) {
            anchor = javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT;
        }

        if ((anchor & javax.microedition.lcdui.Graphics.TOP) != 0) {
            newy -= androidFont.metrics.ascent;
        } else if ((anchor & javax.microedition.lcdui.Graphics.BOTTOM) != 0) {
            newy -= androidFont.metrics.descent;
        }
        if ((anchor & javax.microedition.lcdui.Graphics.HCENTER) != 0) {
            newx -= androidFont.paint.measureText(str) / 2;
        } else if ((anchor & javax.microedition.lcdui.Graphics.RIGHT) != 0) {
            newx -= androidFont.paint.measureText(str);
        }

        androidFont.paint.setColor(strokePaint.getColor());

        canvas.drawText(str, offset, len + offset, newx, newy, androidFont.paint);
	}

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
	    RectF rect = new RectF(x, y, x + width, y + height);
	    canvas.drawArc(rect, -startAngle, -arcAngle, true, fillPaint);
    }

	public void fillRect(int x, int y, int width, int height) {
        canvas.drawRect(x, y, x + width, y + height, fillPaint);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		canvas.drawRoundRect(new RectF(x, y, x + width, y + height), (float) arcWidth, (float) arcHeight, fillPaint);
    }

	public int getClipHeight() {
		return clip.bottom - clip.top;
	}

	public int getClipWidth() {
		return clip.right - clip.left;
	}

	public int getClipX() {
		return clip.left;
	}

	public int getClipY() {
		return clip.top;
	}

	public int getColor() {
		return strokePaint.getColor();
	}

	public Font getFont() {
		return font;
	}

	public int getStrokeStyle() {
		return strokeStyle;
	}

	public void setClip(int x, int y, int width, int height) {
        width = Math.min(display.right, x + width) - x;
        height = Math.min(display.bottom, y + height) - y;
		if (x == clip.left && x + width == clip.right && y == clip.top && y + height == clip.bottom) {
			return;
		}

        clip.left = x;
        clip.top = y;
        clip.right = x + width;
        clip.bottom = y + height;
		canvas.clipRect(clip, Region.Op.REPLACE);
	}

	public void setColor(int RGB) {
        if (0 == (0xFF000000 & RGB)) RGB = 0xff000000 | RGB;
		strokePaint.setColor(RGB);
		fillPaint.setColor(RGB);
	}

	public void setFont(Font font) {
		this.font = font;

        androidFont = AndroidFontManager.getFont(font);

	}

	public void setStrokeStyle(int style) {
		if (style != SOLID && style != DOTTED) {
			throw new IllegalArgumentException();
		}

		this.strokeStyle = style;

		if (style == SOLID) {
			strokePaint.setPathEffect(null);
			fillPaint.setPathEffect(null);
		} else { // DOTTED
			strokePaint.setPathEffect(dashPathEffect);
			fillPaint.setPathEffect(dashPathEffect);
		}
	}

    public void translate(int x, int y) {
        canvas.translate(x, y);

        super.translate(x, y);

        clip.left -= x;
        clip.right -= x;
        clip.top -= y;
        clip.bottom -= y;
    }

	public void drawRGB(int[] rgbData, int offset, int scanlength, int x,
			int y, int width, int height, boolean processAlpha) {
        if (rgbData == null)
            throw new NullPointerException();

        if (width == 0 || height == 0) {
            return;
        }

        int l = rgbData.length;
        if (width < 0 || height < 0 || offset < 0 || offset >= l || (scanlength < 0 && scanlength * (height - 1) < 0)
                || (scanlength >= 0 && scanlength * (height - 1) + width - 1 >= l)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        // MIDP allows almost any value of scanlength, drawBitmap is more strict with the stride
        if (scanlength == 0) {
        	scanlength = width;
        }
        int rows = rgbData.length / scanlength;
        if (rows < height) {
        	height = rows;
        }

       	canvas.drawBitmap(rgbData, offset, scanlength, x, y, width, height, processAlpha, strokePaint);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x1, y1);
        canvas.drawPath(path, fillPaint);
    }

	public void copyArea(int x_src, int y_src, int width, int height,
			int x_dest, int y_dest, int anchor) {
		Logger.debug("copyArea");
	}

    public void fillGradient(int x, int y, int width, int height, int clr1, int clr2) {
        if (0 == (0xFF000000 & clr1)) clr1 = 0xff000000 | clr1;
        if (0 == (0xFF000000 & clr2)) clr2 = 0xff000000 | clr2;
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, 0, 0, height, clr1, clr2, Shader.TileMode.MIRROR));
        canvas.drawRect(x, y, x + width, y + height, p);
    }

	public int getDisplayColor(int color) {
		Logger.debug("getDisplayColor");

		return -1;
	}

    public void setCanvasSize(int width, int height) {
        display.top = 0;
        display.left = 0;
        display.bottom = height;
        display.right = width;
    }
}
