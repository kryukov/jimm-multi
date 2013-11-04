package jimm.comm;

import java.io.ByteArrayOutputStream;

public class OutStream {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    public OutStream() {
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }

    public int size() {
        return stream.size();
    }

    public void reset() {
        try {
            stream.reset();
        } catch (Exception ignored) {
        }
    }

    public void writeZeroes(int count) {
        for (int i = 0; i < count; ++i) {
            writeByte(0);
        }
    }

    public void writeWordBE(int value) {
        try {
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
            stream.write(value & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeWordLE(int value) {
        try {
            stream.write(value & 0xFF);
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array) {
        try {
            stream.write(array);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array, int offset, int length) {
        try {
            stream.write(array, offset, length);
        } catch (Exception ignored) {
        }
    }

    public void writeDWordBE(long longValue) {
        try {
            int value = (int) longValue;
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0x0000FF00) >> 8) & 0xFF);
            stream.write(value & 0x000000FF);
        } catch (Exception ignored) {
        }
    }

    public void writeDWordLE(long longValue) {
        try {
            int value = (int) longValue;
            stream.write(value & 0x000000FF);
            stream.write(((value & 0x0000FF00) >> 8) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByte(int value) {
        try {
            stream.write(value);
        } catch (Exception ignored) {
        }
    }

    public void writeShortLenAndUtf8String(String value) {
        byte[] raw = StringUtils.stringToByteArrayUtf8(value);
        writeByte(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeLenAndUtf8String(String value) {
        byte[] raw = StringUtils.stringToByteArrayUtf8(value);
        writeWordBE(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeUtf8String(String value) {
        byte[] raw = StringUtils.stringToByteArrayUtf8(value);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeProfileAsciizTLV(int type, String value) {
        value = StringUtils.notNull(value);

        byte[] raw = StringUtils.stringToByteArray1251(value);
        writeWordLE(type);
        writeWordLE(raw.length + 3);
        writeWordLE(raw.length + 1);
        writeByteArray(raw);
        writeByte(0);
    }

    public void writeTlvECombo(int type, String value, int code) {
        value = StringUtils.notNull(value);
        writeWordLE(type);
        byte[] raw = StringUtils.stringToByteArray(value);
        writeWordLE(raw.length + 4);
        writeWordLE(raw.length + 1);
        try {
            stream.write(raw, 0, raw.length);
            stream.write(0);
            stream.write(code);
        } catch (Exception ignored) {
        }
    }

    public void writeTLV(int type, byte[] data) {
        writeWordBE(type);
        int length = (null == data) ? 0 : data.length;
        writeWordBE(length);
        if (length > 0) {
            try {
                stream.write(data, 0, data.length);
            } catch (Exception ignored) {
            }
        }
    }

    public void writeTLVWord(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(2);
        writeWordBE(wordValue);
    }

    public void writeTLVDWord(int type, long wordValue) {
        writeWordBE(type);
        writeWordBE(4);
        writeDWordBE(wordValue);
    }

    public void writeTLVByte(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(1);
        writeByte(wordValue);
    }
}