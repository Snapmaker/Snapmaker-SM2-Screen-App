package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.content.Context;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.SetValueRulerWidgetPresenter;

import fabscreen.libraries.legacy.data.Model;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class FireSensorSensitivityPresenter extends SetValueRulerWidgetPresenter {
    public FireSensorSensitivityPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        setPrecision(1);

        mTvTitle.setText(R.string.settings_advance_laser_40w_fire_sensor_sensitivity_pwm);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText("");
        mRvRuler.setUnit(1f);
        mRvRuler.setMinValue(0);
        mRvRuler.setMaxValue(4096);
    }

    public void connect() {
        Disposable sub = getModel().getMachineController().getFireSensorSensitivity()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    int sensitivity = value & 0xFFFF;
                    setTargetValue(sensitivity);
                });
        addDisposable(sub);
    }
}
