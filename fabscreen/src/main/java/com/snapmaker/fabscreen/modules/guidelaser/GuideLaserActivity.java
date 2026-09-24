package com.snapmaker.fabscreen.modules.guidelaser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidelaser.autofocus.GuideLaserAutoFocusPickFragment;
import com.snapmaker.fabscreen.modules.guidelaser.autofocus.GuideLaserAutoFocusStep1Fragment;
import com.snapmaker.fabscreen.modules.guidelaser.autofocus.GuideLaserAutoFocusStep2Fragment;
import com.snapmaker.fabscreen.modules.guidelaser.cameracalibration.GuideLaserCameraCalibrationIntroFragment;
import com.snapmaker.fabscreen.modules.guidelaser.cameracalibration.GuideLaserCameraCalibrationStep1Fragment;
import com.snapmaker.fabscreen.modules.guidelaser.cameracalibration.GuideLaserCameraCalibrationStep2Fragment;
import com.snapmaker.fabscreen.modules.guidelaser.complete.GuideLaserCompleteFragment;
import com.snapmaker.fabscreen.modules.guidelaser.getstarted.GuideLaserGetStartedFragment;
import com.snapmaker.fabscreen.modules.guidelaser.measureheight.GuideLaserMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.guidelaser.measureheight.GuideLaserMeasureHeightIntroFragment;
import com.snapmaker.fabscreen.modules.guidelaser.preparematerial.GuideLaserPrepareMaterialFragment;
import com.snapmaker.fabscreen.modules.guidelaser.preparematerial.GuideLaserPrepareMaterialIntroFragment;
import com.snapmaker.fabscreen.modules.guidelaser.safety.GuideLaserSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.guidelaser.setorigin.GuideLaserSetOriginFragment;
import com.snapmaker.fabscreen.modules.guidelaser.setorigin.GuideLaserSetOriginIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class GuideLaserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, GuideLaserGetStartedFragment.newInstance());
    }

    /**
     * Prepare Material
     */
    public void startPrepareMaterialIntroFragment() {
        addFragment(R.id.fragment_container, GuideLaserPrepareMaterialIntroFragment.newInstance());
    }

    public void startPrepareMaterialFragment() {
        addFragment(R.id.fragment_container, GuideLaserPrepareMaterialFragment.newInstance());
    }

    /**
     * Measure Height
     *
     * onNext -> Move the head to center of plat and proper height.
     */
    public void startMeasureHeightIntroFragment() {
        addFragment(R.id.fragment_container, GuideLaserMeasureHeightIntroFragment.newInstance());
    }

    public void startMeasureHeightFragment() {
        addFragment(R.id.fragment_container, GuideLaserMeasureHeightFragment.newInstance());
    }

    /**
     * Safety Goggles
     */
    public void startSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, GuideLaserSafetyGogglesFragment.newInstance());
    }

    /**
     * Work Origin
     */
    public void startSetOriginIntroFragment() {
        addFragment(R.id.fragment_container, GuideLaserSetOriginIntroFragment.newInstance());
    }

    public void startSetOriginFragment() {
        addFragment(R.id.fragment_container, GuideLaserSetOriginFragment.newInstance());
    }

    /**
     * Auto Focus
     */
    public void startAutoFocusStep1Fragment() {
        addFragment(R.id.fragment_container, GuideLaserAutoFocusStep1Fragment.newInstance());
    }

    public void startAutoFocusStep2Fragment() {
        addFragment(R.id.fragment_container, GuideLaserAutoFocusStep2Fragment.newInstance());
    }

    public void startAutoFocusPickFragment() {
        addFragment(R.id.fragment_container, GuideLaserAutoFocusPickFragment.newInstance());
    }

    /**
     * Camera Calibration
     */
    public void startCameraCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, GuideLaserCameraCalibrationIntroFragment.newInstance());
    }

    public void startCameraCalibrationStep1Fragment() {
        addFragment(R.id.fragment_container, GuideLaserCameraCalibrationStep1Fragment.newInstance());
    }

    public void startCameraCalibrationStep2Fragment() {
        addFragment(R.id.fragment_container, GuideLaserCameraCalibrationStep2Fragment.newInstance());
    }

    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, GuideLaserCompleteFragment.newInstance());
    }
}
