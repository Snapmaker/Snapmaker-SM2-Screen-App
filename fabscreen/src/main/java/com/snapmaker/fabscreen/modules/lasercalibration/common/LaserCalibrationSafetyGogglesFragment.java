package com.snapmaker.fabscreen.modules.lasercalibration.common;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import butterknife.OnClick;

public class LaserCalibrationSafetyGogglesFragment extends BaseFragment {
    public static LaserCalibrationSafetyGogglesFragment newInstance() {
        return new LaserCalibrationSafetyGogglesFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        if (getActivity() != null) {
            boolean isRotaryOnline = getModel().getMachineController().isRotaryModuleAvailable();
            int calibrationMode;
            if (isRotaryOnline) {
                // Laser calibration with rotary module.
                calibrationMode = getModel().getPreferences().getLaser4AxisCalibrationMode();
                if (calibrationMode == 0) {
                    ((LaserCalibrationActivity) getActivity()).gotoLaser4AxisSetOrigin();
                } else {
                    ((LaserCalibrationActivity) getActivity()).gotoLaser4AxisCoarseTurning();
                }
            } else {
                calibrationMode = getModel().getPreferences().getLaserCalibrationMode();
                if (calibrationMode == 0) {
                    ((LaserCalibrationActivity) getActivity()).gotoLaserCalibrationSetOriginFragment();
                } else {
                    ((LaserCalibrationActivity) getActivity()).gotoCoarseTuningFragment();
                }
            }
        }
    }
}
