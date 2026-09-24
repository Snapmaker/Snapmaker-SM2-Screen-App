package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;

public class ZHeightCalibrationManualSuccessFragment extends BaseFragment {

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_complete)
    Button mBtnComplete;

    @BindView(R.id.tv_calibrate_progress)
    TextView mTvCalibrateProcessDesc;

    private ZHeightCalibrationViewModel mViewModel;

    public static Fragment newInstance() {
        return new ZHeightCalibrationManualSuccessFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ZHeightCalibrationViewModel.class);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_z_height_manual_success;
    }

    private void initView() {
        if (mViewModel.getManualCalibrationExtruder() == 0) {
            mBtnComplete.setText(R.string.all_next);
        } else {
            mBtnBack.setVisibility(Button.GONE);
            mTvCalibrateProcessDesc.setVisibility(TextView.GONE);
        }
    }

    @OnClick(R.id.btn_complete)
    void onClickComplete() {
        if (mViewModel.getManualCalibrationExtruder() == 0) {
            mViewModel.setManualCalibrationExtruder(1);
            ((DualExtruderZHeightCalibrationActivity) requireActivity()).goZHeightCalibration(1);
        } else {
            if (getModel().getPreferences().getNeedDoZHeightCalibration()) {
                getModel().getPreferences().setNeedDoZHeightCalibration(false);
            }
            requireActivity().finish();
        }
    }
}
