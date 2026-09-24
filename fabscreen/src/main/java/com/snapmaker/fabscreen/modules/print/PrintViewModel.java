package com.snapmaker.fabscreen.modules.print;

import com.orhanobut.logger.Logger;

import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class PrintViewModel extends BaseViewModel {
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_PRINTING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_COMPLETED = 3;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public PrintViewModel() {
        super();
    }

    public int getHeadType() {
        return getModel().getMachineController().getHeadType();
    }

    public Observable<Integer> getCountObservable() {
        return getModel().getPrintController().getTickCounter().getCountObservable();
    }

    public boolean isMachineStatePrinting() {
        int machinePrintStatus = getModel().getMachineController().getMachineStatus().printerStatus;
        final boolean isMachineStatusPrinting = machinePrintStatus == 3 || machinePrintStatus == 4;
        final boolean isPrintJobStatePrinting = MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue());
        return isMachineStatusPrinting && isPrintJobStatePrinting;
    }

    public void initAirPurifier() {
        int headType = getModel().getMachineController().getHeadType();
        // check air purifier flag
        if (getModel().getMachineController().isAirPurifierPlugged()) {
            boolean airPurifierAutoTurnOnFlag;
            switch (headType) {
                case Constants.HEAD_3DP:
                case Constants.HEAD_3DP_DUAL_EXTRUDER:
                    airPurifierAutoTurnOnFlag = getModel().getPreferences().getAirPurifier3DPAutoFlag();
                    break;
                case Constants.HEAD_LASER:
                case Constants.HEAD_LASER_2W_IR:
                case Constants.HEAD_LASER_10W:
                case Constants.HEAD_LASER_20W:
                case Constants.HEAD_LASER_40W:
                    airPurifierAutoTurnOnFlag = getModel().getPreferences().getAirPurifierLaserAutoFlag();
                    break;
                case Constants.HEAD_CNC:
                case Constants.HEAD_CNC_200W:
                    airPurifierAutoTurnOnFlag = getModel().getPreferences().getAirPurifierCNCAutoTurnOnFlag();
                    break;
                default:
                    airPurifierAutoTurnOnFlag = false;
                    break;
            }

            if (airPurifierAutoTurnOnFlag) {
                getModel().getMachineController().setAirPurifierEnabled(true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(success -> {
                            if (!success) {
                                Logger.w("Open Air Purifier failed!");
                            }
                        }, LogHelper::log);
            }
        }
    }

    public Observable<Boolean> listenResumeEvent() {
        return getModel().getPrintController().getResumeObservable();
    }

    public void requestPrintStart() {
        getModel().getPrintController().start();
    }

    public void requestPrintPause() {
        getModel().getPrintController().pause();
    }

    public void requestPrintResume() {
        getModel().getPrintController().resume();
    }

    public void requestPrintStop() {
        getModel().getPrintController().stop();
    }

    public void requestPrintFinish() {
        getModel().getPrintController().finish();
    }

    public void requestPrintRecoverFromPowerLoss() {
        getModel().getPrintController().recover();
    }

    public void clearErrorFlag() {
        getModel().getSlaveComputer().resetErrorFlag()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Logger.d("Error flag removed.");
                    getModel().getMachineController().clearPowerOutageFlag();
                }, LogHelper::log);
    }

    // Outdated business, not using anymore
    private void preHeatHeatedBed() {
        float temperature = getModel().getPrintController().getOverrideInitialHeatedBedTemperature();

        getModel().getSlaveComputer().requestAdjustSettingHeatedBedTemp(temperature)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(retCode -> {
                    // do nothing
                }, LogHelper::log);
    }

    public void coolDownHeatedBed() {
        getModel().getSlaveComputer().requestAdjustSettingHeatedBedTemp(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(retCode -> {
                    // do nothing
                }, LogHelper::log);
    }

    public Observable<Integer> checkFastCalibration() {
        final int headType = getModel().getMachineController().getHeadType();
        final int calibrationMode = getModel().getPreferences().get3DPCalibrationMode();
        boolean fastCalibrationOn = getModel().getPreferences().get3DPFastCalibrationOn();
        boolean powerOutageFlag = getModel().getPrintController().getRecoveryFlag();

        // do not fast calibration if resuming print via power panic
        if (headType == Constants.HEAD_3DP && !powerOutageFlag && calibrationMode == 0 && fastCalibrationOn) {
            // Fast Calibration may take a few minutes, we can heated up the bed simultaneously.
            // preHeatHeatedBed();

            Logger.d("Start Fast Calibration...");

            return getModel().getSlaveComputer().fastCalibration();

        } else {
            return Observable.just(0);
        }
    }

}
