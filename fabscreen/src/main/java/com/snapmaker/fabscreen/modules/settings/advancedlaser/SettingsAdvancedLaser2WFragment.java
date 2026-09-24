package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

@SuppressLint("NonConstantResourceId")
public class SettingsAdvancedLaser2WFragment extends BaseFragment {
    @BindView(R.id.ll_settings_laser_2w_cross_line_indicator)
    View mViewCrossLineIndicator;

    public static Fragment getInstance() {
        return new SettingsAdvancedLaser2WFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_advance_laser_2w);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_advanced_laser_2w;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {

    }

    @OnClick(R.id.ll_settings_laser_2w_cross_line_indicator)
    void onClickCrossLineIndicator() {
        ((SettingsActivity) requireActivity()).startCrossLineIndicatorOffsetFragment();
    }
}
