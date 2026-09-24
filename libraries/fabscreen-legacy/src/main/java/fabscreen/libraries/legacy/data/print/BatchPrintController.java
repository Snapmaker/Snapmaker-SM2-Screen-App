package fabscreen.libraries.legacy.data.print;

import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class BatchPrintController implements IPrintController {
    private ISlaveComputer mSlaveComputer;

    // Batch
    private BatchCode[] mBatches;
    private final int BATCHES_NUMS = 3;
    // BATCHES_LENGTH = Maximum packet length - EventID - StartLine -EndLine
    private final int BATCHES_LENGTH = 512 - 1 - 4 - 4;

    private int mStartBatchNo;
    private int mEndBatchNo;
    private int mNowBatchNo;
    private BatchCode mBatchCode;

    private boolean mRecoveryFlag = false;
    private boolean mNeedOverrideInitialTemperature = false;

    private OnAirGcodeModifier mGcodeModifier = new OnAirGcodeModifier();
    @Deprecated
    private PrintListener mListener;
    private GcodePlayer mGcodePlayer = new GcodePlayer();
    private ModelBoundary mModelBoundary;
    private TickCounter mTickCounter;

    private PublishSubject<Boolean> mResumeSubject = PublishSubject.create();
    private BehaviorSubject<Integer> mDeprecatedPrintStateSubject = BehaviorSubject.createDefault(STATE_IDLE);

    private BehaviorSubject<MachinePrintJobState> mMachinePrintStateSubject = BehaviorSubject.createDefault(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
    private PublishSubject<PrintJobEvent> mPrintJobEventSubject = PublishSubject.create();

    private Disposable mPrintDisposable;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BatchPrintController(ISlaveComputer slaveComputer, TickCounter tickCounter) {
        mSlaveComputer = slaveComputer;
        mTickCounter = tickCounter;
        reset();
    }

    public BatchPrintController(IPrintController IPrintController) {
        Logger.d("Choosing batch mode.");
        mSlaveComputer = IPrintController.getSlaveComputer();
        mTickCounter = IPrintController.getTickCounter();
        reset();

        Disposable sub = mSlaveComputer.getMachineStatusObservable()
                .flatMap(status -> Observable.just(status.printerStatus))
                .distinctUntilChanged()
                .subscribe(printerStatus -> {
                    switch (printerStatus) {
                        case 0:
                            Logger.d("Machine is idle now.");
                            break;
                        case 1:
                        case 2:
                            break;
                        case 3:
                            Logger.d("Machine is printing now.");
                            break;
                        case 4:
                            Logger.d("Machine is pause now.");
                            break;
                        default:
                            break;
                    }
                });

                sub = mSlaveComputer.getMasterState()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(masterState -> {
//                            if (masterState.states != 0) return;
                            switch (masterState.mOperationId) {
                                case 0x04:
                                    if (masterState.states != 0) return;
                                    Logger.d("Master paused");
                                    mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
                                    mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
                                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_PAUSED_TRIGGERED_BY_CONTROLLER, 0));
//                                    mListener.onPauseSuccess();
                                    break;
                                case 0x07:
                                    Logger.d("Master finish");
                                    Logger.i("Finish print.");
                                    mDeprecatedPrintStateSubject.onNext(STATE_COMPLETED);
                                    if (masterState.states == 0) {
                                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_FINISHED);
//                                        mListener.onFinishSuccess();
                                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_FINISH_SUCCESS, masterState.states));
                                        mTickCounter.stop();
                                    } else {
//                                        mListener.onFinishFailed(masterState.states);
                                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_FINISH_FAILED, masterState.states));
                                    }
                                    break;
                            }
                        }, LogHelper::log);

                sub = mSlaveComputer.watchPrintPauseState().subscribe(state -> {
                    if (state == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
                    }
                }, LogHelper::log);
    }

    @Override
    public void reset() {
        mDeprecatedPrintStateSubject.onNext(STATE_IDLE);

        mGcodeModifier.reset();

        if (mPrintDisposable != null && !mPrintDisposable.isDisposed()) {
            mPrintDisposable.dispose();
            mPrintDisposable = null;
        }
    }

    private void prepare() {
        compositeDisposable.clear();
        clearBatch();
        compositeDisposable.add(
                mSlaveComputer.getBatchGcodeResponseSubject()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(batchGcodeResponse -> {
//                            Logger.d("batchGcodeResponse Request LineNo:" + batchGcodeResponse.getLineNo());
                            doNext(batchGcodeResponse.getLineNo());
                        }, LogHelper::log)
        );
    }

    /**
     * Start printing.
     * <p>
     * When start success, We empty the Batch cache and wait for the master request
     */
    @Override
    public void start() {
        Logger.i("Start print.");

        prepare();
        // Fixme: Re-consider prepare print logic and adjust settings definition.
        Disposable sub = mSlaveComputer.requestAdjustSettingLaserPower(getOverrideLaserPower()).subscribe();
        setActionDisposable(sub);
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_STARTING);
        sub = mSlaveComputer.start()
                .flatMap(retCode -> {
                    // After the master control status changes successfully, request the master control to start batch printing
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        return mSlaveComputer.useBatchGcodeMode(1)
                                .onExceptionResumeNext(Observable.just(-1));
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
                        return Observable.just(retCode);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
//                        mListener.onStartSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_SUCCESS, retCode));
                        mTickCounter.reset();
                        mTickCounter.start();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
//                        mListener.onStartFailed(retCode);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onStartFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    /**
     * Resume printing.
     */
    @Override
    public void pause() {
        Logger.i("Pause print.");
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSING);
        Disposable sub = getSlaveComputer().pause()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
//                        mListener.onPauseSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_SUCCESS, retCode));
                        mTickCounter.stop();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
//                        mListener.onPauseFailed(retCode);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onPauseFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    /**
     * Resume printing.m
     */
    @Override
    public void resume() {
        Logger.i("Resume print.");
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_RESUMING);
        Disposable sub = mSlaveComputer.resume().flatMap(retCode -> {
            // After the master control status changes successfully, request the master control to start batch printing
            if (retCode == 0) {
                mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                return mSlaveComputer.useBatchGcodeMode(1).onExceptionResumeNext(Observable.just(-1));
            } else {
                mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
                return Observable.just(retCode);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
//                        mListener.onResumeSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_RESUME_SUCCESS, retCode));
                        mTickCounter.start();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
//                        mListener.onResumeFailed(retCode);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_RESUME_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onResumeFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_RESUME_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    /**
     * Stop printing.
     */
    @Override
    public void stop() {
        Logger.i("Stop print.");
        MachinePrintJobState lastState = mMachinePrintStateSubject.getValue();
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_STOPPING);
        Disposable sub = mSlaveComputer.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mDeprecatedPrintStateSubject.onNext(STATE_IDLE);
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
//                        mListener.onStopSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_STOP_SUCCESS, retCode));
                        mTickCounter.stop();
                    } else {
                        mMachinePrintStateSubject.onNext(lastState);
//                        mListener.onStopFailed(retCode);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_STOP_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onStopFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_STOP_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    /**
     * Recover print from Power-Loss.
     */
    @Override
    public void recover() {
        Logger.i("Recover print.");
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_RECOVERING);
        prepare();
        Disposable sub = mSlaveComputer.resumeFromPowerOutage().observeOn(AndroidSchedulers.mainThread())
                .flatMap(retCode -> {
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        return mSlaveComputer.useBatchGcodeMode(1).onExceptionResumeNext(Observable.just(-1));
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
                        return Observable.just(retCode);
                    }
                })
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
//                        mListener.onResumeFromPowerOutageSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_POWER_LOSS_RECOVER_SUCCESS, retCode));
                        mTickCounter.load();
                        mTickCounter.start();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
//                        mListener.onResumeFromPowerOutageFailed(retCode);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_POWER_LOSS_RECOVER_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onResumeFromPowerOutageFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_POWER_LOSS_RECOVER_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    /**
     * Finish printing (Hit to file end).
     */
    @Override
    public void finish() {
        Logger.i("Finish print.");
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_FINISHING);
        Disposable sub = mSlaveComputer.finish()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_FINISHED);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_FINISH_SUCCESS, retCode));
                        mTickCounter.stop();
                    } else {
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_FINISH_FAILED, retCode));

                    }
                }, e -> {
                    LogHelper.log(e);
//                    mListener.onFinishFailed(1);
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.PRINT_FINISH_FAILED, 1));
                });
        setActionDisposable(sub);
    }


    @Deprecated
    @Override
    public Observable<Integer> getPauseState() {
        return mSlaveComputer.watchPrintPauseState();
    }

    @Override
    public void masterPause() {
        mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
//        mListener.onPauseSuccess();
    }

    @Override
    public Observable<MachinePrintJobState> getPrintJobStateObservable() {
        return mMachinePrintStateSubject.hide();
    }

    @Override
    public Observable<PrintJobEvent> getPrintJobEventObservable() {
        return mPrintJobEventSubject.hide();
    }

    /**
     * Actual next function, send G-code or resend G-code.
     * Query batch corresponding to the start line number of the output based on the line number
     * of the master control request.
     *
     * @param lineNo
     */
    private void doNext(int lineNo) {
        if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
            return;
        }

        mBatchCode = new BatchCode("", lineNo, lineNo);
        StringBuffer linesBuffer = new StringBuffer();
        int batchLength = 0;

        if (lineNo < mStartBatchNo) {
            Logger.d("Abnormal: LineNo %s < mStartBatchNo %s,mEndBatchNo:%s", lineNo, mStartBatchNo, mEndBatchNo);
            clearBatch();
            doNext(lineNo);
            return;
        }

        if (lineNo > mEndBatchNo) {
            // Not starting at line 0 is supported
            if (mNowBatchNo == -1) {
                mGcodePlayer.setLineno(lineNo);
                mGcodePlayer.nextLine();
            }

            while (mGcodePlayer.getLineNo() < lineNo) {
                mGcodePlayer.nextLine();
            }
            // Failed to obtain G-code, complete the task
            if (mGcodePlayer.getLine() == null) {
                send();
                return;
            }


            while (batchLength < BATCHES_LENGTH) {
                String line = mGcodePlayer.getLine();
                if (mGcodePlayer.getLine() == null) {
                    // Failed to obtain G-code, complete the task, break
                    break;
                }

                // Rewrite the requirements only once
                if (mNeedOverrideInitialTemperature && !mGcodeModifier.IsAllInitialCodeOverride()) {
                    line = mGcodeModifier.newOverride(line);
                    // Override flag will reset if all initial temperature G-code was override.
                    mNeedOverrideInitialTemperature = !mGcodeModifier.IsAllInitialCodeOverride();
                }

                // Replace comments in G-code code
                int index = line.indexOf(";");
                if (index != -1) {
                    line = line.substring(0, index + 1);
                }
                line += "\n";
                batchLength += line.getBytes().length;
                if (batchLength < BATCHES_LENGTH) {
                    linesBuffer.append(line);
                    mGcodePlayer.nextLine();
                }
            }

            String tempBatch = linesBuffer.toString();
            mNowBatchNo = lineNo;
            mEndBatchNo = mGcodePlayer.getLineNo() - 1;
            mBatchCode = new BatchCode(tempBatch, mNowBatchNo, mEndBatchNo);
            int batchIndex = (getBatch(lineNo - 1) + 1) % BATCHES_NUMS;
            mBatches[batchIndex] = mBatchCode;
        } else {
            //  Reissue or resend
            int batchIndex = getBatch(lineNo);
            // Failed to find the required batch
            if (batchIndex == -1) {
                Logger.d("Error: Batch can't find :" + lineNo);
                send();
                return;
            }
            BatchCode tempBatch = mBatches[batchIndex];
            String[] tempGcodes = tempBatch.getGcodes().split("\n");
            for (int i = lineNo - tempBatch.getStartNo(); i < tempGcodes.length; i++) {
                linesBuffer.append(tempGcodes[i]).append("\n");
            }
            mBatchCode = new BatchCode(linesBuffer.toString(), lineNo, tempBatch.getEndNo());
            mBatches[batchIndex] = mBatchCode;
        }
        send();
    }

    private void clearBatch() {
        mBatches = new BatchCode[BATCHES_NUMS];
        mEndBatchNo = -1;
        mNowBatchNo = -1;
        mStartBatchNo = -1;
    }

    private void send() {
        mStartBatchNo = getStartBatchNo();
        mSlaveComputer.sendPrintBatchGcode(mBatchCode.getStartNo(), mBatchCode.getEndNo(), mBatchCode.getGcodes());
    }

    /**
     * Find the batch with the smallest current line number
     */
    private int getStartBatchNo() {
        int batchNo = -1;
        for (int i = 0; i < mBatches.length; i++) {
            if (mBatches[i] == null) {
                continue;
            }
            batchNo = batchNo == -1 ? mBatches[i].getStartNo() : Math.min(batchNo, mBatches[i].getStartNo());
        }
        return batchNo;
    }

    /**
     * Find the batch containing the line number based on the line number
     */
    private int getBatch(int lineNo) {
        for (int i = 0; i < mBatches.length; i++) {
            if (mBatches[i] == null) {
                continue;
            }
            if (mBatches[i].getStartNo() <= lineNo && mBatches[i].getEndNo() >= lineNo) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Set `disposable` as current active disposable.
     * <p>
     * In our new implementation, there is only one action disposable is allowed to be active.
     *
     * @param disposable Disposable to be set
     */

    @Override
    public void setActionDisposable(Disposable disposable) {
        if (mPrintDisposable != null && !mPrintDisposable.isDisposed()) {
            mPrintDisposable.dispose();
            mPrintDisposable = null;
        }
        mPrintDisposable = disposable;
    }

    @Override
    public ISlaveComputer getSlaveComputer() {
        return mSlaveComputer;
    }

    @Override
    public Observable<Integer> getPrintStatusObservable() {
        return mDeprecatedPrintStateSubject.hide();
    }

    @Override
    public MachinePrintJobState getPrintJobState() {
        return mMachinePrintStateSubject.getValue();
    }

    @Override
    public void setListener(PrintListener listener) {
        mListener = listener;
    }

    @Override
    public int getProgressCount() {
        return mGcodePlayer.getProgressCount();
    }

    @Override
    public float getProgress() {
        return mGcodePlayer.getProgress();
    }

    @Override
    public void setFile(IFile file) {
        mGcodePlayer.setGcodeFile(file);
    }

    @Override
    public void setTotalLines(int lines) {
        mGcodePlayer.setTotalCount(lines);
    }

    /**
     * Pause when filament sensor triggered that filament used out.
     */
    @Override
    public void pauseOnFilamentUsedOut() {
        // Controller is already in pause status automatically, don't send pause command.
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSING);
        AndroidSchedulers.mainThread().scheduleDirect(this::delaySetFilamentPauseState, 30, TimeUnit.SECONDS);
        mTickCounter.stop();
    }

    public void delaySetFilamentPauseState() {
        mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
    }

    @Override
    public void pauseOnEnclosureDoorDetected() {
        if (mDeprecatedPrintStateSubject.getValue() == STATE_PRINTING) {
            // Controller is already in pause status automatically, don't send pause command.
            mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
        }
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);

    }

    @Override
    public void onEmergencyStop() {
        reset();
        if (mGcodePlayer != null) {
            mGcodePlayer.reset();
        }
    }

    @Override
    public boolean getRecoveryFlag() {
        return mRecoveryFlag;
    }

    @Override
    public float getOverrideInitialHeatedBedTemperature() {
        return mGcodeModifier.getOverrideInitialHeatedBedTemperature();
    }

    @Override
    public void setPowerOutageFlag(boolean flag) {
        mRecoveryFlag = flag;
    }

    @Override
    public Observable<Boolean> getResumeObservable() {
        return mResumeSubject;
    }

    @Override
    public Integer getPrintState() {
        return mDeprecatedPrintStateSubject.getValue();
    }

    @Override
    public boolean getInitialM190Flag() {
        return mGcodeModifier.getInitialM190Marker();
    }

    @Override
    public float getOverrideFeedRate() {
        return mGcodeModifier.getOverrideFeedRate();
    }

    @Override
    public float getOverrideInitialNozzleTemperature() {
        return mGcodeModifier.getOverrideInitialNozzleTemperature();
    }

    @Override
    public float getOverrideInitialNozzleTemperature(int which) {
        return mGcodeModifier.getOverrideInitialNozzleTemperature(which);
    }

    @Override
    public boolean getInitialM109Flag() {
        return mGcodeModifier.getInitialM109Marker();
    }

    @Override
    public float getOverrideZOffset() {
        return mGcodeModifier.getOverrideZOffset();
    }

    @Override
    public float getOverrideZOffset(int which) {
        return mGcodeModifier.getOverrideZOffset(which);
    }

    @Override
    public void setModelBoundary(ModelBoundary boundary) {
        mModelBoundary = boundary;
    }

    @Override
    public ModelBoundary getModelBoundary() {
        return mModelBoundary;
    }

    @Override
    public void setResume() {
        mResumeSubject.onNext(true);
    }

    @Override
    public void disposeAll() {
        dispose(compositeDisposable);
        dispose(mPrintDisposable);
    }

    private void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    public TickCounter getTickCounter() {
        return mTickCounter;
    }

    @Override
    public Observable<FabPacketContent.HeaderSecurity> getHeaderSecurityStatus() {
        return mSlaveComputer.requestHeaderSecurityStatus();
    }

    @Override
    public int getTotalLines() {
        return mGcodePlayer.getTotalCount();
    }

    @Override
    public void setOverrideFeedRate(float feedRate) {
        mGcodeModifier.setOverrideFeedRate(feedRate);
        UpdateChange();
    }

    @Override
    public void setOverrideZOffset(float zOffset) {
        mGcodeModifier.setOverrideZOffset(zOffset);
        UpdateChange();
    }

    @Override
    public void setOverrideZOffset(int which, float zOffset) {
        mGcodeModifier.setOverrideZOffset(which, zOffset);
        UpdateChange();
    }

    @Override
    public void setOverrideFlowRate(int which, float flowRate) {
        if (which == 0) {
            mGcodeModifier.setOverrideLeftFlowRate(flowRate);
        } else {
            mGcodeModifier.setOverrideRightFlowRate(flowRate);
        }
        UpdateChange();
    }

    @Override
    public void setOverrideHeatedBedTemperature(float temp) {
        mGcodeModifier.setOverrideHeatedBedTemperature(temp);
        UpdateChange();
    }

    @Override
    public void setOverrideLaserPower(float power) {
        mGcodeModifier.setOverrideLaserPower(power);
        UpdateChange();
    }

    @Override
    public void setOverrideNozzleTemperature(float temp) {
        mGcodeModifier.setOverrideNozzleTemperature(temp);
        UpdateChange();
    }

    @Override
    public void setOverrideNozzleTemperature(int which, float temp) {
        mGcodeModifier.setOverrideNozzleTemperature(which, temp);
        UpdateChange();
    }

    @Override
    public void setOverrideInitialNozzleTemperature(float temp) {
        // only for preview
        mGcodeModifier.setOverrideInitialNozzleTemperature(temp);
    }

    @Override
    public void setOverrideInitialNozzleTemperature(int which, float temp) {
        mGcodeModifier.setOverrideInitialNozzleTemperature(which, temp);
    }

    @Override
    public void setOverrideInitialNozzleTemperature(float temp, boolean isUserModified) {
        mGcodeModifier.setOverrideInitialNozzleTemperature(temp);
        mNeedOverrideInitialTemperature = isUserModified;
    }

    @Override
    public void setOverrideInitialHeatedBedTemperature(float temp) {
        // only for preview
        mGcodeModifier.setOverrideInitialHeatedBedTemperature(temp);
    }

    @Override
    public void setOverrideInitialHeatedBedTemperature(float temp, boolean isUserModified) {
        mGcodeModifier.setOverrideInitialHeatedBedTemperature(temp);
        mNeedOverrideInitialTemperature = isUserModified;
    }

    private void UpdateChange() {
        if (!MachinePrintJobState.isStatePrinting(mMachinePrintStateSubject.getValue().getValue())) {
            return;
        }
        Observable<Integer> action = mGcodeModifier.getModifyAction(mSlaveComputer);
        if (action != null) {
            compositeDisposable.add(action.subscribe());
        }
    }

    @Override
    public boolean getOverrideNozzleTemperatureDirty() {
        return mGcodeModifier.getOverrideNozzleTemperatureDirty();
    }

    @Override
    public float getOverrideNozzleTemperature() {
        return mGcodeModifier.getOverrideNozzleTemperature();
    }

    @Override
    public float getOverrideNozzleTemperature(int which) {
        return mGcodeModifier.getOverrideNozzleTemperature(which);
    }

    @Override
    public boolean getOverrideHeatedBedTemperatureDirty() {
        return mGcodeModifier.getOverrideHeatedBedTemperatureDirty();
    }

    @Override
    public float getOverrideHeatedBedTemperature() {
        return mGcodeModifier.getOverrideHeatedBedTemperature();
    }

    @Override
    public boolean getOverrideLaserPowerDirty() {
        return mGcodeModifier.getOverrideLaserPowerDirty();
    }

    @Override
    public float getOverrideLaserPower() {
        return mGcodeModifier.getOverrideLaserPower();
    }

}
