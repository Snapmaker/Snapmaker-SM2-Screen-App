package fabscreen.libraries.legacy.viewmodel;

import static fabscreen.libraries.legacy.lib.network.AccessPoint.ConnectState.CONFIRMED;
import static fabscreen.libraries.legacy.lib.network.AccessPoint.ConnectState.CONNECTED;
import static fabscreen.libraries.legacy.lib.network.AccessPoint.ConnectState.CONNECTING;
import static fabscreen.libraries.legacy.lib.network.AccessPoint.ConnectState.SELECTED;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.NetworkController;
import fabscreen.libraries.legacy.lib.network.AccessPoint;
import fabscreen.libraries.legacy.lib.network.AccessPoint.ConnectState;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class WifiConnectionViewModel extends BaseViewModel {

    private NetworkController mNetworkController;

    private String mConnectingSSID;
    private boolean mIsUserSelectedConnecting;
    private Disposable mTimeoutWatcherDisposable;

    private JSONObject mSavedWifiInfo;
    private final File mWifiInfoFile;
    private static final String WIFI_INFO_FILE_NAME = "wifi_info";
    private AccessPoint mUserConfirmedAP;

    // Search state
    public enum SearchState {
        IDLE,
        SEARCHING,
        SEARCH_DONE,
        SEARCH_DONE_EMPTY
    }

    private BehaviorSubject<SearchState> mSearchStateSubject = BehaviorSubject.createDefault(SearchState.IDLE);

    // Password
    public enum PasswordTip {
        TIP_EMPTY,
        TIP_TOO_SHORT,
        TIP_OK
    }

    private PublishSubject<NetworkController.ConnectResult> mConnectResultSubject = PublishSubject.create();

    private AccessPoint mSelectedAccessPoint = null;

    private BehaviorSubject<List<AccessPoint>> mAccessPointsSubject = BehaviorSubject.createDefault(new ArrayList<>());
    private BehaviorSubject<String> mPasswordSubject = BehaviorSubject.createDefault("");
    private BehaviorSubject<PasswordTip> mPasswordTipSubject = BehaviorSubject.createDefault(PasswordTip.TIP_EMPTY);

    private PublishSubject<Boolean> mConnectEventSubject = PublishSubject.create();

    public WifiConnectionViewModel() {
        super();

        mNetworkController = getModel().getNetworkController();

        mNetworkController.watchAccessPointList()
                .skip(1)
                .as(bindToLifecycle())
                .subscribe(accessPoints -> {
                    mAccessPointsSubject.onNext(accessPoints);

                    if (accessPoints.isEmpty()) {
                        mSearchStateSubject.onNext(SearchState.SEARCH_DONE_EMPTY);
                    } else {
                        mSearchStateSubject.onNext(SearchState.SEARCH_DONE);
                    }
                });

        mNetworkController.watchSupplicantStateChange()
                .as(bindToLifecycle())
                .subscribe(this::onSupplicantChange);

        mPasswordSubject
                .map(password -> {
                    if (password.isEmpty()) {
                        return PasswordTip.TIP_EMPTY;
                    }
                    if (password.length() < 8) {
                        return PasswordTip.TIP_TOO_SHORT;
                    }
                    return PasswordTip.TIP_OK;
                })
                .as(bindToLifecycle())
                .subscribe(tip -> mPasswordTipSubject.onNext(tip));


        mWifiInfoFile = new File(getModel().getDataDir(), WIFI_INFO_FILE_NAME);
        String wifiInfoJson = getModel().getNetworkController().readWifiInfoFromFile(mWifiInfoFile);
        mSavedWifiInfo = getWifiInfoFromJson(wifiInfoJson);
    }

    private JSONObject getWifiInfoFromJson(String wifiInfoJson) {
        try {
            mSavedWifiInfo = new JSONObject(wifiInfoJson);
            return mSavedWifiInfo;
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public String getSelectedPassword() {
        if (getSelected() != null) {
            return getPasswordBySsid(getSelected().getSSID());
        }
        return "";
    }

    private String getPasswordBySsid(String ssid) {
        String password = mSavedWifiInfo.optString(ssid);
        return TextUtils.isEmpty(password) ? "" : password;
    }

    public void savePassword(String ssid, String password) {
        // Do nothing if password has not changed.
        if (getPasswordBySsid(ssid).equals(password)) return;
        // Save password to mWifiObject then write to file.
        try {
            mSavedWifiInfo.put(ssid, password);
            getModel().getNetworkController().savePasswordToFile(mSavedWifiInfo.toString(), mWifiInfoFile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * System broadcast received. Detail info is in the intent extra.
     *
     * @param intent Where Wi-Fi connect info( state, supplicant error, etc.) stored in.
     */
    private void onSupplicantChange(Intent intent) {
        if (intent == null) return;
        SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        if (state == null) return;
        switch (state) {
            case ASSOCIATING:
            case ASSOCIATED:
                mConnectingSSID = mNetworkController.getConnectingOrConnectedAP().getSSID();
                ConnectState lastState = mNetworkController.getAPConnectStateBySSID(mConnectingSSID);
                // The state is already CONNECTING, no need to set again. When do a connect, one or
                // both of ASSOCIATING and ASSOCIATE will broadcast, we only need one of them.
                if (lastState == ConnectState.CONNECTING) break;

                // If state is [USER_CONFIRMED_CONNECT] before connecting, we can mark this connect as userSelected.
                mIsUserSelectedConnecting = lastState == ConnectState.CONFIRMED;
                mNetworkController.setAPConnectStateAndRefresh(mConnectingSSID, ConnectState.CONNECTING);
                break;

            case DISCONNECTED:
                // We should distinguish user-selected and system-selected APs.
                // If this connect is userSelected, we should show toast, otherwise only state
                // will be set, no toast will be shown.
                // A connect has its lifecycle: IDLE -> SELECTED -> CONNECTING -> CONNECTED/IDLE
                // Once a connect arrives at the end of its lifecycle([DISCONNECTED/COMPLETE(IDLE/CONNECTED)]),
                // this connect is no longer userSelected, unless select again.
                ConnectState apLastState = mNetworkController.getAPConnectStateBySSID(mConnectingSSID);
                if (apLastState != ConnectState.CONNECTING) return;
                boolean isAuthenticatingError = mNetworkController.isAuthenticatingError(intent);
                if (mIsUserSelectedConnecting) {
                    // We only care the last CONFIRMED AP.
                    if (mUserConfirmedAP == null) return;
                    if (mConnectingSSID != null && mConnectingSSID.equals(mUserConfirmedAP.getSSID())) {
                        if (isAuthenticatingError) {
                            mNetworkController.removeOrDisableConfigBySSID(mConnectingSSID);
                            mConnectResultSubject.onNext(NetworkController.ConnectResult.FAIL_WRONG_PASSWORD);
                        } else {
                            mConnectResultSubject.onNext(NetworkController.ConnectResult.FAIL_OTHER);
                        }
                    }
                    // disposeTimeoutWatcher();
                } else {
                    if (isAuthenticatingError) {
                        mNetworkController.removeOrDisableConfigBySSID(mConnectingSSID);
                    }
                }
                // Refresh list AFTER notify result.
                mNetworkController.setAPConnectStateAndRefresh(mConnectingSSID, ConnectState.IDLE);
                mConnectingSSID = null;
                mIsUserSelectedConnecting = false;
                break;

            case COMPLETED:
                String connectedSSID = mNetworkController.getConnectingOrConnectedAP().getSSID();
                mNetworkController.setAPConnectStateAndRefresh(connectedSSID, ConnectState.CONNECTED);
                if (mIsUserSelectedConnecting) {
                    mConnectResultSubject.onNext(NetworkController.ConnectResult.SUCCESS);
                    // disposeTimeoutWatcher();
                }
                mConnectingSSID = null;
                mIsUserSelectedConnecting = false;
                break;
        }
    }

    private void disposeTimeoutWatcher() {
        if (mTimeoutWatcherDisposable != null && !mTimeoutWatcherDisposable.isDisposed()) {
            mTimeoutWatcherDisposable.dispose();
        }
    }

    /**
     * Get network (Wi-Fi) enable state.
     */
    public boolean isWifiEnabled() {
        return getModel().getNetworkController().isWifiEnabled();
    }

    /**
     * Enable Wi-Fi.
     * <p>
     * Applications must have the {@link android.Manifest.permission#CHANGE_WIFI_STATE}
     * permission to toggle wifi.
     *
     * @return a Single indicates whether the operation is successful.
     */
    public Single<Boolean> enableWiFi() {
        return getModel().getNetworkController().setWifiEnabled(true);
    }

    /**
     * Disable Wi-Fi.
     * <p>
     * Applications must have the {@link android.Manifest.permission#CHANGE_WIFI_STATE}
     * permission to toggle wifi.
     *
     * @return a Single indicates whether the operation is successful.
     */
    public Single<Boolean> disableWiFi() {
        return getModel().getNetworkController().setWifiEnabled(false);
    }

    /**
     * Start scanning network.
     */
    public void startScanNetwork() {
        mSearchStateSubject.onNext(SearchState.SEARCHING);
        getModel().getNetworkController().startScan();
    }

    /**
     * Stop scanning network.
     */
    public void stopScanNetwork() {
        mSearchStateSubject.onNext(SearchState.IDLE);
        getModel().getNetworkController().stopScan();
    }

    /**
     * Get all access points.
     */
    public Observable<List<AccessPoint>> getAccessPointsObservable() {
        return mAccessPointsSubject.hide();
    }

    /**
     * Get all available access points.
     * <p>
     * If access point is selected, then we consider it being not available for selecting.
     */
    public Observable<List<AccessPoint>> getAvailableAccessPointsObservable() {
        return Observable.combineLatest(
                getAccessPointsObservable(),
                getNotIdleAccessPointObservable(),
                (accessPoints, accessPoint) -> {
                    List<AccessPoint> newList = new ArrayList<>();

                    for (AccessPoint accessPoint1 : accessPoints) {
                        if (!accessPoint1.getSSID().equals(accessPoint.getSSID())) {
                            newList.add(accessPoint1);
                        }
                    }

                    return newList;
                });
    }

    public Observable<SearchState> getSearchStateObservable() {
        return mSearchStateSubject.hide();
    }

    public Observable<PasswordTip> getPasswordTipObservable() {
        return mPasswordTipSubject.hide();
    }

    /**
     * Get selected access point.
     *
     * @return {AccessPoint}
     */
    public AccessPoint getSelected() {
        return mSelectedAccessPoint;
    }

    /**
     * Triggered by a user click.
     */
    public void setSelected(AccessPoint accessPoint) {
        // Back from password input page, AP'll be null.
        String SSID = null;
        mSelectedAccessPoint = accessPoint;
        if (mSelectedAccessPoint != null) {
            SSID = mSelectedAccessPoint.getSSID();
        }
        mNetworkController.setAPConnectStateAndRefresh(SSID, ConnectState.SELECTED);
    }

    public void setPassword(String password) {
        mPasswordSubject.onNext(password);
    }

    public Observable<Boolean> getConnectEventObservable() {
        return mConnectEventSubject.hide();
    }

    /**
     * Use this function to notify another fragment to connect.
     */
    public void notifyConnect() {
        mConnectEventSubject.onNext(true);
    }

    /**
     * Connect to selected access point.
     *
     * @return {@code true} if the operation succeeded.
     */
    public boolean connect() {
        // "connect" clicked, selectedAP is being enabled and wait for CONNECTING/CONNECTED state.
        mNetworkController.setAPConnectStateAndRefresh(mSelectedAccessPoint.getSSID(), ConnectState.CONFIRMED);
        mUserConfirmedAP = mSelectedAccessPoint;
        watchForConnectTimeout(mUserConfirmedAP);
        if (mUserConfirmedAP.isEncrypted()) {
            String password = mPasswordSubject.getValue();
            mUserConfirmedAP.setPassword(password);

            // save btw
            savePassword(mUserConfirmedAP.getSSID(), password);
//            getModel().getNetworkController().save(mSelectedAccessPoint.getSSID(), password);
        }

        return getModel().getNetworkController().connect(mUserConfirmedAP);
    }

    /**
     * If an AP stay in SELECTED/CONNECTING state for more than 20s, the connect is timeout.
     * If connect() is triggered again, the watcher is setup again.
     * If system reconnect the AP, and in the 20th second it's still connecting, the connect will
     * be ignored.
     *
     * @param accessPoint for whom the connect will be watched.
     */
    private void watchForConnectTimeout(AccessPoint accessPoint) {
        disposeTimeoutWatcher();
        mTimeoutWatcherDisposable = Observable.timer(20, TimeUnit.SECONDS)
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    if (accessPoint == null) return;
                    ConnectState status = accessPoint.getConnectState();
                    if (status != ConnectState.CONNECTED && status != ConnectState.IDLE) {
                        mNetworkController.setAPConnectStateAndRefresh(accessPoint.getSSID(), ConnectState.IDLE);
                        mConnectResultSubject.onNext(NetworkController.ConnectResult.FAIL_TIMEOUT);
                    }
                }, e -> {
                });
    }

    /**
     * Check if selected access point is connected.
     *
     * @return {@code true} if connected.
     */
    public boolean isConnected() {
        final AccessPoint active = getModel().getNetworkController().getActiveAccessPointImmediately();
        final AccessPoint selected = getSelected();

        return (active != AccessPoint.NULL_ACCESS_POINT
                && selected != AccessPoint.NULL_ACCESS_POINT
                && active.getSSID().equals(selected.getSSID()));
    }

    public Observable<AccessPoint> getNotIdleAccessPointObservable() {
        return mAccessPointsSubject.flatMap(accessPoints -> {
            AccessPoint selectedAP = null;
            AccessPoint userConfirmedAP = null;
            AccessPoint connectingAP = null;
            AccessPoint connectedAP = null;

            for (AccessPoint accessPoint : accessPoints) {
                ConnectState status = accessPoint.getConnectState();
                if (status == SELECTED) selectedAP = accessPoint;
                if (status == CONFIRMED) userConfirmedAP = accessPoint;
                if (status == CONNECTING) connectingAP = accessPoint;
                if (status == CONNECTED) connectedAP = accessPoint;
            }
            // visibility priority: SELECTED >USER_CONFIRMED_CONNECT > CONNECTING > CONNECTED.
            // DO NOT CHANGE THE ORDER OF THE BELOW CODES!
            if (selectedAP != null) return Observable.just(selectedAP);
            if (userConfirmedAP != null) return Observable.just(userConfirmedAP);
            if (connectingAP != null) return Observable.just(connectingAP);
            if (connectedAP != null) return Observable.just(connectedAP);
            // No SELECTED/CONNECTING/CONNECTED AP was found.
            return Observable.just(AccessPoint.NULL_ACCESS_POINT);
        });
    }

    public String getConnectedAccessPointExtraInfo(Context context) {
        DhcpInfo dhcpInfo = mNetworkController.getDHCPInfo();
        NetworkCapabilities cm = mNetworkController.getNetworkCapabilities();
        if (cm == null) {
            return "N/A";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(R.string.settings_wifi_connected_ap_info_ip_address)).append(intToIp(dhcpInfo.ipAddress));
        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_netmask)).append(intToIp(dhcpInfo.netmask));
        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_gateway)).append(intToIp(dhcpInfo.gateway));
        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_dns1)).append(intToIp(dhcpInfo.dns1));
        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_dns2)).append(intToIp(dhcpInfo.dns2));
        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_bandwidth_format,
                cm.getLinkDownstreamBandwidthKbps() / 1000,
                cm.getLinkUpstreamBandwidthKbps() / 1000));

        builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_internet_access))
                .append(cm.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));

        List<AccessPoint> accessPoints = mAccessPointsSubject.getValue();
        if (accessPoints != null) {
            for (AccessPoint accessPoint : accessPoints) {
                ConnectState status = accessPoint.getConnectState();
                if (status == CONNECTED) {
                    builder.append("\n").append(context.getString(R.string.settings_wifi_connected_ap_info_signal_strength))
                            .append(accessPoint.getRssi()).append(" dBm");
                }
            }
        }
        Logger.d("AP Info:\n" + builder.toString());
        return builder.toString();
    }


    private static String intToIp(int ipAddress) {
        return (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
    }

    public Observable<NetworkController.ConnectResult> getConnectResultObservable() {
        return mConnectResultSubject.hide();
    }
}