package ru.net.jimm.config;

import jimm.comm.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.12.12 15:21
 *
 * @author vladimir
 */
public class Templates {
    private static final String TEMPLATES_FILE = "templates.ini";

    private static final Map<String, String> storageToIni = new HashMap<String, String>();
    static {
        storageToIni.put(jimm.modules.Templates.TEMPLATE_STORAGE, TEMPLATES_FILE);
    }

    public void store(String storageName, Vector<String> templates) {
        final IniBuilder sb = new IniBuilder();
        int num = 1;
        for (Object template : templates) {
            sb.line("" + num, template);
            num++;
        }
        HomeDirectory.putContent(storageToIni.get(storageName), sb.toString());
    }

    public Vector<String> load(String storageName, Vector<String> old) {
        Config config = new Config(HomeDirectory.getContent(storageToIni.get(storageName)));
        Vector<String> result = new Vector<String>();
        for (String tpl : config.getValues()) {
            result.add(IniBuilder.extract(tpl));
        }
        if (null != old) for (String tpl : old) {
            if (!result.contains(tpl)) result.add(tpl);
        }
        return result;
    }
}
