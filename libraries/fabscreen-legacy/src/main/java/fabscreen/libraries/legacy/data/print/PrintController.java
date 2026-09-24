package fabscreen.libraries.legacy.data.print;

import android.os.SystemClock;

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
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;


public class PrintController implements IPrintController {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ISlaveComputer mSlaveComputer;

    private BehaviorSubject<Integer> mDeprecatedPrintStateSubject = BehaviorSubject.createDefault(STATE_IDLE);

    private GcodePlayer mGcodePlayer = new GcodePlayer();
    private PrintListener mListener;

    private OnAirGcodeModifier mGcodeModifier = new OnAirGcodeModifier();
    private TickCounter mTickCounter;

    private boolean mPrintResendFlag = false;
    private long mPrintLastReplyTime;
    private Disposable mPrintDisposable;
    private Disposable mPrintGcodeCheckSubscription;

    private PublishSubject<Boolean> mNextSubject = PublishSubject.create();

    // FIXME: This is a temporary workaround.
    //  This subject is used in ChangeFilamentFragment, to inform PrintFragment that
    //  the filament has been changed, and jumping back.
    //  Solution: We should have a ViewModel for PrintFragment to deal with filament run out resume.
    private PublishSubject<Boolean> mResumeSubject = PublishSubject.create();
    private boolean mRecoveryFlag = false;

    private boolean mPauseDirty = false;
    private boolean mStopDirty = false;

    private ModelBoundary mModelBoundary;

    private BehaviorSubject<MachinePrintJobState> mMachinePrintStateSubject = BehaviorSubject.createDefault(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
    private PublishSubject<PrintJobEvent> mPrintJobEventSubject = PublishSubject.create();


    public PrintController(ISlaveComputer slaveComputer, TickCounter tickCounter) {
        mSlaveComputer = slaveComputer;
        mTickCounter = tickCounter;

        init();

        reset();
    }

    public PrintController(IPrintController IPrintController) {
        Logger.d("Choosing line mode.");
        mSlaveComputer = IPrintController.getSlaveComputer();
        mTickCounter = IPrintController.getTickCounter();
        reset();
    }

    private void init() {
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

        sub = mSlaveComputer.watchPrintPauseState().subscribe(state -> {
            if (state == 0) {
                mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
            }
        }, LogHelper::log);
    }

    /**
     * Reset all.
     */
    @Override
    public void reset() {
        mDeprecatedPrintStateSubject.onNext(STATE_IDLE);

        mGcodeModifier.reset();

        if (mPrintDisposable != null && !mPrintDisposable.isDisposed()) {
            mPrintDisposable.dispose();
            mPrintDisposable = null;
        }

        if (mPrintGcodeCheckSubscription != null && !mPrintGcodeCheckSubscription.isDisposed()) {
            mPrintGcodeCheckSubscription.dispose();
            mPrintGcodeCheckSubscription = null;
        }
    }

    private void prepare() {
        compositeDisposable.clear();

        // Deliver next to IO scheduler
        compositeDisposable.add(
                mNextSubject
                        .observeOn(Schedulers.io())
                        .subscribe(t -> doNext(), LogHelper::log)
        );

        // Make connections
        compositeDisposable.add(
                getSlaveComputer().watchWaitEvents()
                        .observeOn(Schedulers.io())
                        .subscribe(event -> doResend(), LogHelper::log)
        );

        // Add a checker: If print G-code request is not replied in 5 seconds,
        // log a warning for debugging. Note that this will report M109 as well.
        mPrintGcodeCheckSubscription = Observable.interval(10, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .subscribe(tick -> {
                    if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
                        return;
                    }

                    long time = SystemClock.elapsedRealtime();
                    // 20s
                    if (time - mPrintLastReplyTime > 20000) {
                        String line = mGcodePlayer.getLine();
                        int lineNo = mGcodePlayer.getLineNo();
                        Logger.w("No reply for %s ms since last response (#%d: %s)", time - mPrintLastReplyTime, lineNo, line);
                    }
                }, LogHelper::log);
    }

    @Override
    public ISlaveComputer getSlaveComputer() {
        return mSlaveComputer;
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
    public Integer getPrintState() {
        return mDeprecatedPrintStateSubject.getValue();
    }

    @Override
    public Observable<Integer> getPrintStatusObservable() {
        return mDeprecatedPrintStateSubject.hide();
    }

    @Override
    public void setFile(IFile file) {
        mGcodePlayer.setGcodeFile(file);
    }

    @Override
    public boolean getRecoveryFlag() {
        return mRecoveryFlag;
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
    public void setResume() {
        mResumeSubject.onNext(true);
    }

    @Override
    public TickCounter getTickCounter() {
        return mTickCounter;
    }

    /**
     * Start printing.
     * <p>
     * When start success, we reset line number and call next() to start sending G-code.
     */
    @Override
    public void start() {
//        if (mDeprecatedPrintStateSubject.getValue() != STATE_IDLE) {
//            Logger.w("Start state is not idle");
//            mListener.onStartFailed(1); // unexpected state
//            return;
//        }

        Logger.i("Start print.");
        prepare();

        // Start printing
        Disposable sub = mSlaveComputer.start()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_SUCCESS, retCode));
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        if (mListener != null) {
                            mListener.onStartSuccess();
                        }
                        mTickCounter.reset();
                        mTickCounter.start();

                        mGcodePlayer.setLineno(0);
                        next();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
                        if (mListener != null) {
                            mListener.onStartFailed(retCode);
                        }
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_FAILED, retCode));
                    }
                }, e -> {
                    LogHelper.log(e);
                    if (mListener != null) {
                        mListener.onStartFailed(1);
                    }
                    mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_START_FAILED, 1));
                });
        setActionDisposable(sub);
    }

    @Override
    public void pause() {
//        if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
//            mListener.onPauseFailed(1); // unexpected state
//            return;
//        }

        Logger.i("Pause print.");

        mPauseDirty = true;
    }

    @Override
    public void masterPause() {
        mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
        mListener.onPauseSuccess();
    }

    @Override
    public Observable<MachinePrintJobState> getPrintJobStateObservable() {
        // TODO
        return mMachinePrintStateSubject.hide();
    }

    @Override
    public MachinePrintJobState getPrintJobState() {
        return mMachinePrintStateSubject.getValue();
    }

    @Override
    public Observable<PrintJobEvent> getPrintJobEventObservable() {
        return mPrintJobEventSubject.hide();
    }

    /**
     * Pause when filament sensor triggered that filament used out.
     */
    @Override
    public void pauseOnFilamentUsedOut() {
        // Controller is already in pause status automatically, don't send pause command.
        mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
    }

    @Override
    public void pauseOnEnclosureDoorDetected() {
        if (mDeprecatedPrintStateSubject.getValue() == STATE_PRINTING) {
            // Controller is already in pause status automatically, don't send pause command.
            mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);
        }
    }

    /**
     * Resume printing.
     */
    @Override
    public void resume() {
//        if (mDeprecatedPrintStateSubject.getValue() != STATE_PAUSED) {
//            mListener.onResumeFailed(1); // unexpected state
//            return;
//        }
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_RESUMING);
        Logger.i("Resume print.");

        Disposable sub = mSlaveComputer.getLineNumber()
                .observeOn(Schedulers.computation())
                .flatMap(lineNo -> {
                    mGcodePlayer.setLineno(lineNo);
                    return mSlaveComputer.resume();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_RESUME_SUCCESS, retCode));
                        mTickCounter.start();

