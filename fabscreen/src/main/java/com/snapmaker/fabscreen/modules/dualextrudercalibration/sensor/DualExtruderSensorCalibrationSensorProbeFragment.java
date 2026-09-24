package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderSensorCalibrationSensorProbeFragment extends BaseFragment {

    private DualExtruderSensorCalibrationViewModel mActivityViewModel;

    public static Fragment newInstance() {
        return new DualExtruderSensorCalibrationSensorProbeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityViewModel = getActivityScopeViewModel(DualExtruderSensorCalibrationViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_sensor_calibration_sensor_probe;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

        // Start Sensor Calibration when view created.
        mActivityViewModel.startSensorCalibration();

        mActivityViewModel.getProcessObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(process -> {
                    switch (process) {
                        case CALIBRATING:
                            // Start Sensor Calibration as default, do nothing.
                            break;
                        case LEFT_PROBE_COMPLETE:
                            break;
                        case RIGHT_PROBE_COMPLETE:
                            gotoRightFineTune();
                            break;
                        case FAIL:
                            // Failed, pop up dialog and abort calibration.
                            break;
                        case COMPLETE:
                        case LEFT_FINE_TUNE_COMPLETE:
                        case RIGHT_FINE_TUNE_COMPLETE:
                            break;
                        default:
                            break;
                    }
                });
    }

    private void initView() {

    }

    private void gotoRightFineTune() {
        ((DualExtruderSensorCalibrationActivity) requireActivity()).startRightExtruderFineTune();
    }
}
