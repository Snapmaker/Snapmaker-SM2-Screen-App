package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.PrintDetailPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.home.HomeActivity;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.DateHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.view.CircularProgressView;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderXYCalibrationPrintFragment extends BaseFragment {
    // progress
    @BindView(R.id.cpv_print_progress)
    CircularProgressView mCpvProgress;
    @BindView(R.id.tv_print_progress)
    TextView mTvProgress;

    // basic info
    @BindView(R.id.tv_print_file_name)
    TextView mTvFilename;

    // detail panel
    @BindView(R.id.widget_detail_panel_3dp_dual_extruder)
    View mViewDetailPanel3DPDualExtruder;

    // buttons
    @BindView(R.id.btn_print_stop_old)
    Button mBtnStop;

    private DualExtruderXYCalibrationPrintViewModel mViewModel;

    private PrintDetailPanelWidgetPresenter mDetailPanelWidgetPresenter;
    private int mHeadType;

    // indicates the the machine is moving, hence buttons should be disabled temporarily.
    private BehaviorSubject<Boolean> mWaitingSubject = BehaviorSubject.createDefault(false);

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private Disposable mPrintJobEventDisposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mViewModel = getActivityScopeViewModel(DualExtruderXYCalibrationPrintViewModel.class);

        mDetailPanelWidgetPresenter = new PrintDetailPanelWidgetPresenter();
        mDetailPanelWidgetPresenter.bind(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
        initPrintJob();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_xy_calibration_print;
    }

    @Override
    protected void back() {
        ((DualExtruderXYCalibrationActivity) requireActivity()).lockDrawer();
        mCompositeDisposable.clear();
        super.back();
    }

    private void initView() {
        mTvFilename.setSelected(true);
        mTvFilename.setText(R.string.guide_3dp_dual_extruder_xy_calibration_print_model_name);
        getModel().getPrintController().reset();

        mHeadType = getModel().getMachineController().getHeadType();

        // display views according to head type
        switch (mHeadType) {
            case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                mViewDetailPanel3DPDualExtruder.setVisibility(View.VISIBLE);
                break;
            }
            case Constants.HEAD_3DP:
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W: {
                mViewDetailPanel3DPDualExtruder.setVisibility(View.GONE);
                break;
            }
            default:
                break;
        }

        mDetailPanelWidgetPresenter.setEstimatedTime(10 * 60);
        // watch machine status and update detail panel
        getModel().getMachineController()
                .getMachineStatusObservable()
                .throttleFirst(2000, Constants.THROTTLE_TIME_UNIT)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    switch (mHeadType) {
                        case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                            if (getModel().getPrintController().getOverrideNozzleTemperatureDirty()) {
                                float target = getModel().getPrintController().getOverrideNozzleTemperature();
                                mDetailPanelWidgetPresenter.setLeftNozzleTemp(status.headTemperature, target);
                                mDetailPanelWidgetPresenter.setRightNozzleTemp(status.headTemperature, target);
                            } else {
                                mDetailPanelWidgetPresenter.setLeftNozzleTemp(status.headTemperature, status.headTargetTemperature);
                                mDetailPanelWidgetPresenter.setRightNozzleTemp(status.extruder1Temperature, status.extruder1TargetTemperature);
                            }
                            if (getModel().getPrintController().getOverrideHeatedBedTemperatureDirty()) {
                                float target = getModel().getPrintController().getOverrideHeatedBedTemperature();
                                mDetailPanelWidgetPresenter.setHeatedBedTempDualExtruder(status.bedTemperature, target);
                            } else {
                                mDetailPanelWidgetPresenter.setHeatedBedTempDualExtruder(status.bedTemperature, status.bedTargetTemperature);
                            }
                            mDetailPanelWidgetPresenter.setLeftFeedRatePerSecond(status.feedRate / 60);

                            break;
                        }
                        case Constants.HEAD_3DP:
                        case Constants.HEAD_LASER:
                        case Constants.HEAD_LASER_2W_IR:
                        case Constants.HEAD_LASER_10W:
                        default:
                            break;
                    }
                }, Throwable::printStackTrace);

        getModel().getPrintController().getPrintJobStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(state -> {
                    Logger.d("current print job state is %d", state.getValue());
                    if (state.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue())) {
                        updateProgress();
                    }
                    updateButtonsByState(state);
                });
    }

    private void updateButtonsByState(MachinePrintJobState printJobState) {
        Logger.d("update button state %d", printJobState.getValue());
        final boolean isPrintStateChanging = MachinePrintJobState.isPrintStateChanging(printJobState.getValue());
        final boolean isJobPrinting = printJobState.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_PRINTING.getValue());

        mBtnStop.setEnabled(!isPrintStateChanging);
        mBtnStop.setVisibility(printJobState.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue()) ? Button.INVISIBLE : Button.VISIBLE);

    }

    private void initPrintJob() {
        final IFile printFile = mViewModel.getCalibrationPrintFile();
        if (printFile == null || !printFile.exists() || printFile.length() == 0) {
            FabConfirm.create(getContext())
                    .setDescription(R.string.print_warning_open_unable)
                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                        dialog.dismiss();
                        back();
                    })
                    .show();
            return;
        }
        AndroidSchedulers.mainThread().scheduleDirect(this::startPrint, 300, Constants.TIME_UNIT);
    }

    @SuppressLint("AutoDispose")
    private void startPrint() {
        mCompositeDisposable.clear();

        IPrintController printController = getModel().getPrintController();
        printController.setFile(mViewModel.getCalibrationPrintFile());
        printController.setTotalLines(mViewModel.getFileTotalLine());

        subscribePrintEvent();

        printController.start();

        // reset filament flag before print start.
        getModel().getMachineController().clearFilamentOutFlag();

        // Watch errors from slave computer
        Disposable sub = getModel().getMachineController()
                .getFilamentObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFilamentOut -> {
                    if (isFilamentOut) {
                        Logger.d("Filament out detected.");
                        getModel().getPrintController().pauseOnFilamentUsedOut();
                        FabConfirm.create(requireContext())
                                .setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setDescription(R.string.guide_3dp_dual_extruder_xy_calibration_print_dialog_filament_runout_desc)
                                .setCanceledOnTouchOutSide(false)
                                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                    if (!getModel().getPrintController().getPrintJobState().valueEqual(MachinePrintJobState.PRINT_JOB_STATE_PAUSED.getValue())) {
                                        Logger.d("Print Job was not in the paused state, returning..");
                                        return;
                                    } else {
                                        dialog.dismiss();
                                        getModel().getPrintController().stop();
                                        getModel().getMachineController().clearFilamentOutFlag();
                                    }
                                })
                                .show();
                    }
                }, LogHelper::log);
        mCompositeDisposable.add(sub);

        // reset door detection flag before print start.
        getModel().getMachineController().clearEnclosureDoorFlag();

        // watch enclosure door detection
        sub = getModel().getMachineController().getEnclosureDoorObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isEnclosureDoorOpened -> {
                    if (getModel().getMachineController().isEnclosureDoorDetectionEnabled() && isEnclosureDoorOpened) {
                        printController.pauseOnEnclosureDoorDetected();
                        getModel().getPrintController().stop();
                        back();
                    }
                });
        mCompositeDisposable.add(sub);

