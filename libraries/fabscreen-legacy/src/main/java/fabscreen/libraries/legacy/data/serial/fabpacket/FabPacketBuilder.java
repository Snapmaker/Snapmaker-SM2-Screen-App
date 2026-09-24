package fabscreen.libraries.legacy.data.serial.fabpacket;

import androidx.annotation.NonNull;

import okio.Buffer;
import okio.ByteString;

public class FabPacketBuilder {
    private static FabPacket buildPacket(byte eventId, byte[] content) {
        FabPacket packet = FabPacket.create();
        packet.setEventId(eventId);
        packet.setContent(content);
        packet.build();
        return packet;
    }

    public static FabPacket gcodeRequest(String gcode, int lineno) {
        Buffer buffer = new Buffer();
        buffer.writeInt(lineno);
        buffer.write(gcode.getBytes());

        return buildPacket(FabPacket.GCODE_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket printGcodeRequest(@NonNull String gcode, int lineno) {
        Buffer buffer = new Buffer();
        buffer.writeInt(lineno);
        buffer.write(gcode.getBytes());

        return buildPacket(FabPacket.PRINT_GCODE_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket printBatchGcodeRequest(int startLine, int endLine, String gcode) {
        Buffer buffer = new Buffer();
        buffer.writeInt(startLine);
        buffer.writeInt(endLine);
        buffer.write(gcode.getBytes());
        byte[] bytes = buffer.readByteArray();
        return buildPacket(FabPacket.PRINT_BATCH_GCODE_REQUEST_EVENT_ID, bytes);
    }

    public static FabPacket extendGcodeRequest() {
        return buildPacket(FabPacket.GCODE_REQUEST_EXTEND_EVENT_ID, new byte[]{(byte) 0x02});
    }

    /*
    public static FabPacket printGcodeResponse(@NonNull String response) {
        Buffer buffer = new Buffer();
        buffer.writeString(response, FabPacketContent.UTF_8);

        return buildPacket(FabPacket.PRINT_GCODE_RESPONSE_EVENT_ID, buffer.readByteArray());
    }
    */

    /**
     * File Operation Request: mount USB to check if USB is plugged in.
     */
    /*
    public static FabPacket fileOperationRequestMount() {
        byte[] content = ByteString.decodeHex("00").toByteArray();
        return buildPacket(FabPacket.FILE_OPERATION_REQUEST_EVENT_ID, content);
    }

    public static FabPacket fileOperationRequestCWD() {
        byte[] content = ByteString.decodeHex("01").toByteArray();
        return buildPacket(FabPacket.FILE_OPERATION_REQUEST_EVENT_ID, content);
    }

    public static FabPacket fileOperationRequestCD(String dir) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x03);
        buffer.write(dir.getBytes());

        return buildPacket(FabPacket.FILE_OPERATION_REQUEST_EVENT_ID, buffer.readByteArray());
    }
    */

    /**
     * File Operation Request: Get files on current directory. The firmware will determine
     * how many filenames to return for a single request, we need to request more than once
     * if there are files of more than one page (e.g. more than 20 files).
     *
     * @param rewind If rewind is true, the file pointer will move to the start and then scan files.
     */
    /*
    public static FabPacket fileOperationRequestGetFiles(boolean rewind) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x04);
        buffer.writeByte(rewind ? 0x01 : 0x00);

        return buildPacket(FabPacket.FILE_OPERATION_REQUEST_EVENT_ID, buffer.readByteArray());
    }
    */


    /**
     * File Operation Request: Print specified file.
     *
     * @param filename name of file to be print (on current directory).
     * @return packet
     */
    /*
    public static FabPacket fileOperationRequestPrintFile(String filename) {
        Buffer buffer = new Buffer();
        buffer.write(ByteString.decodeHex("06").toByteArray());
        buffer.write(filename.getBytes());

        return buildPacket(FabPacket.FILE_OPERATION_REQUEST_EVENT_ID, buffer.readByteArray());
    }
    */

    /**
     * Status Sync Request: Machine Status.
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineStatus() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x01});
    }

    /**
     * Status Sync Request: Anormal Status
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineAbnormalStatus() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x02});
    }

    /**
     * Status Sync Request: Start Print
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineStartPrint() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x03});
    }

    /**
     * Status Sync Request: Pause Print
     *
     * @return packet
     */
    public static FabPacket statusRequestMachinePausePrint() {
        byte[] content = ByteString.decodeHex("04").toByteArray();
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, content);
    }

    /**
     * Status Sync Request: Resume Print
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineResumePrint() {
        byte[] content = ByteString.decodeHex("05").toByteArray();
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, content);
    }

    /**
     * Status Sync Request: Stop Print
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineStopPrint() {
        byte[] content = ByteString.decodeHex("06").toByteArray();
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, content);
    }

    /**
     * Status Sync Request: Finish Print (0x07 0x07)
     *
     * @return packet
     */
    public static FabPacket statusRequestMachineFinishPrint() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x07});
    }

