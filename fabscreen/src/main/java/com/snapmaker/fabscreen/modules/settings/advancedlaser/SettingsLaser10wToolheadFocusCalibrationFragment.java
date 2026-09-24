package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class SettingsLaser10wToolheadFocusCalibrationFragment extends BaseFragment {
    @BindView(R.id.btn_start_toolhead_focus_calibration)
    Button mBtnStart;
    @BindView(R.id.btn_start_toolhead_focus_calibration_content)
    TextView mTvContent;

    public static Fragment getInstance() {
        return new SettingsLaser10wToolheadFocusCalibrationFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_laser_toolhead_focus_calibration_title);
        mBtnStart.setText(R.string.all_start);
        mTvContent.setText(R.string.settings_laser_toolhead_focus_calibration_page_desc);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_toolhead_focus_calibration;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_start_toolhead_focus_calibration)
    void onStartClicked() {
        SettingsActivity activity = (SettingsActivity) requireActivity();
        activity.gotoLaser10wTouchPlatformFragment();
    }
}
