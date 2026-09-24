package com.snapmaker.fabscreen.modules.remote;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.alibaba.android.arouter.launcher.ARouter;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.MachineController;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.data.remote.SessionManager;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.data.serial.fabpacket.content.ExtruderModel;
import fabscreen.libraries.legacy.lib.DateHelper;
import fabscreen.libraries.legacy.lib.FirebaseHelper;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.network.AccessPoint;
import fabscreen.libraries.legacy.route.RoutePath;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import fabscreen.libraries.legacy.view.FabLoading;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

import static fabscreen.libraries.legacy.data.remote.SessionManager.NULL_SESSION;

public class RemoteHomeFragment extends BaseFragment {
    public static RemoteHomeFragment getInstance() {
        return new RemoteHomeFragment();
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

    @BindView(R.id.view_home_3dp_add_on)
    View mViewAddOn3DP;
    @BindView(R.id.view_home_laser_add_on)
    View mViewAddOnLaser;
    @BindView(R.id.view_home_cnc_add_on)
    View mViewAddOnCNC;

    @BindView(R.id.tv_home_status_machine_name)
    TextView mTvMachineName;

    @BindView(R.id.iv_home_3dp_status_heated_bed)
    ImageView mIvStatusHeatedBed;
    @BindView(R.id.tv_widget_nozzle_temp_value)
    TextView mTvNozzleTemp;
    @BindView(R.id.tv_widget_heated_bed_temp_value)
    TextView mTvHeatedBedTemp;

    @BindView(R.id.iv_home_laser_status_camera)
    ImageView mIvStatusCamera;
    @BindView(R.id.tv_widget_laser_focus_value)
    TextView mTvLaserFocus;
    @BindView(R.id.view_home_laser_camera)
    View mViewLaserCamera;

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

    @BindView(R.id.btn_remote_select_last_received_file)
    Button mBtnLastReceivedFile;
    @BindView(R.id.btn_remote_home_disconnect)
    Button mBtnDisconnect;

    private boolean mIsRemotePrintNotFinish = false;
    private FabConfirm mPrintNotFinishDialog = null;
    private FabConfirm mFileTransferCompletedDialog = null;
    private FabLoading mFabLoading;

    private BehaviorSubject<File> mLastReceivedFileSubject = BehaviorSubject.create();
    private BehaviorSubject<MachinePrintJobState> mMachinePrintStateSubject = BehaviorSubject.create();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
        initialize();
        getModel().getRemoteController().setRemotePageFlag(true);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_remote_home;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    public void onDestroy() {
        getModel().getRemoteController().setRemotePageFlag(false);
        super.onDestroy();
    }

    private void initView() {
        // Network
        getModel().getNetworkController().getActiveNetworkObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoint -> {
                    if (accessPoint != AccessPoint.NULL_ACCESS_POINT) {
                        mViewWifi.setVisibility(View.VISIBLE);
                        mTvWifiSSID.setText(accessPoint.getSSID());
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

        mView3DPStatus.setVisibility(headType == Constants.HEAD_3DP ? View.VISIBLE : View.GONE);

        int laserVisibility = headType == Constants.HEAD_LASER || headType == Constants.HEAD_LASER_10W ? View.VISIBLE : View.GONE;

        mViewLaserStatus.setVisibility(workType == MachineController.WorkType.LASER ? View.VISIBLE : View.GONE);
        mViewCNCStatus.setVisibility((headType == Constants.HEAD_CNC || headType == Constants.HEAD_CNC_200W) ? View.VISIBLE : View.GONE);
        mTvLaserType.setText(headType == Constants.HEAD_LASER_10W ? R.string.all_10w_laser : R.string.all_laser);

        mView3DPDualStatus.setVisibility(headType == Constants.HEAD_3DP_DUAL_EXTRUDER ? View.VISIBLE : View.GONE);


        mViewAddOn3DP.setVisibility(headType == Constants.HEAD_3DP ? View.VISIBLE : View.GONE);
        mViewAddOnLaser.setVisibility(workType == MachineController.WorkType.LASER ? View.VISIBLE : View.GONE);
        mViewAddOnCNC.setVisibility((headType == Constants.HEAD_CNC || headType == Constants.HEAD_CNC_200W)? View.VISIBLE : View.GONE);

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
            case Constants.HEAD_LASER_10W: {
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                mViewLaserFocus.setVisibility(View.GONE);
                initLaserStatus();
                updateLaserStatus();
                break;
            }
            case Constants.HEAD_LASER_20W: {
                mTvLaserType.setText(R.string.all_20w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                updateLaserStatus();
                mViewLaserCamera.setVisibility(View.GONE);
                mViewLaserFocus.setVisibility(View.GONE);
                break;
            }
            case Constants.HEAD_LASER_40W: {
                mTvLaserType.setText(R.string.all_40w_laser);
                mViewEnclosureStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_enclosure);
                mViewRotaryStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_rotary);
                mViewEmergencyStopStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_emergency_stop);
                mViewAirPurifierStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_air_purifier);
                mViewQuickSwapStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_quick_swap_kit);
                mViewBracingKitStatus = mViewAddOnLaser.findViewById(R.id.view_home_add_on_bracing_kit);
                updateLaserStatus();
                mViewLaserCamera.setVisibility(View.GONE);
                mViewLaserFocus.setVisibility(View.GONE);
                break;
            }
            case Constants.HEAD_LASER_2W_IR: {
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
            }
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

        mLastReceivedFileSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(lastFile -> {
                    Logger.d("Last recv!");
                    if (lastFile == null) return;

                    Logger.d("last file %s, path %s", lastFile.getName(), lastFile.getAbsolutePath());

                    boolean isLastFileAvailable = lastFile.exists() && !lastFile.isDirectory();
                    if (!isLastFileAvailable) return;

                    String filename = lastFile.getName();
                    mBtnLastReceivedFile.setText(filename);
                    mBtnLastReceivedFile.setVisibility(Button.VISIBLE);
                });

        initAddOnStatus();

        // Confirmation
        getModel().getRemoteController().getCurrentSessionObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(auth -> checkCurrentSession());

        // Workaround here.
        // Cause we don't have pages to manage remote printing that remote were already disconnected,
        // remote home page will pop up confirm dialog when disconnecting from remote printing(either active disconnect or timed out from interval heartbeat).
//        Observable.combineLatest(getModel().getRemoteController().getCurrentSessionObservable(),
//                getModel().getRemoteController().getRemotePrintStateObservable(), (auth, state) -> {
//                    boolean isShowDialog = false;
//                    if (auth == NULL_SESSION) {
//                        switch (state) {
//                            case PrintController.STATE_IDLE:
//                            case PrintController.STATE_COMPLETED: {
//                                isShowDialog = false;
//                                break;
//                            }
//                            case PrintController.STATE_PRINTING:
//                            case PrintController.STATE_PAUSED: {
//                                isShowDialog = true;
//                                break;
//                            }
//                            default:
//                                isShowDialog = false;
//                        }
//                    }
//                    return isShowDialog;
//                })
//                .observeOn(AndroidSchedulers.mainThread())
//                .as(bindToLifecycle())
//                .subscribe(showDialog -> {
//                    if (showDialog) {
//                        showUpRemotePrintNotFinishDialog();
//                    } else {
//                        dismissRemotePrintNotFinishDialog();
//                        // If no connection is ever maintain, then exit page.
//                        if (getModel().getRemoteController().getCurrentSession() == NULL_SESSION) {
//                            Logger.d("Session closed, exiting...");
//                            back();
//                        }
//                    }
//                });

