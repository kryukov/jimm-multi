/* JIMMY - Instant Mobile Messenger
   Copyright (C) 2006  JIMMY Project

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
 **********************************************************************
 File: jimmy/MsnConnection.java
 Version: alpha  Date: 2006/04/11
 Author(s): Zoran Mesec
 */
// #sijapp cond.if protocols_MSN is "true" #
package protocol.msn;

import java.util.*;
import jimm.Jimm;
import jimm.JimmException;
import jimm.chat.ChatHistory;
import jimm.chat.message.PlainMessage;
import jimm.chat.message.SystemNotice;
import jimm.comm.MD5;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import protocol.*;
/**
 * This class is used to connect with a remote server using SocketConnection class.
 * @author Zoran Mesec
 */
public class MsnConnection implements  Runnable {
    final public static String CMD_CVR = "CVR";
    final public static String CMD_VER = "VER";
    final public static String CMD_USR = "USR";
    final public static String CMD_CHG = "CHG";
    final public static String CMD_XFR = "XFR";
    final public static String CMD_FLN = "FLN";

    final public static String CMD_ILN = "ILN";
    final public static String CMD_NLN = "NLN";
    final public static String CMD_ADG = "ADG";
    final public static String CMD_CHL = "CHL";
    final public static String CMD_RNG = "RNG";
    final public static String CMD_OUT = "OUT";
    final public static String CMD_LST = "LST";
    final public static String CMD_LSG = "LSG";
    final public static String CMD_MSG = "MSG";

    private String username;
    private String password;
    private boolean connected_;
    private boolean stop;
    private Thread thread_;

    private MSNTransaction tr;
    private MsnContact movingUser = null;
    private boolean busy = false;
    private final Hashtable contacts_ = new Hashtable();
    private final Hashtable commands = new Hashtable();

    private byte status_;
    public static final byte DISCONNECTED = 0; //disconnected, ok
    public static final byte CONNECTED = 1; //connected, ok
    public static final byte CONNECTING = 2; //connecting, busy
    public static final byte WRONG_PASSWORD = -1; //wrong password, error
    public static final byte NO_CONNECTION = -2; //connection cannot be established, error

    private ServerHandler sh;

    /**
     * A Vector of all ChatSession classes. Each ChatSession represents a conversation with a contact.
     */
    protected final Vector chatSessions_ = new Vector();	//list of active chat sessions
    /**
     * A Vector of all ServerHandler classes that are required for Chatsessions. MSN
     * Protocol has different IP number for every conversation between two users,
     * therefore a ServerHandler for each conversation is required.
     */
    //protected final Vector sessionHandlers_ = new Vector();	//list of IPs of alive chat sessions
    /**
     * This Hashtable maps Chatsessions and their Transaction IDs. With every message
     * the transaction ID of Chatsession increases.
     */
    private final Hashtable groupID = new Hashtable();

    // definitions of constants
    //const of protocol MSNP10
    final String NsURL = "messenger.hotmail.com";
    final String ProductKey = "YMM8C_H7KCQ2S_KL";
    final String ProductIDhash = "Q1P7W2E4J9R8U3S5";
    final String ProductID = "msmsgs@msnmsgr.com";
    //final String NSredirectURL = "207.46.114.22";

    final String ProductIDMSNP12 = "PROD0090YUAUV{2B"; //ilya

    private static final long MSNP11_MAGIC_NUM = 0x0E79A9C1;
    final int serverPort = 1863;
    final int NexusPort = 443;
    final String               DALOGIN                      = "DALogin=";
    final String               DASTATUS                     = "da-status=";
    final String               TICKET                       = "from-PP=";
    final String               SUCCESS                      = "success";
    final String               KEY_PASSPORT_URLS            = "PassportURLs";
    final String               KEY_LOCATION                 = "Location";
    final String               KEY_AUTHENTICATION_INFO      = "Authentication-Info";
    static final String        PASSPORT_LIST_SERVER_ADDRESS = "https://nexus.passport.com/rdr/pprdr.asp";


