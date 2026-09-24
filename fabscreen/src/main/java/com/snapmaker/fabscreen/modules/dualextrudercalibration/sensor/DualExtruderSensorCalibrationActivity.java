package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DualExtruderSensorCalibrationActivity extends BaseActivity {
    DualExtruderSensorCalibrationViewModel mSensorCalibrationViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        mSensorCalibrationViewModel = getViewModel(DualExtruderSensorCalibrationViewModel.class);
        startSensorCalibrationIntro();

        mSensorCalibrationViewModel.getProcessObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(process -> {
                    if (process == DualExtruderSensorCalibrationViewModel.Process.FAIL) {
                        FabConfirm.create(this)
                                .setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setDescription(R.string.calibration_dual_extruder_sensor_calibration_dialog_process_failed_desc)
                                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                    mSensorCalibrationViewModel.exitSensorCalibration()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .as(bindToLifecycle())
                                            .subscribe(success -> {
                                                if (success) {
                                                    Logger.d("Abort sensor calibration.");
                                                }
                                                dialog.dismiss();
                                                finish();
                                            });
                                })
                                .setCancel(R.string.all_cancel, ((dialog, which) -> dialog.dismiss()))
                                .show();
                    }
                });
    }

    private void startSensorCalibrationIntro() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationIntroFragment.newInstance());
    }

    public void startSensorCalibrationProbe() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationSensorProbeFragment.newInstance());
    }

    public void startRightExtruderFineTune() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationSensorRightFineTuneFragment.newInstance());
    }

    public void gotoRightExtruderFineTuneComplete() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationRightFineTuneCompleteFragment.newInstance());
    }

    public void startLeftExtruderFineTune() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationSensorLeftFineTuneFragment.newInstance());
    }

    public void gotoSensorCalibrationComplete() {
        addFragment(R.id.fragment_container, DualExtruderSensorCalibrationCompleteFragment.newInstance());
    }
}
