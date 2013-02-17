/**
 *
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import java.util.Date;
import java.util.Vector;
import jimm.comm.StringConvertor;
import jimm.comm.Util;

/**
 * @author Dejan Sakel?ak
 *
 */
public class Utils {
    private static String[][] url_encode_map_unsafe ={
        {" ","%20"},{"!","%21"},{"\"","%22"},{"#","%23"},{"$","%24"},{"%","%25"},{"&","%26"},{"'","%27"},{"(","%28"},{")","%29"},
        {"*","%2A"},{"+","%2B"},{",","%2C"},{"/","%2F"},
        {":","%3A"},{";","%3B"},{"<","%3C"},{"=","%3D"},
        {">","%3E"},{"?","%3F"},{"@","%40"},
        {"[","%5B"},{"\\","%5C"},{"]","%5D"},{"^","%5E"},{"`","%60"},
        {"{","%7B"},{"|","%7C"},{"}","%7D"},{"~","%7E"}, {".", "%2E"}
    };
    
    private static String[] normalChars;
    private static String[] codingChars;
    private static String normalKeys;
    private static String codingKeys;
    static {
        normalChars = new String[url_encode_map_unsafe.length];
        codingChars = new String[url_encode_map_unsafe.length];
        StringBuffer nkeys = new StringBuffer();
        StringBuffer ckeys = new StringBuffer();
        for (int i=0; i<url_encode_map_unsafe.length; ++i) {
            normalChars[i] = url_encode_map_unsafe[i][0];
            codingChars[i] = url_encode_map_unsafe[i][1];
            nkeys.append(normalChars[i].charAt(0));
            ckeys.append(codingChars[i].charAt(0));
        }
        normalKeys = nkeys.toString();
        codingKeys = ckeys.toString();
    }

    /**
     * Simple ASCII url encoder.
 
     * @return result String url encoded string
 
     * @param String s - string to be encode
     */
    
    public static String urlEncode(String s) {
        return Util.replace(s, normalChars, codingChars, normalKeys);
    }
    
    /**
     * Simple ASCII url decoder.
 
     * @return result String url decoded string
 
     * @param String s - string to decode
     */
    
    public static String urlDecode(String s) {
        if (null == s) return null;
        return Util.replace(s, codingChars, normalChars, codingKeys);
    }
    
    /** XML-like easy parser methods*/
    
    private static int _fromIndex = 0; //static variable to keep tag search position
    
    public static String getStringForTag(String Tag, String strTmp) {
        return getStringForTag(Tag,strTmp,true);
    }
    public static String getStringForTag(String Tag, String strTmp, boolean newSearch) {
        int tag1, tag2;
        String untaggedString = null;
        String beginTag = "<"+Tag+">";
        String endTag = "</"+Tag+">";
        if (newSearch) _fromIndex = 0;
        if (strTmp != null) {
            tag1 = strTmp.indexOf(beginTag, _fromIndex);
            tag2 = strTmp.indexOf(endTag, _fromIndex);
            if (tag1 !=-1 && tag2 !=-1) {
                tag1 += beginTag.length();
                untaggedString = strTmp.substring(tag1, tag2);
                _fromIndex = tag2 + endTag.length();
            }
        }
        return untaggedString;
    }
    
    public static String getStringBetweenTags(String Tag, String ClosingTag, String strTmp) {
        int tag1, tag2;
        String untaggedString = null;
        String beginTag = Tag;
        String endTag = ClosingTag;
        if (strTmp != null) {
            tag1 = strTmp.indexOf(beginTag, 0);
            tag2 = strTmp.indexOf(endTag, tag1);
            if (tag1 !=-1 && tag2 !=-1) {
                tag1 += beginTag.length();
                untaggedString = strTmp.substring(tag1, tag2);
                _fromIndex = tag2 + endTag.length();
            }
        }
        return untaggedString;
    }
    
    /**
     * Tokenizes a string with the default delimiter(whitespace).
     * @return Array of tokens(strings).
     * @param String s String to be tokenized.
     * @param char delimiter Character that delimits the string.
     */
    public static String[] tokenize(String s) {
        return Util.explode(s, ' ');
    }
}
// #sijapp cond.end #