/*
 * JabberXml.java
 *
 * Created on 12 Июль 2008 г., 19:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

import jimm.chat.message.*;
import jimm.search.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.util.JLocale;
import protocol.*;
import javax.microedition.lcdui.*;
import protocol.net.ClientConnection;

/**
 *
 * @author Vladimir Krukov
 */
public final class JabberXml extends ClientConnection {
    private Socket socket;
    private Jabber protocol;

    private String fullJid_;
    private String domain_ = "";
    private String resource;
    private final boolean xep0048 = false;
    private byte[] pingPacket = new byte[]{' '};
    private byte[] forPongPacket = StringConvertor.stringToByteArrayUtf8(
            "<iq type='get'><ping xmlns='urn:xmpp:ping'/></iq>");

    private String verHash = "";
    private String featureList = "";

    private final Vector packets = new Vector();

    private boolean isGTalk_ = false;
    private boolean authorized_ = false;
    private boolean rosterLoaded = false;

    private UserInfo singleUserInfo;
    private String autoSubscribeDomain;
    private JabberForm jabberForm;
    // #sijapp cond.if modules_FILES is "true"#
    private IBBFileTransfer ibb;
    // #sijapp cond.end#
    private ServiceDiscovery serviceDiscovery = null;
    private AdHoc adhoc;

    private SASL_ScramSha1 scramSHA1;

    private static final String[] statusCodes = {
            "u" + "navailable",
            "", // online
            "a" + "way",
            "c" + "h" + "a" + "t",
            "",
            "",
            "",
            "",
            "",
            "x" + "a",
            "",
            "d" + "nd",
            "",
            ""
    };


    private static final String S_TEXT = "te" + "xt";
    private static final String S_FROM = "fr" + "om";
    private static final String S_TYPE = "ty" + "pe";
    private static final String S_ERROR = "e" + "rror";
    private static final String S_NONE = "n" + "o" + "ne";
    private static final String S_NODE = "n" + "o" + "de";
    private static final String S_NICK = "ni" + "ck";
    private static final String S_SET = "s" + "e" + "t";
    private static final String S_REMOVE = "r" + "emove";
    private static final String S_RESULT = "r" + "esult";
    private static final String S_GROUP = "g" + "roup";
    private static final String S_ITEM = "i" + "tem";
    private static final String S_ITEMS = "i" + "tems";
    private static final String S_TRUE = "t" + "rue";
    private static final String S_FALSE = "fa" + "lse";
    private static final String S_GET = "g" + "e" + "t";
    private static final String S_TIME = "t" + "ime";
    private static final String S_TITLE = "t" + "itle";
    private static final String S_CODE = "c" + "ode";
    private static final String S_QUERY = "que" + "ry";
    private static final String S_STATUS = "st" + "atus";
    private static final String S_VCARD = "vCard";
    private static final String S_SUBJECT = "subje" + "ct";
    private static final String S_BODY = "b" + "ody";
    private static final String S_URL = "u" + "r" + "l";
    private static final String S_DESC = "d" + "es"+ "c";
    private static final String S_COMPOSING = "c" + "omposing";
    private static final String S_ACTIVE = "ac" + "tive";
    private static final String S_PAUSED = "p" + "aused";
    private static final String S_CHAT = "c" + "hat";
    private static final String S_GROUPCHAT = "groupc" + "hat";
    private static final String S_HEADLINE = "h" + "eadline";
    /**
     * Get roster request
     */
    private static final String GET_ROSTER_XML = "<iq type='get' id='roster'>"
            + "<query xmlns='jabber:iq:roster'/>"
            + "</iq>";

    private static final byte IQ_TYPE_RESULT = 0;
    private static final byte IQ_TYPE_GET = 1;
    private static final byte IQ_TYPE_SET = 2;
    private static final byte IQ_TYPE_ERROR = 3;

