package fabscreen.libraries.legacy.data;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.version.VersionRequirement;
import fabscreen.libraries.legacy.data.version.VersionRequirementManager;
import fabscreen.libraries.legacy.lib.FabException;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.SemVerHelper;
import fabscreen.libraries.legacy.lib.fabserver.RetryWithDelay;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

import static fabscreen.libraries.legacy.data.Constants.FIVE_MINUTES_DELAY_DURATION;

/**
 * Controller that manages machine status.
 */
public class MachineController {

    private CompositeDisposable disposables = new CompositeDisposable();

    private static final String DEVICE = "/dev/ttyHSL1";

    private ISlaveComputer mSlaveComputer;
    private ILaserCameraController mLaserCameraController;
    private IPrintController mPrintController;
    private Preferences preferences;
    private VersionRequirementManager mVersionRequirementManager;

    private int mHeadType = Constants.HEAD_UNPLUGGED;
    private int mMachineModel = Constants.MACHINE_MODEL_UNKNOWN;
    private int mSizeX = 0;
    private int mSizeY = 0;
    private int mSizeZ = 0;

    private boolean mHomed = false;
    private boolean mCoordinateAligned = false;
    private int mCoordinateID = 0;
    private float mCoordinateX = 0;
    private float mCoordinateY = 0;
    private float mCoordinateZ = 0;

    private WorkType mWorkType = WorkType.NONE;

    // Add On
    private boolean mIsEnclosureReady = false;
    private boolean mIsEnclosureDoorDetectionEnabled = false;
    private int mEnclosureLedValue = 0;
    private int mEnclosureFanValue = 0;

    private boolean mIsRotaryAvailable = false;
    private boolean mIsEmergencyStopAvailable = false;
    private boolean mAirPurifierFanEnabled = false;
    private int mAirPurifierFanSpeed = 0;
    private int mAirPurifierFilterLifeTime = 0;
    private Disposable mAirPurifierAutoTurnOffSubscription = null;

    private byte mLaser10WErrorState = 0;
    private int mLaserCameraInterval = 10;
    private String mLaserCameraAddress;

    private BehaviorSubject<Boolean> mPowerOutageSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> m3DPFilamentSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Float> mLaserFocusSubject = BehaviorSubject.createDefault(0f);

    private BehaviorSubject<Boolean> mEnclosureDoorSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Byte> mRotaryModuleStatusSubject = BehaviorSubject.createDefault((byte) 1);
    private BehaviorSubject<Boolean> mEmergencyStopSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<FabPacketContent.AirPurifierStatus> mAirPurifierStatusSubject = BehaviorSubject.createDefault(FabPacketContent.AirPurifierStatus.MOCK_AIR_PURIFIER_STATUS_NOT_PLUGGED);
    private BehaviorSubject<FabPacketContent.AirPurifierFan> mAirPurifierFanSubject = BehaviorSubject.createDefault(new FabPacketContent.AirPurifierFan());
    private BehaviorSubject<Integer> mAirPurifierLifeTimeSubject = BehaviorSubject.createDefault(0);
    private BehaviorSubject<Boolean> mNozzleTemperatureMaxExceed = BehaviorSubject.createDefault(false);

    private BehaviorSubject<Float> mLaserShotOutputPowerSubject = BehaviorSubject.createDefault(0f);

    private BehaviorSubject<ArrayList<String>> mOutdatedVersionModuleListSubject = BehaviorSubject.createDefault(new ArrayList<>());

    private BehaviorSubject<FabPacketContent.ExtendKitInfo> mExtendInfoSubject = BehaviorSubject.createDefault(new FabPacketContent.ExtendKitInfo((byte) 0, (byte) 0));

    private Disposable printStateSubscription;

    MachineController(ISlaveComputer slaveComputer, ILaserCameraController laserCameraController, IPrintController printController,
                      Preferences preferences, VersionRequirementManager versionRequirementManager) {
        mSlaveComputer = slaveComputer;
        mLaserCameraController = laserCameraController;
        mPrintController = printController;
        this.preferences = preferences;
        mVersionRequirementManager = versionRequirementManager;

        observeMachineConnection();
    }

    private Preferences getPreferences() {
        return preferences;
    }

    private void observeMachineConnection() {
        if (mSlaveComputer == null) {
            return;
        }

        // Subscribe connection state changes
        disposables.add(
                mSlaveComputer.getConnectedObservable()
                        .subscribe(connected -> {
                            if (connected) {
                                this.onConnected();
                            } else {
                                this.onDisconnected();
                            }
                        })
        );
    }

    // Initialization on Connected and Disconnected
    public void connect() {
        mSlaveComputer.connect(DEVICE);
    }

    public void reconnect() {
        connect();
    }

