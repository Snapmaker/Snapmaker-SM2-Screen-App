package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import fabscreen.libraries.legacy.base.BaseViewModel;

public class ZHeightCalibrationViewModel extends BaseViewModel {
    private int mCalibrationMode = 0;
    private int mManualCalibrationExtruder = 0;

    /**
     * @param mode 0-auto; 1-manual
     */
    public void setCalibrationMode(int mode) {
        mCalibrationMode = mode;
    }

    public int getCalibrationMode() {
        return mCalibrationMode;
    }

    public void setManualCalibrationExtruder(int manualCalibrationExtruder) {
        this.mManualCalibrationExtruder = manualCalibrationExtruder;
    }

    public int getManualCalibrationExtruder() {
        return mManualCalibrationExtruder;
    }
}
