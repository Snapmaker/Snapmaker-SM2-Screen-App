package com.snapmaker.fabscreen.modules.lasercalibration;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.TextFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.autofocus.LaserCalibrationAutoFocusStep1Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.autofocus.LaserCalibrationAutoFocusStep2Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.common.LaserCalibrationMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.common.LaserCalibrationPickFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.common.LaserCalibrationSafetyGogglesFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.common.LaserCalibrationSetMaterialThicknessFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.common.LaserCalibrationSetOriginFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.complete.Calibration2wLaserTouchPlatformCompleteFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.complete.Calibration40wLaserTouchPlatformCompleteFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.manualfocus.LaserCalibrationCoarseTuningFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.manualfocus.LaserCalibrationManualFocusStep1Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.manualfocus.LaserCalibrationManualFocusStep2Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisAutoFocusStep1Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisAutoFocusStep2Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisCoarseTuningFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisInstallMaterialFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisManualFocusStep1Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisManualFocusStep2Fragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisMeasureHeightFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisPickFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisSetOriginFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.rotary.LaserCalibration4AxisSetWorkpieceFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.Calibration2wPlatformHeightLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.Calibration2wPlatformHeightLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.Calibration40wPlatformHeightLaserPullInFocusLeverFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.Calibration40wPlatformHeightLaserPullOutFocusLeverFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.CalibrationLaser2wTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.CalibrationLaser2wTouchPlatformFragmentIntroFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.CalibrationLaser40wTouchPlatformFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.touchplatform.CalibrationLaser40wTouchPlatformFragmentIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.data.Constants;

