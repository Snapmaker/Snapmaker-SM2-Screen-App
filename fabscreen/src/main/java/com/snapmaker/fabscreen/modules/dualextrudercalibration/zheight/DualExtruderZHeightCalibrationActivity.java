package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class DualExtruderZHeightCalibrationActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        showZHeightCalibrationStart();
    }

    private void showZHeightCalibrationStart() {
        addFragment(R.id.fragment_container, ZHeightCalibrationStartFragment.newInstance());
    }

    /**
     * @param mode 0-auto; 1-manual
     */
    public void goZHeightCalibration(int mode) {
        Fragment fragment = mode == 0 ? AutoZHeightCalibrationFragment.newInstance() : ManualZHeightCalibrationFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void goManualZHeightSuccess() {
        Fragment fragment = ZHeightCalibrationManualSuccessFragment.newInstance();
        addFragment(R.id.fragment_container, fragment);
    }
}
