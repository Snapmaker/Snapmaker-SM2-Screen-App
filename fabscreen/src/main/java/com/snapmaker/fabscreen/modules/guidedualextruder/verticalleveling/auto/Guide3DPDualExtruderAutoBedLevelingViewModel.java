package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class Guide3DPDualExtruderAutoBedLevelingViewModel extends BaseViewModel {
    private int mGrid;
    // 1~255:point; -1 complete.
    private final BehaviorSubject<Integer> mProgressSubj = BehaviorSubject.createDefault(1);
    private final BehaviorSubject<Integer> mResultSubject = BehaviorSubject.createDefault(0);

    public void startLeveling(int grid) {
        mGrid = grid;
        Logger.d("Start auto leveling...");
        getModel().getSlaveComputer().startDualExtruderAutoLeveling(grid)
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Logger.d("auto leveling returns " + success);
                    if (success) {
                        levelingPoint(1);
                    }
                }, LogHelper::log);
    }

    private void levelingPoint(int point) {
        Logger.d("start leveling point %d ...", point);
        getModel().getSlaveComputer().dualExtruderAutoLevelPoint(point)
                .as(bindToLifecycle())
                .subscribe(result -> {
                    if (result == 0) {
                        mProgressSubj.onNext(point);
                        if (point + 1 <= mGrid * mGrid) {
                            levelingPoint(point + 1);
                        } else {
                            resetPosition();
                        }
                    } else {
                        mResultSubject.onNext(result);
                    }
                });
    }

    public Observable<Integer> getCalibrationResultObservable() {
        return mResultSubject.hide();
    }

    private void resetPosition() {
        Logger.d("finishing auto leveling...");
        getModel().getSlaveComputer().finishDualExtruderAutoLeveling()
                /*.concatMap(success -> getModel().getSlaveComputer().stopDualExtruderAutoLeveling())*/
                .concatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .as(bindToLifecycle())
                .subscribe(response -> {
                    mProgressSubj.onNext(-1);
                    // save leveling result
//                    getModel().getSlaveComputer().finishDualExtruderAutoLeveling();
                }, LogHelper::log);
    }

    public Observable<Boolean> stopLeveling() {
        Logger.d("Exiting leveling...");
        return getModel().getSlaveComputer().exitCalibration()
                .concatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .map(response -> true);
    }

//    private void observeProgress() {
//        getModel().getSlaveComputer().getAutoCalibrationProgress()
//                .as(bindToLifecycle())
//                .subscribe(pointIndex -> {
//                    mProgressSubj.onNext(pointIndex);
//                    if (pointIndex ==)
//                }, LogHelper::log);
//    }

    public Observable<Integer> getActiveExtruder() {
        return getModel().getSlaveComputer().getActivatedExtruder();
    }

    public Observable<Integer> getProgressObservable() {
        return mProgressSubj.hide();
    }
}
