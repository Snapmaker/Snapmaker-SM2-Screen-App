package com.snapmaker.fabscreen.modules.print;

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
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.lib.DateHelper;
import fabscreen.libraries.legacy.lib.FirebaseHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.view.CircularProgressView;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.SingleSubject;

public class PrintFragment extends BaseFragment {
    // progress
    @BindView(R.id.cpv_print_progress)
    CircularProgressView mCpvProgress;
    @BindView(R.id.tv_print_progress)
    TextView mTvProgress;
    @BindView(R.id.tv_print_remaining_time_value)
    TextView mTvRemainingTime;

    // basic info
    @BindView(R.id.tv_print_file_name)
    TextView mTvFilename;

    // detail panel
    @BindView(R.id.widget_detail_panel_3dp)
    View mViewDetailPanel3DP;
    @BindView(R.id.widget_detail_panel_3dp_dual_extruder)
    View mViewDetailPanel3DPDualExtruder;
    @BindView(R.id.widget_detail_panel_laser)
    View mViewDetailPanelLaser;
    @BindView(R.id.widget_detail_print_cnc)
    View mViewDetailPanelCNC;

    // buttons
    @BindView(R.id.btn_print_start)
    Button mBtnStart;
    @BindView(R.id.btn_print_pause)
    Button mBtnPause;
    @BindView(R.id.btn_print_resume)
    Button mBtnResume;
    @BindView(R.id.btn_print_stop)
    Button mBtnStop;
    @BindView(R.id.btn_print_complete)
    Button mBtnComplete;
    @BindView(R.id.btn_print_print_again)
    Button mBtnPrintAgain;

    private PrintDetailPanelWidgetPresenter mDetailPanelWidgetPresenter;

    private String mFilename;
    private int mHeadType;
    private float mEstimatedTime = 0;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private Disposable mPrintJobEventDisposable;
    private Disposable mPrintJobStateDisposable;
    private boolean mIsPrintPageSuspended = false;

    private PrintViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mDetailPanelWidgetPresenter = new PrintDetailPanelWidgetPresenter();
        mDetailPanelWidgetPresenter.bind(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get parameters
        mFilename = getModel().getWorkspace().getFileName();
        mEstimatedTime = getModel().getWorkspace().getEstimatedTime();

        mHeadType = mViewModel.getHeadType();

        initView();
        initPrintJob();
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribePrintEvent();
        subscribePrintState();
    }

    @Override
    public void onPause() {
        super.onPause();
        disposeEventAndStateObservable();
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsPrintPageSuspended = true;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print;
    }

    @Override
    protected PrintViewModel getViewModel() {
        return getViewModelProvider().get(PrintViewModel.class);
    }

    @Override
    protected void back() {
        mCompositeDisposable.clear();

        disposeEventAndStateObservable();

        super.back();
    }

    @Override
    protected void backToHome(Class clazz) {
        mCompositeDisposable.clear();
        disposeEventAndStateObservable();

        super.backToHome(clazz);
        requireActivity().finish();
    }