//                        mListener.onResumeSuccess();

                        next();
                    } else {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_RESUME_FAILED, retCode));

//                        mListener.onResumeFailed(retCode);
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
//        if (mDeprecatedPrintStateSubject.getValue() == STATE_IDLE) {
//            mListener.onStopFailed(1); // unexpected state
//            return;
//        }

        Logger.i("Stop print.");
        MachinePrintJobState lastState = mMachinePrintStateSubject.getValue();
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_STOPPING);
        if (MachinePrintJobState.isStatePrinting(getPrintJobState().getValue())) {
            mStopDirty = true;
        } else {
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
//                        mListener.onStopFailed(1);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_STOP_FAILED, 1));
                    });
            setActionDisposable(sub);
        }
    }

    /**
     * Finish printing (Hit to file end).
     */
    @Override
    public void finish() {
//        if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
//            mListener.onFinishFailed(1); // unexpected state
//            return;
//        }
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_FINISHING);
        mDeprecatedPrintStateSubject.onNext(STATE_COMPLETED);

        Logger.i("Finish print.");

        Disposable sub = mSlaveComputer.finish()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_IDLE);
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

    /**
     * Recover print from Power-Loss.
     */
    @Override
    public void recover() {
//        if (mDeprecatedPrintStateSubject.getValue() != STATE_IDLE) {
//            mListener.onResumeFromPowerOutageFailed(1); // unexpected state
//            return;
//        }

        Logger.i("Recover print.");
        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_RECOVERING);
        prepare();

        Disposable sub = mSlaveComputer.getLineNumber()
                .observeOn(Schedulers.computation())
                .flatMap(lineno -> {
                    Logger.i("Recover from Power-Loss, line number = %d", lineno);
                    mGcodePlayer.setLineno(lineno);
                    return mSlaveComputer.resumeFromPowerOutage();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(retCode -> {
                    Logger.i("Recover API return code = %d", retCode);
                    if (retCode == 0) {
                        mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                        mDeprecatedPrintStateSubject.onNext(STATE_PRINTING);
//                        mListener.onResumeFromPowerOutageSuccess();
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_POWER_LOSS_RECOVER_SUCCESS, retCode));
                        mTickCounter.load();
                        mTickCounter.start();

                        next();
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
     * previous G-code are replied, ask for next line.
     */
    private void next() {
        mNextSubject.onNext(true);
    }

    /**
     * Actual next function, send G-code or resend G-code.
     * <p>
     * Note that this function is running under IO scheduler.
     */
    private void doNext() {
        if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
            return;
        }

        if (executeActions()) {
            return;
        }

        Disposable sub;

        // action
        Observable<Integer> action = mGcodeModifier.getModifyAction(mSlaveComputer);
        if (action != null) {
            sub = action.subscribe(retCode -> next(), LogHelper::log);
            setActionDisposable(sub);
            return;
        }

        String line;
        int lineNo;
        if (mPrintResendFlag) {
            mPrintResendFlag = false;
            line = mGcodePlayer.getLine();
            lineNo = mGcodePlayer.getLineNo();
            Logger.d("Handle wait event, sending current G-code... #%d: %s", lineNo, line);
        } else {
            line = mGcodePlayer.nextLine();
            lineNo = mGcodePlayer.getLineNo();
        }
        if (line != null) {
            String newLine = mGcodeModifier.override(line);

            if (newLine.isEmpty()) {
                mGcodePlayer.skipLine();
                next();
            } else {
                sub = mSlaveComputer.sendPrintGcode(newLine, lineNo)
                        .observeOn(Schedulers.io())
                        .subscribe(this::onAck, LogHelper::log);
                setActionDisposable(sub);
            }
        } else {
            mPrintLastReplyTime = SystemClock.elapsedRealtime();
            mGcodePlayer.onAck();

            finish();
        }
    }

    /**
     * Handle with wait event, decide whether to resend G-code or to send next G-code.
     * <p>
     * Note that this function is running under IO scheduler.
     */
    private void doResend() {
        if (mDeprecatedPrintStateSubject.getValue() != STATE_PRINTING) {
            return;
        }

        long time = SystemClock.elapsedRealtime();
        if (time - mPrintLastReplyTime < 1000) {
            // Discard the wait event since we thought ack is not lost yet.
            return;
        }

        int sent = mGcodePlayer.getSentCount();
        int recv = mGcodePlayer.getReceivedCount();
        if (sent == recv) {
            // been ACKed, send next G-code line
            int lineNo = mGcodePlayer.getLineNo();
            Logger.d("Handle wait event, sending next G-code... (previous line #%d)", lineNo);
            next();
        } else {
            // not been ACKed, resend
            mPrintResendFlag = true;
            next();
        }
    }

    private boolean executeActions() {
        if (mStopDirty) {
            mStopDirty = false;
            MachinePrintJobState lastState = mMachinePrintStateSubject.getValue();
            Disposable sub = getSlaveComputer().stop()
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
//                        mListener.onStopFailed(1);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_STOP_FAILED, 1));
                    });
            setActionDisposable(sub);
            return true;
        }
        if (mPauseDirty) {
            mPauseDirty = false;
            Disposable sub = getSlaveComputer().pause()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(retCode -> {
                        if (retCode == 0) {
                            mDeprecatedPrintStateSubject.onNext(STATE_PAUSED);

                            mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PAUSED);
                            mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_SUCCESS, retCode));
//                            mListener.onPauseSuccess();
                            mTickCounter.stop();
                        } else {
                            mMachinePrintStateSubject.onNext(MachinePrintJobState.PRINT_JOB_STATE_PRINTING);
                            mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_FAILED, retCode));
