package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class Guide3DPDualExtruderXYCalibrationCheckResultViewModel extends BaseViewModel {
    private float mXOffset = 0f;
    private float mYOffset = 0f;

    private BehaviorSubject<Boolean> mTouchXSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mTouchYSubject = BehaviorSubject.createDefault(false);

    public void setCalibrationOffset(int direction, float value) {
        Logger.d("direction %d offset %.2f", direction, value);
        if (direction == 0) {
            mXOffset = value;
            mTouchXSubject.onNext(true);
        } else {
            mYOffset = value;
            mTouchYSubject.onNext(true);
        }
    }

    public Observable<Boolean> saveXYCalibrationOffsetResult() {
        return getModel().getSlaveComputer().setExtruderOffset(0, mXOffset)
                .doOnNext(success -> {
                    Logger.d("set X Offset %.2f %b", mXOffset, success);
                })
                .concatMap(success -> success ? getModel().getSlaveComputer().setExtruderOffset(1, mYOffset) : Observable.just(false))
                .doOnNext(success -> {
                    Logger.d("set Y Offset %.2f %b", mYOffset, success);
                    getModel().getPreferences().setNeedDoXYOffsetCalibration(false);
                });
    }

    public Observable<Boolean> getResultAllSetObservable() {
        return Observable.combineLatest(mTouchXSubject, mTouchYSubject, (x, y) -> x && y);
    }

}