    private void onConnected() {
        Disposable sub = mSlaveComputer.getMachineStatusObservable()
                .map(machineStatus -> machineStatus.headStatus)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(headStatus -> {
                    // Record head type
                    if (headStatus != mHeadType) {
                        mHeadType = headStatus;
                        Logger.d("Head type %d found.", mHeadType);
                        onHeadTypeDetected();
                    }
                }, LogHelper::log);
        disposables.add(sub);

        requestMachineModel();

        sub = mSlaveComputer.getMachineErrors()
                .subscribe(machineErrors -> {
                    final boolean isFilamentOut = (machineErrors.bits & FabPacketContent.MachineErrors.PRINT_FILAMENT_ERROR) != 0;
                    final boolean isPowerOutage = (machineErrors.bits & FabPacketContent.MachineErrors.PRINT_POWER_OFF) != 0;
                    final boolean isEnclosureDoorOpened = (machineErrors.bits & FabPacketContent.MachineErrors.ENCLOSURE_DOOR_OPEN) != 0;
                    final boolean isNozzleMaxExceed = (machineErrors.bits & FabPacketContent.MachineErrors.NOZZLE_TEMPERATURE_MAX_EXCEED) != 0;

                    m3DPFilamentSubject.onNext(isFilamentOut);
                    mPowerOutageSubject.onNext(isPowerOutage);
                    mEnclosureDoorSubject.onNext(isEnclosureDoorOpened);
                    mNozzleTemperatureMaxExceed.onNext(isNozzleMaxExceed);
                }, LogHelper::log);
        disposables.add(sub);

        // Watch machine errors in one place.
        sub = mSlaveComputer.watchMachineErrors()
                .subscribe(errors -> {
                    if (errors.bits == 0) return;

                    boolean isEnclosureDoorOpened = (errors.bits & FabPacketContent.MachineErrors.ENCLOSURE_DOOR_OPEN) != 0;
                    final boolean isFilamentOut = (errors.bits & FabPacketContent.MachineErrors.PRINT_FILAMENT_ERROR) != 0;
                    final boolean isNozzleMaxExceed = (errors.bits & FabPacketContent.MachineErrors.NOZZLE_TEMPERATURE_MAX_EXCEED) != 0;

                    mEnclosureDoorSubject.onNext(isEnclosureDoorOpened);
                    m3DPFilamentSubject.onNext(isFilamentOut);
                    mNozzleTemperatureMaxExceed.onNext(isNozzleMaxExceed);
                }, LogHelper::log);
        disposables.add(sub);

        checkModulesVersion();

        requestMachineInfoForLog();

        checkAddonStatus();
    }

    private void onDisconnected() {
        // Back to default status
        mHeadType = Constants.HEAD_UNPLUGGED;
        mWorkType = WorkType.NONE;

        // Enclosure is not available while machine was disconnected.
        mIsEnclosureReady = false;

        // TODO: Sort out disconnect logic.
    }

    public Observable<Float> getLaserOutputPowerObservable() {
        return mLaserShotOutputPowerSubject.hide();
    }

    public float getLaserOutputPower() {
        return mLaserShotOutputPowerSubject.getValue();
    }

    // Machine or controller related

    public WorkType getWorkType() {
        return mWorkType;
    }

    public FabPacketContent.MachineStatus getMachineStatus() {
        return mSlaveComputer.getMachineStatus();
    }

    public Observable<FabPacketContent.MachineStatus> getMachineStatusObservable() {
        return mSlaveComputer.getMachineStatusObservable();
    }

    public Observable<ArrayList<String>> getOutdatedVersionModuleListObservable() {
        return mOutdatedVersionModuleListSubject.debounce(200, TimeUnit.MILLISECONDS).hide();
    }

    private void requestMachineInfoForLog() {
        // Using send G-code to check about machine module status
        Disposable disposable = mSlaveComputer.sendGcode("M1005", true)
                .flatMap(m1005Response -> {
                    Logger.d("M1005 \n%s", m1005Response.getContent());
                    return mSlaveComputer.sendGcode("M1006", true);
                })
                .flatMap(m1006Response -> {
                    Logger.d("M1006 \n%s", m1006Response.getContent());
                    return mSlaveComputer.sendGcode("M2000 S0", true);
                })
                .subscribe(m2000Response -> {
                    Logger.d("M2000 S0 \n%s", m2000Response.getContent());
                });
        disposables.add(disposable);
    }

    /**
     * get machine size
     */

    public int getMachineModel() {
        return mMachineModel;
    }

