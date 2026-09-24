package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;
import com.snapmaker.fabscreen.router.Router;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.FabAlert;

@SuppressLint("NonConstantResourceId")
public class SettingsAdvancedLaser10WFragment extends BaseFragment {

    @BindView(R.id.ll_settings_laser_camera_calibration)
    View mCameraCalibration;
    @BindView(R.id.ll_settings_laser_thickness_measure_calibration)
    View mThicknessMeasureCalibration;
    @BindView(R.id.ll_settings_laser_toolhead_focus_calibration)
    View mToolHeadFocusCalibration;
    @BindView(R.id.ll_settings_laser_shot_output_power)
    View mLaserShotOutputPower;
    @BindView(R.id.btn_settings_advanced_light)
    Button mBtnLight;

    private SettingsActivity mActivity;

    public static Fragment getInstance() {
        return new SettingsAdvancedLaser10WFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_advance_laser_10w);
        mActivity = (SettingsActivity) requireActivity();

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_advanced_laser_10w;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        boolean isRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();
        mCameraCalibration.setVisibility(isRotaryAvailable ? View.GONE : View.VISIBLE);
        mThicknessMeasureCalibration.setVisibility(isRotaryAvailable ? View.GONE : View.VISIBLE);
        mToolHeadFocusCalibration.setVisibility(isRotaryAvailable ? View.GONE : View.VISIBLE);
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        mBtnLight.setActivated(cameraLightOn);
    }


    @OnClick(R.id.btn_settings_advanced_light)
    void onCameraLightClicked(View view) {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        cameraLightOn = !cameraLightOn;

        Logger.i("Setting camera light mode " + cameraLightOn);

        getModel().getPreferences().setLaserCameraLightOn(cameraLightOn);
        mBtnLight.setActivated(cameraLightOn);
    }

    @OnClick(R.id.ll_settings_laser_camera_calibration)
    void onCameraCalibrationClicked() {
        calibrateCamera();
    }

    @OnClick(R.id.ll_settings_laser_thickness_measure_calibration)
    void onThicknessMeasureCalibration() {
        Router.getInstance().routeTo10wThicknessCalibrationPage().start(requireContext());
    }

    @OnClick(R.id.ll_settings_laser_toolhead_focus_calibration)
    void onToolheadFocusCalibrationClicked() {
        mActivity.gotoLaser10wToolheadFocusCalibration();
    }

    @OnClick(R.id.ll_settings_laser_shot_output_power)
    void onClickLaserShotPower() {
        mActivity.gotoLaserShotOutputPower();
    }

    private void calibrateCamera() {
        // open camera calibration
        if (getModel().getLaserCameraController().isConnected()) {
            SettingsActivity activity = (SettingsActivity) getContext();
            if (activity != null) {
                activity.goto10wCameraCalibrationIntroFragment();
            }
        } else {
            FabAlert.alert(getContext(), R.string.laser_camera_alert_camera_not_connected);
        }
    }
}
