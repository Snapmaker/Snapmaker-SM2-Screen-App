package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class Guide3DPDualExtruderBedLevelingStartFragment extends BaseFragment {

    private GuideDualExtruderVerticalLevelingViewModel mViewModel;

    public static Fragment newInstance() {
        return new Guide3DPDualExtruderBedLevelingStartFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getActivityScopeViewModel(GuideDualExtruderVerticalLevelingViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_bed_leveling_start;
    }

    @OnClick(R.id.btn_start)
    void onStartClick() {
        Logger.d("Start Dual Extruder Heated bed leveling.");
        ((GuideDualExtruderVerticalLevellingActivity) requireActivity()).goHeating();
    }
}