    private Msn msn;
    /**
     * The constructor method.
     * @param URL URL of the server. No protocolis specified here! Example: messenger.hotmail.com.
     * @param PORT PORT of the server(inputs as a String). Example: "1863".
     */
    public MsnConnection(Msn msn) {
        this.msn = msn;

        this.connected_ = false;
        this.busy = false;
        this.commands.put(CMD_ADG,"1");
        this.commands.put(CMD_LSG,"2");
        this.commands.put(CMD_LST,"3");
        this.commands.put(CMD_CHL,"4");
        this.commands.put(CMD_FLN,"5");
        this.commands.put(CMD_ILN,"6");
        this.commands.put(CMD_NLN,"7");
        this.commands.put(CMD_RNG,"8");
        this.commands.put(CMD_USR,"9");
        this.commands.put(CMD_VER,"10");
        this.commands.put(CMD_XFR,"11");
        this.commands.put(CMD_OUT,"12");
        this.commands.put(CMD_CHG,"13");
        this.commands.put(CMD_CVR,"14");

    }
    /**
     *
     * @see method login(String, String)
     */
    public boolean login() {
        this.status_ = CONNECTING;
        this.thread_ = new Thread(this);
        this.thread_.start();
        return true;
    }
    private void setProgress(int progress) {
        msn.setConnectingProgress(progress);
    }
    public static final String productKey = "CFHUR$52U_{VIX5T";
    public static final String productId = "PROD0101{0RM?UBW";
    private boolean connectToServer() throws JimmException {
        setProgress(10);
        {   // remove all chats
            Vector contacts = msn.getContactItems();
            for (int i = 0; i < contacts.size(); ++i) {
                Contact c = ((Contact)contacts.elementAt(i));
                if (c.hasChat()) {
                    ChatHistory.instance.unregisterChat(msn.getChat(c));
                }
            }
        }

        msn.setContactList(new Vector(), new Vector());
        this.username = msn.getUserId();
        this.password = msn.getPassword();
        try {
            this.tr = new MSNTransaction();
            this.tr.newTransaction();
            this.sh= new ServerHandler(this.NsURL, this.serverPort);
            this.sh.connect();

            this.tr.setType(CMD_VER);
            this.tr.addArgument("MSNP11");
            this.tr.addArgument("CVR0");
            //String message = "VER 1 MSNP8 CVR0\r\n";
            this.sh.sendRequest(this.tr.toString());
            //System.out.print(this.tr.toString());
            this.sh.getReply();
            setProgress(10);
            //message = "CVR 2 0x0409 win 4.10 i386 MSNMSGR 5.0.0544 MSMSGS avgustin.ocepek@yahoo.com.au\r\n"; //MSNP10
            //message = "CVR 2 0x040c winnt 5.1 i386 MSNMSGR 7.0.0813 msmsgs idanilov@ua.fm\r\n"; \\MSNP11
            //message = "CVR 2 0x0409 winnt 5.1 i386 MSNMSGR 7.5.0324 msmsgs idanilov@ua.fm\r\n"; \\MSNP12
            //message = "CVR 2 0x0409 winnt 5.1 i386 MSG80BETA 8.0.0566 msmsgs alice@hotmail.com\r\n"; \\MSNP13
            this.tr.newTransaction();
            this.tr.setType(CMD_CVR);
            this.tr.addArgument("0x040c winnt 5.1 i386 MSNMSGR 7.0.0813 msmsgs"); //MSNP11
            this.tr.addArgument(username);
            this.sh.sendRequest(this.tr.toString());
            this.sh.getReply();
            setProgress(20);

            //message="USR 3 TWN I avgustin.ocepek@yahoo.com.au\r\n";
            this.tr.newTransaction();
            this.tr.setType(CMD_USR);
            this.tr.addArgument("TWN I");
            this.tr.addArgument(this.username);

            this.sh.sendRequest(this.tr.toString());
            String data = this.sh.getReply();
            setProgress(30);

            String NSredirectURL = data.substring(data.indexOf("NS")+3, data.indexOf(" ", 10));
            this.sh.disconnect();
            setProgress(35);


            this.sh = new ServerHandler(NSredirectURL);
            try {
                this.sh.connect();
            } catch (Exception e) {
                return false;
            }
            setProgress(40);

            this.tr.newTransaction();
            this.tr.setType(CMD_VER);
            this.tr.addArgument("MSNP11");    //  only in v2 :)
            this.tr.addArgument("CVR0");
            this.sh.sendRequest(this.tr.toString());
            this.sh.getReply();
            //parseReply(this.sh.getReply());
            setProgress(45);

            this.tr.newTransaction();
            this.tr.setType(CMD_CVR);
            this.tr.addArgument("0x0409 win 5.1 i386 MSNMSGR 7.0.0813 msmsgs");
            this.tr.addArgument(this.username);
            this.sh.sendRequest(this.tr.toString());
            this.sh.getReply();
            //parseReply(this.sh.getReply());
            setProgress(50);

            this.tr.newTransaction();
            this.tr.setType(CMD_USR);
            this.tr.addArgument("TWN I");
            this.tr.addArgument(this.username);
            this.sh.sendRequest(this.tr.toString());
            //jimm.modules.DebugLog.println(this.tr.toString());
            String USRreply = this.sh.getReply();
            String challenge = USRreply.substring(12);
            setProgress(60);

            //this is where the password stuff fun starts

            // We have to establish a connection MSN Passport network
            // for authentification with user password.

            //its required for S40 handsets which has a bug with header length and due this unable to use Passport 1.4
            PassportLoginNet pn = new PassportLoginNet();
            String ticket = pn.requestAuthorizationTicket(this.username, this.password, challenge);

            if (null == ticket) {
                this.status_ = WRONG_PASSWORD;
                msn.setPassword(null);
                throw new JimmException(111, 0);
            }
            setProgress(70);

            this.tr.newTransaction();
            this.tr.setType(CMD_USR);
            this.tr.addArgument("TWN S");
            this.tr.addArgument(ticket);
            this.sh.sendRequest(this.tr.toString());
            this.sh.getReply();    //gets the SBS or anything else that is not important
            setProgress(80);

            this.tr.newTransaction();
            this.tr.setType("SYN");
            this.tr.addArgument("2007-08-14T06:03:32.863-07:00 2006-08-16T06:03:33.177-07:00");
            this.sh.sendRequest(this.tr.toString());
            this.sh.getReply();//SBS
            setProgress(90);

            parseMainReply(this.sh.getReply());//MSG

            String[] syn  = Util.explode(this.sh.getReply(), ' ');// SYN
            int groupNum = Util.strToIntDef(syn[syn.length - 2], 0);
            int contactNum = Util.strToIntDef(syn[syn.length - 1], 0);
            while ((0 < contactNum) && (0 < groupNum)) {
                String reply = this.sh.getReply();
                if (null == reply) {
                    continue;
                }
                parseMainReply(reply);
                if (reply.startsWith("LST")) {
                    contactNum--;
                } else if (reply.startsWith("LSG")) {
                    groupNum--;
                }
            }

            this.sh.sendRequest(getStatusPacket());
            setProgress(95);

            this.status_ = CONNECTED;
            this.stop = false;
        } catch (JimmException e) {
            throw e;
        } catch (Exception e) {
            this.status_ = NO_CONNECTION;
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("msn", e);
            // #sijapp cond.end #
            throw new JimmException(100, 0);
        }
        msn.setContactList(msn.getGroupItems(), msn.getContactItems());
        setProgress(100);
        return true;
    }
/*
BSY - Busy
IDL - Idle
BRB - Be Right Back
AWY - Away
LUN - Out to Lunch
PHN - On the Phone
FLN - Offline
HDN - Hidden
 */
    private String getStatus() {
        return CMD_NLN;
    }
    private String getStatusPacket() {
        this.tr.newTransaction();
        this.tr.setType(CMD_CHG);
        this.tr.addArgument(getStatus());
        //this.tr.addArgument("HDN"); //log in as anavailable(for testing)
        this.tr.addArgument("" + 0x01);
        return this.tr.toString();
    }

