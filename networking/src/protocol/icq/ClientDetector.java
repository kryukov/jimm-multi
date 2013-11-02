/*
 * ClientDetector.java
 *
 * Created on 19 Октябрь 2009 г., 19:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_ICQ is "true" #
// #sijapp cond.if modules_CLIENTS is "true" #
package protocol.icq;

import jimmui.view.icons.*;
import java.io.*;
import jimm.comm.*;
import protocol.ui.ClientInfo;
import protocol.net.TcpSocket;

/**
 *
 * @author Vladimir Krukov
 */
public class ClientDetector {
    /** Creates a new instance of ClientDetector */
    private boolean unloaded = true;
    private String[] clients;
    private short[] iconIndex;
    private byte[] code;
    private int[] dataFp;
    private byte[] dataGuid;

    private final ImageList clientIcons = ImageList.createImageList("/clients.png");

    public static final ClientDetector instance = new ClientDetector();
    public boolean has(int id) {
        return !unloaded && (0 <= id) && (id < clients.length);
    }
    public ClientInfo get() {
        return new ClientInfo(clientIcons, iconIndex, clients);
    }

    private short[] readBytes(DataInputStream in, int size) throws IOException {
        short[] array = new short[size];
        for (int i = 0; i < size; ++i) {
            array[i] = (short)in.readUnsignedByte();
        }
        return array;
    }

    public ClientDetector() {
        InputStream stream = null;
        DataInputStream is = null;
        try {
            stream = jimm.Jimm.getResourceAsStream("/clients.bin");
            is = new DataInputStream(stream);

            code = new byte[is.readInt()];
            is.readFully(code);

            dataGuid = new byte[is.readInt()];
            is.readFully(dataGuid);

            dataFp = new int[is.readInt()];
            for (int i = 0; i < dataFp.length; ++i) {
                dataFp[i] = is.readInt();
            }

            clients = new String[is.readUnsignedByte()];
            for (int i = 0; i < clients.length; ++i) {
                clients[i] = is.readUTF();
            }

            iconIndex = readBytes(is, clients.length);
            for (int i = 0; i < iconIndex.length; ++i) {
                iconIndex[i]--;
            }

            unloaded = false;
        } catch (Exception e) {
            unloaded = true;
            code = null;
            dataGuid = null;
            dataFp = null;
            clients = null;
            code = new byte[0];
            clients = new String[]{"None"};
        }
        TcpSocket.close(stream);
        TcpSocket.close(is);
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        _g();
        // #sijapp cond.end #
    }
    private int getByte(byte[] data, int offset) {
        return ((int)data[offset]) & 0xFF;
    }
    private int getWord(byte[] data, int offset) {
        return (data[offset + 1] & 0xFF) | ((data[offset + 0] & 0xFF) << 8);
    }
    private byte[] getGuid(byte[] buf, int offset) {
        byte[] guid = new byte[16];
        System.arraycopy(buf, offset, guid, 0, 16);
        return guid;
    }

