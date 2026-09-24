package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.SlidingRulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderXYCalibrationCheckResultIntroFragment extends BaseFragment {


    public static Fragment newInstance() {
        return new Guide3DPDualExtruderXYCalibrationCheckResultIntroFragment();
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
        ((GuideDualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationCheckResult();
    }
}