public class LaserCalibrationActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            int headType = getModel().getMachineController().getHeadType();
            initRootFragment(headType);
        }
    }

    private void initRootFragment(int headType) {
        Fragment fragment = null;
        switch (headType) {
            case Constants.HEAD_UNPLUGGED: {
                TextFragment fragment1 = new TextFragment();
                fragment1.setTitle(getResources().getString(R.string.all_calibration));
                fragment1.setText(getResources().getString(R.string.calibration_tool_head_not_detected));
                fragment = fragment1;
                break;
            }
            case Constants.HEAD_LASER: {
                if (getModel().getMachineController().isRotaryModuleAvailable()) {
                    gotoLaserCalibration4AxisSetWorkpiece();
                } else {
                    gotoSetMaterialThicknessFragment();
                }
                break;
            }
            case Constants.HEAD_LASER_2W_IR:
                if (getModel().getMachineController().isRotaryModuleAvailable()) {
                    //
                } else {
                    start2wPlatformHeightCalibrationIntro();
                }
                break;
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                if (getModel().getMachineController().isRotaryModuleAvailable()) {
//                    gotoLaserCalibration4AxisSetWorkpiece();
                } else {
                    start40wPlatformHeightCalibrationIntro();
                }
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W: {
                TextFragment fragment1 = new TextFragment();
                fragment1.setTitle(getResources().getString(R.string.all_calibration));
                fragment1.setText(getResources().getString(R.string.cnc_calibration_description));
                fragment = fragment1;
                break;
            }
        }

        if (fragment != null) {
            replaceFragment(R.id.fragment_container, fragment);
        }
    }

    /*
     * Laser Focus
     * <p>
     * Auto Mode
     * <p>
     * Manual Mode:
     * 1. Set Material Thickness (move Z axis)
     * 2. Manual coarse tuning
     * 3. Safety Goggles (start engrave test)
     * 4. Manual pick line
     */

    /**
     * Set Material Thickness
     * <p>
     * Auto Mode step 1
     * Manual Mode step 1
     */
    public void gotoSetMaterialThicknessFragment() {
        addFragment(R.id.fragment_container, LaserCalibrationSetMaterialThicknessFragment.newInstance());
    }

    /**
     * Measure Height
     * <p>
     * Auto Mode step 2
     * Manual Mode step 2
     */
    public void gotoMeasureHeightFragment() {
        addFragment(R.id.fragment_container, LaserCalibrationMeasureHeightFragment.newInstance());
    }

    /**
     * Safety Goggles
     * <p>
     * Auto Mode step 3
     * Manual Mode step 3
     */
    public void gotoSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, LaserCalibrationSafetyGogglesFragment.newInstance());
    }

    /**
     * Set Origin
     * <p>
     * Auto Mode step 4
     */
    public void gotoLaserCalibrationSetOriginFragment() {
        addFragment(R.id.fragment_container, LaserCalibrationSetOriginFragment.newInstance());
    }

    /**
     * Auto Fine Tune
     * <p>
     * Auto Mode step 5
     */
    public void startAutoFocusStep1Fragment() {
        addFragment(R.id.fragment_container, LaserCalibrationAutoFocusStep1Fragment.newInstance());
    }

    /**
     * Auto Fine Tune
     * <p>
     * Auto Mode step 6
     */
    public void startAutoFocusStep2Fragment() {
        addFragment(R.id.fragment_container, LaserCalibrationAutoFocusStep2Fragment.newInstance());
    }

    /**
     * Coarse Tuning
     * <p>
     * Manual Mode step 4
     */
    public void gotoCoarseTuningFragment() {
        addFragment(R.id.fragment_container, LaserCalibrationCoarseTuningFragment.newInstance());
    }

    public void startManualFocusStep1Fragment() {
        addFragment(R.id.fragment_container, LaserCalibrationManualFocusStep1Fragment.newInstance());
    }

    /**
     * Manual Fine Tune
     * <p>
     * Manual Mode step 5
     */
    public void gotoManualFocusStep2Fragment() {
        LaserCalibrationManualFocusStep2Fragment fragment = new LaserCalibrationManualFocusStep2Fragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * Manual Fine Tune Pick
     * <p>
     * Manual Mode step 6
     */
    public void gotoLaserCalibrationManualFineTunePick() {
        LaserCalibrationPickFragment fragment = new LaserCalibrationPickFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    // 4Axis
    /**
     * 4Axis Set Workpiece
     * <p>
     * 4Axis Auto Mode step 1
     * 4Axis Manual Mode step 1
     */
    public void gotoLaserCalibration4AxisSetWorkpiece() {
        LaserCalibration4AxisSetWorkpieceFragment fragment = new LaserCalibration4AxisSetWorkpieceFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * 4Axis Tighten Material
     * <p>
     * 4Axis Auto Mode step 2
     * 4Axis Manual Mode step 2
     */
    public void gotoLaser4AxisInstallMaterial() {
        LaserCalibration4AxisInstallMaterialFragment fragment = new LaserCalibration4AxisInstallMaterialFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoLaser4AxisMeasureHeight() {
        LaserCalibration4AxisMeasureHeightFragment fragment = new LaserCalibration4AxisMeasureHeightFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * 4Axis Coarse Turning
     * <p>
     * 4Axis Manual Mode step 5
     */
    public void gotoLaser4AxisCoarseTurning() {
        addFragment(R.id.fragment_container, LaserCalibration4AxisCoarseTuningFragment.newInstance());
    }

    /**
     * 4Axis Set Origin
     * <p>
     * 4Axis Auto Mode step 5
     * 4Axis Manual Mode step 6
     */
    public void gotoLaser4AxisSetOrigin() {
        addFragment(R.id.fragment_container, LaserCalibration4AxisSetOriginFragment.newInstance());
    }

    /**
     * 4Axis Auto Fine Tune
     * <p>
     * 4Axis Auto Mode step 6
     */
    public void start4AxisAutoFocusStep1Fragment() {
        addFragment(R.id.fragment_container, LaserCalibration4AxisAutoFocusStep1Fragment.newInstance());
    }

    /**
     * 4Axis Auto Fine Tune
     * <p>
     * 4Axis Auto Mode step 7
     */
    public void start4AxisAutoFocusStep2Fragment() {
        addFragment(R.id.fragment_container, LaserCalibration4AxisAutoFocusStep2Fragment.newInstance());
    }

    public void start4AxisManualFocusStep1Fragment() {
        addFragment(R.id.fragment_container, LaserCalibration4AxisManualFocusStep1Fragment.newInstance());
    }

    /**
     * Manual Fine Tune
     * <p>
     * 4Axis Manual Mode step 5
     */
    public void goto4AxisManualFocusStep2Fragment() {
        LaserCalibration4AxisManualFocusStep2Fragment fragment = new LaserCalibration4AxisManualFocusStep2Fragment();
        addFragment(R.id.fragment_container, fragment);
    }

    /**
     * 4Axis Manual Pick
     * <p>
     * 4Axis Manual Mode step 8
     */
    public void goto4AxisManualFineTunePick() {
        LaserCalibration4AxisPickFragment fragment = new LaserCalibration4AxisPickFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start40wPlatformHeightCalibrationIntro() {
        CalibrationLaser40wTouchPlatformFragmentIntroFragment fragment = new CalibrationLaser40wTouchPlatformFragmentIntroFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start40wPlatformHeightPullOutFocusLever() {
        Calibration40wPlatformHeightLaserPullOutFocusLeverFragment fragment = new Calibration40wPlatformHeightLaserPullOutFocusLeverFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start40wPlatformHeightCalibration() {
        CalibrationLaser40wTouchPlatformFragment fragment = new CalibrationLaser40wTouchPlatformFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start40wPlatformHeightPullInFocusLever() {
        Calibration40wPlatformHeightLaserPullInFocusLeverFragment fragment = new Calibration40wPlatformHeightLaserPullInFocusLeverFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start40wPlatformHeightComplete() {
        Calibration40wLaserTouchPlatformCompleteFragment fragment = Calibration40wLaserTouchPlatformCompleteFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start2wPlatformHeightCalibrationIntro() {
        CalibrationLaser2wTouchPlatformFragmentIntroFragment fragment = new CalibrationLaser2wTouchPlatformFragmentIntroFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start2wPlatformHeightPullOutFocusLever() {
        Calibration2wPlatformHeightLaserPullOutFocusLeverFragment fragment = new Calibration2wPlatformHeightLaserPullOutFocusLeverFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start2wPlatformHeightCalibration() {
        CalibrationLaser2wTouchPlatformFragment fragment = new CalibrationLaser2wTouchPlatformFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start2wPlatformHeightPullInFocusLever() {
        Calibration2wPlatformHeightLaserPullInFocusLeverFragment fragment = new Calibration2wPlatformHeightLaserPullInFocusLeverFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void start2wPlatformHeightComplete() {
        Calibration2wLaserTouchPlatformCompleteFragment fragment = Calibration2wLaserTouchPlatformCompleteFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }
}
