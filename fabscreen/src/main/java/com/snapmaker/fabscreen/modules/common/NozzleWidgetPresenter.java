package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class NozzleWidgetPresenter extends SetValueRulerWidgetPresenter {
    private int mCurrentExtruder = 0;
    public NozzleWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);

        if (getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER) {
            Disposable sub = getModel().getSlaveComputer().getActivatedExtruder()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(which -> mCurrentExtruder = which, LogHelper::log);
            addDisposable(sub);
        }
    }

    @Override
    public void bind(View view) {
        super.bind(view);

        mTvTitle.setText(R.string.print_nozzle_temp);

        mTvValueCurrent.setVisibility(View.GONE);
        mTvValueSlash.setVisibility(View.GONE);
        mTvValueUnit.setVisibility(View.VISIBLE);

        mTvValueUnit.setText(R.string.all_unit_temperature);
        mRvRuler.setMaxValue(275);
    }

    /**
     * connect print settings.
     * - current: N/A
     * - target: ruler value (initially use settings value)
     */
    public void connectPrintSettings() {
        int initialValue = (int) getModel().getPrintController().getOverrideInitialNozzleTemperature();
        setTargetValue(initialValue);

        Disposable sub = getTargetValueObservable()
                .skip(1)
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> getModel().getPrintController().setOverrideInitialNozzleTemperature(value, true));
        addDisposable(sub);
    }

    /**
     * Connect printing.
     * - current: machine status
     * - target: ruler value
     */
    public void connectPrint() {
        mTvValueCurrent.setVisibility(View.VISIBLE);
        mTvValueSlash.setVisibility(View.VISIBLE);

        Disposable sub;
        if (getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER) {
            sub = getModel().getSlaveComputer().getMachineStatusObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        setCurrentValue(mCurrentExtruder == 0 ? machineStatus.headTemperature : machineStatus.extruder1Temperature);
                    });
            addDisposable(sub);

            // send target temp.
            sub = getTargetValueObservable()
                    .skip(1) // skip initial value
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(value -> {
                        if (!getModel().getPrintController().getInitialM109Flag()) {
                            getModel().getPrintController().setOverrideInitialNozzleTemperature(value, true);
                        }
                        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
                        int targetTemperature = mCurrentExtruder == 0 ? machineStatus.headTargetTemperature : machineStatus.extruder1TargetTemperature;

                        if (value != targetTemperature) {
                            getModel().getPrintController().setOverrideNozzleTemperature(0, value);
                        }
                    });
        } else {
            // update current / target temp.
            sub = getModel().getSlaveComputer().getMachineStatusObservable()
                    .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        setCurrentValue(machineStatus.headTemperature);
                    });
            addDisposable(sub);

            // send target temp.
            sub = getTargetValueObservable()
                    .skip(1) // skip initial value
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(value -> {
                        if (!getModel().getPrintController().getInitialM109Flag()) {
                            getModel().getPrintController().setOverrideInitialNozzleTemperature(value, true);
                        }
                        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
                        if (value != machineStatus.headTargetTemperature) {
                            getModel().getPrintController().setOverrideNozzleTemperature(value);
                        }
                    });
        }
        addDisposable(sub);
    }

    /**
     * connect machine status
     * - current: current nozzle temp.
     * - target: ruler value (initially use machine status target value)
     */
    public void connectMachineStatus() {
        mTvValueCurrent.setVisibility(View.VISIBLE);
        mTvValueSlash.setVisibility(View.VISIBLE);

        Disposable sub;
        if (getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER) {
// init target temp.
            FabPacketContent.MachineStatus machineStatus0 = getModel().getSlaveComputer().getMachineStatus();
            setCurrentValue(mCurrentExtruder == 0 ? machineStatus0.headTemperature : machineStatus0.extruder1Temperature);
            setTargetValue(mCurrentExtruder == 0 ? machineStatus0.headTargetTemperature : machineStatus0.extruder1TargetTemperature);

            // update current / target temp.
            sub = getModel().getSlaveComputer().getMachineStatusObservable()
                    .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        setCurrentValue(mCurrentExtruder == 0 ? machineStatus.headTemperature : machineStatus.extruder1Temperature);
                    }, LogHelper::log);
            addDisposable(sub);

        } else {
            // init target temp.
            FabPacketContent.MachineStatus machineStatus0 = getModel().getSlaveComputer().getMachineStatus();
            setCurrentValue(machineStatus0.headTemperature);
            setTargetValue(machineStatus0.headTargetTemperature);

            // update current / target temp.
            sub = getModel().getSlaveComputer().getMachineStatusObservable()
                    .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(machineStatus -> {
                        setCurrentValue(machineStatus.headTemperature);
                    }, LogHelper::log);
            addDisposable(sub);
        }

        // send target temp.
        sub = getTargetValueObservable()
                .skip(1) // skip initial value
                .debounce(200, TimeUnit.MILLISECONDS)
                .flatMap(value -> getModel().getSlaveComputer().sendGcode("M104 S" + value))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> { /**/ });
        addDisposable(sub);
    }

    @Override
    protected String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }
}