        // Watch file transit events
        getModel().getHTTPEventBus().watchReceiveFileEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(file -> {
                    Logger.d("File transfer via Wi-Fi is done.");
                    MachineController.WorkType machineWorkType = getModel().getMachineController().getWorkType();
                    boolean isFileTypeMatch = false;

                    String suffix = file.getName()
                            .substring(file.getName().lastIndexOf(".") + 1)
                            .toLowerCase();
                    suffix = file.isDirectory() ? "" : suffix;
                    switch (suffix) {
                        case "gcode":
                            isFileTypeMatch = machineWorkType.equals(MachineController.WorkType.FDM);
                            break;
                        case "nc":
                            isFileTypeMatch = machineWorkType.equals(MachineController.WorkType.LASER);
                            break;
                        case "cnc":
                            isFileTypeMatch = machineWorkType.equals(MachineController.WorkType.CNC);
                            break;
                        case "bin":
                        default:
                            break;
                    }
                    boolean isCurrentPrinting = MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue());

                    if (!isCurrentPrinting && isFileTypeMatch) {
                        mLastReceivedFileSubject.onNext(file);
                    }
                    // Log Firebase event
                    LogFirebaseReceiveFileEvent(file);

                    // If last confirm dialog exists, then dismiss before new dialog created.
                    if (mFileTransferCompletedDialog != null) {
                        dismissFileTransferCompletedDialog();
                    }
                    // Show up file transfer complete dialog.
                    mFileTransferCompletedDialog = FabConfirm.create(getContext())
                            .setIcon(R.drawable.pic_dialog_success_72x72)
                            .setDescription(R.string.dialog_file_transfer_successful)
                            .setCanceledOnTouchOutSide(false)
                            .setConfirm(R.string.guide_got_it, (dialog, which) -> {
                                dismissFileTransferCompletedDialog();
                            });
                    mFileTransferCompletedDialog.show();
                });

        getModel().getHTTPEventBus().watchReceiveProgressEvent()
                .filter(progress -> progress == 0)
                .flatMap(result -> getModel().getHTTPEventBus().watchReceiveProgressEvent()
                        .timeout(10, TimeUnit.SECONDS)
                        .onExceptionResumeNext(Observable.just(-1))
                        .takeUntil(progress -> progress == 100)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress == -1) {
                        Logger.d("Failed to receive the file.");
                        FabAlert.alert(getContext(), getResources().getText(R.string.exception_receiving_file));
                        if (mFabLoading != null) {
                            mFabLoading.dismiss();
                            mFabLoading = null;
                        }
                        return;
                    }
                    if (progress == 100) {
                        if (mFabLoading != null) {
                            mFabLoading.dismiss();
                            mFabLoading = null;
                        }
                        return;
                    }
                    if (mFabLoading != null) {
                        mFabLoading.setProgress(String.valueOf(progress));
                    } else {
                        Logger.d("Start receiving file...");
                        mFabLoading = FabLoading.create(getContext())
                                .setTitle(R.string.all_receiving_file_title)
                                .setDescription(R.string.desc_receiving_file)
                                .setProgress(String.valueOf(progress));
                        mFabLoading.show();
                    }
                }, LogHelper::log);

        getModel().getNetworkController().watchAccessPointList()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(accessPoints -> {
                    for (AccessPoint ap : accessPoints) {
                        if (ap.getSSID().isEmpty()) {
                            continue;
                        }

                        String currentSSID = getModel().getNetworkController().getActiveAccessPointImmediately().getSSID();

                        if (ap.getSSID().equals(currentSSID)) {
                            mTvWifiSSID.setText(currentSSID + "\n" + ap.getRssi() + "dBm");
                        }

                    }
                });

        Observable.interval(0, 30, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(tick -> {
                    getModel().getNetworkController().startScanSilently();
                });

        checkCurrentSession();
    }

    private void initialize() {
        getModel().getMachineController().updateCoordinateSystem()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {/*doNothing*/});

        // reset door detection flag if connected
        getModel().getMachineController().clearEnclosureDoorFlag();

        getModel().getMachineController().getEnclosureDoorObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isDoorOpen -> {
                    if (isDoorOpen) {
                        getModel().getRemoteController().setEnclosureDoorPause();

                        // Calculate door open count, used for notify remote client enclosure door status has changed.
                        int doorCount = getModel().getRemoteController().getEnclosureDoorCount();
                        doorCount = (doorCount >= 32) ? 0 : (doorCount + 1);
                        getModel().getRemoteController().setEnclosureDoorCount(doorCount);
                    }
                });

        getModel().getRemoteController().connectPrintController();

        getModel().getPrintController().getPrintJobStateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(state -> {
                    // FIXME: Workaround here.
                    // Set flag for remoteController to notice viewSub jump into print page.
                    boolean isStatePrinting = MachinePrintJobState.isStatePrinting(state.getValue());
                    boolean isPrintInCalibration = getModel().getWorkspace().getPrintSource() == Constants.PRINT_SOURCE_INTERNAL_NOT_RECOVERABLE_FILES;
                    getModel().getRemoteController().setNeedBackToPrint(!isPrintInCalibration && isStatePrinting);
                    mMachinePrintStateSubject.onNext(state);
                }, LogHelper::log);

        getModel().getPrintController().getPrintJobEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(event -> {
                    switch (event.getPrintEventState()) {
                        case PRINT_FINISH_SUCCESS:
                            Logger.i("Print Finished.");
                            Logger.d("Print job costs %s.", DateHelper.formatTime2(getModel().getPrintController().getTickCounter().getCount()));
                            break;
                        case REQUEST_POWER_LOSS_RECOVER_SUCCESS:
                            getModel().getPrintController().setPowerOutageFlag(false);
                            Logger.i("Print recovered.");
                            clearErrorFlag();
                            break;
                        default:
                            break;
                    }
                });
    }

    private void checkCurrentSession() {
        SessionManager.Session session = getModel().getRemoteController().getCurrentSession();

        if (session != NULL_SESSION) {
            // If not granted
            if (!session.isGranted()) {
                FabFullScreenDialog dialog = FabFullScreenDialog.create(requireContext());
                dialog.setIcon(R.drawable.pic_warning_remote_connect_120x120)
                        .setTitle(R.string.remote_connect_request)
                        .setMessage(R.string.remote_connect_request_desc);
                dialog.setPositive(R.string.all_yes, (v, which) -> {
                    v.dismiss();
                    getModel().getRemoteController().grantCurrentSession();
                    Logger.i("Remote access request approved, session #%s", session.getToken());
                });
                dialog.setNegative(R.string.all_no, (v, which) -> {
                    v.dismiss();
                    Logger.i("Remote access request denied. %s", session.getToken());
                    getModel().getRemoteController().denyCurrentSession();
                });
                dialog.show();
            }
        } else {
            Logger.d("Session closed, exiting...");
            requireActivity().finish();
        }
    }

    private void dismissFileTransferCompletedDialog() {
        if (mFileTransferCompletedDialog != null && mFileTransferCompletedDialog.isShowing()) {
            mFileTransferCompletedDialog.dismiss();
            mFileTransferCompletedDialog = null;
        }
    }

    private void showUpRemotePrintNotFinishDialog() {
        if (mPrintNotFinishDialog != null) return;

        // TODO: Description needs to be check and update.
        mPrintNotFinishDialog = FabConfirm.create(getContext())
                .setDescription(R.string.remote_print_job_not_finish_confirm)
                .setCanceledOnTouchOutSide(false)
                .setConfirm(R.string.all_stop, (dialog, which) -> {
                    dialog.dismiss();
                    stopRemotePrint(false);
                });
        mPrintNotFinishDialog.show();
    }

    private void dismissRemotePrintNotFinishDialog() {
        if (mPrintNotFinishDialog != null && mPrintNotFinishDialog.isShowing()) {
            mPrintNotFinishDialog.dismiss();
            mPrintNotFinishDialog = null;
        }
    }

    private void initAddOnStatus() {
        final boolean isEmergencyAvailable = getModel().getMachineController().isEmergencyStopAvailable();

        mIvEnclosureStatus = mViewEnclosureStatus.findViewById(R.id.iv_home_enclosure_status);
        mIvRotaryStatus = mViewRotaryStatus.findViewById(R.id.iv_home_rotary_status);

        // Enclosure status
        getModel().getMachineController().updateEnclosureStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(enclosureStatus -> {
                    mViewEnclosureStatus.setVisibility(enclosureStatus.isReady() ? View.VISIBLE : View.GONE);
                    mIvEnclosureStatus.setBackgroundResource(enclosureStatus.isReady() ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                });

        getModel().getMachineController().getRotaryStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    mViewRotaryStatus.setVisibility((status == (byte) 0) ? View.VISIBLE : View.GONE);
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
                });

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

        // watch filament status, auto pause if filament is out while printing
        getModel().getMachineController().getFilamentObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isFilamentOut -> {
                    final FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
                    if (isFilamentOut && status.printerStatus != 0) {
                        getModel().getRemoteController().setFilamentOutPause();
                    }
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

    private void initLaserStatus() {
        getModel().getLaserCameraController().getBluetoothConnectedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(connected -> {
                    mIvStatusCamera.setBackgroundResource(connected ? R.drawable.ic_home_status_normal_10x10 : R.drawable.ic_home_status_abnormal_10x10);
                });
    }

    private void updateLaserStatus() {
        getModel().getMachineController().getLaserFocusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(laserFocus -> {
                    mTvLaserFocus.setText(String.format(Locale.getDefault(), "%.1f mm", laserFocus));
                });
    }

    @OnClick(R.id.btn_remote_home_disconnect)
    void onClickDisconnect() {
//        final int remotePrintState = getModel().getRemoteController().getRemotePrintState();
//
//        if (remotePrintState == PrintController.STATE_PRINTING || remotePrintState == PrintController.STATE_PAUSED) {
//            showDisconnectWarningDialog();
//        } else {
//            getModel().getRemoteController().setCurrentSession(NULL_SESSION);
//        }
        getModel().getRemoteController().setCurrentSession(NULL_SESSION);
    }

    @OnClick(R.id.btn_remote_select_last_received_file)
    void onClickLastReceivedFile() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(Router.IntentKeys.IS_LOCAL, true);
        bundle.putBoolean("force_refresh", true);
        bundle.putString(Router.IntentKeys.FILE_PATH, mLastReceivedFileSubject.getValue().getPath());
        ARouter.getInstance().build(RoutePath.PREVIEW_ACTIVITY).with(bundle).withFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation();

        getModel().getRemoteController().setCurrentSession(NULL_SESSION);
    }

    private void showDisconnectWarningDialog() {
        FabConfirm.create(getContext())
                .setDescription(R.string.remote_disconnect_confirm)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    // Fixme: Workaround here, there's no page to manage remote printing,
                    //  so we need to stop the print to avoid machine working out of control.
                    dialog.dismiss();
                    stopRemotePrint(true);
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void stopRemotePrint(boolean selfDisconnect) {
        // Show loading dialog while requesting stop print.
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_remote_stop_print_loading, null);
        dialog.setView(view);
        dialog.show();

        getModel().getRemoteController().stop()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (selfDisconnect) {
                        getModel().getRemoteController().setCurrentSession(NULL_SESSION);
                    }
                    dialog.dismiss();
                }, e -> {
                    LogHelper.log(e);
                    dialog.dismiss();
                    showStopPrintErrorDialog();
                });
    }

    private void showStopPrintErrorDialog() {
        // Show loading dialog while requesting stop print.
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_remote_stop_print_failed, null);
        dialog.setView(view);
        dialog.show();

        AndroidSchedulers.mainThread().scheduleDirect(dialog::dismiss, 3000, Constants.TIME_UNIT);
    }

    private void LogFirebaseReceiveFileEvent(File file) {
        String suffix = file.getName()
                .substring(file.getName().lastIndexOf(".") + 1)
                .toLowerCase();
        FirebaseHelper.logRemoteFileEvent(getFirebaseAnalytics(), file.length(), suffix);
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

    private boolean isPrintStatePrinting(MachinePrintJobState state) {
        return MachinePrintJobState.isStatePrinting(state.getValue());
    }

    public void clearErrorFlag() {
        getModel().getSlaveComputer().resetErrorFlag()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    Logger.d("Error flag removed.");
                    getModel().getMachineController().clearPowerOutageFlag();
                }, LogHelper::log);
    }

    private void selfFinish() {
        requireActivity().finish();
    }
}
