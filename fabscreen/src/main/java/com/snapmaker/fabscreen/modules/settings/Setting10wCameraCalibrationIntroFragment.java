package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Setting10wCameraCalibrationIntroFragment extends BaseFragment {

    public static Setting10wCameraCalibrationIntroFragment newInstance() {
        return new Setting10wCameraCalibrationIntroFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.laser_camera_calibration);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_camera_calibration_into;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }


    @OnClick(R.id.btn_start_10w_camera_calibration_into)
    void onClickNext() {
        if (getActivity() == null) return;
        ((SettingsActivity) getActivity()).goto10wCameraCalibrationStep1();
    }

}