//        sub = getModel().getPrintController().getResumeObservable()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(resume -> resumeFromChangeFilament());
//        mCompositeDisposable.add(sub);

        // Update
        sub = Observable.interval(5, TimeUnit.SECONDS)
                .takeUntil(tick -> (getModel().getPrintController().getPrintJobState().valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue())))
                .filter(tick -> mViewModel.isMachineStatePrinting())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    updateProgress();
                }, Throwable::printStackTrace);
        mCompositeDisposable.add(sub);
        updateProgress();
    }

    @SuppressLint("AutoDispose")
    private void subscribePrintEvent() {
        if (mPrintJobEventDisposable != null && !mPrintJobEventDisposable.isDisposed()) {
            mPrintJobEventDisposable.dispose();
        }

        mPrintJobEventDisposable = getModel().getPrintController().getPrintJobEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(printJobEvent -> {
                    Logger.d("printJobEvent " + printJobEvent.getPrintEventState().name());
                    int retCode = printJobEvent.getErrorCode();
                    switch (printJobEvent.getPrintEventState()) {
                        case REQUEST_START_SUCCESS:
                            Logger.i("Print started.");
                            break;
                        case REQUEST_START_FAILED:
                            if (retCode == 203) {
                                Logger.d("Unable to start printing, enclosure door open detected.");
//                                  handleEnclosureDoorPaused();
                                back();
                                break;
                            }

                            if (retCode == 202) {
                                Logger.w("Filament used out, unable to start printing.");
                                FabConfirm.create(requireContext())
                                        .setIcon(R.drawable.pic_dialog_warning_72x72)
                                        .setDescription(R.string.guide_3dp_dual_extruder_xy_calibration_print_dialog_filament_runout_desc)
                                        .setCanceledOnTouchOutSide(false)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
//                                              getModel().getPrintController().stop();

                                            getModel().getMachineController().clearFilamentOutFlag();
                                            back();
                                        })
                                        .show();
                            } else {
                                Logger.w("Unable to start printing, ret code %d", retCode);
                                FabConfirm.create(getContext())
                                        .setDescription(R.string.print_warning_start_unable)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                            backToHome(HomeActivity.class);
                                        }).show();
                            }
                            break;
                        case REQUEST_PAUSE_SUCCESS:
                            Logger.i("Print paused.");
                            break;
                        case REQUEST_PAUSE_FAILED:
                            Logger.w("Unable to pause printing, errorCode %d", printJobEvent.getErrorCode());
                            FabConfirm.create(getContext())
                                    .setDescription(R.string.print_warning_pause_unable)
                                    .setConfirm(R.string.all_confirm, (dialog, which) -> dialog.dismiss())
                                    .show();
                            break;
                        case REQUEST_RESUME_SUCCESS:
                            Logger.i("Print resumed.");
                            break;
                        case REQUEST_RESUME_FAILED:
                            if (retCode == 203) {
                                getModel().getPrintController().pauseOnEnclosureDoorDetected();
//                                  handleEnclosureDoorPaused();
                                back();
                                return;
                            }

                            if (retCode == 202) {
                                Logger.d("Filament used out, unable to resume printing.");
                                getModel().getPrintController().pauseOnFilamentUsedOut();
//                                  handleFilamentRunOut(null);
                                FabConfirm.create(requireContext())
                                        .setIcon(R.drawable.pic_dialog_warning_72x72)
                                        .setDescription(R.string.guide_3dp_dual_extruder_xy_calibration_print_dialog_filament_runout_desc)
                                        .setCanceledOnTouchOutSide(false)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                            getModel().getPrintController().stop();
                                            back();
                                        })
                                        .show();
                            } else {
                                Logger.w("Unable to resume printing, errorCode %d", printJobEvent.getErrorCode());
                                FabConfirm.create(getContext())
                                        .setDescription(R.string.print_warning_resume_unable)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                        })
                                        .show();
                            }
                            break;
                        case REQUEST_STOP_SUCCESS:
                            Logger.i("print stopped.");
                            back();
                            break;
                        case REQUEST_STOP_FAILED:
                            Logger.w("Unable to stop printing, ret code %d", retCode);
                            FabConfirm.create(getContext())
                                    .setDescription(R.string.print_warning_stop_unable)
                                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                        dialog.dismiss();
                                        back();
                                    })
                                    .show();
                            break;
                        case PRINT_FINISH_SUCCESS:
                            Logger.i("Print Finished.");
                            Logger.d("Print job costs %s.", DateHelper.formatTime2(getModel().getPrintController().getTickCounter().getCount()));

                            handleCalibrationPrintFinish();
                            break;
                        case PRINT_FINISH_FAILED:
                            Logger.w("Unable to finish printing, ret code %d", retCode);
                            FabConfirm.create(getContext())
                                    .setDescription(R.string.print_warning_finish_unable)
                                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .show();
                            break;
                        case REQUEST_POWER_LOSS_RECOVER_SUCCESS:
                            // we resumed from power outage
                            getModel().getPrintController().setPowerOutageFlag(false);
                            Logger.i("Print recovered.");

                            // clear flag when resume success
