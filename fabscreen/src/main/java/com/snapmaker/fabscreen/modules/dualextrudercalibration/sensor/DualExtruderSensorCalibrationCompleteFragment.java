package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class DualExtruderSensorCalibrationCompleteFragment extends BaseFragment {
    public static DualExtruderSensorCalibrationCompleteFragment newInstance() {
        return new DualExtruderSensorCalibrationCompleteFragment();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_sensor_calibration_all_complete;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_all_complete)
    void onClickComplete() {
        Logger.i("Sensor Calibration done.");
        requireActivity().finish();
    }
}
