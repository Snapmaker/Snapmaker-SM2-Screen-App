package fabscreen.libraries.legacy.data.print;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.file.IFile;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public interface IPrintController {
    int STATE_IDLE = 0;
    int STATE_PRINTING = 1;
    int STATE_PAUSED = 2;
    int STATE_COMPLETED = 3;

    void reset();

    void start();

    void pause();

    void resume();

    void stop();

    void recover();

    void finish();

    ISlaveComputer getSlaveComputer();

    int getTotalLines();

    void setTotalLines(int lines);

    boolean getRecoveryFlag();

    void setPowerOutageFlag(boolean flag);

    void setModelBoundary(ModelBoundary boundary);

    ModelBoundary getModelBoundary();

    void setActionDisposable(Disposable disposable);

    Observable<FabPacketContent.HeaderSecurity> getHeaderSecurityStatus();

    @Deprecated
    Integer getPrintState();

    @Deprecated
    void setListener(PrintListener listener);

    int getProgressCount();

    float getProgress();

    void setFile(IFile file);

    Observable<Boolean> getResumeObservable();

    @Deprecated
    Observable<Integer> getPrintStatusObservable();

    Observable<MachinePrintJobState> getPrintJobStateObservable();

    MachinePrintJobState getPrintJobState();

    Observable<PrintJobEvent> getPrintJobEventObservable();

    TickCounter getTickCounter();

    @Deprecated
    Observable<Integer> getPauseState();

    @Deprecated
    void masterPause();

    void setResume();

    void onEmergencyStop();

    void pauseOnFilamentUsedOut();

    void pauseOnEnclosureDoorDetected();

    void disposeAll();

    boolean getInitialM190Flag();

    boolean getInitialM109Flag();

    // Print Parameters modified

    void setOverrideFeedRate(float feedRate);

    void setOverrideZOffset(float zOffset);

    void setOverrideZOffset(int which, float zOffset);

    void setOverrideFlowRate(int which, float flowRate);

    void setOverrideHeatedBedTemperature(float temp);

    void setOverrideLaserPower(float power);

    void setOverrideNozzleTemperature(float temp);

    void setOverrideNozzleTemperature(int which, float temp);

    void setOverrideInitialNozzleTemperature(float temp);

    void setOverrideInitialNozzleTemperature(int which, float temp);

    void setOverrideInitialNozzleTemperature(float temp, boolean isUserModified);

    void setOverrideInitialHeatedBedTemperature(float temp);

    void setOverrideInitialHeatedBedTemperature(float temp, boolean isUserModified);

    boolean getOverrideNozzleTemperatureDirty();

    float getOverrideNozzleTemperature();

    float getOverrideNozzleTemperature(int which);

    boolean getOverrideHeatedBedTemperatureDirty();

    float getOverrideHeatedBedTemperature();

    boolean getOverrideLaserPowerDirty();

    float getOverrideLaserPower();

    float getOverrideInitialHeatedBedTemperature();

    float getOverrideFeedRate();

    float getOverrideInitialNozzleTemperature();

    float getOverrideInitialNozzleTemperature(int which);

    float getOverrideZOffset();

    float getOverrideZOffset(int which);
}