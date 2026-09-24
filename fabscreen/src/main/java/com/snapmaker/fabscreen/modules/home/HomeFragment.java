package com.snapmaker.fabscreen.modules.home;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.welcome.WelcomeActivity;
import com.snapmaker.fabscreen.router.Router;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.data.api.ApiClient;
import fabscreen.libraries.legacy.data.api.VersionResponse;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderModel;
import fabscreen.libraries.legacy.lib.FirebaseHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.network.AccessPoint;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class HomeFragment extends BaseFragment {
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @BindView(R.id.view_home_wifi)
    View mViewWifi;
    @BindView(R.id.tv_home_wifi_ssid)
    TextView mTvWifiSSID;

    @BindView(R.id.view_home_3dp_status)
    View mView3DPStatus;
    @BindView(R.id.view_home_laser_status)
    View mViewLaserStatus;
    @BindView(R.id.view_home_cnc_status)
    View mViewCNCStatus;

    @BindView(R.id.tv_laser_type)
    TextView mTvLaserType;
    @BindView(R.id.view_widget_laser_focus)
    View mViewLaserFocus;

    @BindView(R.id.tv_cnc_type)
    TextView mTvCNCType;
    // Addon
    @BindView(R.id.view_home_3dp_add_on)
    View mViewAddOn3DP;
    @BindView(R.id.view_home_laser_add_on)
    View mViewAddOnLaser;
    @BindView(R.id.view_home_cnc_add_on)
    View mViewAddOnCNC;

    @BindView(R.id.view_home_add_on_enclosure)
    View mViewEnclosureStatus;
    @BindView(R.id.view_home_add_on_rotary)
    View mViewRotaryStatus;
    @BindView(R.id.view_home_add_on_emergency_stop)
    View mViewEmergencyStopStatus;
    @BindView(R.id.view_home_add_on_air_purifier)
    View mViewAirPurifierStatus;
    @BindView(R.id.view_home_add_on_quick_swap_kit)
    View mViewQuickSwapStatus;
    @BindView(R.id.view_home_add_on_bracing_kit)
    View mViewBracingKitStatus;

    @BindView(R.id.tv_home_status_machine_name)
    TextView mTvMachineName;

    @BindView(R.id.iv_home_3dp_status_heated_bed)
    ImageView mIvStatusHeatedBed;
    @BindView(R.id.tv_widget_nozzle_temp_value)
    TextView mTvNozzleTemp;
    @BindView(R.id.tv_widget_heated_bed_temp_value)
    TextView mTvHeatedBedTemp;

    @BindView(R.id.view_home_laser_camera)
    View mViewLaserCamera;
    @BindView(R.id.iv_home_laser_status_camera)
    ImageView mIvStatusCamera;
    @BindView(R.id.tv_widget_laser_focus_value)
    TextView mTvLaserFocus;

    @BindView(R.id.iv_home_enclosure_status)
    ImageView mIvEnclosureStatus;
    @BindView(R.id.iv_home_rotary_status)
    ImageView mIvRotaryStatus;
    @BindView(R.id.iv_home_emergency_stop_status)
    ImageView mIvStatusEmergencyStop;
    @BindView(R.id.iv_home_air_purifier_status)
    ImageView mIvStatusAirPurifier;
    @BindView(R.id.iv_home_quick_swap_status)
    ImageView mIvStatusQuickSwapKit;
    @BindView(R.id.iv_home_bracing_kit_status)
    ImageView mIvStatusBracingKit;

    // dual extruder
    @BindView(R.id.ll_3dp_dual_status)
    View mView3DPDualStatus;
    @BindView(R.id.tv_nozzle_model_l)
    TextView mTvNozzleModelL;
    @BindView(R.id.iv_dual_extruder_l)
    ImageView mIvDualExtruderLStatus;
    @BindView(R.id.tv_nozzle_model_r)
    TextView mTvNozzleModelR;
    @BindView(R.id.iv_dual_extruder_r)
    ImageView mIvDualExtruderRStatus;
    @BindView(R.id.iv_dual_extruder_heated_bed)
    ImageView mIvDualExtruderHeatedBedStatus;
    @BindView(R.id.ll_dual_extruder_enclosure)
    LinearLayout mLlDualExtruderEnclosure;
    @BindView(R.id.ll_dual_extruder_rotary)
    LinearLayout mLlDualExtruderRotary;
    @BindView(R.id.ll_dual_extruder_e_stop)
    LinearLayout mLlDualExtruderEStop;
    @BindView(R.id.ll_dual_extruder_purifier)
    LinearLayout mLlDualExtruderPurifier;
    @BindView(R.id.ll_dual_extruder_quick_swap_kit)
    LinearLayout mLlDualExtruderQuickSwapKit;
    @BindView(R.id.ll_dual_extruder_bracing_kit)
    LinearLayout mLlDualExtruderBracingKit;
    @BindView(R.id.tv_dual_extruder_temp_heated_bed)
    TextView mTvDualExtruderTempHeatedBed;
    @BindView(R.id.tv_dual_extruder_temp_l)
    TextView mTvDualExtruderTempL;
    @BindView(R.id.tv_dual_extruder_temp_r)
    TextView mTvDualExtruderTempR;

    private FabConfirm mModuleConfirmDialog;
    private boolean mVersionUpdateNotificationFlag = true;
    private boolean mIsLaserModuleGuideCompleted = true;
    private FabConfirm mFabConfirm;
    private FabFullScreenDialog mFabHotEndWarnDialog;

    private Disposable mDualExtruderHotEndSub = null;
    private boolean mIsExtendKitCheckOnStartUp = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    public void onResume() {
        super.onResume();

        int headType = getModel().getMachineController().getHeadType();
        byte rotaryStatus = getModel().getMachineController().getRotaryModuleStatus();

        // Factory mode
        if (headType >= Constants.HEAD_FACTORY_3DP && headType <= Constants.HEAD_FACTORY_LASER) {
            Router.getInstance().routeToFactoryActivity().start(getContext());
            return;
        }

        // Emergency stop button triggered.
        if (getModel().getMachineController().isEmergencyStopTriggered()) {
            getModel().onEmergencyStop();
            Router.getInstance().routeToEmergencyStopPage(true).start(getContext());
            return;
        }

        // Setup Machine
        if (headType != Constants.HEAD_UNPLUGGED) {
            if (!getModel().getPreferences().getMachineSetupFlag()) {
                Logger.d("Setting up machine.");
                Router.getInstance().routeWithClass(WelcomeActivity.class).start(getContext());
                return;
            }
        }

        // Machine needs to confirm quick swap is (not) installed before Guide procedure.
        int machineModel = getModel().getMachineController().getMachineModel();
        boolean isQuickSwapModelApplicable = machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A250 || machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A350;
        if (getModel().getPreferences().getExtendKitCheckOnStartUp() && isQuickSwapModelApplicable && !mIsExtendKitCheckOnStartUp) {
            // We only check once in this page.
            // Once the page has already routes, we don't needs to check it until next start up.
            mIsExtendKitCheckOnStartUp = true;
            Router.getInstance().routeToExtendKitSetUpPage().start(requireContext());
            return;
        } else {
            // We assumed app completed the start up check.
            mIsExtendKitCheckOnStartUp = true;
        }

        // Guide to tool head
        switch (headType) {
            case Constants.HEAD_3DP:
                if (!getModel().getPreferences().getMachineSetup3DP()) {
                    Logger.d("Guide to 3DP.");
                    Router.getInstance().routeToGuide3DP().start(getContext());
                }
                break;
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                if (!getModel().getPreferences().getMachineSetup3DPDualExtruder()) {
                    Logger.d("Guide to Dual Extrusion Module 3DP.");
                    Router.getInstance().routeToGuide3DPDualExtruderPage().start(getContext());
                }
                break;
            case Constants.HEAD_LASER:
                if (!getModel().getPreferences().getMachineSetupRotaryLaser() && (rotaryStatus == (byte) 0)) {
                    Logger.d("Guide to Rotary Laser.");
                    Router.getInstance().routeToGuideRotaryLaser().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetupLaser() && (rotaryStatus == (byte) 1)) {
                    Logger.d("Guide to Laser.");
                    Router.getInstance().routeToGuideLaser().start(getContext());
                }
                break;
            case Constants.HEAD_LASER_10W:
                if (!getModel().getPreferences().getMachineSetupRotary10WLaser() && (rotaryStatus == (byte) 0)) {
                    mIsLaserModuleGuideCompleted = false;
                    Logger.d("Guide to Rotary Laser.");
                    Router.getInstance().routeToGuideRotaryLaser().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetup10WLaser() && (rotaryStatus == (byte) 1)) {
                    mIsLaserModuleGuideCompleted = false;
                    Logger.d("Guide to 10W Laser.");
                    Router.getInstance().routeToGuide10WLaser().start(getContext());
                }
                break;
            case Constants.HEAD_LASER_20W:
                if (!getModel().getPreferences().getMachineSetupRotary20WLaser() && (rotaryStatus == (byte) 0)) {
                    Logger.d("Guide to Rotary Laser.");
                    Router.getInstance().routeToGuideRotaryLaser().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetup20WLaser() && (rotaryStatus == (byte) 1)) {
                    Logger.d("Guide to 20W Laser.");
                    Router.getInstance().routeToGuide20WLaser().start(getContext());
                }
                break;
            case Constants.HEAD_LASER_2W_IR:
                if (!getModel().getPreferences().getMachineSetupRotary2WLaser() && (rotaryStatus == (byte) 0)) {
                    Logger.d("Guide to Rotary Laser.");
                    Router.getInstance().routeToGuideRotaryLaser().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetup2WLaser() && (rotaryStatus == (byte) 1)) {
                    Logger.d("Guide to 2W Laser.");
                    Router.getInstance().routeToGuide2WLaser().start(getContext());
                }
                break;
            case Constants.HEAD_LASER_40W:
                if (!getModel().getPreferences().getMachineSetupRotary40WLaser() && (rotaryStatus == (byte) 0)) {
                    Logger.d("Guide to Rotary Laser.");
                    Router.getInstance().routeToGuideRotaryLaser().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetup40WLaser() && (rotaryStatus == (byte) 1)) {
                    Logger.d("Guide to 40W Laser.");
                    Router.getInstance().routeToGuide40WLaser().start(getContext());
                }
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                if (!getModel().getPreferences().getMachineSetupRotaryCNC() && (rotaryStatus == (byte) 0)) {
                    Logger.d("Guide to Rotary CNC.");
                    Router.getInstance().routeToGuideRotaryCNC().start(getContext());
                } else if (!getModel().getPreferences().getMachineSetupCNC() && (rotaryStatus == (byte) 1)) {
                    Logger.d("Guide to CNC.");
                    Router.getInstance().routeToGuideCNC().start(getContext());
                }
                break;
        }

        String machineName = getModel().getPreferences().getMachineName();
        if (!machineName.contentEquals(mTvMachineName.getText())) {
            mTvMachineName.setText(machineName);
        }

        // Check update flag
        if (getModel().getPreferences().getMachineUpdatedFlag()) {
            Logger.i("Upgrade successfully");
            getModel().getPreferences().setMachineUpdatedFlag(false);
//            Router.getInstance().routeToAboutPage().start(getContext());
            String desc = getResources().getString(R.string.home_dialog_update_successful_desc);
            desc += " " + getModel().getPreferences().getLastUpdatePackageVersion();
            FabFullScreenDialog.create(getContext())
                    .setTitle(R.string.home_dialog_update_successful)
                    .setIcon(R.drawable.pic_dialog_success_72x72)
                    .setMessage(desc)
                    .setPositive(R.string.all_complete, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }

        // FIXME: Workaround for dual extrusion
        if (headType == Constants.HEAD_3DP_DUAL_EXTRUDER) {
            updateExtrudersStatus();
        }
        initAddOnStatus();

        if (!mIsLaserModuleGuideCompleted && mFabConfirm != null && mFabConfirm.isShowing()) {
            mFabConfirm.dismiss();
            mFabConfirm = null;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_home;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        // Check Power outage
        getModel().getMachineController().getPowerOutageObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isPowerOutage -> {
                    if (!isPowerOutage) return;
                    if (getModel().getWorkspace().getPrintSource() == Constants.PRINT_SOURCE_INTERNAL_NOT_RECOVERABLE_FILES) {
                        Logger.w("XY Offset Print and Print Check could not recovered for now, clear power outage flag.");
                        getModel().getSlaveComputer().resetErrorFlag()
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(retCode -> {
                                    getModel().getMachineController().clearPowerOutageFlag();
                                    Logger.d("Clear error flags.");
                                }, LogHelper::log);
                    } else {
                        handlePowerOutage();
                    }
                }, LogHelper::log);

        // Network
        getModel().getNetworkController().getActiveNetworkObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoint -> {
                    if (accessPoint != AccessPoint.NULL_ACCESS_POINT) {
                        Logger.d("Wi-Fi connected.");
                        mViewWifi.setVisibility(View.VISIBLE);
                        mTvWifiSSID.setText(accessPoint.getSSID());

                        // check new update if machine is on
                        if (getModel().getPreferences().getCheckUpdateFlag()) {
                            checkNewUpdate();
                            getModel().getPreferences().setCheckUpdateFlag(false);
                        }
                    } else {
                        mViewWifi.setVisibility(View.INVISIBLE);
                    }
                });

        // Machine Name
        String machineName = getModel().getPreferences().getMachineName();
        mTvMachineName.setText(machineName);

        // Head type and status
        int headType = getModel().getMachineController().getHeadType();
        MachineController.WorkType workType = getModel().getMachineController().getWorkType();

        mView3DPDualStatus.setVisibility(headType == Constants.HEAD_3DP_DUAL_EXTRUDER ? View.VISIBLE : View.GONE);
        mView3DPStatus.setVisibility(headType == Constants.HEAD_3DP ? View.VISIBLE : View.GONE);
        mViewLaserStatus.setVisibility(workType == MachineController.WorkType.LASER ? View.VISIBLE : View.GONE);
        mViewCNCStatus.setVisibility((headType == Constants.HEAD_CNC || headType == Constants.HEAD_CNC_200W )? View.VISIBLE : View.GONE);

        mViewAddOn3DP.setVisibility(headType == Constants.HEAD_3DP ? View.VISIBLE : View.GONE);
        mViewAddOnLaser.setVisibility(workType == MachineController.WorkType.LASER ? View.VISIBLE : View.GONE);
        mViewAddOnCNC.setVisibility(workType == MachineController.WorkType.CNC ? View.VISIBLE : View.GONE);

        if (headType == Constants.HEAD_LASER_10W) {
            getModel().getPrintController().getHeaderSecurityStatus().as(bindToLifecycle()).subscribe(success -> {
            }, LogHelper::log);
        }
        switch (headType) {
            case Constants.HEAD_3DP: {
                mViewEnclosureStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOn3DP.findViewById(R.id.view_home_add_on_bracing_kit);
                init3DPStatus();
                break;
            }
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                mViewEnclosureStatus = mLlDualExtruderEnclosure;
                mViewRotaryStatus = mLlDualExtruderRotary;
                mViewEmergencyStopStatus = mLlDualExtruderEStop;
                mViewAirPurifierStatus = mLlDualExtruderPurifier;
                mViewQuickSwapStatus = mLlDualExtruderQuickSwapKit;
                mViewBracingKitStatus = mLlDualExtruderBracingKit;
                initExtruderModel();
                init3DPDualStatus();
                break;
            case Constants.HEAD_LASER:
                mTvLaserType.setText(R.string.all_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                mViewLaserFocus.setVisibility(View.GONE);
                initLaserStatus();
                initCameraStatus();
                break;
            case Constants.HEAD_LASER_10W: {
                mTvLaserType.setText(R.string.all_10w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                mViewLaserFocus.setVisibility(View.GONE);
                initLaserStatus();
                initCameraStatus();
                break;
            }
            case Constants.HEAD_LASER_20W:
                mTvLaserType.setText(R.string.all_20w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                initLaserStatus();
                mViewLaserCamera.setVisibility(View.GONE);
                mViewLaserFocus.setVisibility(View.GONE);
                break;
            case Constants.HEAD_LASER_40W:
                mTvLaserType.setText(R.string.all_40w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                initLaserStatus();
                mViewLaserCamera.setVisibility(View.GONE);
                mViewLaserFocus.setVisibility(View.GONE);
                break;
            case Constants.HEAD_LASER_2W_IR:
                mTvLaserType.setText(R.string.all_2w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                initLaserStatus();
                mViewLaserCamera.setVisibility(View.GONE);
                mViewLaserFocus.setVisibility(View.GONE);
                break;
            case Constants.HEAD_CNC: {
                mTvCNCType.setText(R.string.all_cnc);
                mViewEnclosureStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_bracing_kit);
                break;
            }
            case Constants.HEAD_CNC_200W: {
                mTvCNCType.setText(R.string.all_cnc_200w);
                mViewEnclosureStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnCNC.findViewById(R.id.view_home_add_on_bracing_kit);
                break;
            }
        }

        // Log start up event on Firebase
        int machineModel = getModel().getMachineController().getMachineModel();
        FirebaseHelper.logStartUpEvent(getFirebaseAnalytics(), headType, machineModel);

        getModel().getMachineController().getOutdatedVersionModuleListObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(outdatedModules -> {
                    if (outdatedModules.isEmpty()) return;

                    // build outdated module list string for notification.
                    StringBuilder moduleNames = new StringBuilder();
                    for (int i = 0; i < outdatedModules.size(); i++) {
                        moduleNames.append(outdatedModules.get(i));
                        if (i != outdatedModules.size() - 1) {
                            moduleNames.append(", ");
                        }
                    }
                    String updateFileFolder = getModel().getDataDir().getAbsolutePath() + File.separatorChar + "update";
                    File file = new File(updateFileFolder, "update.bin");
                    boolean isFileExists = file.exists();

                    if (mVersionUpdateNotificationFlag) {
                        if (mModuleConfirmDialog != null && mModuleConfirmDialog.isShowing()) {
                            mModuleConfirmDialog.dismiss();
                            mModuleConfirmDialog = null;
                        }
                        final int dialogDescRes = isFileExists ? R.string.dialog_warning_outdated_module_version_detected_update
                                : R.string.dialog_warning_outdated_module_version_detected_notifiy;

                        mModuleConfirmDialog = FabConfirm.create(getContext())
                                .setCanceledOnTouchOutSide(false)
                                .setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setDescription(getResources().getString(dialogDescRes, moduleNames.toString()))
                                .setConfirm(isFileExists ? R.string.all_update : R.string.guide_got_it, (dialog, position) -> {
                                    mVersionUpdateNotificationFlag = false;
                                    if (isFileExists) {
                                        Router.getInstance().routeToUpdateActivity(file.getAbsolutePath(), true).start(getContext());
                                    }
                                    dialog.dismiss();
                                });
                        mModuleConfirmDialog.show();
                    }
                }, LogHelper::log);

        initAddOnStatus();

        if (headType != Constants.HEAD_LASER_10W) {
            // -1 is the default value, indicating that no calibration is performed
            getModel().getPreferences().setHeaderOnlineSyncID(-1);
        } else {
            // If the main control version does not support or has no reply, change the local value to the default value
            getModel().getSlaveComputer().requestHeaderOnlineSyncId(2000)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(headId -> {
                        int i = getModel().getPreferences().getHeaderOnlineSyncID();
                        return Observable.just(headId != i || i == -1);
                    })
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        if (success) {
                            getModel().getPreferences().setHeaderOnlineSyncID(-1);
                            if (mFabConfirm != null && mFabConfirm.isShowing()) {
                                mFabConfirm.dismiss();
                            }

                            // FIXME: Temporary fix, dialog should not be shown when rotary was plugged.
                            if (getModel().getMachineController().getRotaryModuleStatus() != 1)
                                return;

                            mFabConfirm = FabConfirm.create(getContext())
                                    .setDescription(R.string.laser_10W_dialog_changer_header)
                                    .setCancel(R.string.all_cancel, (dialog, position) -> dialog.dismiss())
                                    .setConfirm(R.string.all_settings, (dialog, which) -> {
                                        Router.getInstance().routeToSettingsPage().start(getContext());
                                        dialog.dismiss();
                                    });
                            mFabConfirm.show();
                        }
                    }, e -> {
                        getModel().getPreferences().setHeaderOnlineSyncID(-1);
                        LogHelper.log(e);
                    });
        }
    }

    private void initExtruderModel() {
        getModel().getSlaveComputer().getExtruderModel()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(models -> {
                    ExtruderModel modelL = models.get(0);
                    ExtruderModel modelR = models.get(1);
                    mTvNozzleModelL.setText("L: " + modelL.getName() + " " + modelL.caliber);
                    mTvNozzleModelR.setText("R: " + modelR.getName() + " " + modelR.caliber);
                }, LogHelper::log);
    }

    private void initAddOnStatus() {
        final boolean isEnclosureReady = getModel().getMachineController().isEnclosureReady();
        final boolean isEmergencyAvailable = getModel().getMachineController().isEmergencyStopAvailable();

        mViewEnclosureStatus.setVisibility(isEnclosureReady ? View.VISIBLE : View.GONE);
        mIvEnclosureStatus = mViewEnclosureStatus.findViewById(R.id.iv_home_enclosure_status);
        mIvEnclosureStatus.setBackgroundResource(isEnclosureReady ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);

        mIvRotaryStatus = mViewRotaryStatus.findViewById(R.id.iv_home_rotary_status);
        getModel().getMachineController().getRotaryStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    mViewRotaryStatus.setVisibility((status != (byte) 1) ? View.VISIBLE : View.GONE);
                    mIvRotaryStatus.setBackgroundResource((status == (byte) 0) ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                }, LogHelper::log);

        mViewEmergencyStopStatus.setVisibility(isEmergencyAvailable ? View.VISIBLE : View.GONE);
        mIvStatusEmergencyStop = mViewEmergencyStopStatus.findViewById(R.id.iv_home_emergency_stop_status);
        mIvStatusEmergencyStop.setBackgroundResource(isEmergencyAvailable ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);

        // Air Purifier Status
        mIvStatusAirPurifier = mViewAirPurifierStatus.findViewById(R.id.iv_home_air_purifier_status);
        final boolean isAirPurifierPlugged = getModel().getMachineController().isAirPurifierPlugged();
        mViewAirPurifierStatus.setVisibility(isAirPurifierPlugged ? View.VISIBLE : View.GONE);

        getModel().getMachineController().getAirPurifierStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(airPurifierStatus -> {
                    switch (airPurifierStatus.status) {
                        case (byte) 0:
                            mIvStatusAirPurifier.setBackgroundResource(R.drawable.ic_home_status_normal_10x10);
                            break;
                        case (byte) 1:
                        case (byte) 2:
                        case (byte) 3:
                        default: {
                            mIvStatusAirPurifier.setBackgroundResource(R.drawable.ic_home_status_abnormal_10x10);
                            break;
                        }
                    }
                }, LogHelper::log);

        mIvStatusQuickSwapKit = mViewQuickSwapStatus.findViewById(R.id.iv_home_quick_swap_status);
        final boolean isQuickSwapKitInstalled = getModel().getMachineController().getExtendKitInfo().isQuickSwapInstalled();
        final int machineModel = getModel().getMachineController().getMachineModel();
        // According to the definition of SM2.0 Quick Swap Kit, A150 model is not supported.
        if (isQuickSwapKitInstalled && (machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A350 || machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A250)) {
            mViewQuickSwapStatus.setVisibility(View.VISIBLE);
        } else {
            mViewQuickSwapStatus.setVisibility(View.GONE);
        }

        mIvStatusBracingKit = mViewBracingKitStatus.findViewById(R.id.iv_home_bracing_kit_status);
        final boolean isBracingKitInstalled = getModel().getMachineController().getExtendKitInfo().isBracingKitInstalled();
        // According to the definition of SM2.0 Quick Swap Kit, A150 model is not supported.
        if (isBracingKitInstalled && (machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A350 || machineModel == Constants.MACHINE_MODEL_SNAPMAKER_A250)) {
            mViewBracingKitStatus.setVisibility(View.VISIBLE);
        } else {
            mViewBracingKitStatus.setVisibility(View.GONE);
        }

    }

    private void init3DPStatus() {
        getModel().getSlaveComputer().getMachineStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    double nozzleTemp = machineStatus.headTemperature;
                    mTvNozzleTemp.setText(String.format(Locale.getDefault(), "%.0f °C", nozzleTemp));

                    double heatedBedTemp = machineStatus.bedTemperature;
                    mTvHeatedBedTemp.setText(String.format(Locale.getDefault(), "%.0f °C", heatedBedTemp));
                    mIvStatusHeatedBed.setBackgroundResource(heatedBedTemp > 0 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                });
    }

    private void init3DPDualStatus() {
        getModel().getSlaveComputer().getMachineStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    double nozzleTempL = machineStatus.headTemperature;
                    double nozzleTempR = machineStatus.extruder1Temperature;

                    mTvDualExtruderTempL.setText(String.format(Locale.getDefault(), "L: %.0f °C", nozzleTempL));
                    mTvDualExtruderTempR.setText(String.format(Locale.getDefault(), "R: %.0f °C", nozzleTempR));

                    double heatedBedTemp = machineStatus.bedTemperature;
                    mTvDualExtruderTempHeatedBed.setText(String.format(Locale.getDefault(), "%.0f °C", heatedBedTemp));
                    mIvDualExtruderHeatedBedStatus.setImageResource(heatedBedTemp > 0 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                });
    }

    private void updateExtrudersStatus() {
        if (getModel().getMachineController().isDualExtruderTemperatureExceed()) {
            if (mFabHotEndWarnDialog != null && mFabHotEndWarnDialog.isShowing()) {
                return;
            } else {
                mFabHotEndWarnDialog = FabFullScreenDialog.create(getContext())
                        .setIcon(fabscreen.libraries.legacy.R.drawable.pic_dialog_warning_72x72)
                        .setTitle(R.string.dialog_dual_extruder_hotend_abnormal_title)
                        .setMessage(R.string.dialog_dual_extruder_hotend_abnormal_content)
                        .setPositive(fabscreen.libraries.legacy.R.string.all_ok, (dialog, which) -> {
                            dialog.dismiss();
                            mFabHotEndWarnDialog = null;
                        });
                mFabHotEndWarnDialog.show();

                FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
                mIvDualExtruderLStatus.setImageResource(status.headTemperature < 310 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                mIvDualExtruderRStatus.setImageResource(status.extruder1Temperature < 310 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
            }
        } else {
            if (mDualExtruderHotEndSub != null && !mDualExtruderHotEndSub.isDisposed()) {
                mDualExtruderHotEndSub.dispose();
                mDualExtruderHotEndSub = null;
            }

            mDualExtruderHotEndSub = getModel().getMachineController().getNozzleTemperatureExceedObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isTempExceed -> {
                        if (isTempExceed) {
                            FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
                            mIvDualExtruderLStatus.setImageResource(status.headTemperature < 310 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                            mIvDualExtruderRStatus.setImageResource(status.extruder1Temperature < 310 ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);

                        }
                    }, LogHelper::log);
        }

     }

    private void initLaserStatus() {
        getModel().getMachineController().getLaserFocusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(laserFocus -> {
                    mTvLaserFocus.setText(String.format(Locale.getDefault(), "%.1f mm", laserFocus));
                });
    }

    private void initCameraStatus() {
        getModel().getLaserCameraController().getBluetoothConnectedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(connected -> {
                    mIvStatusCamera.setBackgroundResource(connected ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                });
    }

    /**
     * Handle print power outage situation. This happens every time we connect/re-connect to
     * the machine.
     */
    private void handlePowerOutage() {
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_warning_power_outage_120x120)
                .setTitle(R.string.print_warning_power_outage_title)
                .setMessage(getString(R.string.print_warning_power_outage_content))
                .setPositive(R.string.all_resume, (dialog, which) -> {
                    if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
                        getModel().getPrintController().getHeaderSecurityStatus()
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(headerSecurity -> {
                                    if (headerSecurity.status == 0) {
                                        dialog.dismiss();
                                        // Init last print file.
                                        getModel().getWorkspace().initLastPrintFile();

                                        // To print page
                                        getModel().getPrintController().setPowerOutageFlag(true);
                                        Router.getInstance()
                                                .routeToPrintPage()
                                                .start(getContext());
                                    }
                                }, LogHelper::log);
                    } else {
                        dialog.dismiss();
                        // Init last print file.
                        getModel().getWorkspace().initLastPrintFile();

                        // To print page
                        getModel().getPrintController().setPowerOutageFlag(true);
                        Router.getInstance()
                                .routeToPrintPage()
                                .start(getContext());
                    }
                })
                .setNegative(R.string.all_cancel, (dialog, which) -> {
                    Logger.i("Power outage recover canceled.");
                    dialog.dismiss();
                    getModel().getSlaveComputer().resetErrorFlag()
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(retCode -> {
                                getModel().getMachineController().clearPowerOutageFlag();
                                Logger.i("Clear error flags.");
                            }, LogHelper::log);
                })
                .show();
    }

    private void checkNewUpdate() {
        Logger.d("Start checking new version…");
        ApiClient apiClient = new ApiClient(getModel().getPreferences().getApiHost());
        apiClient.getLatestVersion()
                .as(bindToLifecycle())
                .subscribe(versionResponse -> {
                    String newVersion = versionResponse.data.new_version.version;
                    saveVersionResponse(versionResponse.data.new_version);
                    // Check version here. Notification only show once if new version is available.
                    if (!newVersion.equals(getModel().getPreferences().getLastUpdatePackageVersion())
                            && !newVersion.equals(getModel().getPreferences().getLastCheckVersion())) {
                        Logger.d("New firmware available " + versionResponse.data.new_version.version);
                        getModel().getPreferences().setUpdateNotification(true);
                    }
                }, e -> {
                    if (e instanceof UnknownHostException) {
                        Logger.w("Unable to resolve API server, please check your network connectivity.");
                    } else if (e instanceof SocketTimeoutException) {
                        Logger.w("Socket timeout, please check your network is available.");
                        // AndroidSchedulers.mainThread().scheduleDirect(this::showCheckFailDialog, 1000, TimeUnit.MILLISECONDS);
                    } else if (e instanceof SSLHandshakeException) {
                        Logger.w("SSL Handshake failed due to unacceptable certificate. Please check the local machine time was synchronized by ntp.");
                    } else {
                        LogHelper.log(e);
                    }
                });
    }

    private void saveVersionResponse(VersionResponse.NewVersionData response) {
        File file = new File(getModel().getCacheDir(), "version_check.json");
        FileOutputStream fos;
        String content = new Gson().toJson(response);

        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    return;
                }
            }

            fos = new FileOutputStream(file);
            fos.write(content.getBytes(), 0, content.getBytes().length);

            fos.flush();
            fos.close();
        } catch (IOException e) {
            LogHelper.log(e);
        }
    }

    @OnClick(R.id.btn_home_start)
    void onClickStart() {
        Router.getInstance().routeToFilesPage().start(getContext());
    }

}
