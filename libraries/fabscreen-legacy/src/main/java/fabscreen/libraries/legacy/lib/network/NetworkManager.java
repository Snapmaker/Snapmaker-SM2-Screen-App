package fabscreen.libraries.legacy.lib.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * NetworkManager is a thin wrapper over {@link WifiManager} and {@link ConnectivityManager}.
 * It provides easy-to-use Wi-Fi related APIs:
 * <p>
 * - RxJava style API to enable/disable Wi-Fi.
 * - RxJava observable API to scan and get scan result (access points).
 * - Connect to access point without knowing configuration of {@link WifiConfiguration}.
 * <p>
 * Checkout documents below for how to perform network operations:
 * - https://developer.android.com/training/basics/network-ops
 */
public class NetworkManager {

    private Context mContext;

    @NonNull
    private WifiManager mWifiManager;

    // network scan
    private List<AccessPoint> mAccessPointList = new ArrayList<>();
    private Subject<List<AccessPoint>> mAccessPointListSubject = BehaviorSubject.createDefault(new ArrayList<>());
    private PublishSubject<Intent> mSupplicantStateSubject = PublishSubject.create();

    private boolean registered = false;

    // network config
    private WifiConfiguration mActiveWifiConfiguration = null;
    private int configurationRetryCount = 0;

