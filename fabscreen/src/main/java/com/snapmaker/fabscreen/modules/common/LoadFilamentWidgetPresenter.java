package com.snapmaker.fabscreen.modules.common;


import android.content.Context;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.view.ActionButton;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class LoadFilamentWidgetPresenter extends BasePresenter {
    private static final String TAG = LoadFilamentWidgetPresenter.class.getSimpleName();

    @BindView(R.id.btn_widget_load_filament_unload)
    ActionButton mBtnFilamentUnload;
    @BindView(R.id.btn_widget_load_filament_load)
    ActionButton mBtnFilamentLoad;

    private BehaviorSubject<Boolean> mIsLoadingSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mIsTemperatureEnoughSubject = BehaviorSubject.createDefault(false);
    private int mCurrentExtruder = 0;

    public LoadFilamentWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    public void connect() {
        Disposable sub = getReadyToLoadObservable().subscribe(isReady -> {
            mBtnFilamentUnload.setEnabled(isReady);
            mBtnFilamentLoad.setEnabled(isReady);
        });
        addDisposable(sub);

        if (getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER) {
            sub = getModel().getSlaveComputer().getActivatedExtruder()
                    .doOnNext(active -> mCurrentExtruder = active)
                    .flatMap(active -> getModel().getSlaveComputer().getMachineStatusObservable())
                    .throttleLast(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        boolean ok;
                        // Enable filament buttons until print head temperature up to target temperature
                        if (mCurrentExtruder == 0) {
                            ok = (machineStatus.headTemperature >= 175
                                    && machineStatus.headTemperature + 5 >= machineStatus.headTargetTemperature);
                        } else {
                            ok = (machineStatus.extruder1Temperature > 175
                                    && machineStatus.extruder1Temperature + 5 >= machineStatus.extruder1TargetTemperature);
                        }
                        mIsTemperatureEnoughSubject.onNext(ok);
                    });
            addDisposable(sub);
        } else {
            sub = getModel().getSlaveComputer().getMachineStatusObservable()
                    .throttleLast(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        // Enable filament buttons until print head temperature up to target temperature
                        boolean ok = (machineStatus.headTemperature >= 175
                                && machineStatus.headTemperature + 5 >= machineStatus.headTargetTemperature);
                        mIsTemperatureEnoughSubject.onNext(ok);
                    });
            addDisposable(sub);
        }

    }

    public Observable<Boolean> getReadyToLoadObservable() {
        return Observable.combineLatest(
                mIsLoadingSubject,
                mIsTemperatureEnoughSubject.distinctUntilChanged(),
                (isMoving, enough) -> !isMoving && enough);
    }

    public Observable<Boolean> getIsLoadingObservable() {
        return mIsLoadingSubject;
    }

    @OnClick(R.id.btn_widget_load_filament_load)
    void onClickLoadFilament() {
        Logger.i("Loading filament...");
        mIsLoadingSubject.onNext(true);
        mBtnFilamentLoad.setActivated(true);

        Disposable sub = getModel().getSlaveComputer().requestExtrusion(0, 60, 200, 0, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mIsLoadingSubject.onNext(false);
                    mBtnFilamentLoad.setActivated(false);
                    Logger.d("Filament loaded.");
                }, e -> {
                    LogHelper.log(e);
                    mIsLoadingSubject.onNext(false);
                    mBtnFilamentLoad.setActivated(false);
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_widget_load_filament_unload)
    void onClickUnloadFilament() {
        Logger.i("Unloading filament...");
        mIsLoadingSubject.onNext(true);
        mBtnFilamentUnload.setActivated(true);

        Disposable sub = getModel().getSlaveComputer().requestExtrusion(0, 6, 200, 60, 150)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mIsLoadingSubject.onNext(false);
                    mBtnFilamentUnload.setActivated(false);
                    Logger.d("Filament unloaded.");
                }, e -> {
                    LogHelper.log(e);
                    mIsLoadingSubject.onNext(false);
                    mBtnFilamentUnload.setActivated(false);
                });
        addDisposable(sub);
    }
}