//                            mViewModel.clearErrorFlag();
                            getModel().getPrintController().setPowerOutageFlag(false);
                            Logger.i("Print recovered.");

                            // clear flag when resume success
                            Disposable sub = getModel().getSlaveComputer().resetErrorFlag()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(success -> {
                                        Logger.d("Error flag removed.");
                                        getModel().getMachineController().clearPowerOutageFlag();
                                    }, LogHelper::log);
                            mCompositeDisposable.add(sub);
                            break;
                        case REQUEST_POWER_LOSS_RECOVER_FAILED:
                            if (retCode == 203) {
                                back();
                                return;
                            }

                            if (retCode == 202) {
                                Logger.d("Filament used out, failed to recover from power loss.");
                                FabConfirm.create(requireContext())
                                        .setIcon(R.drawable.pic_dialog_warning_72x72)
                                        .setDescription(R.string.guide_3dp_dual_extruder_xy_calibration_print_dialog_filament_runout_desc)
                                        .setCanceledOnTouchOutSide(false)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                            getModel().getPrintController().stop();
                                            back();
                                        })
                                        .show();
                            } else {
                                Logger.w("Failed to recover from power loss.");
                                FabConfirm.create(getContext())
                                        .setDescription(R.string.print_warning_resume_unable)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                            getModel().getPrintController().setPowerOutageFlag(false);
//                                    backToHome(HomeActivity.class);
                                            back();
                                        })
                                        .show();
                            }
                            break;
                        case PRINT_PAUSED_TRIGGERED_BY_CONTROLLER:
                            break;
                        case FDM_FILAMENT_RUN_OUT_PAUSED_TRIGGERED:
                            break;
                        case FDM_FILAMENT_RUN_OUT_CHANGING_FILAMENT:
                            break;
                        case FDM_FILAMENT_RUN_OUT_RECHECK_PENDING:
                            break;
                        case ENCLOSURE_DOOR_OPEN_PAUSED_TRIGGERED:
                            break;
                        default:
                            break;
                    }
                });
    }

    // prepare print listener

    private void updateProgress() {
        float p = getModel().getPrintController().getProgress();

        // Add valid value check for progress, value must be between 0 to 1.
        p = Math.max(0f, Math.min(1.0f, p));

        // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
//        int elapsed = getModel().getPrintController().getTickCounter().getCount();

        final int percentage = (int) (100 * p);
        mCpvProgress.setPercentage(percentage);
        mTvProgress.setText(String.valueOf(percentage));
    }

    private void handleCalibrationPrintFinish() {
        ((DualExtruderXYCalibrationActivity) requireActivity()).lockDrawer();
        ((DualExtruderXYCalibrationActivity) requireActivity()).goXYCalibrationPrintComplete();
    }

    @OnClick(R.id.btn_print_stop_old)
    void onClickStop() {
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setTitle(R.string.all_warning)
                .setMessage(R.string.print_warning_stop)
                .setPositive(R.string.all_yes, (dialog, which) -> {
                    dialog.dismiss();

                    mWaitingSubject.onNext(true);

                    IPrintController printController = getModel().getPrintController();
                    printController.stop();
                })
                .setNegative(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
