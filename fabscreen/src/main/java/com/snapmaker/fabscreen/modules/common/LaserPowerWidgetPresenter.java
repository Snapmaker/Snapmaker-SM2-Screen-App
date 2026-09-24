package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LaserPowerWidgetPresenter extends SetValueRulerWidgetPresenter {
    public LaserPowerWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);
        mTvTitle.setText(R.string.print_laser_power);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_percentage);
        mRvRuler.setMaxValue(100);
        mRvRuler.setUnit(0.5f);
    }

    public void connectPrintSettings() {
        float initialValue = getModel().getPrintController().getOverrideLaserPower();
        setTargetValue(initialValue);

        Disposable sub = getTargetValueObservable()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPrintController().setOverrideLaserPower(value));
        addDisposable(sub);
    }

    public void connectPrint() {
        // init power
        // FabPacketContent.MachineStatus machineStatus0 = getModel().getSlaveComputer().getMachineStatus();
        // setTargetValue((float) machineStatus0.laserPower);

        // tricky part here, we use override power as initial value, while
        // the power from machine status is always 0.5% for focusing.
        // float initialValue = getModel().getPrintController().getOverrideLaserPower();
        // setTargetValue(initialValue);

        // change override power
        Disposable sub = getTargetValueObservable()
                .skip(1) // skip initial value
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    getModel().getPrintController().setOverrideLaserPower(value);
                });
        addDisposable(sub);
    }

    public void connectPrint(float power) {
        setTargetValue(power);

        Disposable sub = getTargetValueObservable()
                .skip(1) // skip initial value
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    getModel().getPrintController().setOverrideLaserPower(value);
                });
        addDisposable(sub);
    }

    public void connectControl() {
        float initialValue = getModel().getPreferences().getLaserControlPower();
        setTargetValue(initialValue);

        // Save the changed value to preferences
        Disposable sub = getTargetValueObservable()
                .debounce(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPreferences().setLaserControlPower(value));
        addDisposable(sub);
    }
}
