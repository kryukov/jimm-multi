/*
 * Scheme.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui.base;

import DrawControls.icons.ImageList;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;

/**
 *
 * Warning! This code used hack.
 * Current scheme not cloned (the reference to the base scheme is used),
 * but current scheme content will be rewritten, when current scheme is changed.
 *
 * @author Vladimir Krukov
 */
public class Scheme {

    /**
     * Creates a new instance of Scheme
     */
    private Scheme() {
    }

    public static final Image backImage = ImageList.loadImage("/back.png");
    public static final Image captionImage = ImageList.loadImage("/caption.png");
    public static final Image softbarImage = ImageList.loadImage("/softbar.png");

    private static final int[] baseTheme = {
        0xFFFFFF, 0x000000, 0xF0F0F0, 0x000000, 0x0000FF,
        0xFF0000, 0x0000FF, 0x808080, 0x000000, 0x0000FF,
        0x404040, 0x808080, 0x808080, 0x0000FF, 0xE0E0E0,
        0x006FB1, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
        0x000000, 0xFFFFFF, 0x000000, 0x0000FF, 0x000000,
        0xFF0000, 0x0000FF, 0x000000, 0x606060, 0xD0D0D0,
        0x202020, 0x202020, 0xC0F0C0, 0xD0D0D0, 0x000000,
        0x606060, 0x202020, 0xD0D0D0, 0x202020, 0x000000,
        0x800000, 0xC0C0FF, 0x808080, 0x000000, 0xF0F0F0,
        0x0000FF, 0x000000, 0x0000FF, 0xFFFFFF,
        0xFFE7BA, 0xBFEFFF, 0xEED8AE, 0xB2DFEE, 0xFFA54F, 0xF8F8FF};

    private static int[] currentTheme = new int[baseTheme.length];
    private static int[][] themeColors;
    private static String[] themeNames;

    public static void load() {
        setColorScheme(baseTheme);

        Vector themes = new Vector();
        try {
            String content = Config.loadResource("/themes.txt");
            Config.parseIniConfig(content, themes);
        } catch (Exception ignored) {
        }
        themeNames  = new String[themes.size() + 1];
        themeColors = new int[themes.size() + 1][];

        themeNames[0]  = "Black on white (default)";
        themeColors[0] = baseTheme;
        for (int i = 0; i < themes.size(); ++i) {
            Config config = (Config)themes.elementAt(i);
            themeNames[i + 1]  = config.getName();
            themeColors[i + 1] = configToTheme(config);
        }
    }

    private static int[] configToTheme(Config config) {
        String[] keys = config.getKeys();
        String[] values = config.getValues();
        int[] theme = new int[baseTheme.length];
        System.arraycopy(baseTheme, 0, theme, 0, theme.length);
        try {
            for (int keyIndex = 0; keyIndex < keys.length; ++keyIndex) {
                int index = Util.strToIntDef(keys[keyIndex], -1);
                if ((0 <= index) && (index < theme.length)) {
                    theme[index] = Integer.parseInt(values[keyIndex].substring(2), 16);
                    if (0 == index) {
                        theme[48] = theme[0];
                        theme[49] = theme[50] = theme[51] = theme[52] = theme[53] = theme[54] = theme[0];
                    } else if (1 == index) {
                        theme[46] = theme[41] = theme[40] = theme[39] = theme[1];
                    } else if (2 == index) {
                        theme[44] = theme[2];
                    } else if (4 == index) {
                        theme[45] = theme[4];
                    } else if (13 == index) {
                        theme[47] = theme[13];
                    } else if (10 == index) {
                        theme[42] = theme[10];
                    } else if (39 == index) {
                        theme[43] = theme[39];
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return theme;
    }

    /**
     * Retrieves color value from color scheme
     */
    public static int[] getScheme() {
        return currentTheme;
    }

    /* Retrieves color value from color scheme */
    public static String[] getSchemeNames() {
        return themeNames;
    }

    public static void setColorScheme(int schemeNum) {
        if (themeNames.length <= schemeNum) {
            schemeNum = 0;
        }
        Options.setInt(Options.OPTION_COLOR_SCHEME, schemeNum);
        setColorScheme(themeColors[schemeNum]);
    }
    private static void setColorScheme(int[] scheme) {
        System.arraycopy(scheme, 0, currentTheme, 0 , currentTheme.length);
    }
}