//                            mListener.onPauseFailed(retCode);
                        }
                    }, e -> {
                        LogHelper.log(e);
//                        mListener.onPauseFailed(1);
                        mPrintJobEventSubject.onNext(new PrintJobEvent(PrintEventState.REQUEST_PAUSE_FAILED, 1));
                    });
            setActionDisposable(sub);
            return true;
        }

        return false;
    }

    /**
     * Note that this function is running under IO scheduler.
     */
    private void onAck(FabPacketContent.GcodeResponse response) {
        mPrintLastReplyTime = SystemClock.elapsedRealtime();

        mGcodePlayer.onAck();
        next();
    }

    @Override
    public void onEmergencyStop() {
        reset();
        if (mGcodePlayer != null) {
            mGcodePlayer.reset();
        }
    }

    @Override
    public void setListener(PrintListener listener) {
        mListener = listener;
    }

    @Override
    public float getProgress() {
        return mGcodePlayer.getProgress();
    }

    @Override
    public int getProgressCount() {
        return mGcodePlayer.getProgressCount();
    }

    @Override
    public void setTotalLines(int lines) {
        mGcodePlayer.setTotalCount(lines);
    }

    @Override
    public int getTotalLines() {
        return mGcodePlayer.getTotalCount();
    }

    @Override
    public void setModelBoundary(ModelBoundary boundary) {
        mModelBoundary = boundary;
    }

    @Override
    public ModelBoundary getModelBoundary() {
        return mModelBoundary;
    }

    //region G-code Modifier
    public OnAirGcodeModifier getModifier() {
        return mGcodeModifier;
    }

    @Override
    public float getOverrideFeedRate() {
        return mGcodeModifier.getOverrideFeedRate();
    }

    @Override
    public void setOverrideFeedRate(float feedRate) {
        mGcodeModifier.setOverrideFeedRate(feedRate);
    }

    @Override
    public float getOverrideZOffset() {
        return mGcodeModifier.getOverrideZOffset();
    }

    @Override
    public float getOverrideZOffset(int which) {
        return 0;
    }

    @Override
    public void setOverrideZOffset(float zOffset) {
        mGcodeModifier.setOverrideZOffset(zOffset);
    }

    @Override
    public void setOverrideZOffset(int which, float zOffset) {
        mGcodeModifier.setOverrideZOffset(which, zOffset);
    }

    @Override
    public void setOverrideFlowRate(int which, float flowRate) {
        if (which == 0) {
            mGcodeModifier.setOverrideLeftFlowRate(flowRate);
        } else {
            mGcodeModifier.setOverrideRightFlowRate(flowRate);
        }

    }

    @Override
    public float getOverrideNozzleTemperature() {
        return mGcodeModifier.getOverrideNozzleTemperature();
    }

    @Override
    public float getOverrideNozzleTemperature(int which) {
        return 0;
    }

    @Override
    public boolean getOverrideNozzleTemperatureDirty() {
        return mGcodeModifier.getOverrideNozzleTemperatureDirty();
    }

    @Override
    public void setOverrideNozzleTemperature(float temp) {
        mGcodeModifier.setOverrideNozzleTemperature(temp);
    }

    @Override
    public void setOverrideNozzleTemperature(int which, float temp) {
        mGcodeModifier.setOverrideNozzleTemperature(which, temp);
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
    public void setOverrideInitialNozzleTemperature(float temp) {
        mGcodeModifier.setOverrideInitialNozzleTemperature(temp);
    }

    @Override
    public void setOverrideInitialNozzleTemperature(int which, float temp) {
        mGcodeModifier.setOverrideInitialNozzleTemperature(which, temp);
    }

    @Override
    public void setOverrideInitialNozzleTemperature(float temp, boolean isUserModified) {
        mGcodeModifier.setOverrideInitialNozzleTemperature(temp);
    }

    @Override
    public boolean getInitialM109Flag() {
        return mGcodeModifier.getInitialM109Marker();
    }

    @Override
    public float getOverrideHeatedBedTemperature() {
        return mGcodeModifier.getOverrideHeatedBedTemperature();
    }

    @Override
    public boolean getOverrideHeatedBedTemperatureDirty() {
        return mGcodeModifier.getOverrideHeatedBedTemperatureDirty();
    }

    @Override
    public boolean getInitialM190Flag() {
        return mGcodeModifier.getInitialM190Marker();
    }

    @Override
    public void setOverrideHeatedBedTemperature(float temp) {
        mGcodeModifier.setOverrideHeatedBedTemperature(temp);
    }

    @Override
    public float getOverrideInitialHeatedBedTemperature() {
        return mGcodeModifier.getOverrideInitialHeatedBedTemperature();
    }

    @Override
    public void setOverrideInitialHeatedBedTemperature(float temp) {
        mGcodeModifier.setOverrideInitialHeatedBedTemperature(temp);
    }

    @Override
    public void setOverrideInitialHeatedBedTemperature(float temp, boolean isUserModified) {
        mGcodeModifier.setOverrideInitialHeatedBedTemperature(temp);
    }

    @Override
    public float getOverrideLaserPower() {
        return mGcodeModifier.getOverrideLaserPower();
    }

    @Override
    public boolean getOverrideLaserPowerDirty() {
        return mGcodeModifier.getOverrideLaserPowerDirty();
    }

    @Override
    public void setOverrideLaserPower(float power) {
        mGcodeModifier.setOverrideLaserPower(power);
    }

    @Override
    public Observable<FabPacketContent.HeaderSecurity> getHeaderSecurityStatus() {
        return mSlaveComputer.requestHeaderSecurityStatus();
    }

    @Override
    public Observable<Integer> getPauseState() {
        return mSlaveComputer.watchPrintPauseState();
    }

    //endregion

    @Override
    public void disposeAll() {
        dispose(compositeDisposable);
        dispose(mPrintDisposable);
        dispose(mPrintGcodeCheckSubscription);
    }

    private void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