    /**
     * method disconnects the SocketConnection of this class.
     */
    public void logout() {
        this.status_ = DISCONNECTED;
        this.sh.sendRequest(this.tr.getLogoutString());

        this.stop = true;
        msn = null;
        //this.thread_.interrupt();
    }
    /**
     * Parses a reply from the server according to first three letters.
     * @param reply raw data from the server, presented as a string
     */
    public void parseReply(String reply) {
        if (reply == null) {
            return;
        }
        reply = reply.trim();
        if (reply.length() < 3) {
            return;
        }
        String cmd = (String)commands.get(reply.substring(0, 3));
        if (null == cmd) {
            return;
        }

        switch (Integer.parseInt(cmd)) {
            case 1: //ADG
                groupAdded(reply);
                break;

            case 2: //LSG
                parseGroups(reply);
                break;
            case 3: //LST
                parseContacts(reply);
                break;
            case 4: //CHL
                parseChallenge(reply);
                break;
            case 5: //FLN
                userGoesOffline(reply);
                break;
            case 6: //ILN
                parsePresence(reply);
                break;
            case 7: //NLN
                changePresence(reply);
                break;
            case 8: //RNG
                SBhandle(reply);
                break;
            case 11: //XFR
                startSession(reply);
                break;
            case 12: //OUT
                this.connected_ = false;
                this.status_ = DISCONNECTED;
                this.logout();
                break;
            default:
                //unknown command
                break;
        }
    }

