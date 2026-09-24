package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class ZOffsetWidgetPresenter extends BasePresenter {
    @BindView(R.id.tv_widget_z_offset_title)
    TextView mTvTitle;

    @BindView(R.id.tv_widget_z_offset_current)
    TextView mTvValueCurrent;

    @BindView(R.id.tv_widget_z_offset_slash)
    TextView mTvValueSlash;

    @BindView(R.id.tv_widget_z_offset_target)
    TextView mTvValueTarget;

    @BindView(R.id.tv_widget_z_offset_unit)
    TextView mTvValueUnit;

    @BindView(R.id.sbg_widget_z_offset_steps)
    SegmentedButtonGroup mSbgSteps;

    private BehaviorSubject<Float> mZOffsetSubject = BehaviorSubject.createDefault(0f);
    private float mMoveStep = 0.1f;

    public ZOffsetWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        // changes
        mSbgSteps.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mMoveStep = 0.05f;
                    break;
                case 1:
                    mMoveStep = 0.1f;
                    break;
                case 2:
                    mMoveStep = 0.2f;
                    break;
            }
        });
        mSbgSteps.setPosition(1, false);
        mMoveStep = 0.1f;

        Disposable sub = mZOffsetSubject
                .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(temp -> {
                    if (temp > 0) {
                        mTvValueTarget.setText(String.format(Locale.US, "+%.2f", temp));
                    } else {
                        mTvValueTarget.setText(String.format(Locale.US, "%.2f", temp));
                    }
                });
        addDisposable(sub);
    }

    public void connectPrintSettings() {
        float initialValue = getModel().getPrintController().getOverrideZOffset();
        setZOffset(initialValue);
    }

    public void connectPrintSettings(float zOffset) {
        setZOffset(zOffset);
    }

    public void connectPrint() {
        float initialValue = getModel().getPrintController().getOverrideZOffset();
        setZOffset(initialValue);
    }

    public void connectPrint(float zOffset) {
        setZOffset(zOffset);
    }

    private Observable<Float> getZOffsetObservable() {
        return mZOffsetSubject;
    }

    private void setZOffset(float zOffset) {
        mZOffsetSubject.onNext(zOffset);
    }

    @OnClick(R.id.btn_widget_z_offset_up)
    void onClickUp() {
        float targetValue = Math.min((mZOffsetSubject.getValue() + mMoveStep), Constants.MAX_LIVE_Z_OFFSET);
        mZOffsetSubject.onNext(targetValue);
        getModel().getPrintController().setOverrideZOffset(targetValue);
    }

    @OnClick(R.id.btn_widget_z_offset_down)
    void onClickDown() {
        float targetValue = Math.max((mZOffsetSubject.getValue() - mMoveStep), Constants.MIN_LIVE_Z_OFFSET);
        mZOffsetSubject.onNext(targetValue);
        getModel().getPrintController().setOverrideZOffset(targetValue);
    }
}
