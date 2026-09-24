package com.snapmaker.fabscreen.modules.laser10wcamera;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Laser10wCameraCalibrationActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        goto10wCameraCalibrationIntroFragment();
    }

    public void goto10wCameraCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, Laser10wLaserCameraCalibrationIntroFragment.newInstance());
    }

    public void gotoCameraCalibrationStep1() {
        addFragment(R.id.fragment_container, Laser10wCameraCalibrationStep1Fragment.newInstance());
    }

    public void gotoCameraCalibrationStep2() {
        addFragment(R.id.fragment_container, Laser10wCameraCalibrationStep2Fragment.newInstance());
    }

}
