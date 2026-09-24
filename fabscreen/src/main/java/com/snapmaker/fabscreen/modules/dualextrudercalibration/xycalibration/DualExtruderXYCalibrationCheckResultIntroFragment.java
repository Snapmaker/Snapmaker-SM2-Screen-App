package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration.GuideDualExtruderXYCalibrationActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class DualExtruderXYCalibrationCheckResultIntroFragment extends BaseFragment {


    public static Fragment newInstance() {
        return new DualExtruderXYCalibrationCheckResultIntroFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_xy_check_result_intro;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    private void initView() {
        setTitle(R.string.guide_3dp_dual_extruder_xy_calibration_check_result_intro_title);
    }

    @OnClick(R.id.btn_calibration_dual_extruder_xy_check_result_intro_next)
    void onCompleteClicked() {
        ((DualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationCheckResult();
    }
}
