package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class SpindleWidgetPresenter extends SetValueRulerWidgetPresenter {
    private int mHeadType;

    public SpindleWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);

        mHeadType = model.getMachineController().getHeadType();
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        mTvTitle.setText(R.string.print_spindle_spend);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        if (mHeadType == Constants.HEAD_CNC) {
            mTvValueUnit.setText(R.string.all_unit_percentage);
            mRvRuler.setMinValue(50);
            mRvRuler.setMaxValue(100);
        } else {
            mTvValueUnit.setText(R.string.all_unit_rpm);
            mRvRuler.setMinValue(8000);
            mRvRuler.setMaxValue(18000);
            mRvRuler.setUnit(500);
        }
    }

    public void connectControl() {
        // TODO: Use spindle's own preference
        if (mHeadType == Constants.HEAD_CNC) {
            float initialValue = getModel().getPreferences().getLaserControlPower();
            setTargetValue(initialValue);
        } else {
            setTargetValue(18000);
        }


        // Save the changed value to preferences
        Disposable sub = getTargetValueObservable()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    if (mHeadType == Constants.HEAD_CNC) {
                        getModel().getPreferences().setLaserControlPower(value);
                    }
                });
        addDisposable(sub);
    }
}
