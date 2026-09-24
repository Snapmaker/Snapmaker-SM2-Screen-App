package com.snapmaker.fabscreen.modules.about;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.BuildConfig;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.welcome.name.WelcomeNameViewModel;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsAboutFragment extends BaseFragment {
    public static SettingsAboutFragment getInstance() {
        return new SettingsAboutFragment();
    }

    @BindView(R.id.iv_settings_about_cover)
    ImageView mIvCover;

    @BindView(R.id.tv_settings_about_product_name)
    TextView mTvProduct;

    @BindView(R.id.tv_about_product_name)
    TextView mTvProductName;
    @BindView(R.id.tv_about_product_size)
    TextView mTvProductSize;
    @BindView(R.id.tv_about_update_package_version)
    TextView mTvUpdatePackageVersion;
    @BindView(R.id.tv_about_app_version)
    TextView mTvApplicationVersion;
    @BindView(R.id.tv_about_controller_version)
    TextView mTvControllerVersion;
    @BindView(R.id.tv_about_ip_address)
    TextView mTvIPAddress;

    private WelcomeNameViewModel mViewModel;
    private int mCoverClicks = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_about_machine);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_about;
    }

    @Override
    protected WelcomeNameViewModel getViewModel() {
        return getViewModelProvider().get(WelcomeNameViewModel.class);
    }

    private void initView() {
        String machineName = getModel().getPreferences().getMachineName();
        mTvProduct.setText(machineName);

        mViewModel.getNameObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(name -> {
                    mTvProduct.setText(name);
                });

        int machineModel = getModel().getMachineController().getMachineModel();
        int x = getModel().getMachineController().getSizeX();
        int y = getModel().getMachineController().getSizeY();
        int z = getModel().getMachineController().getSizeZ();

        switch (machineModel) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150: {
                mIvCover.setImageResource(R.drawable.pic_machine_model_a150_320x160);
                mTvProductName.setText(R.string.settings_snapmaker2_a150);
                break;
            }
            case Constants.MACHINE_MODEL_SNAPMAKER_A250: {
                mIvCover.setImageResource(R.drawable.pic_machine_model_a250_320x160);
                mTvProductName.setText(R.string.settings_snapmaker2_a250);
                break;
            }
            case Constants.MACHINE_MODEL_SNAPMAKER_A350: {
                mIvCover.setImageResource(R.drawable.pic_machine_model_a350_320x160);
                mTvProductName.setText(R.string.settings_snapmaker2_a350);
                break;
            }
            default:
                break;
        }
        mTvProductSize.setText(String.format("%s × %s × %s mm", x, y, z));

        // Package version
        String packageVersion = getModel().getPreferences().getLastUpdatePackageVersion();
        Logger.d("Package version %s", packageVersion);
        mTvUpdatePackageVersion.setText(packageVersion);

        // Controller version
        getModel().getSlaveComputer().getControllerVersion()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(version -> {
                    Logger.d("Controller version %s", version);
                    mTvControllerVersion.setText(version);
                }, LogHelper::log);

        if (getContext() != null) {
            mTvApplicationVersion.setText(BuildConfig.VERSION_NAME);
            Logger.d("Touch screen version %s", BuildConfig.VERSION_NAME);
        }

        // check ip address
        String addressString = "";
        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaceList) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (isIPv4) {
                            addressString = sAddr;
                            mTvIPAddress.setText(addressString);
                        }
                    }
                }

                if (addressString.isEmpty()) {
                    mTvIPAddress.setText(R.string.settings_network_not_connected);
                }
            }
        } catch (SocketException e) {
            LogHelper.log(e);
        }
    }

    @OnClick(R.id.btn_settings_about_change_name)
    void onClickChangeName() {
        AboutActivity activity = (AboutActivity) getActivity();
        if (activity != null) {
            activity.gotoChangeName();
        }
    }

    @OnClick(R.id.iv_settings_about_cover)
    void onClickCover() {
        mCoverClicks++;

        if (mCoverClicks % 5 == 0) {
            FabConfirm.create(getContext())
                    .setDescription(R.string.experiment_developer_mode_notice)
                    .setConfirm(R.string.all_ok, (dialog, which) -> {
                        Logger.i("Enter developer mode.");
                        getModel().getPreferences().setDebugFlag(true);
                        dialog.dismiss();
                    })
                    .show();
        } else {
            Logger.i("Exit developer mode.");
            getModel().getPreferences().setDebugFlag(false);
        }
    }
}