    private byte nativeStatus2StatusIndex(String rawStatus) {
        rawStatus = StringConvertor.notNull(rawStatus);
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (rawStatus.equals(statusCodes[i])) {
                return i;
            }
        }
        return StatusInfo.STATUS_ONLINE;
    }
    private String getNativeStatus(byte statusIndex) {
        return statusCodes[statusIndex];
    }


    static boolean isTrue(String val) {
        return S_TRUE.equals(val) || "1".equals(val);
    }

    public String getCaps() {
        return "<c xmlns='http://jabber.org/protocol/caps'"
                + " node='http://jimm.net.ru/caps' ver='"
                + Util.xmlEscape(verHash)
                + "' hash='md5'/>";
    }

    public JabberXml() {
    }

    public void setJabber(Jabber jabber) {
        protocol = jabber;
        resource = jabber.getResource();
        fullJid_ = jabber.getUserId() + '/' + resource;
        domain_ = Jid.getDomain(fullJid_);
    }
    private void setProgress(int percent) {
        getJabber().setConnectingProgress(percent);
    }

    // #sijapp cond.if modules_ZLIB is "true" #
    private void setStreamCompression() throws JimmException {
        setProgress(20);
        socket.activateStreamCompression();
        write(getOpenStreamXml(domain_));

        // #sijapp cond.if modules_DEBUGLOG is "true" #
        //jimm.modules.DebugLog.println("zlib turn on");
        // #sijapp cond.end #
        readXmlNode(true); // "stream:stream"
        parseAuth(readXmlNode(true));
    }
    // #sijapp cond.end #

    public Jabber getJabber() {
        return (Jabber)protocol;
    }


    /////////////////////////////////////////////////////

    private void write(byte[] data) throws JimmException {
        socket.write(data);
    }

    private void connectTo(String url) throws JimmException {
        socket = new Socket();
        socket.connectTo(url);
    }

    public final void disconnect() {
        connect = false;
        protocol = null;
    }

    protected final void ping() throws JimmException {
        write(pingPacket);
    }
    protected final void pingForPong() throws JimmException {
        write(forPongPacket);
    }

    private void putPacketIntoQueue(Object packet) {
        synchronized (packets) {
            packets.addElement(packet);
        }
    }
    private boolean hasOutPackets() {
        synchronized (packets) {
            return !packets.isEmpty();
        }
    }
    private void sendPacket() throws JimmException {
        String packet = null;
        synchronized (packets) {
            packet = (String)packets.elementAt(0);
            packets.removeElementAt(0);
        }
        writePacket(packet);
    }

    protected final boolean processPacket() throws JimmException {
        if (hasOutPackets()) {
            sendPacket();
            return true;
        }
        if (hasInPackets()) {
            updateTimeout();
            processInPacket();
            return true;
        }
        return false;
    }

    protected final void closeSocket() {
        try {
            write("<presence type='unavailable'><status>Logged out</status></presence>");
        } catch (Exception ignored) {
        }
        socket.close();
        socket = null;
    }

    protected Protocol getProtocol() {
        return protocol;
    }
    /////////////////////////////////////////////////////
    private boolean hasInPackets() throws JimmException {
        return (0 < socket.available());
    }

    private void write(String xml) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        //DebugLog.systemPrintln("[OUT]:\n" + xml);
        // #sijapp cond.end #
        write(StringConvertor.stringToByteArrayUtf8(xml));
    }
    private void writePacket(String packet) throws JimmException {
        write(packet);
    }
    private XmlNode readXmlNode(boolean notEmpty) throws JimmException {
        while (hasInPackets() || notEmpty) {
            XmlNode x = XmlNode.parse(socket);
            if (null != x) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                //DebugLog.systemPrintln("[IN]:\n" + x.toString());
                // #sijapp cond.end #
                return x;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    private void sendRequest(String request) throws JimmException {
        write(request);
    }
    // -----------------------------------------------------------------------

    private void setAuthStatus(boolean authorized) throws JimmException {
        if (!authorized_) {
            authorized_ = authorized;
            if (!authorized) {
                getJabber().setPassword(null);
                throw new JimmException(111, 0);
            }
        }
    }
    XmlNode newAccountConnect(String domain, String server) throws JimmException {
        domain = Util.xmlEscape(domain);
        connectTo(server);
        write(getOpenStreamXml(domain));
        readXmlNode(true); // "stream:stream"
        XmlNode features = readXmlNode(true); // "stream:features"
        if (!features.contains("regis" + "ter")) {
            return null;
        }
        write("<iq type='get' to='" + domain
                + "' id='1'><query xmlns='jabber:iq:register'/></iq>");
        return readXmlNode(true);
    }
    XmlNode newAccountRegister(String xml) throws JimmException {
        write(xml);
        XmlNode x = readXmlNode(true);
        socket.close();
        return x;
    }

    private String getSocketUrl(String server) {
        String defaultServer = getJabber().getDefaultServer(domain_);

        String[] url = Util.explode(server, ':');
        String[] socketUrl = new String[3];
        final String S_SOCKET = "s"+"ocket";
        final String S_SSL = "ss"+"l";
        final String S_5222 = "5222";
        if (3 == url.length) {
            socketUrl[0] = url[0];
            socketUrl[1] = url[1];
            socketUrl[2] = url[2];

        } else if (2 == url.length) {
            socketUrl[0] = url[1].equals(S_5222) ? S_SOCKET : S_SSL;
            socketUrl[1] = url[0];
            socketUrl[2] = url[1];

        } else if (1 == url.length) {
            socketUrl[0] = S_SOCKET;
            socketUrl[1] = url[0];
            socketUrl[2] = S_5222;
        }
        if (null != defaultServer) {
            socketUrl[1] = defaultServer;
            url = Util.explode(defaultServer, ':');
            if (3 == url.length) {
                socketUrl = url;
            }
        }
        return socketUrl[0] + "://" + socketUrl[1] + ":" + socketUrl[2];
    }

    protected final void connect() throws JimmException {
        connect = true;
        setProgress(0);
        // #sijapp cond.if modules_MULTI is "true" #
        //if (Profile.PROTOCOL_VK == getJabber().getProfile().protocolType) {
        //    connectToVK();
        //}
        // #sijapp cond.end #

        initFeatures();

        protocol.net.SrvResolver resolver = new protocol.net.SrvResolver();
        String server = Config.getConfigValue(domain_, "/jabber-servers.txt");
        String defaultServer = getJabber().getDefaultServer(domain_);
        if (StringConvertor.isEmpty(server) && (null == defaultServer)) {
            server = resolver.getXmpp(domain_);
        }
        if (StringConvertor.isEmpty(server)) {
            server = domain_;
        }
        connectTo(getSocketUrl(server));

        write(getOpenStreamXml(domain_));
        setProgress(10);

        readXmlNode(true); // "stream:stream"
        resolver.close();
        parseAuth(readXmlNode(true));
        while (!authorized_) {
            loginParse(readXmlNode(true));
        }

        setProgress(50);
        write(GET_ROSTER_XML);
        usePong();
    }
    private void processInPacket() throws JimmException {
        XmlNode x = null;
        try {
            x = readXmlNode(false);
            if (null == x) {
                return;
            }
            parse(x);
            x = null;

        } catch (JimmException e) {
            throw e;

        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Jabber parse", e);
            if (null != x) {
                DebugLog.println("xml: " + x.toString());
            }
            // #sijapp cond.end #
        }
    }
    // -----------------------------------------------------------------------

    /**
     * Parse inbound xml for authentication
     *
     * @param x Received xml
     */
    private void parseAuth(XmlNode x) throws JimmException {
        if ((null == x) || !x.is("stream:features")) {
            nonSaslLogin();
        } else {
            loginParse(x);
        }
    }
    private void nonSaslLogin() throws JimmException {
        String user = Jid.getNick(protocol.getUserId());
        sendRequest(
                "<iq type='set' to='" + domain_ + "' id='login'>"
                + "<query xmlns='jabber:iq:auth'>"
                + "<username>" + Util.xmlEscape(user) + "</username>"
                + "<password>" + Util.xmlEscape(protocol.getPassword()) + "</password>"
                + "<resource>"+ Util.xmlEscape(resource) + "</resource>"
                + "</query>"
                + "</iq>");
        XmlNode answer = readXmlNode(true);
        setAuthStatus(S_RESULT.equals(answer.getAttribute(S_TYPE)));
    }

    private void loginParse(XmlNode x) throws JimmException {
        if (x.is("stream:features")) {
            parseStreamFeatures(x);
            return;

        // #sijapp cond.if modules_ZLIB is "true" #
        } else if (x.is("compressed")) {
            setStreamCompression();
            return;
        // #sijapp cond.end #

            /* Reply to DIGEST-MD5 challenges */
        } else if (x.is("challenge")) {
            parseChallenge(x);
            return;

        } else if (x.is("failure")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Failed");
            // #sijapp cond.end #
            setAuthStatus(false);
            return;

        } else if (x.is("success")) {
            if (null != scramSHA1) {
                if (!scramSHA1.success(new String(Util.base64decode(x.value)))) {
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    DebugLog.systemPrintln("Server answer not valid");
                    // #sijapp cond.end #
                    setAuthStatus(false);
                    return;
                }
                scramSHA1 = null;
            }
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Auth success");
            DebugLog.systemPrintln("auth " + authorized_);
            // #sijapp cond.end #
            sendRequest(getOpenStreamXml(domain_));
            return;

        } else if (x.is("iq")) {
            XmlNode iqQuery = x.childAt(0);
            String id = x.getId();
            if ("sess".equals(id)) {
                setAuthStatus(true);
                return;
            }
            if (null == iqQuery) {
                return;
            }
            String queryName = iqQuery.name;

            // non sasl login
            if (IQ_TYPE_ERROR == getIqType(x)) {
                if ("jabber:iq:auth".equals(iqQuery.getXmlns())) {
                    setAuthStatus(false);
                }
            }

            if ("bind".equals(queryName)) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Send open session request");
                // #sijapp cond.end #
                fullJid_ = iqQuery.getFirstNodeValue(XmlNode.S_JID);
                sendRequest("<iq type='set' id='sess'>"
                        + "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
                        + "</iq>");
                return;

            }
        }
        parse(x);
    }

    /**
     * Parse inbound xml and execute apropriate action
     *
     * @param x Received xml
     */
    private void parse(XmlNode x) throws JimmException {
        if (x.is("iq")) {
            parseIq(x);

        } else if (x.is("presence")) {
            parsePresence(x);

        } else if (x.is("m" + "essage")) {
            parseMessage(x);

        } else if (x.is("stream:error")) {
            setAuthStatus(false);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            XmlNode err = (null == x.childAt(0)) ? x : x.childAt(0);
            DebugLog.systemPrintln("[INFO-JABBER] Stream error!: " + err.name + "," + err.value);
            // #sijapp cond.end #
        }
    }

    private String generateId(String key) {
        return key + Util.uniqueValue();
    }
    private String generateId() {
        return "jimm" + Util.uniqueValue();
    }

    private boolean isNoAutorized(String subscription) {
        return S_NONE.equals(subscription) || S_FROM.equals(subscription);
    }

    private byte getIqType(XmlNode iq) {
        String iqType = iq.getAttribute(S_TYPE);
        if (S_RESULT.equals(iqType)) {
            return IQ_TYPE_RESULT;
        }
        if (S_GET.equals(iqType)) {
            return IQ_TYPE_GET;
        }
        if (S_SET.equals(iqType)) {
            return IQ_TYPE_SET;
        }
        return IQ_TYPE_ERROR;
    }
    private void parseIqError(XmlNode iqNode, String from) throws JimmException {
        XmlNode errorNode = iqNode.getFirstNode(S_ERROR);
        iqNode.removeNode(S_ERROR);

        // #sijapp cond.if modules_DEBUGLOG is "true" #
        if (null == errorNode) {
            DebugLog.println("Error without description is stupid");
        } else {
            DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + getError(errorNode));
        }
        // #sijapp cond.end #

        XmlNode query = iqNode.childAt(0);
        if (null == query) {
            // some bad happend

        } else if (S_VCARD.equals(query.name)) {
            loadVCard(null, from);

        } else if (S_QUERY.equals(query.name)) {
            String xmlns = query.getXmlns();
            if ("jabber:iq:register".equals(xmlns) && (null != jabberForm)) {
                jabberForm.error(getError(errorNode));
                jabberForm = null;

            } else if ("jabber:iq:roster".equals(query.name)) {
                //FIXME: stop loading if roster service was down.
            } else if ("http://jabber.org/protocol/disco#items".equals(xmlns)) {
                ServiceDiscovery disco = serviceDiscovery;
                if (null != disco) {
                    serviceDiscovery = null;
                    disco.setError(getError(errorNode));
                }
                AdHoc commands = adhoc;
                if ((null != commands) && commands.getJid().equals(from)) {
                    adhoc = null;
                    commands.addItems(null);
                }
            }
        }
    }

    private void sendIqError(String query, String xmlns, String from, String id) {
        putPacketIntoQueue("<iq type='error' to='"
                + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                + "<" + query + " xmlns='" + Util.xmlEscape(xmlns) + "'/>"
                + "<error type='cancel'>"
                + "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                + "</error>"
                + "</iq>");
    }
    private boolean isMy(String from) {
        if (StringConvertor.isEmpty(from)) return true;
        if (getJabber().getUserId().equals(Jid.getBareJid(from))) return true;
        return false;
    }

    private void processEmptyId(String id, byte iqType, String from) {
        if (IQ_TYPE_RESULT != iqType) {
            return;
        }
        if (id.startsWith(S_VCARD)) {
            loadVCard(null, from);
        }
        if ((null != jabberForm) && jabberForm.getId().equals(id)) {
            jabberForm.success();
            jabberForm = null;
        }
    }
    /**
     * Parse the <<lit>iq</lit>> node and launch apropriate action
     *
     * @param iq {@link XmlNode} to parse
     */
    private void parseIq(XmlNode iq) throws JimmException {
        String from = StringConvertor.notNull(iq.getAttribute(S_FROM));
        byte iqType = getIqType(iq);
        String id = iq.getId();
        if (StringConvertor.isEmpty(id)) {
            id = generateId();
        }
        // #sijapp cond.if modules_FILES is "true"#
        if (null != ibb) {
            boolean processed = processIbb(iq, iqType, id);
            if (processed) {
                return;
            }
        }
        // #sijapp cond.end#
        if (IQ_TYPE_ERROR == iqType) {
            parseIqError(iq, from);
            return;
        }

        XmlNode iqQuery = iq.childAt(0);
        if (null == iqQuery) {
            processEmptyId(id, iqType, from);
            return;
        }
        String queryName = iqQuery.name;
        Jabber jabber = getJabber();

        if (S_QUERY.equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("jabber:iq:roster".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                if ((IQ_TYPE_RESULT == iqType) && !rosterLoaded) {
                    rosterLoaded = true;
                    TemporaryRoster roster = new TemporaryRoster(jabber);
                    jabber.setContactListStub();

                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = iqQuery.popChildNode();
                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        Contact contact = roster.makeContact(jid);
                        contact.setName(itemNode.getAttribute(XmlNode.S_NAME));

                        String groupName = itemNode.getFirstNodeValue(S_GROUP);
                        if (StringConvertor.isEmpty(groupName) || Jid.isConference(jid)) {
                            groupName = contact.getDefaultGroupName();
                        }
                        contact.setGroup(roster.getOrCreateGroup(groupName));

                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                        roster.addContact(contact);
                    }
                    if (!isConnected()) {
                        return;
                    }
                    jabber.setContactList(roster.getGroups(), roster.mergeContacts());
                    Contact selfContact = jabber.getItemByUIN(jabber.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        jabber.ui_updateContact(selfContact);
                    }

                    jabber.s_updateOnlineStatus();
                    // #sijapp cond.if modules_XSTATUSES is "true" #
                    String xcode = Jabber.xStatus.getCode(jabber.getProfile().xstatusIndex);
                    if ((null != xcode) && !xcode.startsWith(JabberXStatus.XSTATUS_START)) {
                        setXStatus();
                    }
                    // #sijapp cond.end #
                    getBookmarks();
                    setProgress(100);

                } else if (IQ_TYPE_SET == iqType) {
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = (XmlNode)iqQuery.popChildNode();

                        String subscription = itemNode.getAttribute("subsc" + "ription");
                        String jid = itemNode.getAttribute(XmlNode.S_JID);

                        if (Jid.isConference(jid)) {

                        } else if ((S_REMOVE).equals(subscription)) {
                            jabber.removeLocalContact(jabber.getItemByUIN(jid));

                        } else {
                            String name = itemNode.getAttribute(XmlNode.S_NAME);
                            Contact contact = jabber.createTempContact(jid);
                            String group = itemNode.getFirstNodeValue(S_GROUP);
                            if (StringConvertor.isEmpty(group) || Jid.isConference(contact.getUserId())) {
                                group = contact.getDefaultGroupName();
                            }
                            contact.setName(name);
                            contact.setGroup(jabber.getOrCreateGroup(group));
                            contact.setTempFlag(false);
                            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                            jabber.addLocalContact(contact);
                        }
                    }
                    Contact selfContact = jabber.getItemByUIN(jabber.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        jabber.ui_updateContact(selfContact);
                    }
                }
                return;

            } else if ("http://jabber.org/protocol/disco#info".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<iq type='result' to='")
                    .append(Util.xmlEscape(from))
                    .append("' id='").append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#info'>");
                    sb.append(featureList);
                    sb.append("</query></iq>");
                    write(sb.toString());
                    return;
                }
                if (IQ_TYPE_RESULT != iqType) {
                    return;
                }
                String name = iqQuery.getFirstNodeAttribute("identity", XmlNode.S_NAME);
                jabber.setConferenceInfo(from, name);
                return;

            } else if ("http://jabber.org/protocol/disco#items".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    sendIqError(S_QUERY, xmlns, from, id);
                    return;
                }
                ServiceDiscovery disco = serviceDiscovery;
                if (null != disco) {
                    serviceDiscovery = null;
                    disco.setTotalCount(iqQuery.childrenCount());
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode item = iqQuery.popChildNode();
                        String name = item.getAttribute(XmlNode.S_NAME);
                        String jid = item.getAttribute(XmlNode.S_JID);
                        disco.addItem(name, jid);
                    }
                    disco.update();
                    return;
                }
                AdHoc commands = adhoc;
                if ((null != commands) && commands.getJid().equals(from)) {
                    adhoc = null;
                    commands.addItems(iqQuery);
                }
                return;

            } else if ("jabber:iq:register".equals(xmlns)) {
                if ((null != jabberForm) && jabberForm.getId().equals(id)) {
                    if (jabberForm.isWaiting()) {
                        jabberForm.loadFromXml(iqQuery, iqQuery);
                        jabberForm = null;

                    } else {
                        processEmptyId(id, iqType, from);
                    }
                }
                return;

            } else if ("jabber:iq:private".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                XmlNode storage = iqQuery.getFirstNode("sto" + "rage", "storage:bookmarks");
                if (null != storage) {
                    loadBookmarks(storage);
                }
                return;

            } else if ("jabber:iq:version".equals(xmlns)) {
                // #sijapp cond.if modules_CLIENTS is "true" #
                if (IQ_TYPE_RESULT == iqType) {
                    String name = iqQuery.getFirstNodeValue(XmlNode.S_NAME);
                    String ver = iqQuery.getFirstNodeValue("v" + "ersion");
                    String os = iqQuery.getFirstNodeValue("o" + "s");
                    name = Util.notUrls(name);
                    ver = Util.notUrls(ver);
                    os = Util.notUrls(os);
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    DebugLog.println("ver " + jid + " " + name + " " + ver + " in " + os);
                    // #sijapp cond.end #
                    StatusView sv = jimm.cl.ContactList.getInstance().getStatusView();
                    Contact c = sv.getContact();
                    // TODO: check protocol instance
                    if ((null != c) && c.getUserId().equals(jid)) {
                        sv.setClientVersion(name + " " + ver + " " + os);
                        getJabber().updateStatusView(sv, c);
                    }
                    return;
                }
                // #sijapp cond.end #
                if (IQ_TYPE_GET == iqType) {
                    putPacketIntoQueue("<iq type='result' to='"
                            + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:version'><name>Jimm</name><version>"
                            + Util.xmlEscape(jimm.Jimm.VERSION + " (###DATE###)")
                            + "</version><os>"
                            + Util.xmlEscape(jimm.Jimm.microeditionPlatform)
                            + "</os></query></iq>");
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    MagicEye.addAction(jabber, jid, "get_version");
                    // #sijapp cond.end #
                }
                return;

            } else if ("jabber:iq:last".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    // #sijapp cond.if modules_MAGIC_EYE is "true" #
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    MagicEye.addAction(jabber, jid, "last_activity_request");
                    // #sijapp cond.end #
                    long time = Jimm.getCurrentGmtTime() - jabber.getLastStatusChangeTime();
                    putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                            + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:last' seconds='"
                            + time
                            + "'/></iq>");
                }
                return;

            } else if ("http://jabber.org/protocol/muc#owner".equals(xmlns)) {
                if (IQ_TYPE_RESULT == iqType) {
                    if (null != jabberForm) {
                        jabberForm.loadFromXml(iqQuery, iq);
                        jabberForm = null;
                    }
                }
            }

        } else if (S_TIME.equals(queryName)) {
            if (IQ_TYPE_GET != iqType) {
                return;
            }
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
            MagicEye.addAction(jabber, jid, "get_time");
            // #sijapp cond.end #
            int gmtOffset = Options.getInt(Options.OPTION_GMT_OFFSET);
            putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(id) + "'>"
                    + "<time xmlns='urn:xmpp:time'><tzo>"
                    + (0 <= gmtOffset ? "+" : "-") + Util.makeTwo(Math.abs(gmtOffset)) + ":00"
                    + "</tzo><utc>"
                    + Util.getUtcDateString(Jimm.getCurrentGmtTime())
                    + "</utc></time></iq>");
            return;

        } else if (("p" + "ing").equals(queryName)) {
            writePacket("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' type='result'/>");
            return;

        } else if (("pu" + "bsub").equals(queryName)) {
            if (!isMy(from)) {
                return;
            }
            loadBookmarks(iqQuery.getFirstNodeRecursive("sto" + "rage"));
            return;

        } else if (S_VCARD.equals(queryName)) {
            if (IQ_TYPE_RESULT == iqType) {
                loadVCard(iqQuery, from);
            }
            return;

        } else if ("x".equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("http://jabber.org/protocol/rosterx".equals(xmlns)) {
                if (Jid.isGate(from)) {
            	    Contact c = getJabber().getItemByUIN(from);
            	    if ((null != c) && !c.isTemp() && c.isAuth()) {
                        putPacketIntoQueue("<iq type='result' to='"
                                + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' />");
                        parseRosterExchange(iqQuery, '@' + from);
                        return;
                    }
                }
            }

        } else if (("c" + "ommand").equals(queryName)) {
            if (null != adhoc) {
                adhoc.loadCommandXml(iq, id);
            }
        }

        if (IQ_TYPE_GET == iqType) {
            sendIqError(iqQuery.name, iqQuery.getXmlns(), from, id);
        }
    }


    public void saveVCard(UserInfo userInfo) {
        if (null == userInfo.vCard) {
            userInfo.vCard = XmlNode.getEmptyVCard();
        }
        userInfo.vCard.removeBadVCardTags("TEL");
        userInfo.vCard.removeBadVCardTags("EMAIL");
        userInfo.vCard.setValue("NICKNAME", userInfo.nick);
        userInfo.vCard.setValue("BDAY", userInfo.birthDay);
        userInfo.vCard.setValue("URL", userInfo.homePage);
        userInfo.vCard.setValue("FN", userInfo.getName());
        userInfo.vCard.setValue("N", null, "GIVEN", userInfo.firstName);
        userInfo.vCard.setValue("N", null, "FAMILY", userInfo.lastName);
        userInfo.vCard.setValue("N", null, "MIDDLE", "");
        userInfo.vCard.setValue("EMAIL", new String[]{"INTERNET"}, "USERID", userInfo.email);
        userInfo.vCard.setValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", userInfo.cellPhone);

        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "STREET", userInfo.homeAddress);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "LOCALITY", userInfo.homeCity);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "REGION", userInfo.homeState);

        userInfo.vCard.setValue("TEL", new String[]{}, "NUMBER", "");
        userInfo.vCard.setValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER", userInfo.workPhone);
        userInfo.vCard.setValue("ORG", null, "ORGNAME", userInfo.workCompany);
        userInfo.vCard.setValue("ORG", null, "ORGUNIT", userInfo.workDepartment);
        userInfo.vCard.setValue("TITLE", userInfo.workPosition);
        userInfo.vCard.setValue("DESC", userInfo.about);
        userInfo.vCard.cleanXmlTree();

        StringBuffer packet = new StringBuffer();
        packet.append("<iq type='set' id='").append(generateId()).append("'>");
        userInfo.vCard.toString(packet);
        packet.append("</iq>");
        putPacketIntoQueue(packet.toString());
    }

    private void loadVCard(XmlNode vCard, String from) {
        UserInfo userInfo = singleUserInfo;
        if ((null == userInfo) || !from.equals(userInfo.realUin)) {
            return;
        }
        userInfo.auth = false;
        userInfo.uin = from;
        if (Jid.isConference(from)) {
            Contact c = getJabber().getItemByUIN(Jid.getBareJid(from));
            if (c instanceof JabberServiceContact) {
                JabberContact.SubContact sc = ((JabberServiceContact)c).getExistSubContact(Jid.getResource(from, null));
                if ((null != sc) && (null != sc.realJid)) {
                    userInfo.uin = sc.realJid;
                }
            }
        }

        if (null == vCard) {
            userInfo.updateProfileView();
            singleUserInfo = null;
            return;
        }

        String name[] = new String[3];
        name[0] = vCard.getFirstNodeValue("N", "GIVEN");
        name[1] = vCard.getFirstNodeValue("N", "MIDDLE");
        name[2] = vCard.getFirstNodeValue("N", "FAMILY");
        if (StringConvertor.isEmpty(Util.implode(name, ""))) {
            userInfo.firstName = vCard.getFirstNodeValue("FN");
            userInfo.lastName = null;
        } else {
            userInfo.lastName = name[2];
            name[2] = null;
            userInfo.firstName = Util.implode(name, " ");
        }
        userInfo.nick = vCard.getFirstNodeValue("NICKNAME");
        userInfo.birthDay = vCard.getFirstNodeValue("BDAY");
        userInfo.email = vCard.getFirstNodeValue("EMAIL", new String[]{"INTERNET"}, "USERID", true);
        userInfo.about = vCard.getFirstNodeValue("DESC");
        userInfo.homePage = vCard.getFirstNodeValue("URL");

        userInfo.homeAddress = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "STREET", true);
        userInfo.homeCity = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "LOCALITY", true);
        userInfo.homeState = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "REGION", true);
        userInfo.cellPhone = vCard.getFirstNodeValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", true);

        userInfo.workCompany = vCard.getFirstNodeValue("ORG", null, "ORGNAME");
        userInfo.workDepartment = vCard.getFirstNodeValue("ORG", null, "ORGUNIT");
        userInfo.workPosition = vCard.getFirstNodeValue("TITLE");
        userInfo.workPhone = vCard.getFirstNodeValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER");

        if (!Jid.isGate(from)) {
            userInfo.setOptimalName();
        }
        if (userInfo.isEditable()) {
            userInfo.vCard = vCard;
        }
        userInfo.updateProfileView();

        try {
            XmlNode bs64photo = vCard.getFirstNode("PHOTO");
            bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
            byte[] avatarBytes = null;
            if (null != bs64photo) {
                avatarBytes = userInfo.isEditable()
                        ? bs64photo.getBinValue()
                        : bs64photo.popBinValue();
            }

            if ((null != avatarBytes) && Jimm.hasMemory(avatarBytes.length * 2)) {
                Image avatar = Image.createImage(avatarBytes, 0, avatarBytes.length);
                avatarBytes = null;
                userInfo.setAvatar(avatar);
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError er) {
        } catch (Exception e) {
        }

        singleUserInfo = null;
    }

    private void loadBookmarks(XmlNode storage) {
        if ((null == storage) || (0 == storage.childrenCount())) {
            return;
        }
        if (!"storage:bookmarks".equals(storage.getXmlns())) {
            return;
        }
        Jabber jabber = getJabber();
        Group group = jabber.getOrCreateGroup(JLocale.getString(Jabber.CONFERENCE_GROUP));
        int autoJoinCount = jabber.isReconnect() ? 0 : 7;
        Vector groups = jabber.getGroupItems();
        Vector contacts = jabber.getContactItems();
        while (0 < storage.childrenCount()) {
            XmlNode item = storage.popChildNode();

            String jid = item.getAttribute(XmlNode.S_JID);
            if ((null == jid) || !Jid.isConference(jid)) {
                continue;
            }
            String name = item.getAttribute(XmlNode.S_NAME);
            String nick = item.getFirstNodeValue(S_NICK);
            boolean autojoin = isTrue(item.getAttribute("au" + "tojoin"));
            String password = item.getAttribute("passwor" + "d");

            JabberServiceContact conference = (JabberServiceContact)jabber.createTempContact(jid, name);
            conference.setMyName(nick);
            conference.setTempFlag(false);
            conference.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            conference.setAutoJoin(autojoin);
            conference.setPassword(password);
            conference.setGroup(group);
            if (-1 == Util.getIndex(contacts, conference)) {
                contacts.addElement(conference);
            }
            if (conference.isAutoJoin() && (0 < autoJoinCount)) {
                jabber.join(conference);
                autoJoinCount--;
            }
        }
        jabber.setContactListAddition(group);
        jabber.rejoin();
    }

    /**
     * Parse the <<lit>presence</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     */
    private void parsePresence(XmlNode x) {
        final String fromFull = x.getAttribute(S_FROM);
        final String from = Jid.getBareJid(fromFull);
        final String fromRes = Jid.getResource(fromFull, "");
        String type = x.getAttribute(S_TYPE);

        if (S_ERROR.equals(type)) {
            XmlNode errorNode = x.getFirstNode(S_ERROR);
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + errorNode.getFirstNodeValue(S_TEXT));
            // #sijapp cond.end #

            boolean showError = Jid.isGate(from);
            if (Jid.isConference(from)) {
                JabberServiceContact conf = (JabberServiceContact)getJabber().getItemByUIN(from);
                if (null != conf) {
                    int code = Util.strToIntDef(errorNode.getAttribute(S_CODE), -1);
                    conf.nickError(getJabber(), fromRes, code, getError(errorNode));
                    return;
                }
            }
            if (showError) {
                getJabber().addMessage(new SystemNotice(getJabber(),
                        SystemNotice.SYS_NOTICE_ERROR, from, getError(errorNode)));
            }

            Contact c = getJabber().getItemByUIN(from);
            if (null == c) {
                return;
            }
            c.setOfflineStatus();
            return;
        }

        if (("subscr" + "ibe").equals(type)) {
            if (isAutoGateContact(from)) {
                sendSubscribed(from);
                requestSubscribe(from);
            } else {
                getJabber().addMessage(new SystemNotice(getJabber(), SystemNotice.SYS_NOTICE_AUTHREQ, from, null));
            }
            Contact c = getJabber().getItemByUIN(from);
            autoRenameContact(c, x);
            autoMoveContact(c, x);
            return;
        }
        if (("subscr" + "ibed").equals(type)) {
            if (!isAutoGateContact(from)) {
                getJabber().setAuthResult(from, true);
            }
            autoRenameContact(getJabber().getItemByUIN(from), x);
            return;
        }
        if (("unsubscr" + "ibed").equals(type)) {
            getJabber().setAuthResult(from, false);
            return;
        }
        if (null == type) {
            type = x.getFirstNodeValue("sh" + "ow");
        }
        if (null == type) {
            type = "";
        }

        JabberContact contact = (JabberContact)getJabber().getItemByUIN(from);
        if (null == contact) {
            String fullJid = Jid.realJidToJimmJid(fromFull);
            contact = (JabberContact)getJabber().getItemByUIN(fullJid);
            if (null == contact) {
                return;
            }
        }

        int priority = Util.strToIntDef(x.getFirstNodeValue("prior" + "ity"), 0);
        String statusString = x.getFirstNodeValue(S_STATUS);

        if (Jid.isConference(from)) {
            XmlNode xMuc = x.getXNode("http://jabber.org/protocol/muc#user");
            XmlNode item = (null == xMuc) ? null : xMuc.getFirstNode(S_ITEM);
            JabberServiceContact conf = (JabberServiceContact)contact;
            String reasone = null;
            priority = 0;
            if (null != item) {
                String affiliation = item.getAttribute(XmlNode.S_AFFILIATION);
                if (("m" + "ember").equals(affiliation)) {
                    priority |= JabberServiceContact.AFFILIATION_MEMBER;
                } else if (("o" + "wner").equals(affiliation)) {
                    priority |= JabberServiceContact.AFFILIATION_OWNER;
                } else if (("a" + "dmin").equals(affiliation)) {
                    priority |= JabberServiceContact.AFFILIATION_ADMIN;
                } else {
                    priority |= JabberServiceContact.AFFILIATION_NONE;
                }

                String role = item.getAttribute(XmlNode.S_ROLE);
                if (("m" + "oderator").equals(role)) {
                    priority |= JabberServiceContact.ROLE_MODERATOR;

                } else if (("p" + "articipant").equals(role)) {
                    priority |= JabberServiceContact.ROLE_PARTICIPANT;

                } else if (S_NONE.equals(role)) {
                    reasone = item.getFirstNodeValue("r" + "eason");
                    item = null;

                } else {// "visitor"
                    priority |= JabberServiceContact.ROLE_VISITOR;
                }
            }
            getJabber().setConfContactStatus(conf, fromRes,
                    nativeStatus2StatusIndex(type), statusString, priority);
            if (null != item) {
                String newNick = item.getAttribute(XmlNode.S_NICK);
                if (null != newNick) {
                    getJabber().setConfContactStatus(conf, newNick,
                            nativeStatus2StatusIndex(""), "", priority);
                    conf.nickChainged(getJabber(), fromRes, newNick);
                } else {
                    conf.nickOnline(getJabber(), fromRes);
                }

                String realJid = item.getAttribute(XmlNode.S_JID);
                if (null != realJid) {
                    conf.setRealJid(fromRes, Jid.getBareJid(realJid));
                }
                // #sijapp cond.if modules_CLIENTS is "true" #
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", S_NODE));
                // #sijapp cond.end #

            } else {
                int code = 0;
                if (null != xMuc) {
                    code = Util.strToIntDef(xMuc.getFirstNodeAttribute(S_STATUS, S_CODE), 0);
                }
                conf.nickOffline(getJabber(), fromRes, code, reasone);
            }

            if (conf.getMyName().equals(fromRes)) {
                getJabber().ui_changeContactStatus(conf);
            }
            updateConfPrivate(conf, fromRes);

        } else {
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if (!("u" + "navailable").equals(type)) {
                if ((XStatusInfo.XSTATUS_NONE == contact.getXStatusIndex())
                        || !Jabber.xStatus.isPep(contact.getXStatusIndex())) {
                    XmlNode xNode = x.getXNode(S_FEATURE_XSTATUS);
                    String id = getXStatus(xNode);

                    String xtext = null;
                    if (null != id) {
                        xtext = xNode.getFirstNodeValue(S_TITLE);
                        String s = StringConvertor.notNull(statusString);
                        if (StringConvertor.isEmpty(xtext)) {
                            xtext = null;

                        } else if (s.startsWith(xtext)) {
                            xtext = statusString;
                            statusString = null;
                        }
                    }
                    contact.setXStatus(id, xtext);
                }
                if (Jid.isPyIcqGate(from)) {
                    setXStatusToIcqTransport((JabberServiceContact)contact);
                }
            }
            // #sijapp cond.end #
            getJabber().setContactStatus(contact, fromRes, nativeStatus2StatusIndex(type), statusString, priority);
            contact.updateMainStatus(getJabber());
            // #sijapp cond.if modules_CLIENTS is "true" #
            if (contact.isOnline()) {
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", S_NODE));
            }
            // #sijapp cond.end #

            if (contact.getUserId().equals(contact.getName())) {
                getJabber().renameContact(contact, getNickFromNode(x));
            }
            getJabber().ui_changeContactStatus(contact);
        }
    }
    private String getNickFromNode(XmlNode x) {
        String name = x.getFirstNodeValueRecursive("n" + "ickname");
        return (null == name) ? x.getFirstNodeValue(S_NICK) : name;
    }
    private void autoRenameContact(Contact contact, XmlNode x) {
        if (null == contact) {
            return;
        }
        String name = getNickFromNode(x);
        if (null == name) {
            return;
        }
        if (contact.getUserId().equals(contact.getName())) {
            getJabber().renameContact(contact, name);
        }
    }
    private void autoMoveContact(Contact contact, XmlNode presence) {
        if (null == contact) {
            return;
        }
        XmlNode x = presence.getXNode("http:/" + "/delx.cjb.net/protocol/roster-subsync");
        if (null == x) {
            return;
        }
        x = x.childAt(0);
        if (null == x) {
            return;
        }
        Group g = getJabber().getOrCreateGroup(x.getFirstNodeValue(S_GROUP));
        String name = x.getAttribute(XmlNode.S_NAME);
        boolean update = false;
        if (null != g) {
            contact.setGroup(g);
            update = true;
        }
        if (StringConvertor.isEmpty(name)) {
            contact.setName(name);
            update = true;
        }
        if (update) {
            updateContact((JabberContact) contact);
        }
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private void parseEvent(XmlNode eventNode, String fullJid) {
        if (null == eventNode) {
            return;
        }
        JabberContact contact = (JabberContact)getJabber().getItemByUIN(Jid.getBareJid(fullJid));
        if (null == contact) {
            return;
        }

        XmlNode statusNode = eventNode.getFirstNode(S_ITEMS);
        String eventType = "";
        if (null != statusNode) {
            eventType = statusNode.getAttribute(S_NODE);
            int start = eventType.lastIndexOf('/');
            if (-1 != start) {
                eventType = eventType.substring(start + 1);
            }
            statusNode = statusNode.getFirstNode(S_ITEM);
        }
        if (null != statusNode) {
            statusNode = statusNode.childAt(0);
        }
        if (-1 == "|mood|activity|tune".indexOf(eventType)) {
            return;
        }

        if ((null == statusNode) || (0 == statusNode.childrenCount())) {
            if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                    && Jabber.xStatus.isType(contact.getXStatusIndex(), eventType)) {
                contact.setXStatus("", "");
            }
            return;
        }
        String text = statusNode.getFirstNodeValue(S_TEXT);
        statusNode.removeNode(S_TEXT);
        StringBuffer status = new StringBuffer();
        while (null != statusNode) {
            status.append(':').append(statusNode.name);
            statusNode = statusNode.childAt(0);
        }
        status.deleteCharAt(0);
        if ((XStatusInfo.XSTATUS_NONE == contact.getXStatusIndex())
                || Jabber.xStatus.isPep(contact.getXStatusIndex())) {
            contact.setXStatus(status.toString(), text);
        }
    }

    private String getXStatus(XmlNode x) {
        return (null == x) ? null : (JabberXStatus.XSTATUS_START + x.getId());
    }
    // #sijapp cond.end #

    private void parseMessageEvent(XmlNode messageEvent, String from) {
        if (null == messageEvent) {
            return;

        }
        if (messageEvent.contains("offl" + "ine")) {
            // <x><offline/><id/></x>
            setMessageSended(messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_SERVER);
            return;
        }
        if (messageEvent.contains("deli" + "vered")) {
            setMessageSended(messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_CLIENT);
            return;
        }
        // #sijapp cond.if modules_SOUND is "true" #
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            getJabber().beginTyping(from, messageEvent.contains(S_COMPOSING));
        }
        // #sijapp cond.end #
    }
    // #sijapp cond.if modules_SOUND is "true" #
    private void parseChatState(XmlNode message, String from) {
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            if (message.contains(S_ACTIVE)
                    || message.contains("gon" + "e")
                    || message.contains(S_PAUSED)
                    || message.contains("inactiv" + "e")) {
                getJabber().beginTyping(from, false);

            } else if (message.contains(S_COMPOSING)) {
                getJabber().beginTyping(from, true);
            }
        }
    }
    // #sijapp cond.end #
    private String getDate(XmlNode message) {
        XmlNode offline = message.getXNode("jabber:x:delay");
        if (null == offline) {
            offline = message.getFirstNode("d" + "elay");
        }
        return (null == offline) ? null : offline.getAttribute("stamp");
    }

    private void prepareFirstPrivateMessage(String jid) {
        final JabberServiceContact conf =
                (JabberServiceContact)getJabber().getItemByUIN(Jid.getBareJid(jid));
        if (null == conf) { // don't have conference
            return;
        }

        JabberContact.SubContact sub = conf.getExistSubContact(Jid.getResource(jid, ""));
        if (null == sub) { // don't have contact
            return;
        }

        byte role = (byte)(sub.priority & JabberServiceContact.ROLE_MASK);
        if (JabberServiceContact.ROLE_MODERATOR == role) {
            // moderators without antispam
            getJabber().addTempContact(getJabber().createTempContact(jid));
        }
    }


    /**
     * Parse the <<lit>message</lit>> node and launch apropriate action
     *
     * @param msg {@link XmlNode} to parse
     */
    private void parseMessage(XmlNode msg) {
        msg.removeNode("h" + "tml");

        String type = msg.getAttribute(S_TYPE);
        boolean isGroupchat = ("groupc" + "hat").equals(type);
        boolean isError = S_ERROR.equals(type);

        String fullJid = msg.getAttribute(S_FROM);
        boolean isConference = Jid.isConference(fullJid);
        if (!isGroupchat) {
            fullJid = Jid.realJidToJimmJid(fullJid);
        }
        String from = Jid.getBareJid(fullJid);

        // message resender
        if (from.equals(getJabber().getUserId())) {
            XmlNode addresses = msg.getFirstNode("a" + "ddresses");
            if (null != addresses) {
                String ofrom = null;
                while (0 < addresses.childrenCount()) {
                    XmlNode address = addresses.popChildNode();
                    if ("ofrom".equals(address.getAttribute(S_TYPE))) {
                        ofrom = address.getAttribute(XmlNode.S_JID);
                        break;
                    }
                }
                if (null != ofrom) {
                    fullJid = ofrom;
                    if (!isGroupchat) {
                        fullJid = Jid.realJidToJimmJid(fullJid);
                    }
                    isConference = Jid.isConference(fullJid);
                    from = Jid.getBareJid(fullJid);
                }
            }
        }

        if (isConference && !isGroupchat) {
            from = fullJid;
        }
        String fromRes = Jid.getResource(fullJid, null);

        String subject = msg.getFirstNodeValue(S_SUBJECT);
        String text = msg.getFirstNodeValue(S_BODY);
        if ((null != subject) && (null == text)) {
            text = "";
        }
        if ("jubo@nologin.ru".equals(from) && msg.contains("juick")) {
            parseBlogMessage("juick@juick.com", msg, text, "JuBo");
            return;
        }
        if (protocol.isBlogBot(from)) {
            parseBlogMessage(from, msg, text, null);
            return;
        }

        if (!isConference) {
            if (msg.contains("atte" + "ntion")) {
                type = S_CHAT;
                text = PlainMessage.CMD_WAKEUP;
                subject = null;
            }
        }
        // #sijapp cond.if modules_SOUND is "true" #
        if (isConference ? !isGroupchat : true) {
            parseChatState(msg, from);
        }
        // #sijapp cond.end #

        if (null == text) {
            XmlNode received = msg.getFirstNode("recei" + "ved");
            if (null != received) {
                String id = received.getId();
                if (null == id) {
                    id = msg.getId();
                }
                setMessageSended(id, PlainMessage.NOTIFY_FROM_CLIENT);
                return;
            }
            if (!Jid.isConference(from) && !isError) {
                parseMessageEvent(msg.getXNode("jabber:x:event"), from);
                // #sijapp cond.if modules_XSTATUSES is "true" #
                parseEvent(msg.getFirstNode("ev" + "ent"), fullJid);
                // #sijapp cond.end #
            }
            return;
        }
        // #sijapp cond.if modules_DEBUGLOG isnot "true" #
        msg.removeNode(S_SUBJECT);
        msg.removeNode(S_BODY);
        // #sijapp cond.end #
        if ((null != subject) && (-1 == text.indexOf(subject))) {
            text = subject + "\n\n" + text;
        }
        text = StringConvertor.trim(text);

        final JabberContact c = (JabberContact)getJabber().getItemByUIN(from);

        if (msg.contains(S_ERROR)) {
            final String errorText = getError(msg.getFirstNode(S_ERROR));
            if (null != errorText) {
                text = errorText + "\n-------\n" + text;
            }

        } else {
            if ((null != c) && msg.contains("c" + "aptcha")) {
                final JabberForm form = new JabberForm(JabberForm.TYPE_CAPTCHA,
                        getJabber(), from);
                form.showCaptcha(msg);
                return;
            }

            final XmlNode oobNode = msg.getXNode("jabber:x:oob");
            if (null != oobNode) {
                String url = oobNode.getFirstNodeValue(S_URL);
                if (null != url) {
                    text += "\n\n" + url;
                    String desc = oobNode.getFirstNodeValue(S_DESC);
                    if (null != desc) {
                        text += "\n" + desc;
                    }
                    msg.removeNode(S_URL);
                    msg.removeNode(S_DESC);
                }
            }

            if (!isGroupchat && msg.contains("reques" + "t") && (null != msg.getId())) {
                putPacketIntoQueue("<message to='" + Util.xmlEscape(fullJid)
                    + "' id='" + Util.xmlEscape(msg.getId())
                    + "'><received xmlns='urn:xmpp:receipts' id='"
                    + Util.xmlEscape(msg.getId()) + "'/></message>");
            }

            if (c instanceof JabberServiceContact) {
                isConference = c.isConference();
                if ((null != subject) && isConference && isGroupchat) {
                    String prevSubject = StringConvertor.notNull(c.getStatusText());
                    ((JabberServiceContact)c).setSubject(subject);
                    getJabber().ui_changeContactStatus(c);
                    if (prevSubject.equals(subject)) {
                        prevSubject = null;
                    }
                    subject = null;
                    fromRes = null;
                    if (StringConvertor.isEmpty(prevSubject) && !c.hasUnreadMessage()) {
                        if (!(c.hasChat() && protocol.getChat(c).isVisibleChat())) {
                            return;
                        }
                    }
                }
            }
        }
        if (StringConvertor.isEmpty(text)) {
            return;
        }

        text = StringConvertor.convert(Jid.isMrim(from)
                ? StringConvertor.MRIM2JIMM : StringConvertor.JABBER2JIMM,
                text);

        final String date = getDate(msg);
        final boolean isOnlineMessage = (null == date);
        long time = isOnlineMessage ? Jimm.getCurrentGmtTime() : Util.createGmtDate(date);
        final PlainMessage message = new PlainMessage(from, getJabber(), time, text, !isOnlineMessage);

        if (null == c) {
            if (isConference && !isGroupchat) {
                prepareFirstPrivateMessage(from);
            }

        } else {
            if (isConference) {
                final JabberServiceContact conf = (JabberServiceContact)c;

                if (isGroupchat && (null != fromRes)) {
                    if (isOnlineMessage && fromRes.equals(conf.getMyName())) {
                        if (isMessageExist(msg.getId())) {
                            setMessageSended(msg.getId(),
                                    PlainMessage.NOTIFY_FROM_CLIENT);
                            return;
                        }
                        if (Jid.isIrcConference(fullJid)) {
                            return;
                        }
                    }
                    message.setName(conf.getNick(fromRes));
                }

            } else {
                c.setActiveResource(fromRes);
            }
        }

        getJabber().addMessage(message, S_HEADLINE.equals(type));
    }
    private void parseBlogMessage(String to, XmlNode msg, String text, String botNick) {
        text = StringConvertor.notNull(text);
        String userNick = getNickFromNode(msg);
        if (null == userNick) {
            userNick = msg.getFirstNodeAttribute("juick", "uname");
        }
        if (StringConvertor.isEmpty(userNick)) {
            userNick = null;
        }
        String nick = userNick;
        if (null != botNick) {
            nick = StringConvertor.notNull(userNick) + "@" + botNick;
        }
        if (null != userNick) {
            userNick += ':';
            int nickPos = text.indexOf(userNick);
            if (0 == nickPos) {
                text = text.substring(userNick.length() + 1);
            }
        }

        text = StringConvertor.convert(StringConvertor.JABBER2JIMM, text);
        text = StringConvertor.trim(text);
        if (StringConvertor.isEmpty(text)) {
            return;
        }


        String date = getDate(msg);
        long time = (null == date) ? Jimm.getCurrentGmtTime() : Util.createGmtDate(date);
        PlainMessage message = new PlainMessage(to, getJabber(), time, text, false);
        if (null != nick) {
            message.setName(('@' == nick.charAt(0)) ? nick.substring(1) : nick);
        }
        getJabber().addMessage(message);
    }

    String getError(XmlNode errorNode) {
        if (null == errorNode) {
            return S_ERROR;
        }
        String errorText = errorNode.getFirstNodeValue(S_TEXT);
        if (null == errorText) {
            errorText = errorNode.value;
        }
        if (null == errorText) {
            errorText = "error " + errorNode.getAttribute(S_CODE);
            if (null != errorNode.childAt(0)) {
                errorText += ": " + errorNode.childAt(0).name;
            }
        }
        return errorText;
    }

    private boolean isMechanism(XmlNode list, String myMechanism) {
        for (int i = 0; i < list.childrenCount(); ++i) {
            XmlNode mechanism = list.childAt(i);
            if (mechanism.is("mechanism") && myMechanism.equals(mechanism.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the <<lit>stream:features</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     */
    private void parseStreamFeatures(XmlNode x) throws JimmException {
        XmlNode x2 = null;
        if (0 == x.childrenCount()) {
            nonSaslLogin();
            return;
        }
        // #sijapp cond.if modules_ZLIB is "true" #
        /* Check for stream compression method */
        x2 = x.getFirstNode("compression");
        if ((null != x2) && "zlib".equals(x2.getFirstNodeValue("method"))) {
            sendRequest("<compress xmlns='http://jabber.org/protocol/compress'><method>zlib</method></compress>");
            return;
        }
        // #sijapp cond.end #

        /* Check for authentication mechanisms */
        x2 = x.getFirstNode("mechanisms");
        if ((null != x2) && x2.contains("mechanism")) {

            String auth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' ";

            String googleToken = null;
            /* X-GOOGLE-TOKEN authentication */
            if (isMechanism(x2, "X-GOOGLE-TOKEN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using X-GOOGLE-TOKEN");
                // #sijapp cond.end #
                isGTalk_ = true;
                googleToken = getGoogleToken(getJabber().getUserId(), getJabber().getPassword());
                if (null == googleToken) {
                    throw new JimmException(111, 1);
                }
            }

            /* DIGEST-MD5 authentication */
            if (isMechanism(x2, "DIGEST-MD5")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using DIGEST-MD5");
                // #sijapp cond.end #
                auth += "mechanism='DIGEST-MD5'/>";

            } else if (isMechanism(x2, "SCRAM-SHA-1")) {
                auth += "mechanism='SCRAM-SHA-1'>";
                scramSHA1 = new SASL_ScramSha1();
                auth +=Util.xmlEscape(scramSHA1.init(protocol.getUserId(), protocol.getPassword()));
                auth += "</auth>";

            } else if (null != googleToken) {
                auth += "mechanism='X-GOOGLE-TOKEN'>" + googleToken + "</auth>";

                /* PLAIN authentication */
            } else if (isMechanism(x2, "PLAIN")) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Using PLAIN");
                // #sijapp cond.end #
                auth += "mechanism='PLAIN'>";
                Util data = new Util();
                data.writeUtf8String(getJabber().getUserId());
                data.writeByte(0);
                data.writeUtf8String(Jid.getNick(getJabber().getUserId()));
                data.writeByte(0);
                data.writeUtf8String(getJabber().getPassword());
                auth += MD5.toBase64(data.toByteArray());
                auth += "</auth>";

            } else if (isGTalk_) {
                nonSaslLogin();
                return;

            } else {
                /* Unknown authentication method */
                setAuthStatus(false);
                return;
            }

            sendRequest(auth);
            return;
        }
        /* Check for resource bind */
        if (x.contains("bind")) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Send bind request");
            // #sijapp cond.end #
            sendRequest("<iq type='set' id='bind'>"
                    + "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                    + "<resource>" + Util.xmlEscape(resource) + "</resource>"
                    + "</bind>"
                    + "</iq>");
            return;
        }
        x2 = x.getFirstNode("a"+"uth", "http://jabber.org/features/iq-auth");
        if (null != x2) {
            nonSaslLogin();
            return;
        }

    }

    /**
     * Parse the <<lit>challenge</lit>> node and launch apropriate action
     *
     * @param x {@link XmlNode} to parse
     */
    private void parseChallenge(XmlNode x) throws JimmException {
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        DebugLog.systemPrintln("[INFO-JABBER] Received challenge");
        // #sijapp cond.end #
        String resp = "<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'";
        String challenge = MD5.decodeBase64(x.value);

        if (null != scramSHA1) {
            resp += ">" + scramSHA1.response(challenge) + "</response>";

        } else { // md5
            int nonceIndex = challenge.indexOf("nonce=");
            if (nonceIndex >= 0) {
                nonceIndex += 7;
                String nonce = challenge.substring(nonceIndex, challenge.indexOf('\"', nonceIndex));
                String cnonce = "123456789abcd";

                resp += ">";
                resp += responseMd5Digest(
                        Jid.getNick(getJabber().getUserId()),
                        getJabber().getPassword(),
                        domain_,
                        "xmpp/" + domain_,
                        nonce,
                        cnonce);
                resp += "</response>";
            } else {
                resp += "/>";
            }
        }


        sendRequest(resp);
    }

    /**
     * This routine generates MD5-DIGEST response via SASL specification
     * (From BOMBUS project)
     *
     * @param user user
     * @param pass password
     * @param realm realm
     * @param digestUri digest uri
     * @param nonce nonce
     * @param cnonce cnonce
     * @return md5 response
     */
    private String responseMd5Digest(String user, String pass,
            String realm, String digestUri, String nonce, String cnonce) {
        MD5 hUserRealmPass = new MD5();
        hUserRealmPass.init();
        hUserRealmPass.updateASCII(user);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(realm);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(pass);
        hUserRealmPass.finish();

        MD5 hA1 = new MD5();
        hA1.init();
        hA1.update(hUserRealmPass.getDigestBits());
        hA1.update((byte) ':');
        hA1.updateASCII(nonce);
        hA1.update((byte) ':');
        hA1.updateASCII(cnonce);
        hA1.finish();

        MD5 hA2 = new MD5();
        hA2.init();
        hA2.updateASCII("AUTHENTICATE:");
        hA2.updateASCII(digestUri);
        hA2.finish();

        MD5 hResp = new MD5();
        hResp.init();
        hResp.updateASCII(hA1.getDigestHex());
        hResp.update((byte) ':');
        hResp.updateASCII(nonce);
        hResp.updateASCII(":00000001:");
        hResp.updateASCII(cnonce);
        hResp.updateASCII(":auth:");
        hResp.updateASCII(hA2.getDigestHex());
        hResp.finish();

        String quote = "\"";
        if (Profile.PROTOCOL_VK == getJabber().getProfile().protocolType) {
            quote = "";
        }
        return MD5.toBase64(StringConvertor.stringToByteArrayUtf8(
                new StringBuffer()
                .append("username=\"").append(user)
                .append("\",realm=\"").append(realm)
                .append("\",nonce=\"").append(nonce)
                .append("\",cnonce=\"").append(cnonce)
                .append("\",nc=00000001,digest-uri=\"").append(digestUri)
                .append("\",qop=auth,response=").append(quote).append(hResp.getDigestHex())
                .append(quote).append(",charset=utf-8").toString()));
    }


    /**
     * Generates X-GOOGLE-TOKEN response by communication with
     * http://www.google.com
     * (From mGTalk project)
     *
     * @param jid
     * @param passwd
     * @return
     */
    private String getGoogleToken(String jid, String passwd) {
        try {
            String escapedJid = Util.urlEscape(jid);
            String first = "Email=" + escapedJid
                    + "&Passwd=" + Util.urlEscape(passwd)
                    + "&PersistentCookie=false&source=googletalk";

            HttpsConnection c = (HttpsConnection) Connector
                    .open("https:/" + "/www.google.com:443/accounts/ClientAuth?" + first);

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("[INFO-JABBER] Connecting to www.google.com");
            // #sijapp cond.end #
            DataInputStream dis = c.openDataInputStream();
            String str = readLine(dis);
            if (str.startsWith("SID=")) {
                String SID = str.substring(4, str.length());
                str = readLine(dis);
                String LSID = str.substring(5, str.length());
                first = "SID=" + SID + "&LSID=" + LSID + "&service=mail&Session=true";
                dis.close();
                c.close();
                c = (HttpsConnection) Connector
                        .open("https://www.google.com:443/accounts/IssueAuthToken?" + first);

                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.systemPrintln("[INFO-JABBER] Next www.google.com connection");
                // #sijapp cond.end #
                dis = c.openDataInputStream();
                str = readLine(dis);

                Util data = new Util();
                data.writeByte(0);
                data.writeUtf8String(Jid.getNick(jid));
                data.writeByte(0);
                data.writeUtf8String(str);
                String token = MD5.toBase64(data.toByteArray());
                dis.close();
                c.close();
                return token;
            }

        } catch (Exception ex) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.systemPrintln("EX: " + ex.toString());
            // #sijapp cond.end #
        }
        return null;
    }

    /**
     * Service routine for google token
     * (From mGTalk project)
     *
     * @param dis
     * @return
     */
    private String readLine(DataInputStream dis) {
        StringBuffer s = new StringBuffer();
        try {
            for (byte ch = dis.readByte(); ch != -1; ch = dis.readByte()) {
                if (ch == '\n') {
                    return s.toString();
                }
                s.append((char)ch);
            }
        } catch (Exception e) {
        }
        return s.toString();
    }

    private void updateConfPrivate(JabberServiceContact conf, String resource) {
        String privateJid = Jid.realJidToJimmJid(conf.getUserId() + '/' + resource);
        Contact privateContact = getJabber().getItemByUIN(privateJid);
        if (null != privateContact) {
            ((JabberServiceContact)privateContact).setPrivateContactStatus(conf);
            getJabber().ui_changeContactStatus(privateContact);
        }
    }

    public void updateContacts(Vector contacts) {
        StringBuffer xml = new StringBuffer();

        int itemCount = 0;
        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); ++i) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (Jid.isConference(contact.getUserId())) {
                continue;
            }
            itemCount++;
            xml.append("<item name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            Group group = getProtocol().getGroup(contact);
            if (null != group) {
                xml.append("'><group>");
                xml.append(Util.xmlEscape(group.getName()));
                xml.append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        xml.append("</query></iq>");
        if (0 < itemCount) {
            putPacketIntoQueue(xml.toString());
        }
    }
    private void parseRosterExchange(XmlNode x, String domain) {
        StringBuffer xml = new StringBuffer();
        Jabber j = (Jabber)protocol;
        Vector subscribes = new Vector();
        for (int i = 0; i < x.childrenCount(); ++i) {
            XmlNode item = x.childAt(i);
            String jid = item.getAttribute(XmlNode.S_JID);
            if (!jid.endsWith(domain)) {
                continue;
            }
            boolean isDelete = item.getAttribute("a" + "ction").equals("d" + "elete");
            boolean isModify = item.getAttribute("a" + "ction").equals("m" + "odify");

            JabberContact contact = (JabberContact)j.getItemByUIN(jid);
            if (null == contact) {
                if (isModify || isDelete) {
                    continue;
                }
                contact = (JabberContact)j.createTempContact(jid);
                contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            }
            String group = item.getFirstNodeValue(S_GROUP);
            if (!isDelete) {
                contact.setName(item.getAttribute(XmlNode.S_NAME));
                if (StringConvertor.isEmpty(group)) {
                    group = contact.getDefaultGroupName();
                }
                contact.setGroup(j.getOrCreateGroup(group));
                if ((null != group) && group.equals(contact.getDefaultGroupName())) {
                    group = null;
                }
                contact.setTempFlag(false);
                if (!contact.isAuth()) {
                    subscribes.addElement(contact);
                }
            }

            xml.append("<item jid='").append(Util.xmlEscape(jid));
            if (isDelete) {
                xml.append("' subscription='remove'/>");
                continue;
            } else if (!isModify) {
                xml.append("' ask='subscribe");
            }
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            if (null != group) {
                xml.append("'><group>")
                        .append(Util.xmlEscape(group))
                        .append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        if (0 < xml.length()) {
            putPacketIntoQueue("<iq type='set' id='" + generateId()
                    + "'><query xmlns='jabber:iq:roster'>"
                    + xml.toString() + "</query></iq>");
            xml = new StringBuffer();
            for (int i = 0; i < subscribes.size(); ++i) {
                xml.append("<presence type='subscribe' to='")
                        .append(Util.xmlEscape(((Contact)subscribes.elementAt(i)).getUserId()))
                        .append("'/>");
            }
            if (0 < xml.length()) {
                putPacketIntoQueue(xml.toString());
            }
        }
    }


    public String getConferenceStorage() {
        StringBuffer xml = new StringBuffer();
        Vector contacts = getJabber().getContactItems();
        xml.append("<storage xmlns='storage:bookmarks'>");
        for (int i = 0; i < contacts.size(); ++i) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.isConference() || contact.isTemp()) {
                continue;
            }
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);

            JabberServiceContact conf = (JabberServiceContact)contact;
            xml.append("<conference autojoin='");
            xml.append(conf.isAutoJoin() ? S_TRUE : S_FALSE);
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            if (!StringConvertor.isEmpty(conf.getPassword())) {
                xml.append("' password='");
                xml.append(Util.xmlEscape(conf.getPassword()));
            }
            xml.append("'><nick>");
            xml.append(Util.xmlEscape(conf.getMyName()));
            xml.append("</nick></conference>");
        }
        xml.append("</storage>");
        return xml.toString();
    }
    public void saveConferences() {
        StringBuffer xml = new StringBuffer();

        String storage = getConferenceStorage();
        xml.append("<iq type='set'><query xmlns='jabber:iq:private'>");
        xml.append(storage);
        xml.append("</query></iq>");

        // XEP-0048
        if (xep0048) {
            xml.append("<iq type='set'>");
            xml.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
            xml.append("<publish node='storage:bookmarks'><item id='current'>");
            xml.append(storage);
            xml.append("</item></publish></pubsub></iq>");
        }

        putPacketIntoQueue(xml.toString());
    }
    public void removeGateContacts(String gate) {
        if (StringConvertor.isEmpty(gate)) {
            return;
        }
        gate = "@" + gate;
        Vector contacts = getJabber().getContactItems();
        StringBuffer xml = new StringBuffer();

        xml.append("<iq type='set' id='").append(generateId())
            .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); ++i) {
            JabberContact contact = (JabberContact)contacts.elementAt(i);
            if (!contact.getUserId().endsWith(gate)) {
                continue;
            }

            xml.append("<item subscription='remove' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            xml.append("'/>");
        }
        xml.append("</query></iq>");

        putPacketIntoQueue(xml.toString());
    }

    public void updateContact(JabberContact contact) {
        if (contact.isConference() && Jid.isConference(contact.getUserId()) && !isGTalk_) {
            contact.setTempFlag(false);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            String groupName = contact.getDefaultGroupName();
            Group group = getJabber().getOrCreateGroup(groupName);
            contact.setGroup(group);
            saveConferences();
            return;
        }

        Group g = getProtocol().getGroup(contact);
        if (contact.isConference()) {
            g = getJabber().getOrCreateGroup(contact.getDefaultGroupName());

        } else if (g.getName().equals(contact.getDefaultGroupName())) {
            g = null;
        }

        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item name='" + Util.xmlEscape(contact.getName())
                + "' jid='" + Util.xmlEscape(contact.getUserId()) + "'>"
                + (null == g ? "" : "<group>" + Util.xmlEscape(g.getName()) + "</group>")
                + "</item>"
                + "</query></iq>");
    }
    public void removeContact(String jid) {
        if (Jid.isConference(jid) && !isGTalk_) {
            saveConferences();
        }
        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item subscription='remove' jid='" + Util.xmlEscape(jid) + "'/>"
                + "</query></iq>");
    }

    public void getBookmarks() {
        putPacketIntoQueue("<iq type='get' id='0'><query xmlns='jabber:iq:private'><storage xmlns='storage:bookmarks'/></query></iq>");
        // XEP-0048
        if (xep0048) {
            putPacketIntoQueue("<iq type='get' id='1'><pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='storage:bookmarks'/></pubsub></iq>");
        }
    }


    /**
     * Get open stream request
     */
    private String getOpenStreamXml(String server) {
        return "<?xml version='1.0'?>"
                + "<stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http:/" + "/etherx.jabber.org/streams' "
                + "version='1.0' "
                + "to='" + server + "'"
                + " xml:lang='" + jimm.util.JLocale.getLanguageCode()+ "'>";
    }

    private void getVCard(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(generateId(S_VCARD)) + "'>"
                + "<vCard xmlns='vcard-temp' version='2.0' prodid='-/"
                + "/HandGen/" + "/NONSGML vGen v1.0/" + "/EN'/>"
                + "</iq>");
    }

    private void sendMessage(String to, String msg, String type, boolean notify, String id) {
        to = Jid.jimmJidToRealJid(to);
        boolean buzz = msg.startsWith(PlainMessage.CMD_WAKEUP) && S_CHAT.equals(type);
        if (buzz) {
            type = S_HEADLINE;
            notify = false;
            if (!getJabber().isContactOverGate(to)) {
                msg = msg.substring(PlainMessage.CMD_WAKEUP.length()).trim();
                if (StringConvertor.isEmpty(msg)) {
                    msg = "/me " + JLocale.getString("wake_you_up");
                }
            }
        }
        if (Jid.isMrim(to)) {
            msg = StringConvertor.convert(StringConvertor.JIMM2MRIM, msg);
        }
        String chatState = "";
        if ((1 < Options.getInt(Options.OPTION_TYPING_MODE)) && S_CHAT.equals(type)) {
            chatState = getChatStateTag(S_ACTIVE);
        }
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to) + "'"
                + " type='" + type + "' id='" + Util.xmlEscape(id) + "'>"
                + (isGTalk_ ? "<nos:x value='disabled' xmlns:nos='google:nosave'/>" : "")
                + (buzz ? "<attention xmlns='urn:xmpp:attention:0'/>" : "")
                + "<body>" + Util.xmlEscape(msg) + "</body>"
                + (notify ? "<request xmlns='urn:xmpp:receipts'/><x xmlns='jabber:x:event'><offline/><delivered/></x>" : "")
                + chatState
                + "</message>");
    }

    void sendMessage(String to, String msg) {
        String type = S_CHAT;
        if (Jid.isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        sendMessage(to, msg, type, false, generateId());
    }

    /**
     * Sends a message to a user
     *
     * @param message Message to send
     */
    void sendMessage(PlainMessage message) {
        String to = message.getRcvrUin();
        JabberContact toContact = (JabberContact)protocol.getItemByUIN(to);
        if (null != toContact) {
            to = toContact.getReciverJid();
        }
        String type = S_CHAT;
        if (Jid.isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        message.setMessageId(Util.uniqueValue());
        boolean notify = true;

        sendMessage(to, message.getText(), type, S_CHAT.equals(type),
                String.valueOf(message.getMessageId()));

        if (notify) {
            addMessage(message);
        }
    }
    private String getChatStateTag(String state) {
        return "<" + state + " xmlns='http://jabber.org/protocol/chatstates'/>";
    }
    void sendTypingNotify(String to, boolean composing) {
        String tag = getChatStateTag(composing ? S_COMPOSING : S_PAUSED);
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to)
                + "' id='0'>" + tag + "</message>");
    }


    void sendPresence(JabberServiceContact conf) {
        String to = conf.getUserId();
        String xml = "";
        if (conf.isConference()) {
            to += "/" + conf.getMyName();
            String xNode = "";
            String password = conf.getPassword();
            if (!StringConvertor.isEmpty(password)) {
                xNode += "<password>" + Util.xmlEscape(password) + "</password>";
            }
            long time = conf.hasChat() ? getJabber().getChat(conf).getLastMessageTime() : 0;
            time = (0 == time) ? 24*60*60 : (Jimm.getCurrentGmtTime() - time);
            xNode += "<history maxstanzas='20' seconds='" + time + "'/>";
            if (!StringConvertor.isEmpty(xNode)) {
                xml += "<x xmlns='http://jabber.org/protocol/muc'>" + xNode + "</x>";
            }
        }
        String status = getNativeStatus(getJabber().getProfile().statusIndex);
        if (!StringConvertor.isEmpty(status)) {
            xml += "<show>" + status + "</show>";
        }

        xml = "<presence to='"+ Util.xmlEscape(to) + "'>" + xml
                + getCaps() + "</presence>";
        putPacketIntoQueue(xml);
    }
    void sendPresenceUnavailable(String to) {
        putPacketIntoQueue("<presence type='unavailable' to='" + Util.xmlEscape(to)
                + "'><status>I&apos;ll be back</status></presence>");
    }

    void setStatus(byte statusIndex, String msg, int priority) {
        setStatus(getNativeStatus(statusIndex), msg, priority);
    }
    void setStatus(String status, String msg, int priority) {
        // #sijapp cond.if modules_XSTATUSES is "true" #
        // FIXME
        String xXml = getQipXStatus();
        if (0 != xXml.length()) {
            msg = getJabber().getProfile().xstatusTitle;
            String descr = getJabber().getProfile().xstatusDescription;
            if (!StringConvertor.isEmpty(descr)) {
                msg = msg + " " + descr;
            }
        }
        // #sijapp cond.end #
        String xml = "<presence>"
                + (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>")
                + (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>")
                + (0 < priority ? "<priority>" + priority + "</priority>" : "")
                + getCaps()
                // #sijapp cond.if modules_XSTATUSES is "true" #
                + xXml
                // #sijapp cond.end #
                + "</presence>";
        putPacketIntoQueue(xml);
    }

    public void sendSubscribed(String jid) {
        requestPresence(jid, "s" + "ubscribed");
    }
    public void sendUnsubscribed(String jid) {
        requestPresence(jid, "u" + "nsubscribed");
    }
    public void requestSubscribe(String jid) {
        requestPresence(jid, "s" + "ubscribe");
    }


    private void requestPresence(String jid, String type) {
        putPacketIntoQueue("<presence type='" + Util.xmlEscape(type) + "' to='" + Util.xmlEscape(jid) + "'/>");
    }
    private void requestIq(String jid, String xmlns, String id) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid)
                + "' id='" + Util.xmlEscape(id) + "'><query xmlns='" + xmlns + "'/></iq>");
    }
    private void requestIq(String jid, String xmlns) {
        requestIq(jid, xmlns, generateId());
    }

    public void requestClientVersion(String jid) {
        requestIq(jid, "jabber:iq:version");
    }
    public void requestConferenceInfo(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#info");
    }
    public void requestConferenceUsers(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getJabber().getServiceDiscovery();
    }

    public void requestDiscoItems(String server) {
        requestIq(server, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getJabber().getServiceDiscovery();
    }

    void requestRawXml(String xml) {
        putPacketIntoQueue(xml);
    }
    public void setMucRole(String jid, String nick, String role) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'/></query></iq>");
    }
    public void setMucAffiliation(String jid, String userJid, String affiliation) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'/></query></iq>");
    }

    UserInfo getUserInfo(Contact contact) {
        singleUserInfo = new UserInfo(getJabber(), contact.getUserId());
        getVCard(contact.getUserId());
        return singleUserInfo;
    }
    void register2(JabberForm form, String rawXml, String jid) {
        jabberForm = form;
        autoSubscribeDomain = jid;
        requestRawXml(rawXml);
    }
    private boolean isAutoGateContact(String jid) {
        return !StringConvertor.isEmpty(autoSubscribeDomain)
        && (jid.equals(autoSubscribeDomain) || jid.endsWith('@' + autoSubscribeDomain));
    }
    void register(String jid) {
        jabberForm = new JabberForm(JabberForm.TYPE_REGISTER, getJabber(), jid);
        requestIq(jid, "jabber:iq:register", jabberForm.getId());
        jabberForm.show();
    }
    void unregister(String jid) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
        + "' id='unreg1'><query xmlns='jabber:iq:register'><remove/></query></iq>");
    }

    void requestOwnerForm(String jid) {
        jabberForm = new JabberForm(JabberForm.TYPE_OWNER, getJabber(), jid);
        requestIq(jid, "http://jabber.org/protocol/muc#owner", jabberForm.getId());
        jabberForm.show();
    }

    // #sijapp cond.if modules_XSTATUSES is "true" #
    private void sendXStatus(String xstatus, String text) {
        String[] path = Util.explode(Util.xmlEscape(xstatus), ':');
        StringBuffer sb = new StringBuffer();
        String typeUrl = "http://jabber.org/protocol/" + path[0];

        sb.append("<iq type='set' id='").append(generateId());
        sb.append("'><pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<publish node='").append(typeUrl).append("'><item>");
        sb.append("<").append(path[0]).append(" xmlns='").append(Util.xmlEscape(typeUrl));
        if (1 == path.length) {
            sb.append("'/>");
        } else {
            sb.append("'><").append(path[1]);
            if (2 == path.length) {
                sb.append("/>");
            } else {
                sb.append("><").append(path[2]).append("/></").append(path[1]).append(">");
            }
            if (!StringConvertor.isEmpty(text)) {
                sb.append("<text>").append(Util.xmlEscape(text)).append("</text>");
            }
            sb.append("</").append(path[0]).append(">");
        }
        sb.append("</item></publish></pubsub></iq>");
        putPacketIntoQueue(sb.toString());
    }
    private String getQipXStatus() {
        byte x = getJabber().getProfile().xstatusIndex;
        if (XStatusInfo.XSTATUS_NONE == x) {
            return "";
        }
        String code = Jabber.xStatus.getCode(x);
        if ((null == code) || !code.startsWith(JabberXStatus.XSTATUS_START)) {
            return "";
        }
        if (code.equals(JabberXStatus.XSTATUS_TEXT_NONE)) {
            return "";
        }
        String id = code.substring(JabberXStatus.XSTATUS_START.length());
        return "<x xmlns='" + S_FEATURE_XSTATUS + "' id='"
                + Util.xmlEscape(id) + "'><title>"
                + Util.xmlEscape(getJabber().getProfile().xstatusTitle)
                + "</title></x>";
    }

    private static final String S_FEATURE_XSTATUS = "http://qip.ru/x-status";
    void setXStatus() {
        String xstatusCode = Jabber.xStatus.getCode(getJabber().getProfile().xstatusIndex);
        if (null == xstatusCode) {
            return;
        }

        setXStatusToIcqTransports();

        if (xstatusCode.startsWith(JabberXStatus.XSTATUS_START)) {
            setStatus(getNativeStatus(getJabber().getProfile().statusIndex),
                    getJabber().getProfile().statusMessage, Jabber.PRIORITY);
            return;
        }
        final String mood = "mo"+"od";
        final String activity = "acti" + "vity";
        if (!xstatusCode.startsWith(mood)) {
            sendXStatus(mood, null);
        }
        if (!xstatusCode.startsWith(activity)) {
            sendXStatus(activity, null);
        }
        if (xstatusCode.startsWith(mood) || xstatusCode.startsWith(activity)) {
            sendXStatus(xstatusCode, getJabber().getProfile().xstatusTitle);
        }
    }

    private void setXStatusToIcqTransport(JabberServiceContact gate) {
        String xstatus = Jabber.xStatus.getIcqXStatus(getJabber().getProfile().xstatusIndex);
        if (null == xstatus) {
            return;
        }
        String desc = "None".equals(xstatus) ? null : getJabber().getProfile().xstatusTitle;
        desc = StringConvertor.notNull(desc);
        if (gate.isOnline() && Jid.isPyIcqGate(gate.getUserId())) {
            String out = "<iq type='set' id='" + generateId() + "' to='"
                + Util.xmlEscape(gate.getUserId())
                + "'><command xmlns='http://jabber.org/protocol/commands' node='setxstatus' action='complete'><x xmlns='jabber:x:data' type='submit'><field var='xstatus_desc'><value>"
                + Util.xmlEscape(desc)
                + "</value></field><field var='xstatus_name'><value>"
                + Util.xmlEscape(xstatus)
                + "</value></field></x></command></iq>";
            putPacketIntoQueue(out);
        }
    }
    private void setXStatusToIcqTransports() {
        String x = Jabber.xStatus.getIcqXStatus(getJabber().getProfile().xstatusIndex);
        if (null == x) {
            return;
        }
        Vector contacts = getJabber().getContactItems();
        for (int i = contacts.size() - 1; i >= 0; --i) {
            JabberContact c = (JabberContact)contacts.elementAt(i);
            if (c.isOnline() && Jid.isPyIcqGate(c.getUserId())) {
                setXStatusToIcqTransport((JabberServiceContact)c);
            }
        }
    }
    // #sijapp cond.end #

    private String getVerHash(Vector features) {
        StringBuffer sb = new StringBuffer();
        sb.append("client/phone/" + "/Jimm<");
        for (int i = 0; i < features.size(); ++i) {
            sb.append(features.elementAt(i)).append('<');
        }
        return MD5.toBase64(new MD5().calculate(StringConvertor.stringToByteArrayUtf8(sb.toString())));
    }
    private String getFeatures(Vector features) {
        StringBuffer sb = new StringBuffer();
        sb.append("<identity category='client' type='phone' name='Jimm'/>");
        for (int i = 0; i < features.size(); ++i) {
            sb.append("<feature var='").append(features.elementAt(i)).append("'/>");
        }
        return sb.toString();
    }
    private void initFeatures() {
        Vector features = new Vector();
        features.addElement("bugs");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement("http://jabber.org/protocol/activity");
        features.addElement("http://jabber.org/protocol/activity+notify");
        // #sijapp cond.end #
        // #sijapp cond.if modules_SOUND is "true" #
        if (0 < Options.getInt(Options.OPTION_TYPING_MODE)) {
            features.addElement("http://jabber.org/protocol/chatstates");
        }
        // #sijapp cond.end #
        features.addElement("http://jabber.org/protocol/disco#info");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement("http://jabber.org/protocol/mood");
        features.addElement("http://jabber.org/protocol/mood+notify");
        // #sijapp cond.end #
        features.addElement("http://jabber.org/protocol/rosterx");
        // #sijapp cond.if modules_XSTATUSES is "true" #
        features.addElement(S_FEATURE_XSTATUS);//"http://qip.ru/x-status");
        // #sijapp cond.end #
        features.addElement("jabber:iq:last");
        features.addElement("jabber:iq:version");
        features.addElement("urn:xmpp:attention:0");
        features.addElement("urn:xmpp:time");

        verHash = getVerHash(features);
        featureList = getFeatures(features);
    }

    private boolean isMessageExist(String id) {
        return isMessageExist(Util.strToIntDef(id, -1));
    }
    private void setMessageSended(String id, int state) {
        markMessageSended(Util.strToIntDef(id, -1), state);
    }

    void resetAdhoc() {
        adhoc = null;
    }
    void requestCommand(AdHoc adhoc, String node) {
        this.adhoc = adhoc;
        putPacketIntoQueue("<iq to='" + Util.xmlEscape(adhoc.getJid())
                + "' type='set' id='" + Util.xmlEscape(generateId()) + "'>"
                + "<command xmlns='http://jabber.org/protocol/commands' "
                + "node='" + Util.xmlEscape(node) + "'/></iq>");
    }
    void requestCommandList(AdHoc adhoc) {
        this.adhoc = adhoc;
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(adhoc.getJid())
                + "' id='" + Util.xmlEscape(generateId()) + "'><query xmlns='"
                + "http://jabber.org/protocol/disco#items"
                + "' node='http://jabber.org/protocol/commands'/></iq>");
    }

    // #sijapp cond.if modules_FILES is "true"#
    void setIBB(IBBFileTransfer transfer) {
        ibb = transfer;
        ibb.setProgress(0);
        putPacketIntoQueue(ibb.getRequest());
    }
    private boolean processIbb(XmlNode iq, byte type, String id) {
        id = StringConvertor.notNull(id);
        if (!id.startsWith("jimmibb_")) {
            return false;
        }
        if (IQ_TYPE_RESULT != type) {
            // something bad happend
            ibb.setProgress(-1);
            ibb.destroy();
            ibb = null;
            return true;
        }
        if ("jimmibb_si".equals(id)) {
            ibb.setProgress(10);
            putPacketIntoQueue(ibb.initTransfer());
            return true;
        }

        if ("jimmibb_close".equals(id)) {
            return true;
        }
        if (ibb.isCanceled()) {
            putPacketIntoQueue(ibb.close());
            ibb.destroy();
            ibb = null;
            return true;
        }

        ibb.setProgress(ibb.getPercent());
        String stanza = ibb.nextBlock();
        if (null == stanza) {
            stanza = ibb.close();
            ibb.setProgress(100);
            ibb.destroy();
            ibb = null;
        }
        putPacketIntoQueue(stanza);
        return true;
    }
    // #sijapp cond.end#
}
// #sijapp cond.end #