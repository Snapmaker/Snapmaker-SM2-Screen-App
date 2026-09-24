package fabscreen.libraries.legacy.server.http.handlers;

import android.graphics.Bitmap;
import android.os.SystemClock;

import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.FileBody;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.util.StatusCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.data.imgprocess.LaserDistanceMeasureProcess;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFile;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.parser.GcodeParser;
import fabscreen.libraries.legacy.lib.parser.IGcodeParser;
import fabscreen.libraries.legacy.server.http.HTTPEventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static fabscreen.libraries.legacy.data.MockConst.CAMERA_HEIGHT_OFFSET;
import static fabscreen.libraries.legacy.data.MockConst.H1_Z_POSITION;
import static fabscreen.libraries.legacy.data.MockConst.H2_Z_POSITION;

@RestController
class MainRequestHandler extends BaseRequestHandler {
    private static final String URI_HEARTBEAT_PING = "/api/v1/ping";

    private static final String URI_STATUS = "/api/v1/status";

    private static final String URI_EXECUTE_CODE = "/api/v1/execute_code";

    // file
    private static final String URI_UPLOAD_FILE = "/api/v1/upload";
    private static final String URI_PRINT_FILE = "/api/v1/print_file";

    // print
    private static final String URI_PREPARE_PRINT = "/api/v1/prepare_print";
    private static final String URI_START_PRINT = "/api/v1/start_print";
    private static final String URI_PAUSE_PRINT = "/api/v1/pause_print";
    private static final String URI_RESUME_PRINT = "/api/v1/resume_print";
    private static final String URI_STOP_PRINT = "/api/v1/stop_print";

    // filament load/unload
    private static final String URI_FILAMENT_UNLOAD = "/api/v1/filament_unload";
    private static final String URI_FILAMENT_LOAD = "/api/v1/filament_load";

    private static final String URI_SWITCH_EXTRUDER = "/api/v1/switch_extruder";
    private static final String URI_ACTIVE_EXTRUDER = "/api/v1/active_extruder";

    // override parameters
    private static final String URI_OVERRIDE_NOZZLE = "/api/v1/override_nozzle_temperature";
    private static final String URI_OVERRIDE_NOZZLE_BY_INDEX = "/api/v1/override_nozzle_temperature_by_index";
    private static final String URI_OVERRIDE_HEATED_BED = "/api/v1/override_bed_temperature";
    private static final String URI_OVERRIDE_Z_OFFSET = "/api/v1/override_z_offset";
    private static final String URI_OVERRIDE_WORK_SPEED = "/api/v1/override_work_speed";
    private static final String URI_OVERRIDE_LASER_POWER = "/api/v1/override_laser_power";

    // enclosure
    private static final String URI_ENCLOSURE = "/api/v1/enclosure";

    // air purifier
    private static final String URI_AIR_PURIFIER_SWITCH = "/api/v1/air_purifier_switch";
    private static final String URI_AIR_PURIFIER_FAN_SPEED = "/api/v1/air_purifier_fan_speed";

    // New module api for SM2
    private static final String URI_MODULE_LIST = "/api/v1/module_list";
    private static final String URI_MODULE_INFO = "/api/v1/module_info";

    // IQC test api
    private static final String URI_IQC_CHECK_ALIVE = "/api/v1/iqc_check_alive";
    private static final String URI_IQC_UPLOAD_TEST = "/api/v1/iqc_upload_test";

    private static final String URI_LASER_MATERIAL_THICKNESS = "/api/request_Laser_Material_Thickness";

    private static final String URI_GET_LASER_CROSS_LINE_INDICATOR_STATUS = "/api/v1/get_laser_crosshair_indicator_status";
    private static final String URI_SET_LASER_CROSS_LINE_INDICATOR_STATUS = "/api/v1/set_laser_crosshair_indicator_status";

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @GetMapping(path = URI_HEARTBEAT_PING)
    void getPing(HttpRequest request, HttpResponse response) {
        long startTime = SystemClock.elapsedRealtime();
        if (!ensureConnection(request, response)) {
            logResponseCostTime(request, startTime);
            return;
        }

        response.setStatus(StatusCode.SC_OK);
        logResponseCostTime(request, startTime);
    }

