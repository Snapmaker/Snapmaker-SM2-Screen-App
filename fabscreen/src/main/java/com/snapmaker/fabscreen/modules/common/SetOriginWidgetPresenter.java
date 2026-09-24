package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.view.ActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class SetOriginWidgetPresenter extends BasePresenter {
    @BindView(R.id.btn_control_laser_page_set_origin_set_origin)
    ActionButton mBtnPageSetOriginSetOrigin;
    @BindView(R.id.btn_control_laser_page_set_origin_set_origin_x)
    ActionButton mBtnPageSetOriginSetOriginX;
    @BindView(R.id.btn_control_laser_page_set_origin_set_origin_y)
    ActionButton mBtnPageSetOriginSetOriginY;
    @BindView(R.id.btn_control_laser_page_set_origin_set_origin_z)
    ActionButton mBtnPageSetOriginSetOriginZ;
    @BindView(R.id.btn_control_laser_page_set_origin_goto_origin)
    ActionButton mBtnPageSetOriginGotoOrigin;
    @BindView(R.id.btn_control_laser_page_set_origin_home)
    ActionButton mBtnHome;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    public SetOriginWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);

        Disposable sub = mMovingEventSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
        addDisposable(sub);
    }

    public void connect() {
        // automatically connected via OnClick binding
    }

    public void setEnabled(boolean enabled) {
        mBtnPageSetOriginSetOrigin.setEnabled(enabled);
        mBtnPageSetOriginSetOriginX.setEnabled(enabled);
        mBtnPageSetOriginSetOriginY.setEnabled(enabled);
        mBtnPageSetOriginSetOriginZ.setEnabled(enabled);
        mBtnPageSetOriginGotoOrigin.setEnabled(enabled);
        mBtnHome.setEnabled(enabled);
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_set_origin_x)
    void onClickSetOriginX() {
        mMovingEventSubject.onNext(true);
        mBtnPageSetOriginSetOriginX.setActivated(true);

        Logger.i("Requesting set origin x...");

        Disposable sub = getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_X)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginX.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginX.setActivated(false);
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_set_origin_y)
    void onClickSetOriginY() {
        mMovingEventSubject.onNext(true);
        mBtnPageSetOriginSetOriginY.setActivated(true);

        Logger.i("Requesting set origin y...");

        Disposable sub = getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Y)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginY.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginY.setActivated(false);
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_set_origin_z)
    void onClickSetOriginZ() {
        mMovingEventSubject.onNext(true);
        mBtnPageSetOriginSetOriginZ.setActivated(true);

        Logger.i("Requesting set origin z...");

        Disposable sub = getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_Z)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginZ.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOriginZ.setActivated(false);
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_set_origin)
    void onClickSetOrigin() {
        mMovingEventSubject.onNext(true);
        mBtnPageSetOriginSetOrigin.setActivated(true);

        Logger.i("Requesting set origin...");

        Disposable sub = getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ)
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOrigin.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnPageSetOriginSetOrigin.setActivated(false);
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_goto_origin)
    void onClickGotoOrigin() {
        final float currentZ = (float) getModel().getMachineController().getMachineStatus().z;
        mMovingEventSubject.onNext(true);
        mBtnPageSetOriginGotoOrigin.setActivated(true);

        Logger.i("Requesting go to origin...");

        if (currentZ > 0) {
            // Engage direction, move X Y linear module first, then Z.
            Disposable sub = getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G0 Z0 F1800"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        mMovingEventSubject.onNext(false);
                        mBtnPageSetOriginGotoOrigin.setActivated(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                        mBtnPageSetOriginGotoOrigin.setActivated(false);
                    });
            addDisposable(sub);
        } else {
            // Retract direction, move Z linear module first, then X Y.
            Disposable sub = getModel().getSlaveComputer().sendGcode("G0 Z0 F1800")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        mMovingEventSubject.onNext(false);
                        mBtnPageSetOriginGotoOrigin.setActivated(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                        mBtnPageSetOriginGotoOrigin.setActivated(false);
                    });
            addDisposable(sub);
        }
    }

    @OnClick(R.id.btn_control_laser_page_set_origin_home)
    void onClickHome() {
        mMovingEventSubject.onNext(true);
        mBtnHome.setActivated(true);

        Logger.i("Requesting G28...");

        Disposable sub = getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(response -> getModel().getSlaveComputer().sendGcode("G28"))
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    mBtnHome.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                    mBtnHome.setActivated(false);
                });
        addDisposable(sub);
    }
}
