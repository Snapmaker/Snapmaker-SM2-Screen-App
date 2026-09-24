package fabscreen.libraries.legacy.data.serial.fabpacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

public class FabPacket {
    private static final ByteString PROTO_MARKER = ByteString.decodeHex("aa55");

    public static final byte GCODE_REQUEST_EVENT_ID = 0x01;
    public static final byte GCODE_RESPONSE_EVENT_ID = 0x02;
    public static final byte PRINT_GCODE_REQUEST_EVENT_ID = 0x03;
    public static final byte PRINT_GCODE_RESPONSE_EVENT_ID = 0x04;
    public static final byte GCODE_REQUEST_EXTEND_EVENT_ID = 0x05;
    public static final byte GCODE_RESPONSE_EXTEND_EVENT_ID = 0x06;
    public static final byte STATUS_SYNC_REQUEST_EVENT_ID = 0x07;
    public static final byte STATUS_RESPONSE_EVENT_ID = 0x08;
    public static final byte SETTINGS_REQUEST_EVENT_ID = 0x09;
    public static final byte SETTINGS_RESPONSE_EVENT_ID = 0x0a;
    public static final byte MOVEMENT_REQUEST_EVENT_ID = 0x0b;
    public static final byte MOVEMENT_RESPONSE_EVENT_ID = 0x0c;
    public static final byte LASER_CAMERA_OPERATION_REQUEST_EVENT_ID = 0x0d;
    public static final byte LASER_CAMERA_OPERATION_RESPONSE_EVENT_ID = 0x0e;
    public static final byte ADD_ON_OPERATION_REQUEST_EVENT_ID = 0x11;
    public static final byte ADD_ON_OPERATION_RESPONSE_EVENT_ID = 0x12;
    public static final byte PRINT_BATCH_GCODE_REQUEST_EVENT_ID = 0x13;
    public static final byte PRINT_BATCH_GCODE_RESPONSE_EVENT_ID = 0x14;

    public static final byte UPDATE_REQUEST_EVENT_ID = (byte)0xa9;
    public static final byte UPDATE_RESPONSE_EVENT_ID = (byte)0xaa;

    private ByteString marker;
    private int length;
    private byte version;
    private byte lengthVerify;
    private int checksum;
    private byte eventId;
    private byte[] content;

    private FabPacket() {
        marker = FabPacket.PROTO_MARKER;
        version = 0x01;
    }

    public FabPacket(byte[] packet) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(new ByteArrayInputStream(packet)));
        marker = source.readByteString(2);
        length = source.readShort();
        version = source.readByte();
        lengthVerify = source.readByte();
        checksum = source.readShort();
        eventId = source.readByte();
        content = source.readByteArray(length - 1);
    }

    public byte getEventId() {
        return eventId;
    }

    public void setEventId(byte eventId) {
        this.eventId = eventId;
    }

    public byte getSubeventId() {
        if (content.length == 0) {
            return 0;
        }
        return content[0];
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public static FabPacket create() {
        return new FabPacket();
    }

    public void build() {
        length = content.length + 1;
        lengthVerify = (byte)((length >> 8) ^ (length & 0xFF));
        checksum = 0;
        byte[] bytes = toByteArray();
        checksum = FabPacket.calculateChecksum(bytes, 8, length);
    }

    public static int calculateChecksum(byte[] buffer, int offset, int length) {
        // TCP/IP checksum
        // https://locklessinc.com/articles/tcp_checksum/
        int sum = 0;

        for (int i = 0; i < length - 1; i += 2) {
            sum += (buffer[offset + i] & 0xFF) * 0x100 + (buffer[offset + i + 1] & 0xFF);
        }

        if ((length & 1) > 0) {
            sum += (buffer[offset + length - 1] & 0xFF);
        }

        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return ((~sum) & 0xFFFF);
    }

    public int getKey() {
        if (eventId <= 0x04) {
            return ((eventId + 1) & 0xFE) * 1000;
        } else {
            return ((eventId + 1) & 0xFE) * 1000 + content[0];
        }
    }

    public byte[] toByteArray() {
        Buffer buffer = new Buffer();
        buffer.write(marker);
        buffer.writeShort(length);
        buffer.writeByte(version);
        buffer.writeByte(lengthVerify);
        buffer.writeShort(checksum);
        buffer.writeByte(eventId);
        buffer.write(content);
        return buffer.readByteArray();
    }
}
