package com.snapmaker.fabscreen.modules.lasercalibration.manualfocus;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibrationCoarseTuningFragment extends BaseFragment {
    public static LaserCalibrationCoarseTuningFragment newInstance() {
        return new LaserCalibrationCoarseTuningFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.btn_laser_calibration_next)
    Button mBtnNext;

    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;

    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_coarse_tuning;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.laser_calibration_set_reference_points);

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(getView(), 0.1f, 1f, 10f);
        mControlPanelPresenter.connect();

        mIsMovingSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mBtnNext.setEnabled(!isMoving);
                    mControlPanelPresenter.setEnabled(!isMoving);
                });

        start();
    }

    private Observable<FabPacketContent.GcodeResponse> turnOnLaser() {
        // We use 1% as safe viable power for 10w laser module, 0.5% as default for 1.6w laser module.
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
                .flatMap(response -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ))
                .flatMap(this::updateCoordinateSystem)
                .map(res -> true);
    }

    private void start() {
        // move to initial position
        mIsMovingSubject.onNext(true);

        gotoInitialPosition()
                .flatMap(this::setAsOrigin)
                .flatMap(res -> turnOnLaser())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    mIsMovingSubject.onNext(false);
                }, e -> {
                    mIsMovingSubject.onNext(false);
                    LogHelper.log(e);
                });
    }

    @Override
    protected void back() {
        turnOffLaser()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> super.back());
    }

    @OnClick(R.id.btn_laser_calibration_next)
    void onClickNext() {
        float bottomZ = getModel().getPreferences().getLaserBottomZ();
        float bottomWorkZ = bottomZ + 10 * Constants.LASER_TEST_PATTERN_Z_DIFF + 1;

        mIsMovingSubject.onNext(true);
        getModel().getSlaveComputer().requestMachineStatus()
                .flatMap(machineStatus -> {
                    final float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();
                    float z = (float) machineStatus.z - offsetZ;
                    if (z < bottomWorkZ) {
                        return getModel().getSlaveComputer().gotoRelativePosition(0, 0, bottomWorkZ - z);
                    } else {
                        return Observable.just(true);
                    }
                })
                .flatMap(res -> turnOffLaser())
                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ))
                .flatMap(this::updateCoordinateSystem)
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mIsMovingSubject.onNext(false);

                    LaserCalibrationActivity activity = (LaserCalibrationActivity) getActivity();
                    if (activity != null) {
                        activity.gotoLaserCalibrationSetOriginFragment();
                    }
                }, e -> {
                    mIsMovingSubject.onNext(false);
                    LogHelper.log(e);
                });
    }
}