    public String getMachineModelSeries() {
        switch (mMachineModel) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150:
                return "Snapmaker 2.0 A150";
            case Constants.MACHINE_MODEL_SNAPMAKER_A250:
                return "Snapmaker 2.0 A250";
            case Constants.MACHINE_MODEL_SNAPMAKER_A350:
                return "Snapmaker 2.0 A350";
            default:
                return "Unknown";
        }
    }

    public int getSizeX() {
        return mSizeX;
    }

    public int getSizeY() {
        return mSizeY;
    }

    public int getSizeZ() {
        return mSizeZ;
    }

    // Module version
    private void checkModulesVersion() {
        // Get Module Version and check if version is valid.
        Disposable disposable = mSlaveComputer.watchModuleVersion()
                .subscribe(moduleVersion -> {
                    // Ensure if module version is valid.
                    String version = moduleVersion.version.trim().toUpperCase();
                    if (SemVerHelper.isValid(version)) {
                        VersionRequirement requirement = mVersionRequirementManager.getRequirementByModule(moduleVersion);
                        Logger.d("Module 0x%02X found, Module ID is 0x%06X, version is %s", moduleVersion.moduleType, moduleVersion.moduleID, moduleVersion.version);
                        if (requirement != null) {
                            try {
                                if (SemVerHelper.lt(version, requirement.getRequiredVersion())) {
                                    // Module version is outdated.
                                    addOutdatedVersionModule(requirement.getName());
                                }
                            } catch (FabException e) {
                                LogHelper.log(e);
                            }
                        }
                    } else {
                        Logger.w("Module 0x%02X version (%s) is invalid! ", moduleVersion.moduleType, moduleVersion.version);
                    }
                }, LogHelper::log);
        disposables.add(disposable);

        Logger.d("Requesting module info...");
        mSlaveComputer.requestModuleVersion();
    }

    private void addOutdatedVersionModule(String moduleName) {
        ArrayList<String> moduleList = mOutdatedVersionModuleListSubject.getValue();
        if (!moduleList.isEmpty()) {
            // check if module name is already added.
            for (int i = 0; i < moduleList.size(); i++) {
                if (moduleList.get(i).equals(moduleName)) {
                    return;
                }
            }
        }

        moduleList.add(moduleName);
        mOutdatedVersionModuleListSubject.onNext(moduleList);
    }

    public void requestMachineModel() {
        RetryWithDelay retryWithDelay = new RetryWithDelay();
        Disposable sub = mSlaveComputer.getMachineSize()
                // Add a retransmission mechanism to reduce model loss problems
                // TODO: How to guarantee receipt?  What are the retransmission intervals and times?
                .retryWhen(retryWithDelay)
                .subscribe(machineSize -> {
                    if (machineSize != null) {
                        mMachineModel = machineSize.machineModel;
                        mSizeX = machineSize.xSize;
                        mSizeY = machineSize.ySize;
                        mSizeZ = machineSize.zSize;
                        Logger.d("Machine model %d, size x %d y %d z %d", mMachineModel, mSizeX, mSizeY, mSizeZ);
                        switch (mMachineModel) {
                            case Constants.MACHINE_MODEL_SNAPMAKER_A150: {
                                preferences.setMachineModel(Constants.MACHINE_TYPE_A150);
                                break;
                            }
                            case Constants.MACHINE_MODEL_SNAPMAKER_A250: {
                                preferences.setMachineModel(Constants.MACHINE_TYPE_A250);
                                break;
                            }
                            case Constants.MACHINE_MODEL_SNAPMAKER_A350: {
                                preferences.setMachineModel(Constants.MACHINE_TYPE_A350);
                                break;
                            }
                            case Constants.MACHINE_MODEL_UNKNOWN: {
                                // todo user modified model
                                break;
                            }
                            default:
                                break;
                        }
                    } else {
                        Logger.d("mMachineModel is NULL");
                    }
                }, LogHelper::log);
        disposables.add(sub);
    }

    // Coordinate System
    /**
     * Check if home is executed after booted.
     */
    public boolean isHomed() {
        return mHomed;
    }

    public int getCoordinateID() {
        return mCoordinateID;
    }

    public boolean isCoordinateAligned() {
        return mCoordinateAligned;
    }

    public Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem() {
        return updateCoordinateSystem(-1);
    }

    public Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(int coordinateID) {
        if (coordinateID != -1) {
            Logger.i("update coordinate system... CS#" + coordinateID);
            return mSlaveComputer.sendGcode("G" + (53 + coordinateID))
                    .flatMap(response -> mSlaveComputer.requestCoordinateSystem())
                    .doOnNext(coordinateSystem -> {
                        mHomed = coordinateSystem.homed;
                        mCoordinateAligned = coordinateSystem.coordinateAligned;
                        mCoordinateID = coordinateSystem.coordinateID;
                        mCoordinateX = coordinateSystem.coordinateX;
                        mCoordinateY = coordinateSystem.coordinateY;
                        mCoordinateZ = coordinateSystem.coordinateZ;
                        Logger.i("Coordinate system updated, CS#" + mCoordinateID + " X = " + mCoordinateX + ", Y = " + mCoordinateY + ", Z =" + mCoordinateZ);
                    });
        } else {
            Logger.i("update coordinate system...");
            return mSlaveComputer.requestCoordinateSystem()
                    .doOnNext(coordinateSystem -> {
                        mHomed = coordinateSystem.homed;
                        mCoordinateAligned = coordinateSystem.coordinateAligned;
                        mCoordinateID = coordinateSystem.coordinateID;
                        mCoordinateX = coordinateSystem.coordinateX;
                        mCoordinateY = coordinateSystem.coordinateY;
                        mCoordinateZ = coordinateSystem.coordinateZ;
                        Logger.i("Coordinate system updated, CS#" + mCoordinateID + " X = " + mCoordinateX + ", Y = " + mCoordinateY + ", Z =" + mCoordinateZ);
                    });
        }
    }

    public Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(int coordinateID, int timeout) {
        if (coordinateID != -1) {
            Logger.i("update coordinate system... CS#" + coordinateID);
            return mSlaveComputer.sendGcode("G" + (53 + coordinateID))
                    .flatMap(response -> mSlaveComputer.requestCoordinateSystem(timeout))
                    .doOnNext(coordinateSystem -> {
                        mHomed = coordinateSystem.homed;
                        mCoordinateAligned = coordinateSystem.coordinateAligned;
                        mCoordinateID = coordinateSystem.coordinateID;
                        mCoordinateX = coordinateSystem.coordinateX;
                        mCoordinateY = coordinateSystem.coordinateY;
                        mCoordinateZ = coordinateSystem.coordinateZ;
                        Logger.i("Coordinate system updated, CS#" + mCoordinateID + " X = " + mCoordinateX + ", Y = " + mCoordinateY + ", Z =" + mCoordinateZ);
                    });
        } else {
            Logger.i("update coordinate system...");
            return mSlaveComputer.requestCoordinateSystem(timeout)
                    .doOnNext(coordinateSystem -> {
                        mHomed = coordinateSystem.homed;
                        mCoordinateAligned = coordinateSystem.coordinateAligned;
                        mCoordinateID = coordinateSystem.coordinateID;
                        mCoordinateX = coordinateSystem.coordinateX;
                        mCoordinateY = coordinateSystem.coordinateY;
                        mCoordinateZ = coordinateSystem.coordinateZ;
                        Logger.i("Coordinate system updated, CS#" + mCoordinateID + " X = " + mCoordinateX + ", Y = " + mCoordinateY + ", Z =" + mCoordinateZ);
                    });
        }
    }

    public float getCoordinateOffsetX() {
        return mCoordinateX;
    }

    public float getCoordinateOffsetY() {
        return mCoordinateY;
    }

    public float getCoordinateOffsetZ() {
        return mCoordinateZ;
    }

    // Print business
    public void setPrintController(IPrintController printController) {
        mPrintController = printController;
        if (printStateSubscription != null && !printStateSubscription.isDisposed()) {
            disposables.remove(printStateSubscription);
            printStateSubscription.dispose();
        }
        printStateSubscription = mPrintController.getPrintJobStateObservable()
                .distinctUntilChanged()
                .subscribe(state -> {
                    if (isAirPurifierAutoTurnOffNeeded()) {
                        setAirPurifierAutoTurnOffEnabled((MachinePrintJobState.PRINT_JOB_STATE_FINISHED.valueEqual(state.getValue())));
                    } else {
                        // TODO: Temporary fix.
                        //  If we stop or complete print job, update air purifier once for synchronizing status.
                        //  We need to do that because controller won't push air purifier status when print job finished or stopped.
                        if (!MachinePrintJobState.isStatePrinting(state.getValue())) {
                            disposables.add(updateAirPurifierStatus().subscribe());
                        }
                    }
                }, LogHelper::log);
        disposables.add(printStateSubscription);
    }

    public Observable<Boolean> getPowerOutageObservable() {
        return mPowerOutageSubject.hide();
    }

    public void clearPowerOutageFlag() {
        mPowerOutageSubject.onNext(false);
    }

    // Add-on

    private void checkAddonStatus() {
        // TODO: There's a better way to know which add-on was plugged instead of requesting status every time.
        //  Need to refactor this after we define add-on list request/response.
        Disposable sub = mSlaveComputer.getEnclosureStatus()
                .subscribe(enclosureStatus -> {
                    mIsEnclosureReady = enclosureStatus.isReady();
                    mIsEnclosureDoorDetectionEnabled = enclosureStatus.isEnclosureEnabled();
                    mEnclosureLedValue = enclosureStatus.ledLevel;
                    mEnclosureFanValue = enclosureStatus.fanLevel;
                }, LogHelper::log);
        disposables.add(sub);

        final boolean isEnclosureAutoLightingOn = preferences.getEnclosureAutoLightingOn();
        if (isEnclosureAutoLightingOn) {
            // Set Enclosure Lighting, range [0 - 100]
            // Full power(100) as "on" while setting enclosure lighting.
            final int value = 100;
            sub = mSlaveComputer.setEnclosureLed(100).subscribe(success -> {
                Logger.d("Set Enclosure lighting " + success);
            }, LogHelper::log);
            disposables.add(sub);
        }

        sub = mSlaveComputer.requestEmergencyStopStatus()
                .subscribe(status -> {
                    switch (status) {
                        case (byte) 0:
                            mEmergencyStopSubject.onNext(false);
                            mIsEmergencyStopAvailable = true;
                            onEmergencyStopConnected();
                            break;
                        case (byte) 1:
                            mEmergencyStopSubject.onNext(false);
                            mIsEmergencyStopAvailable = false;
                            break;
                        case (byte) 2:
                            mIsEmergencyStopAvailable = true;
                            mEmergencyStopSubject.onNext(true);
                            break;
                        default:
                            break;
                    }
                }, LogHelper::log);
        disposables.add(sub);

        sub = mSlaveComputer.requestRotaryModuleStatus()
                .subscribe(status -> {
                    mRotaryModuleStatusSubject.onNext(status);
                    switch (status) {
                        case (byte) 0:
                            // rotary connected and ready to go
                            mIsRotaryAvailable = true;
                            if (mHeadType == Constants.HEAD_LASER) {
                                updateLaserFocus();
                            }
                            break;
                        case (byte) 1:
                            // rotary not connected
                        case (byte) 2:
                            // rotary detected but not available
                            mIsRotaryAvailable = false;
                            break;
                        default:
                            break;
                    }
                }, LogHelper::log);
        disposables.add(sub);

        sub = mSlaveComputer.requestAirPurifierAddOnStatus()
                .subscribe(airPurifierStatus -> {
                    mAirPurifierStatusSubject.onNext(airPurifierStatus);
                    if (airPurifierStatus.status != (byte) 0x01) {
                        onAirPurifierConnected();
                    }

                    switch (airPurifierStatus.status) {
                        case 0x00:
                            // OK
                            break;
                        case 0x01:
                            // not plugged
                            break;
                        case 0x02:
                            // no power
                            break;
                        case 0x03:
                            // error
                            break;
                        default:
                            break;
                    }
                }, LogHelper::log);
        disposables.add(sub);

        requestExtendKitInfo();
    }

    // Enclosure

    public void clearEnclosureDoorFlag() {
        mEnclosureDoorSubject.onNext(false);
    }

    public Observable<Boolean> getEnclosureDoorObservable() {
        return mEnclosureDoorSubject.hide();
    }

    public boolean isEnclosureOpen() {
        return mEnclosureDoorSubject.getValue();
    }

    public Observable<FabPacketContent.EnclosureStatus> updateEnclosureStatus() {
        return mSlaveComputer.getEnclosureStatus()
                .doOnNext(enclosureStatus -> {
                    mIsEnclosureReady = enclosureStatus.isReady();
                    mIsEnclosureDoorDetectionEnabled = enclosureStatus.isEnclosureEnabled();
                    mEnclosureLedValue = enclosureStatus.ledLevel;
                    mEnclosureFanValue = enclosureStatus.fanLevel;
                });
    }

    public boolean isEnclosureReady() {
        return mIsEnclosureReady;
    }

    public boolean isEnclosureLedOn() {
        return mEnclosureLedValue != 0;
    }

    public boolean isEnclosureFanOn() {
        return mEnclosureFanValue != 0;
    }

    public boolean isEnclosureDoorDetectionEnabled() {
        return mIsEnclosureDoorDetectionEnabled;
    }

    public int getEnclosureLed() {
        return mEnclosureLedValue;
    }

    public int getEnclosureFan() {
        return mEnclosureFanValue;
    }

    // Rotary
    public boolean isRotaryModuleAvailable() {
        return mIsRotaryAvailable;
    }

    public Observable<Byte> getRotaryStatusObservable() {
        return mRotaryModuleStatusSubject.hide();
    }

    public Byte getRotaryModuleStatus() {
        return mRotaryModuleStatusSubject.getValue();
    }

    // Emergency Stop
    public Observable<Boolean> getEmergencyStopObservable() {
        return mEmergencyStopSubject.hide();
    }

    public boolean isEmergencyStopTriggered() {
        return mEmergencyStopSubject.getValue();
    }

    public boolean isEmergencyStopAvailable() {
        return mIsEmergencyStopAvailable;
    }

    public void setEmergencyStopTriggered(boolean isTriggered) {
        mEmergencyStopSubject.onNext(isTriggered);
    }

    private void onEmergencyStopConnected() {
        Disposable sub = mSlaveComputer.watchEmergencyStopStatus().subscribe(status -> {
            if (status == 2) {
                mEmergencyStopSubject.onNext(true);
            }
        });
        disposables.add(sub);
    }

    // Air Purifier
    private void onAirPurifierConnected() {
        // Initialize air purifier status
        Disposable sub = mSlaveComputer.watchAirPurifierAddOnStatus()
                .subscribe(airPurifierStatus -> {
                    mAirPurifierStatusSubject.onNext(airPurifierStatus);
                }, LogHelper::log);
        disposables.add(sub);

        sub = mSlaveComputer.getAirPurifierFilterLifeTime()
                .doOnNext(l -> mAirPurifierLifeTimeSubject.onNext(l))
                .flatMap(life -> mSlaveComputer.watchAirPurifierFilterLifeTime())
                .subscribe(life -> {
                    mAirPurifierLifeTimeSubject.onNext(life);
                });
        disposables.add(sub);

        sub = mSlaveComputer.requestAirPurifierFan().subscribe(airPurifierFan -> {
            mAirPurifierFanEnabled = airPurifierFan.isOn;
            mAirPurifierFanSpeed = airPurifierFan.level;
            mAirPurifierFanSubject.onNext(airPurifierFan);
        }, LogHelper::log);
        disposables.add(sub);

        // Observe Print state with PrintController for auto turn off task.
        printStateSubscription = mPrintController.getPrintJobStateObservable()
                .distinctUntilChanged()
                .subscribe(state -> {
                    if (isAirPurifierAutoTurnOffNeeded()) {
                        setAirPurifierAutoTurnOffEnabled(MachinePrintJobState.PRINT_JOB_STATE_FINISHED.valueEqual(state.getValue()));
                    } else {
                        // TODO: Temporary fix.
                        //  If we stop or complete print job, update air purifier once for synchronizing status.
                        //  We need to do that because controller won't push air purifier status when print job finished or stopped.
                        if (!MachinePrintJobState.isStatePrinting(state.getValue())) {
                            disposables.add(updateAirPurifierStatus().subscribe());
                        }
                    }
                }, LogHelper::log);
        disposables.add(printStateSubscription);
    }

    // Quick SWAP Kit & Bracing Kit
    public void requestExtendKitInfo() {
        Disposable sub = mSlaveComputer.requestExtendKitInfo()
                .subscribe(info -> {
                    Logger.d("Request extend kit info state " + info.toString());
                    mExtendInfoSubject.onNext(info);
                }, LogHelper::log);
        disposables.add(sub);
    }

    public Observable<FabPacketContent.ExtendKitInfo> getExtendKitInfoObservable(){
        return mExtendInfoSubject.hide();
    }

    public FabPacketContent.ExtendKitInfo getExtendKitInfo() {
        return mExtendInfoSubject.getValue();
    }

    public Observable<Boolean> setExtendKitConf(byte extendKitConf) {
        boolean isQuickStopInstall = (extendKitConf & FabPacketContent.ExtendKitInfo.BIT_EXTEND_KIT_QUICK_SWAP) > 0;
        return mSlaveComputer.setExtendKitInfo(isQuickStopInstall, extendKitConf)
                .flatMap(result -> {
                    if (result != 0) {
                        Logger.w("Set quick swap state failed, replace from last state");
                        return Observable.just(mExtendInfoSubject.getValue());
                    } else {
                        return mSlaveComputer.requestExtendKitInfo();
                    }
                })
                .doOnNext(info -> {
                    Logger.d("Set quick swap state success, info:\n " + info.toString());
                    mExtendInfoSubject.onNext(info);
                    requestMachineModel();
                })
                .flatMap(info -> Observable.just(true));
    }

    private boolean isAirPurifierAutoTurnOffNeeded() {
        return preferences.getAirPurifierAutoTurnOffFlag() && mAirPurifierFanEnabled;
    }

    private boolean isAirPurifierAutoTurnOffEnabled() {
        return mAirPurifierAutoTurnOffSubscription != null;
    }

    private void setAirPurifierAutoTurnOffEnabled(boolean enabled) {
        if (enabled) {
            // Prevent multiply subscribe when this calls.
            if (mAirPurifierAutoTurnOffSubscription != null) {
                mAirPurifierAutoTurnOffSubscription.dispose();
            }

            Logger.d("Start countdown for Air Purifier auto turn off...");

            mAirPurifierAutoTurnOffSubscription = Observable.interval(FIVE_MINUTES_DELAY_DURATION, Constants.TIME_UNIT)
                    .take(1)
                    .subscribe(t -> {
                        // Turn off the air purifier when timeout
                        Logger.d("Air Purifier auto turn off.");
                        disposables.add(
                                mSlaveComputer.setAirPurifierEnabled(false)
                                        .subscribe(success -> {/**/}, LogHelper::log)
                        );
                    });
        } else {
            Logger.d("Air Purifier auto turn off canceled.");
            // Disable Auto Turn Off Task
            if (mAirPurifierAutoTurnOffSubscription != null) {
                mAirPurifierAutoTurnOffSubscription.dispose();
                mAirPurifierAutoTurnOffSubscription = null;
            }
        }
    }

    public Observable<FabPacketContent.AirPurifierStatus> getAirPurifierStatusObservable() {
        return mAirPurifierStatusSubject.hide();
    }

    public boolean isAirPurifierPlugged() {
        return mAirPurifierStatusSubject.getValue().status != 0x01;
    }

    public boolean isAirPurifierReady() {
        return mAirPurifierStatusSubject.getValue().status == 0x00;
    }

    public Observable<FabPacketContent.AirPurifierStatus> updateAirPurifierStatus() {
        return mSlaveComputer.requestAirPurifierAddOnStatus().doOnNext(airPurifierStatus -> {
            mAirPurifierStatusSubject.onNext(airPurifierStatus);
        });
    }

    public FabPacketContent.AirPurifierStatus getAirPurifierStatus() {
        return mAirPurifierStatusSubject.getValue();
    }

    public Observable<FabPacketContent.AirPurifierFan> updateAirPurifierFan() {
        return mSlaveComputer.requestAirPurifierFan().doOnNext(airPurifierFan -> {
            mAirPurifierFanEnabled = airPurifierFan.isOn;
            mAirPurifierFanSpeed = airPurifierFan.level;
            mAirPurifierFanSubject.onNext(airPurifierFan);
        });
    }

    public int getAirPurifierFanSpeed() {
        return mAirPurifierFanSpeed;
    }

    public boolean isAirPurifierFanOn() {
        return mAirPurifierFanEnabled;
    }

    public int getAirPurifierFilterLifeTime() {
        return mAirPurifierLifeTimeSubject.getValue();
    }

    public Observable<Boolean> setAirPurifierEnabled(boolean enabled) {
        return mSlaveComputer.setAirPurifierEnabled(enabled)
                .doOnNext(ret -> {
                    disposables.add(updateAirPurifierFan().subscribe(success -> {/**/}, LogHelper::log));
                    if (isAirPurifierAutoTurnOffEnabled()) {
                        setAirPurifierAutoTurnOffEnabled(false);
                    }
                });
    }

    public Observable<Integer> getAirPurifierFilterLifeTimeObservable() {
        return mAirPurifierLifeTimeSubject.hide();
    }

    public Observable<FabPacketContent.AirPurifierFan> getAirPurifierFanObservable() {
        return mAirPurifierFanSubject.hide();
    }

    // ToolHead related
    /**
     * Get Head Type
     */
    public int getHeadType() {
        return mHeadType;
    }

    private void onHeadTypeDetected() {
        switch (mHeadType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
            case Constants.HEAD_FACTORY_3DP:
                mWorkType = WorkType.FDM;
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_FACTORY_LASER:
            case Constants.HEAD_LASER_10W:
                mWorkType = WorkType.LASER;
                initLaserCamera();
                initLaserFocus();
                break;
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
            case Constants.HEAD_LASER_2W_IR:
                mWorkType = WorkType.LASER;
                initLaserFocus();
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
            case Constants.HEAD_FACTORY_CNC:
                mWorkType = WorkType.CNC;
                break;
            case Constants.HEAD_UNPLUGGED:
            default:
                mWorkType = WorkType.NONE;
                break;
        }
    }

    // Single Extrusion

    public void clearFilamentOutFlag() {
        m3DPFilamentSubject.onNext(false);
    }

    public boolean isFilamentOut() {
        return m3DPFilamentSubject.getValue();
    }

    public Observable<Boolean> getFilamentObservable() {
        return m3DPFilamentSubject.hide();
    }

    // Dual Extrusion
    public Observable<Boolean> getNozzleTemperatureExceedObservable() {
        return mNozzleTemperatureMaxExceed.hide();
    }

    public boolean isDualExtruderTemperatureExceed() {
        return mNozzleTemperatureMaxExceed.getValue() && mHeadType == Constants.HEAD_3DP_DUAL_EXTRUDER;
    }

    // Laser General
    private void initLaserFocus() {
        Disposable sub = mSlaveComputer.getLaserFocalLength()
                .subscribe(focalLength -> {
                    float actualFocal;
                    if (mIsRotaryAvailable) {
                        actualFocal = focalLength
                                + MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                                + MockConst.LASER_MOCK_ROTARY_HEIGHT;
                    } else {
                        actualFocal = focalLength + MockConst.LASER_MOCK_PLATE_HEIGHT;
                    }
                    mLaserFocusSubject.onNext(actualFocal);
                }, LogHelper::log);
        disposables.add(sub);

        if (mLaserShotOutputPowerSubject.getValue() == 0) {
            initDefaultLaserShotOutputPower();
        }
        requestLaserShotOutputPower();
    }

    public void initDefaultLaserShotOutputPower() {
        switch (mHeadType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
                mLaserShotOutputPowerSubject.onNext(0.5f);
                break;
            case Constants.HEAD_LASER_10W:
                mLaserShotOutputPowerSubject.onNext(1f);
                break;
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                mLaserShotOutputPowerSubject.onNext(0.2f);
                break;
        }
    }

    public void requestLaserShotOutputPower() {
        Disposable sub = mSlaveComputer.getLaserIndicatorPower()
                .subscribe(laserPower -> {
                    Logger.d("get low-intensity laser power" + laserPower);
                    float minValue = 0.5f;
                    switch (mHeadType) {
                        case Constants.HEAD_LASER:
                        case Constants.HEAD_LASER_2W_IR:
                            minValue = 0.5f;
                            break;
                        case Constants.HEAD_LASER_10W:
                            minValue = 1f;
                            break;
                        case Constants.HEAD_LASER_20W:
                        case Constants.HEAD_LASER_40W:
                            minValue = 0.2f;
                            break;
                    }
                    float limitPower = Math.max(minValue, Math.min(laserPower, 3f));
                    mLaserShotOutputPowerSubject.onNext(limitPower);
                });
        disposables.add(sub);
    }

    public Observable<Boolean> setLaserShotOutputPower(float power) {
        return mSlaveComputer.setLaserIndicatorPower(power)
                .doOnNext(response -> requestLaserShotOutputPower());
    }

    public void updateLaserFocus() {
        Disposable sub = mSlaveComputer.getLaserFocalLength()
                .subscribe(focalLength -> {
                    float actualFocal;
                    if (mIsRotaryAvailable) {
                        actualFocal = focalLength / 1000f
                                + MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                                + MockConst.LASER_MOCK_ROTARY_HEIGHT;
                    } else {
                        actualFocal = focalLength / 1000f + MockConst.LASER_MOCK_PLATE_HEIGHT;
                    }
                    mLaserFocusSubject.onNext(actualFocal);
                });
        disposables.add(sub);
    }

    public float getLaserFocus() {
        return mLaserFocusSubject.getValue();
    }

    public Observable<Float> getLaserFocusObservable() {
        return mLaserFocusSubject.hide();
    }

    public Observable<Boolean> setLaserFocus(float laserFocus) {
        float actualFocal;
        if (mIsRotaryAvailable) {
            actualFocal = laserFocus - MockConst.LASER_MOCK_ROTARY_WASTE_BOARD_HEIGHT
                    - MockConst.LASER_MOCK_ROTARY_HEIGHT;
        } else {
            actualFocal = laserFocus - MockConst.LASER_MOCK_PLATE_HEIGHT;
        }
        return mSlaveComputer.setLaserFocalLength(actualFocal)
                .doOnNext(success -> mLaserFocusSubject.onNext(laserFocus));
    }

    public Observable<Short> getFireSensorSensitivity() {
        return mSlaveComputer.getFireSensorSensitivity();
    }

    public Observable<Boolean> setFireSensorSensitivity(int value) {
        return mSlaveComputer.setFireSensorSensitivity(value);
    }

    public Observable<FabPacketContent.CrossLineIndicatorOffset> getLaserCrossLineIndicatorOffset() {
        return mSlaveComputer.getCrossLineIndicatorOffset();
    }

    public Observable<Boolean> setLaserCrossLineIndicatorOffset(float xOffset, float yOffset) {
        return mSlaveComputer.setCrossLineIndicatorOffset(xOffset, yOffset);
    }

    // Laser Camera business
    private void initLaserCamera() {
        if (!mLaserCameraController.isEnabled()) {
            mLaserCameraController.setEnabled(true);
            // wait 5 seconds for opening bluetooth device
            AndroidSchedulers.mainThread().scheduleDirect(this::initLaserCamera, 5000, TimeUnit.MILLISECONDS);
            return;
        }
        waitForLaserCameraReady();
    }

    private void waitForLaserCameraReady() {
        Disposable sub = mSlaveComputer.getLaserBluetoothStatus()
                .subscribe(status -> {
                    Logger.d("Laser Camera status " + status.isReady() + "," + status.getMacAddress());
                    if (status.isReady()) {
                        mLaserCameraAddress = status.getMacAddress();

                        // remove pair records if count is over 10
                        if (mLaserCameraController.getBondedDeviceCount() > 10) {
                            mLaserCameraController.removeBondedDeviceRecords();
                        }
                        connectLaserCamera();
                    } else {
                        // wait another 10s
                        AndroidSchedulers.mainThread().scheduleDirect(this::waitForLaserCameraReady, 10000, TimeUnit.MILLISECONDS);
                    }
                });
        disposables.add(sub);
    }

    private void connectLaserCamera() {
        // update connection status
        mLaserCameraController.updateConnectionStatus();

        // reconnect if bluetooth is disconnected
        if (!mLaserCameraController.isConnected()) {
            Disposable sub = mLaserCameraController.connect(mLaserCameraAddress)
                    .observeOn(Schedulers.computation())
                    .subscribe(success -> {
                        if (success) {
                            Logger.d("Laser Camera connected!");
                            onLaserCameraConnected();
                        } else {
                            Logger.d("Laser Camera connect failed!");
                            onLaserCameraConnectFailed();
                        }
                    }, LogHelper::log);
            disposables.add(sub);
        } else {
            onLaserCameraConnected();
        }
    }

    private void onLaserCameraConnected() {
        mLaserCameraInterval = 10;
        AndroidSchedulers.mainThread().scheduleDirect(this::connectLaserCamera, 300, TimeUnit.SECONDS);
    }

    private void onLaserCameraConnectFailed() {
        mLaserCameraInterval = Math.max(3600, mLaserCameraInterval * 2);
        AndroidSchedulers.mainThread().scheduleDirect(this::connectLaserCamera, mLaserCameraInterval, TimeUnit.SECONDS);
    }

    // Laser 10w
    public byte getLaser10WErrorState() {
        return mLaser10WErrorState;
    }

    public void setLaser10WErrorState(byte laser10WErrorState) {
        this.mLaser10WErrorState = laser10WErrorState;
    }
    // Laser 20w

    // Laser 40w

    // CNC

    public enum WorkType {
        NONE,
        FDM,
        LASER,
        CNC
    }

}
