package fabscreen.libraries.legacy.data.remote;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Preferences;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.PrintController;
import fabscreen.libraries.legacy.data.print.PrintEventState;
import fabscreen.libraries.legacy.data.print.PrintJobEvent;
import fabscreen.libraries.legacy.data.print.PrintListener;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static fabscreen.libraries.legacy.data.remote.SessionManager.NULL_SESSION;

public class RemoteController {
    public final String KEY_TOOL_HEAD_3DP_1 = "TOOLHEAD_3DPRINTING_1";
    public final String KEY_TOOL_HEAD_LASER_1 = "TOOLHEAD_LASER_1";
    public final String KEY_TOOL_HEAD_LASER_2 = "TOOLHEAD_LASER_2";
    public final String KEY_TOOL_HEAD_CNC_1 = "TOOLHEAD_CNC_1";
    private CompositeDisposable disposables = new CompositeDisposable();

    private IPrintController mPrintController;
    private SessionManager sessionManager;

    private int mFileType = Constants.FILE_TYPE_UNKNOWN;
    private String mFileName;
    private IFile mFile;
    private int mTotalLines = 0;
    private float mEstimatedTime = 0;
    private int mEnclosureDoorCount = 0;

    private PublishSubject<Integer> mStartResultSubject = PublishSubject.create();
    private PublishSubject<Integer> mPauseResultSubject = PublishSubject.create();
    private PublishSubject<Integer> mResumeResultSubject = PublishSubject.create();
    private PublishSubject<Integer> mStopResultSubject = PublishSubject.create();

    private boolean mRemotePageFlag = false;
    private boolean mNeedBackToPrint = false;
    @Deprecated
    private BehaviorSubject<Integer> mRemotePrintState = BehaviorSubject.createDefault(PrintController.STATE_IDLE);

    public RemoteController(File dataDir, IPrintController printController) {
        mPrintController = printController;

        File sessionConfigFile = new File(dataDir, "session.json");
        sessionManager = new SessionManager(sessionConfigFile);

        bind();
    }

    private void bind() {
        Disposable sub = Observable.interval(5, TimeUnit.SECONDS)
                .subscribe(tick -> {
                    sessionManager.checkCurrentSessionActive();
                });
        disposables.add(sub);
    }

    private IPrintController getPrintController() {
        return mPrintController;
    }

