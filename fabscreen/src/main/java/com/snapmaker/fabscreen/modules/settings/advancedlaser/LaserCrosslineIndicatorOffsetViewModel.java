package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class LaserCrosslineIndicatorOffsetViewModel extends BaseViewModel {
    public enum IndicatorOffsetInputTip {
        TIP_EMPTY,
        TIP_OK
    }

    // EditText input subject
    private PublishSubject<String> mXOffsetInputSubject = PublishSubject.create();
    private PublishSubject<String> mYOffsetInputSubject = PublishSubject.create();
    // value subject
    private BehaviorSubject<Float> mXOffsetValueSubject = BehaviorSubject.createDefault(0f);
    private BehaviorSubject<Float> mYOffsetValueSubject = BehaviorSubject.createDefault(0f);
    // tips subject
    private BehaviorSubject<IndicatorOffsetInputTip> mXOffsetTipsSubject = BehaviorSubject.createDefault(IndicatorOffsetInputTip.TIP_EMPTY);
    private BehaviorSubject<IndicatorOffsetInputTip> mYOffsetTipsSubject = BehaviorSubject.createDefault(IndicatorOffsetInputTip.TIP_EMPTY);

    public LaserCrosslineIndicatorOffsetViewModel() {
        super();

        bindEvents();
    }

    public void setXOffsetInput(String input) {
        mXOffsetInputSubject.onNext(input);
    }

    public float getXOffset() {
        return mXOffsetValueSubject.getValue();
    }

    public void setYOffsetInput(String input) {
        mYOffsetInputSubject.onNext(input);
    }

    public float getYOffset() {
        return mYOffsetValueSubject.getValue();
    }


    public Observable<IndicatorOffsetInputTip> getXOffsetTipObservable() {
        return mXOffsetTipsSubject.hide();
    }

    public Observable<IndicatorOffsetInputTip> getYOffsetTipObservable() {
        return mYOffsetTipsSubject.hide();
    }

    private void bindEvents() {
        mYOffsetInputSubject
                .as(bindToLifecycle())
                .subscribe(s -> {
                    if (s.isEmpty()) {
                        mYOffsetTipsSubject.onNext(IndicatorOffsetInputTip.TIP_EMPTY);
                    } else {
                        final float yOffsetValue;
                        try {
                            // Fix negative zero(or signed zero) for float.
                            if (s.equals("-")) {
                                s = "-0";
                            }
                            yOffsetValue = Float.parseFloat(s);
                            mYOffsetValueSubject.onNext(yOffsetValue);
                        } catch (NumberFormatException e) {
                            LogHelper.log(e);
                        }
                    }
                });

        mXOffsetInputSubject
                .as(bindToLifecycle())
                .subscribe(s -> {
                    if (s.isEmpty()) {
                        mXOffsetTipsSubject.onNext(IndicatorOffsetInputTip.TIP_EMPTY);
                    } else {
                        final float xOffsetValue;
                        try {
                            // Fix negative zero(or signed zero) for float.
                            if (s.equals("-")) {
                                s = "-0";
                            }
                            xOffsetValue = Float.parseFloat(s);
                            mXOffsetValueSubject.onNext(xOffsetValue);
                        } catch (NumberFormatException e) {
                            LogHelper.log(e);
                        }
                    }
                });


        mXOffsetValueSubject
                .skip(1)
                .map(xOffset -> IndicatorOffsetInputTip.TIP_OK)
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    mXOffsetTipsSubject.onNext(tip);
                });

        mYOffsetValueSubject
                .skip(1)
                .map(yOffset -> IndicatorOffsetInputTip.TIP_OK)
                .as(bindToLifecycle())
                .subscribe(tip -> {
                    mYOffsetTipsSubject.onNext(tip);
                });

        getModel().getMachineController().getLaserCrossLineIndicatorOffset()
                .as(bindToLifecycle())
                .subscribe(crossLineIndicatorOffset -> {
                    mXOffsetValueSubject.onNext(crossLineIndicatorOffset.getXOffset());
                    mYOffsetValueSubject.onNext(crossLineIndicatorOffset.getYOffset());
                });
    }

    public Observable<Boolean> getOffsetInputReady() {
        return Observable.combineLatest(mXOffsetInputSubject, mYOffsetInputSubject, mXOffsetValueSubject, mYOffsetValueSubject,
                (inputX, inputY, d, l) -> !inputX.isEmpty() && !inputY.isEmpty());
    }

    public Observable<Boolean> saveLaserCrossLineIndicatorOffset() {
        return getModel().getMachineController().setLaserCrossLineIndicatorOffset(mXOffsetValueSubject.getValue(), mYOffsetValueSubject.getValue());
    }
}
