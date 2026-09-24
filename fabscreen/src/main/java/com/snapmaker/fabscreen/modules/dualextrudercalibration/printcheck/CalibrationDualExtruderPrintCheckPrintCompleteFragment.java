package com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class CalibrationDualExtruderPrintCheckPrintCompleteFragment extends BaseFragment {
    public static CalibrationDualExtruderPrintCheckPrintCompleteFragment newInstance() {
        return new CalibrationDualExtruderPrintCheckPrintCompleteFragment();
    }

    @BindView(R.id.btn_calibration_dual_extruder_print_check_next)
    Button mBtnNext;
    @BindView(R.id.btn_calibration_dual_extruder_print_check_restart_print)
    Button mBtnRestart;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_print_check_print_complete;
    }

    @Override
    protected void back() {
        ((CalibrationDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckGetStart();
//        super.back();
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_next)
    void onClickNext() {
        ((CalibrationDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckConfirmZ();
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_restart_print)
    void onClickRestartPrint() {
        ((CalibrationDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckGetStart();
    }
}
