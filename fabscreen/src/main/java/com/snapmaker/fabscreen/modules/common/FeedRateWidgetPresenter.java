package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Model;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class FeedRateWidgetPresenter extends SetValueRulerWidgetPresenter {
    public FeedRateWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        super.bind(view);

        mTvTitle.setText(R.string.print_feed_rate);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_percentage);
        mRvRuler.setMaxValue(500);
    }

    public void connectPrintSettings() {
        float initialValue = getModel().getPrintController().getOverrideFeedRate();
        setTargetValue(initialValue);

        Disposable sub = getTargetValueObservable()
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPrintController().setOverrideFeedRate(value));
        addDisposable(sub);
    }

    public void connectPrint() {
        float initialValue = getModel().getPrintController().getOverrideFeedRate();
        connectPrint(initialValue);
    }

    public void connectPrint(float workSpeed) {
        setTargetValue(workSpeed);

        Disposable sub = getTargetValueObservable()
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPrintController().setOverrideFeedRate(value));
        addDisposable(sub);
    }

    @Override
    protected String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }
}
