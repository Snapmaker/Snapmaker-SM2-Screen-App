package fabscreen.libraries.legacy.data;


import java.util.List;

import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacket;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderTemperature;
import io.reactivex.Observable;

public interface ISlaveComputer {

    int FLAG_X = 1;
    int FLAG_Y = 1 << 1;
    int FLAG_Z = 1 << 2;
    int FLAG_B = 1 << 3;
    int FLAG_XY = FLAG_X | FLAG_Y;
    int FLAG_XYZ = FLAG_X | FLAG_Y | FLAG_Z;
    int FLAG_XYZB = FLAG_X | FLAG_Y | FLAG_Z | FLAG_B;

    void connect(String device);

    Observable<Boolean> getConnectedObservable();

    void setHeartbeatEnabled(boolean enabled);

    FabPacketContent.MachineStatus getMachineStatus();

    Observable<FabPacketContent.MachineStatus> getMachineStatusObservable();

    Observable<Boolean> getMachineStatusValidObservable();

    boolean isMachineStatusValid();

    void send(FabPacket packet);

    /**
     * Send G-code (not print) to serial port (0x01).
     *
     * @param gcode G-code command to be sent.
     */
    Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode);

    Observable<FabPacketContent.GcodeResponse> sendGcode(String gcode, boolean replyContent);

    // 0x03 Print G-code request
    Observable<FabPacketContent.GcodeResponse> sendPrintGcode(String gcode, int lineno);

    // 0x13 Batch Print G-code request
    void sendPrintBatchGcode(int startLine, int endLine, String gcode);

    // 0x07 Machine Status
    // 0x07 0x01
    Observable<FabPacketContent.MachineStatus> requestMachineStatus();

    // 0x07 0x02
    Observable<FabPacketContent.MachineErrors> getMachineErrors();

    Observable<FabPacketContent.MachineErrors> getMachineErrors(int timeout);

    Observable<FabPacketContent.MachineErrors> watchMachineErrors();

    // 0x07 0x03
    Observable<Integer> start();

    Observable<Integer> pause();

    Observable<Integer> resume();

    Observable<Integer> stop();

    Observable<Integer> finish();

    Observable<Integer> getLineNumber();

    /**
     * Enable the Batch Print Gcode.  (0x07 0x12)
     */
    Observable<Integer> useBatchGcodeMode(int check);

    /**
     * Status request: Reset Error flag. (0x07 0x0a)
     */
    Observable<Integer> resetErrorFlag();

    /**
     * 0x08 0x0b
     */
    Observable<Integer> resumeFromPowerOutage();

    /**
     * 0x08 0x0c
     * WAIT event, send only by FW.
     * <p>
     * When firmware buffer is drained, it may send WAIT event asking for more G-code.
     * It sendsWAIT event when in print mode only.
     */
    Observable<Boolean> watchWaitEvents();

    // 0x07 0x0e
    Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem();

    Observable<FabPacketContent.CoordinateSystem> requestCoordinateSystem(int timeout);

    // 0x09 Machine Settings
    Observable<Boolean> setWorkspace(int xSize, int xHomeOffset, int xMaxDir, int xStepperDir,
                                     int ySize, int yHomeOffset, int yMaxDir, int yStepperDir,
                                     int zSize, int zHomeOffset, int zMaxDir, int zStepperDir);

    Observable<Boolean> startAutoCalibration();

    Observable<Boolean> startAutoCalibration(int grid);

    Observable<Integer> getAutoCalibrationProgress();

    Observable<Boolean> startManualCalibration();

    Observable<Boolean> startManualCalibration(int grid);

    Observable<Boolean> gotoCalibrationPoint(int point);

    Observable<Boolean> moveCalibrationPoint(double offset);

    /**
     * Settings: Save Calibration. (0x09 0x07)
     */
    Observable<Boolean> saveCalibration();

    Observable<Boolean> exitCalibration();

    Observable<Boolean> resetCalibration();

    Observable<FabPacketContent.MachineSize> getMachineSize();

    /**
     * Settings: Get Focal Length (0x09 0x0a)
     */
    Observable<Float> getLaserFocalLength();

    Observable<Float> getLaserFocalLength(int timeout);

    // 0x09 0x0b
    Observable<Boolean> setLaserFocalLength(float focalLength);

    // 0x09 0x0c
    Observable<Boolean> startLaserFocusSetting(float xPosition, float yPosition, float zPosition);

    // 0x09 0x0d
    Observable<Boolean> startLaserFineTune();

    Observable<Boolean> startLaserFineTune(float zOffset);

    /**
     * Settings: Fast 3DP Calibration (0x09 0x0e)
     */
    Observable<Integer> fastCalibration();

    /**
     * Settings: Adjust Settings (0x09 0x0f)
     */
    Observable<Integer> requestAdjustSetting(int type, float value);

    Observable<Integer> requestAdjustSettingFeedRate(float value);

    Observable<Integer> requestAdjustSettingNozzleTemp(float value);
    Observable<Integer> requestAdjustSettingNozzleTemp(int which, float value);

    Observable<Integer> requestAdjustSettingHeatedBedTemp(float value);

    Observable<Integer> requestAdjustSettingLaserPower(float value);

    Observable<Integer> requestAdjustSettingZOffset(float value);

    Observable<Integer> requestAdjustSettingZOffset(int which, float value);

    Observable<Integer> requestAdjustSettingFlowRate(int which, float value);

    Observable<Integer> requestAdjustSettingCNCPower(float value);

    Observable<FabPacketContent.AdjustSettings> getAdjustSetting(int type);

    Observable<Boolean> setAFAssistLightState(int state);

    Observable<FabPacketContent.AdjustSettings> getAdjustSettingFeedRate();

    Observable<FabPacketContent.AdjustSettings> getAdjustSettingLaserPower();

    Observable<FabPacketContent.AdjustSettings> getAdjustSettingZOffset();

    Observable<FabPacketContent.AdjustSettings> getAdjustSettingCNCPower();

    Observable<FabPacketContent.ExtendKitInfo> requestExtendKitInfo();

    Observable<Byte> setExtendKitInfo(boolean isQuickSwapInstalled,  byte extendKitConf);

    /**
     * 0x09 0x15
     */
    Observable<Boolean> checkCalibrationEverSucceeded();

    /**
     * Movement: Home Z (G28 Z) (0x0b 0x01)
     */
    // 0x0b
    Observable<Boolean> gotoZHome();

    /**
     * Movement: Set Position (G92), this method is used to solve the G92 saving issue temporarily.
     * <p>
     * G92 X{} Y{} Z{}
     */
    Observable<Boolean> setPosition(float x, float y, float z, int flag);

    Observable<Boolean> setPosition(float x, float y, float z, float b, int flag);

    Observable<Boolean> gotoAbsolutePosition(float x, float y, float z);

    Observable<Boolean> gotoAbsolutePosition(float x, float y, float z, float f);

    Observable<Boolean> gotoRelativePosition(float x, float y, float z);

    Observable<Boolean> gotoRelativePosition(float x, float y, float z, float f);

    Observable<String> getControllerVersion();

    Observable<Boolean> startUpdate();

    Observable<Short> watchPacketIndexRequest();

    /**
     * Movement: Request Extrusion (0x0b 0x04)
     */
    Observable<Boolean> requestExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut);

    /**
     * 0xa9 0x01
     * 0xa9 0x02
     */
    void sendUpdatePackage(byte opCode, short index, byte[] content);

    void requestModuleVersion();

    Observable<FabPacketContent.ModuleVersion> watchModuleVersion();

    // 0x0d 0x01
    Observable<Boolean> setupLaserNetwork(String SSID, String password);

    Observable<FabPacketContent.LaserWifiStatus> getLaserWifiStatus();

    // 0x0d 0x05
    Observable<FabPacketContent.LaserBtStatus> getLaserBluetoothStatus();

    /**
     * Add-on: Get enclosure status (0x11 0x01)
     */
    Observable<FabPacketContent.EnclosureStatus> getEnclosureStatus();

    /**
     * Add-on: Set enclosure Led (0x11 0x02)
     */
    Observable<Boolean> setEnclosureLed(int value);

    /**
     * Add-on: Set enclosure Fan (0x11 0x03)
     */
    Observable<Boolean> setEnclosureFan(int value);

    /**
     * Add-on: Set enclosure Door Detection (0x11 0x04)
     */
    Observable<Boolean> setEnclosureDoorDetection(boolean enabled);

    // 0x11 0x08
    Observable<Byte> requestRotaryModuleStatus();

    // 0x11 0x07
    Observable<Byte> requestEmergencyStopStatus();

    Observable<Byte> watchEmergencyStopStatus();

    void onEmergencyStop();

    Observable<FabPacketContent.AirPurifierStatus> requestAirPurifierAddOnStatus();

    Observable<FabPacketContent.AirPurifierStatus> watchAirPurifierAddOnStatus();

    Observable<FabPacketContent.AirPurifierFan> requestAirPurifierFan();

    Observable<Boolean> setAirPurifierEnabled(boolean enabled);

    Observable<Boolean> setAirPurifierFanSpeedLevel(int level);

    Observable<Integer> getAirPurifierFilterLifeTime();

    Observable<Integer> watchAirPurifierFilterLifeTime();

    Observable<FabPacketContent.BatchGcodeResponse> getBatchGcodeResponseSubject();

    Observable<FabPacketContent.MasterState> getMasterState();

    /**
     * Request execution header security status (07 11)
     */
    Observable<FabPacketContent.HeaderSecurity> requestHeaderSecurityStatus();

    Observable<FabPacketContent.HeaderSecurity> watchHeaderSecurityStatus();

    /**
     * Listen to the master actively suspend the task signal (08 04)
     */
    Observable<Integer> watchPrintPauseState();

    /**
     * Request header Online Sync ID (09 12)
     */
    Observable<Integer> requestHeaderOnlineSyncId(int timeout);

    /**
     * Set header Online Sync ID (09 13)
     *
     * @param headerId
     * @return
     */
    Observable<Boolean> setHeaderOnlineSyncId(int headerId);

    void setAbnormalTemperatureRange(int protectTemperature, int recoveryTemperature);


    /*--------------3dp dual-extruder----------------*/

    /**
     * 0713
     */
    Observable<List<ExtruderModel>> getExtruderModel();

    /**
     * 0714
     *
     * @return [bool_left, bool_right] true-has filament; false-no filament
     */
    Observable<List<Boolean>> getExtrudersHaveFilament();

    /**
     * 0715
     *
     * @return [left, right]
     */
    Observable<List<ExtruderTemperature>> getExtruderTemperatures();

    // 090f

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Boolean> setWorkSpeed(int which, float value);

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Boolean> setExtruderTemperature(int which, float value);

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Boolean> setLiveZOffset(int which, float value);

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Boolean> setFlowRate(int which, float value);

    //0910

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Float> getWorkSpeed(int which);

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Float> getLiveZOffset(int which);

    //0918

    /**
     * @param which 0-left; 1-right.
     */
    Observable<Boolean> switchToExtruder(int which);

    //0919

    /**
     * @return [x, y, z]
     */
    Observable<List<Float>> getExtruderOffset();

    //091a

    /**
     * @param which 0-left; 1-right.
     * @deprecated not need
     */
    @Deprecated
    Observable<Boolean> confirmExtruderPosition(int which);


    //091b

    /**
     * @param direction 0-x; 1-y; 2-z.
     */
    Observable<Boolean> setExtruderOffset(int direction, float offset);

    //091c

    /**
     * @param which 0-left model; 1-right model; 2-throat
     * @param speed 0~255
     */
    Observable<Boolean> setToolheadFanSpeed(int which, int speed);


    //091d sensor calibration

    /**
     * @param which  0-left; 1-right
     * @param isAuto auto touch or not
     */
    Observable<Boolean> calibrateSensorTouchBed(int which, boolean isAuto);

    Observable<Boolean> calibrateSensorManualConfirm(int which);

    Observable<Boolean> calibrationSensorRequestAbort();

    // 091e,091f

    /**
     * @param which  0-left; 1-right
     * @param isAuto auto touch or not
     */
    Observable<Boolean> probeBedPosition(int which, boolean isAuto);

    /**
     * Only need for manual probe
     */
    Observable<Boolean> saveBedPosition();

    /**
     * 0920
     *
     * @param grid eg:11-11*11
     */
    Observable<Boolean> startDualExtruderAutoLeveling(int grid);

    /**
     * 0921
     *
     * @param point 1<=point<=121
     */
    Observable<Integer> dualExtruderAutoLevelPoint(int point);

    /**
     * 0922
     * Save result and make leveling data work.
     */
    Observable<Boolean> finishDualExtruderAutoLeveling();

    /**
     * 0923
     *
     * @param grid eg:11-11*11
     */
    Observable<Boolean> startDualExtruderManualLeveling(int grid);

    /**
     * 0924
     *
     * @param point 1<=point<=121
     */
    Observable<Boolean> dualExtruderManualLevelPoint(int point);

    /**
     * 0925
     * Save result and make leveling data work.
     */
    Observable<Boolean> finishDualExtruderManualLeveling();

    /**
     * 0926
     * Get activated extruder index. Will always return 0 if machine not homed.
     *
     * @return 0-left; 1-right
     */
    Observable<Integer> getActivatedExtruder();

    Observable<Boolean> setCrossLineLaserIndicator(boolean active);

    public Observable<Boolean> getCrossLineLaserIndicatorStatus();

    Observable<Short> getFireSensorSensitivity();

    Observable<Boolean> setFireSensorSensitivity(int sensitivity);

    Observable<Boolean> setCrossLineIndicatorOffset(float xOffset, float yOffset);

    Observable<FabPacketContent.CrossLineIndicatorOffset> getCrossLineIndicatorOffset();

    Observable<Boolean> setPrintOffsetWithCrossLine(boolean enabled);

    Observable<Float> getLaserIndicatorPower();
    Observable<Boolean> setLaserIndicatorPower(float power);

    /**
     * 0b05
     *
     * @param direction 0-extrude; 1-pullback
     * @param speed     mm/s int float
     */
    Observable<Boolean> extrudeInfinitely(int direction, int speed);

    /**
     * 0b06
     * Stop all axes from moving.
     */
    Observable<Boolean> stopMovement();
}
