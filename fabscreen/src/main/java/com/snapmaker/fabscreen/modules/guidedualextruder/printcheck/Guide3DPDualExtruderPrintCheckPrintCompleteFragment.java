package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPDualExtruderPrintCheckPrintCompleteFragment extends BaseFragment {
    public static Guide3DPDualExtruderPrintCheckPrintCompleteFragment newInstance() {
        return new Guide3DPDualExtruderPrintCheckPrintCompleteFragment();
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
        ((Guide3DPDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckGetStarted();
//        super.back();
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_next)
    void onClickNext() {
        ((Guide3DPDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckConfirmZ();
    }

    @OnClick(R.id.btn_calibration_dual_extruder_print_check_restart_print)
    void onClickRestartPrint() {
        ((Guide3DPDualExtruderPrintCheckActivity) requireActivity()).startPrintCheckGetStarted();
    }
}
