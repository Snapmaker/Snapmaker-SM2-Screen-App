package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.CallSuper;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class SetValueRulerWidgetPresenter extends BasePresenter {
    @BindView(R.id.tv_widget_set_value_ruler_title)
    protected TextView mTvTitle;

    @BindView(R.id.tv_widget_set_value_ruler_value_current)
    protected TextView mTvValueCurrent;

    @BindView(R.id.tv_widget_set_value_ruler_value_slash)
    protected TextView mTvValueSlash;

    @BindView(R.id.tv_widget_set_value_ruler_value_target)
    protected TextView mTvValueTarget;

    @BindView(R.id.tv_widget_set_value_ruler_value_unit)
    protected TextView mTvValueUnit;

    @BindView(R.id.rv_widget_set_value_ruler_ruler)
    protected RulerView mRvRuler;

    private int mPrecision = 1;

    private BehaviorSubject<Float> mTargetValueSubject = BehaviorSubject.createDefault(0f);

    public SetValueRulerWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public int getPrecision() {
        return mPrecision;
    }

    public void setPrecision(int precision) {
        mPrecision = precision;
    }

    @CallSuper
    public void bind(View view) {
        ButterKnife.bind(this, view);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.GONE);

        // changes
        mRvRuler.setOnValueChangedListener(value -> {
            float pow = (float) Math.pow(10, mPrecision);
            float newValue = (int) (value * pow) / pow;
            if (value != mTargetValueSubject.getValue()) {
                mTargetValueSubject.onNext(newValue);
            }
        });

        Disposable sub = mTargetValueSubject
                .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> mTvValueTarget.setText(formatValue(value)));
        addDisposable(sub);
    }

    /**
     * Can be override.
     */
    protected String formatValue(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public float getTargetValue() {
        return mTargetValueSubject.getValue();
    }

    public Observable<Float> getTargetValueObservable() {
        return mTargetValueSubject;
    }

    public void setCurrentValue(float value) {
        mTvValueCurrent.setText(formatValue(value));
    }

    public void setTargetValue(float value) {
        mRvRuler.setCurrentValue(value);
    }

    public void setMaxValue(float value) {
        mRvRuler.setMaxValue(value);
    }
}
