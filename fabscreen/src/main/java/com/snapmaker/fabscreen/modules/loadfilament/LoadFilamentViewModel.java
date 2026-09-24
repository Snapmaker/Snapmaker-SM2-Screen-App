package com.snapmaker.fabscreen.modules.loadfilament;

import com.orhanobut.logger.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LoadFilamentViewModel extends BaseViewModel {
    private BehaviorSubject<DualExtruderTemperature> mDualExtruderTemperatureSubject = BehaviorSubject.createDefault(new DualExtruderTemperature());
    private BehaviorSubject<Integer> mActiveExtruderSubject = BehaviorSubject.createDefault(0);
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mLeftExtruderReadySubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mRightExtruderReadySubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<DualExtruderTemperature> mTargetValueSubject = BehaviorSubject.createDefault(new DualExtruderTemperature());

    private final static int FILAMENT_MELT_MIN_TEMP = 170;
    private final static int TEMPERATURE_HEATED_TOLERANCE = 3;

    public LoadFilamentViewModel() {
        super();

        bindDualExtruderStatus();
    }

    private void bindDualExtruderStatus() {
        // Subscribe dual extruder temperature.
        getModel().getMachineController()
                .getMachineStatusObservable()
                .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    DualExtruderTemperature temp = new DualExtruderTemperature();
                    temp.setExtruder0Temp(machineStatus.headTemperature);
                    temp.setExtruder0TargetTemp(machineStatus.headTargetTemperature);
                    temp.setExtruder1Temperature(machineStatus.extruder1Temperature);
                    temp.setExtruder1TargetTemperature(machineStatus.extruder1TargetTemperature);
                    mDualExtruderTemperatureSubject.onNext(temp);

                    // Check extruder was ready(to extrude or retract) due to current temperature.
                    checkExtruderReadyToUse(temp);
                });

        mTargetValueSubject
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .as(bindToLifecycle())
                .subscribe(dualTemp -> {
                    DualExtruderTemperature currentTemp = getDualExtruderTemperature();
                    if (currentTemp.getExtruder0TargetTemperature() != dualTemp.getExtruder0TargetTemperature()) {
                        sendTargetTemperature(0, dualTemp.getExtruder0TargetTemperature());
                    }

                    if (currentTemp.getExtruder1TargetTemperature() != dualTemp.getExtruder1TargetTemperature()) {
                        sendTargetTemperature(1, dualTemp.getExtruder1TargetTemperature());
                    }
                });

        updateActiveExtruder();
    }

    public DualExtruderTemperature getDualExtruderTemperature() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        DualExtruderTemperature data = new DualExtruderTemperature();
        data.setExtruder0Temp(status.headTemperature);
        data.setExtruder0TargetTemp(status.headTargetTemperature);
        data.setExtruder1Temperature(status.extruder1Temperature);
        data.setExtruder1TargetTemperature(status.extruder1TargetTemperature);
        return data;
    }

    public int getActiveExtruder() {
        return mActiveExtruderSubject.getValue();
    }

    public Observable<Boolean> getIsMovingObservable() {
        return mIsMovingSubject.hide();
    }

    public void setMoving(boolean isMoving) {
        mIsMovingSubject.onNext(isMoving);
    }

