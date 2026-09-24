package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class Guide3DPDualExtruderAutoZHeightViewModel extends BaseViewModel {
    private final BehaviorSubject<Process> mProcessSubj = BehaviorSubject.createDefault(Process.CALIBRATING);

    public Guide3DPDualExtruderAutoZHeightViewModel() {
        startCalibration();
    }

    private void startCalibration() {
        // left probe -> right probe
        // mainboard will do the homing
        getModel().getSlaveComputer().probeBedPosition(0, true)
                .doOnNext(success -> Logger.d("Left probed."))
                .concatMap(success -> success ? getModel().getSlaveComputer().probeBedPosition(1, true) : Observable.just(false))
                .doOnNext(disposable -> Logger.d("Right probed."))
                .concatMap(success -> success ? getModel().getSlaveComputer().sendGcode("G28").map(response -> true) : Observable.just(false))
                .doOnNext(success -> getModel().getPreferences().setNeedDoZHeightCalibration(false))
                .as(bindToLifecycle())
                .subscribe(success -> mProcessSubj.onNext(success ? Process.COMPLETE : Process.FAIL), LogHelper::log);
    }

    public Observable<Process> getProcessObservable() {
        return mProcessSubj.hide();
    }

    enum Process {
        CALIBRATING,
        COMPLETE,
        FAIL
    }
}
