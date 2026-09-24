package fabscreen.libraries.legacy.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.launcher.ARouter;
import com.orhanobut.logger.Logger;

import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.R;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.print.MachinePrintJobState;
import fabscreen.libraries.legacy.data.print.PrintEventState;
import fabscreen.libraries.legacy.data.remote.SessionManager;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.ToastHelper;
import fabscreen.libraries.legacy.route.RoutePath;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import fabscreen.libraries.legacy.view.FabScreenDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

// TODO: temporary class
// enter/leave methods should be moved to Router
// listens should be moved to FabApplication?
public class ViewSub {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Context mContext;
    private Model mModel;

    private boolean mHomeFlag = false;

    public ViewSub(Model model) {
        mModel = model;

        listenAlways();

    }

    @Nullable
    public Context getContext() {
        return mContext;
    }

    private Model getModel() {
        return mModel;
    }

    @Nullable
    private Resources getResources() {
        if (mContext == null) {
            return null;
        }
        return mContext.getResources();
    }

    void listenAlways() {
        Disposable sub;

        sub = getModel().getSlaveComputer().getConnectedObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connected -> {
                    if (!connected) {
                        if (getModel().getMachineController().isEmergencyStopTriggered()) return;
                        // Lost connection
                        Logger.w("Loss connection from machine.");
                        FabFullScreenDialog.create(getContext())
                                .setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setTitle(R.string.all_warning)
                                .setMessage(R.string.warning_machine_not_responding)
                                .setPositive(R.string.all_reconnect, (dialog, which) -> {
                                    // Reconnect
                                    Logger.i("Try to reconnect machine...");
                                    getModel().getMachineController().reconnect();
                                    dialog.dismiss();
                                })
                                .setNegative(R.string.all_no, (dialog, which) -> {
                                    // Do nothing (to be defined)
                                    Logger.i("Reconnect has been canceled.");
                                    dialog.dismiss();
                                    ARouter.getInstance().build(RoutePath.HOME_ACTIVITY).navigation();
                                })
                                .show();
                    }
                });
        compositeDisposable.add(sub);

        sub = getModel().getRemoteController().getRemoteStateObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(remoteState -> {
                    if (remoteState == SessionManager.State.STATE_ACTIVE && !getModel().getRemoteController().getRemotePageFlag()) {
                        ARouter.getInstance().build(RoutePath.REMOTE_ACTIVITY).withFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation();
                    } else if (getModel().getRemoteController().isNeedBackToPrint()
                            && MachinePrintJobState.isStatePrinting(getModel().getPrintController().getPrintJobState().getValue())) {
                        AndroidSchedulers.mainThread().scheduleDirect(this::gotoPrintSilently, 1000, TimeUnit.MILLISECONDS);
                    }
                }, LogHelper::log);
        compositeDisposable.add(sub);

        sub = getModel().getMachineController().getEmergencyStopObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isTriggered -> {
                    if (isTriggered) {
                        Logger.d("Emergency Stop button triggered.");
                        ARouter.getInstance().build(RoutePath.MAIN_ACTIVITY).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).navigation();
                    }
                });
        compositeDisposable.add(sub);

        sub = Observable.combineLatest(getModel().getMachineController().getNozzleTemperatureExceedObservable(),
                getMachineStatus(),
                (exceed, status) -> exceed && status)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isTemperatureExceed -> {
                            if (isTemperatureExceed) {
                                FabFullScreenDialog.create(getContext())
                                        .setIcon(R.drawable.pic_dialog_warning_72x72)
                                        .setTitle(R.string.dialog_dual_extruder_hotend_abnormal_title)
                                        .setMessage(R.string.dialog_dual_extruder_hotend_abnormal_content)
                                        .setPositive(R.string.all_ok, (dialog, which) -> {
                                            dialog.dismiss();
                                        })
                                        .show();
                            }
                        });
        compositeDisposable.add(sub);
    }

    private void gotoPrintSilently() {
        ARouter.getInstance().build(RoutePath.PRINT_ACTIVITY).withFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation();
    }

    Observable<Boolean> getMachineStatus() {
        return getModel().getMachineController().getMachineStatusObservable()
                .filter(machineStatus -> !machineStatus.isDefault)
                .filter(machineStatus -> machineStatus.headStatus == Constants.HEAD_3DP_DUAL_EXTRUDER)
                .flatMap(ret -> Observable.just(true))
                .take(1);
    }

    void enter(Context context) {
        mContext = context;
    }

    void leave(Context context) {
        // do nothing
        // mContext = null;
    }

    public boolean getHomeFlag() {
        return mHomeFlag;
    }

    public void setHomeFlag(boolean flag) {
        mHomeFlag = flag;
    }

    public void setObservableUSBAttach(Observable<Boolean> fileManagerStateObservable) {
        Disposable disposable = fileManagerStateObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(isAttach -> {
                    new ToastHelper.Builder()
                            .setDrawable(isAttach ? R.drawable.ic_usb_detected_80x80 : R.drawable.ic_usb_unplugged_80x80)
                            .setMessage(isAttach ? R.string.toast_usb_device_detected : R.string.toast_usb_device_unpuggled)
                            .build()
                            .showToast(mContext);
                });
    }

    FabScreenDialog fabFullScreenDialog;

    public void listenerHeaderSecurityStatus(Observable<FabPacketContent.HeaderSecurity> headerSecurityObservable) {
        Disposable sub = headerSecurityObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(headerSecurity -> {
                    Logger.d(headerSecurity);
                    getModel().getMachineController().setLaser10WErrorState(headerSecurity.status);
                    if (fabFullScreenDialog == null || !fabFullScreenDialog.isAlive()) {
                        fabFullScreenDialog = FabScreenDialog.create(getContext());
                    }

                    if ((headerSecurity.status & FabPacketContent.HeaderSecurity.HEADER_ROLL_ABNORMAL_ANGLE) != 0) {
                        fabFullScreenDialog.setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setTitle(R.string.laser_10W_dialog_not_placed_correctly)
                                .setDescription(R.string.laser_10W_dialog_not_placed_correctly_desc)
                                .setConfirm(R.string.all_ok, (dialog, which) -> {
                                    dialog.dismiss();
                                });
                        fabFullScreenDialog.show();
                    } else if ((headerSecurity.status & FabPacketContent.HeaderSecurity.HEADER_TEMPERATURE_ANOMALY) != 0) {
                        fabFullScreenDialog.setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setTitle(R.string.laser_10W_dialog_temperature_too_high)
                                .setDescription(R.string.laser_10W_dialog_temperature_too_high_desc)
                                .setConfirm(R.string.all_ok, (dialog, which) -> {
                                    dialog.dismiss();
                                });
                        fabFullScreenDialog.show();
                    } else if ((headerSecurity.status & FabPacketContent.HeaderSecurity.HEADER_FIRE_DETECTION) != 0) {
                        fabFullScreenDialog.setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setTitle(R.string.laser_40w_dialog_visible_frames_detected_title)
                                .setDescription(R.string.laser_40w_dialog_visible_frames_detected_desc)
                                .setConfirm(R.string.all_ok, ((dialog, which) -> {
                                    dialog.dismiss();
                                }));
                        fabFullScreenDialog.show();
                    } else {
                        fabFullScreenDialog.dismiss();
                        fabFullScreenDialog = null;
                    }
                });
        compositeDisposable.add(sub);
    }
}
