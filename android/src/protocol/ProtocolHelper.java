package protocol;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 13.07.13 12:38
 *
 * @author vladimir
 */
public class ProtocolHelper {
    public static void connect(Protocol p) {
        jimm.modules.DebugLog.println("connecting to " + p.getUserId());
        p.startConnection();
    }
}
