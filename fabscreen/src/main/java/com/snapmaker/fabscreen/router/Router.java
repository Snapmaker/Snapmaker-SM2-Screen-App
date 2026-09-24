package com.snapmaker.fabscreen.router;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.modules.about.AboutActivity;
import com.snapmaker.fabscreen.modules.browse.BrowseActivity;
import com.snapmaker.fabscreen.modules.calibration.CalibrationActivity;
import com.snapmaker.fabscreen.modules.cncbitassistant.CNCBitAssistantActivity;
import com.snapmaker.fabscreen.modules.cncoriginassistant.CNCOriginAssistantActivity;
import com.snapmaker.fabscreen.modules.control.ControlActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.DualExtruderBedLevellingActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.entries.DualExtruderCalibrationEntriesActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.sensor.DualExtruderSensorCalibrationActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration.DualExtruderXYCalibrationActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.zheight.DualExtruderZHeightCalibrationActivity;
import com.snapmaker.fabscreen.modules.emergencystop.EmergencyStopActivity;
import com.snapmaker.fabscreen.modules.factory.FactoryToolsActivity;
import com.snapmaker.fabscreen.modules.guide10wlaser.Guide10wLaserActivity;
import com.snapmaker.fabscreen.modules.guide20wlaser.Guide20wLaserActivity;
import com.snapmaker.fabscreen.modules.guide2wlaser.Guide2wLaserActivity;
import com.snapmaker.fabscreen.modules.guide3dp.Guide3DPActivity;
import com.snapmaker.fabscreen.modules.guide40wlaser.Guide40wLaserActivity;
import com.snapmaker.fabscreen.modules.guidecnc.GuideCNCActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.Guide3DPDualExtruderActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.complete.Guide3DPDualExtruderCompleteActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.printcheck.Guide3DPDualExtruderPrintCheckActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.GuideDualExtruderVerticalLevellingActivity;
import com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration.GuideDualExtruderXYCalibrationActivity;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;
import com.snapmaker.fabscreen.modules.guiderotary.cnc.GuideRotaryCNCActivity;
import com.snapmaker.fabscreen.modules.guiderotary.laser.GuideRotaryLaserActivity;
import com.snapmaker.fabscreen.modules.home.HomeActivity;
import com.snapmaker.fabscreen.modules.laser10wcamera.Laser10wCameraCalibrationActivity;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.loadfilament.DualExtruderLoadFilamentActivity;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.modules.print.PrintActivity;
import com.snapmaker.fabscreen.modules.printsettings.PrintSettingsActivity;
import com.snapmaker.fabscreen.modules.remote.RemoteActivity;
import com.snapmaker.fabscreen.modules.settings.SettingsActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessMeasureCalibrationActivity;
import com.snapmaker.fabscreen.modules.settings.firmware.SettingsFirmwareActivity;
import com.snapmaker.fabscreen.modules.settings.extendkit.SettingsExtendKitActivity;
import com.snapmaker.fabscreen.modules.settings.extendkit.SettingsQuickSwapParkingPositionActivity;
import com.snapmaker.fabscreen.modules.update.UpdateActivity;

public class Router {

    public static class IntentKeys {
        public static final String IS_LOCAL = "is_local";
        public static final String FILE_PATH = "file_path";
        public static final String NEED_BACK_HOME = "need_back_home";
        public static final String FORCE_REFRESH = "force_refresh";
    }

    private static volatile Router sInstance;
    private Bundle mBundle;
    private Class mClass;

    private Router() {
        mBundle = new Bundle();
    }

    public static Router getInstance() {
        if (sInstance == null) {
            synchronized (Router.class) {
                if (sInstance == null) {
                    sInstance = new Router();
                }
            }
        }
        return sInstance;
    }

    public Router routeWithClass(Class activity) {
        mClass = activity;
        return this;
    }

    public Router routeToHomeActivity() {
        mClass = HomeActivity.class;
        return this;
    }

    public Router routeToGuide3DP() {
        mClass = Guide3DPActivity.class;
        return this;
    }

    public Router routeToGuideLaser() {
        mClass = GuideLaserActivity.class;
        return this;
    }

    public Router routeToGuideCNC() {
        mClass = GuideCNCActivity.class;
        return this;
    }

    public Router routeToGuideRotaryLaser() {
        mClass = GuideRotaryLaserActivity.class;
        return this;
    }

    public Router routeToGuideRotaryCNC() {
        mClass = GuideRotaryCNCActivity.class;
        return this;
    }

    public Router routeToControlPage() {
        mClass = ControlActivity.class;
        return this;
    }

    public Router routeToFilesPage() {
        mClass = BrowseActivity.class;
        return this;
    }

