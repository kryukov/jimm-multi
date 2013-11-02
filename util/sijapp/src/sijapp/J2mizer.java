package sijapp;

import java.lang.Character;
import java.lang.String;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 29.06.13 13:49
 *
 * @author vladimir
 */
public class J2mizer {
    public String j2mize(String line) {
        return replaceForeach(replaceClasses(removeGenerics(removeOverride(line))));

    }
    private String replaceClasses(String line) {
        return replaceClass(line, "StringBuilder", "StringBuffer");
    }

    private String removeOverride(String line) {
        if (startsWith(line, "@Override")) {
            line = line.replace("@Override", "");
        }
        if (startsWith(line, "@Deprecated")) {
            line = line.replace("@Deprecated", "");
        }
        return line;
    }
    private String removeGenerics(String line) {
        line = removeGeneric(line, "Vector");
        line = removeGeneric(line, "Hashtable");
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

    private String replaceClass(String line, String from, String to) {
        return line.replaceAll(from, to);
    }

    private boolean startsWith(String what, String with) {
        for (int i = 0; i < what.length(); ++i) {
            if (!Character.isSpaceChar(what.charAt(i))) return what.startsWith(with, i);
        }
        return false;
    }

    private String ARRAY_FOREACH_TEMPLATE = "for\\s*\\(\\s*(\\w+)\\s+(\\w+)\\s*:\\s*((?:\\w+\\.)*\\w+)\\s*\\)\\s*\\{\\s*";
    private String J2ME_ARRAY_FOREACH_TEMPLATE = "for (int i_$2 = 0; i_$2 < $3.length; ++i_$2) { $1 $2 = $3[i_$2];";

    private String LIST_FOREACH_TEMPLATE = "for\\s*\\(\\s*(\\w+)\\s+(\\w+)\\s*:\\s*((?:\\w+\\.)*\\w+)\\s*\\)\\s*\\{\\s*";
    private String J2ME_LIST_FOREACH_TEMPLATE = "for (int i_$2 = 0; i_$2 < $3.size(); ++i_$2) { $1 $2 = $3.get(i_$2);";

    private Pattern ARRAY_FOREACH = Pattern.compile(ARRAY_FOREACH_TEMPLATE);
    private String replaceForeach(String line) {
        return ARRAY_FOREACH.matcher(line).replaceAll(J2ME_ARRAY_FOREACH_TEMPLATE);
    }
}
