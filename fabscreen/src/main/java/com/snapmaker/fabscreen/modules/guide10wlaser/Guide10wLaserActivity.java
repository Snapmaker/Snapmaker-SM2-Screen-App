package com.snapmaker.fabscreen.modules.guide10wlaser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide10wlaser.cameralibration.Guide10wLaserCameraCalibrationIntroFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.cameralibration.Guide10wLaserCameraCalibrationStep1Fragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.cameralibration.Guide10wLaserCameraCalibrationStep2Fragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.complete.Guide10wLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.getstarted.Guide10wLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration.Guide10wThicknessMeasureCalibrationMeasureFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration.Guide10wThicknessMeasureCalibrationPointsFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration.Guide10wThicknessMeasurementCalibrationIntroFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.touchplatform.Guide10wLaserTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.guide10wlaser.touchplatform.Guide10wLaserTouchPlatformFragmentIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide10wLaserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, Guide10wLaserGetStartedFragment.newInstance());
    }

    /**
     * Touch Platform
     */
    public void startTouchPlatformFragmentIntroFragment() {
        addFragment(
                Guide10wLaserTouchPlatformFragmentIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide10wLaserTouchPlatformFragmentIntroFragment.newInstance());
    }

    public void startTouchPlatformFragment() {
        addFragment(R.id.fragment_container, Guide10wLaserTouchPlatformFragment.newInstance());
    }

    /**
     * Thickness Measurement Calibration
     */
    public void startThicknessMeasurementCalibrationIntroFragment() {
        addFragment(Guide10wThicknessMeasurementCalibrationIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide10wThicknessMeasurementCalibrationIntroFragment.newInstance());
    }

    public void Guide10wThicknessMeasureCalibrationPointsFragment() {
        addFragment(R.id.fragment_container, Guide10wThicknessMeasureCalibrationPointsFragment.newInstance());
    }

    public void Guide10wThicknessMeasureCalibrationMeasureFragment() {
        addFragment(R.id.fragment_container, Guide10wThicknessMeasureCalibrationMeasureFragment.newInstance());
    }

    /**
     * Camera Calibration
     */
    public void start10wCameraCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, Guide10wLaserCameraCalibrationIntroFragment.newInstance());
    }

    public void start10wCameraCalibrationStep1Fragment() {
        addFragment(R.id.fragment_container, Guide10wLaserCameraCalibrationStep1Fragment.newInstance());
    }

    public void start10wCameraCalibrationStep2Fragment() {
        addFragment(R.id.fragment_container, Guide10wLaserCameraCalibrationStep2Fragment.newInstance());
    }

    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, Guide10wLaserCompleteFragment.newInstance());
    }

}
