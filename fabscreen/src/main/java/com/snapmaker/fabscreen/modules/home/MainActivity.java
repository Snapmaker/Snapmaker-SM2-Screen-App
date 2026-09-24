package com.snapmaker.fabscreen.modules.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.print.BatchPrintController;
import fabscreen.libraries.legacy.data.print.IPrintController;
import fabscreen.libraries.legacy.data.print.PrintController;
import fabscreen.libraries.legacy.data.serial.SerialController;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.SemVerHelper;
import fabscreen.libraries.legacy.lib.crash.CrashCollectHandler;
import fabscreen.libraries.legacy.lib.update.FabUpdatePackage;
import fabscreen.libraries.legacy.route.RoutePath;
import fabscreen.libraries.legacy.service.SerialPortService;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

@Route(path = RoutePath.MAIN_ACTIVITY)
public class MainActivity extends BaseActivity {

    private FabConfirm mStatusInvalidDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        ArrayList<Intent> intents = intent.getParcelableArrayListExtra(CrashCollectHandler.FABSCREEN_CRASH);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // bind background services
        bindBackgroundServices();

        // Check if com.snapmaker.update is installed
        Intent updateIntent = new Intent("com.snapmaker.updateApkBroadcast");
        updateIntent.putExtra("OPERATION", "update");
        sendBroadcast(updateIntent);

        if (getModel().getPreferences().getMachineUpdatedFlag()) {
            finishUpdateFromStartUp();
        }

