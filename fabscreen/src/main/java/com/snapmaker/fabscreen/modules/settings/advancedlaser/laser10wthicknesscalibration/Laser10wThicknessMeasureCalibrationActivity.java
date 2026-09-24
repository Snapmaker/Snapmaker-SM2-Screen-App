package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Laser10wThicknessMeasureCalibrationActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        gotoLaser10wThicknessMeasureCalibrationIntroFragment();
    }

    public void gotoLaser10wThicknessMeasureCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, SettingsLaser10wThicknessMeasureCalibrationIntroFragment.getInstance());
    }

    public void gotoLaser10wThicknessMeasureCalibrationPointsFragment() {
        addFragment(
                SettingsLaser10wThicknessMeasureCalibrationPointsFragment.class.getSimpleName(),
                R.id.fragment_container, SettingsLaser10wThicknessMeasureCalibrationPointsFragment.getInstance());
    }

    public void gotoLaser10wThicknessMeasureCalibrationMeasureFragment() {
        addFragment(R.id.fragment_container, SettingsLaser10wThicknessMeasureCalibrationMeasureFragment.getInstance());
    }

    public void gotoLaser10wThicknessMeasureCalibrationCongratulateFragment() {
        addFragment(R.id.fragment_container,SettingsLaser10wThicknessMeasureCalibrationCongratulateFragment.getInstance());

    }
}
