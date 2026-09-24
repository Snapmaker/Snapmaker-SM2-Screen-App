package fabscreen.libraries.legacy.server.socket;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import okio.Buffer;
import okio.ByteString;


/**
 * Discover Server: Socket server for device discovery.
 * <p>
 * Start server:
 * discoverServer = new DiscoverServer(context, "Snapmaker");
 * discoverServer.start()
 */
public class DiscoverServer extends Thread {
    private static final int BIND_PORT = 20054;
    private static final String DISCOVER_MESSAGE = "discover";

    private WifiManager mWifiManager;
    private DatagramSocket mSocket;

    /**
     * DiscoverServer: bind socket and wait for client to search
     *
     * @param context     Use for creating WifiManager to get current ip
     */
    public DiscoverServer(Context context) {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void run() {
        try {
            mSocket = new DatagramSocket(BIND_PORT);

            byte[] data = new byte[64];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            while (true) {
                mSocket.receive(packet);

                String message = new String(data, 0, packet.getLength(), Charset.forName("UTF-8"));

                if (message.equals(DISCOVER_MESSAGE)) {
                    byte[] bytes = getResponse();
                    SocketAddress address = packet.getSocketAddress();
                    DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, address);
                    mSocket.send(sendPacket);
                    Logger.d("Discover request, response sent.");
                }
            }
        } catch (IOException e) {
            LogHelper.log(e);
        } finally {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        }
    }

    @Override
    public void interrupt() {
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }

        super.interrupt();
    }

    private byte[] getResponse() {
        Model model = BaseApplication.getInstance().getModel();

        final String machineName = model.getPreferences().getMachineName();
        final String machineModal = model.getPreferences().getMachineModel();
        FabPacketContent.MachineStatus status = model.getMachineController().getMachineStatus();

        // Build response string
        // {name}@{ip}|model:{model}|status:{status}
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%s@%s", machineName, getHostAddress()));
        stringBuilder.append(String.format("|%s:%s", "model", machineModal));

        if (status.printerStatus == 0) {
            stringBuilder.append(String.format("|%s:%s", "status", "IDLE"));
        } else if (status.printerStatus == 1 || status.printerStatus == 3) {
            stringBuilder.append(String.format("|%s:%s", "status", "RUNNING"));
        } else {
            stringBuilder.append(String.format("|%s:%s", "status", "PAUSED"));
        }

        String description = stringBuilder.toString();

        Buffer buffer = new Buffer();
        buffer.write(ByteString.encodeUtf8(description));
        return buffer.readByteArray();
    }

    private String getHostAddress() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return intToIp(wifiInfo.getIpAddress());
    }

    private static String intToIp(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
    }
}
