package com.snapmaker.fabscreen.modules.calibration;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class CalibrationActivity extends BaseActivity {

    private CalibrationViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);
        }

        mViewModel = getViewModel(CalibrationViewModel.class);

        initRootFragment();
    }

    private void initRootFragment() {
        boolean isHeatedLevelingOn = getModel().getPreferences().get3DPCalibrationHeatedLevelingOn();

        if (isHeatedLevelingOn) {
            startCalibrationHeatedLevelingIntroFragment();
        } else {
            startCalibrationIntroFragment();
        }
    }

    private void startCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, CalibrationIntroFragment.newInstance());
    }

    private void startCalibrationHeatedLevelingIntroFragment() {
        addFragment(R.id.fragment_container, CalibrationHeatedLevelingIntroFragment.newInstance());
    }

    public void startCalibrationPreHeatedFragment() {
        addFragment(R.id.fragment_container, CalibrationPreHeatedBedFragment.newInstance());
    }

    public void startCalibrationFragment() {
        addFragment(R.id.fragment_container, CalibrationFragment.newInstance());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.dispose();
    }
}
