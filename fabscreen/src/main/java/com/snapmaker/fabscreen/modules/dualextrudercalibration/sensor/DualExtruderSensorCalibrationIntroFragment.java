package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;

public class DualExtruderSensorCalibrationIntroFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new DualExtruderSensorCalibrationIntroFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_sensor_calibration_start;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {

    }

    @OnClick(R.id.btn_calibration_dual_extruder_sensor_calibration_start)
    void onStartClicked() {
        ((DualExtruderSensorCalibrationActivity) requireActivity()).startSensorCalibrationProbe();
    }
}
