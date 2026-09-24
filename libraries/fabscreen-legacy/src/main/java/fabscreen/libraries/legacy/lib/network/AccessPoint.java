package fabscreen.libraries.legacy.lib.network;

import android.net.wifi.WifiInfo;

import java.util.ArrayList;

public class AccessPoint {
    public static AccessPoint NULL_ACCESS_POINT = new AccessPoint();

    public enum ConnectState {
        IDLE,
        // user clicked on an AP.
        SELECTED,
        // user clicked connect, waiting for ASSOCIATING/ASSOCIATED state.
        CONFIRMED,
        // ASSOCIATING/ASSOCIATED
        CONNECTING,
        CONNECTED
    }

    private String SSID;
    private String bssid;
    private String password;
    private float rssi;
    private String encryptionType;
    private int networkId;
    private ConnectState mConnectState;
    /**
     * aps are relative AccessPoints who share the same SSID while different bssid
     * we will treat them as one hotspot
     */
    private ArrayList<AccessPoint> relativeAPs;

    public AccessPoint() {
        this.SSID = "";
        this.bssid = "";
        this.password = "";
        this.rssi = 0;
        this.encryptionType = "";
        this.networkId = -1;
        this.mConnectState = ConnectState.IDLE;
        this.relativeAPs = new ArrayList<>();
    }

    public void setAccessPoint(WifiInfo info) {
        this.SSID = info.getSSID().replace("\"", "").trim();
        this.bssid = info.getBSSID();
        this.networkId = info.getNetworkId();
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String ssid) {
        this.SSID = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public float getRssi() {
        return rssi;
    }

    public void setRssi(float signalStrength) {
        this.rssi = signalStrength;
    }

    public String getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public boolean isEncrypted() {
        return encryptionType.contains("WPA") || encryptionType.contains("WEP");
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    public ConnectState getConnectState() {
        return mConnectState;
    }

    public void setConnectState(ConnectState connectState) {
        this.mConnectState = connectState;
    }

    public ArrayList<AccessPoint> getRelativeAPs() {
        return relativeAPs;
    }

    public void setRelativeAPs(ArrayList<AccessPoint> relativeAPs) {
        this.relativeAPs = relativeAPs;
    }
}
