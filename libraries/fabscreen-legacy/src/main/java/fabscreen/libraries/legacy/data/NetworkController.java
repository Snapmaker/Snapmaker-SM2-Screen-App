package fabscreen.libraries.legacy.data;

import static fabscreen.libraries.legacy.lib.network.AccessPoint.*;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.network.AccessPoint;
import fabscreen.libraries.legacy.lib.network.NetworkManager;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class NetworkController {

    private Disposable mTimeoutWatcherDisposable;

    public enum ConnectResult {
        SUCCESS,
        FAIL_WRONG_PASSWORD,
        FAIL_TIMEOUT,
        FAIL_OTHER
    }

    private CompositeDisposable disposables = new CompositeDisposable();

    private Context mContext;

    private NetworkManager mNetworkManager;

    private BehaviorSubject<AccessPoint> mActiveAccessPointSubject = BehaviorSubject.createDefault(NULL_ACCESS_POINT);
    private List<AccessPoint> mAllAPs = new ArrayList<>();
    private BehaviorSubject<List<AccessPoint>> mAllAPsSubject = BehaviorSubject.createDefault(new ArrayList<>());

    NetworkController(Context context) {
        mContext = context.getApplicationContext();

        mNetworkManager = new NetworkManager(context);

        // Check network periodically
        Disposable intervalSubscription = Observable.interval(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> checkNetworkStatus(), LogHelper::log);
        disposables.add(intervalSubscription);

        // Watch, store and notify AP list changes.
        Disposable APListSubscription = mNetworkManager.watchAccessPointList()
                .subscribe(APList -> {
                    List<AccessPoint> newAccessPoints = new ArrayList<>();
                    for (AccessPoint newAP : APList) {
                        if (newAP.getSSID().isEmpty()) {
                            continue;
                        }
                        for (AccessPoint oldAP : mAllAPs) {
                            if (newAP.getSSID().equals(oldAP.getSSID())) {
                                newAP.setConnectState(oldAP.getConnectState());
                            }
                        }
                        newAccessPoints.add(newAP);
                    }
                    mAllAPs.clear();
                    mAllAPs.addAll(newAccessPoints);
                    mAllAPsSubject.onNext(mAllAPs);
                });
        disposables.add(APListSubscription);
    }

    public Observable<Intent> watchSupplicantStateChange() {
        return mNetworkManager.getSupplicantStateObservable();
    }

    public void dispose() {
        disposables.dispose();
    }

    /**
     * Get network (Wi-Fi) enable state.
     */
    public boolean isWifiEnabled() {
        return mNetworkManager.isWifiEnabled();
    }

    /**
     * Enable or disable Wi-Fi.
     * <p>
     * Applications must have the {@link android.Manifest.permission#CHANGE_WIFI_STATE}
     * permission to toggle wifi.
     *
     * @return a Single indicates whether the operation is successful.
     */
    public Single<Boolean> setWifiEnabled(boolean enabled) {
        if (!enabled) {
            // Manually set wifi status to IDLE when turn off wifi, otherwise wifi state will be
            // wrongly restored.
            // Since "DISCONNECTED" won't be triggered when wifi turned off.
            setAllAPsIdle();
        }
        return mNetworkManager.setWifiEnabled(enabled);
    }

    public AccessPoint getActiveAccessPointImmediately() {
        return mNetworkManager.getActiveAccessPoint(mContext);
    }

    /**
     * Only use for home wifi status display.
     */
    public Observable<AccessPoint> getActiveNetworkObservable() {
        return mActiveAccessPointSubject.distinctUntilChanged();
    }

    public AccessPoint getConnectingOrConnectedAP() {
        return mNetworkManager.getConnectingOrConnectedAP();
    }

    private boolean same(AccessPoint a, AccessPoint b) {
        return (a == NULL_ACCESS_POINT && b == NULL_ACCESS_POINT)
                || (a != NULL_ACCESS_POINT && b != NULL_ACCESS_POINT && a.getSSID().equals(b.getSSID()));
    }

    private void checkNetworkStatus() {
        if (!this.isWifiEnabled()) {
            // Home wifi status need to be refreshed when wifi disabled.
            mActiveAccessPointSubject.onNext(NULL_ACCESS_POINT);
            return;
        }

        final AccessPoint activeAccessPoint = mNetworkManager.getActiveAccessPoint(mContext);
        if (!same(activeAccessPoint, mActiveAccessPointSubject.getValue())) {
            mActiveAccessPointSubject.onNext(activeAccessPoint);
        }
    }

    public Observable<List<AccessPoint>> watchAccessPointList() {
        return mAllAPsSubject.hide();
    }

    /**
     * Try connect to specified access point.
     *
     * @return {@code true} if the operation succeeded.
     */
    public boolean connect(AccessPoint accessPoint) {
        return mNetworkManager.connectAccessPoint(accessPoint);
    }

    public void startScan() {
        mNetworkManager.startScan(mContext);
    }

    public void startScanSilently() {
        mNetworkManager.startScanSilently(mContext);
    }

    public void stopScan() {
        mNetworkManager.stopScan(mContext);
    }

    /**
     * The target AP(with the specific SSID)'s connect state will be set to the given state.
     * By default, if SSID is null, APs with given state will be set to IDLE, but when forceOthersIdle
     * is true, all APs will be set to IDLE.
     *
     * @param SSID  The target AP's SSID.
     * @param state The given state.
     */
    public void setAPConnectStateAndRefresh(String SSID, ConnectState state) {
        for (int i = 0; i < mAllAPs.size(); i++) {
            AccessPoint tempAP = mAllAPs.get(i);
            if (SSID == null) {
                if (tempAP.getConnectState() != ConnectState.IDLE) {
                    if (tempAP.getConnectState() == state) {
                        tempAP.setConnectState(ConnectState.IDLE);
                    }
                }
            } else {
                if (SSID.equals(tempAP.getSSID())) {
                    tempAP.setConnectState(state);
                } else {
                    // An AP is confirmed to connect, all other APs should go IDLE.
                    if (tempAP.getConnectState() == state || state == ConnectState.CONFIRMED) {
                        tempAP.setConnectState(ConnectState.IDLE);
                    }
                }
            }
        }
        mAllAPsSubject.onNext(mAllAPs);
    }

    private void setAllAPsIdle() {
        for (AccessPoint ap : mAllAPs) {
            ap.setConnectState(ConnectState.IDLE);
        }
        mAllAPsSubject.onNext(mAllAPs);
    }

    public ConnectState getAPConnectStateBySSID(String SSID) {
        if (SSID == null) return null;
        for (int i = 0; i < mAllAPs.size(); i++) {
            AccessPoint tempAP = mAllAPs.get(i);
            if (SSID.equals(tempAP.getSSID())) {
                return tempAP.getConnectState();
            }
        }
        return null;
    }

    public boolean isAuthenticatingError(Intent intent) {
        return mNetworkManager.isAuthenticatingError(intent);
    }


    public void removeOrDisableConfigBySSID(String SSID) {
        mNetworkManager.removeOrDisableConfigBySSID(SSID);
    }

    public DhcpInfo getDHCPInfo() {
        return mNetworkManager.getDHCPInfo();
    }

    public NetworkCapabilities getNetworkCapabilities() {
        return mNetworkManager.getNetworkCapabilities();
    }


    /**
     * Save key-value pairs in a json file.
     */
    public void savePasswordToFile(String jsonInfo, File file) {
        try {
            FileUtils.writeByteArrayToFile(file, jsonInfo.getBytes());
        } catch (IOException e) {
            LogHelper.log(e);
        }
    }

    /**
     * Get password from local file by ssid.
     *
     * @return Json string stores ssid-password pairs.
     */
    public String readWifiInfoFromFile(File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogHelper.log(e);
            return "";
        }
    }

    public String getMacAddress() {
        return mNetworkManager.getMacAddress();
    }
}