    private void initView() {
        // file name
        mTvFilename.setSelected(true);
        mTvFilename.setText(mFilename);

        initDetailPanel();

        // Update Buttons once and subscribe state changes.
        updateButtonsByState(getModel().getPrintController().getPrintJobState());

        mPrintJobStateDisposable = getModel().getPrintController().getPrintJobStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    Logger.d("current print job state is %d", state.getValue());
                    if (state.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue())) {
                        updateProgress();
                    }
                    updateButtonsByState(state);
                });
    }

    private void initDetailPanel() {
        // display views according to head type
        switch (mHeadType) {
            case Constants.HEAD_3DP: {
                mViewDetailPanel3DP.setVisibility(View.VISIBLE);
                mViewDetailPanel3DPDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
            }
            case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanel3DPDualExtruder.setVisibility(View.VISIBLE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
            }
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W: {
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanel3DPDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.VISIBLE);
                mViewDetailPanelCNC.setVisibility(View.GONE);
                break;
            }
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W: {
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanel3DPDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.VISIBLE);
                break;
            }
            default:
                break;
        }

        mDetailPanelWidgetPresenter.useElapsedTime();
        mViewModel.getCountObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(count -> mDetailPanelWidgetPresenter.setEstimatedTime(count));

        // watch machine status and update detail panel
        getModel().getMachineController()
                .getMachineStatusObservable()
                .throttleFirst(2000, Constants.THROTTLE_TIME_UNIT)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    switch (mHeadType) {
                        case Constants.HEAD_3DP: {
                            if (getModel().getPrintController().getOverrideNozzleTemperatureDirty()) {
                                float target = getModel().getPrintController().getOverrideNozzleTemperature();
                                mDetailPanelWidgetPresenter.setNozzleTemp(status.headTemperature, target);
                            } else {
                                mDetailPanelWidgetPresenter.setNozzleTemp((double)status.headTemperature, (double)status.headTargetTemperature);
                            }
                            if (getModel().getPrintController().getOverrideHeatedBedTemperatureDirty()) {
                                float target = getModel().getPrintController().getOverrideHeatedBedTemperature();
                                mDetailPanelWidgetPresenter.setHeatedBedTemp(status.bedTemperature, target);
                            } else {
                                mDetailPanelWidgetPresenter.setHeatedBedTemp(status.bedTemperature, status.bedTargetTemperature);
                            }
                            mDetailPanelWidgetPresenter.setFeedRatePerSecond(status.feedRate / 60f);
                            break;
                        }
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
                            mDetailPanelWidgetPresenter.setLeftFeedRatePerSecond(status.feedRate / 60f);
                            break;
                        }
                        case Constants.HEAD_LASER:
                        case Constants.HEAD_LASER_2W_IR:
                        case Constants.HEAD_LASER_10W:
                        case Constants.HEAD_LASER_20W:
                        case Constants.HEAD_LASER_40W: {
                            if (getModel().getPrintController().getOverrideLaserPowerDirty()) {
                                float target = getModel().getPrintController().getOverrideLaserPower();
                                mDetailPanelWidgetPresenter.setLaserPower(target);
                            } else {
                                mDetailPanelWidgetPresenter.setLaserPower((float) status.laserPower);
                            }
                            mDetailPanelWidgetPresenter.setFeedRate(status.feedRate);
                            break;
                        }
                        case Constants.HEAD_CNC:
                        case Constants.HEAD_CNC_200W: {
                            mDetailPanelWidgetPresenter.setFeedRate(status.feedRate);
                            mDetailPanelWidgetPresenter.setSpindleSpeed(status.spindleSpeed);
                            break;
                        }
                        default:
                            break;
                    }
                }, LogHelper::log);
    }

    private void updateButtonsByState(MachinePrintJobState printJobState) {
        Logger.d("update button state %d", printJobState.getValue());
        final boolean isPrintStateChanging = MachinePrintJobState.isPrintStateChanging(printJobState.getValue());
        final boolean isJobPrinting = printJobState.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_PRINTING.getValue());

        mBtnStart.setEnabled(!isPrintStateChanging);
        mBtnPause.setEnabled(!isPrintStateChanging);
        mBtnResume.setEnabled(!isPrintStateChanging);
        mBtnStop.setEnabled(!isPrintStateChanging);

        switch (printJobState) {
            case PRINT_JOB_STATE_IDLE:
            case PRINT_JOB_STATE_STARTING:
            case PRINT_JOB_STATE_RECOVERING:
                mBtnStart.setVisibility(View.VISIBLE);
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnStop.setVisibility(View.INVISIBLE);
                mBtnComplete.setVisibility(View.INVISIBLE);
                mBtnPrintAgain.setVisibility(View.INVISIBLE);
                break;
            case PRINT_JOB_STATE_PRINTING:
            case PRINT_JOB_STATE_PAUSING:
                mBtnStart.setVisibility(View.INVISIBLE);
                mBtnPause.setVisibility(View.VISIBLE);
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnStop.setVisibility(View.VISIBLE);
                mBtnComplete.setVisibility(View.INVISIBLE);
                mBtnPrintAgain.setVisibility(View.INVISIBLE);
                break;
            case PRINT_JOB_STATE_PAUSED:
            case PRINT_JOB_STATE_RESUMING:
                mBtnStart.setVisibility(View.INVISIBLE);
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.VISIBLE);
                mBtnStop.setVisibility(View.VISIBLE);
                mBtnComplete.setVisibility(View.INVISIBLE);
                mBtnPrintAgain.setVisibility(View.INVISIBLE);
                break;
            case PRINT_JOB_STATE_STOPPING:
                mBtnStart.setVisibility(View.INVISIBLE);
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnStop.setVisibility(View.VISIBLE);
                mBtnComplete.setVisibility(View.INVISIBLE);
                mBtnPrintAgain.setVisibility(View.INVISIBLE);
                break;
            case PRINT_JOB_STATE_STOPPED:
                mBtnStart.setVisibility(View.VISIBLE);
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnStop.setVisibility(View.INVISIBLE);
                mBtnComplete.setVisibility(View.VISIBLE);
                mBtnPrintAgain.setVisibility(View.INVISIBLE);
                break;
            case PRINT_JOB_STATE_FINISHING:
            case PRINT_JOB_STATE_FINISHED:
                mBtnStart.setVisibility(View.INVISIBLE);
                mBtnPause.setVisibility(View.INVISIBLE);
                mBtnResume.setVisibility(View.INVISIBLE);
                mBtnStop.setVisibility(View.INVISIBLE);
                mBtnComplete.setVisibility(View.VISIBLE);
                mBtnPrintAgain.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void initPrintJob() {
        if (mViewModel.isMachineStatePrinting()) {
            subscribeExtraEvents();
            return;
        }

        final IFile printFile = getModel().getWorkspace().getPrintFile();
        if (printFile == null || !printFile.exists() || printFile.length() == 0) {
            FabConfirm.create(getContext())
                    .setDescription(R.string.print_warning_open_unable)
                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                        dialog.dismiss();
                        backToHome(HomeActivity.class);
                    })
                    .show();
            return;
        }

        // Wait 300ms until fragment is shown, then we can start printing
        AndroidSchedulers.mainThread().scheduleDirect(this::startPrint, 300, Constants.TIME_UNIT);
    }

    @SuppressLint("AutoDispose")
    private void subscribeExtraEvents() {
        // Watch errors from slave computer
        Disposable sub = getModel().getMachineController()
                .getFilamentObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isFilamentOut -> {
                    if (isFilamentOut) {
                        Logger.d("Filament out detected.");
                        getModel().getPrintController().pauseOnFilamentUsedOut();
//                        getModel().getPrintController().getTickCounter().stop();
                        handleFilamentRunOut(result -> {
                            if (result == HandleFilamentRunOutCallback.RESULT_CANCEL) {
                                Logger.d("Load canceled, ready to resume.");
                            }
                        });
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
                        getModel().getPrintController().pauseOnEnclosureDoorDetected();
                        getModel().getPrintController().getTickCounter().stop();
                        handleEnclosureDoorPaused();
                    }
                });
        mCompositeDisposable.add(sub);

        mViewModel.initAirPurifier();

        sub = mViewModel.listenResumeEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resume -> resumeFromChangeFilament());
        mCompositeDisposable.add(sub);

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
    private void startPrint() {
        mCompositeDisposable.clear();

        IPrintController printController = getModel().getPrintController();
        printController.setFile(getModel().getWorkspace().getPrintFile());
        printController.setTotalLines(getModel().getWorkspace().getFileTotalLineCount());

        subscribePrintEvent();

        // Power Panic
        boolean powerOutageFlag = getModel().getPrintController().getRecoveryFlag();

        if (powerOutageFlag) {
            mViewModel.requestPrintRecoverFromPowerLoss();
        } else {
            mViewModel.requestPrintStart();
        }

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
//                        getModel().getPrintController().getTickCounter().stop();
                        handleFilamentRunOut(result -> {
                            if (result == HandleFilamentRunOutCallback.RESULT_CANCEL) {
                                Logger.d("Load canceled, ready to resume.");
                            }
                        });
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
                        getModel().getPrintController().getTickCounter().stop();
                        handleEnclosureDoorPaused();
                    }
                });
        mCompositeDisposable.add(sub);

        mViewModel.initAirPurifier();

        sub = mViewModel.listenResumeEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resume -> resumeFromChangeFilament());
        mCompositeDisposable.add(sub);

        // Update
        sub = Observable.interval(5, TimeUnit.SECONDS)
                .takeUntil(tick -> (getModel().getPrintController().getPrintJobState().valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue())))
                .filter(tick -> mViewModel.isMachineStatePrinting())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    updateProgress();
                }, Throwable::printStackTrace);
        mCompositeDisposable.add(sub);
    }

    private void subscribePrintEvent() {
        Logger.d("Subscribe print event.");
        if (mPrintJobEventDisposable != null && !mPrintJobEventDisposable.isDisposed()) {
            mPrintJobEventDisposable.dispose();
        }
        mPrintJobEventDisposable = getModel().getPrintController().getPrintJobEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(printJobEvent -> {
                    int retCode = printJobEvent.getErrorCode();
                    switch (printJobEvent.getPrintEventState()) {
                        case REQUEST_START_SUCCESS:
                            Logger.i("Print started.");
                            break;
                        case REQUEST_START_FAILED:
                            if (retCode == 203) {
                                Logger.d("Unable to start printing, enclosure door open detected.");
                                handleEnclosureDoorPaused();
                                return;
                            }

                            if (retCode == 202) {
                                Logger.w("Filament used out, unable to start printing.");
                                // TODO
                                handleFilamentRunOut(result -> {
                                    if (result == HandleFilamentRunOutCallback.RESULT_CANCEL) {
                                        Logger.d("Load canceled, exiting.");
                                        back();
                                    }
                                });
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
                                handleEnclosureDoorPaused();
                                return;
                            }

                            if (retCode == 202) {
                                Logger.d("Filament used out, unable to resume printing.");
                                handleFilamentRunOut(null);
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
                            backToHome(HomeActivity.class);
                            break;
                        case REQUEST_STOP_FAILED:
                            Logger.w("Unable to stop printing, ret code %d", retCode);
                            FabConfirm.create(getContext())
                                    .setDescription(R.string.print_warning_stop_unable)
                                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                        dialog.dismiss();
                                        backToHome(HomeActivity.class);
                                    })
                                    .show();
                            break;
                        case PRINT_FINISH_SUCCESS:
                            Logger.i("Print Finished.");
                            Logger.d("Print job costs %s.", DateHelper.formatTime2(getModel().getPrintController().getTickCounter().getCount()));
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
                            mViewModel.clearErrorFlag();
                            break;
                        case REQUEST_POWER_LOSS_RECOVER_FAILED:
                            if (retCode == 203) {
                                handleEnclosureDoorPaused();
                                return;
                            }

                            if (retCode == 202) {
                                Logger.d("Filament used out, failed to recover from power loss.");
                                handleFilamentRunOut(result -> {
                                    if (result == HandleFilamentRunOutCallback.RESULT_CANCEL) {
                                        Logger.d("Load canceled, exiting.");
                                        // Clear power outage flag before exiting.
                                        getModel().getPrintController().setPowerOutageFlag(false);
                                        mViewModel.clearErrorFlag();
                                        getModel().getMachineController().clearPowerOutageFlag();
                                        back();
                                    }
                                });
                            } else {
                                Logger.w("Failed to recover from power loss.");
                                FabConfirm.create(getContext())
                                        .setDescription(R.string.print_warning_resume_unable)
                                        .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                            dialog.dismiss();
                                            getModel().getPrintController().setPowerOutageFlag(false);
                                            backToHome(HomeActivity.class);
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

    private void subscribePrintState() {
        Logger.d("Subscribe print state.");
        if (mPrintJobStateDisposable != null && !mPrintJobStateDisposable.isDisposed()) {
            mPrintJobStateDisposable.dispose();
        }

        mPrintJobStateDisposable = getModel().getPrintController().getPrintJobStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    Logger.d("current print job state is %d", state.getValue());

                    if (state.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_IDLE.getValue()) && mIsPrintPageSuspended) {
                        // Print already stopped, exiting the page.
                        backToHome(HomeActivity.class);
                        return;
                    }
                    if (state.valueEqual(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.getValue())) {
                        updateProgress();
                    }
                    updateButtonsByState(state);
                });
    }

    private void updateProgress() {
        float p = getModel().getPrintController().getProgress();

        // Add valid value check for progress, value must be between 0 to 1.
        p = Math.max(0f, Math.min(1.0f, p));

        // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
        int elapsed = getModel().getPrintController().getTickCounter().getCount();
        int remaining = (int) ((1 - p) * elapsed + (1 - p) * (1 - p) * mEstimatedTime);
        mTvRemainingTime.setText(DateHelper.formatTime2(remaining));

        final int percentage = (int) (100 * p);
        mCpvProgress.setPercentage(percentage);
        mTvProgress.setText(String.valueOf(percentage));
    }

    private void handleFilamentRunOut(@Nullable HandleFilamentRunOutCallback handleFilamentRunOutCallback) {
        Logger.i("Filament has run out.");

        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_warning_filament_runout_120x120)
                .setTitle(R.string.print_warning_filament_run_out_title)
                .setMessage(getString(R.string.print_warning_filament_run_out_content))
                .setPositive(R.string.print_ready_to_load, (dialog, which) -> {
                    Logger.d("Ready to load filament.");
                    dialog.dismiss();
                    if (handleFilamentRunOutCallback != null) {
                        handleFilamentRunOutCallback.onCallbackResult(HandleFilamentRunOutCallback.RESULT_LOAD);
                    }
                    PrintActivity activity = (PrintActivity) getContext();
                    if (activity != null) {
                        activity.gotoChangeFilamentFragment();
                    }
                })
                .setNegative(R.string.all_cancel, (dialog, which) -> {
                    dialog.dismiss();
                    if (handleFilamentRunOutCallback != null) {
                        handleFilamentRunOutCallback.onCallbackResult(HandleFilamentRunOutCallback.RESULT_CANCEL);
                    }
                })
                .show();
    }

    private void resumeFromChangeFilament() {
        int printState = getModel().getPrintController().getPrintState();

        if (printState == PrintViewModel.STATUS_IDLE) { // IDLE
            // Since we didn't start work yet, set the nozzle temperature back to 0°C.
            getModel().getSlaveComputer().sendGcode("M104 S0")
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> { /**/ });

            getModel().getMachineController().clearFilamentOutFlag();

            boolean powerOutageFlag = getModel().getPrintController().getRecoveryFlag();
            if (powerOutageFlag) {
                Logger.i("Complete change filament, recover printing.");
                getModel().getPrintController().recover();
            } else {
                Logger.i("Complete change filament, re-start previous job.");
                getModel().getPrintController().start();
            }
        } else if (printState == PrintViewModel.STATUS_PAUSED) { // PAUSE
            Logger.i("Complete change filament, resume previous job.");
            getModel().getMachineController().clearFilamentOutFlag();
            getModel().getPrintController().resume();
        }
    }

    private void handleEnclosureDoorPaused() {
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_warning_enclosure_120x120)
                .setTitle(R.string.print_warning_enclosure_door_detection_title)
                .setMessage(getString(R.string.print_warning_enclosure_door_detection_content))
                .setPositive(R.string.all_confirm, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void gotoOriginAndPrintAgain(boolean laserOffset) {
        final float currentZ = (float) getModel().getMachineController().getMachineStatus().z;
        Logger.i("Requesting go to origin...");
        if (getModel().getMachineController().getWorkType() == MachineController.WorkType.LASER) {
            getModel().getSlaveComputer().setPrintOffsetWithCrossLine(laserOffset)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(result -> {
                /*Do nothing*/
            });
        }
        if (getModel().getMachineController().isRotaryModuleAvailable()) {
            if (currentZ > 0) {
                // Engage direction, move X Y linear module and B rotary module first, then Z.
                getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000")
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 B0 F3000"))
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 Z0 F1800"))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(success -> {
                            initPrintJob();
                        }, e -> {
                            LogHelper.log(e);
                        });
            } else {
                // Retract direction, move Z linear module first, then X Y and B.
                getModel().getSlaveComputer().sendGcode("G1 Z0 F1800")
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000"))
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 B0 F3000"))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(success -> {
                            initPrintJob();
                        }, e -> {
                            LogHelper.log(e);
                        });
            }
        } else {
            if (currentZ > 0) {
                // Engage direction, move X Y linear module first, then Z.
                getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000")
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 Z0 F1800"))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(success -> {
                            initPrintJob();
                        }, e -> {
                            LogHelper.log(e);
                        });
            } else {
                // Retract direction, move Z linear module first, then X Y.
                getModel().getSlaveComputer().sendGcode("G1 Z0 F1800")
                        .flatMap(res -> getModel().getSlaveComputer().sendGcode("G1 X0 Y0 F3000"))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(success -> {
                            initPrintJob();
                        }, e -> {
                            LogHelper.log(e);
                        });
            }
        }
    }

    @OnClick(R.id.btn_print_start)
    void onClickStart() {
        if (mHeadType == Constants.HEAD_LASER_10W || mHeadType == Constants.HEAD_LASER_20W || mHeadType == Constants.HEAD_LASER_40W || mHeadType == Constants.HEAD_LASER_2W_IR) {
            // check Header Security Status before startPrint
            getModel().getPrintController().getHeaderSecurityStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(headerSecurity -> {
                        if (headerSecurity.status == 0) {
                            startPrint();
                        }
                    });
        } else {
            startPrint();
        }
    }

    @OnClick(R.id.btn_print_pause)
    void onClickPause() {
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setTitle(R.string.all_warning)
                .setMessage(R.string.print_warning_pause)
                .setPositive(R.string.all_pause, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.requestPrintPause();
                })
                .setNegative(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @OnClick(R.id.btn_print_resume)
    void onClickResume() {
        if (mHeadType == Constants.HEAD_LASER_10W
                || mHeadType == Constants.HEAD_LASER_20W
                || mHeadType == Constants.HEAD_LASER_40W
                || mHeadType == Constants.HEAD_LASER_2W_IR) {
            // check Header Security Status before resume
            getModel().getPrintController().getHeaderSecurityStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(headerSecurity -> {
                        if (headerSecurity.status == 0) {
                            mViewModel.requestPrintResume();
                        }
                    }, Logger::d);
        } else {
            mViewModel.requestPrintResume();
        }
    }

    @OnClick(R.id.btn_print_stop)
    void onClickStop() {
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setTitle(R.string.all_warning)
                .setMessage(R.string.print_warning_stop)
                .setPositive(R.string.all_yes, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.requestPrintStop();
                })
                .setNegative(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @OnClick(R.id.btn_print_complete)
    void onClickComplete() {
        backToHome(HomeActivity.class);
        // Log Firebase Analytics event
        FirebaseHelper.logFinishPrintEvent(getFirebaseAnalytics(), mHeadType);
    }

    @OnClick(R.id.btn_print_print_again)
    void onClickPrintAgain() {
        switch (mHeadType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                initPrintJob();
                break;
            }
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W: {
                gotoOriginAndPrintAgain(false);
                break;
            }
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W: {
                gotoOriginAndPrintAgain(getModel().getPreferences().getLaserIndicatorMode() == 0);
                break;
            }

            case Constants.HEAD_LASER_2W_IR: {
                gotoOriginAndPrintAgain(true);
                break;
            }
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W: {
                mViewDetailPanel3DP.setVisibility(View.GONE);
                mViewDetailPanel3DPDualExtruder.setVisibility(View.GONE);
                mViewDetailPanelLaser.setVisibility(View.GONE);
                mViewDetailPanelCNC.setVisibility(View.VISIBLE);
                gotoOriginAndPrintAgain(false);
                break;
            }
            default:
                break;
        }
    }

    interface HandleFilamentRunOutCallback {
        int RESULT_CANCEL = 0;
        int RESULT_LOAD = 1;

        void onCallbackResult(int result);
    }

    public void checkFastCalibration() {
        SingleSubject<Boolean> resultSubject = SingleSubject.create();

        mViewModel.checkFastCalibration().observeOn(AndroidSchedulers.mainThread()).as(bindToLifecycle()).subscribe(retCode -> {
            if (retCode == 0) { // success
                resultSubject.onSuccess(true);
            } else if (retCode == 1) { // failed
                mViewModel.coolDownHeatedBed();

                Logger.w("Fast Calibration failed, ret code %d", retCode);

                FabFullScreenDialog dialog = FabFullScreenDialog.create(getContext());
                dialog.setMessage(R.string.calibration_failed);
                dialog.setPositive(R.string.all_ok, (v, which) -> {
                    v.dismiss();
                    back();
                });
                dialog.show();
            } else if (retCode == 2) { // not calibrated yet
                mViewModel.coolDownHeatedBed();

                Logger.w("Machine is not calibrated yet, ret code %d", retCode);

                FabFullScreenDialog dialog = FabFullScreenDialog.create(getContext());
                dialog.setMessage(R.string.calibration_missing_notice);
                dialog.setPositive(R.string.all_yes, (v, which) -> {
                    v.dismiss();
                    resultSubject.onSuccess(true);
                });
                dialog.setNegative(R.string.all_no, (v, which) -> {
                    v.dismiss();
                    back();
                });
                dialog.show();
            }
        }, e -> {
            mViewModel.coolDownHeatedBed();
            LogHelper.log(e);
        });
    }

    private void disposeEventAndStateObservable() {
        Logger.d("Dispose events and states.");
        if (mPrintJobEventDisposable != null && !mPrintJobEventDisposable.isDisposed()) {
            mPrintJobEventDisposable.dispose();
        }

        if (mPrintJobStateDisposable != null && !mPrintJobStateDisposable.isDisposed()) {
            mPrintJobStateDisposable.dispose();
        }
    }
}
