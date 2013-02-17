/*
 * UserInfo.java
 *
 * Created on 25 Март 2008 г., 19:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.search;

import jimm.ui.text.TextListModel;
import jimm.ui.text.TextList;
import DrawControls.icons.*;
import javax.microedition.lcdui.*;
import jimm.JimmException;
import jimm.cl.ContactList;
import jimm.comm.Util;
import jimm.forms.*;
// #sijapp cond.if modules_FILES="true"#
import jimm.modules.fs.*;
import jimm.modules.photo.*;
// #sijapp cond.end#
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.ui.text.TextListController;
import jimm.util.JLocale;
import protocol.net.TcpSocket;
import protocol.*;
import protocol.icq.*;
import protocol.jabber.*;
import protocol.mrim.*;
// #sijapp cond.if protocols_OBIMP is "true" #
import protocol.obimp.*;
// #sijapp cond.end #


/**
 *
 * @author vladimir
 */
public class UserInfo implements
        // #sijapp cond.if protocols_JABBER is "true" #
        // #sijapp cond.if modules_FILES="true"#
        PhotoListener, FileBrowserListener,
        // #sijapp cond.end #
        // #sijapp cond.end #
        ActionListener {
    private final Protocol protocol;
    private TextList profileView;
    private boolean avatarIsLoaded = false;
    private boolean searchResult = false;

    public Icon avatar;
    // #sijapp cond.if protocols_ICQ is "true" #
    public String status;
    // #sijapp cond.end #
    // #sijapp cond.if protocols_JABBER is "true" #
    public protocol.jabber.XmlNode vCard;
    // #sijapp cond.end #
    /////////////////////////////////////
    public final String realUin;
    public String localName;

    public String uin;
    public String nick;
    public String email;
    public String homeCity;
    public String firstName;
    public String lastName;

    public String homeState;
    public String homePhones;
    public String homeFax;
    public String homeAddress;
    public String cellPhone;

    public String homePage;
    public String interests;

    public String about;

    public String workCity;
    public String workState;
    public String workPhone;
    public String workFax;
    public String workAddress;
    public String workCompany;
    public String workDepartment;
    public String workPosition;
    public String birthDay;

    public int age;
    public byte gender;
    public boolean auth; // required

    /** Creates a new instance of UserInfo */
    public UserInfo(Protocol prot, String uin) {
        protocol = prot;
        realUin = uin;
    }
    public UserInfo(Protocol prot) {
        protocol = prot;
        realUin = null;
    }
    public void setProfileView(TextList view) {
        profileView = view;
    }
    public void createProfileView(String name) {
        localName = name;
        setProfileView(new TextList(localName));
        profileView.setModel(new TextListModel());
    }
    public void showProfile() {
        profileView.show();
    }
    void setSeachResultFlag() {
        searchResult = true;
    }


    private static final int INFO_MENU_COPY     = 1040;
    private static final int INFO_MENU_COPY_ALL = 1041;
    private static final int INFO_MENU_GOTO_URL = 1042;
    private static final int INFO_MENU_EDIT     = 1044;
    private static final int INFO_MENU_REMOVE_AVATAR = 1045;
    private static final int INFO_MENU_ADD_AVATAR    = 1046;
    private static final int INFO_MENU_TAKE_AVATAR   = 1047;

///////////////////////////////////////////////////////////////////////////

    public void setOptimalName() {
        Contact contact = protocol.getItemByUIN(uin);
        if (null != contact) {
            String name = contact.getName();
            if (name.equals(contact.getUserId()) || name.equals(protocol.getUniqueUserId(contact))) {
                String newNick = getOptimalName();
                if (newNick.length() != 0) {
                    protocol.renameContact(contact, newNick);
                }
            }
        }
    }
    public synchronized void updateProfileView() {
        if (null == profileView) {
            return;
        }
        TextListModel profile = new TextListModel();

        updateProfileView(profile);

        // #sijapp cond.if (protocols_MRIM is "true") or (protocols_ICQ is "true") #
        if ((null != uin) && !avatarIsLoaded) {
            avatarIsLoaded = true;
            boolean hasAvatarItem = false;
            // #sijapp cond.if protocols_MRIM is "true"#
            hasAvatarItem |= (protocol instanceof Mrim);
            // #sijapp cond.end #
            // #sijapp cond.if protocols_ICQ is "true"#
            hasAvatarItem |= (protocol instanceof Icq);
            // #sijapp cond.end #
            if (hasAvatarItem) {
                protocol.getAvatar(this);
            }
        }
        // #sijapp cond.end #

        if (!searchResult) {
            addMenu();
        }
        profileView.setModel(profile);
    }
    private void updateProfileView(TextListModel profile) {
        profile.clear();

        profile.setHeader("main_info");
        profile.addParam(protocol.getUserIdName(), uin);
        // #sijapp cond.if protocols_ICQ is "true" #
        profile.addParamImage("user_statuses", getStatusAsIcon());
        // #sijapp cond.end #
        profile.addParam("nick",   nick);
        profile.addParam("name", getName());
        profile.addParam("gender", getGenderAsString());
        if (0 < age) {
            profile.addParam("age", Integer.toString(age));
        }
        profile.addParam("email",  email);
        if (auth) {
            profile.addParam("auth", JLocale.getString("yes"));
        }
        profile.addParam("birth_day",  birthDay);
        profile.addParam("cell_phone", cellPhone);
        profile.addParam("home_page",  homePage);
        profile.addParam("interests",  interests);
        profile.addParam("notes",      about);

        profile.setHeader("home_info");
        profile.addParam("addr",  homeAddress);
        profile.addParam("city",  homeCity);
        profile.addParam("state", homeState);
        profile.addParam("phone", homePhones);
        profile.addParam("fax",   homeFax);

        profile.setHeader("work_info");
        profile.addParam("title",    workCompany);
        profile.addParam("depart",   workDepartment);
        profile.addParam("position", workPosition);
        profile.addParam("addr",     workAddress);
        profile.addParam("city",     workCity);
        profile.addParam("state",    workState);
        profile.addParam("phone",    workPhone);
        profile.addParam("fax",      workFax);

        profile.setHeader("avatar");
        profile.addParamImage(null, avatar);
    }
    private void addMenu() {
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     INFO_MENU_COPY);
        menu.addItem("copy_all_text", INFO_MENU_COPY_ALL);
        menu.addItem("goto_url", INFO_MENU_GOTO_URL);
        if (isEditable()) {
            menu.addItem("edit",      INFO_MENU_EDIT);
            // #sijapp cond.if protocols_JABBER is "true" #
            // #sijapp cond.if modules_FILES="true"#
            if (protocol instanceof Jabber) {
                // #sijapp cond.if target is "MIDP2" #
                menu.addItem("take_photo", INFO_MENU_TAKE_AVATAR);
                // #sijapp cond.end #
                if (jimm.modules.fs.FileSystem.isSupported()) {
                    menu.addItem("add_from_fs", INFO_MENU_ADD_AVATAR);
                }
                menu.addItem("remove", INFO_MENU_REMOVE_AVATAR);
            }
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
        menu.setActionListener(new Binder(this));
        profileView.setController(new TextListController(menu, INFO_MENU_COPY));
    }
    public void setProfileViewToWait() {
        TextListModel profile = profileView.getModel();
        profile.clear();
        profile.addParam(protocol.getUserIdName(), uin);
        profile.setInfoMessage(JLocale.getString("wait"));
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     INFO_MENU_COPY);
        menu.addItem("copy_all_text", INFO_MENU_COPY_ALL);
        menu.setActionListener(new Binder(this));
        profileView.setController(new TextListController(menu, INFO_MENU_COPY));
        profileView.setModel(profile);
    }

    public boolean isEditable() {
        boolean isEditable = false;
        // #sijapp cond.if protocols_ICQ is "true" #
        isEditable |= (protocol instanceof Icq);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_OBIMP is "true" #
        isEditable |= (protocol instanceof Obimp);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        isEditable |= (protocol instanceof Jabber);
        // #sijapp cond.end #
        return isEditable && protocol.getUserId().equals(uin)
                && protocol.isConnected();
    }

    public void action(CanvasEx canvas, int cmd) {
        switch (cmd) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                profileView.getController().copy(INFO_MENU_COPY_ALL == cmd);
                profileView.restore();
                break;

            case INFO_MENU_GOTO_URL:
                String text = profileView.getModel().getParText(profileView.getCurrItem());
                ContactList.getInstance().gotoUrl(text);
                break;

            // #sijapp cond.if protocols_ICQ is "true" | protocols_JABBER is "true" | protocols_OBIMP is "true" #
            case INFO_MENU_EDIT:
                new EditInfo(protocol, this).init().show();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            // #sijapp cond.if modules_FILES="true"#
            // #sijapp cond.if target is "MIDP2" #
            case INFO_MENU_TAKE_AVATAR:
                // #sijapp cond.if modules_ANDROID isnot "true" #
                ViewFinder vf = new ViewFinder();
                vf.setPhotoListener(this);
                vf.show();
                // #sijapp cond.else #
                ru.net.jimm.JimmActivity.getInstance().startCamera(this, 640, 480);
                // #sijapp cond.end #
                break;

            // #sijapp cond.end #
            case INFO_MENU_REMOVE_AVATAR:
                removeAvatar();
                protocol.saveUserInfo(this);
                updateProfileView();
                profileView.restore();
                break;

            case INFO_MENU_ADD_AVATAR:
                FileBrowser fsBrowser = new FileBrowser(false);
                fsBrowser.setListener(this);
                fsBrowser.activate();
                break;
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
    }

    // #sijapp cond.if protocols_ICQ is "true" #
    private Icon getStatusAsIcon() {
        if (protocol instanceof Icq) {
            byte statusIndex = StatusInfo.STATUS_NA;
            switch (Util.strToIntDef(status, -1)) {
                case 0: statusIndex = StatusInfo.STATUS_OFFLINE;   break;
                case 1: statusIndex = StatusInfo.STATUS_ONLINE;    break;
                case 2: statusIndex = StatusInfo.STATUS_INVISIBLE; break;
                default: return null;
            }
            return protocol.getStatusInfo().getIcon(statusIndex);
        }
        return null;
    }
    // #sijapp cond.end #
    // Convert gender code to string
    public String getGenderAsString() {
        String[] g = {"", "female", "male"};
        return JLocale.getString(g[gender % 3]);
    }

    private String packString(String str) {
        return (null == str) ? "" : str.trim();
    }
    public String getName() {
        return packString(packString(firstName) + " " + packString(lastName));
    }
    public String getOptimalName() {
        String optimalName = packString(nick);
        if (optimalName.length() == 0) {
            optimalName = packString(getName());
        }
        if (optimalName.length() == 0) {
            optimalName = packString(firstName);
        }
        if (optimalName.length() == 0) {
            optimalName = packString(lastName);
        }
        return optimalName;
    }

    public void setAvatar(Image img) {
        avatar = null;
        if (null != img) {
            int height = NativeCanvas.getScreenHeight() * 2 / 3;
            int width = NativeCanvas.getScreenWidth() - 5;
            Image image = Util.createThumbnail(img, width, height);
            avatar = new Icon(image, 0, 0, image.getWidth(), image.getHeight());
        }
    }

    public void removeAvatar() {
        avatar = null;
        avatarIsLoaded = false;
        // #sijapp cond.if protocols_JABBER is "true" #
        // #sijapp cond.if modules_FILES="true"#
        if (null != vCard) {
            vCard.removeNode("PHOTO");
        }
        // #sijapp cond.end#
        // #sijapp cond.end#
    }
    // #sijapp cond.if protocols_JABBER is "true" #
    // #sijapp cond.if modules_FILES="true"#
    private String getImageType(byte[] data) {
        if (('P' == data[1]) && ('N' == data[2]) && ('G' == data[3])) {
            return "image/png";
        }
        return "image/jpeg";
    }
    public void setBinAvatar(byte[] data) {
        try {

            setAvatar(Image.createImage(data, 0, data.length));

            vCard.setValue("PHOTO", null, "TYPE", getImageType(data));
            vCard.setValue("PHOTO", null, "BINVAL", Util.base64encode(data));
        } catch (Exception ignored) {
        }
    }
    public void onFileSelect(String filename) throws JimmException {
        try {
            JSR75FileSystem file = FileSystem.getInstance();
            file.openFile(filename);
            // FIXME resource leak
            java.io.InputStream fis = file.openInputStream();
            int size = (int)file.fileSize();
            if (size <= 30*1024*1024) {
                byte[] binAvatar = new byte[size];
                int readed = 0;
                while (readed < binAvatar.length) {
                    int read = fis.read(binAvatar, readed, binAvatar.length - readed);
                    if (-1 == read) break;
                    readed += read;
                }
                setBinAvatar(binAvatar);
                binAvatar = null;
            }

            TcpSocket.close(fis);
            file.close();
            fis = null;
            file = null;
        } catch (Throwable ignored) {
        }
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
        profileView.restore();
    }

    public void onDirectorySelect(String directory) {
    }
    public void processPhoto(byte[] data) {
        setBinAvatar(data);
        data = null;
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
        profileView.restore();
    }
    // #sijapp cond.end #
    // #sijapp cond.end #
}
