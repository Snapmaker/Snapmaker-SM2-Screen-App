package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderSensorCalibrationViewModel extends BaseViewModel {
    private final BehaviorSubject<Process> mProcessSubj = BehaviorSubject.createDefault(Process.CALIBRATING);

    public DualExtruderSensorCalibrationViewModel() {
        mProcessSubj.distinctUntilChanged()
                .as(bindToLifecycle())
                .subscribe(process -> {
                    switch (process) {
                        case CALIBRATING:
                            Logger.d("Start sensor calibrating");
                            break;
                        case LEFT_PROBE_COMPLETE:
                            Logger.d("Left sensor probed.");
                            break;
                        case RIGHT_PROBE_COMPLETE:
                            Logger.d("Right sensor probed.");
                            break;
                        case LEFT_FINE_TUNE_COMPLETE:
                            Logger.d("Left extruder fine tune completed.");
                            break;
                        case RIGHT_FINE_TUNE_COMPLETE:
                            Logger.d("Right extruder fine tune completed.");
                            break;
                        case COMPLETE:
                            Logger.d("Sensor Calibration completed.");
                            break;
                        case FAIL:
                            Logger.d("Sensor Calibration failed.");
                            break;
                        default:
                            break;
                    }
                });
    }

    public Observable<Process> getProcessObservable() {
        return mProcessSubj.hide();
    }

    public void startSensorCalibration() {
        // left probe -> right probe
        // mainboard will do the homing
        getModel().getSlaveComputer().calibrateSensorTouchBed(0, true)
                .doOnNext(success -> mProcessSubj.onNext(success ? Process.LEFT_PROBE_COMPLETE : Process.FAIL))
                .concatMap(success -> success ? getModel().getSlaveComputer().calibrateSensorTouchBed(1, true) : Observable.just(false))
                .as(bindToLifecycle())
                .subscribe(success -> mProcessSubj.onNext(success ? Process.RIGHT_PROBE_COMPLETE : Process.FAIL), LogHelper::log);
    }

    public Observable<Boolean> saveSensorFineTuneResult(int which) {
        return getModel().getSlaveComputer().calibrateSensorManualConfirm(which)
                .doOnNext(success -> mProcessSubj.onNext(success ? (which == 0 ? Process.LEFT_FINE_TUNE_COMPLETE : Process.RIGHT_FINE_TUNE_COMPLETE) : Process.FAIL));
    }

    public Observable<Boolean> startSensorLeftFineTune() {
        return getModel().getSlaveComputer().calibrateSensorTouchBed(0, false)
                .doOnNext(success -> mProcessSubj.onNext(success ? Process.RIGHT_FINE_TUNE_COMPLETE : Process.FAIL));
    }

    public Observable<Boolean> exitSensorCalibration() {
        return getModel().getSlaveComputer().calibrationSensorRequestAbort()
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .flatMap(response -> Observable.just(true));
    }

    public Observable<Boolean> requestGoHome() {
        return getModel().getSlaveComputer().sendGcode("G28").flatMap(response -> Observable.just(true));
    }

    public enum Process {
        CALIBRATING,
        LEFT_PROBE_COMPLETE,
        RIGHT_PROBE_COMPLETE,
        LEFT_FINE_TUNE_COMPLETE,
        RIGHT_FINE_TUNE_COMPLETE,
        COMPLETE,
        FAIL
    }
}
