package com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight;

import com.orhanobut.logger.Logger;

import java.util.Locale;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class ManualZHeightCalibrationViewModel extends BaseViewModel {
    private final BehaviorSubject<Process> mProcessSubj = BehaviorSubject.createDefault(Process.MOVING);

    public ManualZHeightCalibrationViewModel() {

    }

    public void startManualZCalibration(int extruder) {
        getModel().getSlaveComputer().probeBedPosition(extruder, false)
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Logger.d("Manual probe %s z position %b", extruder == 0 ? "left" : "right", success);
                    mProcessSubj.onNext(success ? Process.COMPLETE : Process.FAIL);
                }, e -> {
                    LogHelper.log(e);
                    mProcessSubj.onNext(Process.FAIL);
                });
    }

    public Observable<Boolean> saveManualZCalibration() {
        mProcessSubj.onNext(Process.MOVING);
        return getModel().getSlaveComputer().saveBedPosition()
                .doOnNext(success -> mProcessSubj.onNext(success ? Process.COMPLETE : Process.FAIL));
    }

    public void moveZAxisByStep(float moveStep) {
        mProcessSubj.onNext(Process.MOVING);
        String moveGcode = String.format(Locale.ENGLISH, "G0 Z%.2f F1800", moveStep);
        getModel().getSlaveComputer().sendGcode("G91")
                .flatMap(success -> getModel().getSlaveComputer().sendGcode(moveGcode))
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("G90"))
                .as(bindToLifecycle())
                .subscribe(response -> {
                    mProcessSubj.onNext(Process.COMPLETE);
                }, e -> {
                    LogHelper.log(e);
                    mProcessSubj.onNext(Process.FAIL);
                });
    }

    public Observable<Process> getProcessObservable() {
        return mProcessSubj.hide();
    }

    enum Process {
        MOVING,
        COMPLETE,
        FAIL
    }

}
