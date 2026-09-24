package com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor;


import com.orhanobut.logger.Logger;

import java.util.Locale;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderSensorCalibrationFineTuneViewModel extends BaseViewModel {

    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private float mStep = 0.1f;

    public DualExtruderSensorCalibrationFineTuneViewModel() {

    }

    public Observable<Boolean> getIsMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public float getCurrentStep() {
        return mStep;
    }

    public void setCurrentStep(float step) {
        mStep = step;
    }


    public void requestExtruderMove(int direction, float value) {
        mIsMovingSubject.onNext(true);
        getModel().getSlaveComputer().sendGcode("G91")
                .flatMap(response -> getModel().getSlaveComputer().sendGcode(String.format(Locale.ENGLISH, "G0 Z%.2f F1800", (direction == 0) ? value : -value)))
                .flatMap(g0Response -> getModel().getSlaveComputer().sendGcode("G90"))
                .as(bindToLifecycle())
                .subscribe(g90Response -> {
                    mIsMovingSubject.onNext(false);
                }, LogHelper::log);
    }
}
