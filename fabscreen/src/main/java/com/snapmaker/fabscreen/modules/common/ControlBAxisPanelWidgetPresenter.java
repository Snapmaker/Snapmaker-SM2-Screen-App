package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ActionButton;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class ControlBAxisPanelWidgetPresenter {
    private Context mContext;
    private Model mModel;
    private CompositeDisposable mCompositeDisposable;

    @BindView(R.id.sbg_control_b_axis_steps)
    SegmentedButtonGroup mSbgBAxisSteps;

    @BindView(R.id.btn_widget_b_axis_clockwise)
    ActionButton mBtnBAxisClockwise;
    @BindView(R.id.btn_widget_b_axis_counterclockwise)
    ActionButton mBtnBAxisCounterClockwise;

    private double mMoveStep = 1;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    public ControlBAxisPanelWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        mContext = context;
        mModel = model;
        mCompositeDisposable = compositeDisposable;
    }

    private Context getContext() {
        return mContext;
    }

    private Model getModel() {
        return mModel;
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);

        mSbgBAxisSteps.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mMoveStep = 0.2;
                    break;
                case 1:
                    mMoveStep = 1;
                    break;
                case 2:
                    mMoveStep = 5;
                    break;
            }
        });
        mSbgBAxisSteps.setPosition(1, false);
        mMoveStep = 1;

        Disposable sub = mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
        mCompositeDisposable.add(sub);
    }

    public void connect() {
        mBtnBAxisCounterClockwise.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = getModel().getSlaveComputer().sendGcode("G91")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 B+" + mMoveStep + " F1800"))
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G90"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
            mCompositeDisposable.add(sub);
        });

        mBtnBAxisClockwise.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = getModel().getSlaveComputer().sendGcode("G91")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 B-" + mMoveStep + " F1800"))
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G90"))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(res -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
            mCompositeDisposable.add(sub);
        });
    }

    public void setEnabled(boolean enabled) {
        mBtnBAxisClockwise.setEnabled(enabled);
        mBtnBAxisCounterClockwise.setEnabled(enabled);
    }
}
