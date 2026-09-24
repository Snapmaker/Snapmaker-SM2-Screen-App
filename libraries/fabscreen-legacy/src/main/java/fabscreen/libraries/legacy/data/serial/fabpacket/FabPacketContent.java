package fabscreen.libraries.legacy.data.serial.fabpacket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fabscreen.libraries.legacy.lib.LogHelper;
import okio.Buffer;
import okio.ByteString;

/**
 * FabPacket content.
 * <p>
 * A collection of classes that represent different structures of request / response.
 * Classes that provide getters for attributes for ease of use.
 */
public class FabPacketContent {
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * G-code response.
     */
    public static class GcodeResponse {
        private int lineNo;
        private String content = "";

        public static GcodeResponse parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            GcodeResponse r = new GcodeResponse();
            try {
                r.lineNo = buffer.readInt();
                r.content = buffer.readString(UTF_8);
                return r;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
        }

        public boolean isOK() {
            return content.equals("ok");
        }

        public int getLineNo() {
            return lineNo;
        }

        public void mergeContent(String newContent) {
            content += newContent;
        }

        public void setLineNo(int no) {
            lineNo = no;
        }

        @NonNull
        public String getContent() {
            return content;
        }

        public static GcodeResponse EMPTY_GCODE_RESPONSE = new GcodeResponse();
    }

    public static class BatchGcodeResponse {
        private int lineNo;

        public static BatchGcodeResponse parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);
            BatchGcodeResponse r = new BatchGcodeResponse();
            try {
                r.lineNo = buffer.readInt();
                return r;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
        }

