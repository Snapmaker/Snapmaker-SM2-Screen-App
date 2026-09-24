package com.snapmaker.fabscreen.modules.laser10wcamera;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class Laser10wLaserCameraCalibrationIntroFragment extends BaseFragment {
    public static Laser10wLaserCameraCalibrationIntroFragment newInstance() {
        return new Laser10wLaserCameraCalibrationIntroFragment();
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


    @OnClick(R.id.btn_start_10w_camera_calibration_into)
    void onClickNext() {
        if (getActivity() == null) return;
        ((Laser10wCameraCalibrationActivity) getActivity()).gotoCameraCalibrationStep1();
    }

}
