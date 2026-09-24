package com.snapmaker.fabscreen.modules.lasercalibration.common;

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
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import fabscreen.libraries.legacy.view.RulerView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibrationSetMaterialThicknessFragment extends BaseFragment {
    public static LaserCalibrationSetMaterialThicknessFragment newInstance() {
        return new LaserCalibrationSetMaterialThicknessFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.rv_laser_calibration_material_thickness_ruler)
    RulerView mRvMaterialThickness;

    @BindView(R.id.tv_laser_calibration_material_offset_value)
    TextView mTvMaterialThickness;

    @BindView(R.id.btn_laser_calibration_next)
    Button mBtnNext;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Float> mThicknessSubject = BehaviorSubject.createDefault(0f);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_set_material_thickness;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        int calibrationMode = getModel().getPreferences().getLaserCalibrationMode();
        if (calibrationMode == 0) {
            setTitle(R.string.laser_calibration_auto_focus);
        } else {
            setTitle(R.string.laser_calibration_manual_focus);
        }
        Logger.d("Enter %s laser focus.", calibrationMode == 0 ? "auto" : "manual");

        // Initial value
        final float thickness0 = getModel().getPreferences().getLaserMaterialThickness();
        mThicknessSubject.onNext(thickness0);
        mRvMaterialThickness.setCurrentValue(thickness0);

        // On value changes
        mRvMaterialThickness.setOnValueChangedListener(value -> {
            final float newValue = (int) (value * 10) / 10f;
            if (newValue != mThicknessSubject.getValue()) {
                mThicknessSubject.onNext(newValue);
            }
        });

        // Display
        mThicknessSubject
                .as(bindToLifecycle())
                .subscribe(thickness ->
                        mTvMaterialThickness.setText(String.format(Locale.getDefault(), getString(R.string.all_format_float), thickness)));

        // On moving event
        mIsMovingSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setVisibility(isMoving ? View.INVISIBLE : View.VISIBLE);
                    mBtnNext.setEnabled(!isMoving);
                    mRvMaterialThickness.setEnabled(!isMoving);
                });

        // CS#0, where offset x, y, z = 0
        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(0);
    }

    private void finish() {
        mIsMovingSubject.onNext(false);

        if (getActivity() != null) {
            ((LaserCalibrationActivity) getActivity()).gotoMeasureHeightFragment();
        }
    }

    @OnClick(R.id.btn_laser_calibration_next)
    void onClickNext() {
        final float thickness = mThicknessSubject.getValue();
        Logger.d("Set material %.1f mm.", thickness);
        getModel().getPreferences().setLaserMaterialThickness(thickness);

        mIsMovingSubject.onNext(true);

        int sizeX = getModel().getMachineController().getSizeX();
        int sizeY = getModel().getMachineController().getSizeY();

        float z = MockConst.LASER_PLATE_SAFETY_HEIGHT + thickness + MockConst.LASER_HOOD_HEIGHT + 10;

        // Make sure on CS#0 and then go to center for measuring height.
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f, sizeY / 2f)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    finish();
                }, e -> {
                    LogHelper.log(e);
                    Logger.e("Failed to move.");
                });
    }
}