    public void connectPrintController() {
//        getPrintController().setListener(mListener);

        // Sync print state nad arguments
//        int printState = mPrintController.getPrintState();
//        mRemotePrintState.onNext(printState);
        mTotalLines = mPrintController.getTotalLines();

        Disposable sub = mPrintController.getPrintJobEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    switch (state.getPrintEventState()) {
                        case REQUEST_START_SUCCESS:
                            mStartResultSubject.onNext(0);
                            break;
                        case REQUEST_START_FAILED:
                            mStartResultSubject.onNext(state.getErrorCode());
                            break;
                        case REQUEST_PAUSE_SUCCESS:
                            mPauseResultSubject.onNext(0);
                            break;
                        case REQUEST_PAUSE_FAILED:
                            mPauseResultSubject.onNext(state.getErrorCode());
                            break;
                        case REQUEST_RESUME_SUCCESS:
                            mResumeResultSubject.onNext(0);
                            break;
                        case REQUEST_RESUME_FAILED:
                            mResumeResultSubject.onNext(state.getErrorCode());
                            break;
                        case REQUEST_STOP_SUCCESS:
                            mStopResultSubject.onNext(0);
                            break;
                        case REQUEST_STOP_FAILED:
                            mStopResultSubject.onNext(state.getErrorCode());
                            break;
                        case REQUEST_POWER_LOSS_RECOVER_SUCCESS:
                        case REQUEST_POWER_LOSS_RECOVER_FAILED:
                        case PRINT_PAUSED_TRIGGERED_BY_CONTROLLER:
                        case PRINT_FINISH_SUCCESS:
                        case PRINT_FINISH_FAILED:
                            break;
                    }
                }, LogHelper::log);
        disposables.add(sub);
    }

    //region Remote Auth

    public Observable<SessionManager.State> getRemoteStateObservable() {
        return sessionManager.getCurrentSessionObservable()
                .map(session -> {
                    if (session != NULL_SESSION) {
                        return SessionManager.State.STATE_ACTIVE;
                    } else {
                        return SessionManager.State.STATE_INACTIVE;
                    }
                });
    }

    /**
     * Create session
     */
    @NonNull
    public SessionManager.Session createSession() {
        return sessionManager.createSession("unknown");
    }

    /**
     * Get session.
     */
    @NonNull
    public SessionManager.Session getSession(Preferences preferences, @NonNull String token) {
        return sessionManager.getSession(preferences, token);
    }

    @NonNull
    public SessionManager.Session getCurrentSession() {
        return sessionManager.getCurrentSession();
    }

    public Observable<SessionManager.Session> getCurrentSessionObservable() {
        return sessionManager.getCurrentSessionObservable();
    }

    public void setCurrentSession(@NonNull SessionManager.Session session) {
        sessionManager.setCurrentSession(session);
    }

    public void accessCurrentSession() {
        sessionManager.accessCurrentSession();
    }

    public void grantCurrentSession() {
        sessionManager.grantCurrentSession();
    }

    public void denyCurrentSession() {
        sessionManager.denyCurrentSession();
    }

    //endregion

    public void setFileType(int fileType) {
        mFileType = fileType;
    }

    public void setFileName(String fileName) {
        // one of print.gcode / print.nc / print.cnc
        mFileName = fileName;
    }

    public String getFileName() {
        return mFileName != null ? mFileName : "";
    }

    public void setFile(IFile file) {
        mFile = file;
    }

    public IFile getFile() {
        return mFile;
    }

    public void setTotalLines(int totalLines) {
        mTotalLines = totalLines;
    }

    public int getTotalLines() {
        return mTotalLines;
    }

    public void setEstimatedTime(float estimatedTime) {
        mEstimatedTime = estimatedTime;
    }

    public float getEstimatedTime() {
        return mEstimatedTime;
    }

    public int getElapsedTIme() {
        return getPrintController().getTickCounter().getCount();
    }

    public double getRemainingTime() {
        float p = getPrintController().getProgress();

        // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
        int elapsed = getPrintController().getTickCounter().getCount();
        return ((1 - p) * elapsed + (1 - p) * (1 - p) * mEstimatedTime);
    }

    // Progress Count
    public int getProgressCount() {
        return getPrintController().getProgressCount();
    }

    public float getProgress() {
        return getPrintController().getProgress();
    }

    public void setNeedBackToPrint(boolean isPrinting) {
        mNeedBackToPrint = isPrinting;
    }

    public boolean isNeedBackToPrint() {
        return mNeedBackToPrint;
    }

    // override
    public void reset() {
        mRemotePrintState.onNext(IPrintController.STATE_IDLE);
        mNeedBackToPrint = false;
        getPrintController().reset();
    }

    public void overrideNozzleTemperature(float nozzleTemperature) {
        getPrintController().setOverrideNozzleTemperature(nozzleTemperature);
    }

    public void overrideNozzleTemperature(int index, float nozzleTemperature) {
        getPrintController().setOverrideNozzleTemperature(index, nozzleTemperature);
    }
    public void overrideHeatedBedTemperature(float heatedBedTemperature) {
        getPrintController().setOverrideHeatedBedTemperature(heatedBedTemperature);
    }

    public void overrideZOffset(float offset) {
        getPrintController().setOverrideZOffset(offset);
    }

    public void overrideWorkSpeed(float workSpeed) {
        getPrintController().setOverrideFeedRate(workSpeed);
    }

    public void overrideLaserPower(float laserPower) {
        getPrintController().setOverrideLaserPower(laserPower);
    }

    // print
    private PrintListener mListener = new PrintListener() {
        @Override
        public void onStartSuccess() {
            mStartResultSubject.onNext(0);
            mRemotePrintState.onNext(PrintController.STATE_PRINTING);
            mPrintController.getTickCounter().reset();
            mPrintController.getTickCounter().start();
        }

        @Override
        public void onStartFailed(int retCode) {
            mStartResultSubject.onNext(retCode);
        }

        @Override
        public void onPauseSuccess() {
            mPauseResultSubject.onNext(0);
            mRemotePrintState.onNext(PrintController.STATE_PAUSED);

            mPrintController.getTickCounter().stop();
        }

        @Override
        public void onPauseFailed(int retCode) {
            mPauseResultSubject.onNext(retCode);
        }

        @Override
        public void onResumeSuccess() {
            mResumeResultSubject.onNext(0);
            mRemotePrintState.onNext(PrintController.STATE_PRINTING);

            mPrintController.getTickCounter().start();
        }

        @Override
        public void onResumeFailed(int retCode) {
            mResumeResultSubject.onNext(retCode);
        }

        @Override
        public void onResumeFromPowerOutageSuccess() {
            // TODO: power-loss recovery on remote access
        }

        @Override
        public void onResumeFromPowerOutageFailed(int retCode) {

        }

        @Override
        public void onStopSuccess() {
            mStopResultSubject.onNext(0);
            mRemotePrintState.onNext(PrintController.STATE_IDLE);
            mPrintController.getTickCounter().stop();
        }

        @Override
        public void onStopFailed(int retCode) {
            mStopResultSubject.onNext(retCode);
        }

        @Override
        public void onFinishSuccess() {
            mRemotePrintState.onNext(PrintController.STATE_COMPLETED);
            mPrintController.getTickCounter().stop();
        }

        @Override
        public void onFinishFailed(int retCode) {

        }
    };

    public Observable<Integer> getRemotePrintStateObservable() {
        return mRemotePrintState.hide();
    }

    public int getRemotePrintState() {
        return mRemotePrintState.getValue();
    }

    public Observable<Integer> start() {
        if (mFile == null) {
            return Observable.just(200);
        }

        getPrintController().setFile(mFile);
        getPrintController().setTotalLines(mTotalLines);

        AndroidSchedulers.mainThread().scheduleDirect(() -> getPrintController().start(), 50, TimeUnit.MILLISECONDS);

        return mStartResultSubject;
    }

    public Observable<Integer> pause() {
        AndroidSchedulers.mainThread().scheduleDirect(() -> getPrintController().pause(), 50, TimeUnit.MILLISECONDS);

        return mPauseResultSubject;
    }

    public Observable<Integer> resume() {
        AndroidSchedulers.mainThread().scheduleDirect(() -> getPrintController().resume(), 50, TimeUnit.MILLISECONDS);

        return mResumeResultSubject;
    }

    public Observable<Integer> stop() {
        AndroidSchedulers.mainThread().scheduleDirect(() -> getPrintController().stop(), 50, TimeUnit.MILLISECONDS);

        return mStopResultSubject;
    }

    public void setFilamentOutPause() {
        getPrintController().pauseOnFilamentUsedOut();
    }

    public void setEnclosureDoorPause() {
        getPrintController().pauseOnEnclosureDoorDetected();
    }

    public int getEnclosureDoorCount() {
        return mEnclosureDoorCount;
    }

    public void setEnclosureDoorCount(int count) {
        mEnclosureDoorCount = count;
    }

    // FIXME: Temporary workaround.
    public void setRemotePageFlag(boolean flag) {
        mRemotePageFlag = flag;
    }

    public boolean getRemotePageFlag() {
        return mRemotePageFlag;
    }

    public void setPrintController(IPrintController printController) {
        mPrintController = printController;
    }
}
