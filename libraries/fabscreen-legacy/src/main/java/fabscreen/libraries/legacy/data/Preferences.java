package fabscreen.libraries.legacy.data;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    // machine
    private static final String MACHINE_NAME = "MACHINE_NAME";
    private static final String MACHINE_MODEL = "MACHINE_MODEL";
    private static final String MACHINE_SETUP_FLAG = "MACHINE_SETUP_FLAG";
    private static final String MACHINE_UPDATE_FLAG = "MACHINE_UPDATE_FLAG";

    private static final String MACHINE_SETUP_LANGUAGE = "MACHINE_SETUP_LANGUAGE";

    private static final String MACHINE_SETUP_3DP = "MACHINE_SETUP_3DP";
    private static final String MACHINE_SETUP_LASER = "MACHINE_SETUP_LASER";
    private static final String MACHINE_SETUP_CNC = "MACHINE_SETUP_CNC";
    private static final String MACHINE_SETUP_10W_LASER = "MACHINE_SETUP_10W_LASER";
    private static final String MACHINE_SETUP_20W_LASER = "MACHINE_SETUP_20W_LASER";
    private static final String MACHINE_SETUP_40W_LASER = "MACHINE_SETUP_40W_LASER";
    private static final String MACHINE_SETUP_2W_LASER = "MACHINE_SETUP_2W_LASER";
    private static final String MACHINE_SETUP_ROTARY_LASER = "MACHINE_SETUP_ROTARY_LASER";
    private static final String MACHINE_SETUP_ROTARY_CNC = "MACHINE_SETUP_ROTARY_CNC";
    private static final String MACHINE_SETUP_ROTARY_10W_LASER = "MACHINE_SETUP_ROTARY_10W_LASER";
    private static final String MACHINE_SETUP_3DP_DUAL_EXTRUDER = "MACHINE_SETUP_3DP_DUAL_EXTRUDER";
    private static final String MACHINE_SETUP_ROTARY_20W_LASER = "MACHINE_SETUP_ROTARY_20W_LASER";
    private static final String MACHINE_SETUP_ROTARY_40W_LASER = "MACHINE_SETUP_ROTARY_40W_LASER";
    private static final String MACHINE_SETUP_ROTARY_2W_LASER = "MACHINE_SETUP_ROTARY_2W_LASER";



    // ?
    private static final String API_HOST = "API_HOST";
    private static final String UPDATE_PACKAGE_VERSION = "UPDATE_PACKAGE_VERSION";
    private static final String CHECK_UPDATE_FLAG = "CHECK_UPDATE_FLAG";
    private static final String UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION";
    private static final String UPDATE_LAST_CHECK_VERSION = "UPDATE_LAST_CHECK_VERSION";

    // laser
    private static final String LASER_MATERIAL_THICKNESS = "LASER_MATERIAL_THICKNESS";
    private static final String LASER_BOTTOM_Z = "LASER_BOTTOM_Z";
    private static final String LASER_CAMERA_CALIBRATION = "LASER_CAMERA_CALIBRATION";
    private static final String LASER_CONTROL_POWER = "LASER_CONTROL_POWER";

    // 10W Laser
    private static final String LASER_10W_CAMERA_CALIBRATION = "LASER_10W_CAMERA_CALIBRATION";

    // print
    private static final String FILE_LOC = "FILE_LOC";
    private static final String PRINT_SOURCE = "PRINT_SOURCE";
    private static final String FILE_NAME = "FILE_NAME";
    private static final String FILE_PATH = "FILE_PATH";
    private static final String FILE_ESTIMATED_TIME = "FILE_ESTIMATED_TIME";
    private static final String FILE_TOTAL_LINES = "FILE_TOTAL_LINES";
    private static final String PRINT_ELAPSED_TIME = "PRINT_ELAPSED_TIME";

    // 3DP
    private static final String A3DP_CALIBRATION_MODE = "3DP_CALIBRATION_MODE";
    private static final String A3DP_FAST_CALIBRATION_ON = "3DP_FAST_CALIBRATION_ON";
    private static final String A3DP_CALIBRATION_GRID = "3DP_CALIBRATION_GRID";
    private static final String A3DP_CALIBRATION_HEATED_LEVELING_ON = "3DP_CALIBRATION_HEATED_LEVELING_ON";
    private static final String A3DP_CALIBRATION_HEATED_UP_TEMPERATURE = "3DP_CALIBRATION_HEATED_UP_TEMPERATURE";

    //3DP dual
    private static final String A3DP_DUAL_NEED_DO_Z_HEIGHT_CALIBRATION = "A3DP_DUAL_NEED_DO_Z_HEIGHT_CALIBRATION";
    private static final String A3DP_DUAL_NEED_DO_XY_OFFSET_CALIBRATION = "A3DP_DUAL_NEED_DO_XY_OFFSET_CALIBRATION";

    // Laser
    private static final String LASER_CALIBRATION_MODE = "LASER_CALIBRATION_MODE";
    private static final String LASER_CAMERA_LIGHT_ON = "LASER_CAMERA_LIGHT_ON";
    private static final String LASER_4AXIS_CALIBRATION_MODE = "LASER_4AXIS_CALIBRATION_MODE";
    private static final String LASER_PARAM_S1_PLUS_VALUE = "LASER_MEASURE_S1_PLUS_VALUE";
    private static final String LASER_PARAM_S2_PLUS_VALUE = "LASER_MEASURE_S2_PLUS_VALUE";
    private static final String LASER_PLATFORM_Z = "LASER_PLATFORM_Z";

    // Addon
    private static final String ADD_ON_AIR_PURIFIER_3DP_AUTO_ON_FLAG = "AIR_PURIFIER_3DP_AUTO_ON_FLAG";
    private static final String ADD_ON_AIR_PURIFIER_LASER_AUTO_ON_FLAG = "AIR_PURIFIER_LASER_AUTO_ON_FLAG";
    private static final String ADD_ON_AIR_PURIFIER_CNC_AUTO_ON_FLAG = "AIR_PURIFIER_CNC_AUTO_ON_FLAG";
    private static final String ADD_ON_AIR_PURIFIER_AUTO_OFF_FLAG = "AIR_PURIFIER_AUTO_OFF_FLAG";
    private static final String ADD_ON_ENCLOSURE_AUTO_LIGHTING_ON_FLAG = "ENCLOSURE_AUTO_LIGHTING_ON_FLAG";

    // Settings

    private static final String SETTINGS_FIREBASE_ANALYTICS_FLAG = "SETTINGS_FIREBASE_ANALYTICS_FLAG";
    private static final String SETTINGS_USER_SELECTED_LANGUAGE = "SETTINGS_USER_SELECTED_LANGUAGE";
    // Legacy String for sp
    private static final String SETTINGS_EXTEND_KIT_CHECK_ON_START_UP = "SETTINGS_QUICK_SWAP_CHECK_ON_START_UP";

    private static final String SETTING_LASER_INDICATOR_MODE = "SETTING_LASER_INDICATOR_MODE";

    private static final String HEADER_ONLINE_SYNC_ID = "HEADER_ONLINE_SYNC_ID";
    private static final String DEBUG_FLAG = "DEBUG_FLAG";

    private Context context;

    public Preferences(Context context) {
        this.context = context;
    }

    private void removeKey(String key) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().remove(key).apply();
    }

    private boolean getPref(String key, boolean defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, defValue);
    }

    private String getPref(String key, String defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }

    private float getPref(String key, float defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getFloat(key, defValue);
    }

    private int getPref(String key, int defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defValue);
    }

    private void setPref(String key, boolean value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(key, value).apply();
    }

    private void setPref(String key, float value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putFloat(key, value).apply();
    }

    private void setPref(String key, int value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putInt(key, value).apply();
    }

    private void setPref(String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putString(key, value).apply();
    }

    public void reset() {
        setMachineName("");
        setMachineModel("");

        setUserSelectedLanguage(MultiLanguageManager.LANGUAGE_DEFAULT);

        setMachineSetupFlag(false);
        setMachineSetup3DP(false);
        setMachineSetup3DPDualExtruder(false);
        setMachineSetupLaser(false);
        setMachineSetup10WLaser(false);
        setMachineSetup20WLaser(false);
        setMachineSetup40WLaser(false);
        setMachineSetupCNC(false);
        setMachineSetupRotaryLaser(false);
        setMachineSetupRotary10WLaser(false);
        setMachineSetupRotary20WLaser(false);
        setMachineSetupRotary40WLaser(false);
        setMachineSetupRotaryCNC(false);
        setMachineUpdatedFlag(false);
        setMachineSetupLanguage(false);

        setExtendKitCheckOnStartUp(true);

        setLastUpdatePackageVersion("");
        setCheckUpdateFlag(true);
        setLastCheckVersion("0.0.0.0");
        setUpdateNotification(false);
        setLaserMaterialThickness(1.5f);
        setLaserControlPower(100f);

        setPrintLoc(true);
        setPrintSource(0);
        setPrintFilePath(null);

        set3DPCalibrationMode(0);
        set3DPFastCalibrationOn(true);
        set3DPCalibrationHeatedLevelingOn(false);
        set3DPCalibrationHeatedUpTemperature(70.0f);
        setLaserCalibrationMode(0);
        setLaser4AxisCalibrationMode(1);

        setEnclosureAutoLightingOn(true);

        setAirPurifier3DPAutoFlag(false);
        setAirPurifierLaserAutoFlag(true);
        setAirPurifierCNCAutoTurnOnFlag(false);
        setAirPurifierAutoTurnOffFlag(true);

        setLaserIndicatorMode(0);

        setDebugFlag(false);
    }

    // -- Machine

    public String getMachineName() {
        return getPref(MACHINE_NAME, "");
    }

    public void setMachineName(String name) {
        setPref(MACHINE_NAME, name);
    }

    public String getMachineModel() {
        return getPref(MACHINE_MODEL, Constants.MACHINE_TYPE_A150);
    }

    public void setMachineModel(String machineModel) {
        setPref(MACHINE_MODEL, machineModel);
    }

    /**
     * Indicates if screen has been setup.
     */
    public boolean getMachineSetupFlag() {
        return getPref(MACHINE_SETUP_FLAG, false);
    }

    public void setMachineSetupFlag(boolean flag) {
        setPref(MACHINE_SETUP_FLAG, flag);
    }

    public boolean getMachineSetupLanguage() {
        return getPref(MACHINE_SETUP_LANGUAGE, false);
    }

    public void setMachineSetupLanguage(boolean flag) {
        setPref(MACHINE_SETUP_LANGUAGE, flag);
    }

    public boolean getMachineSetup3DP() {
        return getPref(MACHINE_SETUP_3DP, false);
    }

    public void setMachineSetup3DP(boolean flag) {
        setPref(MACHINE_SETUP_3DP, flag);
    }

    public boolean getMachineSetup3DPDualExtruder() {
        return getPref(MACHINE_SETUP_3DP_DUAL_EXTRUDER, false);
    }


    public void setMachineSetup3DPDualExtruder(boolean flag) {
        setPref(MACHINE_SETUP_3DP_DUAL_EXTRUDER, flag);
    }

    public boolean getMachineSetupLaser() {
        return getPref(MACHINE_SETUP_LASER, false);
    }

    public void setMachineSetupLaser(boolean flag) {
        setPref(MACHINE_SETUP_LASER, flag);
    }

    public boolean getMachineSetup10WLaser() {
        return getPref(MACHINE_SETUP_10W_LASER, false);
    }

    public void setMachineSetup10WLaser(boolean flag) {
        setPref(MACHINE_SETUP_10W_LASER, flag);
    }

    public void setMachineSetup20WLaser(boolean flag) {
        setPref(MACHINE_SETUP_20W_LASER, flag);
    }

    public boolean getMachineSetup20WLaser() {
        return getPref(MACHINE_SETUP_20W_LASER, false);
    }

    public void setMachineSetup2WLaser(boolean flag) {
        setPref(MACHINE_SETUP_2W_LASER, flag);
    }

    public boolean getMachineSetup2WLaser() {
        return getPref(MACHINE_SETUP_2W_LASER, false);
    }

    public void setMachineSetup40WLaser(boolean flag) {
        setPref(MACHINE_SETUP_40W_LASER, flag);
    }

    public boolean getMachineSetup40WLaser() {
        return getPref(MACHINE_SETUP_40W_LASER, false);
    }

    public boolean getMachineSetupCNC() {
        return getPref(MACHINE_SETUP_CNC, false);
    }

    public void setMachineSetupCNC(boolean flag) {
        setPref(MACHINE_SETUP_CNC, flag);
    }

    public boolean getMachineSetupRotaryLaser() {
        return getPref(MACHINE_SETUP_ROTARY_LASER, false);
    }

    public void setMachineSetupRotaryLaser(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_LASER, flag);
    }

    public boolean getMachineSetupRotary10WLaser() {
        return getPref(MACHINE_SETUP_ROTARY_10W_LASER, false);
    }

    public void setMachineSetupRotary10WLaser(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_10W_LASER, flag);
    }

    public boolean getMachineSetupRotary20WLaser() {
        return getPref(MACHINE_SETUP_ROTARY_20W_LASER, false);
    }

    public void setMachineSetupRotary20WLaser(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_20W_LASER, flag);
    }

    public boolean getMachineSetupRotary40WLaser() {
        return getPref(MACHINE_SETUP_ROTARY_40W_LASER, false);
    }

    public void setMachineSetupRotary40WLaser(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_40W_LASER, flag);
    }

    public boolean getMachineSetupRotary2WLaser() {
        return getPref(MACHINE_SETUP_ROTARY_2W_LASER, false);
    }

    public void setMachineSetupRotary2WLaser(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_2W_LASER, flag);
    }

    public boolean getMachineSetupRotaryCNC() {
        return getPref(MACHINE_SETUP_ROTARY_CNC, false);
    }

    public void setMachineSetupRotaryCNC(boolean flag) {
        setPref(MACHINE_SETUP_ROTARY_CNC, flag);
    }

    /**
     * Indicates if screen/main controller is just updated.
     */
    public boolean getMachineUpdatedFlag() {
        return getPref(MACHINE_UPDATE_FLAG, false);
    }

    public void setMachineUpdatedFlag(boolean flag) {
        setPref(MACHINE_UPDATE_FLAG, flag);
    }

    // -- ?

    public String getApiHost() {
        return getPref(API_HOST, "https://api.snapmaker.com/");
    }

    public void setApiHost(String apiHost) {
        setPref(API_HOST, apiHost);
    }

    public String getLastUpdatePackageVersion() {
        return getPref(UPDATE_PACKAGE_VERSION, "");
    }

    public void setLastUpdatePackageVersion(String version) {
        setPref(UPDATE_PACKAGE_VERSION, version);
    }

    public boolean getCheckUpdateFlag() {
        return getPref(CHECK_UPDATE_FLAG, true);
    }

    public void setCheckUpdateFlag(boolean updateFlag) {
        setPref(CHECK_UPDATE_FLAG, updateFlag);
    }

    public boolean getUpdateNotification() {
        return getPref(UPDATE_NOTIFICATION, false);
    }

    public void setUpdateNotification(boolean notification) {
        setPref(UPDATE_NOTIFICATION, notification);
    }

    public String getLastCheckVersion() {
        return getPref(UPDATE_LAST_CHECK_VERSION, "0.0.0.0");
    }

    public void setLastCheckVersion(String version) {
        setPref(UPDATE_LAST_CHECK_VERSION, version);
    }

    // -- Laser

    public float getLaserMaterialThickness() {
        return getPref(LASER_MATERIAL_THICKNESS, 0f);
    }

    public void setLaserMaterialThickness(float thickness) {
        setPref(LASER_MATERIAL_THICKNESS, thickness);
    }

    public float getLaserBottomZ() {
        return getPref(LASER_BOTTOM_Z, 0f);
    }

    public void setLaserBottomZ(float z) {
        setPref(LASER_BOTTOM_Z, z);
    }

    public float getLaserPlatformZ() {
        return getPref(LASER_PLATFORM_Z, 0f);
    }

    public void setLaserPlatformZ(float z) {
        setPref(LASER_PLATFORM_Z, z);
    }

    public String getCameraCalibration() {
        return getPref(LASER_CAMERA_CALIBRATION, null);
    }

    public void setCameraCalibration(String result) {
        setPref(LASER_CAMERA_CALIBRATION, result);
    }

    public float getLaserControlPower() {
        return getPref(LASER_CONTROL_POWER, 15f);
    }

    public void setLaserControlPower(float power) {
        setPref(LASER_CONTROL_POWER, power);
    }

    // -- 10W Laser
    public String get10WLaserCameraCalibration() {
        return getPref(LASER_10W_CAMERA_CALIBRATION, null);
    }

    public void set10WLaserCameraCalibration(String result) {
        setPref(LASER_10W_CAMERA_CALIBRATION, result);
    }

    // -- File / Print

    public boolean getPrintLoc() {
        return getPref(FILE_LOC, false); // local: true, USB: false
    }

    public void setPrintLoc(boolean isLocal) {
        setPref(FILE_LOC, isLocal);
    }

    public int getPrintSource() {
        return getPref(PRINT_SOURCE, 0);
    }

    public void setPrintSource(int source) {
        setPref(PRINT_SOURCE, source);
    }

    public String getPrintFilePath() {
        return getPref(FILE_PATH, null);
    }

    public void setPrintFilePath(String filePath) {
        setPref(FILE_PATH, filePath);
    }

    public int getPrintFileTotalLines() {
        return getPref(FILE_TOTAL_LINES, 0);
    }

    public void setPrintFileTotalLines(int linesCount) {
        setPref(FILE_TOTAL_LINES, linesCount);
    }

    public float getPrintFileEstimatedTime() {
        return getPref(FILE_ESTIMATED_TIME, 0f);
    }

    public void setPrintFileEstimatedTime(float estimatedTime) {
        setPref(FILE_ESTIMATED_TIME, estimatedTime);
    }

    public int getPrintElapsedTime() {
        return getPref(PRINT_ELAPSED_TIME, 0);
    }

    public void setPrintElapsedTime(int time) {
        setPref(PRINT_ELAPSED_TIME, time);
    }

    // -- 3DP

    /**
     * Calibration Mode
     * <p>
     * 0 - Auto Mode
     * 1 - Manual Mode
     */
    public int get3DPCalibrationMode() {
        return getPref(A3DP_CALIBRATION_MODE, 0);
    }

    public void set3DPCalibrationMode(int mode) {
        setPref(A3DP_CALIBRATION_MODE, mode);
    }

    public boolean get3DPFastCalibrationOn() {
        return getPref(A3DP_FAST_CALIBRATION_ON, true);
    }

    public void set3DPFastCalibrationOn(boolean on) {
        setPref(A3DP_FAST_CALIBRATION_ON, on);
    }

    public int get3DPCalibrationGrid() {
        return getPref(A3DP_CALIBRATION_GRID, 3);
    }

    public void set3DPCalibrationGrid(int grid) {
        setPref(A3DP_CALIBRATION_GRID, grid);
    }

    public boolean get3DPCalibrationHeatedLevelingOn() {
        return getPref(A3DP_CALIBRATION_HEATED_LEVELING_ON, false);
    }

    public void set3DPCalibrationHeatedLevelingOn(boolean on) {
        setPref(A3DP_CALIBRATION_HEATED_LEVELING_ON, on);
    }

    public float get3DPCalibrationHeatedUpTemperature() {
        return getPref(A3DP_CALIBRATION_HEATED_UP_TEMPERATURE, 70.0f);
    }

    public void set3DPCalibrationHeatedUpTemperature(float temperature) {
        setPref(A3DP_CALIBRATION_HEATED_UP_TEMPERATURE, temperature);
    }

    // -- Laser

    public int getLaserCalibrationMode() {
        return getPref(LASER_CALIBRATION_MODE, 0);
    }

    public void setLaserCalibrationMode(int mode) {
        setPref(LASER_CALIBRATION_MODE, mode);
    }

    public int getLaser4AxisCalibrationMode() {
        return getPref(LASER_4AXIS_CALIBRATION_MODE, 1);
    }

    public void setLaser4AxisCalibrationMode(int mode) {
        setPref(LASER_4AXIS_CALIBRATION_MODE, mode);
    }


    public boolean getLaserCameraLightOn() {
        return getPref(LASER_CAMERA_LIGHT_ON, true);
    }

    public void setLaserCameraLightOn(boolean cameraLightOn) {
        setPref(LASER_CAMERA_LIGHT_ON, cameraLightOn);
    }

    // -- ADD ONS
    public boolean getAirPurifier3DPAutoFlag() {
        return getPref(ADD_ON_AIR_PURIFIER_3DP_AUTO_ON_FLAG, false);
    }

    public void setAirPurifier3DPAutoFlag(boolean flag) {
        setPref(ADD_ON_AIR_PURIFIER_3DP_AUTO_ON_FLAG, flag);
    }

    public boolean getAirPurifierLaserAutoFlag() {
        return getPref(ADD_ON_AIR_PURIFIER_LASER_AUTO_ON_FLAG, true);
    }

    public void setAirPurifierLaserAutoFlag(boolean flag) {
        setPref(ADD_ON_AIR_PURIFIER_LASER_AUTO_ON_FLAG, flag);
    }

    public boolean getAirPurifierCNCAutoTurnOnFlag() {
        return getPref(ADD_ON_AIR_PURIFIER_CNC_AUTO_ON_FLAG, false);
    }

    public void setAirPurifierCNCAutoTurnOnFlag(boolean flag) {
        setPref(ADD_ON_AIR_PURIFIER_CNC_AUTO_ON_FLAG, flag);
    }

    public boolean getAirPurifierAutoTurnOffFlag() {
        return getPref(ADD_ON_AIR_PURIFIER_AUTO_OFF_FLAG, true);
    }

    public void setAirPurifierAutoTurnOffFlag(boolean flag) {
        setPref(ADD_ON_AIR_PURIFIER_AUTO_OFF_FLAG, flag);
    }

    public boolean getEnclosureAutoLightingOn() {
        return getPref(ADD_ON_ENCLOSURE_AUTO_LIGHTING_ON_FLAG, true);
    }

    public void setEnclosureAutoLightingOn(boolean flag) {
        setPref(ADD_ON_ENCLOSURE_AUTO_LIGHTING_ON_FLAG, flag);
    }

    // -- Settings
    public boolean getFirebaseAnalyticsFlag() {
        return getPref(SETTINGS_FIREBASE_ANALYTICS_FLAG, true);
    }

    public void setFirebaseAnalyticsFlag(boolean enabled) {
        setPref(SETTINGS_FIREBASE_ANALYTICS_FLAG, enabled);
    }

    public int getUserSelectedLanguage() {
        return getPref(SETTINGS_USER_SELECTED_LANGUAGE, MultiLanguageManager.LANGUAGE_DEFAULT);
    }

    public void setUserSelectedLanguage(int selectedLanguage) {
        setPref(SETTINGS_USER_SELECTED_LANGUAGE, selectedLanguage);
    }

    public void setHeaderOnlineSyncID(int leaserHeaderID) {
        setPref(HEADER_ONLINE_SYNC_ID, leaserHeaderID);
    }

    public int getHeaderOnlineSyncID() {
        return getPref(HEADER_ONLINE_SYNC_ID, -1);
    }

    public void setExtendKitCheckOnStartUp(boolean needCheckOnStartUp) {
        setPref(SETTINGS_EXTEND_KIT_CHECK_ON_START_UP, needCheckOnStartUp);
    }

    public boolean getExtendKitCheckOnStartUp() {
        return getPref(SETTINGS_EXTEND_KIT_CHECK_ON_START_UP, true);
    }

    public void setLaserIndicatorMode(int mode) {
        setPref(SETTING_LASER_INDICATOR_MODE, mode);
    }

    public int getLaserIndicatorMode() {
        return getPref(SETTING_LASER_INDICATOR_MODE, 0);
    }

    // -- Debug
    public boolean getDebugFlag() {
        return getPref(DEBUG_FLAG, false);
    }

    public void setDebugFlag(boolean flag) {
        setPref(DEBUG_FLAG, flag);
    }

    public float getLaserThicknessS1Plus() {
        return getPref(LASER_PARAM_S1_PLUS_VALUE, -1f);
    }

    public float getLaserThicknessS2Plus() {
        return getPref(LASER_PARAM_S2_PLUS_VALUE, -1f);
    }

    public void setLaserParamS1Plus(float s1Plus) {
        setPref(LASER_PARAM_S1_PLUS_VALUE, s1Plus);
    }

    public void setLaserParamS2Plus(float s2Plus) {
        setPref(LASER_PARAM_S2_PLUS_VALUE, s2Plus);
    }

    public boolean getNeedDoZHeightCalibration() {
        return getPref(A3DP_DUAL_NEED_DO_Z_HEIGHT_CALIBRATION, true);
    }

    public void setNeedDoZHeightCalibration(boolean need) {
        setPref(A3DP_DUAL_NEED_DO_Z_HEIGHT_CALIBRATION, need);
    }

    public boolean getNeedDoXYOffsetCalibration() {
        return getPref(A3DP_DUAL_NEED_DO_XY_OFFSET_CALIBRATION, true);
    }

    public void setNeedDoXYOffsetCalibration(boolean need) {
        setPref(A3DP_DUAL_NEED_DO_XY_OFFSET_CALIBRATION, need);
    }
}
