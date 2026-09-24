package com.snapmaker.fabscreen.modules.experiment;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.ISlaveComputer;

public class ExperimentDualExtruderAPIViewModel extends BaseViewModel {
    private static final String TAG = "ExperimentDualExtruder";
    private final List<String> mApis = new ArrayList<>();
    private final ISlaveComputer mSc;

    public ExperimentDualExtruderAPIViewModel() {
        mSc = getModel().getSlaveComputer();
    }

    public void testAPI(int position) {
        Logger.d("Testing %s", mApis.get(position));
        switch (position) {
            case 0:
                mSc.getExtruderModel().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 1:
                mSc.getExtrudersHaveFilament().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 2:
                mSc.getExtruderTemperatures().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 3:
                mSc.setWorkSpeed(new Random().nextInt(2), new Random().nextFloat()).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 4:
                mSc.setExtruderTemperature(new Random().nextInt(2), 100).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 5:
                mSc.setLiveZOffset(new Random().nextInt(2), 5.5f).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 6:
                mSc.setFlowRate(new Random().nextInt(2), 50f).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 7:
                mSc.getWorkSpeed(new Random().nextInt(2)).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 8:
                mSc.getLiveZOffset(new Random().nextInt(2)).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 9:
                mSc.switchToExtruder(0).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 10:
                mSc.switchToExtruder(1).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 11:
                mSc.getExtruderOffset().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 12:
                // no need
                mSc.confirmExtruderPosition(new Random().nextInt(2)).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 13:
                mSc.setExtruderOffset(0, 26.2f).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 14:
                mSc.setExtruderOffset(1, 1.0f).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 15:
                mSc.setExtruderOffset(2, -1.1f).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 16:
                mSc.setToolheadFanSpeed(0, 100).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 17:
                mSc.setToolheadFanSpeed(1, 100).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 18:
                // cause machine status not push
                mSc.calibrateSensorTouchBed(0, true).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 19:
                mSc.calibrateSensorTouchBed(1, true).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 20:
                mSc.calibrateSensorTouchBed(0, false).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 21:
                mSc.calibrateSensorTouchBed(1, false).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 22:
                mSc.calibrateSensorManualConfirm(new Random().nextInt(2)).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 23:
                // cause machine status not push
                mSc.probeBedPosition(0, true).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 24:
                mSc.probeBedPosition(1, true).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 25:
                mSc.probeBedPosition(0, false).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 26:
                mSc.probeBedPosition(1, false).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 27:
                mSc.saveBedPosition().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 28:
                mSc.startDualExtruderAutoLeveling(11).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 29:
                mSc.dualExtruderAutoLevelPoint(1).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 30:
                mSc.dualExtruderAutoLevelPoint(30).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 31:
                mSc.dualExtruderAutoLevelPoint(121).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 32:
                mSc.finishDualExtruderAutoLeveling().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 33:
                mSc.startDualExtruderManualLeveling(5).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 34:
                mSc.dualExtruderManualLevelPoint(1).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 35:
                mSc.dualExtruderManualLevelPoint(10).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 36:
                mSc.dualExtruderManualLevelPoint(25).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 37:
                mSc.finishDualExtruderManualLeveling().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 38:
                mSc.getActivatedExtruder().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 39:
                mSc.extrudeInfinitely(0, 5).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 40:
                mSc.extrudeInfinitely(1, 5).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 41:
                mSc.stopMovement().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 42:
                // speed 100 not work, speed 255 work, hardware issue.
                mSc.setToolheadFanSpeed(2, 255).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 43:
                mSc.extrudeInfinitely(0, 10).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 44:
                mSc.extrudeInfinitely(1, 10).as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
            case 45:
                mSc.calibrationSensorRequestAbort().as(bindToLifecycle()).subscribe(result -> Logger.t(TAG).d(result));
                break;
        }
    }

    public List<String> getAPIs() {
        if (mApis.size() != 0) return mApis;
        mApis.add("getExtruderModel");
        mApis.add("getExtrudersHaveFilament");
        mApis.add("getExtruderTemperatures");
        mApis.add("setWorkSpeed");
        mApis.add("setExtruderTemperature");
        mApis.add("setLiveZOffset");
        mApis.add("setFeedRate");
        mApis.add("getWorkSpeed");
        mApis.add("getLiveZOffset");
        mApis.add("switchToExtruderL");
        mApis.add("switchToExtruderR");
        mApis.add("getExtruderOffset");
        mApis.add("confirmExtruderPosition");
        mApis.add("setExtruderOffset");
        mApis.add("setExtruderOffset");
        mApis.add("setExtruderOffset");
        mApis.add("setToolheadFanSpeed");
        mApis.add("setToolheadFanSpeed");
        mApis.add("calibrateSensorTouchBed");
        mApis.add("calibrateSensorTouchBed");
        mApis.add("calibrateSensorTouchBed");
        mApis.add("calibrateSensorTouchBed");
        mApis.add("calibrateSensorManualConfirm");
        mApis.add("probeBedPosition");
        mApis.add("probeBedPosition");
        mApis.add("probeBedPosition");
        mApis.add("probeBedPosition");
        mApis.add("saveBedPosition");
        mApis.add("startDualExtruderAutoLeveling");
        mApis.add("dualExtruderAutoLevelPoint");
        mApis.add("dualExtruderAutoLevelPoint");
        mApis.add("dualExtruderAutoLevelPoint");
        mApis.add("stopDualExtruderAutoLeveling");
        mApis.add("startDualExtruderManualLeveling");
        mApis.add("dualExtruderManualLevelPoint");
        mApis.add("dualExtruderManualLevelPoint");
        mApis.add("dualExtruderManualLevelPoint");
        mApis.add("stopDualExtruderManualLeveling");
        mApis.add("getActivatedExtruder");
        mApis.add("extrudeInfinitely");
        mApis.add("extrudeInfinitely");
        mApis.add("stopMovement");
        mApis.add("setToolheadFanSpeed");
        mApis.add("extrudeInfinitely-10");
        mApis.add("extrudeInfinitely-10");
        mApis.add("calibrationSensorRequestAbort");
        return mApis;
    }
}