    // Receiver to receive scan results and connect results.
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    // In our test, it takes 2 to 12 seconds to get completed search result.
                    // A.K.A. SCAN_RESULTS_AVAILABLE_ACTION action is only sent when search completes.
                    boolean isResultsUpdated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (isResultsUpdated) {
                        List<ScanResult> scanResults = getScanResults();
                        if (scanResults != null) {
                            // Convert ScanResult to AccessPoint List
                            mAccessPointList = deriveAccessPointListFromScanResults(scanResults);
                            // Merge duplicated access points in AccessPoint List
                            mAccessPointList = mergeRelativeAPs(mAccessPointList);
                            mAccessPointListSubject.onNext(mAccessPointList);
                            if (mAccessPointList == null || mAccessPointList.isEmpty()) {
                                Logger.d("NetworkManager: mAccessPointList is NULL.");
                            }
                        } else {
                            Logger.d("NetworkManager: get scan result NULL.");
                        }
                    } else {
                        Logger.d("NetworkManager: get scan result fails.");
                    }
                    break;
                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                    mSupplicantStateSubject.onNext(intent);
                    break;
            }
        }
    };

    public NetworkManager(Context context) {
        mContext = context;

        // We assume that Wi-Fi Manager is always not NULL
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Initialize network 5000ms later, wait for Wi-Fi to start
        // TODO: initial it when Wi-Fi enabled?
        new Handler().postDelayed(this::initConfiguredNetworks, 5000);
    }

    /**
     * Initialize NetworkManager state by scanning on currently configured networks.
     */
    private void initConfiguredNetworks() {
        List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();
        if (wifiConfigurations == null) {
            return;
        }

        // Scan for active Wi-Fi configuration
        mActiveWifiConfiguration = null;
        for (int i = 0; i < wifiConfigurations.size(); i++) {
            WifiConfiguration wifiConfiguration = wifiConfigurations.get(i);

            // comment this line if not debugging Wi-Fi
            if (wifiConfiguration.status == WifiConfiguration.Status.CURRENT) {
                Logger.d("NetworkManager: found Wi-Fi configuration #%d %s, state = CURRENT.", wifiConfiguration.networkId, wifiConfiguration.SSID);
            } else if (wifiConfiguration.status == WifiConfiguration.Status.DISABLED) {
                Logger.d("NetworkManager: found Wi-Fi configuration #%d %s, state = DISABLED.", wifiConfiguration.networkId, wifiConfiguration.SSID);
            } else {
                Logger.d("NetworkManager: found Wi-Fi configuration #%d %s, state = ENABLED.", wifiConfiguration.networkId, wifiConfiguration.SSID);
            }

            // 0 - current, 1 - disabled, 2 - enabled
            // active configuration = CURRENT
            if (wifiConfiguration.status == WifiConfiguration.Status.CURRENT) {
                mActiveWifiConfiguration = wifiConfiguration;
            }
        }

        // Get networkId of active Wi-Fi configuration
        if (mActiveWifiConfiguration != null) {
            Logger.d("NetworkManager: current Wi-Fi configuration is #%d %s", mActiveWifiConfiguration.networkId, mActiveWifiConfiguration.SSID);

            // Disable all configured network other than the active one
            for (int i = 0; i < wifiConfigurations.size(); i++) {
                WifiConfiguration wifiConfiguration = wifiConfigurations.get(i);

                if (wifiConfiguration != mActiveWifiConfiguration) {
                    removeOrDisableWifiConfiguration(wifiConfiguration);
                }
            }
        } else {
            // If any Wi-Fi configuration exists, we will check active one later.
            if (wifiConfigurations.size() > 0) {
                configurationRetryCount++;

                if (configurationRetryCount < 6) {
                    new Handler().postDelayed(this::initConfiguredNetworks, 5000);
                } else {
                    // If failed 6 times, enable the first config and disable all rests
                    for (int i = 0; i < wifiConfigurations.size(); i++) {
                        WifiConfiguration wifiConfiguration = wifiConfigurations.get(i);
                        if (i == 0) {
                            enableWifiConfiguration(wifiConfiguration);
                        } else {
                            removeOrDisableWifiConfiguration(wifiConfiguration);
                        }
                    }
                }
            }
        }
    }

    /**
     * check if Wi-Fi is enabled.
     * <p>
     * TODO: Maybe cache the Wi-Fi state if the interface takes long.
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * Enable or disable Wi-Fi.
     * <p>
     * Applications must have the {@link Manifest.permission#CHANGE_WIFI_STATE}
     * permission to toggle wifi.
     *
     * @return a {@link Single<Boolean>} indicates whether the operation is successful.
     */
    public Single<Boolean> setWifiEnabled(boolean enabled) {
        if (mWifiManager.isWifiEnabled() == enabled) {
            return Single.just(true);
        }

        if (mWifiManager.setWifiEnabled(enabled)) {
            // Check Wi-Fi enabled state repeatedly until its state changed as expected
            // In our tests, it will usually takes 1 to 3 seconds to wait for Wi-Fi being WIFI_STATE_ENABLED state.
            return Observable.interval(100, TimeUnit.MILLISECONDS)
                    .takeUntil(tick -> mWifiManager.isWifiEnabled() == enabled)
                    .map(tick -> true)
                    .lastOrError();
        } else {
            return Single.just(false);
        }
    }

    /**
     * Request a scan for access points. Returns immediately. The availability
     * of the results is made known later by means of an asynchronous event sent
     * on completion of the scan.
     */
    public void startScan(Context context) {
        Logger.i("NetworkManager start scanning...");
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            context.registerReceiver(mWifiStateReceiver, filter);
            registered = true;
        }
        mWifiManager.startScan();
    }

    public void startScanSilently(Context context) {
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            context.registerReceiver(mWifiStateReceiver, filter);
            registered = true;
        }
        mWifiManager.startScan();
    }

    // testing function. It's not completely tested
    public void stopScan(Context context) {
        Logger.i("NetworkManager stop scanning...");
        if (registered) {
            context.unregisterReceiver(mWifiStateReceiver);
            registered = false;
        }
    }

    /**
     * Get network scan result.
     */
    @Nullable
    private List<ScanResult> getScanResults() {
        return mWifiManager.getScanResults();
    }

    /**
     * Config {@link WifiConfiguration} wifiConfiguration according to {@link AccessPoint} accessPoint.
     *
     * @param wifiConfiguration {@link WifiConfiguration} to be configured.
     * @param accessPoint       access point to be used to create {@link WifiConfiguration}
     */
    private void configWifiConfiguration(WifiConfiguration wifiConfiguration, AccessPoint accessPoint) {
        String SSID = accessPoint.getSSID();
        String password = accessPoint.getPassword();

//        wifiConfiguration.SSID = "\"" + SSID + "\"";
        wifiConfiguration.SSID = SSID;

        Logger.d("Configuring Wi-Fi configuration #%d %s", wifiConfiguration.networkId, wifiConfiguration.SSID);

        String encryptionType = accessPoint.getEncryptionType();
        if (encryptionType.contains("WEP")) {
            // special handling according to password length is a must for WEP
            Logger.d("encryption type = WEP");
            int l = password.length();
            if (((10 == l || (26 == l) || (58 == l))) && (password.matches("[0-9A-FA-f]*"))) {
                wifiConfiguration.wepKeys[0] = password;
            } else {
                wifiConfiguration.wepKeys[0] = "\"" + password + "\"";
            }
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if (encryptionType.contains("WPA")) {
            Logger.d("encryption type = WPA");
            wifiConfiguration.preSharedKey = "\"" + password + "\"";
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else {
            Logger.d("encryption type = None");
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
    }

    private boolean enableWifiConfiguration(@NonNull WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.networkId == -1) return false;

        // This check looks not useful since removed wifi config won't have a DISABLE status.
        // if (wifiConfiguration.status != WifiConfiguration.Status.DISABLED) return false;

        boolean success = mWifiManager.enableNetwork(wifiConfiguration.networkId, true);
        success = success && mWifiManager.saveConfiguration();
        Logger.d("NetworkManager: enabling Wi-Fi configuration #%d %s...%s", wifiConfiguration.networkId, wifiConfiguration.SSID, success);

        return success;
    }

    private boolean removeOrDisableWifiConfiguration(@NonNull WifiConfiguration wifiConfiguration) {
        boolean success;

        // Disable the configuration
        if (wifiConfiguration.status != WifiConfiguration.Status.DISABLED) {
            success = mWifiManager.disableNetwork(wifiConfiguration.networkId);
            success = success && mWifiManager.saveConfiguration();
            Logger.d("NetworkManager: disabling Wi-Fi configuration #%d %s...%s", wifiConfiguration.networkId, wifiConfiguration.SSID, success);
        }

        // Remove the configuration
        success = mWifiManager.removeNetwork(wifiConfiguration.networkId);
        success = success && mWifiManager.saveConfiguration();
        Logger.d("NetworkManager: removing Wi-Fi configuration #%d %s...%s", wifiConfiguration.networkId, wifiConfiguration.SSID, success);

        return success;
    }

    public void removeOrDisableConfigBySSID(String SSID) {
        if (SSID == null) return;
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : configuredNetworks) {
            if (configuration.SSID == null) continue;
            if (SSID.equals(configuration.SSID.replace("\"", "").trim())) {
                removeOrDisableWifiConfiguration(configuration);
                break;
            }
        }
    }

    /**
     * Connect to specific access point. Note that old Wi-Fi configuration will be removed,
     * a new Wi-Fi configuration linking to the access point will be created and added as
     * the only one Wi-Fi configuration.
     *
     * @param accessPoint the access point to be connected to
     * @return {@code true} if the operation succeeded
     */
    public boolean connectAccessPoint(AccessPoint accessPoint) {
        // Remove existing network and add new network
        if (mActiveWifiConfiguration != null) {
            removeOrDisableWifiConfiguration(mActiveWifiConfiguration);
            mActiveWifiConfiguration = null;
        }

        // Scan for existing Wi-Fi configuration(s), config and update
        List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();
        final String SSID = "\"" + accessPoint.getSSID() + "\"";
        int networkId = -1;
        // From newest to oldest, search for network with the same SSID
        for (int i = wifiConfigurations.size() - 1; i >= 0; i--) {
            WifiConfiguration wifiConfiguration = wifiConfigurations.get(i);

            if (wifiConfiguration.SSID.equals(SSID)) {
                // Using old Wi-Fi configuration because calls to addNetwork() / updateNetwork() always fail (return -1)
                configWifiConfiguration(wifiConfiguration, accessPoint);

                // try update an old one
                networkId = mWifiManager.updateNetwork(wifiConfiguration);

                // configure success
                if (networkId != -1) {
                    mActiveWifiConfiguration = wifiConfiguration;
                    Logger.d("NetworkManager: update Wi-Fi configuration %d %s, networkId = %d", wifiConfiguration.networkId, wifiConfiguration.SSID, networkId);

                    // Get first one that can be configured and break. This could be choosing
                    // another Wi-Fi with same name wrongly, we just ignore the case.
                    break;
                }
            }
        }

        // No old configuration found, or update on old ones all fails
        if (mActiveWifiConfiguration == null) {
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            configWifiConfiguration(wifiConfiguration, accessPoint);

            networkId = mWifiManager.addNetwork(wifiConfiguration);
            if (networkId != -1) {
                wifiConfiguration.networkId = networkId;
                mActiveWifiConfiguration = wifiConfiguration;
                Logger.d("NetworkManager: add Wi-Fi configuration %s, networkId = %d", wifiConfiguration.SSID, networkId);
            }
        }

        // All fails
        if (mActiveWifiConfiguration == null) {
            return false;
        }

        return enableWifiConfiguration(mActiveWifiConfiguration);
    }

    /**
     * Check current connected network.
     *
     * @return current connected AccessPoint or null
     */
    public AccessPoint getActiveAccessPoint(@NonNull Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return AccessPoint.NULL_ACCESS_POINT;

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (networkInfo.isConnected()) {
                final AccessPoint accessPoint = new AccessPoint();
                accessPoint.setSSID(networkInfo.getExtraInfo().replace("\"", "").trim());
                return accessPoint;
            }
        }
        return AccessPoint.NULL_ACCESS_POINT;
    }

    /**
     * Check if any AccessPoint is being connecting or connected.
     * Note this is different with getSelected() in WifiConnectionViewModel.
     *
     * @return current connecting AccessPoint or null
     */
    public AccessPoint getConnectingOrConnectedAP() {
        final WifiInfo info = mWifiManager.getConnectionInfo();

        if (info.getNetworkId() != -1) {
            final AccessPoint selectedAccessPoint = new AccessPoint();
            selectedAccessPoint.setAccessPoint(info);
            return selectedAccessPoint;
        } else {
            return AccessPoint.NULL_ACCESS_POINT;
        }
    }

    /**
     * Check if AccessPoint is being configured
     *
     * @return return network ID if AccessPoint is configured, else return -1
     */
    private int isConfigured(AccessPoint ap) {
        if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                mContext.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }
        List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
        if (configurations == null || configurations.isEmpty()) {
            return -1;
        }
        for (WifiConfiguration configuration : configurations) {
            if (configuration.SSID.replace("\"", "").trim().equals(ap.getSSID())) {
                return configuration.networkId;
            }
        }
        return -1;
    }

    /**
     * Transform ScanResult list into AccessPoint list
     */
    private List<AccessPoint> deriveAccessPointListFromScanResults(List<ScanResult> resultsList) {
        List<AccessPoint> accessPointList = new ArrayList<>();

        for (ScanResult scanResult : resultsList) {
            // skip empty ssid
            if (TextUtils.isEmpty(scanResult.SSID)) {
                continue;
            }

            AccessPoint accessPoint = new AccessPoint();
            int networkID = isConfigured(accessPoint);

            accessPoint.setSSID(scanResult.SSID);
            accessPoint.setBssid(scanResult.BSSID);
            accessPoint.setEncryptionType(scanResult.capabilities);
            accessPoint.setRssi(scanResult.level);
            accessPoint.setNetworkId(networkID);

            AccessPoint activeAp = getActiveAccessPoint(mContext);
            if (activeAp != null && scanResult.SSID.equals(activeAp.getSSID())) {
                accessPoint.setConnectState(AccessPoint.ConnectState.CONNECTED);
            }

            accessPointList.add(accessPoint);
        }

        return accessPointList;
    }

    /**
     * Merge the network which is related with other. Such as two networks have different ssid
     * but hold the same bssid(basic service set identifiers)
     * <p>
     * https://en.wikipedia.org/wiki/Service_set_(802.11_network)
     */
    private static List<AccessPoint> mergeRelativeAPs(List<AccessPoint> aps) {
        ArrayList<AccessPoint> resultAPs = new ArrayList<>();
        while (aps.size() > 0) {
            for (int i = 0; i < aps.size(); i++) {
                AccessPoint tempAp = aps.get(i);
                ArrayList<AccessPoint> relativeAPs = new ArrayList<>();
                relativeAPs.add(tempAp);
                for (int j = i + 1; j < aps.size(); j++) {
                    AccessPoint tempAp1 = aps.get(j);
                    if (tempAp.getSSID().trim()
                            .equals(tempAp1.getSSID().trim())) {
                        if (!tempAp.getBssid().equals(tempAp1.getBssid())) {
                            relativeAPs.add(tempAp1);
                        }
                    }
                }
                aps.removeAll(relativeAPs);
                if (relativeAPs.size() > 1) {
                    AccessPoint mainAp = relativeAPs.get(0);
                    relativeAPs.remove(0);
                    mainAp.setRelativeAPs(relativeAPs);
                    resultAPs.add(mainAp);
                    break;
                } else {
                    resultAPs.add(tempAp);
                    break;
                }
            }
        }
        return resultAPs;
    }

    public DhcpInfo getDHCPInfo() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        return dhcpInfo;
    }

    public NetworkCapabilities getNetworkCapabilities() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return null;

        return connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
    }


    public Observable<List<AccessPoint>> watchAccessPointList() {
        return mAccessPointListSubject.hide();
    }

    public boolean isAuthenticatingError(Intent intent) {
        if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {
            int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            return supplicantError == WifiManager.ERROR_AUTHENTICATING;
        }
        return false;
    }

    public Observable<Intent> getSupplicantStateObservable() {
        return mSupplicantStateSubject.hide();
    }
    public String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }

}

