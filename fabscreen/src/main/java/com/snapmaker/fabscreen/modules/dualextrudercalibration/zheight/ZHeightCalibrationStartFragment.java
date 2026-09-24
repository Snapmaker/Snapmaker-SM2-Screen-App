package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.view.FabConfirm;

public class ZHeightCalibrationStartFragment extends BaseFragment {

    private ZHeightCalibrationViewModel mViewModel;

    public static Fragment newInstance() {
        return new ZHeightCalibrationStartFragment();
    }

    @BindView(R.id.sbg_cali_mode)
    SegmentedButtonGroup mSbgCaliMode;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_z_height_calibration_start;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(ZHeightCalibrationViewModel.class);
        initView();
    }

    private void initView() {
        mSbgCaliMode.setPosition(mViewModel.getCalibrationMode(), false);
        mSbgCaliMode.setOnPositionChanged(position -> mViewModel.setCalibrationMode(position));
    }

    @OnClick(R.id.btn_start)
    void onStartClicked() {
        FabConfirm.create(requireContext()).setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.leveling_bed_clean_nozzle_content)
                .setConfirm(R.string.all_confirm, ((dialog, which) -> {
                    dialog.dismiss();
                    Logger.d("Start Z Offset %s calibration", (mViewModel.getCalibrationMode() == 0) ? "Auto" : "Manual");
                    ((DualExtruderZHeightCalibrationActivity) requireActivity()).goZHeightCalibration(mViewModel.getCalibrationMode());
                })).show();
    }
}
