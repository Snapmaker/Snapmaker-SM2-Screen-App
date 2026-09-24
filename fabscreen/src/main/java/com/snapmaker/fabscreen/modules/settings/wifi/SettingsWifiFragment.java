package com.snapmaker.fabscreen.modules.settings.wifi;

import static fabscreen.libraries.legacy.data.NetworkController.ConnectResult.FAIL_WRONG_PASSWORD;
import static fabscreen.libraries.legacy.data.NetworkController.ConnectResult.SUCCESS;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.NetworkController;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import fabscreen.libraries.legacy.viewmodel.WifiConnectionViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SettingsWifiFragment extends BaseFragment {

    private AlertDialog mConnectResultDialog;

    public static SettingsWifiFragment newInstance() {
        return new SettingsWifiFragment();
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @BindView(R.id.btn_settings_wifi_switch)
    Button mBtnWifiSwitch;

    @BindView(R.id.lv_settings_wifi_access_point_list)
    ListView mLvAccessPointListView;

    @BindView(R.id.view_settings_wifi_searching)
    View mViewSearching;
    @BindView(R.id.view_settings_wifi_no_found)
    View mViewNoFound;

    // ListView shows APs connected
    @BindView(R.id.lv_settings_wifi_connected_access_point_list)
    ListView mLvConnectedAccessPointListView;

    private WifiConnectionViewModel mViewModel;
    private SettingsWifiListConnectedAccessPointAdapter mConnectedAccessPointAdapter;
    private SettingsWifiListAccessPointListAdapter mAccessPointListAdapter;

    private BehaviorSubject<Boolean> mWifiEnabledSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(getResources().getString(R.string.all_wifi));

        initView();
    }

    @Override
    public void onStop() {
        super.onStop();

        // stop scanning once activity stopped
        mViewModel.stopScanNetwork();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_wifi;
    }

    @Override
    protected WifiConnectionViewModel getViewModel() {
        return getViewModelProvider().get(WifiConnectionViewModel.class);
    }

    private void initView() {
        // Init access point list
        mAccessPointListAdapter = new SettingsWifiListAccessPointListAdapter();
        mAccessPointListAdapter.setOnItemClickListener(accessPoint -> {
            if (accessPoint.isEncrypted()) {
                mViewModel.setSelected(accessPoint);
                if (getActivity() != null) {
                    ((SettingsActivity) getActivity()).startWifiPasswordFragment();
                }
            } else {
                mViewModel.setSelected(accessPoint);
                connect();
            }
        });

        mLvAccessPointListView.setAdapter(mAccessPointListAdapter);
        mLvAccessPointListView.setOnItemClickListener(mAccessPointListAdapter);

        // setup connected access point list
        mConnectedAccessPointAdapter = new SettingsWifiListConnectedAccessPointAdapter(getContext());
        mLvConnectedAccessPointListView.setAdapter(mConnectedAccessPointAdapter);

        mConnectedAccessPointAdapter.setOnClickInfoListener((accessPoint) -> {
            FabFullScreenDialog.create(requireContext())
                    .setIcon(R.drawable.pic_warning_remote_connect_120x120)
                    .setTitle(accessPoint.getSSID())
                    .setMessage(mViewModel.getConnectedAccessPointExtraInfo(requireContext()))
                    .setPositive(R.string.all_confirm, (dialog, which) -> dialog.dismiss()).show();

        });

        // state -> view (list, searching, no found)
        mViewModel.getSearchStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(searchState -> {
                    switch (searchState) {
                        case IDLE:
                            mLvAccessPointListView.setVisibility(View.GONE);
                            mViewSearching.setVisibility(View.GONE);
                            mViewNoFound.setVisibility(View.GONE);
                            mLvConnectedAccessPointListView.setVisibility(View.GONE);
                            break;
                        case SEARCHING:
                            mLvAccessPointListView.setVisibility(View.GONE);
                            mViewSearching.setVisibility(View.VISIBLE);
                            mViewNoFound.setVisibility(View.GONE);
                            mLvConnectedAccessPointListView.setVisibility(View.GONE);
                            break;
                        case SEARCH_DONE:
                            mLvAccessPointListView.setVisibility(View.VISIBLE);
                            mViewSearching.setVisibility(View.GONE);
                            mViewNoFound.setVisibility(View.GONE);
                            mLvConnectedAccessPointListView.setVisibility(View.VISIBLE);
                            break;
                        case SEARCH_DONE_EMPTY:
                            mLvAccessPointListView.setVisibility(View.GONE);
                            mViewSearching.setVisibility(View.GONE);
                            mViewNoFound.setVisibility(View.VISIBLE);
                            mLvConnectedAccessPointListView.setVisibility(View.GONE);
                            break;
                    }
                });

        mViewModel.getNotIdleAccessPointObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoint -> {
                    AndroidSchedulers.mainThread().scheduleDirect(() -> {
                        mConnectedAccessPointAdapter.notifyNetworkStateChange(accessPoint);
                    }, 100, TimeUnit.MILLISECONDS);
                });

        // listen on access points changes
        mViewModel.getAvailableAccessPointsObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoints -> {
                    AndroidSchedulers.mainThread().scheduleDirect(() -> {
                        mAccessPointListAdapter.setAccessPoints(accessPoints);
                        mAccessPointListAdapter.notifyDataSetChanged();
                    }, 100, TimeUnit.MILLISECONDS);
                });

        // bind connect event
        mViewModel.getConnectEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(connectEvent -> connect());

        mViewModel.getConnectResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::showToast);

        // Init ON/OFF switch
        initSwitch();
    }

    private void showToast(NetworkController.ConnectResult result) {
        if (!this.isVisible()) return;
        if (result == SUCCESS) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        mConnectResultDialog = builder.create();
        mConnectResultDialog.setCanceledOnTouchOutside(false);
        if (mConnectResultDialog.getWindow() != null) {
            mConnectResultDialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            mConnectResultDialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_welcome_wifi_connect_failed, null);
        TextView tvFailMsg = view.findViewById(R.id.tv_fail_msg);
        tvFailMsg.setText(result == FAIL_WRONG_PASSWORD ? R.string.all_wifi_dialog_connect_failed_wrong_password : R.string.all_wifi_dialog_connect_failed);
        mConnectResultDialog.setView(view);
        mConnectResultDialog.show();

        AndroidSchedulers.mainThread().scheduleDirect(mConnectResultDialog::dismiss, 3000, TimeUnit.MILLISECONDS);
    }

    private void initSwitch() {
        mWifiEnabledSubject
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(enabled -> {
                    mBtnWifiSwitch.setActivated(enabled);

                    if (enabled) {
                        return mViewModel.enableWiFi().toObservable();
                    } else {
                        return mViewModel.disableWiFi().toObservable();
                    }
                })
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        if (mWifiEnabledSubject.getValue()) {
                            startScanNetwork();
                        } else {
                            stopScanNetwork();
                        }
                    }
                });

        // set initial value
        mWifiEnabledSubject.onNext(mViewModel.isWifiEnabled());
    }

    /**
     * Check Wi-Fi permission and then start scan network.
     */
    private void startScanNetwork() {
        if (getContext() == null) {
            return;
        }

        // Check coarse location permission
        if (getContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If COARSE Location permission is granted, just start scan network.
            mViewModel.startScanNetwork();
        } else {
            // Otherwise request permission first, and then start scanning when permission is granted.
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Stop scanning network.
     */
    private void stopScanNetwork() {
        mViewModel.stopScanNetwork();
    }

    /**
     * Start connecting to selected access point.
     */
    private void connect() {
        if (mViewModel.getSelected() != null) {
            mViewModel.connect();
        }
    }

    @OnClick(R.id.btn_settings_wifi_switch)
    void onClickWifiSwitch() {
        mWifiEnabledSubject.onNext(!mWifiEnabledSubject.getValue());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mViewModel.startScanNetwork();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden && mConnectResultDialog != null && mConnectResultDialog.isShowing()) {
            mConnectResultDialog.dismiss();
        }
    }
}
