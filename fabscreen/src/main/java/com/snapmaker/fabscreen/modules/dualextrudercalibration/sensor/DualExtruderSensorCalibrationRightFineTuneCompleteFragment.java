package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabLoading;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DualExtruderSensorCalibrationRightFineTuneCompleteFragment extends BaseFragment {

    @BindView(R.id.tv_calibration_dual_extruder_fine_tune_complete_title)
    TextView mTvTitle;
    @BindView(R.id.tv_calibration_dual_extruder_fine_tune_complete_desc)
    TextView mTvDesc;
    @BindView(R.id.btn_calibration_dual_extruder_fine_tune_result_complete)
    Button mBtnNext;

    private DualExtruderSensorCalibrationViewModel mActivityViewModel;

    public static DualExtruderSensorCalibrationRightFineTuneCompleteFragment newInstance() {
        return new DualExtruderSensorCalibrationRightFineTuneCompleteFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityViewModel = getActivityScopeViewModel(DualExtruderSensorCalibrationViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTvTitle.setText(R.string.calibration_dual_extruder_sensor_calibration_right_fine_tune_complete_title);
        mTvDesc.setText(R.string.calibration_dual_extruder_sensor_calibration_right_fine_tune_complete_desc);
        mBtnNext.setText(R.string.all_next);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_sensor_calibration_right_extruder_complete;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_calibration_dual_extruder_fine_tune_result_complete)
    void onClickNext() {
        mBtnNext.setEnabled(false);

        // Show dialog and switch to left fine tune.
        mActivityViewModel.startSensorLeftFineTune()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mBtnNext.setEnabled(true);
                    if (success) {
                        ((DualExtruderSensorCalibrationActivity) requireActivity()).startLeftExtruderFineTune();
                    } else {
                        // Failed
                    }
                }, e -> {
                    mBtnNext.setEnabled(true);
                    LogHelper.log(e);
                });
    }


}
