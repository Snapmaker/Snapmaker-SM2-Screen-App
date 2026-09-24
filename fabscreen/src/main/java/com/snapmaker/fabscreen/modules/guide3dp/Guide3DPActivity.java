package com.snapmaker.fabscreen.modules.guide3dp;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.calibration.CalibrationViewModel;
import com.snapmaker.fabscreen.modules.guide3dp.calibration.Guide3DPCalibrationFragment;
import com.snapmaker.fabscreen.modules.guide3dp.calibration.Guide3DPCalibrationIntroFragment;
import com.snapmaker.fabscreen.modules.guide3dp.complete.Guide3DPCompleteFragment;
import com.snapmaker.fabscreen.modules.guide3dp.getstarted.Guide3DPGetStartedFragment;
import com.snapmaker.fabscreen.modules.guide3dp.preparefilament.Guide3DPPrepareFilamentFragment;
import com.snapmaker.fabscreen.modules.guide3dp.preparefilament.Guide3DPPrepareFilamentIntroFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide3DPActivity extends BaseActivity {

    private CalibrationViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }

        mViewModel = getViewModel(CalibrationViewModel.class);
    }

    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, Guide3DPGetStartedFragment.newInstance());
    }

    public void startCalibrationIntroFragment() {
        addFragment(Guide3DPCalibrationIntroFragment.class.getSimpleName(),
                R.id.fragment_container,
                Guide3DPCalibrationIntroFragment.newInstance());
    }

    public void startCalibrationFragment() {
        addFragment(R.id.fragment_container, Guide3DPCalibrationFragment.newInstance());
    }

    public void startPrepareFilamentIntroFragment() {
        addFragment(R.id.fragment_container, Guide3DPPrepareFilamentIntroFragment.newInstance());
    }

    public void startPrepareFilamentFragment() {
        addFragment(R.id.fragment_container, Guide3DPPrepareFilamentFragment.newInstance());
    }

    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, Guide3DPCompleteFragment.newInstance());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.dispose();
    }
}
