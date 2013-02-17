/*
 * PassportLoginNet.java
 *
 * Created 2007, Jan 28, 15:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;
import java.io.*;
import java.util.Hashtable;
import javax.microedition.io.*;
import jimm.comm.StringConvertor;
import jimm.comm.Util;

/**
 *
 * @author Ilya Danilov
 */
public class PassportLoginNet {
    //Official Passport 3.0 login server. Note: It gives "Error In HTTP operation" on S40 DP2.0 and DP3.0
    //static final String        TWNServer = "loginnet.passport.com"; //possible same as login.live.com
    
    //Supposed to be official Passport 3.0 login server. Better to use this server to avoid S40 errors
    static final String        TWNServer = "login.live.com"; //possible same as login.live.com
    static final String        TWNPage = "/RST.srf";
    private static String[] challengeParams ={"lc","id","tw","fs","ru","kpp","kv","ver"};
    
    private HttpsConnection httpsConn;
    private String loginServerURL;
    
    private String faultBody;
    
    
    /** Creates a new instance of PassportLoginNet */
    public PassportLoginNet() {
        try {
            httpsConn = (HttpsConnection)Connector.open("https:/" + "/"+TWNServer);
        } catch (IOException ex) {
        }
    }
    
    String proceedAuthorization(String twnserver, String twnpage, String body){
        OutputStream os = null; //stream to post the vars
        InputStream is = null;
        
        String resp = null;
        try  {
            this.loginServerURL = "https:/" + "/"+twnserver+twnpage;
            
            httpsConn = (HttpsConnection)Connector.open(this.loginServerURL);
            httpsConn.setRequestMethod(HttpConnection.POST);
            
            /* Ilya: in examples its recommended to use following headers. But it works without them and with them it gives "Bad header" for S40 DP20 and DP30
             * so i commented this
            httpsConn.setRequestProperty("Accept", "text/*");
            httpsConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            httpsConn.setRequestProperty("Host", twnserver);
            httpsConn.setRequestProperty("Content-Length", ""+body.length());
            httpsConn.setRequestProperty("Connection", "Keep-Alive");
            httpsConn.setRequestProperty("Cache-Control", "no-cache");
            /*/
            
            // Getting the output stream may flush the headers
            os = httpsConn.openOutputStream();
            os.write(StringConvertor.stringToByteArrayUtf8(body));
            
            //flush output
            int rc = httpsConn.getResponseCode();
            
            if (rc != HttpConnection.HTTP_OK) {
                System.err.println("HTTP response code: " + rc);
            }
            
            //read response
            int len = (int) httpsConn.getLength();
            is =  httpsConn.openDataInputStream();
            
            byte[] data = null;
            
            if (len > 0) {
                int actual = 0;
                int bytesread = 0 ;
                data = new byte[len];
                while ((bytesread != len) && (actual != -1)) {
                    actual = is.read(data, bytesread, len - bytesread);
                    bytesread += actual;
                }
            } else {
                int ch, cc=0;
                data = new byte[16384];
                while ((ch = is.read()) != -1) {
                    data[cc++]=(byte)ch;
                }
                // trim buffer
                byte[] tmpData = new byte[cc];
                System.arraycopy(data, 0, tmpData, 0, cc);
                data = tmpData;
            }
            resp = StringConvertor.utf8beByteArrayToString(data, 0, data.length);
            System.out.println("[DEBUG] "+resp);
        } catch(IOException e ){
            return null;
        } finally {
            try {
                if (os != null) os.close();
                if (is != null) is.close();
                if (httpsConn != null){
                    httpsConn.close();
                    httpsConn = null;
                }
            } catch (Exception e) {}
        }
        return resp;
    }
    /**
     * Request authorization ticket to login in passport
     * server.
     *
     * @param strUserName The login user name
     * @param strPassword The user password
     * @strChallenge The challenge string sent by passport server
     */
    String requestAuthorizationTicket(String strUserName, String strPassword, String strChallenge) {
        String ticket = null;
        if (null == strUserName) return null;
        if (null == strPassword) return null;
        if (null == strChallenge) return null;
        
        strChallenge = strChallenge.substring(0, strChallenge.length()-2);
        
        /**
         * Order of this parameters is important.
         * String tokenizer first parses all parameters from challenge
         * string and sorts them(if neccessary)
         */
        Hashtable challengeParamsHash = new Hashtable();
        String[] param = Util.explode(strChallenge,',');
        String[] tokens;
        for(int i = 0; i < param.length ; ++i) {
            tokens = Util.explode(param[i],'=');
            challengeParamsHash.put(tokens[0], tokens[1]);
        }
        strChallenge="";
        for(int i = 0; i < PassportLoginNet.challengeParams.length ; ++i) {
            if(challengeParamsHash.containsKey(PassportLoginNet.challengeParams[i])) {
                strChallenge = strChallenge.concat(((i==0)?"":",")
                        + PassportLoginNet.challengeParams[i] + "="
                        + challengeParamsHash.get(PassportLoginNet.challengeParams[i]));
            }
        }
        
        StringBuffer buf = new StringBuffer();
        StringBuffer buf2 = new StringBuffer();
        
        buf2.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buf2.append("<Envelope xmlns=\"http:/" + "/schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http:/" + "/schemas.xmlsoap.org/ws/2003/06/secext\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" xmlns:wsp=\"http:/" + "/schemas.xmlsoap.org/ws/2002/12/policy\" xmlns:wsu=\"http:/" + "/docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsa=\"http:/" + "/schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wssc=\"http:/" + "/schemas.xmlsoap.org/ws/2004/04/sc\" xmlns:wst=\"http:/" + "/schemas.xmlsoap.org/ws/2004/04/trust\"><Header>");
        buf2.append("<ps:AuthInfo xmlns:ps=\"http:/" + "/schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"PPAuthInfo\">");
        buf2.append("<ps:HostingApp>{7108E71A-9926-4FCB-BCC9-9A9D3F32E423}</ps:HostingApp>");
        buf2.append("<ps:BinaryVersion>4</ps:BinaryVersion>");
        buf2.append("<ps:UIVersion>1</ps:UIVersion>");
        buf2.append("<ps:Cookies></ps:Cookies>");
        buf2.append("<ps:RequestParams>AQAAAAIAAABsYwQAAAAzMDg0</ps:RequestParams>");
        buf2.append("</ps:AuthInfo>");
        buf2.append("<wsse:Security>");
        buf2.append("<wsse:UsernameToken Id=\"user\">");
        buf2.append("<wsse:Username>").append(Util.xmlEscape(strUserName)).append("</wsse:Username>");
        buf2.append("<wsse:Password>").append(Util.xmlEscape(strPassword)).append("</wsse:Password>");
        buf2.append("</wsse:UsernameToken>");
        buf2.append("</wsse:Security>");
        buf2.append("</Header>");
        buf2.append("<Body>");
        buf2.append("<ps:RequestMultipleSecurityTokens xmlns:ps=\"http:/" + "/schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"RSTS\">");
        buf2.append("<wst:RequestSecurityToken Id=\"RST0\">");
        buf2.append("<wst:RequestType>http:/" + "/schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>");
        buf2.append("<wsp:AppliesTo>");
        buf2.append("<wsa:EndpointReference>");
        buf2.append("<wsa:Address>http:/" + "/Passport.NET/tb</wsa:Address>");
        buf2.append("</wsa:EndpointReference>");
        buf2.append("</wsp:AppliesTo>");
        buf2.append("</wst:RequestSecurityToken>");
        buf2.append("<wst:RequestSecurityToken Id=\"RST1\">");
        buf2.append("<wst:RequestType>http:/" + "/schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>");
        buf2.append("<wsp:AppliesTo>");
        buf2.append("<wsa:EndpointReference>");
        buf2.append("<wsa:Address>messenger.msn.com</wsa:Address>");
        buf2.append("</wsa:EndpointReference>");
        buf2.append("</wsp:AppliesTo>");
        buf2.append("<wsse:PolicyReference URI=\"?");
        buf2.append(Util.xmlEscape(Util.replace(strChallenge, ",", "&")));
        buf2.append("\"></wsse:PolicyReference>");
        buf2.append("</wst:RequestSecurityToken>");
        buf2.append("</ps:RequestMultipleSecurityTokens>");
        buf2.append("</Body></Envelope>");
        
        String strAuthBody = buf2.toString();
        
        System.out.println("[DEBUG] " + strAuthBody);
        
        String t_twnserver = TWNServer;
        String t_twnpage = TWNPage;
        String xmlresponse = null;
        
        do {
            xmlresponse = proceedAuthorization(t_twnserver, t_twnpage, strAuthBody);
            
            //drop to null to exit from redirection loop
            t_twnserver = null;
            t_twnpage = null;
            
            if (xmlresponse != null) {
                //find either OK or FAILURE?
                faultBody = Utils.getStringForTag("S:Fault", xmlresponse);
                if (faultBody==null) { //no failure
                    
                    //get ticket
                    ticket = Utils.getStringBetweenTags("<wsse:BinarySecurityToken Id=\"Compact1\">","</wsse:BinarySecurityToken>", xmlresponse);
                    if (ticket != null) {
                        ticket = Util.xmlUnescape(ticket);
                    }
                } else {
                    //redirect or failure
                    String faultCode = Utils.getStringForTag("faultcode", faultBody);
                    if ("psf:Redirect".equals(faultCode)) {
                        String redirectUrl = Utils.getStringForTag("psf:redirectUrl", faultBody);
                        //Example: https:/" + "/login.live.com/pp400/RST.srf
                        redirectUrl = Util.replace(redirectUrl, "https:/" + "/","");
                        int uri_ind = redirectUrl.indexOf("/");
                        
                        //assign new values
                        t_twnserver = redirectUrl.substring(0, uri_ind);
                        t_twnpage = redirectUrl.substring(uri_ind);
                    } else {  //error
                        //... TO DO: add failure handler
                        //... Can be internal error, wrong username, password or any other error
                    }
                }
            }
        } while (t_twnserver != null && t_twnpage != null);
        return ticket;
    }
    
}
// #sijapp cond.end #