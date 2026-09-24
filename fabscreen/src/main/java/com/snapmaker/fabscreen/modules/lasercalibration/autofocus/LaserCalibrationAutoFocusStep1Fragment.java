package com.snapmaker.fabscreen.modules.lasercalibration.autofocus;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LaserCalibrationAutoFocusStep1Fragment extends BaseFragment {
    public static LaserCalibrationAutoFocusStep1Fragment newInstance() {
        return new LaserCalibrationAutoFocusStep1Fragment();
    }

    @BindView(R.id.btn_guide_next)
    Button mBtnNext;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_laser_auto_focus_step1;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnNext.setText(R.string.all_start);
    }

    private Observable<Boolean> gotoInitialPosition() {
        // Assume we are at CS#1
        return getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000")
                .flatMap(response -> getModel().getSlaveComputer().sendGcode("G0 Z0 F1800"))
                .map(coordinateSystem -> true);
    }

    @OnClick(R.id.btn_guide_next)
    void onClickNext() {
        mBtnNext.setEnabled(false);

        gotoInitialPosition()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mBtnNext.setEnabled(true);

                    if (getActivity() != null) {
                        ((LaserCalibrationActivity) getActivity()).startAutoFocusStep2Fragment();
                    }
                }, e -> {
                    mBtnNext.setEnabled(true);
                    LogHelper.log(e);
                });
    }
}
