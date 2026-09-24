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
public class SettingsAdvancedLaser20WFragment extends BaseFragment {

    @BindView(R.id.ll_settings_laser_20w_fire_sensor_sensitivity)
    View mViewFireSensorSensitivity;
    @BindView(R.id.ll_settings_laser_20w_cross_line_indicator)
    View mViewCrossLineIndicator;

    public static Fragment getInstance() {
        return new SettingsAdvancedLaser20WFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_advance_laser_20w);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_advanced_laser_20w;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {

    }

    @OnClick(R.id.ll_settings_laser_20w_shot_output_power)
    void onClickLaserShotPower() {
        ((SettingsActivity) requireActivity()).gotoLaserShotOutputPower();
    }

    @OnClick(R.id.ll_settings_laser_20w_fire_sensor_sensitivity)
    void onClickFireSensorSensitivity() {
        ((SettingsActivity) requireActivity()).startFireSensorSensitivityFragment();
    }

    @OnClick(R.id.ll_settings_laser_20w_cross_line_indicator)
    void onClickCrossLineIndicator() {
        ((SettingsActivity) requireActivity()).startCrossLineIndicatorOffsetFragment();
    }
}
