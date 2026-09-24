package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration.GuideDualExtruderXYCalibrationActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class DualExtruderXYCalibrationPrintCompleteFragment extends BaseFragment {

    private DualExtruderXYCalibrationPrintViewModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mViewModel = getActivityScopeViewModel(DualExtruderXYCalibrationPrintViewModel.class);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_xy_calibration_print_complete;
    }

    @Override
    protected void back() {
        requireActivity().finish();
//        super.back();
    }

    private void initView() {

    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_xy_print_next)
    void onClickNext() {
        ((DualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationCheckResultIntro();
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_xy_print_again)
    void onClickPrintAgain() {
        ((DualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationStart();
    }
}
