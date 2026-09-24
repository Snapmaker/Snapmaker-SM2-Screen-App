package com.snapmaker.fabscreen.modules.lasercalibration.common;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SetOriginPagerPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.parser.Position;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibrationSetOriginFragment extends BaseFragment {
    public static LaserCalibrationSetOriginFragment newInstance() {
        return new LaserCalibrationSetOriginFragment();
    }

    @BindView(R.id.btn_preview_laser_prepare_set_origin_next)
    Button mBtnNext;

    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private SetOriginPagerPresenter mSetOriginPagerPresenter;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_set_origin;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.control_set_origin);
        mBtnNext.setText(R.string.all_next);

        mCoordinateXYZWidgetPresenter = new CoordinateXYZGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZWidgetPresenter.bind(getView());
        mCoordinateXYZWidgetPresenter.connect();

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(getView(), 0.1f, 1f, 10f);
        mControlPanelPresenter.connect();
        mControlPanelPresenter.disabledZ();

        mSetOriginPagerPresenter = new SetOriginPagerPresenter(getContext(), getModel());
        mSetOriginPagerPresenter.bindView(getLifecycle(), getView());
        ModelBoundary boundary = new ModelBoundary();
        boundary.updateBoundary(new Position(-20, -5f, 0));
        boundary.updateBoundary(new Position(-20, 5f, 0));
        boundary.updateBoundary(new Position(20, -5f, 0));
        boundary.updateBoundary(new Position(20, 5f, 0));
        mSetOriginPagerPresenter.setBoundary(boundary);

        mControlPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });
        mSetOriginPagerPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mBtnNext.setEnabled(!movingEvent);
                    mControlPanelPresenter.setEnabled(!movingEvent);
                    mSetOriginPagerPresenter.setEnabled(!movingEvent);
                });

        start();
    }

    private Observable<FabPacketContent.GcodeResponse> turnOnLaser() {
        // We use 1% as safe viable power for 10w laser module, 0.5% as default for 1.6w laser module.
        int headType = getModel().getMachineController().getHeadType();
        float laserSafePowerPercent = getModel().getMachineController().getLaserOutputPower();
        return getModel().getSlaveComputer().sendGcode("M3 P " + laserSafePowerPercent);
    }

    private Observable<FabPacketContent.GcodeResponse> turnOffLaser() {
        return getModel().getSlaveComputer().sendGcode("M5");
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    private float calculateWorkingZ(float laserFocus) {
        float bottomZ = getModel().getPreferences().getLaserBottomZ();
        float thickness = getModel().getPreferences().getLaserMaterialThickness();

        Logger.d("Get laser focus ", laserFocus);

        return Math.max(laserFocus + thickness, bottomZ + 10 * Constants.LASER_TEST_PATTERN_Z_DIFF + 1);
    }

    private Observable<Boolean> gotoInitialPosition() {
        float laserFocus = getModel().getMachineController().getLaserFocus();
        final float z = calculateWorkingZ(laserFocus);

        // Make sure we are at G53 before movement
        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .map(res -> true);
    }

    private Observable<Boolean> setAsOrigin(boolean success) {
        // Switch to CS#1 and then mark current position as origin
        return getModel().getMachineController().updateCoordinateSystem(1)
                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ))
                .flatMap(this::updateCoordinateSystem)
                .map(res -> true);
    }

    private void start() {
        int calibrationMode = getModel().getPreferences().getLaserCalibrationMode();
        if (calibrationMode == 0) {
            mMovingEventSubject.onNext(true);
            gotoInitialPosition()
                    .flatMap(this::setAsOrigin)
                    .flatMap(res -> turnOnLaser())
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        Logger.e("Failed to prepare for set origin.");
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });

            turnOnLight();
        } else {
            // Manual mode we don't move it anymore
            mMovingEventSubject.onNext(true);
            turnOnLaser()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        Logger.e("Failed to prepare for set origin.");
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
        }
    }

    private void turnOnLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    private void turnOffLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_set_origin_next)
    void onClickNext() {
        mMovingEventSubject.onNext(true);

        turnOffLaser()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    if (getActivity() != null) {
                        int calibrationMode = getModel().getPreferences().getLaserCalibrationMode();
                        if (calibrationMode == 0) {
                            ((LaserCalibrationActivity) getActivity()).startAutoFocusStep1Fragment();
                        } else {
                            ((LaserCalibrationActivity) getActivity()).startManualFocusStep1Fragment();
                        }
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }

    @Override
    protected void back() {
        turnOffLight();

        turnOffLaser()
                .flatMap(res -> getModel().getMachineController().updateCoordinateSystem(0))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> super.back(), LogHelper::log);
    }
}
