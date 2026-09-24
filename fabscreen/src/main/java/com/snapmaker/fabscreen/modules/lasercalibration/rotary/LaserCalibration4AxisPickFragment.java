package com.snapmaker.fabscreen.modules.lasercalibration.rotary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.VerticalRulerView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibration4AxisPickFragment extends BaseFragment {
    public static LaserCalibration4AxisPickFragment newInstance() {
        return new LaserCalibration4AxisPickFragment();
    }

    private BehaviorSubject<Float> mZOffsetValueSubject = BehaviorSubject.createDefault(0f);

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.btn_laser_calibration_save)
    Button mBtnLaserCalibrationSave;

    @BindView(R.id.tv_laser_calibration_pick_offset)
    TextView mTvFineTuneValue;

    @BindView(R.id.vrv_laser_calibration_pick_ruler)
    VerticalRulerView mRvFineTuneRuler;

    private LaserCalibrationViewModel mViewModel;
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_4axis_fine_tune_pick;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    private void initView() {
        int calibrationMode = getModel().getPreferences().getLaserCalibrationMode();
        if (calibrationMode == 0) {
            setTitle(R.string.laser_calibration_auto_focus);
        } else {
            setTitle(R.string.laser_calibration_manual_focus);
        }
        mBtnBack.setVisibility(Button.INVISIBLE);

        // Display
        mZOffsetValueSubject
                .as(bindToLifecycle())
                .subscribe(value -> {
                    mTvFineTuneValue.setText(String.format(Locale.getDefault(), "%.1f", value));
                });

        // TODO: change RulerView!
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
                    mBtnLaserCalibrationSave.setEnabled(!isMoving);
                    mBtnBack.setEnabled(!isMoving);
                });
    }

    private void finish() {
        mIsMovingSubject.onNext(false);
        Logger.d("Laser Calibration finished.");
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @OnClick(R.id.btn_laser_calibration_save)
    void onClickSave() {
        final float initialZ = -getModel().getMachineController().getCoordinateOffsetZ();

        float offset = mZOffsetValueSubject.getValue();
        float focalLength = initialZ + offset - mViewModel.getWorkpieceDiameter() / 2f;
        Logger.d("mFocalLength is " + focalLength);

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
                                .subscribe(response -> {
                                    finish();
                                });
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
