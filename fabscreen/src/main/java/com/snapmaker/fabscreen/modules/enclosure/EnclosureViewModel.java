package com.snapmaker.fabscreen.modules.enclosure;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class EnclosureViewModel extends BaseViewModel {

    private BehaviorSubject<FabPacketContent.EnclosureStatus> mEnclosureStatusSubject = BehaviorSubject.create();

    public EnclosureViewModel() {
        super();

        // update status
        updateEnclosureStatus();
    }

    private void updateEnclosureStatus() {
        getModel().getMachineController()
                .updateEnclosureStatus()
                .as(bindToLifecycle())
                .subscribe(enclosureStatus -> {
                    mEnclosureStatusSubject.onNext(enclosureStatus);
                }, LogHelper::log);
    }

    Observable<FabPacketContent.EnclosureStatus> getEnclosureStatusObservable() {
        return mEnclosureStatusSubject.hide();
    }

    boolean isDoorDetectionEnabled() {
        return getModel().getMachineController().isEnclosureDoorDetectionEnabled();
    }

    boolean isEnclosureLedOn() {
        return getModel().getMachineController().isEnclosureLedOn();
    }

    boolean isEnclosureFanOn() {
        return getModel().getMachineController().isEnclosureFanOn();
    }

    void setLedLevel(int value) {
        getModel().getSlaveComputer()
                .setEnclosureLed(value)
                .doOnNext(success -> updateEnclosureStatus())
                .as(bindToLifecycle())
                .subscribe(ret -> {/**/}, LogHelper::log);
    }

    void setFanLevel(int value) {
        getModel().getSlaveComputer()
                .setEnclosureFan(value)
                .doOnNext(success -> updateEnclosureStatus())
                .as(bindToLifecycle())
                .subscribe(ret -> {/**/}, LogHelper::log);
    }

    void setDoorDetection(boolean enabled) {
        getModel().getSlaveComputer()
                .setEnclosureDoorDetection(enabled)
                .doOnNext(success -> updateEnclosureStatus())
                .as(bindToLifecycle())
                .subscribe(ret -> {/**/}, LogHelper::log);
    }

    void setEnclosureAutoLighting(boolean enabled) {
        getModel().getPreferences().setEnclosureAutoLightingOn(enabled);
    }

    boolean isEnclosureAutoLighting() {
        return getModel().getPreferences().getEnclosureAutoLightingOn();
    }
}
