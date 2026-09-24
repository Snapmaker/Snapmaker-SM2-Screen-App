package com.snapmaker.fabscreen;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketBuilder;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketInputStream;
import okio.ByteString;

public class FabPacketTest {
    @Test
    public void packetConstruct() {
        String gcode = "G0 X0 Y0 Z0\n";
        FabPacket packetIn = FabPacketBuilder.gcodeRequest(gcode, 0);

        try {
            FabPacket packetOut = new FabPacket(packetIn.toByteArray());

            Assert.assertEquals(0x01, packetOut.getEventId());

//            DebugUtil.printByteArray(packetIn.toByteArray());
//            DebugUtil.printByteArray(packetOut.toByteArray());

            byte[] inArray = packetIn.toByteArray();
            byte[] outArray = packetOut.toByteArray();
            for (int i = 0; i < inArray.length; i++) {
                Assert.assertEquals(inArray[i], outArray[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checksum() {
        ByteString bs = ByteString.of(
                (byte) 0x11, (byte) 0x11, (byte) 0x22, (byte) 0x33,
                (byte) 0xaa, (byte) 0x55, (byte) 0x00, (byte) 0x06,
                (byte) 0x01, (byte) 0x06, (byte) 0xcb, (byte) 0x06,
                (byte) 0x01, (byte) 0x47, (byte) 0x30, (byte) 0x20,
                (byte) 0x58, (byte) 0x30, (byte) 0xFF);

        int checksum = FabPacket.calculateChecksum(bs.toByteArray(), 12, 7);
        Assert.assertEquals(30057, checksum);
    }

    @Test
    public void checksum2() {
        // status sync request
        ByteString bs = ByteString.of(
                (byte)0xaa, (byte)0x55, (byte)0x00, (byte)0x02,
                (byte)0x01, (byte)0x02, (byte)0x00, (byte)0x00,
                (byte)0x07, (byte)0x01);

        int checksum = FabPacket.calculateChecksum(bs.toByteArray(), 8, 2);
        Assert.assertEquals(63742, checksum);
    }

    @Test
    public void reader() {
        ByteString bs = ByteString.of(
                (byte)0x11,
                (byte)0xaa, (byte)0x55, (byte)0x00, (byte)0x06,
                (byte)0x01, (byte)0x06, (byte)0xcb, (byte)0x06,
                (byte)0x01, (byte)0x47, (byte)0x30, (byte)0x20,
                (byte)0x58, (byte)0x30, (byte)0xFF);

        InputStream in = new ByteArrayInputStream(bs.toByteArray());
        FabPacketInputStream reader = new FabPacketInputStream(in);
        while (true) {
            try {
                FabPacket packet = reader.readPacket();

                if (packet == null) {
                    break;
                }

                Assert.assertEquals(0x01, packet.getEventId());
//                Assert.assertEquals("G0 X0", packet.getGcode());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
