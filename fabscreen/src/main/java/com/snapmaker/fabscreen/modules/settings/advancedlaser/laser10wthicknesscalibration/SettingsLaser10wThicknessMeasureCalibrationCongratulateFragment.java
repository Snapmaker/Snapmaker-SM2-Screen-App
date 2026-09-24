package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

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
import fabscreen.libraries.legacy.base.BaseFragment;

public class SettingsLaser10wThicknessMeasureCalibrationCongratulateFragment extends BaseFragment {

    @BindView(R.id.tv_guide_complete_content)
    TextView mTvDesc;
    @BindView(R.id.btn_guide_complete_next)
    Button mBtnOK;
    private Laser10wThicknessCalibrationViewModel mViewModel;

    public static Fragment getInstance() {
        return new SettingsLaser10wThicknessMeasureCalibrationCongratulateFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        mTvDesc.setText(R.string.settings_laser_thickness_calibration_complete);
        mBtnOK.setText(R.string.all_ok);
    }

    @Override
    protected Laser10wThicknessCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Laser10wThicknessCalibrationViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_complete;
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickOK() {
        mViewModel.switchAFAssistLight(false);
        requireActivity().finish();
    }
}