        public int getLineNo() {
            return lineNo;
        }
    }

    public static class FileOperationResponse {
        private byte operationId;
        private boolean ok;
        private int offset;
        private int count;
        private List<Integer> types;
        private List<String> filenames;
        private String filePath;

        public static FileOperationResponse parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            FileOperationResponse response = new FileOperationResponse();
            try {
                response.operationId = buffer.readByte();
                response.ok = buffer.readByte() == 0;

                switch (response.operationId) {
                    case 0x00:
                    case 0x02:
                    case 0x06:
                        break;

                    case 0x01:
                        response.filePath = buffer.readString(UTF_8);
                        break;

                    case 0x04:
                        response.offset = buffer.readShort();
                        response.count = buffer.readByte() & 0xFF;
                        response.types = new ArrayList<>();
                        response.filenames = new ArrayList<>();

                        int offset = 5;
                        int length = content.length;

                        while (offset < length) {
                            int type = content[offset++];

                            int end = offset;
                            while (end < length && content[end] != 0x00) end++;

                            byte[] bytes = new byte[end - offset];
                            System.arraycopy(content, offset, bytes, 0, end - offset);

                            response.types.add(type);
                            response.filenames.add(new String(bytes, UTF_8));
                            if (response.filenames.size() == response.count) {
                                break;
                            }

                            offset = end + 1;
                        }
                        break;
                }
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }

            return response;
        }

        public byte getOperationId() {
            return operationId;
        }

        public boolean isOk() {
            return ok;
        }

        public String getFilePath() {
            return filePath;
        }

        public int getOffset() {
            return offset;
        }

        public int getCount() {
            return count;
        }

        public List<Integer> getTypes() {
            return types;
        }

        public List<String> getFilenames() {
            return filenames;
        }
    }

    public static class LaserWifiStatus {
        private final static byte STATUS_CONNECTED = 0;
        private final static byte STATUS_NOT_CONNECTED = 1;
        private final static byte STATUS_NOT_UNAVAILABLE = 2;
        public byte networkStatus; // Wi-Fi status
        private String SSID;
        private String password;
        public String address;

        @Nullable
        public static LaserWifiStatus parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            LaserWifiStatus status = new LaserWifiStatus();

            try {
                buffer.readByte();
                status.networkStatus = buffer.readByte();
                // use end + offset to get String

                final int length = content.length;
                int offset = 2;
                ArrayList<String> args = new ArrayList<>();

                while (offset < length) {
                    int end = offset;
                    while (end < length && content[end] != 0x00) end++;

                    byte[] bytes = new byte[end - offset];
                    System.arraycopy(content, offset, bytes, 0, end - offset);

                    args.add(new String(bytes, UTF_8));
                    offset = end + 1;
                }

                // illegal args
                if (args.size() < 3) {
                    return null;
                }

                status.SSID = args.get(0);
                status.password = args.get(1);
                status.address = args.get(2);
            } catch (EOFException e) {
                LogHelper.log(e);
                return null;
            }

            return status;
        }

        public int getConnectStatus() {
            return networkStatus;
        }

        public String getSSID() {
            return SSID;
        }

        public String getPassword() {
            return password;
        }

        public String getAddress() {
            return address;
        }

        public boolean isConnected() {
            return networkStatus == STATUS_CONNECTED;
        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "%s networkStatus: %d SSID: %s password: %s address: %s",
                    this.getClass().getSimpleName(), networkStatus, SSID, password, address);
        }
    }

    public static class LaserBtStatus {
        public byte status;
        public String macAddress;

        public static LaserBtStatus parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            LaserBtStatus btStatus = new LaserBtStatus();

            try {
                // operation id
                buffer.readByte();

                // format mac address, eg. 02:00:00:00:00:FE
                btStatus.status = buffer.readByte();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    if (i != 0) {
                        builder.append(":");
                    }
                    builder.append(ByteString.of(buffer.readByte()).hex().toUpperCase());
                }

                btStatus.macAddress = builder.toString();
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
            return btStatus;
        }

        public boolean isReady() {
            return status == 0;
        }

        public byte getStatus() {
            return status;
        }

        public String getMacAddress() {
            return macAddress;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                    "status: %d MacAddress: %s",
                    status, macAddress);
        }
    }

    public static class ModuleVersion {
        final public static int TYPE_MODULE_TOOL_HEAD_3DP = 0x00;
        final public static int TYPE_MODULE_TOOL_HEAD_CNC = 0x01;
        final public static int TYPE_MODULE_TOOL_HEAD_LASER_1600 = 0x02;
        final public static int TYPE_MODULE_LINEAR_MODULE_2_0 = 0x03;
        final public static int TYPE_MODULE_ADD_ON_LED_STRIP = 0x04;
        final public static int TYPE_MODULE_ADD_ON_ENCLOSURE = 0x05;
        final public static int TYPE_MODULE_ADD_ON_ROTARY_MODULE = 0x06;
        final public static int TYPE_MODULE_ADD_ON_AIR_PURIFIER = 0x07;
        final public static int TYPE_MODULE_ADD_ON_EMERGENCY_STOP_BUTTON = 0x08;
        final public static int TYPE_MODULE_ADD_ON_CNC_TOOL_SETTER = 0x09;
        final public static int TYPE_MODULE_CHANGEOVER_TOOL_HEAD_ORIGINAL = 0x0A;
        final public static int TYPE_MODULE_ADD_ON_FAN = 0x0B;
        final public static int TYPE_MODULE_LINEAR_MODULE_2_5 = 0x0C;
        final public static int TYPE_MODULE_TOOL_HEAD_3DP_DUAL_NOZZLE = 0x0D;
        public int moduleID;
        public int moduleType;
        public String version;

        public static ModuleVersion parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            ModuleVersion moduleVersion = new ModuleVersion();

            try {
                buffer.readByte();
                moduleVersion.moduleID = buffer.readInt();
                moduleVersion.version = buffer.readString(FabPacketContent.UTF_8).trim();

                moduleVersion.moduleType = (moduleVersion.moduleID & 0x1FF00000) >> 20;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }

            return moduleVersion;
        }

        public boolean equalType(int type) {
            return (moduleType & type) > 0;
        }
    }

    public static class MachineStatus {
        public boolean isDefault = true;
        public double x;
        public double y;
        public double z;
        public double e;
        public int bedTemperature;
        public int bedTargetTemperature;
        public int headTemperature;
        public int headTargetTemperature;
        public short feedRate;
        public double laserPower;
        public int spindleSpeed;
        public double b;
        public int printerStatus;
        public int peripheralStatus;
        public int headStatus;
        public int printingLineNo;
        public int extruder1Temperature;
        public int extruder1TargetTemperature;
//        public int extruder1FeedRate;

        public static MachineStatus sDefaultInstance = new MachineStatus();

        public static MachineStatus getDefaultInstance() {
            return sDefaultInstance;
        }

        @Nullable
        public static MachineStatus parse(byte[] content) {
//            Logger.d("MachineStatus content bytes: %s", ByteString.of(content).hex());
            Buffer buffer = new Buffer();
            buffer.write(content);

            MachineStatus machineStatus = new MachineStatus();
            machineStatus.isDefault = false;

            try {
                buffer.readByte();
                machineStatus.x = buffer.readInt() / 1000.0;
                machineStatus.y = buffer.readInt() / 1000.0;
                machineStatus.z = buffer.readInt() / 1000.0;
                machineStatus.e = buffer.readInt() / 1000.0;

                machineStatus.bedTemperature = buffer.readShort();
                machineStatus.bedTargetTemperature = buffer.readShort();
                machineStatus.headTemperature = buffer.readShort();
                machineStatus.headTargetTemperature = buffer.readShort();

                machineStatus.feedRate = buffer.readShort();

                // Due to fact that tool head can't be applied in two different function,
                // LaserPower and SpindleSpeed both share the same protocol position.
                final int power = buffer.readInt();
                machineStatus.laserPower = power / 1000.0;
                machineStatus.spindleSpeed = power;

                // If rotary module is plugged, then pretend the extend position to be B axis data.
                machineStatus.b = buffer.readInt() / 1000.0;

                machineStatus.printerStatus = buffer.readByte() & 0xFF;
                machineStatus.peripheralStatus = buffer.readByte() & 0xFF;
                machineStatus.headStatus = buffer.readByte() & 0xFF;
                if (!buffer.exhausted()) {
                    machineStatus.printingLineNo = buffer.readInt();
                }
                // FIXME: Workaround here.
                if (!buffer.exhausted()) {
                    machineStatus.extruder1Temperature = buffer.readShort();
                }

                if (!buffer.exhausted()) {
                    machineStatus.extruder1TargetTemperature = buffer.readShort();
                }
//                machineStatus.extruder1FeedRate = buffer.readShort();
            } catch (EOFException e) {
                LogHelper.log(e);
                return null;
            }

            return machineStatus;
        }

        public boolean isDefault() {
            return isDefault;
        }


        @Override
        public String toString() {
            return String.format(Locale.US, "%s (%.2f, %.2f, %.2f) %.2f Temp %d/%d %d/%d headInfo %d %.1f %d (extend b-axis %.2f) printer %d %d %d",
                    this.getClass().getSimpleName(), x, y, z, e,
                    bedTemperature, bedTargetTemperature, headTemperature, headTargetTemperature,
                    feedRate, laserPower, spindleSpeed, b,
                    printerStatus, peripheralStatus, headStatus);
        }
    }

    public static class MachineErrors {
        public static final int PRINT_FILAMENT_ERROR = 1 << 3;
        public static final int PRINT_POWER_OFF = 1 << 6;
        public static final int NOZZLE_TEMPERATURE_MAX_EXCEED = 1 << 14;
        public static final int ENCLOSURE_DOOR_OPEN = 1 << 21;

        public int bits;

        @Nullable
        public static MachineErrors parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            MachineErrors machineErrors = new MachineErrors();

            try {
                buffer.readByte();
                machineErrors.bits = buffer.readInt();
            } catch (EOFException e) {
                LogHelper.log(e);
                return null;
            }

            return machineErrors;
        }

        @Override
        public String toString() {
            int bit = 0;
            final StringBuilder stringBuilder = new StringBuilder("Machine Error bit:");
            while (bit < 32) {
                if (bit % 8 == 0) {
                    stringBuilder.append("\n");
                }
                if ((bits & (1 << bit)) != 0) {
                    stringBuilder.append(1);
                } else {
                    stringBuilder.append(0);
                }
                stringBuilder.append(" ");
                bit++;
            }
            return stringBuilder.toString();
        }
    }

    public static class MachineSize {
        private byte ok;
        public byte machineModel;
        public int xSize;
        public int ySize;
        public int zSize;
        private int xMaxDir;
        private int yMaxDir;
        private int zMaxDir;
        private int xStepperDir;
        private int yStepperDir;
        private int zStepperDir;
        private int xHomeOffset;
        private int yHomeOffset;
        private int zHomeOffset;

        @Nullable
        public static MachineSize parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            MachineSize machineSize = new MachineSize();

            try {
                // operation ID
                buffer.readByte();

                machineSize.ok = buffer.readByte();
                machineSize.machineModel = buffer.readByte();
                // size
                machineSize.xSize = buffer.readInt() / 1000;
                machineSize.ySize = buffer.readInt() / 1000;
                machineSize.zSize = buffer.readInt() / 1000;
                // homeDir
                machineSize.xMaxDir = buffer.readInt();
                machineSize.yMaxDir = buffer.readInt();
                machineSize.zMaxDir = buffer.readInt();
                // stepperDir
                machineSize.xStepperDir = buffer.readInt();
                machineSize.yStepperDir = buffer.readInt();
                machineSize.zStepperDir = buffer.readInt();
                // homeOffset
                machineSize.xHomeOffset = buffer.readInt() / 1000;
                machineSize.yHomeOffset = buffer.readInt() / 1000;
                machineSize.zHomeOffset = buffer.readInt() / 1000;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }

            return machineSize;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.US, "%s machineModel %d size (%d %d %d) homeDir (%d %d %d) StepperDir (%d %d %d) HomeOffset (%d %d %d)",
                    this.getClass().getSimpleName(), machineModel,
                    xSize, ySize, zSize,
                    xMaxDir, yMaxDir, zMaxDir,
                    xStepperDir, yStepperDir, zStepperDir,
                    xHomeOffset, yHomeOffset, zHomeOffset);
        }
    }

    public static class CoordinateSystem {
        public boolean homed;
        public int coordinateID;
        public boolean coordinateAligned;
        public float coordinateX;
        public float coordinateY;
        public float coordinateZ;

        @Nullable
        public static CoordinateSystem parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            CoordinateSystem coordinateSystem = new CoordinateSystem();

            try {
                buffer.readByte();

                coordinateSystem.homed = buffer.readByte() == 0;
                coordinateSystem.coordinateID = (int) buffer.readByte();
                coordinateSystem.coordinateAligned = buffer.readByte() == 0;
                coordinateSystem.coordinateX = buffer.readInt() / 1000f;
                coordinateSystem.coordinateY = buffer.readInt() / 1000f;
                coordinateSystem.coordinateZ = buffer.readInt() / 1000f;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }

            return coordinateSystem;
        }
    }

    /**
     * Adjust Settings
     */
    public static class AdjustSettings {
        public byte retCode;
        public float value;

        @Nullable
        public static AdjustSettings parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);
            AdjustSettings adjustSettings = new AdjustSettings();
            try {
                buffer.readByte();

                adjustSettings.retCode = buffer.readByte();
                adjustSettings.value = buffer.readInt() / 1000.0f;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
            return adjustSettings;
        }
    }

    /**
     * Add-on: Enclosure status
     */
    public static class EnclosureStatus {
        public byte enclosureStatus;
        public byte ledLevel;
        public byte fanLevel;
        public boolean enclosureEnabled;

        @Nullable
        public static EnclosureStatus parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            EnclosureStatus enclosureStatus = new EnclosureStatus();

            try {
                buffer.readByte();

                enclosureStatus.enclosureStatus = buffer.readByte();

                // 0 led/fan is off,
                // 1-100 led/fan is on, max value up to 100
                enclosureStatus.ledLevel = buffer.readByte();
                enclosureStatus.fanLevel = buffer.readByte();

                // 1 is enabled, 0 is disabled
                enclosureStatus.enclosureEnabled = buffer.readByte() != 0;
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }

            return enclosureStatus;
        }

        public boolean isReady() {
            return enclosureStatus == 0;
        }

        public boolean isEnclosureEnabled() {
            return enclosureEnabled;
        }

        public boolean isLedOn() {
            return ledLevel != 0;
        }

        public boolean isFanOn() {
            return fanLevel != 0;
        }
    }

    public static class AirPurifierStatus {
        public static final byte AIR_PURIFIER_STATUS_OK = 0x00;
        public static final byte AIR_PURIFIER_STATUS_NOT_PLUGGED = 0x01;
        public static final byte AIR_PURIFIER_STATUS_POWER_OFF = 0x02;
        public static final byte AIR_PURIFIER_STATUS_ERROR = 0x03;
        public static final int BIT_CONTROLLER_WRONG_VOLTAGE = 1;
        public static final int BIT_POWER_SUPPLIES_WRONG_VOLTAGE = 1 << 1;
        public static final int BIT_FAN_SPEED_TOO_LOW = 1 << 2;
        public static final int BIT_FILTER_DRAW_OUT = 1 << 3;
        public static final int BIT_HATCH_OPENED_WHEN_WORKING = 1 << 4;
        public static final int BIT_EMERGENCY_STOPPED = 1 << 5;
        public byte status;
        public byte errorBit;

        public static AirPurifierStatus parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            AirPurifierStatus airPurifierStatus = new AirPurifierStatus();

            try {
                // skip operation id
                buffer.readByte();

                airPurifierStatus.status = buffer.readByte();
                airPurifierStatus.errorBit = buffer.readByte();

            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
            return airPurifierStatus;
        }

        public boolean isReady() {
            return status == 0;
        }

        private static AirPurifierStatus mockAirPurifierNotPluggedStatus() {
            AirPurifierStatus status = new AirPurifierStatus();
            status.status = AIR_PURIFIER_STATUS_NOT_PLUGGED;
            return status;
        }

        public static AirPurifierStatus MOCK_AIR_PURIFIER_STATUS_NOT_PLUGGED = mockAirPurifierNotPluggedStatus();


        @NotNull
        @Override
        public String toString() {
            int bit = 0;
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Air Purifier status ").append(status).append(", error bit ");
            while (bit < 8) {
                if (bit % 8 == 0) {
                    stringBuilder.append("\n");
                }
                if ((errorBit & (1 << bit)) != 0) {
                    stringBuilder.append(1);
                } else {
                    stringBuilder.append(0);
                }
                stringBuilder.append(" ");
                bit++;
            }
            return stringBuilder.toString();
        }
    }

    public static class AirPurifierFan {
        public boolean isOn;
        public int level;

        public static AirPurifierFan parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            AirPurifierFan airPurifierFan = new AirPurifierFan();

            try {
                // skip operation id;
                buffer.readByte();

                airPurifierFan.isOn = (buffer.readByte() == (byte) 0x01);
                airPurifierFan.level = buffer.readByte();
            } catch (IOException e) {
                LogHelper.log(e);
                return null;
            }
            return airPurifierFan;
        }
    }

    public static class MasterState {
        public byte mOperationId;
        public int states;

        public static MasterState parse(byte[] content) {
            MasterState masterState = new MasterState();
            masterState.states = content[1] & 0xff;
            masterState.mOperationId = content[0];
            return masterState;
        }
    }

    public static class HeaderSecurity {
        public byte status = -1;
        public byte temperature;
        public byte rollHigh;
        public byte rollLess;
        public byte pitchHigh;
        public byte pitchLess;
        public static final int HEADER_SENSOR_STATUS = 1;
        public static final int HEADER_TEMPERATURE_ANOMALY = 1 << 1;
        public static final int HEADER_ROLL_ABNORMAL_ANGLE = 1 << 2;
        public static final int HEADER_FIRE_DETECTION = 1 << 5;

        public HeaderSecurity(byte status) {
            this.status = status;
        }

        public static HeaderSecurity parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            HeaderSecurity headerSecurity = new HeaderSecurity((byte) -1);
            try {
                // skip operation id
                buffer.readByte();
                headerSecurity.status = buffer.readByte();
                headerSecurity.temperature = buffer.readByte();
                headerSecurity.rollHigh = buffer.readByte();
                headerSecurity.rollLess = buffer.readByte();
                headerSecurity.pitchHigh = buffer.readByte();
                headerSecurity.pitchLess = buffer.readByte();
            } catch (IOException e) {
                LogHelper.log(e);
            }
            return headerSecurity;
        }

        @Override
        public String toString() {
            return "HeaderSecurity{" +
                    "status=" + status +
                    ", temperature=" + temperature +
                    ", rollHigh=" + rollHigh +
                    ", rollLess=" + rollLess +
                    ", pitchHigh=" + pitchHigh +
                    ", pitchLess=" + pitchLess +
                    '}';
        }
    }


    public static class ExtendKitInfo {
        private byte quickSwapState;
        private byte extendKitConf;
        public static final int BIT_EXTEND_KIT_QUICK_SWAP = 1;
        public static final int BIT_EXTEND_KIT_BRACING_KIT = 1 << 1;

        public ExtendKitInfo() { }

        public ExtendKitInfo(byte state, byte conf) {
            quickSwapState = state;
            extendKitConf = conf;
        }

        public static ExtendKitInfo parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            ExtendKitInfo info = new ExtendKitInfo();

            try {
                buffer.skip(2);
                // legacy byte
                info.setQuickSwapState(buffer.readByte());
                info.setExtendKitConf(buffer.readByte());
            } catch (EOFException e) {
                LogHelper.log(e);
                return null;
            }
            return info;
        }

        public byte getQuickSwapState() {
            return quickSwapState;
        }

        public void setQuickSwapState(byte state) {
            quickSwapState = state;
        }

        public byte getExtendKitConf() {
            return extendKitConf;
        }

        public void setExtendKitConf(byte conf) {
            extendKitConf = conf;
        }

        public boolean isQuickSwapInstalled() {
            return quickSwapState != 0 || ((extendKitConf & BIT_EXTEND_KIT_QUICK_SWAP) != 0);
        }

        public boolean isBracingKitInstalled() {
            return (extendKitConf & BIT_EXTEND_KIT_BRACING_KIT) != 0;
        }


        @Override
        public String toString() {
            return "QuickSwapKitInfo{" +
                    "quickSwapState=" + quickSwapState +
                    ", extendKitConf=" + extendKitConf +
                    '}';
        }
    }

    public static class CrossLineIndicatorOffset {
        private byte status;
        private float xOffset;
        private float yOffset;

        public CrossLineIndicatorOffset() { }

        public CrossLineIndicatorOffset(float xOffset, float yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public static CrossLineIndicatorOffset parse(byte[] content) {
            Buffer buffer = new Buffer();
            buffer.write(content);

            CrossLineIndicatorOffset crossLineIndicatorOffset = new CrossLineIndicatorOffset();
            try {
                // sub-eventId
                buffer.readByte();
                // status
                crossLineIndicatorOffset.status = buffer.readByte();
                crossLineIndicatorOffset.xOffset = buffer.readInt() / 1000.0f;
                crossLineIndicatorOffset.yOffset = buffer.readInt() / 1000.0f;

            } catch (IOException e) {
                LogHelper.log(e);
            }

            return crossLineIndicatorOffset;
        }

        public float getXOffset() {
            return xOffset;
        }

        public float getYOffset() {
            return yOffset;
        }

        @Override
        public String toString() {
            return "CrossLineIndicatorOffset{" +
                    "status=" + status +
                    ", xOffset=" + xOffset +
                    ", yOffset=" + yOffset +
                    '}';
        }
    }
}

