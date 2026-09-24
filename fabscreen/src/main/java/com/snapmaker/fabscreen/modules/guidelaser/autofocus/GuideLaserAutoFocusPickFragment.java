package com.snapmaker.fabscreen.modules.guidelaser.autofocus;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.RulerView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class GuideLaserAutoFocusPickFragment extends BaseFragment {
    public static GuideLaserAutoFocusPickFragment newInstance() {
        return new GuideLaserAutoFocusPickFragment();
    }

    private BehaviorSubject<Float> mZOffsetValueSubject = BehaviorSubject.createDefault(0f);

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_calibration_save)
    Button mBtnLaserCalibrationSave;

    @BindView(R.id.tv_laser_calibration_pick_offset)
    TextView mTvFineTuneValue;

    @BindView(R.id.rv_laser_calibration_pick_ruler)
    RulerView mRvFineTuneRuler;

    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_fine_tune_pick;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.laser_calibration_auto_focus);

        // Display
        mZOffsetValueSubject
                .as(bindToLifecycle())
                .subscribe(value -> {
                    mTvFineTuneValue.setText(String.format(Locale.getDefault(), "%.1f", value));
                });

        // On value changes
        mRvFineTuneRuler.setOnValueChangedListener(value -> {
            // final float newValue = ((int) (value * 10) / 10f - 10) * mZStep;
            if (mZOffsetValueSubject.getValue() != value) {
                mZOffsetValueSubject.onNext(value);
            }
        });

        mIsMovingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mBtnLaserCalibrationSave.setEnabled(!isMoving);
                });
    }

    private void finish() {
        mIsMovingSubject.onNext(false);
        if (getActivity() != null) {
            ((GuideLaserActivity) getActivity()).startCameraCalibrationIntroFragment();
        }
    }

    @OnClick(R.id.btn_laser_calibration_save)
    void onClickSave() {
        final float initialZ = -getModel().getMachineController().getCoordinateOffsetZ();
        float thickness = getModel().getPreferences().getLaserMaterialThickness();

        float offset = mZOffsetValueSubject.getValue();
        float focalLength = initialZ + offset - thickness;
        Logger.i("mFocalLength is " + focalLength);

        mIsMovingSubject.onNext(true);
        getModel().getMachineController()
                .setLaserFocus(focalLength)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000")
                                .flatMap(res -> getModel().getSlaveComputer().sendGcode("G0 Z" + offset + " F1800"))
                                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z))
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(response -> finish());
                    } else {
                        FabAlert.alert(getContext(), R.string.laser_alert_failed_to_get_focal_length);
                        finish();
                    }
                }, e -> {
                    LogHelper.log(e);
                    FabAlert.alert(getContext(), R.string.laser_alert_failed_to_get_focal_length);
                    finish();
                });
    }
}
