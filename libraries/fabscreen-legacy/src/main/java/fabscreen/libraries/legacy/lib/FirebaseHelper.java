package fabscreen.libraries.legacy.lib;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import fabscreen.libraries.legacy.data.Constants;

public class FirebaseHelper {
    private final static String EVENT_START_UP = "start";
    private final static String EVENT_SCREEN_FINISH_PRINT = "finish_print";
    private final static String EVENT_REMOTE_FILE_RECEIVED = "remote_file_received";

    private final static String PARAM_HEAD_TYPE = "head_type";
    private final static String PARAM_FILE_TYPE = "file_type";
    private final static String PARAM_REMOTE_RECEIVE_FILE_LENGTH_LEVEL = "file_length_level";
    private final static String PARAM_MODEL = "model";

    public static void logStartUpEvent(FirebaseAnalytics firebaseAnalytics, int headType, int machineModel) {
        if (firebaseAnalytics == null) return;

        Bundle params = new Bundle();

        switch (headType) {
            case Constants.HEAD_3DP:
                params.putString(PARAM_HEAD_TYPE, "3DP");
                break;
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                params.putString(PARAM_HEAD_TYPE, "Dual Extrusion 3DP");
                break;
            case Constants.HEAD_LASER:
                params.putString(PARAM_HEAD_TYPE, "Laser");
                break;
            case Constants.HEAD_LASER_10W:
                params.putString(PARAM_HEAD_TYPE, "Laser 10W");
                break;
            case Constants.HEAD_LASER_20W:
                params.putString(PARAM_HEAD_TYPE, "Laser 20W");
                break;
            case Constants.HEAD_LASER_40W:
                params.putString(PARAM_HEAD_TYPE, "Laser 40W");
                break;
            case Constants.HEAD_CNC:
                params.putString(PARAM_HEAD_TYPE, "CNC");
                break;
            case Constants.HEAD_CNC_200W:
                params.putString(PARAM_HEAD_TYPE, "CNC 200W");
                break;
            case Constants.HEAD_UNPLUGGED:
                params.putString(PARAM_HEAD_TYPE, "Unplugged");
                break;
            case Constants.HEAD_FACTORY_3DP:
            case Constants.HEAD_FACTORY_LASER:
            case Constants.HEAD_FACTORY_CNC:
                params.putString(PARAM_HEAD_TYPE, "Factory Tools");
                break;
            default:
                params.putString(PARAM_HEAD_TYPE, "Unknown");
                break;
        }

        switch (machineModel) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150:
                params.putString(PARAM_MODEL, Constants.MACHINE_TYPE_A150);
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A250:
                params.putString(PARAM_MODEL, Constants.MACHINE_TYPE_A250);
                break;
            case Constants.MACHINE_MODEL_SNAPMAKER_A350:
                params.putString(PARAM_MODEL, Constants.MACHINE_TYPE_A350);
                break;
            default:
                params.putString(PARAM_MODEL, "Unknown");
                break;
        }

        firebaseAnalytics.logEvent(EVENT_START_UP, params);
    }

    public static void logFinishPrintEvent(FirebaseAnalytics firebaseAnalytics, int headType) {
        if (firebaseAnalytics == null) return;

        Bundle params = new Bundle();

        switch (headType) {
            case Constants.HEAD_3DP:
                params.putString(PARAM_HEAD_TYPE, "3DP");
                break;
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                params.putString(PARAM_HEAD_TYPE, "Dual Extrusion 3DP");
                break;
            case Constants.HEAD_LASER:
                params.putString(PARAM_HEAD_TYPE, "Laser");
                break;
            case Constants.HEAD_LASER_10W:
                params.putString(PARAM_HEAD_TYPE, "Laser 10W");
                break;
            case Constants.HEAD_LASER_2W_IR:
                params.putString(PARAM_HEAD_TYPE, "Laser 2W");
                break;
            case Constants.HEAD_LASER_20W:
                params.putString(PARAM_HEAD_TYPE, "Laser 20W");
                break;
            case Constants.HEAD_LASER_40W:
                params.putString(PARAM_HEAD_TYPE, "Laser 40W");
                break;
            case Constants.HEAD_CNC:
                params.putString(PARAM_HEAD_TYPE, "CNC");
                break;
            case Constants.HEAD_CNC_200W:
                params.putString(PARAM_HEAD_TYPE, "CNC 200W");
                break;
            case Constants.HEAD_UNPLUGGED:
                params.putString(PARAM_HEAD_TYPE, "Unplugged");
                break;
            case Constants.HEAD_FACTORY_3DP:
            case Constants.HEAD_FACTORY_LASER:
            case Constants.HEAD_FACTORY_CNC:
                params.putString(PARAM_HEAD_TYPE, "Factory Tools");
                break;
            default:
                params.putString(PARAM_HEAD_TYPE, "Unknown");
                break;
        }

        firebaseAnalytics.logEvent(EVENT_SCREEN_FINISH_PRINT, params);
    }

    public static void logRemoteFileEvent(FirebaseAnalytics firebaseAnalytics, long fileLength, String suffix) {
        if (firebaseAnalytics == null) return;

        Bundle params = new Bundle();

        final int oneMB = 1024 * 1024;
        String fileLengthLevel = "";
        if (fileLength == 0) {
            fileLengthLevel = "0MB";
        } else if (fileLength < oneMB) {
            fileLengthLevel = "<1MB";
        } else if (fileLength < 20 * oneMB) {
            fileLengthLevel = "1-20MB";
        } else if (fileLength < 50 * oneMB) {
            fileLengthLevel = "20-50MB";
        } else if (fileLength < 100 * oneMB) {
            fileLengthLevel = "50-100MB";
        } else if (fileLength < 200 * oneMB) {
            fileLengthLevel = "100-200MB";
        } else if (fileLength < 400 * oneMB){
            fileLengthLevel = "200-400MB";
        } else {
            fileLengthLevel = ">400MB";
        }

        params.putString(PARAM_REMOTE_RECEIVE_FILE_LENGTH_LEVEL, fileLengthLevel);

        switch (suffix) {
            case "gcode":
            case "nc":
            case "cnc":
                params.putString(PARAM_FILE_TYPE, suffix);
                break;
            default:
                params.putString(PARAM_FILE_TYPE, "Others");
                break;
        }

        firebaseAnalytics.logEvent(EVENT_REMOTE_FILE_RECEIVED, params);
    }
}