    /**
     * API for getting status of the machine. This API is designed to be called every second.
     */
    // This API was about to deprecated or abandoned
    @GetMapping(path = URI_STATUS)
    void getStatus(HttpRequest request, HttpResponse response) {
        long startTime = SystemClock.elapsedRealtime();
        if (!ensureConnection(request, response)) {
            logResponseCostTime(request, startTime);
            return;
        }

        final FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();

        JSONObject data = new JSONObject();
        try {
            if (status.printerStatus == 0) {
                data.put("status", "IDLE");
            } else if (status.printerStatus == 1 || status.printerStatus == 3) {
                data.put("status", "RUNNING");
            } else {
                data.put("status", "PAUSED");
            }

            data.put("x", status.x);
            data.put("y", status.y);
            data.put("z", status.z);

            if (getModel().getMachineController().isRotaryModuleAvailable()) {
                data.put("b", status.b);
            }

            data.put("homed", getModel().getMachineController().isHomed());
            data.put("offsetX", getModel().getMachineController().getCoordinateOffsetX());
            data.put("offsetY", getModel().getMachineController().getCoordinateOffsetY());
            data.put("offsetZ", getModel().getMachineController().getCoordinateOffsetZ());

            // toolHead
            int headType = getModel().getMachineController().getHeadType();
            String toolHeadKey;
            switch (headType) {
                case Constants.HEAD_3DP:
                    toolHeadKey = getModel().getRemoteController().KEY_TOOL_HEAD_3DP_1;
                    break;
                case Constants.HEAD_3DP_DUAL_EXTRUDER:
                    toolHeadKey = getModel().getRemoteController().KEY_TOOL_HEAD_3DP_1;
                    break;
                case Constants.HEAD_CNC:
                case Constants.HEAD_CNC_200W:
                    toolHeadKey = getModel().getRemoteController().KEY_TOOL_HEAD_CNC_1;
                    break;
                case Constants.HEAD_LASER:
                    toolHeadKey = getModel().getRemoteController().KEY_TOOL_HEAD_LASER_1;
                    break;
                case Constants.HEAD_LASER_10W:
                    toolHeadKey = getModel().getRemoteController().KEY_TOOL_HEAD_LASER_2;
                    break;
                case Constants.HEAD_UNPLUGGED:
                default:
                    toolHeadKey = "unplugged";
                    break;
            }
            // This key had been deprecated. DO NOT update this key.
            data.put("toolHead", toolHeadKey);

            switch (headType) {
                case Constants.HEAD_3DP:
                    data.put("nozzleTemperature", status.headTemperature);
                    data.put("nozzleTargetTemperature", status.headTargetTemperature);
                    data.put("heatedBedTemperature", status.bedTemperature);
                    data.put("heatedBedTargetTemperature", status.bedTargetTemperature);

                    boolean isFilamentOut = getModel().getMachineController().isFilamentOut();
                    data.put("isFilamentOut", isFilamentOut);
                    break;
                case Constants.HEAD_3DP_DUAL_EXTRUDER:
                    data.put("nozzleTemperature1", status.headTemperature);
                    data.put("nozzleTargetTemperature1", status.headTargetTemperature);
                    data.put("nozzleTemperature2", status.extruder1Temperature);
                    data.put("nozzleTargetTemperature2", status.extruder1TargetTemperature);
                    data.put("heatedBedTemperature", status.bedTemperature);
                    data.put("heatedBedTargetTemperature", status.bedTargetTemperature);

                    isFilamentOut = getModel().getMachineController().isFilamentOut();
                    data.put("isFilamentOut", isFilamentOut);
                    break;
                case Constants.HEAD_LASER:
                case Constants.HEAD_LASER_10W:
                    // FIXME: 2021/9/8 Laser 10w may not match this!
                    float laserFocus = getModel().getMachineController().getLaserFocus();
                    data.put("laserFocalLength", laserFocus);
                    data.put("laserPower", status.laserPower);
                    data.put("laserCamera", getModel().getLaserCameraController().isConnected());
                    data.put("laser10WErrorState", getModel().getMachineController().getLaser10WErrorState());
                    break;
                case Constants.HEAD_CNC:
                case Constants.HEAD_CNC_200W:
                    data.put("spindleSpeed", status.spindleSpeed);
                    break;
            }

            data.put("workSpeed", status.feedRate);

            // print status
            MachinePrintJobState state = getModel().getPrintController().getPrintJobState();

            String printStatus = "Idle";
            switch (state) {
                case PRINT_JOB_STATE_IDLE:
                case PRINT_JOB_STATE_STOPPED:
                    printStatus = "Idle";
                    break;
                case PRINT_JOB_STATE_STARTING:
                case PRINT_JOB_STATE_PRINTING:
                case PRINT_JOB_STATE_PAUSING:
                case PRINT_JOB_STATE_STOPPING:
                case PRINT_JOB_STATE_FINISHING:
                case PRINT_JOB_STATE_RECOVERING:
                case PRINT_JOB_STATE_RESUMING:
                    printStatus = "Printing";
                    break;
                case PRINT_JOB_STATE_PAUSED:
                    printStatus = "Paused";
                    break;
                case PRINT_JOB_STATE_FINISHED:
                    printStatus = "Complete";
                    break;
                default:
                    printStatus = "Idle";
                    break;
            }
            data.put("printStatus", printStatus);

            if (MachinePrintJobState.isStatePrinting(state.getValue())) {
                boolean isPrintFromRemote = getModel().getWorkspace().getPrintSource() == Constants.PRINT_SOURCE_LUBAN;
                String printFileName = "";
                if (getModel().getWorkspace().getPrintFile() == null && getModel().getRemoteController().getFile() == null) {
                    printFileName = "";
                } else {
                    printFileName = getModel().getWorkspace().getFileName();
                }

                float estimatedTime = getModel().getWorkspace().getEstimatedTime();
                float p = getModel().getPrintController().getProgress();
                // Add valid value check for progress, value must be between 0 to 1
                p = Math.max(0f, Math.min(1.0f, p));
                // formula: remaining = (1 - p) * p * elapsed / p + (1 - p) * (1 - p) * ETA
                int elapsed = getModel().getPrintController().getTickCounter().getCount();
                int remaining = (int) ((1 - p) * elapsed + (1 - p) * (1 - p) * estimatedTime);

                data.put("fileName", printFileName);
                data.put("totalLines", getModel().getPrintController().getTotalLines());
                data.put("estimatedTime", estimatedTime);
                data.put("currentLine", getModel().getRemoteController().getProgressCount());
                data.put("progress", p);
                data.put("elapsedTime", elapsed);
                data.put("remainingTime", remaining);
            }

            // Warning: Add on status has deprecated, use module_info instead!
            // collect add-on module list
            JSONObject addOnModules = new JSONObject();
            addOnModules.put("enclosure", getModel().getMachineController().isEnclosureReady());
            addOnModules.put("rotaryModule", getModel().getMachineController().isRotaryModuleAvailable());
            addOnModules.put("emergencyStopButton", getModel().getMachineController().isEmergencyStopAvailable());
            addOnModules.put("airPurifier", getModel().getMachineController().isAirPurifierReady());

            data.put("moduleList", addOnModules);

            if (getModel().getMachineController().isEnclosureReady()) {
                data.put("isEnclosureDoorOpen", getModel().getMachineController().isEnclosureOpen());
                data.put("doorSwitchCount", getModel().getRemoteController().getEnclosureDoorCount());
            }

            if (getModel().getMachineController().isEmergencyStopTriggered()) {
                data.put("isEmergencyStopped", getModel().getMachineController().isEmergencyStopTriggered());
            }

            if (getModel().getMachineController().isAirPurifierPlugged()) {
                data.put("airPurifierSwitch", getModel().getMachineController().isAirPurifierFanOn());
                data.put("airPurifierFanSpeed", getModel().getMachineController().getAirPurifierFanSpeed());
                data.put("airPurifierFilterHealth", getModel().getMachineController().getAirPurifierFilterLifeTime());
            }

            response.setBody(new JsonBody(data));
        } catch (JSONException e) {
            LogHelper.log(e);
        }
        logResponseCostTime(request, startTime);
    }

