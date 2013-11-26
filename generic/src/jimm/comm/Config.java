/*
 * Config.java
 *
 * Created on 17 Октябрь 2009 г., 18:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.comm;

import java.util.Vector;

/**
 *
 * @author Vladimir Krukov
 */
public final class Config {
    private String name;
    private String[] keys;
    private String[] values;
    public Config(String name, Vector<String> keys, Vector<String> values) {
        this.name = name;
        this.keys = vectorToArray(keys);
        this.values = vectorToArray(values);
    }

    public Config load(String path) {
        return (Config)parseIni(new Tokenizer(path, false), new Vector<Config>()).elementAt(0);
    }
    public Config loadLocale(String path) {
        return (Config)parseIni(new Tokenizer(path, true), new Vector<Config>()).elementAt(0);
    }

    public static void parseIniConfig(String path, Vector<Config> configs) {
        parseIni(new Tokenizer(path, false), configs);
    }

    /** Creates a new instance of Options */
    public Config() {
    }
    public Config(String content) {
        Config cfg = load(content);
        keys = cfg.getKeys();
        values = cfg.getValues();
    }

    private String[] vectorToArray(Vector v) {
        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }

    public final String getName() {
        return name;
    }
    public final String[] getKeys() {
        return keys;
    }
    public final String[] getValues() {
        return values;
    }

    public final String getValue(String key) {
        for (int i = 0; i < keys.length; ++i) {
            if (key.equals(keys[i])) return values[i];
        }
        return null;
    }

    public static String getConfigValue(String key, String path) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        Tokenizer t = new Tokenizer(path, false);
        char ch = t.nextChat();
        while (!t.isNotEof()) {
            if (';' == ch) {
                skipLine(t, ch);
            } else {
                if (key.equals(readKey(t, ch, '='))) {
                    return readKey(t, ch, '\n');
                }
                skipLine(t, t.nextChat());
            }
            ch = t.nextChat();
        }
        return null;
    }

    public static Vector<Config> parseIni(Tokenizer t, Vector<Config> configs) {
        char ch = t.nextChat();
        String section = "default section";
        Vector<String> keys = new Vector<String>();
        Vector<String> values = new Vector<String>();

        while (!t.isNotEof()) {
            ch = skipSpace(t, ch);
            if ('[' == ch) {
                if (0 < keys.size()) {
                    configs.addElement(new Config(section, keys, values));
                    keys = new Vector<String>();
                    values = new Vector<String>();
                }
                section = readHeader(t);

            } else if (';' == ch) {
                skipLine(t, ch);

            } else if ('\n' == ch) {
                // skip
                skipSpace(t, ch);

            } else {
                String key = readKey(t, ch, '=');
                ch = skipSpace(t, t.nextChat());
                String value = readKey(t, ch, '\n');
                keys.addElement(key);
                values.addElement(value);
            }
            ch = t.nextChat();
        }
        configs.addElement(new Config(section, keys, values));
        return  configs;
    }

    private static String readHeader(Tokenizer t) {
        StringBuilder sb = new StringBuilder();
        char ch = t.nextChat();
        while (']' != ch && '\n' != ch) {
            sb.append(ch);
            ch = t.nextChat();
        }
        skipLine(t, ch);
        return sb.toString();
    }

    private static char skipSpace(Tokenizer t, char ch) {
        while (' ' == ch) {
            ch = t.nextChat();
        }
        return ch;
    }
    private static void skipLine(Tokenizer t, char ch) {
        while ('\n' != ch) {
            ch = t.nextChat();
        }
    }
    private static String readKey(Tokenizer t, char ch, char endChar) {
        StringBuilder sb = new StringBuilder();
        while (endChar != ch && '\n' != ch) {
            if ('\\' == ch) {
                ch = t.nextChat();
                if ('n' == ch) ch = '\n';
                sb.append(ch);
            } else {
                sb.append(ch);
            }
            ch = t.nextChat();
        }
        return sb.toString().trim();
    }
}
