package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.manual;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.data.XYZMoveController.Direction;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class ManualBedLevelingViewModel extends BaseViewModel {
    public static final float[] STEP_WIDTHS = {0.05f, 0.1f, 0.5f};
    private int mStepWidthPos = 1;
    private int mNextPoint = 1;
    private int mSelectedGrid;
    private final BehaviorSubject<Boolean> mTravelSubj = BehaviorSubject.createDefault(true);
    private final BehaviorSubject<Boolean> mJoggingSubj = BehaviorSubject.create();
    private final BehaviorSubject<Integer> mProgressSubj = BehaviorSubject.createDefault(1);

    public void startLeveling(int selectedGrid) {
        mSelectedGrid = selectedGrid;
        Logger.d("Starting manual leveling %dx%d", selectedGrid, selectedGrid);
        getModel().getSlaveComputer().startDualExtruderManualLeveling(selectedGrid)
                .doOnSubscribe(disposable -> mTravelSubj.onNext(true))
                .doOnError(throwable -> mTravelSubj.onNext(false))
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mNextPoint = 1;
                    goToNextPoint();
                }, LogHelper::log);
    }

    /**
     * Save current point; Go to next point.
     */
    public void goToNextPoint() {
        Logger.d("Go to next point %d", mNextPoint);
        getModel().getSlaveComputer().dualExtruderManualLevelPoint(mNextPoint)
                .doOnSubscribe(disposable -> mTravelSubj.onNext(true))
                .doOnNext(success -> mTravelSubj.onNext(false))
                .doOnError(throwable -> mTravelSubj.onNext(false))
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (!success) return;
                    mProgressSubj.onNext(mNextPoint);
                    if (mNextPoint < mSelectedGrid * mSelectedGrid) {
                        mNextPoint++;
                    } else {
                        // all points leveled
                        mProgressSubj.onNext(-1);
                    }
                }, LogHelper::log);
    }

    /**
     * Move up or down to find better z.
     */
    public void moveZ(Direction direction) {
        if (direction != Direction.UP && direction != Direction.DOWN) return;
        float offset = direction == Direction.UP ? STEP_WIDTHS[mStepWidthPos] : -STEP_WIDTHS[mStepWidthPos];
        Logger.d("Input calibration offset %.2f for %d", offset, mNextPoint);
        getModel().getSlaveComputer().moveCalibrationPoint(offset)
                .doOnSubscribe(disposable -> mJoggingSubj.onNext(true))
                .doOnNext(response -> mJoggingSubj.onNext(false))
                .doOnError(throwable -> mJoggingSubj.onNext(false))
                .as(bindToLifecycle())
                .subscribe(response -> {
                }, LogHelper::log);
    }

    public void setStepWidthPos(int position) {
        mStepWidthPos = position;
    }

    public Observable<Boolean> getTravellingObservable() {
        return mTravelSubj.hide();
    }

    public Observable<Boolean> getJoggingObservable() {
        return mJoggingSubj.hide();
    }

    public Observable<Integer> getProgressObservable() {
        return mProgressSubj.hide();
    }

    public int getProgress() {
        return mProgressSubj.getValue();
    }

    public void saveLeveling() {
        Logger.d("Finishing manual bed leveling...");
        getModel().getSlaveComputer().finishDualExtruderManualLeveling()
                .doOnSubscribe(disposable -> mTravelSubj.onNext(true))
                /*.concatMap(success -> getModel().getSlaveComputer().stopDualExtruderManualLeveling())*/
                .concatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .doOnNext(response -> mTravelSubj.onNext(false))
                .doOnError(throwable -> mTravelSubj.onNext(false))
                .as(bindToLifecycle())
                .subscribe(response -> mProgressSubj.onNext(-2), LogHelper::log);
    }

    public int getStepPos() {
        return mStepWidthPos;
    }

    public Observable<Boolean> stopLeveling() {
        Logger.d("Stop manual leveling...");
        return getModel().getSlaveComputer().exitCalibration()
                .concatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .map(response -> true);
    }
}