    @GetMapping(path = URI_MODULE_LIST)
    void getModuleListFromSM2(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject data = new JSONObject();

        JSONArray moduleList = new JSONArray();

        try {
            // ToolHead
            JSONObject toolHeadModule = new JSONObject();
            int headType = getModel().getMachineController().getHeadType();
            int toolHeadID = -1;
            switch (headType) {
                case Constants.HEAD_3DP:
                    toolHeadID = 0;
                    break;
                case Constants.HEAD_3DP_DUAL_EXTRUDER:
                    toolHeadID = 18;
                    break;
                case Constants.HEAD_CNC:
                    toolHeadID = 1;
                    break;
                case Constants.HEAD_CNC_200W:
                    toolHeadID = 15;
                    break;
                case Constants.HEAD_LASER:
                    toolHeadID = 2;
                    break;
                case Constants.HEAD_LASER_10W:
                    toolHeadID = 14;
                    break;
                case Constants.HEAD_LASER_20W:
                    toolHeadID = 19;
                    break;
                case Constants.HEAD_LASER_40W:
                    toolHeadID = 20;
                    break;
                case Constants.HEAD_LASER_2W_IR:
                    toolHeadID = 23;
                    break;
                case Constants.HEAD_UNPLUGGED:
                default:
                    toolHeadID = -1;
                    break;
            }
            toolHeadModule.put("key", 1);
            toolHeadModule.put("moduleId", toolHeadID);
            toolHeadModule.put("status", true);
            moduleList.put(toolHeadModule);

            // Virtual Module: Heated Bed
            boolean isHeatedAvailable = getModel().getMachineController().getMachineStatus().bedTemperature > 0;
            if (isHeatedAvailable) {
                JSONObject heatedBedModule = new JSONObject();
                heatedBedModule.put("key", 2);
                heatedBedModule.put("moduleId", 512);
                heatedBedModule.put("status", isHeatedAvailable);
                moduleList.put(heatedBedModule);
            }

            // Addon - Enclosure
            if (getModel().getMachineController().isEnclosureReady()) {
                JSONObject enclosureModule = new JSONObject();
                enclosureModule.put("key", 3);
                enclosureModule.put("moduleId", 5);
                enclosureModule.put("status", getModel().getMachineController().isEnclosureReady());
                moduleList.put(enclosureModule);
            }

            // Addon - Rotary Module
            if (getModel().getMachineController().isRotaryModuleAvailable()) {
                JSONObject rotaryModule = new JSONObject();
                rotaryModule.put("key", 4);
                rotaryModule.put("moduleId", 6);
                rotaryModule.put("status", getModel().getMachineController().getRotaryModuleStatus() == (byte) 0);
                moduleList.put(rotaryModule);
            }

            // Addon - Emergency Stop Module
            if (getModel().getMachineController().isEmergencyStopAvailable()) {
                JSONObject emergencyStopModule = new JSONObject();
                emergencyStopModule.put("key", 5);
                emergencyStopModule.put("moduleId", 8);
                emergencyStopModule.put("status", getModel().getMachineController().isEmergencyStopAvailable());
                moduleList.put(emergencyStopModule);
            }

            // Addon - Air Purifier Module
            if (getModel().getMachineController().isAirPurifierPlugged()) {
                JSONObject airPurifierModule = new JSONObject();
                airPurifierModule.put("key", 6);
                airPurifierModule.put("moduleId", 7);
                airPurifierModule.put("status", getModel().getMachineController().isAirPurifierReady());
                moduleList.put(airPurifierModule);
            }

            // Virtual Addon - Quick Swap Kit
            if (getModel().getMachineController().getExtendKitInfo().isQuickSwapInstalled()) {
                JSONObject quickSwapModule = new JSONObject();
                quickSwapModule.put("key", 7);
                quickSwapModule.put("moduleId", 519);
                quickSwapModule.put("status", getModel().getMachineController().getExtendKitInfo().isQuickSwapInstalled());
                moduleList.put(quickSwapModule);
            }

            if (getModel().getMachineController().getExtendKitInfo().isBracingKitInstalled()) {
                JSONObject bracingKitModule = new JSONObject();
                bracingKitModule.put("key", 8);
                bracingKitModule.put("moduleId", 522);
                bracingKitModule.put("status", getModel().getMachineController().getExtendKitInfo().isBracingKitInstalled());
                moduleList.put(bracingKitModule);
            }

            data.put("moduleList", moduleList);
            response.setBody(new JsonBody(data));
        } catch (JSONException e) {
            LogHelper.log(e);
            response.setBody(new StringBody("Get data failed."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(path = URI_MODULE_INFO)
    void getModuleListInfoFromSM2(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject data = new JSONObject();

        JSONArray moduleInfoList = new JSONArray();

        try {
            // ToolHead
            JSONObject toolHeadInfo = new JSONObject();
            int headType = getModel().getMachineController().getHeadType();
            final FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
            toolHeadInfo.put("key", 1);
            switch (headType) {
                case Constants.HEAD_3DP:
                    toolHeadInfo.put("nozzleTemperature", status.headTemperature);
                    toolHeadInfo.put("nozzleTargetTemperature", status.headTargetTemperature);

                    boolean isFilamentOut = getModel().getMachineController().isFilamentOut();
                    toolHeadInfo.put("isFilamentOut", isFilamentOut);
                    break;
                case Constants.HEAD_3DP_DUAL_EXTRUDER:
                    toolHeadInfo.put("nozzleTemperature1", status.headTemperature);
                    toolHeadInfo.put("nozzleTargetTemperature1", status.headTargetTemperature);
                    toolHeadInfo.put("nozzleTemperature2", status.extruder1Temperature);
                    toolHeadInfo.put("nozzleTargetTemperature2", status.extruder1TargetTemperature);

                    isFilamentOut = getModel().getMachineController().isFilamentOut();
                    toolHeadInfo.put("isFilamentOut", isFilamentOut);
                    break;
                case Constants.HEAD_LASER:
                    float laserFocus = getModel().getMachineController().getLaserFocus();
                    toolHeadInfo.put("laserFocalLength", laserFocus);
                    toolHeadInfo.put("laserPower", status.laserPower);
                    toolHeadInfo.put("laserCamera", getModel().getLaserCameraController().isConnected());
                    break;
                case Constants.HEAD_LASER_10W:
                    toolHeadInfo.put("laserPower", status.laserPower);
                    toolHeadInfo.put("laserCamera", getModel().getLaserCameraController().isConnected());
                    toolHeadInfo.put("laser10WErrorState", getModel().getMachineController().getLaser10WErrorState());
                    break;
                case Constants.HEAD_LASER_20W:
                    toolHeadInfo.put("laserPower", status.laserPower);
                    toolHeadInfo.put("laser20WErrorState", getModel().getMachineController().getLaser10WErrorState());
                    break;
                case Constants.HEAD_LASER_40W:
                    toolHeadInfo.put("laserPower", status.laserPower);
                    toolHeadInfo.put("laser40WErrorState", getModel().getMachineController().getLaser10WErrorState());
                    break;
                case Constants.HEAD_LASER_2W_IR:
                    float platformZ = getModel().getPreferences().getLaserPlatformZ();
                    toolHeadInfo.put("laserFocalLength", platformZ);
                    toolHeadInfo.put("laserPower", status.laserPower);
                    toolHeadInfo.put("laser2WErrorState", getModel().getMachineController().getLaser10WErrorState());
                    break;
                case Constants.HEAD_CNC:
                case Constants.HEAD_CNC_200W:
                    toolHeadInfo.put("spindleSpeed", status.spindleSpeed);
                    break;
            }
            moduleInfoList.put(toolHeadInfo);

            // Virtual Module: Heated Bed
            boolean isHeatedBedAvailable = status.bedTemperature > 0;
            if (isHeatedBedAvailable) {
                JSONObject heatedBedInfo = new JSONObject();
                heatedBedInfo.put("key", 2);
                heatedBedInfo.put("heatedBedTemperature", status.bedTemperature);
                heatedBedInfo.put("heatedBedTargetTemperature", status.bedTargetTemperature);
                moduleInfoList.put(heatedBedInfo);
            }

            // Enclosure
            if (getModel().getMachineController().isEnclosureReady()) {
                JSONObject enclosureInfo = new JSONObject();
                enclosureInfo.put("key", 3);
                enclosureInfo.put("isReady", getModel().getMachineController().isEnclosureReady());
                enclosureInfo.put("led", getModel().getMachineController().getEnclosureLed());
                enclosureInfo.put("fan", getModel().getMachineController().getEnclosureFan());
                enclosureInfo.put("isDoorEnabled", getModel().getMachineController().isEnclosureDoorDetectionEnabled());
                enclosureInfo.put("isEnclosureDoorOpen", getModel().getMachineController().isEnclosureOpen());
                enclosureInfo.put("doorSwitchCount", getModel().getRemoteController().getEnclosureDoorCount());
                moduleInfoList.put(enclosureInfo);
            }

            // Emergency Stop Module
            if (getModel().getMachineController().isEmergencyStopAvailable()) {
                JSONObject emergencyStopInfo = new JSONObject();
                emergencyStopInfo.put("key", 5);
                emergencyStopInfo.put("isEmergencyStopped", getModel().getMachineController().isEmergencyStopTriggered());
                moduleInfoList.put(emergencyStopInfo);
            }

            // Addon - Air Purifier Module
            if (getModel().getMachineController().isAirPurifierPlugged()) {
                JSONObject airPurifierInfo = new JSONObject();
                airPurifierInfo.put("key", 6);
                airPurifierInfo.put("airPurifierSwitch", getModel().getMachineController().isAirPurifierFanOn());
                airPurifierInfo.put("airPurifierFanSpeed", getModel().getMachineController().getAirPurifierFanSpeed());
                airPurifierInfo.put("airPurifierFilterHealth", getModel().getMachineController().getAirPurifierFilterLifeTime());
                moduleInfoList.put(airPurifierInfo);
            }

            // Virtual Addon - Quick Swap Kit
            if (getModel().getMachineController().getExtendKitInfo().isQuickSwapInstalled()) {
                JSONObject quickSwapInfo = new JSONObject();
                quickSwapInfo.put("key", 7);
                quickSwapInfo.put("quickSwapState", getModel().getMachineController().getExtendKitInfo().getQuickSwapState());
                quickSwapInfo.put("quickSwapType", getModel().getMachineController().getExtendKitInfo().getExtendKitConf());
                moduleInfoList.put(quickSwapInfo);
            }

            if (getModel().getMachineController().getExtendKitInfo().isBracingKitInstalled()) {
                JSONObject bracingKitInfo = new JSONObject();
                bracingKitInfo.put("key", 8);
                bracingKitInfo.put("bracingKitState", getModel().getMachineController().getExtendKitInfo().isBracingKitInstalled() ? (byte) 0x01 : (byte) 0x00);
                moduleInfoList.put(bracingKitInfo);
            }

            data.put("moduleInfo", moduleInfoList);
            response.setBody(new JsonBody(data));
        } catch (JSONException e) {
            LogHelper.log(e);
            response.setBody(new StringBody("Pack Module Info data failed."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // file
    @PostMapping(path = URI_UPLOAD_FILE)
    void uploadFile(HttpRequest request, HttpResponse response,
                    @RequestParam(name = "file") MultipartFile file) {
        if (!ensureConnection(request, response)) return;

        if (file.isEmpty()) {
            response.setBody(new StringBody("Empty file body"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        if (file.getFilename() == null) {
            response.setBody(new StringBody("Empty file name"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            File targetFile = new File(getModel().getFilesDir(), file.getFilename());
            file.transferTo(targetFile);
            HTTPEventBus.getInstance().onReceiveFile(targetFile);

            response.setBody(new StringBody("Upload successfully."));
            response.setStatus(StatusCode.SC_OK);
        } catch (IOException e) {
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            LogHelper.log(e);
        }
    }

    @GetMapping(path = URI_PRINT_FILE)
    void downloadPrintFile(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        // TODO: Did we define "print file" in this API?
        //  What if local print is running, and remote access request "print file" ?
        File targetFile = new File(getModel().getWorkspace().getWorkspaceDir().getPath(), "remotePrint.gcode");

        if (!targetFile.exists()) {
            response.setBody(new StringBody("File not Exist."));
            response.setStatus(StatusCode.SC_FORBIDDEN);
        } else {
            response.setBody(new FileBody(targetFile));
            response.setStatus(StatusCode.SC_OK);
        }
    }

    // - Movement

    @PostMapping(path = URI_EXECUTE_CODE)
    void executeCode(HttpRequest request, HttpResponse response,
                     @RequestParam(name = "code") String code) {
        if (!ensureConnection(request, response)) return;

        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        if (status.printerStatus != 0) {
            response.setBody(new StringBody("Machine is printing now, movement rejected."));
            response.setStatus(HttpResponse.SC_FORBIDDEN);
            return;
        }

        try {
            code = URLDecoder.decode(code, "GBK");
        } catch (UnsupportedEncodingException e) {
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String[] lines = code.split("\n");
        String line = lines[0];

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable sub = getModel().getSlaveComputer().sendGcode(line, true)
                .subscribe(res -> {
                    String content = res.getContent();
                    if (content.isEmpty()) {
                        content = "ok";
                    }
                    response.setBody(new StringBody(content));
                    countDownLatch.countDown();
                }, e -> {
                    LogHelper.log(e);
                    response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                });
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            if (line.startsWith("G28") || line.startsWith("G53")
                    || line.startsWith("G54")
                    || line.startsWith("G92")) {
                Disposable subscription = getModel().getMachineController()
                        .updateCoordinateSystem().subscribe(coordinateSystem -> {/**/});
                compositeDisposable.add(subscription);
            }

            response.setStatus(HttpResponse.SC_OK);
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Failed to parse G-code file."));
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        }
    }

    // - Print

    @PostMapping(path = URI_PREPARE_PRINT)
    void preparePrint(HttpRequest request, HttpResponse response,
                      @RequestParam(name = "type") String type,
                      @RequestParam(name = "file") MultipartFile file) {
        if (!ensureConnection(request, response)) return;

        if (file.isEmpty()) {
            response.setBody(new StringBody("Empty file body"));
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return;
        }

        int fileType = Constants.FILE_TYPE_UNKNOWN;
        switch (type) {
            case "3DP":
                fileType = Constants.FILE_TYPE_3DP;
                break;
            case "CNC":
                fileType = Constants.FILE_TYPE_CNC;
                break;
            case "Laser":
                fileType = Constants.FILE_TYPE_LASER;
                break;
            default:
                break;
        }

        MachineController.WorkType workType = getModel().getMachineController().getWorkType();
        boolean isFileTypeConflict = false;
        switch (workType) {
            case FDM:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_3DP;
                break;
            case LASER:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_LASER;
                break;
            case CNC:
                isFileTypeConflict = fileType != Constants.FILE_TYPE_CNC;
                break;
            default:
                isFileTypeConflict = true;
                break;

        }
        if (isFileTypeConflict) {
            response.setBody(new StringBody("Wrong file type."));
            response.setStatus(HttpResponse.SC_CONFLICT);
            return;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        File targetFile = new File(getModel().getWorkspace().getWorkspaceDir().getPath(), file.getFilename());
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        IFile iFile = new FabLocalFile(targetFile);
        getModel().getRemoteController().setFile(iFile);
        getModel().getRemoteController().setFileType(fileType);
        getModel().getRemoteController().setFileName(file.getFilename());
        // Workspace
        getModel().getWorkspace().setPrintSource(Constants.PRINT_SOURCE_LUBAN);
        getModel().getWorkspace().setRemotePrintToWorkSpace(targetFile);

        final IGcodeParser parser = new GcodeParser();
        parser.startParse(iFile, fileType);

        // Wait file parsing to finish
        Disposable sub = parser.getParseProgressObservable()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe(progress -> {
                    Logger.d("progress = " + progress);
                    if (progress == 100) {
                        getModel().getRemoteController().reset();
                        getModel().getRemoteController().setTotalLines(parser.getTotalLinesCount());
                        getModel().getRemoteController().setEstimatedTime(parser.getEstimatedTime());

                        getModel().getWorkspace().setEstimatedTime(parser.getEstimatedTime());
                        getModel().getWorkspace().setFileTotalLineCount(parser.getTotalLinesCount());
                        countDownLatch.countDown();
                    }
                });
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Failed to parse G-code file."));
            response.setStatus(StatusCode.SC_BAD_REQUEST);
            return;
        }
        response.setBody(new StringBody("Prepare successfully."));
    }

    @PostMapping(path = URI_START_PRINT)
    void startPrint(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject result = new JSONObject();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable sub = getModel().getRemoteController().start()
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        response.setStatus(HttpResponse.SC_OK);
                    } else {
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    result.put("code", retCode);
                    countDownLatch.countDown();
                }, LogHelper::log);
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            response.setBody(new JsonBody(result));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_PAUSE_PRINT)
    void pausePrint(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject result = new JSONObject();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable sub = getModel().getRemoteController().pause()
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        response.setStatus(HttpResponse.SC_OK);
                    } else {
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    result.put("code", retCode);
                    countDownLatch.countDown();
                }, LogHelper::log);
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            response.setBody(new JsonBody(result));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_RESUME_PRINT)
    void resumePrint(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        // Fixme: re-thick about filament out situation if requesting start printing
        if (getModel().getMachineController().isFilamentOut()) {
            getModel().getMachineController().clearFilamentOutFlag();
        }

        JSONObject result = new JSONObject();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable sub = getModel().getRemoteController().resume()
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        if (getModel().getMachineController().isEnclosureOpen()) {
                            getModel().getMachineController().clearEnclosureDoorFlag();
                        }
                        response.setStatus(HttpResponse.SC_OK);
                    } else {
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    result.put("code", retCode);
                    countDownLatch.countDown();
                }, LogHelper::log);
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            response.setBody(new JsonBody(result));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_STOP_PRINT)
    void stopPrint(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject result = new JSONObject();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable sub = getModel().getRemoteController().stop()
                .subscribe(retCode -> {
                    if (retCode == 0) {
                        response.setStatus(HttpResponse.SC_OK);
                    } else {
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    result.put("code", retCode);
                    countDownLatch.countDown();
                }, LogHelper::log);
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            response.setBody(new JsonBody(result));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_FILAMENT_UNLOAD)
    void filamentUnload(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable subscription = getModel().getSlaveComputer().requestExtrusion(0, 6, 200, 60, 150)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        response.setStatus(StatusCode.SC_OK);
                    } else {
                        response.setBody(new StringBody("code = " + 0));
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    countDownLatch.countDown();
                });
        compositeDisposable.add(subscription);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_FILAMENT_LOAD)
    void filamentLoad(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Disposable subscription = getModel().getSlaveComputer().requestExtrusion(0, 60, 200, 0, 0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        response.setStatus(StatusCode.SC_OK);
                    } else {
                        response.setBody(new StringBody("code = " + 0));
                        response.setStatus(HttpResponse.SC_CONFLICT);
                    }
                    countDownLatch.countDown();
                });
        compositeDisposable.add(subscription);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_OVERRIDE_NOZZLE)
    void overrideNozzleTemperature(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP && headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        // get parameters
        String nozzleTemp = request.getParameter("nozzleTemp");

        if (nozzleTemp == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        } else {
            float temp = Float.valueOf(nozzleTemp);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            if (MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue())) {
                getModel().getRemoteController().overrideNozzleTemperature(temp);
                countDownLatch.countDown();
            } else {
                Disposable sub = getModel().getSlaveComputer().sendGcode("M104 S" + temp)
                        .subscribe(res -> {
                            countDownLatch.countDown();
                        }, e -> {
                            LogHelper.log(e);
                            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        });
                compositeDisposable.add(sub);
            }

            try {
                countDownLatch.await();
                response.setStatus(StatusCode.SC_OK);
            } catch (InterruptedException e) {
                response.setBody(new StringBody("Interrupted."));
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @PostMapping(path = URI_OVERRIDE_NOZZLE_BY_INDEX)
    void overrideNozzleTemperatureByIndex(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP && headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        // get parameters
        String nozzleTemp = request.getParameter("extruderTemperature");
        String nozzleIndex = request.getParameter("extruderIndex");

        if (nozzleTemp != null && nozzleIndex != null) {
            float temp = Float.parseFloat(nozzleTemp);
            int index = Integer.parseInt(nozzleIndex);

            CountDownLatch countDownLatch = new CountDownLatch(1);
            if (MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue())) {
                getModel().getRemoteController().overrideNozzleTemperature(index, temp);
                countDownLatch.countDown();
            } else {
                Disposable sub = getModel().getSlaveComputer().sendGcode("M104 T" + index + " S" + temp)
                        .subscribe(res -> {
                            countDownLatch.countDown();
                        }, e -> {
                            LogHelper.log(e);
                            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        });
                compositeDisposable.add(sub);
            }

            try {
                countDownLatch.await();
                response.setStatus(StatusCode.SC_OK);
            } catch (InterruptedException e) {
                response.setBody(new StringBody("Interrupted."));
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        }
    }

    @PostMapping(path = URI_OVERRIDE_HEATED_BED)
    void overrideHeatedBedTemperature(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP && headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        // get parameters
        String headBedTemp = request.getParameter("heatedBedTemp");

        if (headBedTemp == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        } else {
            float temp = Float.valueOf(headBedTemp);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            if (MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue())) {
                getModel().getRemoteController().overrideHeatedBedTemperature(temp);
                countDownLatch.countDown();
            } else {
                Disposable sub = getModel().getSlaveComputer().sendGcode("M140 S" + temp)
                        .subscribe(res -> {
                            countDownLatch.countDown();
                        }, e -> {
                            LogHelper.log(e);
                            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        });
                compositeDisposable.add(sub);
            }

            try {
                countDownLatch.await();
                response.setStatus(StatusCode.SC_OK);
            } catch (InterruptedException e) {
                response.setBody(new StringBody("Interrupted."));
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @PostMapping(path = URI_OVERRIDE_Z_OFFSET)
    void overrideZOffset(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP && headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        // get parameters
        String zOffset = request.getParameter("zOffset");

        if (zOffset == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        } else {
            float offset = Float.valueOf(zOffset);
            getModel().getRemoteController().overrideZOffset(offset);

            response.setStatus(StatusCode.SC_OK);
        }
    }

    @PostMapping(path = URI_OVERRIDE_WORK_SPEED)
    void overrideWorkSpeed(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        // get parameters
        String workSpeed = request.getParameter("workSpeed");

        if (workSpeed == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        } else {
            float speed = Float.valueOf(workSpeed);
            getModel().getRemoteController().overrideWorkSpeed(speed);

            response.setStatus(StatusCode.SC_OK);
        }
    }

    @PostMapping(path = URI_OVERRIDE_LASER_POWER)
    void overrideLaserPower(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        if (!isLaser()) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        // get parameters
        String laserPower = request.getParameter("laserPower");

        if (laserPower == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
        } else {
            float power = Float.valueOf(laserPower);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            if (MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue())) {
                getModel().getRemoteController().overrideLaserPower(power);
                countDownLatch.countDown();
            } else {
                Disposable sub = getModel().getSlaveComputer().sendGcode("M3 P" + power)
                        .subscribe(res -> {
                            countDownLatch.countDown();
                        }, e -> {
                            LogHelper.log(e);
                            response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        });
                compositeDisposable.add(sub);
            }

            try {
                countDownLatch.await();
                response.setStatus(StatusCode.SC_OK);
            } catch (InterruptedException e) {
                response.setBody(new StringBody("Interrupted."));
                response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private boolean isLaser() {
        return getModel().getMachineController().getHeadType() == Constants.HEAD_LASER ||
                getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W;
    }

    // enclosure
    @GetMapping(path = URI_ENCLOSURE)
    void getEnclosure(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        JSONObject data = new JSONObject();
        try {
            data.put("isReady", getModel().getMachineController().isEnclosureReady());
            data.put("isDoorEnabled", getModel().getMachineController().isEnclosureDoorDetectionEnabled());
            data.put("led", getModel().getMachineController().getEnclosureLed());
            data.put("fan", getModel().getMachineController().getEnclosureFan());

            response.setBody(new JsonBody(data));
        } catch (JSONException e) {
            LogHelper.log(e);
        }
    }

    @PostMapping(path = URI_ENCLOSURE)
    void setEnclosure(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        if (!getModel().getMachineController().isEnclosureReady()) {
            response.setBody(new StringBody("Enclosure is not available."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(3);
        JSONObject data = new JSONObject();

        String led = request.getParameter("led");
        if (led == null) {
            countDownLatch.countDown();
        } else {
            int value = Integer.parseInt(led);

            Disposable sub = getModel().getSlaveComputer()
                    .setEnclosureLed(value)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(success -> getModel().getMachineController().updateEnclosureStatus())
                    .subscribe(status -> {
                        data.put("led", status.ledLevel);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        String fan = request.getParameter("fan");
        if (fan == null) {
            countDownLatch.countDown();
        } else {
            int value = Integer.parseInt(fan);
            Disposable sub = getModel().getSlaveComputer()
                    .setEnclosureFan(value)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(success -> getModel().getMachineController().updateEnclosureStatus())
                    .subscribe(status -> {
                        data.put("fan", status.fanLevel);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        String doorEnabled = request.getParameter("isDoorEnabled");
        if (doorEnabled == null) {
            countDownLatch.countDown();
        } else {
            boolean isDoorEnabled = Boolean.parseBoolean(doorEnabled);
            Disposable sub = getModel().getSlaveComputer()
                    .setEnclosureDoorDetection(isDoorEnabled)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(success -> getModel().getMachineController().updateEnclosureStatus())
                    .subscribe(status -> {
                        data.put("isDoorEnabled", status.enclosureEnabled);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        try {
            countDownLatch.await();
            if (led == null && fan == null && doorEnabled == null) {
                response.setStatus(StatusCode.SC_BAD_REQUEST);
            }
            response.setBody(new JsonBody(data));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_AIR_PURIFIER_SWITCH)
    void setAirPurifierSwitch(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        if (!getModel().getMachineController().isAirPurifierReady()) {
            response.setBody(new StringBody("Air Purifier is not available."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        JSONObject data = new JSONObject();

        // get parameter
        String stringSwitch = request.getParameter("switch");
        if (stringSwitch == null) {
            countDownLatch.countDown();
        } else {
            boolean enabled = Boolean.parseBoolean(stringSwitch);
            Disposable sub = getModel().getMachineController().setAirPurifierEnabled(enabled)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(ret -> getModel().getMachineController().updateAirPurifierFan())
                    .subscribe(airPurifierFan -> {
                        data.put("airPurifierSwitch", airPurifierFan.isOn);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        try {
            countDownLatch.await();
            if (stringSwitch == null) {
                response.setStatus(StatusCode.SC_BAD_REQUEST);
            }
            response.setBody(new JsonBody(data));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_AIR_PURIFIER_FAN_SPEED)
    void setAirPurifierFanSpeed(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        if (!getModel().getMachineController().isAirPurifierReady()) {
            response.setBody(new StringBody("Air Purifier is not available."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        JSONObject data = new JSONObject();

        // get parameter
        String stringFanSpeed = request.getParameter("fan_speed");
        if (stringFanSpeed == null) {
            countDownLatch.countDown();
        } else {
            int level = Integer.parseInt(stringFanSpeed);
            Disposable sub = getModel().getSlaveComputer().setAirPurifierFanSpeedLevel(level)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(ret -> getModel().getMachineController().updateAirPurifierFan())
                    .subscribe(airPurifierFan -> {
                        data.put("airPurifierFanSpeed", airPurifierFan.level);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        try {
            countDownLatch.await();
            if (stringFanSpeed == null) {
                response.setStatus(StatusCode.SC_BAD_REQUEST);
            }
            response.setBody(new JsonBody(data));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(path = URI_ACTIVE_EXTRUDER)
    void getActiveExtruder(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        JSONObject data = new JSONObject();

        Disposable sub = getModel().getSlaveComputer().getActivatedExtruder()
                .subscribe(which -> {
                    data.put("active", which);
                    countDownLatch.countDown();
                }, e -> {
                    LogHelper.log(e);
                    response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                    countDownLatch.countDown();
                });
        compositeDisposable.add(sub);

        try {
            countDownLatch.await();
            response.setBody(new JsonBody(data));
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_SWITCH_EXTRUDER)
    void switchActiveExtruder(HttpRequest request, HttpResponse response, @RequestParam("active") String extruderIndex) {
        if (!ensureConnection(request, response)) return;

        int headType = getModel().getMachineController().getHeadType();
        if (headType != Constants.HEAD_3DP_DUAL_EXTRUDER) {
            response.setBody(new StringBody("Wrong head type."));
            response.setStatus(StatusCode.SC_CONFLICT);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);

        if (extruderIndex == null) {
            response.setStatus(StatusCode.SC_BAD_REQUEST);
            countDownLatch.countDown();
        } else {
            int value = Integer.parseInt(extruderIndex);
            Disposable sub = getModel().getSlaveComputer().switchToExtruder(value)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .subscribe(result -> {
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            response.setBody(new StringBody("Interrupted."));
            response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = URI_SET_LASER_CROSS_LINE_INDICATOR_STATUS)
    void setCrosshairIndicatorStatus(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        JSONObject data = new JSONObject();

        // get parameter
        String stringStatus = request.getParameter("status");
        if (stringStatus == null) {
            countDownLatch.countDown();
        } else {
            boolean active = Boolean.parseBoolean(stringStatus);
            Disposable sub = getModel().getSlaveComputer().setCrossLineLaserIndicator(active)
                    .doOnNext(success -> response.setStatus(success ? StatusCode.SC_OK : StatusCode.SC_CONFLICT))
                    .flatMap(ret -> getModel().getSlaveComputer().getCrossLineLaserIndicatorStatus())
                    .subscribe(indicatorStatus -> {
                        data.put("status", indicatorStatus);
                        countDownLatch.countDown();
                    }, e -> {
                        LogHelper.log(e);
                        response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                        countDownLatch.countDown();
                    });
            compositeDisposable.add(sub);
        }
    }

    @GetMapping(path = URI_GET_LASER_CROSS_LINE_INDICATOR_STATUS)
    void getCrosshairIndicatorStatus(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        JSONObject data = new JSONObject();

        Disposable sub = getModel().getSlaveComputer().getCrossLineLaserIndicatorStatus()
                .subscribe(indicatorStatus -> {
                    data.put("status", indicatorStatus);
                    countDownLatch.countDown();
                }, e -> {
                    LogHelper.log(e);
                    response.setStatus(StatusCode.SC_INTERNAL_SERVER_ERROR);
                    countDownLatch.countDown();
                });
        compositeDisposable.add(sub);
    }

    // test api for incoming quality control

    @GetMapping(URI_IQC_CHECK_ALIVE)
    void iqcCheckAlive(HttpResponse response) {
        response.setStatus(StatusCode.SC_OK);
    }

    @PostMapping(URI_IQC_UPLOAD_TEST)
    void iqcUploadTest(HttpResponse response, @RequestParam("file") MultipartFile file) {
        response.setStatus(StatusCode.SC_OK);
    }

    @GetMapping(URI_LASER_MATERIAL_THICKNESS)
    void laserMaterialThckness(HttpRequest request, HttpResponse response,
                               @RequestParam("x") float x,
                               @RequestParam("y") float y,
                               @RequestParam("feedRate") int f) {
        JSONObject json = new JSONObject();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        //Measure height.
        boolean isA150Model = getModel().getMachineController().getMachineModel() == Constants.MACHINE_MODEL_SNAPMAKER_A150;
        float initZ = isA150Model ? 150f : 170f;
        Disposable subscription = getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(success -> getModel().getSlaveComputer().gotoAbsolutePosition(x, y, initZ, f))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .flatMap(success -> getModel().getSlaveComputer().setAFAssistLightState(1))
                .flatMap(success -> getModel().getLaserCameraController().setExposeTime(1))
                .flatMap(success -> getModel().getLaserCameraController().requestCapturePhoto())
                .flatMap(success -> getModel().getLaserCameraController().watchPhotoReceive())
                .subscribe(bitmap -> {
                    json.put("status", true);
                    FileOutputStream out = new FileOutputStream(getModel().getCacheDir() + "/distance.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    float spotX = LaserDistanceMeasureProcess.process(bitmap);
                    if (spotX < -200) {
                        json.put("status", false);
                        countDownLatch.countDown();
                        return;
                    }
                    Logger.i("Detected spot x position is %s", spotX);
                    float mS1plus = getModel().getPreferences().getLaserThicknessS1Plus();
                    float mS2plus = getModel().getPreferences().getLaserThicknessS2Plus();
                    float h1 = H1_Z_POSITION + CAMERA_HEIGHT_OFFSET;
                    float h2 = H2_Z_POSITION + CAMERA_HEIGHT_OFFSET;
                    float h3 = h1 - h2;
                    float thickness = h1 - (h1 * ((h3 * mS1plus) + ((mS2plus * h2) - (mS1plus * h1))) / (h3 * spotX + ((mS2plus * h2) - (mS1plus * h1)))) + MockConst.LASER_MATERIAL_MEASURE_CALIBRATION_OBJECT_HEIGHT;
                    json.put("thickness", thickness);
                    countDownLatch.countDown();
                }, e -> {
                    Disposable sub = getModel().getSlaveComputer().setAFAssistLightState(0).subscribe();
                    sub.dispose();
                    e.printStackTrace();
                    json.put("status", false);
                    countDownLatch.countDown();
                });
        compositeDisposable.add(subscription);
        try {
            countDownLatch.await();
            final ResponseBody body = new JsonBody(json);
            response.setBody(body);
        } catch (InterruptedException e) {
            try {
                json.put("status", false);
                final ResponseBody body = new JsonBody(json);
                response.setBody(body);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        } finally {
            Disposable subscribe = getModel().getSlaveComputer()
                    .setAFAssistLightState(0)
                    .flatMap(success -> getModel().getLaserCameraController().setExposeTime(0))
                    .subscribe(success -> {
                    }, LogHelper::log);
            compositeDisposable.add(subscribe);
        }
    }
}