//    public Observable<Boolean> preHeatedHeadTemperature() {
//        return getModel().getSlaveComputer().sendGcode("M104 T1 S200")
//                .flatMap(result -> getModel().getSlaveComputer().sendGcode("M104 T0 S200"))
//                .map(response -> true);
//    }

    public void preHeatedHeadTemperature() {
        DualExtruderTemperature current = getDualExtruderTemperature();
        if (current.getExtruder0TargetTemperature() == 0) {
            current.setExtruder0TargetTemp(200);
        }

        if (current.getExtruder1TargetTemperature() == 0) {
            current.setExtruder1TargetTemperature(200);
        }

        mTargetValueSubject.onNext(current);
    }

    public Observable<Boolean> turnOffHead() {
        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
        boolean isDualExtruder = getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER;

        if (machineStatus.headTargetTemperature > 0) {
            return getModel().getSlaveComputer().sendGcode("M104 S0").flatMap(response -> {
                if (isDualExtruder && machineStatus.extruder1TargetTemperature > 0) {
                    return getModel().getSlaveComputer().sendGcode("M104 T1 S0");
                } else {
                    return Observable.just(response);
                }
            }).map(response -> true);
        } else {
            return Observable.just(true);
        }
    }

    public void sendTargetTemperature(int which, float value) {
        getModel().getSlaveComputer().sendGcode(String.format(Locale.ENGLISH,
                "M104 T%d S%.1f", which, value))
                .as(bindToLifecycle())
                .subscribe(response -> {/*Do nothing*/}, LogHelper::log);
    }

    public Observable<DualExtruderTemperature> getExtrudersTemperatureObservable() {
        return mDualExtruderTemperatureSubject.hide();
    }

    public Observable<Boolean> getExtruderReadyObservable(int which) {
        // We onNext extruder ready (or not) in `checkExtruderReadyToUse`
        // Return observable directly here.
        return which == 0 ? mLeftExtruderReadySubject.hide() : mRightExtruderReadySubject.hide();
    }

    private void checkExtruderReadyToUse(DualExtruderTemperature temperature) {
        float temp0 = temperature.getExtruder0Temperature();
        float temp1 = temperature.getExtruder1Temperature();
        float targetTemp0 = temperature.getExtruder0TargetTemperature();
        float targetTemp1 = temperature.getExtruder1TargetTemperature();

        // T0
        if (targetTemp0 == 0 || mIsMovingSubject.getValue()) {
            mLeftExtruderReadySubject.onNext(false);
        } else {
            boolean isReachTargetTemp = (temp0 > targetTemp0 - TEMPERATURE_HEATED_TOLERANCE)
                    && temp0 > FILAMENT_MELT_MIN_TEMP;
            mLeftExtruderReadySubject.onNext(isReachTargetTemp);
        }

        // T1
        if (targetTemp1 == 0 || mIsMovingSubject.getValue()) {
            mRightExtruderReadySubject.onNext(false);
        } else {
            boolean isReachTargetTemp = (temp1 > targetTemp1 - TEMPERATURE_HEATED_TOLERANCE)
                    && temp1 > FILAMENT_MELT_MIN_TEMP;
            mRightExtruderReadySubject.onNext(isReachTargetTemp);
        }
    }

    public void updateActiveExtruder() {
        getModel().getSlaveComputer().getActivatedExtruder()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(which -> {
                    mActiveExtruderSubject.onNext(which);
                }, LogHelper::log);
    }

    private Observable<Boolean> switchExtruder(int which) {
        return getModel().getSlaveComputer().switchToExtruder(which).doOnNext(result -> updateActiveExtruder());
    }

    public Observable<Boolean> requestExtruderLoad(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestExtruderLoad(which));
        }

        return getModel().getSlaveComputer().extrudeInfinitely(0, 5);
    }

    public Observable<Boolean> requestExtruderUnload(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestExtruderUnload(which));
        }

        return getModel().getSlaveComputer().extrudeInfinitely(1, 5);
    }

    public Observable<Boolean> requestStopExtruderLoad(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestStopExtruderLoad(which));
        }

        return getModel().getSlaveComputer().stopMovement();
    }

    public Observable<Boolean> requestStopExtruderUnload(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestStopExtruderUnload(which));
        }

        return getModel().getSlaveComputer().stopMovement();
    }

    public void setDualExtruderTargetValue(int which, float targetValue) {
        DualExtruderTemperature lastValue = mTargetValueSubject.getValue();
        float lastTemp = 0;
        if (which == 0) {
            lastTemp =  lastValue.getExtruder0Temperature();
            if (targetValue != lastTemp) {
                DualExtruderTemperature newTemp = lastValue;
                newTemp.setExtruder0TargetTemp(targetValue);
                mTargetValueSubject.onNext(newTemp);
            }
        } else {
            lastTemp = lastValue.getExtruder1Temperature();
            if (targetValue != lastTemp) {
                DualExtruderTemperature newTemp = lastValue;
                newTemp.setExtruder1TargetTemperature(targetValue);
                mTargetValueSubject.onNext(newTemp);
            }
        }
    }
}
