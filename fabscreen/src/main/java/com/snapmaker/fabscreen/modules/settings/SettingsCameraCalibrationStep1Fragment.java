package com.snapmaker.fabscreen.modules.settings;

import android.widget.Button;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SettingsCameraCalibrationStep1Fragment extends BaseFragment {
    public static SettingsCameraCalibrationStep1Fragment newInstance() {
        return new SettingsCameraCalibrationStep1Fragment();
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

    private void ensureHomed() {
        getModel().getMachineController().updateCoordinateSystem(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        next();
                    } else {
                        getModel().getSlaveComputer().sendGcode("G28")
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> checkHome());
                    }
                }, LogHelper::log);
    }

    private void checkHome() {
        getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(coordinateSystem -> {
                    boolean homed = coordinateSystem.homed;
                    if (homed) {
                        next();
                    } else {
                        AndroidSchedulers.mainThread().scheduleDirect(this::checkHome, 2000, Constants.TIME_UNIT);
                    }
                }, LogHelper::log);
    }

    private void next() {
        mBtnNext.setEnabled(true);
        if (getActivity() == null) return;
        ((SettingsActivity) getActivity()).gotoCameraCalibrationStep2();
    }

    @OnClick(R.id.btn_camera_calibration_next)
    void onClickStart() {
        mBtnNext.setEnabled(false);
        ensureHomed();
    }
}
