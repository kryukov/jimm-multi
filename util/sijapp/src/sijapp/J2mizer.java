package sijapp;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 29.06.13 13:49
 *
 * @author vladimir
 */
public class J2mizer {
    public String j2mize(String line) {
        return removeGenerics(removeOverride(line));
    }

    private String removeOverride(String line) {
        if (startsWith(line, "@Override")) {
            line = line.replace("@Override", "");
        }
        return line;
    }
    private String removeGenerics(String line) {
        line = removeGeneric(line, "Vector");
        return line;
    }
    private String removeGeneric(String line, String type) {
        String newLine = removeOneGeneric(line, type);
        while (newLine != line) {
            line = newLine;
            newLine = removeOneGeneric(line, type);
        }
        return line;
    }
    private String removeOneGeneric(String line, String type) {
        int start = line.indexOf(type + "<");
        if (0 <= start) {
            int count = 0;
            for (int i = start + type.length(); i < line.length(); ++i) {
                if ('<' == line.charAt(i)) count++;
                if ('>' == line.charAt(i)) {
                    count--;
                    if (count == 0) {
                        int end = i + 1;
                        if (end == line.length()) return line.substring(0, start + type.length());
                        return line.substring(0, start + type.length())
                                + line.substring(end);
                    }
                }
            }
        }
        return line;
    }

    private boolean startsWith(String what, String with) {
        for (int i = 0; i < what.length(); ++i) {
            if (!Character.isSpaceChar(what.charAt(i))) return what.startsWith(with, i);
        }
        return false;
    }
}
