package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.FIRST_CAPTURE;

@SuppressLint("NonConstantResourceId")
public class SettingsLaser10wThicknessMeasureCalibrationIntroFragment extends BaseFragment {

    private Laser10wThicknessCalibrationViewModel mViewModel;
    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_10w_thickness_measure_calibration_start)
    Button mBtnStart;

    public static Fragment getInstance() {
        return new SettingsLaser10wThicknessMeasureCalibrationIntroFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_laser_thickness_measure_calibration_title);
        mViewModel = getViewModel();
        // We can only go ahead after finishing switching coordinate.
        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        coordinateSystemPresenter.setOnCoordinateSwitchListener(this::prepareHead);
        coordinateSystemPresenter.ensureCoordinate(1);
        setButtonsEnabled(false);
    }

    /**
     * Set proper exposeTime(now we set 1) and init camera position for distance measuring.
     * Laser point won't be on the material surface if we don't init the camera position.
     */
    private void prepareHead() {
        mViewModel.initCameraPosition(FIRST_CAPTURE)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        setButtonsEnabled(true);
                    } else {
                        // If fail, we can't start but should have ability to back.
                        mBtnBack.setEnabled(true);
                    }
                }, LogHelper::log);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_thickness_measure_calibration;
    }

    @Override
    protected Laser10wThicknessCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Laser10wThicknessCalibrationViewModel.class);
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btn_laser_10w_thickness_measure_calibration_start)
    void onStartClicked() {
        getModel().getPrintController().getHeaderSecurityStatus().observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(headerSecurity -> {
                    if (headerSecurity.status == 0) {
                        mViewModel.switchAFAssistLight(true);
                        Laser10wThicknessMeasureCalibrationActivity activity = (Laser10wThicknessMeasureCalibrationActivity) requireActivity();
                        activity.gotoLaser10wThicknessMeasureCalibrationPointsFragment();
                    }
                });

    }

    private void setButtonsEnabled(boolean enabled) {
        mBtnBack.setEnabled(enabled);
        mBtnStart.setEnabled(enabled);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            prepareHead();
        }
    }
}
