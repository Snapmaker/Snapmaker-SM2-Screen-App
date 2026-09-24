package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.OnClick;

public class GuideLaserCalibrationIntroFragment extends BaseFragment {
    public static GuideLaserCalibrationIntroFragment newInstance() {
        return new GuideLaserCalibrationIntroFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_laser_calibration_intro;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_laser_calibration_intro_get_started)
    void onClickNext() {
        if (getActivity() != null) {
            ((GuideRotaryLaserActivity) getActivity()).startSetWorkpieceFragment();
        }
    }
}
