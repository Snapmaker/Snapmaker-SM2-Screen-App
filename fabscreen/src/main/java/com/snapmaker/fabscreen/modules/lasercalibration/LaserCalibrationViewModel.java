package com.snapmaker.fabscreen.modules.lasercalibration;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.model.LaserPattern;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class LaserCalibrationViewModel extends BaseViewModel {
    private LaserPattern mLaserPattern;

    public enum LaserCalibrationInputTip {
        TIP_NOT_POSITIVE_NUMBER,
        TIP_EMPTY,
        TIP_OK
    }

    // EditText input subject
    private PublishSubject<String> mWorkpieceDiameterInputSubject = PublishSubject.create();
    private PublishSubject<String> mWorkpieceLengthInputSubject = PublishSubject.create();
    // value subject
    private BehaviorSubject<Float> mWorkpieceDiameterSubject = BehaviorSubject.createDefault(-1.0f);
    private BehaviorSubject<Float> mWorkpieceLengthSubject = BehaviorSubject.createDefault(-1.0f);
    // tips subject
    private BehaviorSubject<LaserCalibrationInputTip> mWorkpieceDiameterTipsSubject = BehaviorSubject.createDefault(LaserCalibrationInputTip.TIP_EMPTY);
    private BehaviorSubject<LaserCalibrationInputTip> mWorkpieceLengthTipsSubject = BehaviorSubject.createDefault(LaserCalibrationInputTip.TIP_EMPTY);

    public LaserCalibrationViewModel() {
        super();

        bindEvents();
    }

    public LaserPattern getLaserPattern() {
        return mLaserPattern;
    }

    public void setLaserPattern(LaserPattern laserPattern) {
        mLaserPattern = laserPattern;
    }

    public void setWorkpieceDiameterInput(String input) {
        mWorkpieceDiameterInputSubject.onNext(input);
    }

    public void setWorkpieceDiameterPreInput(String input) {
        mWorkpieceDiameterInputSubject.onNext(input);
        mWorkpieceDiameterSubject.onNext(Float.parseFloat(input));
    }

    public float getWorkpieceDiameter() {
        return mWorkpieceDiameterSubject.getValue();
    }

    public void setWorkpieceLengthInput(String input) {
        mWorkpieceLengthInputSubject.onNext(input);
    }

    public void setWorkpieceLengthPreInput(String input) {
        mWorkpieceLengthInputSubject.onNext(input);
        mWorkpieceLengthSubject.onNext(Float.parseFloat(input));
    }

    public float getWorkpieceLength() {
        return mWorkpieceLengthSubject.getValue();
    }


    public Observable<LaserCalibrationInputTip> getWorkpieceDiameterTipObservable() {
        return mWorkpieceDiameterTipsSubject.hide();
    }

    public Observable<LaserCalibrationInputTip> getWorkpieceLengthTipObservable() {
        return mWorkpieceLengthTipsSubject.hide();
    }

    private void bindEvents() {
        mWorkpieceLengthInputSubject
                .as(bindToLifecycle())
                .subscribe(s -> {
                    if (s.isEmpty()) {
                        mWorkpieceLengthTipsSubject.onNext(LaserCalibrationInputTip.TIP_EMPTY);
                    } else {
                        final float length;
                        try {
                            length = Float.parseFloat(s);
                            mWorkpieceLengthSubject.onNext(length);
                        } catch (NumberFormatException e) {
                            LogHelper.log(e);
                        }
                    }
                });

        mWorkpieceDiameterInputSubject
                .as(bindToLifecycle())
                .subscribe(s -> {
                    if (s.isEmpty()) {
                        mWorkpieceDiameterTipsSubject.onNext(LaserCalibrationInputTip.TIP_EMPTY);
                    } else {
                        final float diameter;
                        try {
                            diameter = Float.parseFloat(s);
                            mWorkpieceDiameterSubject.onNext(diameter);
                        } catch (NumberFormatException e) {
                            LogHelper.log(e);
                        }
                    }
                });


        mWorkpieceDiameterSubject
                .skip(1)
                .map(diameter -> {
                    if (diameter <= 0) {
                        return LaserCalibrationInputTip.TIP_NOT_POSITIVE_NUMBER;
                    } else {
                        return LaserCalibrationInputTip.TIP_OK;
                    }
                })
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    mWorkpieceDiameterTipsSubject.onNext(tip);
                });

        mWorkpieceLengthSubject
                .skip(1)
                .map(length -> {
                    if (length <= 0) {
                        return LaserCalibrationInputTip.TIP_NOT_POSITIVE_NUMBER;
                    } else {
                        return LaserCalibrationInputTip.TIP_OK;
                    }
                })
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    mWorkpieceLengthTipsSubject.onNext(tip);
                });
    }

    public Observable<Boolean> getMaterialInputReady() {
        return Observable.combineLatest(mWorkpieceDiameterInputSubject, mWorkpieceLengthInputSubject, mWorkpieceDiameterSubject, mWorkpieceLengthSubject,
                (inputD, inputL, d, l) -> !inputD.isEmpty() && !inputL.isEmpty() && (d > 0) && (l > 0));
    }
}
