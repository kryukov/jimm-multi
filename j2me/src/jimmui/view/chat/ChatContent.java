package jimmui.view.chat;

import jimm.Jimm;
import jimmui.model.chat.ChatModel;
import jimmui.model.chat.MessData;
import jimmui.view.base.CanvasEx;
import jimmui.view.base.GraphicsEx;
import jimmui.view.base.NativeCanvas;
import jimmui.view.base.SomeContent;
import jimmui.view.icons.Icon;
import protocol.ui.InfoFactory;

import javax.microedition.lcdui.Font;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.07.13 21:44
 *
 * @author vladimir
 */
public class ChatContent implements SomeContent {
    private ChatModel model;

    public ChatContent(ChatModel model) {
        this.model = model;
    }

    @Override
    public int getSize() {
        return model.size();
    }

    @Override
    public int getItemHeight(int itemIndex) {
        return model.getItemHeight(model.getMessage(itemIndex));
    }

    @Override
    public void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        int header = model.getMessageHeaderHeight(mData);
        if (0 < header) {
            drawMessageHeader(g, mData, x, y, w, header);
            y += header;
            h -= header;
            skip -= header;
        }
        model.getMessage(index).par.paint(model.fontSet, g, 1, y, skip, to);
    }

    public void drawItemBack(GraphicsEx g, int index, int selected, int x, int y, int w, int h, int skip, int to) {
        MessData mData = model.getMessage(index);
        byte bg;
        if (mData.isMarked()) {
            bg = CanvasEx.THEME_CHAT_BG_MARKED;
        } else if (mData.isService()) {
            bg = CanvasEx.THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = mData.isIncoming() ? CanvasEx.THEME_CHAT_BG_IN : CanvasEx.THEME_CHAT_BG_OUT;
        } else {
            bg = mData.isIncoming() ? CanvasEx.THEME_CHAT_BG_IN_ODD : CanvasEx.THEME_CHAT_BG_OUT_ODD;
        }
        if (g.notEqualsColor(CanvasEx.THEME_BACKGROUND, bg)) {
            if (selected == index) {
                g.setThemeColor(CanvasEx.THEME_SELECTION_BACK, bg, 0xA0);
            } else {
                g.setThemeColor(bg);
            }
            g.fillRect(x, y + skip, w, to);
        }
    }

    private void drawMessageHeader(GraphicsEx g, MessData mData, int x1, int y1, int w, int h) {
        Icon icon = InfoFactory.msgIcons.iconAt(mData.iconIndex);
        if (null != icon) {
            int iconWidth = g.drawImage(icon, x1, y1, h) + 1;
            x1 += iconWidth;
            w -= iconWidth;
        }

        Font[] set = model.fontSet;
        Font boldFont = set[CanvasEx.FONT_STYLE_BOLD];
        g.setFont(boldFont);
        g.setThemeColor(getInOutColor(mData.isIncoming()));

        Font plainFont = set[CanvasEx.FONT_STYLE_PLAIN];
        String time = mData.isMarked() ? "  v  " : mData.strTime;
        int timeWidth = plainFont.stringWidth(time);

        g.drawString(mData.getNick(), x1, y1, w - timeWidth, h);

        g.setFont(plainFont);
        g.drawString(time, x1 + w - timeWidth, y1, timeWidth, h);
    }

    private byte getInOutColor(boolean incoming) {
        return incoming ? CanvasEx.THEME_CHAT_INMSG : CanvasEx.THEME_CHAT_OUTMSG;
    }

    @Override
    public void doJimmAction(int keyCode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
