package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.SteeringView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class ControlXYZPanelWidgetPresenter {
    private Context mContext;
    private Model mModel;
    private CompositeDisposable mCompositeDisposable;

    @BindView(R.id.sbg_control_steps)
    SegmentedButtonGroup mSbgControlSteps;
    @BindView(R.id.sv_control_panel_xy)
    SteeringView mSvControlXY;
    @BindView(R.id.btn_control_panel_z_plus)
    Button mBtnControlZPlus;
    @BindView(R.id.btn_control_panel_z_minus)
    Button mBtnControlZMinus;

    private double mMoveStep = 0.1;
    private boolean mDisableXY = false;
    private boolean mDisableZ = false;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    public ControlXYZPanelWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
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

    public void bind(View view, float xStep, float yStep, float zStep) {
        ButterKnife.bind(this, view);

        mSbgControlSteps.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mMoveStep = xStep;
                    break;
                case 1:
                    mMoveStep = yStep;
                    break;
                case 2:
                    mMoveStep = zStep;
                    break;
            }
        });
        mSbgControlSteps.setPosition(1, false);
        mMoveStep = 1;

        Disposable sub = mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> setEnabled(!movingEvent));
        mCompositeDisposable.add(sub);
    }

    public void connect() {
        // x y panel
        mSvControlXY.setOnDirectionClickedListener(direction -> {
            ISlaveComputer slaveComputer = getModel().getSlaveComputer();

            switch (direction) {
                case SteeringView.DIRECTION_UP: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = slaveComputer.sendGcode("G91")
                            .flatMap(res -> slaveComputer.sendGcode("G1 Y-" + mMoveStep + " F3000"))
                            .flatMap(res -> slaveComputer.sendGcode("G90"))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_DOWN: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = slaveComputer.sendGcode("G91")
                            .flatMap(res -> slaveComputer.sendGcode("G1 Y+" + mMoveStep + " F3000"))
                            .flatMap(res -> slaveComputer.sendGcode("G90"))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_LEFT: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = slaveComputer.sendGcode("G91")
                            .flatMap(res -> slaveComputer.sendGcode("G1 X-" + mMoveStep + " F3000"))
                            .flatMap(res -> slaveComputer.sendGcode("G90"))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                case SteeringView.DIRECTION_RIGHT: {
                    mMovingEventSubject.onNext(true);
                    Disposable sub = slaveComputer.sendGcode("G91")
                            .flatMap(res -> slaveComputer.sendGcode("G1 X+" + mMoveStep + " F3000"))
                            .flatMap(res -> slaveComputer.sendGcode("G90"))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> {
                                mMovingEventSubject.onNext(false);
                            }, e -> {
                                LogHelper.log(e);
                                mMovingEventSubject.onNext(false);
                            });
                    mCompositeDisposable.add(sub);
                    break;
                }
                default:
                    break;
            }
        });

        // z height button
        mBtnControlZMinus.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = getModel().getSlaveComputer().sendGcode("G91")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 Z-" + mMoveStep + " F1800"))
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
        mBtnControlZPlus.setOnClickListener(v -> {
            mMovingEventSubject.onNext(true);

            Disposable sub = getModel().getSlaveComputer().sendGcode("G91")
                    .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 Z+" + mMoveStep + " F1800"))
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
        mBtnControlZPlus.setEnabled(enabled && !mDisableZ);
        mBtnControlZMinus.setEnabled(enabled && !mDisableZ);
        mSvControlXY.setEnabled(enabled && !mDisableXY);
    }

    public void disabledXY() {
        mDisableXY = true;
        mSvControlXY.setEnabled(false);
    }

    public void disabledZ() {
        mDisableZ = true;
        mBtnControlZMinus.setEnabled(false);
        mBtnControlZPlus.setEnabled(false);
    }
}
