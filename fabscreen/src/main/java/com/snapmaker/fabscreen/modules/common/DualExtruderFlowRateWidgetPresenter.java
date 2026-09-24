package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderFlowRateWidgetPresenter extends BasePresenter {
    @BindView(R.id.tv_print_settings_3dp_flow_rate_left_title)
    TextView mTvLeftTitle;
    @BindView(R.id.tv_print_settings_3dp_flow_rate_right_title)
    TextView mTvRightTitle;
    @BindView(R.id.tv_print_settings_3dp_flow_rate_left_value_target)
    TextView mTvLeftTargetValue;
    @BindView(R.id.tv_print_settings_3dp_flow_rate_right_value_target)
    TextView mTvRightTargetValue;

    @BindView(R.id.rv_print_settings_3dp_flow_rate_left_ruler)
    RulerView mRvLeftRuler;
    @BindView(R.id.rv_print_settings_3dp_flow_rate_right_ruler)
    RulerView mRvRightRuler;

    private BehaviorSubject<Float> mLeftFlowRateValueSubject = BehaviorSubject.createDefault(100f);
    private BehaviorSubject<Float> mRightFlowRateValueSubject = BehaviorSubject.createDefault(100f);

    public DualExtruderFlowRateWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);

        mTvLeftTitle.setText(getContext().getString(R.string.print_left_extruder_flow_rate));
        mTvRightTitle.setText(getContext().getString(R.string.print_right_extruder_flow_rate));

        mTvLeftTargetValue.setText(formatValue(100));
        mTvRightTargetValue.setText(formatValue(100));

        mRvLeftRuler.setMinValue(50);
        mRvLeftRuler.setMaxValue(150);
        mRvLeftRuler.setOnValueChangedListener(value -> {
            float pow = (int) Math.pow(10, 0);
            float newValue = (int) (value * pow) / pow;
            if (value != mLeftFlowRateValueSubject.getValue()) {
                mLeftFlowRateValueSubject.onNext(newValue);
            }
        });

        mRvRightRuler.setMinValue(50);
        mRvRightRuler.setMaxValue(150);
        mRvRightRuler.setOnValueChangedListener(value -> {
            float pow = (int) Math.pow(10, 0);
            float newValue = (int) (value * pow) / pow;
            if (value != mRightFlowRateValueSubject.getValue()) {
                mRightFlowRateValueSubject.onNext(newValue);
            }
        });

    }


    public void connectPrint() {
        Disposable sub = mLeftFlowRateValueSubject
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    getModel().getPrintController().setOverrideFlowRate(0, value);
                    mTvLeftTargetValue.setText(formatValue(value));
                });
        addDisposable(sub);

        sub = mRightFlowRateValueSubject
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    getModel().getPrintController().setOverrideFlowRate(1, value);
                    mTvRightTargetValue.setText(formatValue(value));
                });
        addDisposable(sub);
    }

    protected String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }
}
