package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight.AutoZHeightCalibrationViewModel;
import com.snapmaker.fabscreen.modules.loadfilament.LoadFilamentViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.SlidingRulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DualExtruderXYCalibrationCheckResultFragment extends BaseFragment {

    @BindView(R.id.srv_calibration_dual_extruder_xy_check_result_x_direction)
    SlidingRulerView mSrvXResult;
    @BindView(R.id.srv_calibration_dual_extruder_xy_check_result_y_direction)
    SlidingRulerView mSrvYResult;
    @BindView(R.id.btn_calibration_dual_extruder_xy_check_result_next)
    Button mBtnNext;

    private XYCalibrationCheckResultViewModel mViewModel;

    public static Fragment newInstance() {
        return new DualExtruderXYCalibrationCheckResultFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_dual_extruder_xy_check_result;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getFragmentScopeViewModel(XYCalibrationCheckResultViewModel.class);
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
                        ((DualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationComplete();
                    } else {
                        FabConfirm.create(requireContext())
                                .setDescription("设置 XY Offset 过程中失败")
                                .setConfirm(R.string.all_confirm, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }, e -> {
                    mBtnNext.setEnabled(true);
                    LogHelper.log(e);
                });
    }
}
