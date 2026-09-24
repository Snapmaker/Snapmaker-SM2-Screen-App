package com.snapmaker.fabscreen.modules.update;

import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.base.BaseViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import fabscreen.libraries.legacy.lib.update.FabUpdatePackage;
import fabscreen.libraries.legacy.lib.update.FabUpdater;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

// TODO: 2021/8/24  viewModel must not acquire context.
public class UpdateViewModel extends BaseViewModel {
    private static final String TAG = "UpdateViewModel";

    private CompositeDisposable disposables = new CompositeDisposable();

    private Context mContext;
    private Model mModel;
    private FabUpdater mUpdater;

    private String mControllerVersion;
    private int mPackageIndex = -1;

    private IFile mFirmwareFile;

    private PublishSubject<Boolean> mUpdateCompleteSubject = PublishSubject.create();
    private BehaviorSubject<String> mMessageSubject = BehaviorSubject.createDefault("");
    private BehaviorSubject<Float> mProgressSubject = BehaviorSubject.createDefault(0f);
    private BehaviorSubject<String> mProgressTextSubject = BehaviorSubject.createDefault("");

    private Disposable mUpdateCheckSubscription;

    UpdateViewModel(Context context, Model model) {
        super();
        mContext = context;
        mModel = model;

        mUpdater = new FabUpdater();

        initEvents();
    }

    boolean parse(IFile file) {
        try {
            return mUpdater.parse(file);
        } catch (Exception e) {
            LogHelper.log(e);
            return false;
        }
    }

    public Observable<IFile> startSearchingFile(IFileManager fileManager, String filePath) {
        mMessageSubject.onNext(mContext.getResources().getString(R.string.settings_update_loading_update_file));
        return fileManager.search(filePath)
                .delay(300, Constants.TIME_UNIT)
                .doOnNext(file -> {
                    mFirmwareFile = file;
                });
    }

    Observable<Boolean> update() {
        getModel().getSlaveComputer().setHeartbeatEnabled(false);

        Disposable sub = getModel().getSlaveComputer().getControllerVersion()
                .subscribe(version -> {
                    mControllerVersion = version;

                    updatePackage(0);
                });
        disposables.add(sub);

        return mUpdateCompleteSubject;
    }

