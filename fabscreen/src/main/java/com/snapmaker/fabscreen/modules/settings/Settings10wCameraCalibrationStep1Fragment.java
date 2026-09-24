package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Settings10wCameraCalibrationStep1Fragment extends BaseFragment {
    public static Settings10wCameraCalibrationStep1Fragment newInstance() {
        return new Settings10wCameraCalibrationStep1Fragment();
    }

    @BindView(R.id.tv_settings_camera_calibration_content)
    TextView mTvDesc;
    @BindView(R.id.btn_camera_calibration_next)
    Button mBtnNext;
    @BindView(R.id.iv_settings_camera_calibration)
    ImageView mIvCover;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_10w_laser_camera_calibration_with_goggles_240x160);
        mTvDesc.setText(R.string.laser_10w_camera_calibnration_step1_desc);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_camera_calibration_step1;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void ensureHomed() {
        getModel().getMachineController().updateCoordinateSystem(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        next();
                    } else {
                        getModel().getSlaveComputer().sendGcode("G28")
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> checkHome());
                    }
                }, LogHelper::log);
    }

    private void checkHome() {
        getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        next();
                    } else {
                        AndroidSchedulers.mainThread().scheduleDirect(this::checkHome, 2000, Constants.TIME_UNIT);
                    }
                }, LogHelper::log);
    }

    private void next() {
        mBtnNext.setEnabled(true);
        if (getActivity() == null) return;
        ((SettingsActivity) getActivity()).goto10wCameraCalibrationStep2();
    }

    @OnClick(R.id.btn_camera_calibration_next)
    void onClickStart() {
        getModel().getPrintController().getHeaderSecurityStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(headerSecurity -> {
                    // When status is 0, the execution header is normal
                    if (headerSecurity.status == 0) {
                        mBtnNext.setEnabled(false);
                        ensureHomed();
                    }
                }, LogHelper::log);

    }
}
