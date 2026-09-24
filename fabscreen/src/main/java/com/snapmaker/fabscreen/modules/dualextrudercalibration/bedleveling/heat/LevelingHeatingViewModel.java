package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.heat;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class LevelingHeatingViewModel extends BaseViewModel {
    private static final int DEFAULT_BED_TEMP = 60;
    private final BehaviorSubject<BedTemperature> mTemperatureSubj = BehaviorSubject.create();
    private final PublishSubject<Integer> mUserSetTempSubj = PublishSubject.create();

    public LevelingHeatingViewModel() {
        subscribeBedTemp();
        processUserInputTemp();
    }

    private void processUserInputTemp() {
        mUserSetTempSubj.hide()
                .debounce(100, TimeUnit.MILLISECONDS)
                .as(bindToLifecycle())
                .subscribe(this::setBedTemperature, LogHelper::log);
    }

    private void subscribeBedTemp() {
        getModel().getMachineController().getMachineStatusObservable()
                .as(bindToLifecycle())
                .subscribe(status -> mTemperatureSubj.onNext(new BedTemperature(status.bedTemperature, status.bedTargetTemperature)), LogHelper::log);
    }

    public Observable<BedTemperature> getTempObservable() {
        return mTemperatureSubj.hide();
    }

    public void startHeatingBed() {
        setBedTemperature(DEFAULT_BED_TEMP);
    }

    public void setBedTemperature(int temperature) {
        Logger.d("Set heated bed temperature " + temperature);
        getModel().getSlaveComputer().sendGcode("M140 S" + temperature)
                .as(bindToLifecycle())
                .subscribe(result -> {
                }, LogHelper::log);
    }

    /**
     * User set target temp via view. Need to be debounced and set to machine.
     */
    public void onUserSetTemperature(float value) {
        mUserSetTempSubj.onNext((int) value);
    }

    public static class BedTemperature {
        int current;
        int target;

        public BedTemperature(int current, int target) {
            this.current = current;
            this.target = target;
        }
    }
}