    private void initEvents() {
        Disposable sub = getModel().getSlaveComputer().watchPacketIndexRequest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::sendPackagePart);
        disposables.add(sub);
    }

    private void updatePackage(int pid) {
        mPackageIndex = pid;

        if (pid == mUpdater.getPacketCount()) {
            // set update flag
            mModel.getPreferences().setMachineUpdatedFlag(true);
            mModel.getPreferences().setLastUpdatePackageVersion(mUpdater.getPackageVersion());

            if (mUpdater.getScreenUpdate() != null) {
                updateApp();
            } else {
                mUpdateCompleteSubject.onNext(true);
            }
        } else {
            byte[] p = mUpdater.usePacket(pid);

            if (p[0] == FabUpdatePackage.UpdatePackageHeader.TYPE_CONTROLLER_FIRMWARE) {
                mMessageSubject.onNext(mContext.getString(R.string.settings_update_msg_updating_controller));
            } else if (p[0] == FabUpdatePackage.UpdatePackageHeader.TYPE_MODULE_FIRMWARE) {
                mMessageSubject.onNext(mContext.getString(R.string.settings_update_msg_updating_modules));
            }

            // Update Controller if update version is not equals with current version.
            if (p[0] == FabUpdatePackage.UpdatePackageHeader.TYPE_CONTROLLER_FIRMWARE
                    && mUpdater.getControllerVersion().equals(mControllerVersion)
                    && !mUpdater.isForceUpdate()) {
                // skip the package
                updatePackage(pid + 1);
            } else {
                // Start sending update packet to Controller.
                startUpdatePackage();
            }
        }
    }

    private File saveToFile(byte[] packet) {
        // TODO: refactor File into IFile
        FileOutputStream fos;
        File file;
        try {
            file = new File(mModel.getCacheDir(), "update.apk");
            fos = new FileOutputStream(file);
            fos.write(packet, 0, packet.length);

            fos.flush();
            fos.close();
        } catch (IOException e) {
            LogHelper.log(e);
            file = null;
        }
        return file;
    }

    private void updateApp() {
        mMessageSubject.onNext(mContext.getString(R.string.settings_update_msg_updating_screen));
        File file = saveToFile(mUpdater.getScreenUpdate());
        if (file != null) {
            // Send broadcast to update FabScreen itself.
            Intent updateIntent = new Intent("com.snapmaker.updateApkBroadcast");
            updateIntent.putExtra("URL", file.getPath());
            updateIntent.putExtra("OPERATION", "local_file");
            mContext.sendBroadcast(updateIntent);
        }
    }

    private void startUpdatePackage() {
        mProgressTextSubject.onNext("");
        Disposable sub = getModel().getSlaveComputer().startUpdate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        // do nothing wait for request
                    } else {
                        mUpdateCompleteSubject.onNext(false);
                    }
                }, e -> {
                    LogHelper.log(e);
                    mUpdateCompleteSubject.onNext(false);
                });
        disposables.add(sub);
    }

    private void sendPackagePart(int index) {
        if (index == mUpdater.getPartCount()) {
            getModel().getSlaveComputer().sendUpdatePackage((byte) 0x02, (short) index, null);
            mProgressSubject.onNext(1f);
            checkPackageUpdated();
        } else {
            final byte[] content = mUpdater.getPart(index);
            getModel().getSlaveComputer().sendUpdatePackage((byte) 0x01, (short) index, content);
            mProgressSubject.onNext(1f * (index + 1) / mUpdater.getPartCount());
            mProgressTextSubject.onNext(String.format(Locale.US, "%d / %d", index + 1, mUpdater.getPartCount()));
        }
    }

    private void checkPackageUpdated() {
        mMessageSubject.onNext(mContext.getString(R.string.settings_update_msg_finishing_up_controller_update));
        // check if controller is available
        mUpdateCheckSubscription = Observable.interval(5, TimeUnit.SECONDS)
                .delaySubscription(10, TimeUnit.SECONDS)
                .flatMap(t ->
                        getModel().getSlaveComputer()
                                .requestMachineStatus()
                                .onErrorReturnItem(FabPacketContent.MachineStatus.getDefaultInstance())
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    if (!status.isDefault) {
                        mUpdateCheckSubscription.dispose();
                        mUpdateCheckSubscription = null;

                        updatePackage(mPackageIndex + 1);
                    }
                }, Throwable::printStackTrace);
    }

    public void setUpdateFile(IFile file) {
        mFirmwareFile = file;
    }

    public IFile getUpdateFile() {
        return mFirmwareFile;
    }

    public void saveUpdateFile(String folderPath, IFile file) {
        Logger.d("Start saving update file into cache…");
        File saveFolder = new File(folderPath);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }

        // clear last update files in folder
        File[] files = saveFolder.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }

        File saveFile = new File(saveFolder, "update.bin");
        BufferedSource bufferedSource = null;
        BufferedSink bufferedSink = null;
        IFileManager iFileManager = file.isLocal() ?
                FabScreenApplication.getInstance().getFabLocalFileManager() :
                FabScreenApplication.getInstance().getFabUsbFileManager();
        try {
            bufferedSource = Okio.buffer(Okio.source(iFileManager.getInputStream(file)));
            bufferedSink = Okio.buffer(Okio.sink(new FileOutputStream(saveFile)));
            // copy file from source with buffer
            int len;
            byte[] buffer = new byte[1024 * 16];
            while ((len = bufferedSource.read(buffer)) != -1) {
                bufferedSink.write(buffer, 0, len);
            }
            bufferedSink.close();
            bufferedSource.close();
        } catch (IOException e) {
            LogHelper.log(e);
        } finally {
            try {
                iFileManager.deviceHang();
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
    }

    public Single<Boolean> checkAvailableSpaceForUpdate(IFile targetFile) {
        SingleSubject<Boolean> resultSubject = SingleSubject.create();
        IFileManager fileManager = BaseApplication.getInstance().getFabLocalFileManager();
        fileManager.mount();
        long availableSpace = fileManager.getTotalSpace() - fileManager.getUsedSpace();
        long requireSpace = calculateRequireSpaceFromFile(targetFile);

        if (requireSpace > availableSpace) {
            Logger.w("not enough available space for file!");
            fileManager.listFiles()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(files -> {
                        files.sort(Comparator.comparingLong(IFile::lastModified));
                        long count = 0;
                        ArrayList<IFile> deleteFiles = new ArrayList<>();
                        for (int i = 0 ; i < files.size() ; i++) {
                            IFile tempFile = files.get(i);
                            if (count < requireSpace - availableSpace) {
                                if (!tempFile.getName().startsWith(".")) {
                                    count += tempFile.length();
                                    deleteFiles.add(tempFile);
                                }
                            }
                        }
                        for (IFile file2 : deleteFiles) {
                            Logger.d("Start deleting file " + file2.getName());
                            fileManager.removeFile(file2);
                        }
                        resultSubject.onSuccess(true);
                    });
        } else {
            resultSubject.onSuccess(true);
        }
        return resultSubject.hide();
    }

    private long calculateRequireSpaceFromFile(IFile targetFile) {
        long requireSpace = 0;
        if (targetFile.isLocal()) {
            requireSpace += Math.max(targetFile.length() * 2, 300 * 1024 * 1024);;
        } else {
            // Cache file into local and Android 7.1 needs double file length space for updating app.
            requireSpace += Math.max(targetFile.length() * 3, 300 * 1024 * 1024);
        }
        return requireSpace;
    }

    Observable<String> getUpdateMessageObservable() {
        return mMessageSubject;
    }

    Observable<String> getUpdateProgressTextObservable() {
        return mProgressTextSubject;
    }

    Observable<Float> getUpdateProgressObservable() {
        return mProgressSubject;
    }

    public void dispose() {
        if (mUpdateCheckSubscription != null && !mUpdateCheckSubscription.isDisposed()) {
            mUpdateCheckSubscription.dispose();
        }
        disposables.clear();
    }
}