    private int findGuid(byte[] guids, int ip) {
        final int packed = getByte(code, ip++);
        //int offset = packed >> 4;
        final int length = packed;
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        jimm.modules.DebugLog.assert0("length is 0", (0 == length));
        // #sijapp cond.end #
        final int where = getWord(code, ip);
        int byteIndex = 0;
        for (int guidNum = 0; guidNum < guids.length; guidNum += 16) {
            if (guids[guidNum] == dataGuid[where]) {
                for (byteIndex = 0; byteIndex < length; ++byteIndex) {
                    if (guids[guidNum + byteIndex] != dataGuid[where + byteIndex]) {
                        break;
                    }
                }
                if (length == byteIndex) {
                    return guidNum;
                }
            }
        }
        return -1;
    }
    /**
     * OpCode bits:
     * 0 - caps count    (capsCount(1b))
     * 1 - fp0           (fpIndex(1b))
     * 2 - fp1           (fpIndex(1b))
     * 3 - fp2           (fpIndex(1b))
     * 4 - caps          (count(1b) (length(1b) where(2b))+)
     * 5 - !caps         (count(1b) (length(1b) where(2b))+)
     * 6 - Version (type(1b)=
     *     1 - GUID string      (length(1b) where(2b)) (offset length)(1b))
     *     2 - GUID bytes       (length(1b) where(2b)) (offset length)(1b))
     *     3 - GUID Miranda     (length(1b) where(2b)) (offset=0 length=0)(1b))
     *     4 - FP bytes         (fpIndex(1b))
     *     5 - FP string        (fpIndex(1b))
     *     6 - FP int           (fpIndex(1b))
     *
     * 6 - Version        (type(1b=0)     (length(1b) where(2b)) (offset length)(1b))
     * 6 - Version        (type(1b=...)   (...)
     * 7 - protocol      (protocol(2b))
     */
    private boolean execVMProc(IcqContact contact, byte[] guids, int[] fps, int protocol, int ip) {
        byte opCode = code[ip++];
        if (0 != (opCode & 0x80)) {
            final int proto = getWord(code, ip);
            if (protocol != proto) {
                return false;
            }
            ip += 2;
        }
        if (0 != (opCode & 0x01)) {
            if ((guids.length / 16) != code[ip++]) {
                return false;
            }
        }
        if (0 != (opCode & 0x02)) {
            if (fps[0] != dataFp[getByte(code, ip++)]) {
                return false;
            }
        }
        if (0 != (opCode & 0x04)) {
            if (fps[1] != dataFp[getByte(code, ip++)]) {
                return false;
            }
        }
        if (0 != (opCode & 0x08)) {
            if (fps[2] != dataFp[getByte(code, ip++)]) {
                return false;
            }
        }
        // Caps is contained
        if (0 != (opCode & 0x10)) {
            int guidsCount = code[ip++];
            for (int i = 0 ; i < guidsCount; ++i) {
                if (-1 == findGuid(guids, ip)) {
                    return false;
                }
                ip += 3;
            }
        }
        // Caps isn't contained
        if (0 != (opCode & 0x20)) {
            int guidsCount = code[ip++];
            for (int i = 0 ; i < guidsCount; ++i) {
                if (-1 != findGuid(guids, ip)) {
                    return false;
                }
                ip += 3;
            }
        }
        String version = null;
        if (0 != (opCode & 0x40)) {
            int versionType = code[ip++];
            switch (versionType) {
                case 0:
                case 1:
                case 2:
                    int versionGuid = findGuid(guids, ip);
                    if (-1 == versionGuid) {
                        return false;
                    }
                    ip += 3;
                    int versionOffset = ((int)code[ip] >> 4) & 0x0F;
                    int versionLength = ((int)code[ip])      & 0x0F;
                    ++ip;
                    version = getGuidVersion(guids, versionGuid, versionOffset, versionLength, versionType);
                    break;
                case 3:
                case 4:
                case 5:
                    version = getFpVersion(fps[getByte(code, ip++)], versionType - 3);
                    break;
            }

        }
        //println(ip + ": " + clients[code[ip]]);
        System.out.print("client " + getByte(code, ip));
        contact.setClient((short)getByte(code, ip), version);
        return true;
    }
    private String getGuidVersion(byte[] guids, int guidOffset, int offset, int length, int versionType) {
        if (0 == versionType) {
            return StringUtils.byteArrayToString(guids, guidOffset + offset, length).trim();
        }
        if (1 == versionType) {
            StringBuilder version = new StringBuilder();
            offset += guidOffset;
            for (int i = 0; i < length; ++i) {
                if (0 < i) version.append('.');
                version.append(guids[offset + i] | 0xFF);
            }
            return version.toString();
        }
        byte[] buf = getGuid(guids, guidOffset);
        byte first = buf[0];
        if (('i' == first) || ('s' == first) || ('e' == first)) {
            String version = makeVersion(buf[0x4] & 0x7F, buf[0x5], buf[0x6], buf[0x7]);
            if ((buf[0x4] & 0x80) != 0) {
                version += "a";
            }
            return version;
        }
        if (('M' == buf[0]) && ('i' == buf[1])) {
            if ( (buf[0xC] == 0) && (buf[0xD] == 0) && (buf[0xE] == 0) && (buf[0xF] == 1) ) {
                return "0.1.2.0";
            } else if ((buf[0xC] == 0) && (buf[0xD] <= 3) && (buf[0xE] <= 3) && (buf[0xF] <= 1)) {
                return makeVersion(0, buf[0xD], buf[0xE], buf[0xF]);
            } else {
                String ver = makeVersion(buf[0x8] & 0x7F, buf[0x9], buf[0xA], buf[0xB]);
                if ((buf[0x8] & 0x80) != 0) {
                    ver += "a";
                }
                return ver;
            }
        }
        String version = makeVersion(buf[offset + 0x0] & 0x7F, buf[offset + 0x1],
                buf[offset + 0x2], buf[offset + 0x3]);
        if ((buf[offset + 0x0] & 0x80) != 0) {
            version += "a";
        }
        return version;
    }
    private String getFpVersion(int fp, int versionType) {
        switch (versionType) {
            case 0: return makeVersion(getByte(fp, 24), getByte(fp, 16), getByte(fp, 8), getByte(fp, 0));
            case 1: return "" + getByte(fp, 24) + getByte(fp, 16) + getByte(fp, 8) + getByte(fp, 0);
            case 2: return String.valueOf(fp);
        }
        return null;
    }
    private String makeVersion(int v0, int v1, int v2, int v3) {
        String ver = (v0 | 0xFF) + "." + (v1 | 0xFF);
        if (0 <= v2 || 0 <= v3) {
            ver += "." + (v2 | 0xFF);
            if (0 <= v3) {
                ver += "." + (v3 | 0xFF);
            }
        }
        return ver;
    }
    private int getByte(int val, int index) {
        return ((val >> index) & 0xFF);
    }
    // #sijapp cond.if modules_DEBUGLOG is "true" #
    private void println(String s) {
        //System.out.println(s);
    }
    private void _g() {
        int ip_ = 0;
        int ip = 0;
        try {
            byte[] _code = code;
            while (ip_ < _code.length) {
                int length = getByte(_code, ip_);
                byte[] cli  = new byte[length];
                System.arraycopy(_code, ip_ + 1, cli, 0, length);
                ip_ += length + 1;

                ip = 0;
                byte opCode = cli[ip++];
                if (0 != (opCode & 0x80)) {
                    println("protocol " + getWord(cli, ip));
                    ip += 2;
                }
                if (0 != (opCode & 0x01)) {
                    println("guid count " + cli[ip++]);
                }
                if (0 != (opCode & 0x02)) {
                    println("FP1 " + dataFp[getByte(cli, ip++)]);
                }
                if (0 != (opCode & 0x04)) {
                    println("FP2 " + dataFp[getByte(cli, ip++)]);
                }
                if (0 != (opCode & 0x08)) {
                    println("FP3 " + dataFp[getByte(cli, ip++)]);
                }
                // Caps is contained
                if (0 != (opCode & 0x10)) {
                    int guidsCount = cli[ip++];
                    println("contains " + guidsCount);
                    for (int i = 0 ; i < guidsCount; ++i) {
                        ip += 3;
                    }
                }
                // Caps isn't contained
                if (0 != (opCode & 0x20)) {
                    int guidsCount = cli[ip++];
                    println("uncontains " + guidsCount);
                    for (int i = 0 ; i < guidsCount; ++i) {
                        ip += 3;
                    }
                }
                String version = null;
                if (0 != (opCode & 0x40)) {
                    int versionType = cli[ip++];
                    switch (versionType) {
                        case 0:
                        case 1:
                        case 2:
                            println("guid ver");
                            final int length_ = getByte(cli, ip++);
                            final int where_ = getWord(cli, ip);
                            ip += 2;
                            byte[] dddddddd = new byte[length_];
                            System.arraycopy(dataGuid, where_, dddddddd, 0, length_);
                            ip++;
                            break;
                        case 3:
                        case 4:
                        case 5:
                            ip++;
                            println("fp ver");
                            break;
                    }

                }
                println("type " + getByte(cli, ip));
                println("client " + clients[getByte(cli, ip)]);
            }
        } catch (Exception e) {
            println("type " + ip_ + ":" + ip);
        }
    }
    // #sijapp cond.end #

    public void execVM(IcqContact contact, byte[] guids, int[] fps, int protocol) {
        contact.setClient(ClientInfo.CLI_NONE, null);
        if (unloaded) return;
        try {
            //jimm.modules.DebugLog.dump("caps", guids);
            //jimm.modules.DebugLog.println("fps " + fps[0] + ", " + fps[1] + ", " + fps[2]);
            //jimm.modules.DebugLog.println("protocol " + protocol);
            int ip = 0;
            while (ip < code.length) {
                if (execVMProc(contact, guids, fps, protocol, ip + 1)) break;
                ip = ip + (code[ip] & 0xFF) + 1;
            }
        } catch (Exception ignored) {
        }
    }
}
// #sijapp cond.end #
// #sijapp cond.end #