    public Router routeToPreviewPage(boolean isLocal, String filePath) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IntentKeys.IS_LOCAL, isLocal);
        bundle.putString(IntentKeys.FILE_PATH, filePath);
        mBundle = bundle;
        mClass = PreviewActivity.class;
        return this;
    }

    public Router routeToPrintSettingsPage() {
        mClass = PrintSettingsActivity.class;
        return this;
    }

    public Router routeToPrintPage() {
        mClass = PrintActivity.class;
        return this;
    }

    public Router routeToPrintPage(boolean forceRefresh) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IntentKeys.FORCE_REFRESH, forceRefresh);
        mBundle = bundle;
        mClass = PrintActivity.class;
        return this;
    }

    /**
     * Compose params for going to CalibrationActivity.
     *
     * @param needBackHome Whether we need to go back home(or directly finish) after diving into
     *                     CalibrationActivity.
     */
    public Router routeToCalibrationPage(boolean needBackHome) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IntentKeys.NEED_BACK_HOME, needBackHome);
        mBundle = bundle;
        mClass = CalibrationActivity.class;
        return this;
    }

    public Router routeToDualExtruderCalibration() {
        mClass = DualExtruderCalibrationEntriesActivity.class;
        return this;
    }

    public Router routeToDualExtruderBedLeveling() {
        mClass = DualExtruderBedLevellingActivity.class;
        return this;
    }

    public Router routeToDualExtruderZHeightCalibration() {
        mClass = DualExtruderZHeightCalibrationActivity.class;
        return this;
    }

    public Router routeToDualExtruderXYCalibration() {
        mClass = DualExtruderXYCalibrationActivity.class;
        return this;
    }

    public Router routeToDualExtruderSensorCalibration() {
        mClass = DualExtruderSensorCalibrationActivity.class;
        return this;
    }

    public Router routeToDualExtruderPrintCheck() {
        mClass = CalibrationDualExtruderPrintCheckActivity.class;
        return this;
    }

    public Router routeToLaserCalibrationPage() {
        mClass = LaserCalibrationActivity.class;
        return this;
    }

    public Router routeTo10wThicknessCalibrationPage() {
        mClass = Laser10wThicknessMeasureCalibrationActivity.class;
        return this;
    }

    public Router routeToCameraCalibration() {
        mClass = Laser10wCameraCalibrationActivity.class;
        return this;
    }

    public Router routeToCNCOriginAssistantPage() {
        mClass = CNCOriginAssistantActivity.class;
        return this;
    }

    public Router routeToCNCBitAssistantPage() {
        mClass = CNCBitAssistantActivity.class;
        return this;
    }

    public Router routeToSettingsPage() {
        mClass = SettingsActivity.class;
        return this;
    }

    public Router routeToAboutPage() {
        mClass = AboutActivity.class;
        return this;
    }

    public Router routeToSettingsFirmwarePage() {
        mClass = SettingsFirmwareActivity.class;
        return this;
    }

    public Router routeToRemotePage() {
        mClass = RemoteActivity.class;
        return this;
    }

    public Router routeToEmergencyStopPage(boolean isTriggeredOnPowerUp) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("is_triggered_on_power_up", isTriggeredOnPowerUp);
        mBundle = bundle;
        mClass = EmergencyStopActivity.class;
        return this;
    }

    public Router routeToFactoryActivity() {
        mClass = FactoryToolsActivity.class;
        return this;
    }

    public Router routeToGuide10WLaser() {
        mClass = Guide10wLaserActivity.class;
        return this;
    }

    public Router routeToGuide20WLaser() {
        mClass = Guide20wLaserActivity.class;
        return this;
    }

    public Router routeToGuide40WLaser() {
        mClass = Guide40wLaserActivity.class;
        return this;
    }

    public Router routeToGuide2WLaser() {
        mClass = Guide2wLaserActivity.class;
        return this;
    }

    public Router routeToGuide3DPDualExtruderPage() {
        mClass = Guide3DPDualExtruderActivity.class;
        return this;
    }
    
    public Router routeToGuide3DPDualExtruderVerticalLevelingPage() {
        mClass = GuideDualExtruderVerticalLevellingActivity.class;
        return this;
    }

    public Router routeToGuide3DPDualExtruderXYCalibrationPage() {
        mClass = GuideDualExtruderXYCalibrationActivity.class;
        return this;
    }
    
    public Router routeToGuide3DPDualExtruderPrintCheckPage() {
        mClass = Guide3DPDualExtruderPrintCheckActivity.class;
        return this;
    }

    public Router routeToGuide3DPDualExtruderCompletePage() {
        mClass = Guide3DPDualExtruderCompleteActivity.class;
        return this;
    }

    // For testing
    public Router routeToLoadFilamentActivity() {
        mClass = DualExtruderLoadFilamentActivity.class;
        return this;
    }

    public Router routeToUpdateActivity(String filePath, Boolean isLocal) {
        Bundle bundle = new Bundle();
        bundle.putString(IntentKeys.FILE_PATH, filePath);
        bundle.putBoolean(IntentKeys.IS_LOCAL, isLocal);
        mBundle = bundle;
        mClass = UpdateActivity.class;
        return this;
    }

    public Router routeToExtendKitSetUpPage() {
        mClass = SettingsExtendKitActivity.class;
        return this;
    }

    public Router routeToQuickSwapParkingPosition() {
        mClass = SettingsQuickSwapParkingPositionActivity.class;
        return this;
    }

    public void startUpdatingApp(Context context) {
        if (context != null) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.snapmaker.updating");
            context.startActivity(intent);
        }
    }

    public void start(Context context) {
        start(context, 0);
    }

    public void start(Context context, int flag) {
        if (context == null) return;
        Intent intent = composeIntent(context, flag);
        context.startActivity(intent);
    }

    public void start(Context context, Intent intent) {
        if (context == null) return;
        if (!mBundle.isEmpty()) {
            intent.putExtras(mBundle);
        }
        context.startActivity(intent);
    }

    public void startForResult(Fragment fragment, int requestCode) {
        if (fragment == null) return;
        fragment.startActivityForResult(composeIntent(fragment.requireContext(), 0), requestCode);
    }

    @NonNull
    private Intent composeIntent(Context context, int flag) {
        Intent intent = new Intent(context, mClass);
        if (!mBundle.isEmpty()) {
            intent.putExtras(mBundle);
        }
        if (flag != 0) {
            intent.setFlags(flag);
        }
        return intent;
    }
}
