package com.snapmaker.fabscreen.modules.common;


import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class HeatedBedWidgetPresenter extends SetValueRulerWidgetPresenter {
    public HeatedBedWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        setPrecision(0);

        mTvTitle.setText(R.string.print_heated_bed_temp);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_temperature);
        mRvRuler.setMaxValue(100);
    }


    public void connectPrintSettings() {
        int initialValue = (int) getModel().getPrintController().getOverrideInitialHeatedBedTemperature();
        setTargetValue(initialValue);

        Disposable sub = getTargetValueObservable()
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPrintController().setOverrideInitialHeatedBedTemperature(value, true));
        addDisposable(sub);
    }

    public void connectPrint() {
        mTvValueCurrent.setVisibility(View.VISIBLE);
        mTvValueSlash.setVisibility(View.VISIBLE);

        // update current / target temp.
        Disposable sub = getModel().getSlaveComputer().getMachineStatusObservable()
                .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(machineStatus -> {
                    setCurrentValue(machineStatus.bedTemperature);
                });
        addDisposable(sub);

        // send target temp.
        sub = getTargetValueObservable()
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    if (!getModel().getPrintController().getInitialM190Flag()) {
                        getModel().getPrintController().setOverrideInitialHeatedBedTemperature(value, true);
                    }
                    FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
                    if (value != machineStatus.bedTargetTemperature) {
                        getModel().getPrintController().setOverrideHeatedBedTemperature(value);
                    }
                });
        addDisposable(sub);
    }

    public void connectMachineStatus() {
        mTvValueCurrent.setVisibility(View.VISIBLE);
        mTvValueSlash.setVisibility(View.VISIBLE);

        // init target temp.
        FabPacketContent.MachineStatus machineStatus0 = getModel().getSlaveComputer().getMachineStatus();
        setTargetValue(machineStatus0.bedTargetTemperature);

        // update current / target temp.
        Disposable sub = getModel().getSlaveComputer().getMachineStatusObservable()
                .debounce(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(machineStatus -> {
                    setCurrentValue(machineStatus.bedTemperature);
                });
        addDisposable(sub);

        // send target temp.
        sub = getTargetValueObservable()
                .skip(1) // skip initial value
                .debounce(200, TimeUnit.MILLISECONDS)
                .flatMap(value -> getModel().getSlaveComputer().sendGcode("M140 S" + value))
                .subscribe(value -> { /**/ });
        addDisposable(sub);
    }

    @Override
    protected String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }
}
