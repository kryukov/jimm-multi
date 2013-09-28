package protocol.xmpp;

// #sijapp cond.if protocols_JABBER is "true" #
import jimm.Jimm;
import jimm.search.UserInfo;

import javax.microedition.lcdui.Image;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 17.03.13 0:20
 *
 * @author vladimir
 */
public class AvatarLoader implements Runnable {
    private byte[] avatarBytes = null;
    private UserInfo userInfo;
    private XmlNode bs64photo;

    public AvatarLoader(UserInfo userInfo, XmlNode bs64photo) {
        this.userInfo = userInfo;
        this.bs64photo = bs64photo;
    }

    public void run() {
        avatarBytes = userInfo.isEditable()
                ? bs64photo.getBinValue()
                : bs64photo.popBinValue();
        bs64photo = null;
        try {
            if ((null != avatarBytes) && Jimm.getJimm().phone.hasMemory(avatarBytes.length * 2)) {
                Image avatar = Image.createImage(avatarBytes, 0, avatarBytes.length);
                avatarBytes = null;
                userInfo.setAvatar(avatar);
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError ignored) {
        } catch (Exception ignored) {
        }
    }
}
// #sijapp cond.end #
