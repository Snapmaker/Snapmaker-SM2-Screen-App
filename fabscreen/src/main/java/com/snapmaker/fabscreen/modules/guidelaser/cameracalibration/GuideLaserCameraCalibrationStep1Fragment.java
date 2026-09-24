package com.snapmaker.fabscreen.modules.guidelaser.cameracalibration;

import android.widget.Button;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GuideLaserCameraCalibrationStep1Fragment extends BaseFragment {
    public static GuideLaserCameraCalibrationStep1Fragment newInstance() {
        return new GuideLaserCameraCalibrationStep1Fragment();
    }

    @BindView(R.id.btn_camera_calibration_next)
    Button mBtnNext;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_camera_calibration_step1;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_camera_calibration_next)
    void onClickNext() {
        mBtnNext.setEnabled(false);
        getModel().getMachineController().updateCoordinateSystem(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    mBtnNext.setEnabled(true);
                    if (getActivity() != null) {
                        ((GuideLaserActivity) getActivity()).startCameraCalibrationStep2Fragment();
                    }
                }, LogHelper::log);
    }

    @Override
    protected void back() {
        getModel().getMachineController().updateCoordinateSystem(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> super.back());
    }
}
