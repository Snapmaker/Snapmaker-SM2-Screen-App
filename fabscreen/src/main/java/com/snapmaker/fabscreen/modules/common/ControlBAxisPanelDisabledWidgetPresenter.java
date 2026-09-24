package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.view.ActionButton;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

// FIXME: Temporary Presenter for disabling B Axis.
public class ControlBAxisPanelDisabledWidgetPresenter extends ControlBAxisPanelWidgetPresenter {
    private CompositeDisposable mCompositeDisposable;

    @BindView(R.id.sbg_control_b_axis_steps)
    SegmentedButtonGroup mSbgBAxisSteps;

    @BindView(R.id.btn_widget_b_axis_clockwise)
    ActionButton mBtnBAxisClockwise;
    @BindView(R.id.btn_widget_b_axis_counterclockwise)
    ActionButton mBtnBAxisCounterClockwise;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    public ControlBAxisPanelDisabledWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
        mCompositeDisposable = compositeDisposable;
    }

    public Observable<Boolean> getMovingEventObservable() {
        return mMovingEventSubject.hide();
    }

    @Override
    public void bind(View view) {
        ButterKnife.bind(this, view);

        Disposable sub = mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movingEvent -> {/**/});
        mCompositeDisposable.add(sub);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mSbgBAxisSteps.setEnabled(false);
        mBtnBAxisClockwise.setEnabled(false);
        mBtnBAxisCounterClockwise.setEnabled(false);
    }
}
