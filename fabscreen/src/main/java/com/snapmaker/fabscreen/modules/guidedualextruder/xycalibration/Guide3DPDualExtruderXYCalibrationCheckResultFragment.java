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
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.SlidingRulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderXYCalibrationCheckResultFragment extends BaseFragment {

    @BindView(R.id.srv_calibration_dual_extruder_xy_check_result_x_direction)
    SlidingRulerView mSrvXResult;
    @BindView(R.id.srv_calibration_dual_extruder_xy_check_result_y_direction)
    SlidingRulerView mSrvYResult;
    @BindView(R.id.btn_calibration_dual_extruder_xy_check_result_next)
    Button mBtnNext;

    private Guide3DPDualExtruderXYCalibrationCheckResultViewModel mViewModel;

    public static Fragment newInstance() {
        return new Guide3DPDualExtruderXYCalibrationCheckResultFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_xy_check_result;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getFragmentScopeViewModel(Guide3DPDualExtruderXYCalibrationCheckResultViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    private void initView() {
        setTitle(R.string.guide_3dp_dual_extruder_xy_calibration_check_result_title);

        mSrvXResult.setShowStr("X");
        mSrvXResult.setOnProgressChangeListener(index -> {
            float offset = index * 0.08f;
            mViewModel.setCalibrationOffset(0, offset);
        });

        mSrvYResult.setShowStr("Y");
        mSrvYResult.setOnProgressChangeListener(index -> {
            float offset = index * 0.08f;
            mViewModel.setCalibrationOffset(1, offset);
        });
    }

    @OnClick(R.id.btn_calibration_dual_extruder_xy_check_result_next)
    void onCompleteClicked() {
        mBtnNext.setEnabled(false);
        mViewModel.saveXYCalibrationOffsetResult()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mBtnNext.setEnabled(true);
                    if (success) {
                        ((GuideDualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationComplete();
                    } else {
                        FabConfirm.create(requireContext())
                                .setDescription(R.string.guide_3dp_dual_extruder_xy_calibration_set_value_failed_desc)
                                .setConfirm(R.string.all_confirm, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }, e -> {
                    mBtnNext.setEnabled(true);
                    LogHelper.log(e);
                });
    }
}