    private boolean hasHeader(String msg, String header, int headerSize) {
        int index = msg.indexOf(header);
        return (-1 < index) && (index < headerSize);
    }
    private void processMessage(String msg, String userId, String nick, ChatSession activeCS) {
        int marker = msg.indexOf("\r\n\r\n");
        if (-1 == marker) marker = msg.length();
        if (hasHeader(msg, "TypingUser:", marker)) {
            // #sijapp cond.if modules_SOUND is "true" #
            msn.beginTyping(activeCS.c.getUserId(), true);
            // #sijapp cond.end #
            return;
        }
        if (hasHeader(msg, "Content-Type: text/x-mms-emoticon", marker)) {
            return;
        }

        String message = null;
        if (hasHeader(msg, "Content-Type: text/x-msnmsgr-datacast", marker)) {
            message = "/wakeup";

        } else if (hasHeader(msg, "Content-Type: text/plain", marker)) {
            if (marker + 4 < msg.length()) {
                message = msg.substring(marker + 4);
            }
        } else {
            if (marker + 4 < msg.length()) {
                message = "Unknown message\n" + msg.substring(marker + 4);
            }
        }

        if (!StringConvertor.isEmpty(message)) {
            PlainMessage m = new PlainMessage(activeCS.c.getUserId(), msn,
                    Jimm.getCurrentGmtTime(), message, false);
            if (!StringConvertor.isEmpty(nick)) {
                m.setName(nick);
            }

            msn.addMessage(m);
            return;
        }
    }
    private String readBody(ServerHandler sh, String line) {
        int lenght = Integer.parseInt(line.substring(line.lastIndexOf(' ') + 1));
        byte[] bytes = sh.getReplyBytes(lenght);
        String body = StringConvertor.utf8beByteArrayToString(bytes, 0, bytes.length);
        return body;
    }
    private void parseMessage(String line, ChatSession cs) {
        // there can be a lot of different structures of MSG incoming string
        // therefore a complex structure of this function
        String[] tokens = Utils.tokenize(line);
        String body = readBody(cs.sh, line);
        processMessage(body, tokens[1], Utils.urlDecode(tokens[2]), cs);
    }
    private void parseADC(String line) {
        //ADC 0 RL N=odar_5ra@hotmail.com F=Kalypso\r\n

        //line = ADC 0 RL N=odar_5ra@hotmail.com F=Kalypso
        String[] atoms = Utils.tokenize(line);
        //atoms[0] = ADC
        //atoms[1] = cid
        //atoms[2] = FL, AL, RL, BL
        //atoms[3] = N=username
        //atoms[4] = F=full name

        if (atoms[2].compareTo("RL")==0) {
            String userID = getVal(atoms, "N=");
            String name = getVal(atoms, "F=");
            Contact c = msn.createContact(userID, name);

            this.addContact(c);
            msn.addLocalContact(c);
        }
    }
    private void userJoin(String data, ChatSession cs) {
        //JOE zoran.mesec@siol.net nick
        String[] params = Utils.tokenize(data);
        String msg = params[2] + " has joined the conversation window.";
        msn.addMessage(new SystemNotice(msn, SystemNotice.SYS_NOTICE_MESSAGE, cs.c.getUserId(), msg));
    }
    private void userBYE(String data, ChatSession cs) {
        //BYE zoran.mesec@siol.net
        String userId = data.substring(3);
        String msg = userId + " has closed the conversation window.";
        msn.addMessage(new SystemNotice(msn, SystemNotice.SYS_NOTICE_MESSAGE, cs.c.getUserId(), msg));
    }
    private void groupAdded(String data) {
        //ADG 15 New%20Group b145b37f-7e09-47e7-880e-9daa5346eaab
    }
    private String getVal(String[] data, String key) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i].startsWith(key)) {
                return data[i].substring(key.length());
            }
        }
        return null;
    }
    private void parseContacts(String data) {
        /**
         * TODO: rewrite this with the new string tokenizer in Utils
         */
        //Normal form:
        //LST N=matevz.jekovec@guest.arnes.si F=Matevz C=d954638f-1963-4e45-b157-2029eae8714f 3 406c6d87-043d-4f20-b569-0450b49ca65d
        //Someone got a reply like this once(repeatedly):
        //LST N=p

        String[] packet = Utils.tokenize(data.trim());
        String userid = getVal(packet, "N=");
        String screenname = Utils.urlDecode(getVal(packet, "F="));
        if (null == screenname) {
            screenname = userid;
        }
        if (null == userid) {
            return;
        }
        String contactHash = getVal(packet, "C=");
        if (null == contactHash) {
            return;
        }
        MsnContact person = (MsnContact)msn.createContact(userid, screenname);
        person.setUserHash(contactHash);

        String groupGuid = packet[packet.length - 1];
        String listStr;
        if ((groupGuid.length() < 10)) {
            //user does not belong to a group
            listStr = packet[packet.length - 1];
        } else {
            listStr = packet[packet.length - 2];
            //person.setGroupHash(groupGuid);
            person.setGroup((Group)groupID.get(groupGuid));
        }
        short list = Short.parseShort(listStr);
        person.setLists(list);
        this.contacts_.put(person.getUserId(), person);

        //the forward list is 1, the allow list is 2, the block list is 4,
        //the reverse list is 8 and the pending list 16.

        switch (list) {
            case 1: //user in my FL list, but i'm not in his/her FL list
                break;
            case 2:
            case 3:
            case 9: //user in my FL list only, i'm in his FL and AL. Means he can't see my presence, so add user to AL list
                this.tr.newTransaction();
                this.tr.setType("ADC");
                this.tr.addArgument("AL N=" + person.getUserId());
                this.sh.sendRequest(this.tr.toString());
                break;
            case 11: //user in my FL and AL. Means he can see my presence
            case 17:
                break;
            default:
                break;
        }
        msn.addLocalContact(person);
    }

    private void userGoesOffline(String data) {
        //FLN matevz.jekovec@guest.arnes.si
        String uID = data.substring(4, data.length()-2);
        MsnContact c = null;
        c = (MsnContact)this.contacts_.get(uID);
        if (c==null) {
            return;
        }
        /*for (int i=0; i<this.contacts_.size();i++)
        {
            c = (Contact)this.contacts_.elementAt(i);
            if (uID.compareTo(c.getUserId())==0)
            {
                 break;
            }
        }*/
        c.setOfflineStatus();
        msn.ui_changeContactStatus(c);
    }
    private byte status2StatusIndex(String presence) {
        if (presence.compareTo("BSY")==0) {
            return StatusInfo.STATUS_AWAY;
        } else if ((presence.compareTo("IDL")==0) || (presence.compareTo("NLN")==0)) {
            return StatusInfo.STATUS_ONLINE;
        } else if (presence.compareTo("AWY")==0) {
            return StatusInfo.STATUS_AWAY;
        }
        return StatusInfo.STATUS_ONLINE;
    }
    private void changePresence(String data) {
        if (data.length()<20 || data.substring(0,3).compareTo("NLN")!=0) {
            return;
        }
        String presence = data.substring(4, 7);
        String uID = data.substring(8, data.indexOf(" ", 10));

        MsnContact con = (MsnContact)this.contacts_.get(uID);
        if (null != con) {
            msn.setContactStatus(con, status2StatusIndex(presence), null);
            msn.ui_changeContactStatus(con);
        }
    }
    private void startSession(String line) {
        if (waitingChats.isEmpty()) return;
        ChatSession cs = (ChatSession) waitingChats.elementAt(0);
        waitingChats.removeElementAt(0);

        //MSN in: XFR 13 SB 64.4.35.58:1863 CKI 1298692120.186224106.56203170
        String[] params = Utils.tokenize(line);
        ServerHandler switchHandler = new ServerHandler(params[3]);
        switchHandler.connect();
        switchHandler.sendRequest("USR 1 " + this.username + " " + params[5] + "\r\n");
        switchHandler.getReply();
        switchHandler.sendRequest("CAL 2 " + cs.c.getUserId()+"\r\n");
        switchHandler.getReply();// CAL
        switchHandler.getReply();// JOI
        cs.sh = switchHandler;
        this.chatSessions_.addElement(cs);
        sendMsg(cs.msg);
        cs.msg = null;
    }
    private void parseGroups(String data) {
        //MSN in: LSG группа%201 6624a16d-1269-4339-bcdc-0e3a5b28f85e
        String[] params = Utils.tokenize(data);
        MsnGroup g = (MsnGroup) msn.createGroup(Utils.urlDecode(params[1]));
        g.setGuid(params[2]);
        msn.getGroupItems().addElement(g);
        this.groupID.put(params[2], g);
    }

    private void parsePresence(String data) {
        //data = ILN 11 NLN idanilov@ua.fm Ilya%20Danilov 1342177280
        String[] atoms = Utils.tokenize(data);
        //atoms[0] = ILN or NLN
        //atoms[1] = cid number
        //atoms[2] = status
        //atoms[3] = contact name (email)
        //atoms[4] = url decoded screen name
        //atoms[5] = contact capabilities (see: http://www.hypothetic.org/docs/msn/notification/presence.php)
        String presence = atoms[2];
        MsnContact con;
        con = (MsnContact)this.contacts_.get(atoms[3]);
        if (con!=null) {
            msn.setContactStatus(con, status2StatusIndex(presence), null);
            msn.ui_changeContactStatus(con);
        }
    }

    private void parseChallenge(String line) {
        //data = CHL 0 25270234921473318824
        String[] atoms = Utils.tokenize(line);
        //atoms[0] = CHL
        //atoms[1] = ?
        //atoms[2] = challenge
        String challenge = atoms[2];


        // first step for v11
        //StringBuffer challenge =new StringBuffer(data.substring(6,data.length()-2));
        //challenge.append(this.ProductKey);
        //String challenge = data.substring(6,26);


        // v11 challenge
        //String hash = new String(md5.toHex(md5.fingerprint(challenge.toString().getBytes())));
        //challenge.concat("YMM8C_H7KCQ2S_KL");
        //challenge = "15570131571988941333";

        challenge+=this.ProductIDhash;

        MD5 md5 = new MD5();
        md5.calculate(challenge.getBytes());
        String hash = md5.getDigestHex();

        this.tr.newTransaction();
        this.tr.setType("QRY");
        this.tr.addArgument(this.ProductID);
        this.tr.addArgument("32\r\n"+hash);
        this.sh.sendRequest(this.tr.toStringNN());
    }
    private void SBhandle(String data)  //Switchboard handle
    {
        //RNG 1083693517 207.46.26.110:1863 CKI 18321026.739392 zoran.mesec@siol.net Zoran%20Mesec
        //ANS 1 name_123@hotmail.com 18321026.739392 11752013\r\n
        String[] params = Utils.tokenize(data);
        String authNr = params[1];
        String sbIP = params[2];
        String authKey = params[4];
        String uID = params[5];
        MSNTransaction answer = new MSNTransaction();
        answer.newTransaction();    //set id to 1
        answer.setType("ANS");
        answer.addArgument(this.username);
        answer.addArgument(authKey);
        answer.addArgument(authNr);

        MsnContact c = (MsnContact)this.contacts_.get(uID);

        if (null == c) {
            return;
        }
        ServerHandler sbHandler = new ServerHandler(sbIP);
        sbHandler.connect();
        sbHandler.sendRequest(answer.toString());

        ChatSession cs = new ChatSession(c);
        cs.id = 3;
        cs.sh = sbHandler;
        this.chatSessions_.addElement(cs);
    }

    private String getField( String strKey, String strField ) {

        try  {
            int nIniPos = strField.indexOf( strKey );
            int nEndPos = 0;


            if ( nIniPos < 0 )
                return "";

            nIniPos+=strKey.length();
            nEndPos = strField.indexOf( ',', nIniPos );

            if ( nEndPos < 0 )
                return "";

            return strField.substring( nIniPos, nEndPos );
        } catch( Exception e )  {
            return "";
        }
    }
    /**
     * yy
     * @deprecated This method does nothing.
     */
    public void disconnect() {

    }
    public void connect() {

    }

    private Vector waitingChats = new Vector();
    /**
     * This method start a Chatsession(a conversation) with a person. The person is given as a function parameter.
     * @param c contact we wish to converse.
     * @return an instance of the newly created ChatSession.
     */
    private ChatSession startChatSession(MsnContact c, PlainMessage msg) {
        ChatSession cs = new ChatSession(c);
        cs.id = 3;
        cs.msg = msg;
        this.busy = true;
        this.tr.newTransaction();
        this.tr.setType("XFR");
        this.tr.addArgument("SB");
        this.sh.sendRequest(this.tr.toString());
        waitingChats.addElement(cs);
        return cs;
    }
    private Vector outgoingMessages = new Vector();
    /**
     * Send a message.
     * @param msg Message in String
     * @param session Active Chat Session to send the message to
     */
    public void sendMessage(PlainMessage message) {
        outgoingMessages.addElement(message);
    }
    private ChatSession getSession(MsnContact c) {
        ChatSession session = null;
        ServerHandler handler = null;
        for (int i = 0; i < this.chatSessions_.size(); ++i) {
            session = (ChatSession)chatSessions_.elementAt(i);
            if (session.c == c) {
                handler = session.sh;
                if ((null == handler) || !handler.isConnected()) {
                    chatSessions_.removeElementAt(i);
                    return null;
                }
                break;
            }
        }
        return (null == handler) ? null : session;
    }
    public void leaveConversation(MsnContact c) {
        ChatSession session = getSession(c);
        if (null == session) {
            return;
        }
        session.outgoingPackets.addElement("BYE " + session.id + "\r\n");
        session.id++;
    }
    private void sendMsg(PlainMessage message) {
        MsnContact c = (MsnContact) msn.getItemByUIN(message.getRcvrUin());
        if (null == c) return;
        String msg = message.getText();
        ChatSession session = getSession(c);
        if (null == session) {
            startChatSession(c, message);
            return;
        }
        ServerHandler handler = session.sh;

        //// Typing notify
        //String payload = "MIME-Version: 1.0\r\nContent-Type: text/x-msmsgscontrol\r\nTypingUser: "+this.username+"\r\n\r\n\r\n";
        //byte[] body = StringConvertor.stringToByteArrayUtf8(payload);
        //sh.sendRequest("MSG " + session.id + " U " + body.length + "\r\n", body);
        //session.id++;

        String payload = "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nX-MMS-IM-Format: FN=MS%20Sans%20Serif; EF=; CO=0; CS=0; PF=0\r\n\r\n";
        byte[] body = StringConvertor.stringToByteArrayUtf8(payload + msg);
        handler.sendRequest("MSG " + session.id + " U " + body.length + "\r\n", body);
        session.id++;
    }

    private void processSession(ChatSession csTemp) {
        if (!csTemp.sh.isConnected()) {
            return;
        }
        try {
            while (!csTemp.outgoingPackets.isEmpty()) {
                csTemp.sh.sendRequest((String)csTemp.outgoingPackets.elementAt(0));
                csTemp.outgoingPackets.removeElementAt(0);
            }
            while (csTemp.sh.available()) {
                String reply = csTemp.sh.getReply();
                if (null == reply) return;

                if (reply.startsWith(CMD_MSG)) {
                    parseMessage(reply, csTemp);

                } else if (reply.startsWith("BYE")) {
                    userBYE(reply, csTemp);

                } else if (reply.startsWith("JOI")) {
                    userJoin(reply, csTemp);
                }
            }
        } catch (Exception e) {
            csTemp.sh.disconnect();
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            jimm.modules.DebugLog.panic("chat", e);
            // #sijapp cond.end #
        }
    }
    /**
     * This method is called when Thread starts. It is a infinite loop that checks all
     * ServerHandlers whether any message has arrived from servers(that you are
     * currently connected to).
     */
    public void run() {
        JimmException exception = null;
        try {
            if (!connectToServer()) {
                return;
            }

            connected_ = true;
            while(!stop) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }

                try {
                    while (!outgoingMessages.isEmpty()) {
                        PlainMessage m = (PlainMessage)outgoingMessages.elementAt(0);
                        outgoingMessages.removeElementAt(0);
                        sendMsg(m);
                    }
                } catch (Exception e) {
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    jimm.modules.DebugLog.panic("err", e);
                    // #sijapp cond.end #
                }

                // main
                if ((this.status_ == CONNECTED) && sh.available()) {
                    parseMainReply(sh.getReply());
                    continue;
                }

                // chats
                String reply = null;
                for (int i = 0; i < this.chatSessions_.size(); ++i) {
                    if (this.status_ != CONNECTED) break;
                    ChatSession csTemp = (ChatSession)this.chatSessions_.elementAt(i);
                    processSession(csTemp);
                    if (!csTemp.sh.isConnected()) {
                        chatSessions_.removeElementAt(i);
                        --i;
                        continue;
                    }
                }
            }
        } catch (JimmException e) {
            exception = e;
        } catch (Exception e) {
            exception = new JimmException(100, 1);
        }
        try {
            this.connected_ = false;
            for (int i = 0; i < this.chatSessions_.size(); ++i) {
                ChatSession csTemp = (ChatSession)chatSessions_.elementAt(i);
                csTemp.sh.disconnect();
            }
            chatSessions_.removeAllElements();
            sh.disconnect();
        } catch (Exception e) {
        }
        Msn m = msn;
        if ((null != m) && (null != exception)) {
            m.processException(exception);
        }
    }
    private void parseMainReply(String reply) {
        if (reply.startsWith(CMD_MSG)) {
            String body = readBody(sh, reply);
        } else if (reply.startsWith("UBX")) {
            String body = readBody(sh, reply);
        } else {
            parseReply(reply);
        }
    }
    /**
     * Removes a contact from all lists.
     * @param Contact c the contact to be removed
     * @return true for success or false if the Contact is not on local list of Contacts
     */
    public boolean removeContact(Contact c) {
        MsnContact con=(MsnContact)this.contacts_.get(c.getUserId());

        if (con!=null) {
            short list = con.getLists();
            this.tr.newTransaction();
            this.tr.setType("REM");
            this.tr.addArgument("FL " + con.getUserHash());
            this.sh.sendRequest(this.tr.toString());
            switch (list) {
                case 11: //user also in my AL list, so don't forget to remove him from this list also
                    this.tr.newTransaction();
                    this.tr.setType("REM");
                    this.tr.addArgument("AL "+c.getUserId());
                    this.sh.sendRequest(this.tr.toString());
                case 1:
                case 3: //???
                    this.tr.newTransaction();
                    this.tr.setType("REM");
                    this.tr.addArgument("FL " + con.getUserHash());
                    this.sh.sendRequest(this.tr.toString());
                    break;
            }

            this.contacts_.remove(c.getUserId());
            return true;
        }
        return false;
    }
    /**
     * This method adds a contact to the local list and then sends a message to the
     * MSN server in order to add a contact to the main list on the server.
     * @param Contact c The contact to be removed.
     */
    public void addContact(Contact c) {
        //ADC 16 FL N=passport@hotmail.com F=Display%20Name\r\n
        //ADC 16 AL N=passport@hotmail.com F=Display%20Name\r\n
        this.contacts_.put(c.getUserId(), c);

        this.tr.newTransaction();
        this.tr.setType("ADC");
        this.tr.addArgument("FL N=" + c.getUserId() + " " + "F=" + Utils.urlDecode(c.getName()));
        this.sh.sendRequest(this.tr.toString());

    }
    public void addGroup(String groupName) {
        //ADG 15 New%20Group
        this.tr.newTransaction();
        this.tr.setType("ADG");
        this.tr.addArgument(groupName.replace(' ', '_'));
        this.sh.sendRequest(this.tr.toString());
    }

    boolean isConnected() {
        return connected_;
    }
}
// #sijapp cond.end #