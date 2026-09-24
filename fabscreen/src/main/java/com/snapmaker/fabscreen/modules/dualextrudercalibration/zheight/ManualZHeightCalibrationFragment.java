package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ManualZHeightCalibrationFragment extends BaseFragment {
    public static Fragment newInstance() {
        return new ManualZHeightCalibrationFragment();
    }

    @BindView(R.id.sbg_step_width)
    SegmentedButtonGroup mSbgStepWidth;
    @BindView(R.id.btn_up)
    Button mBtnUp;
    @BindView(R.id.btn_down)
    Button mBtnDown;
    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_save)
    Button mBtnSave;

    @BindView(R.id.tv_calibration_dual_extruder_z_height_manual_desc)
    TextView mTvDesc;

    private int mCurrentProbeExtruder;
    private float mMoveStep = 0.1f;

    private ZHeightCalibrationViewModel mActivityViewModel;
    private ManualZHeightCalibrationViewModel mFragmentViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActivityViewModel = getActivityScopeViewModel(ZHeightCalibrationViewModel.class);
        mCurrentProbeExtruder = mActivityViewModel.getManualCalibrationExtruder();

        mFragmentViewModel = getFragmentScopeViewModel(ManualZHeightCalibrationViewModel.class);

        initView();

        mFragmentViewModel.getProcessObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::updateView);

        mFragmentViewModel.startManualZCalibration(mCurrentProbeExtruder);
    }

    void updateView(ManualZHeightCalibrationViewModel.Process process) {
        switch (process) {
            case MOVING:
                mBtnBack.setEnabled(false);
                mBtnUp.setEnabled(false);
                mBtnDown.setEnabled(false);
                mBtnSave.setEnabled(false);
                break;
            case FAIL:
                mBtnBack.setEnabled(true);
                mBtnUp.setEnabled(true);
                mBtnDown.setEnabled(true);
                mBtnSave.setEnabled(true);
                // show error dialog.
                break;
            case COMPLETE:
                mBtnBack.setEnabled(true);
                mBtnUp.setEnabled(true);
                mBtnDown.setEnabled(true);
                mBtnSave.setEnabled(true);
                break;
        }
    }

    private void initView() {
        final int titleRes = isLeftExtruder(mCurrentProbeExtruder) ?
                R.string.calibration_z_height_manual_left_nozzle_title :
                R.string.calibration_z_height_manual_right_nozzle_title;
        setTitle(titleRes);

        if (isLeftExtruder(mCurrentProbeExtruder)) {
            mTvDesc.setText(R.string.calibration_z_height_manual_instruction_left_extruder);
        } else {
            mTvDesc.setText(R.string.calibration_z_height_manual_instruction_right_extruder);
        }

        mSbgStepWidth.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mMoveStep = 0.05f;
                    break;
                case 1:
                    mMoveStep = 0.1f;
                    break;
                case 2:
                    mMoveStep = 0.5f;
                    break;
            }
        });
        mSbgStepWidth.setPosition(1, false);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_manual_z_height_calibration;
    }

    private boolean isLeftExtruder(int extruder) {
        return extruder == 0;
    }

    @OnClick(R.id.btn_up)
    void onClickUp() {
        mFragmentViewModel.moveZAxisByStep(mMoveStep);
    }

    @OnClick(R.id.btn_down)
    void onClickDown() {
        mFragmentViewModel.moveZAxisByStep(-mMoveStep);
    }

    @OnClick(R.id.btn_save)
    void onClickSave() {
        if (mCurrentProbeExtruder == 0) {
            ((DualExtruderZHeightCalibrationActivity) requireActivity()).goManualZHeightSuccess();
        } else {
            mFragmentViewModel.saveManualZCalibration()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (success) {
                            ((DualExtruderZHeightCalibrationActivity) requireActivity()).goManualZHeightSuccess();
                        }
                    }, LogHelper::log);
        }
    }
}
