package fabscreen.libraries.legacy.data;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderTemperature;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class MockSlaveComputer implements ISlaveComputer {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private BehaviorSubject<Boolean> mConnectedSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<FabPacketContent.MachineStatus> mStatusSubject = BehaviorSubject.createDefault(new FabPacketContent.MachineStatus());
    private Random mRandom;

    public MockSlaveComputer() {
        mRandom = new Random(System.currentTimeMillis());
    }

    private FabPacketContent.MachineStatus getFakeMachineStatus() {
        FabPacketContent.MachineStatus machineStatus = new FabPacketContent.MachineStatus();
        machineStatus.isDefault = false;
        machineStatus.headStatus = Constants.HEAD_LASER_40W;
        machineStatus.headTemperature = mRandom.nextInt(250);
        machineStatus.extruder1Temperature = mRandom.nextInt(250);
        machineStatus.headTargetTemperature = 200;
        machineStatus.extruder1TargetTemperature = 150;
        machineStatus.bedTemperature = 60 - mRandom.nextInt(5);
        machineStatus.bedTargetTemperature = 60;
        machineStatus.feedRate = 666;
        machineStatus.x = mRandom.nextDouble();
        machineStatus.y = mRandom.nextDouble();
        machineStatus.z = mRandom.nextDouble();
        machineStatus.printerStatus = 3;
        return machineStatus;
    }

    public void connect(String device) {
        mConnectedSubject.onNext(true);

        Disposable sub = Observable.interval(1000, TimeUnit.MILLISECONDS)
                .subscribe(tick -> mStatusSubject.onNext(getFakeMachineStatus()));
        compositeDisposable.add(sub);
    }

    @Override
    public Observable<Boolean> getConnectedObservable() {
        return mConnectedSubject;
    }

    @Override
    public void setHeartbeatEnabled(boolean enabled) {
    }

    @Override
    public FabPacketContent.MachineStatus getMachineStatus() {
        return mStatusSubject.getValue();
    }

    @Override
    public Observable<FabPacketContent.MachineStatus> getMachineStatusObservable() {
        return mStatusSubject;
    }

    @Override
    public Observable<Boolean> getMachineStatusValidObservable() {
        return Observable.just(true);
    }

    @Override
    public boolean isMachineStatusValid() {
        return true;
    }

    @Override
    public void send(FabPacket packet) {
    }

    @Override
    public Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode) {
        return Observable.just(new FabPacketContent.GcodeResponse()).delay(10, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<FabPacketContent.GcodeResponse> sendPrintGcode(String gcode, int lineno) {
        return Observable.just(new FabPacketContent.GcodeResponse()).delay(10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void sendPrintBatchGcode(int startLine, int endLine, String gcode) {
    }

    public Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode, boolean content) {
        return Observable.just(new FabPacketContent.GcodeResponse()).delay(10, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<FabPacketContent.MachineStatus> requestMachineStatus() {
        return Observable.just(getFakeMachineStatus());
    }

    @Override
    public Observable<FabPacketContent.MachineErrors> getMachineErrors() {
        FabPacketContent.MachineErrors errors = new FabPacketContent.MachineErrors();
        return Observable.just(errors);
    }

    @Override
    public Observable<FabPacketContent.MachineErrors> getMachineErrors(int timeout) {
        FabPacketContent.MachineErrors errors = new FabPacketContent.MachineErrors();
        return Observable.just(errors);
    }

    @Override
    public Observable<FabPacketContent.MachineErrors> watchMachineErrors() {
        return PublishSubject.create();
    }

    @Override
    public Observable<Boolean> watchWaitEvents() {
        return PublishSubject.create();
    }

    @Override
    public Observable<Integer> start() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> pause() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> resume() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> stop() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> finish() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> getLineNumber() {
        return Observable.just(10000);
    }

    @Override
    public Observable<Integer> useBatchGcodeMode(int check) {
        return Observable.just(-1);
    }

    @Override
    public Observable<Integer> resetErrorFlag() {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> resumeFromPowerOutage() {
        return Observable.just(0);
    }

    private int count = 0;

    @Override
    public Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem() {
        if (++count < 2) {
            FabPacketContent.CoordinateSystem coordinateSystem = new FabPacketContent.CoordinateSystem();
            coordinateSystem.homed = false;
            coordinateSystem.coordinateAligned = false;
            coordinateSystem.coordinateID = 0;
            coordinateSystem.coordinateX = 1;
            coordinateSystem.coordinateY = 2;
            coordinateSystem.coordinateZ = 3;
            return Observable.just(coordinateSystem);
        } else {
            FabPacketContent.CoordinateSystem coordinateSystem = new FabPacketContent.CoordinateSystem();
            coordinateSystem.homed = true;
            coordinateSystem.coordinateAligned = true;
            coordinateSystem.coordinateID = 1;
            coordinateSystem.coordinateX = 1;
            coordinateSystem.coordinateY = 2;
            coordinateSystem.coordinateZ = 3;
            return Observable.just(coordinateSystem);
        }
    }

    @Override
    public Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem(int timeout) {
        if (++count < 2) {
            FabPacketContent.CoordinateSystem coordinateSystem = new FabPacketContent.CoordinateSystem();
            coordinateSystem.homed = false;
            coordinateSystem.coordinateAligned = false;
            coordinateSystem.coordinateID = 0;
            coordinateSystem.coordinateX = 1;
            coordinateSystem.coordinateY = 2;
            coordinateSystem.coordinateZ = 3;
            return Observable.just(coordinateSystem);
        } else {
            FabPacketContent.CoordinateSystem coordinateSystem = new FabPacketContent.CoordinateSystem();
            coordinateSystem.homed = true;
            coordinateSystem.coordinateAligned = true;
            coordinateSystem.coordinateID = 1;
            coordinateSystem.coordinateX = 1;
            coordinateSystem.coordinateY = 2;
            coordinateSystem.coordinateZ = 3;
            return Observable.just(coordinateSystem);
        }
    }

    @Override
    public Observable<Boolean> setWorkspace(int xSize, int xHomeOffset, int xMaxDir, int xStepperDir, int ySize, int yHomeOffset, int yMaxDir, int yStepperDir, int zSize, int zHomeOffset, int zMaxDir, int zStepperDir) {
        return Observable.just(true);
    }

    private PublishSubject<Boolean> mAutoCalibration = PublishSubject.create();

    @Override
    public Observable<Boolean> startAutoCalibration() {
        return mAutoCalibration;
    }

    @Override
    public Observable<Boolean> startAutoCalibration(int grid) {
        return mAutoCalibration;
    }

    @Override
    public Observable<Integer> getAutoCalibrationProgress() {
        PublishSubject<Integer> subject = PublishSubject.create();
        int[] points = new int[]{1, 2, 3, 6, 9, 8, 7, 4, 5};
//        points = new int[]{1,2,3,4,5,10,15,20,25,24,23,22,21,16,11,6,7,8,9,14,19,18,17,12,13};
        for (int i = 0; i < 9; i++) {
            int point = points[i];
            AndroidSchedulers.mainThread().scheduleDirect(() -> {
                subject.onNext(point);

                if (point == 5) {
                    mAutoCalibration.onNext(true);
                }
            }, 1000 * i, Constants.TIME_UNIT);
        }
        return subject;
    }

    @Override
    public Observable<Boolean> startManualCalibration() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> startManualCalibration(int grid) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> gotoCalibrationPoint(int point) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> moveCalibrationPoint(double offset) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> saveCalibration() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> exitCalibration() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> resetCalibration() {
        return Observable.just(true);
    }

    // 0x09 0x0e
    @Override
    public Observable<Integer> fastCalibration() {
        return Observable.just(0);
    }

    // 0x09 0x0f
    @Override
    public Observable<Integer> requestAdjustSetting(int type, float value) {
        return Observable.just(0);
    }

    @Override
    public Observable<Integer> requestAdjustSettingFeedRate(float value) {
        return requestAdjustSetting(0, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingNozzleTemp(float value) {
        return requestAdjustSetting(1, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingNozzleTemp(int which, float value) {
        return requestAdjustSetting(which == 0 ? 1 : 7, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingHeatedBedTemp(float value) {
        return requestAdjustSetting(2, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingLaserPower(float value) {
        return requestAdjustSetting(3, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingZOffset(float value) {
        return requestAdjustSetting(4, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingZOffset(int which, float value) {
        return requestAdjustSetting(which == 0 ? 4 : 8, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingFlowRate(int which, float value) {
        return requestAdjustSetting(which == 0 ?  9 : 10, value);
    }

    @Override
    public Observable<Integer> requestAdjustSettingCNCPower(float value) {
        return requestAdjustSetting(5, value);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSetting(int type) {
        FabPacketContent.AdjustSettings settings = new FabPacketContent.AdjustSettings();
        settings.retCode = 0;
        settings.value = 5;
        return Observable.just(settings);
    }

    @Override
    public Observable<Boolean> setAFAssistLightState(int state) {
        return Observable.just(true);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingFeedRate() {
        return getAdjustSetting(0);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingLaserPower() {
        return getAdjustSetting(1);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingZOffset() {
        return getAdjustSetting(4);
    }

    @Override
    public Observable<FabPacketContent.AdjustSettings> getAdjustSettingCNCPower() {
        return getAdjustSetting(5);
    }

    @Override
    public Observable<FabPacketContent.ExtendKitInfo> requestExtendKitInfo() {
        return Observable.just(new FabPacketContent.ExtendKitInfo((byte) 1, (byte) 0));
    }

    @Override
    public Observable<Byte> setExtendKitInfo(boolean isQuickSwapOn, byte extendKitConf) {
        return Observable.just((byte) 0);
    }

    @Override
    public Observable<FabPacketContent.MachineSize> getMachineSize() {
        FabPacketContent.MachineSize machineSize = new FabPacketContent.MachineSize();
        machineSize.machineModel = Constants.MACHINE_MODEL_SNAPMAKER_A250;
        machineSize.xSize = 123;
        machineSize.ySize = 323;
        machineSize.zSize = 523;
        return Observable.just(machineSize);
    }

    @Override
    public Observable<Boolean> checkCalibrationEverSucceeded() {
        return Observable.just(true);
    }

    @Override
    public Observable<Float> getLaserFocalLength() {
        return Observable.just(22.1f);
    }

    @Override
    public Observable<Float> getLaserFocalLength(int timeout) {
        return Observable.just(22.1f);
    }

    @Override
    public Observable<Boolean> setLaserFocalLength(float focalLength) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> startLaserFocusSetting(float xPosition, float yPosition, float zPosition) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> startLaserFineTune() {
        return Observable.just(true).delay(5000, Constants.TIME_UNIT);
    }

    @Override
    public Observable<Boolean> startLaserFineTune(float zOffset) {
        return Observable.just(true).delay(5000, Constants.TIME_UNIT);
    }

    @Override
    public Observable<Boolean> gotoZHome() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setPosition(float x, float y, float z, int flag) {
        return Observable.just(true).delay(2500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Boolean> setPosition(float x, float y, float z, float b, int flag) {
        return Observable.just(true).delay(2500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Boolean> gotoAbsolutePosition(float x, float y, float z) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> gotoAbsolutePosition(float x, float y, float z, float f) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> gotoRelativePosition(float x, float y, float z) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> gotoRelativePosition(float x, float y, float z, float f) {
        return Observable.just(true);
    }

    @Override
    public Observable<String> getControllerVersion() {
        return Observable.just("Developer version");
    }

    @Override
    public Observable<Boolean> startUpdate() {
        return Observable.just(true);
    }

    @Override
    public Observable<Short> watchPacketIndexRequest() {
        PublishSubject<Short> subject = PublishSubject.create();
        for (int i = 1; i <= 3; i++) {
            short moduleId = (short) i;
            new Handler().postDelayed(() -> {
                subject.onNext(moduleId);
            }, i * 3000);
        }
        return subject;
    }

    @Override
    public void sendUpdatePackage(byte opCode, short index, byte[] content) {
        //
    }

    @Override
    public void requestModuleVersion() {
        //
    }

    @Override
    public Observable<Boolean> requestExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut) {
        return Observable.just(true).delay(2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<FabPacketContent.ModuleVersion> watchModuleVersion() {
        PublishSubject<FabPacketContent.ModuleVersion> subject = PublishSubject.create();
        for (int i = 0; i < 3; i++) {
            FabPacketContent.ModuleVersion version = new FabPacketContent.ModuleVersion();
            version.moduleID = i + 0x1fffffff;
            version.version = "v1.9." + i + "-alpha1";
            new Handler().postDelayed(() -> {
                subject.onNext(version);
            }, i * 200);
        }
        return subject;
    }

    @Override
    public Observable<Boolean> setupLaserNetwork(String SSID, String password) {
        return Observable.just(true);
    }

    @Override
    public Observable<FabPacketContent.LaserWifiStatus> getLaserWifiStatus() {
        FabPacketContent.LaserWifiStatus laserWifiStatus = new FabPacketContent.LaserWifiStatus();
        laserWifiStatus.address = "6.6.6.6";
        laserWifiStatus.networkStatus = 1; // connected
        return Observable.just(laserWifiStatus);
    }

    @Override
    public Observable<FabPacketContent.LaserBtStatus> getLaserBluetoothStatus() {
        FabPacketContent.LaserBtStatus laserBtStatus = new FabPacketContent.LaserBtStatus();
        laserBtStatus.status = 0;
        laserBtStatus.macAddress = "FF:FF:FF:FF:FF:FF";
        return Observable.just(laserBtStatus);
    }

    @Override
    public Observable<FabPacketContent.EnclosureStatus> getEnclosureStatus() {
        FabPacketContent.EnclosureStatus enclosureStatus = new FabPacketContent.EnclosureStatus();
        enclosureStatus.enclosureStatus = 0;
        enclosureStatus.ledLevel = 0;
        enclosureStatus.fanLevel = 0;
        enclosureStatus.enclosureEnabled = true;
        return Observable.just(enclosureStatus);
    }

    @Override
    public Observable<Boolean> setEnclosureLed(int value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setEnclosureFan(int value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setEnclosureDoorDetection(boolean enabled) {
        return Observable.just(true);
    }

    @Override
    public Observable<Byte> requestRotaryModuleStatus() {
        return Observable.just((byte) 0);
    }

    @Override
    public Observable<Byte> requestEmergencyStopStatus() {
        return Observable.just((byte) 0);
    }

    @Override
    public Observable<Byte> watchEmergencyStopStatus() {
        return Observable.just((byte) 0);
    }

    @Override
    public void onEmergencyStop() {

    }

    @Override
    public Observable<FabPacketContent.AirPurifierStatus> requestAirPurifierAddOnStatus() {
        FabPacketContent.AirPurifierStatus status = new FabPacketContent.AirPurifierStatus();
        status.status = (byte) 0x00;
        status.errorBit = (byte) 0;
        return Observable.just(status);
    }

    @Override
    public Observable<FabPacketContent.AirPurifierStatus> watchAirPurifierAddOnStatus() {
        return PublishSubject.create();
    }

    @Override
    public Observable<FabPacketContent.AirPurifierFan> requestAirPurifierFan() {
        FabPacketContent.AirPurifierFan airPurifierFan = new FabPacketContent.AirPurifierFan();
        airPurifierFan.isOn = true;
        airPurifierFan.level = (byte) 1;
        return Observable.just(airPurifierFan);
    }

    @Override
    public Observable<Boolean> setAirPurifierEnabled(boolean enabled) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setAirPurifierFanSpeedLevel(int level) {
        return Observable.just(true);
    }

    @Override
    public Observable<Integer> getAirPurifierFilterLifeTime() {
        return Observable.just(1);
    }

    @Override
    public Observable<Integer> watchAirPurifierFilterLifeTime() {
        return Observable.just(1);
    }

    @Override
    public Observable<FabPacketContent.BatchGcodeResponse> getBatchGcodeResponseSubject() {
        return null;
    }

    @Override
    public Observable<FabPacketContent.MasterState> getMasterState() {
        return Observable.just(new FabPacketContent.MasterState());
    }

    @Override
    public Observable<FabPacketContent.HeaderSecurity> requestHeaderSecurityStatus() {
        return Observable.just(new FabPacketContent.HeaderSecurity((byte) 0));
    }


    @Override
    public Observable<FabPacketContent.HeaderSecurity> watchHeaderSecurityStatus() {
        return Observable.just(new FabPacketContent.HeaderSecurity((byte) 0));
    }

    @Override
    public Observable<Integer> requestHeaderOnlineSyncId(int timeout) {
        return Observable.just(1);
    }


    @Override
    public Observable<Boolean> setHeaderOnlineSyncId(int headerId) {
        return Observable.just(true);
    }

    @Override
    public void setAbnormalTemperatureRange(int protectTemperature, int recoveryTemperature) {
    }

    @Override
    public Observable<Integer> watchPrintPauseState() {
        return Observable.just(1);
    }

    @Override
    public Observable<List<ExtruderModel>> getExtruderModel() {
        List<ExtruderModel> models = new ArrayList<>();
        models.add(new ExtruderModel(ExtruderModel.BRASS, 0.5f));
        models.add(new ExtruderModel(ExtruderModel.HARD_STEEL, 0.3f));
        return Observable.just(models);
    }

    @Override
    public Observable<List<Boolean>> getExtrudersHaveFilament() {
        List<Boolean> booleans = new ArrayList<>();
        booleans.add(false);
        booleans.add(false);
        return Observable.just(booleans);
    }

    @Override
    public Observable<List<ExtruderTemperature>> getExtruderTemperatures() {
        List<ExtruderTemperature> temperatures = new ArrayList<>();
        temperatures.add(new ExtruderTemperature(100, 200));
        temperatures.add(new ExtruderTemperature(120, 200));
        return Observable.just(temperatures);
    }

    @Override
    public Observable<Boolean> setWorkSpeed(int which, float value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setExtruderTemperature(int which, float value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setLiveZOffset(int which, float value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setFlowRate(int which, float value) {
        return Observable.just(true);
    }

    @Override
    public Observable<Float> getWorkSpeed(int which) {
        return Observable.just(1.5f);
    }

    @Override
    public Observable<Float> getLiveZOffset(int which) {
        return Observable.just(10f);
    }

    @Override
    public Observable<Boolean> switchToExtruder(int which) {
        return Observable.just(true);
    }

    @Override
    public Observable<List<Float>> getExtruderOffset() {
        List<Float> offsets = new ArrayList<>();
        offsets.add(10f);
        offsets.add(10f);
        offsets.add(10f);
        return Observable.just(offsets);
    }

    @Override
    public Observable<Boolean> confirmExtruderPosition(int which) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setExtruderOffset(int direction, float offset) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> setToolheadFanSpeed(int which, int speed) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> calibrateSensorTouchBed(int which, boolean isAuto) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> calibrateSensorManualConfirm(int which) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> calibrationSensorRequestAbort() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> probeBedPosition(int which, boolean isAuto) {
        return Observable.just(true).delay(2, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Boolean> saveBedPosition() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> startDualExtruderAutoLeveling(int grid) {
        return Observable.just(true);
    }

    @Override
    public Observable<Integer> dualExtruderAutoLevelPoint(int point) {
        return Observable.just(0).delay(200, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Boolean> finishDualExtruderAutoLeveling() {
        return Observable.just(true).delay(3, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Boolean> startDualExtruderManualLeveling(int grid) {
        return Observable.just(true).delay(2, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Boolean> dualExtruderManualLevelPoint(int point) {
        return Observable.just(true).delay(1, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Boolean> finishDualExtruderManualLeveling() {
        return Observable.just(true).delay(1, TimeUnit.SECONDS);
    }

    @Override
    public Observable<Integer> getActivatedExtruder() {
        return Observable.just(new Random().nextInt(2));
    }

    @Override
    public Observable<Boolean> setCrossLineLaserIndicator(boolean active) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> getCrossLineLaserIndicatorStatus() {
        return Observable.just(false);
    }

    @Override
    public Observable<Boolean> setFireSensorSensitivity(int sensitivity) {
        return Observable.just(true);
    }

    @Override
    public Observable<Short> getFireSensorSensitivity() {
        short value = 50;
        return Observable.just(value);
    }

    @Override
    public Observable<Boolean> setCrossLineIndicatorOffset(float xOffset, float yOffset) {
        return Observable.just(true);
    }

    @Override
    public Observable<FabPacketContent.CrossLineIndicatorOffset> getCrossLineIndicatorOffset() {
        return Observable.just(new FabPacketContent.CrossLineIndicatorOffset(-1, -1));
    }

    @Override
    public Observable<Boolean> setPrintOffsetWithCrossLine(boolean enabled) {
        return Observable.just(true);
    }

    @Override
    public Observable<Float> getLaserIndicatorPower() {
        return Observable.just(1f);
    }

    @Override
    public Observable<Boolean> setLaserIndicatorPower(float power) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> extrudeInfinitely(int direction, int speed) {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> stopMovement() {
        return Observable.just(true);
    }
}