    /**
     * Status Sync Request: Get Line Number (0x07 0x08)
     *
     * @return packet
     */
    public static FabPacket statusRequestLineNumber() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x08});
    }

    /**
     * Status Request: Get Print Progress (0x07 0x09)
     *
     * @return packet
     */
    public static FabPacket statusRequestPrintProgress() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x09});
    }

    /**
     * Status Request: Reset error Status Flag (0x07 0x0a)
     *
     * @return packet
     */
    public static FabPacket statusRequestResetErrorFlag() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x0a});
    }

    /**
     * (0x07 0x0b)
     */
    public static FabPacket statusRequestResumePrint() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x0b});
    }

    public static FabPacket statusWaitRequest() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x0c});
    }

    /**
     * (0x07 0x0e)
     */
    public static FabPacket statusRequestCoodinateSystem() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x0e});
    }

    /**
     * (0x07 0x12)
     */
    public static FabPacket setgcodeBatchSending(int check) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x12);
        buffer.writeByte(check == 0 ? 0x00 : 0x01);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, content);
    }

    /**
     * 0x08 0x01
     */
    public static FabPacket statusSyncMachineStatus(double x, double y, double z, double e) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x01);
        buffer.writeInt((int) (x * 1000));
        buffer.writeInt((int) (y * 1000));
        buffer.writeInt((int) (z * 1000));
        buffer.writeInt((int) (e * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.STATUS_RESPONSE_EVENT_ID, content);
    }

    /**
     * Settings: Set Workspace Size (0x09 0x01)
     */
    public static FabPacket setWorkspace(int xSize, int xHomeOffset, int xMaxDir, int xStepperDir,
                                         int ySize, int yHomeOffset, int yMaxDir, int yStepperDir,
                                         int zSize, int zHomeOffset, int zMaxDir, int zStepperDir) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x01);
        buffer.writeInt(xSize * 1000);
        buffer.writeInt(ySize * 1000);
        buffer.writeInt(zSize * 1000);
        buffer.writeInt(xMaxDir);
        buffer.writeInt(yMaxDir);
        buffer.writeInt(zMaxDir);
        buffer.writeInt(xStepperDir);
        buffer.writeInt(yStepperDir);
        buffer.writeInt(zStepperDir);
        buffer.writeInt(xHomeOffset * 1000);
        buffer.writeInt(yHomeOffset * 1000);
        buffer.writeInt(zHomeOffset * 1000);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Settings: Start Auto Calibration (0x09 0x02)
     */
    public static FabPacket startAutoCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x02});
    }

    public static FabPacket startAutoCalibration(int grid) {
        byte[] content = new byte[]{(byte) 0x02, (byte) grid};
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Settings: Start Manual Calibration (0x09 0x04)
     */
    public static FabPacket startManualCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x04});
    }

    public static FabPacket startManualCalibration(int grid) {
        byte[] content = new byte[]{(byte) 0x04, (byte) grid};
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Settings: Goto Calibration Point (0x09 0x05)
     */
    public static FabPacket gotoCalibrationPoint(int point) {
        byte[] content = new byte[]{(byte) 0x05, (byte) point};
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }


    /**
     * Settings: Move Calibration Point (0x09 0x06)
     */
    public static FabPacket moveCalibrationPoint(double offset) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x06);
        buffer.writeInt((int) (offset * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Settings: Save Calibration (0x09 0x07)
     */
    public static FabPacket saveCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x07});
    }

    /**
     * Settings: Exit Calibration (0x09 0x07)
     */
    public static FabPacket exitCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x08});
    }

    /**
     * Settings: Reset Calibration (0x09 0x09)
     */
    public static FabPacket resetCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x09});
    }

    /**
     * Settings: Get Laser Focus (0x09 0x0a)
     */
    public static FabPacket getLaserFocalLength() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x0a});
    }

    /**
     * Settings: Set Laser Focus (0x09 0x0b)
     */
    public static FabPacket setLaserFocalLength(float focalHeight) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0b);
        buffer.writeInt((int) (focalHeight * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Setting: Start Laser Focus (0x09 0x0c)
     */
    public static FabPacket startLaserFocusSetting(float xPos, float yPos, float zPos) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0c);
        buffer.writeInt((int) (xPos * 1000));
        buffer.writeInt((int) (yPos * 1000));
        buffer.writeInt((int) (zPos * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Setting: Start Laser Fine Tune (0x09 0x0d)
     */
    public static FabPacket startLaserFineTune() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x0d});
    }

    public static FabPacket startLaserFineTune(float zOffset) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0d);
        buffer.writeInt((int) (zOffset * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    // 0x09 0x0e
    public static FabPacket fastCalibration() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x0e});
    }

    // 0x09 0x0f
    public static FabPacket requestAdjustSettings(byte type, float value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0f);
        buffer.writeByte(type);
        buffer.writeInt((int) (value * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getAdjustSettings(byte type) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x10);
        buffer.writeByte(type);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Set auto focus assist light on/off(0x09 0x11)
     *
     * @param state 0:off 1:on
     */
    public static FabPacket setAFAssistLightState(byte state) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x11);
        buffer.writeByte(state);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getMachineSize() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x14});
    }

    /**
     * Setting: Check if Calibration has ever succeeded(0x09 0x15)
     */
    public static FabPacket checkCalibrationEverSucceeded() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x15});
    }

    public static FabPacket requestExtendKitInfo() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x2b});
    }

    // TODO: refactor this into extendKitInfo
    public static FabPacket setExtendKitInfo(byte quickSwapState, byte extendKitConf) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x2c);
        buffer.writeByte(quickSwapState);
        buffer.writeByte(extendKitConf);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Movement: G28 Z (0x0b 0x01)
     */
    public static FabPacket gotoZHome() {
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, new byte[]{(byte) 0x01});
    }

    /**
     * Movement: Absolute axis movement
     */
    public static FabPacket gotoAbsolutePosition(float x, float y, float z) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x02);
        buffer.writeInt((int) (x * 1000));
        buffer.writeInt((int) (y * 1000));
        buffer.writeInt((int) (z * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, content);
    }

    public static FabPacket gotoAbsolutePosition(float x, float y, float z, float f) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x02);
        buffer.writeInt((int) (x * 1000));
        buffer.writeInt((int) (y * 1000));
        buffer.writeInt((int) (z * 1000));
        // FeedRate use mm per second by hsl
        buffer.writeInt((int) (f / 60 * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, content);
    }

    /**
     * Movement: Relative axis movement
     */
    public static FabPacket gotoRelativePosition(float x, float y, float z) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x03);
        buffer.writeInt((int) (x * 1000));
        buffer.writeInt((int) (y * 1000));
        buffer.writeInt((int) (z * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, content);
    }

    public static FabPacket gotoRelativePosition(float x, float y, float z, float f) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x03);
        buffer.writeInt((int) (x * 1000));
        buffer.writeInt((int) (y * 1000));
        buffer.writeInt((int) (z * 1000));
        // FeedRate use mm per second by hsl
        buffer.writeInt((int) (f / 60 * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, content);
    }

    /**
     * Movement: Request Extrusion (0x0b 0x04)
     */
    public static FabPacket requestExtrusion(int type, float lengthIn, float speedIn, float lengthOut, float speedOut) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x04);
        buffer.writeByte((byte) type);
        buffer.writeInt((int) (lengthIn * 1000));
        buffer.writeInt((int) (speedIn * 1000));
        buffer.writeInt((int) (lengthOut * 1000));
        buffer.writeInt((int) (speedOut * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setCrossLineLaserIndicator(boolean active) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x2d);
        buffer.writeByte(active ? 0x01 : 0x00);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getCrossLineLaserIndicator() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x2e);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setFireSensorSensitivity(int sensitivity) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x2f);
        buffer.writeShort(sensitivity);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getFireSensorSensitivity() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x30);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setCrossLineIndicatorOffset(float xOffset, float yOffset) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x32);
        buffer.writeInt((int) (xOffset * 1000));
        buffer.writeInt((int) (yOffset * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getCrossLineIndicatorOffset() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x33);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setPrintOffsetWithCrossLine(boolean enabled) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x34);
        buffer.writeByte(enabled ? 0x00 : 0x01);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getLaserIndicatorPower() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x35);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setLaserIndicatorPower(float power) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x36);
        buffer.writeInt((int) (power * 1000));
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    /**
     * Laser Camera operation: Set Camera Wi-Fi (0x0d 0x01)
     *
     * @param ssid     SSID of Wi-Fi to connect
     * @param password Password of SSID
     */
    public static FabPacket setupLaserNetwork(String ssid, String password) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x01);
        if (ssid != null) {
            buffer.write(ssid.getBytes());
        }
        buffer.writeByte(0x00);
        if (password != null) {
            buffer.write(password.getBytes());
        }
        buffer.writeByte(0x00);

        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.LASER_CAMERA_OPERATION_REQUEST_EVENT_ID, content);
    }

    /**
     * Laser Camera operation: Get Laser Status (0x0d 0x02)
     */
    public static FabPacket getLaserStatus() {
        return buildPacket(FabPacket.LASER_CAMERA_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x02});
    }


    /**
     * Laser Camera Operation: Get laser bluetooth status and mac address
     */
    public static FabPacket getLaserBtStatus() {
        return buildPacket(FabPacket.LASER_CAMERA_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x07});
    }

    /**
     * Add-on Operation: Get Enclosure status
     */
    public static FabPacket getEnclosureStatus() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x01});
    }

    /**
     * Add-on Operation: Set enclosure led value
     */
    public static FabPacket setEnclosureLed(int value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x02);
        buffer.writeByte((byte) value);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, content);
    }

    /**
     * Add-on Operation: Set enclosure fan value
     */
    public static FabPacket setEnclosureFan(int value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x03);
        buffer.writeByte((byte) value);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, content);
    }

    /**
     * Add-on Operation: Set enclosure door detection
     */
    public static FabPacket setEnclosureDoorDetection(boolean enabled) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x04);
        buffer.writeByte(enabled ? 0x01 : 0x00);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, content);
    }

    public static FabPacket requestRotaryModuleStatus() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x08});
    }

    public static FabPacket requestEmergencyStopStatus() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x07});
    }

    public static FabPacket requestAirPurifierAddOnStatus() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x09});
    }

    public static FabPacket requestAirPurifierFanStatus() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x0A});
    }

    public static FabPacket setAirPurifierFanEnabled(boolean enabled) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0B);
        buffer.writeByte(enabled ? 0x01 : 0x00);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setAirPurifierFanSpeed(int level) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0C);
        buffer.writeByte((byte) level);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, content);
    }

    public static FabPacket requestHeaderSecurityStatus() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x11});
    }

    public static FabPacket requestAirPurifierFilterLifeTime() {
        return buildPacket(FabPacket.ADD_ON_OPERATION_REQUEST_EVENT_ID, new byte[]{(byte) 0x0D});
    }

    /**
     * Update (0xa9 0x03)
     * <p>
     * TODO: re-consider the protocol
     */

    public static FabPacket startUpdate() {
        return buildPacket(FabPacket.UPDATE_REQUEST_EVENT_ID, new byte[]{(byte) 0x00});
    }

    public static FabPacket requestUpdatePackage() {
        return buildPacket(FabPacket.UPDATE_REQUEST_EVENT_ID, new byte[]{(byte) 0x01});
    }

    public static FabPacket sendUpdatePackage(byte opCode, short index, byte[] updatePackage) {
        Buffer buffer = new Buffer();
        buffer.writeByte(opCode);
        if (opCode == 0x01) {
            buffer.writeShort((int) index);
            buffer.write(updatePackage);
        }

        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.UPDATE_REQUEST_EVENT_ID, content);
    }

    public static FabPacket checkControllerVersion() {
        return buildPacket(FabPacket.UPDATE_REQUEST_EVENT_ID, new byte[]{(byte) 0x03});
    }

    public static FabPacket requestModuleVersion() {
        return buildPacket(FabPacket.UPDATE_REQUEST_EVENT_ID, new byte[]{(byte) 0x07});
    }

    /**
     * request header online sync id (0x09 0x12)
     */
    public static FabPacket requestHeaderOnlineSyncId() {
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, new byte[]{(byte) 0x12});
    }

    public static FabPacket setHeaderOnlineSyncId(int headerOnlineSyncId) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x13);
        buffer.writeInt(headerOnlineSyncId);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket setAbnormalTemperatureRange(int protectTemperature, int recoveryTemperature) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x16);
        buffer.writeByte(protectTemperature);
        buffer.writeByte(recoveryTemperature);
        byte[] content = buffer.readByteArray();
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, content);
    }

    public static FabPacket getExtruderModel() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x13});
    }

    public static FabPacket getExtrudersHaveFilament() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x14});
    }

    public static FabPacket getExtruderTemperatures() {
        return buildPacket(FabPacket.STATUS_SYNC_REQUEST_EVENT_ID, new byte[]{(byte) 0x15});
    }

    public static FabPacket setWorkSpeed(int which, float value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0f);
        buffer.writeByte(which == 0 ? 0 : 6);
        buffer.writeInt((int) (value * 1000));
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket setExtruderTemperature(int which, float value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0f);
        buffer.writeByte(which == 0 ? 1 : 7);
        buffer.writeInt((int) (value * 1000));
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket setLiveZOffset(int which, float value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0f);
        buffer.writeByte(which == 0 ? 4 : 8);
        buffer.writeInt((int) (value * 1000));
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket setFeedRate(int which, float value) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x0f);
        buffer.writeByte(which == 0 ? 9 : 10);
        buffer.writeInt((int) (value * 1000));
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket getWorkSpeed(int which) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x10);
        buffer.writeByte(which == 0 ? 0 : 6);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket getLiveZOffset(int which) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x10);
        buffer.writeByte(which == 0 ? 4 : 8);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket switchToExtruder(int which) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x18);
        buffer.writeByte(which);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket getExtruderOffset() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x19);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket confirmExtruderPosition(int which) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1a);
        buffer.writeByte(which);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket setExtruderOffset(int direction, float offset) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1b);
        buffer.writeByte(direction);
        buffer.writeInt((int) (offset * 1000));
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket setToolheadFanSpeed(int which, int speed) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1c);
        buffer.writeByte(which);
        buffer.writeByte(speed);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket calibrateSensorTouchBed(int which, boolean isAuto) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1d);
        buffer.writeByte(which == 0 ? (isAuto ? 0 : 3) : 1);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket calibrateSensorManualConfirm(int which) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1d);
        buffer.writeByte(which == 0 ? 4 : 2);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket calibrationSensorRequestAbort() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1d);
        buffer.writeByte(5);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket probeBedPosition(int which, boolean isAuto) {
        Buffer buffer = new Buffer();
        buffer.writeByte(isAuto ? 0x1e : 0x1f);
        buffer.writeByte(which);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket saveBedPosition() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x1f);
        buffer.writeByte(2);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket startDualExtruderAutoLeveling(int grid) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x20);
        buffer.writeByte(grid);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket dualExtruderAutoLevelPoint(int point) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x21);
        buffer.writeByte(point);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket stopDualExtruderAutoLeveling() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x22);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket startDualExtruderManualLeveling(int grid) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x23);
        buffer.writeByte(grid);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket dualExtruderManualLevelPoint(int point) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x24);
        buffer.writeByte(point);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket dualExtruderStopManualLeveling() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x25);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket getActivatedExtruder() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x26);
        return buildPacket(FabPacket.SETTINGS_REQUEST_EVENT_ID, buffer.readByteArray());
    }


    public static FabPacket extrudeInfinitely(int direction, int speed) {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x05);
        buffer.writeByte(direction);
        buffer.writeInt(speed * 1000);
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, buffer.readByteArray());
    }

    public static FabPacket stopMovement() {
        Buffer buffer = new Buffer();
        buffer.writeByte(0x06);
        return buildPacket(FabPacket.MOVEMENT_REQUEST_EVENT_ID, buffer.readByteArray());
    }
}

