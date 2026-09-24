package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling;

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

public class BedLevelingStartFragment extends BaseFragment {

    private DualExtruderBedLevelingViewModel mViewModel;

    public static Fragment newInstance() {
        return new BedLevelingStartFragment();
    }

    @BindView(R.id.sbg_cali_mode)
    SegmentedButtonGroup mSbgCaliType;
    @BindView(R.id.sbg_heat_or_not)
    SegmentedButtonGroup mSbgHeatOrNot;
    @BindView(R.id.sbg_grid)
    SegmentedButtonGroup mSbgGrid;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_bed_leveling_start;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getActivityScopeViewModel(DualExtruderBedLevelingViewModel.class);
        initView();
    }

    private void initView() {
        mSbgCaliType.setPosition(mViewModel.getIfUserSelectAuto() ? 0 : 1, false);
        mSbgHeatOrNot.setPosition(mViewModel.getIfUserSelectHeat() ? 0 : 1, false);
        mSbgGrid.setPosition(mViewModel.getSelectedGridPosition(), false);

        mSbgCaliType.setOnPositionChanged(position -> mViewModel.setIfSelectAuto(position == 0));
        mSbgHeatOrNot.setOnPositionChanged(position -> mViewModel.setIfSelectHeat(position == 0));
        mSbgGrid.setOnPositionChanged(position -> mViewModel.setSelectedGridPosition(position));
    }

    @OnClick(R.id.btn_start)
    void onStartClick() {
        if (mViewModel.getIfUserSelectHeat()) {
            Logger.d("Start Dual Extruder Heated bed leveling.");
            ((DualExtruderBedLevellingActivity) requireActivity()).goHeating();
        } else {
            Logger.d("Start Dual Extruder leveling.");
            ((DualExtruderBedLevellingActivity) requireActivity()).goLeveling(true);
        }
    }
}