        getModel().getSlaveComputer().getConnectedObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(connected -> connected)
                .flatMap(connected -> getModel().getSlaveComputer().getMachineStatusObservable())
                .filter(machineStatus -> !machineStatus.isDefault)
                .flatMap(machineStatus -> getModel().getSlaveComputer().getControllerVersion())
                .flatMap(version -> {
                    int major = SemVerHelper.major(version);
                    if (major > 4) {
                        return Observable.just(0);
                    } else {
                        return getModel().getSlaveComputer().useBatchGcodeMode(0).onExceptionResumeNext(Observable.just(-1));
                    }
                })
                .take(1)
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {
                    Logger.d("use batchGcodeMode received " + machineStatus);
                    getModel().getPrintController().disposeAll();
                    // Reset PrintController
                    IPrintController printController = (machineStatus != -1) ? new BatchPrintController(getModel().getPrintController()) : new PrintController(getModel().getPrintController());
                    getModel().setPrintController(printController);
                    getModel().getRemoteController().setPrintController(printController);
                    getModel().getMachineController().setPrintController(printController);
                    // Get the Activities opened before crash and open them again through the Router
                    if (intents != null && intents.size() > 2) {
                        for (int i = 1; i < intents.size() - 1; i++) {
                            // Skip opening MainActivity and crashing Activity
                            Router.getInstance().start(FabScreenApplication.getInstance().getViewSub().getContext(), intents.get(i));
                        }
                    } else {
                        Router.getInstance().routeToHomeActivity().start(this);
                    }
                }, LogHelper::log);

        getModel().getSlaveComputer().getMachineStatusValidObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isValid -> {
                    if (!isValid) {
                        if (mStatusInvalidDialog == null) {
                            mStatusInvalidDialog = FabConfirm.create(this)
                                    .setCanceledOnTouchOutSide(false)
                                    .setIcon(R.drawable.pic_dialog_warning_72x72)
                                    .setDescription(R.string.dialog_warning_invalid_status_detected)
                                    .setConfirm(R.string.all_continue, (dialog, which) -> {
                                        Router.getInstance().routeToHomeActivity().start(this);
                                    });
                            mStatusInvalidDialog.show();
                        }
                    } else {
                        if (mStatusInvalidDialog != null && mStatusInvalidDialog.isShowing()) {
                            mStatusInvalidDialog.dismiss();
                            mStatusInvalidDialog = null;
                        }
                    }
                });


        // set Check Update when boot up
        getModel().getPreferences().setCheckUpdateFlag(true);
    }

    @Override
    protected void onResume() {

        if (getModel().getMachineController().isEmergencyStopTriggered()) {
            getModel().onEmergencyStop();
            Logger.d("MainActivity emergency stop");
            AndroidSchedulers.mainThread().scheduleDirect(this::gotoEmergencyPage, 200, Constants.TIME_UNIT);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Only stopped when activity is destroyed for good. And not stopped during an orientation
        // change.
        if (isFinishing()) {
            unbindBackgroundServices();
        }
    }

    private void finishUpdateFromStartUp() {
        Logger.d("Finishing up last update...");
        String dataFolderPath = getModel().getDataDir().getAbsolutePath() + File.separatorChar + "update";
        String cacheFolderPath = getModel().getCacheDir().getAbsolutePath() + File.separatorChar + "update";

        File saveFolder = new File(dataFolderPath);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }

        // Clear update files in folder
        File[] files = saveFolder.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }

        // save update file from cache.
        File cacheUpdateFile = new File(cacheFolderPath, "update.bin");
        if (cacheUpdateFile.exists()) {
            // parse package header
            String cacheUpdateFileVersion = parseUpdateFileHeader(cacheUpdateFile);
            if (cacheUpdateFileVersion != null && !cacheUpdateFileVersion.equals(getModel().getPreferences().getLastUpdatePackageVersion())) {
                // Cache file version is not equal to update version.
                Logger.d("Update: removing outdated cache update file...");
                cacheUpdateFile.delete();
                return;
            }

            File saveFile = new File(saveFolder, "update.bin");
            BufferedSource bufferedSource = null;
            BufferedSink bufferedSink = null;
            try {
                bufferedSource = Okio.buffer(Okio.source(new FileInputStream(cacheUpdateFile)));
                bufferedSink = Okio.buffer(Okio.sink(new FileOutputStream(saveFile)));
                // copy file from source with buffer
                int len;
                byte[] buffer = new byte[1024 * 16];
                while ((len = bufferedSource.read(buffer)) != -1) {
                    bufferedSink.write(buffer, 0, len);
                }
                bufferedSink.close();
                bufferedSource.close();
                Logger.d("Firmware update finish.");
            } catch (IOException e) {
                LogHelper.log(e);
            } finally {
                try {
                    if (bufferedSink != null) {
                        bufferedSink.close();
                    }
                    if (bufferedSource != null) {
                        bufferedSource.close();
                    }
                } catch (IOException e) {
                    LogHelper.log(e);
                }
            }
        } else {
            Logger.w("Update file not exist.");
        }
    }

    private String parseUpdateFileHeader(File file) {
        try {
            BufferedSource source = Okio.buffer(Okio.source(new FileInputStream(file)));
            byte[] p = source.readByteArray(file.length());
            FabUpdatePackage.UpdatePackageHeader cacheHeader = FabUpdatePackage.UpdatePackageHeader.parse(p);
            if (cacheHeader != null && cacheHeader.getVersion() != null) {
                return cacheHeader.getVersion();
            } else {
                return null;
            }
        } catch (IOException e) {
            LogHelper.log(e);
            return null;
        }
    }

    private void bindBackgroundServices() {
        // Bind serial port service
        Intent intent = new Intent(this, SerialPortService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindBackgroundServices() {
        // Unbind serial port service
        unbindService(mServiceConnection);

        // Stop all services
        FabScreenApplication.getInstance().unregister();
        FabScreenApplication.getInstance().stopServices();
        FabScreenApplication.getInstance().stopDiscoverServer();
        FabScreenApplication.getInstance().stopHttpServer();
    }

    // A ServiceConnection to deal with connect and disconnect events.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ISlaveComputer slaveComputer = getModel().getSlaveComputer();
            if (slaveComputer instanceof SerialController) {
                ((SerialController) slaveComputer).setServiceBinder(iBinder);
            }

            Logger.i("Connecting serial port...");

            // After service bind, we can connect the serial port now.
            getModel().getMachineController().connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            ISlaveComputer slaveComputer = getModel().getSlaveComputer();
            if (slaveComputer instanceof SerialController) {
                ((SerialController) slaveComputer).setServiceBinder(null);
            }
        }
    };

    private void gotoEmergencyPage() {
        Router.getInstance().routeToEmergencyStopPage(false).start(this);
    }
}
