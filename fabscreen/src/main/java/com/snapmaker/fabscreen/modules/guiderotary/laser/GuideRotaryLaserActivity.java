package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class GuideRotaryLaserActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserGetStartedFragment.newInstance());
    }

    public void startLaserCalibrationIntroFragment() {
        addFragment(R.id.fragment_container, GuideLaserCalibrationIntroFragment.newInstance());
    }

    /**
     * Prepare Material
     */
    public void startSetWorkpieceFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserSetWorkpieceFragment.newInstance());
    }

    public void startInstallMaterialFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserInstallMaterialFragment.newInstance());
    }

    /**
     * Measure Height
     *
     * onNext -> Move the head to center of plat and proper height.
     */

    public void startMeasureHeightIntroFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserMeasureHeightIntroFragment.newInstance());
    }

    public void startMeasureHeightFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserMeasureHeightFragment.newInstance());
    }

    /**
     * Safety Goggles
     */
    public void startSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserSafetyGogglesFragment.newInstance());
    }

    /**
     * Work Origin
     */
    public void startSetOriginIntroFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserSetOriginIntroFragment.newInstance());
    }

    public void startSetOriginFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserSetOriginFragment.newInstance());
    }

    /**
     * Manual Focus
     */
    public void startManualFocusStep1Fragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserManualFocusStep1Fragment.newInstance());
    }

    public void startManualFocusStep2Fragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserManualFocusStep2Fragment.newInstance());
    }

    public void startManualFocusPickFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserManualPickFragment.newInstance());
    }
    /**
     * Complete
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, GuideRotaryLaserCompleteFragment.newInstance());
    }
}
