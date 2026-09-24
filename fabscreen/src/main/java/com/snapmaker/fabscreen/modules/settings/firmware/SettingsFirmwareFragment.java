package com.snapmaker.fabscreen.modules.settings.firmware;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.BuildConfig;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import fabscreen.libraries.legacy.view.ProgressButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.api.ApiClient;
import fabscreen.libraries.legacy.data.api.HttpDownloader;
import fabscreen.libraries.legacy.data.api.VersionResponse;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okio.BufferedSource;
import okio.Okio;

public class SettingsFirmwareFragment extends BaseFragment {
    public static SettingsFirmwareFragment getInstance() {
        return new SettingsFirmwareFragment();
    }

    private final static int STATUS_CHECK_UPDATE = 0;
    private final static int STATUS_NEW_UPDATE_AVAILABLE = 1;
    private final static int STATUS_DOWNLOAD_UPDATE = 2;
    private final static int STATUS_UPDATE_DOWNLOADED = 3;

    @BindView(R.id.ll_firmware_new_features)
    View mViewNewFeatures;
    @BindView(R.id.ll_firmware_bug_fixes)
    View mViewBugFixes;
    @BindView(R.id.ll_firmware_improvements)
    View mViewImprovements;

    @BindView(R.id.tv_firmware_change_log_version)
    TextView mTvVersion;
    @BindView(R.id.tv_firmware_new_features)
    TextView mTvNewFeaturesDesc;
    @BindView(R.id.tv_firmware_bug_fixes)
    TextView mTvBugFixesDesc;
    @BindView(R.id.tv_firmware_improvements)
    TextView mTvImprovementsDesc;

    @BindView(R.id.btn_settings_firmware_check_updates)
    Button mBtnCheckUpdate;
    @BindView(R.id.btn_settings_firmware_download_update)
    ProgressButton mBtnDownloadUpdate;
    @BindView(R.id.btn_settings_firmware_update)
    Button mBtnFirmwareUpdate;
    @BindView(R.id.tv_settings_firmware_warning)
    TextView mTvLowNetworkQuality;

    private AlertDialog mCheckingUpdateDialog = null;
    private HttpDownloader mHttpDownloader;
    private VersionResponse.NewVersionData mNewVersionData;
    private NetworkQualityChecker mNetworkQualityChecker;

    private BehaviorSubject<Integer> mFirmwareStatusSubject = BehaviorSubject.createDefault(STATUS_CHECK_UPDATE);
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHttpDownloader = new HttpDownloader(getModel().getCacheDir().getAbsolutePath());
        mNetworkQualityChecker = new NetworkQualityChecker();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.settings_firmware_update);

        initView();
        // cancel update notification
        getModel().getPreferences().setUpdateNotification(false);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_firmware;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    protected void back() {
        if (mFirmwareStatusSubject.getValue() == STATUS_DOWNLOAD_UPDATE) {
            FabFullScreenDialog.create(getContext())
                    .setIcon(R.drawable.pic_dialog_warning_72x72)
                    .setTitle(R.string.all_warning)
                    .setMessage(R.string.settings_firmware_warning_cancel_download)
                    .setPositive(R.string.settings_firmware_cancel_update, (dialog, which) -> {
                        // stop download job
                        Logger.d("Stop downloading firmware.");
                        dialog.dismiss();
                        super.back();
                    })
                    .setNegative(R.string.all_continue, (dialog, which) -> {
                        // Do nothing
                        Logger.d("Confirm canceled. ");
                        dialog.dismiss();
                    })
                    .show();
        } else {
            super.back();
        }
    }

    private void initView() {
        // init button status
        mFirmwareStatusSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    mBtnCheckUpdate.setVisibility(status == STATUS_CHECK_UPDATE ? Button.VISIBLE : Button.GONE);
                    mBtnDownloadUpdate.setVisibility((status == STATUS_NEW_UPDATE_AVAILABLE || status == STATUS_DOWNLOAD_UPDATE) ? Button.VISIBLE : Button.GONE);
                    mBtnFirmwareUpdate.setVisibility(status == STATUS_UPDATE_DOWNLOADED ? Button.VISIBLE : Button.GONE);
                    mBtnDownloadUpdate.setEnabled(status == STATUS_NEW_UPDATE_AVAILABLE);
                });

        // init current change logs
        mNewVersionData = getCacheVersionResponse();
        if (mNewVersionData != null) {
            updateChangeLogView(mNewVersionData);
            String lastUpdateVersion = getModel().getPreferences().getLastUpdatePackageVersion();
            if ((!mNewVersionData.version.equals(lastUpdateVersion)) && getModel().getPreferences().getUpdateNotification()) {
                getModel().getPreferences().setLastCheckVersion(mNewVersionData.version);
                mFirmwareStatusSubject.onNext(STATUS_NEW_UPDATE_AVAILABLE);
            } else {
                mFirmwareStatusSubject.onNext(STATUS_CHECK_UPDATE);
            }
        } else {
            // default version?
            VersionResponse.NewVersionData defaultResponse = new VersionResponse.NewVersionData();
            updateChangeLogView(defaultResponse);
            mTvVersion.setText(getModel().getPreferences().getLastUpdatePackageVersion());
        }

        // skip initial status
        mNetworkQualityChecker.getNetWorkQualityObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(quality -> {
                    mTvLowNetworkQuality.setVisibility(quality == NetworkQualityChecker.NETWORK_QUALITY_BAD ? TextView.VISIBLE : TextView.GONE);
                });
    }

    private void showCheckingUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_settings_firmware_update_checking, null);
        dialog.setView(view);
        dialog.show();

        mCheckingUpdateDialog = dialog;
    }

    private void showCheckFailDialog() {
        if (mCheckingUpdateDialog.isShowing()) {
            mCheckingUpdateDialog.dismiss();
            mCheckingUpdateDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_settings_firmware_update_check_failed, null);
        dialog.setView(view);
        dialog.show();

        AndroidSchedulers.mainThread().scheduleDirect(dialog::dismiss, 3000, TimeUnit.MILLISECONDS);
    }

    private void showDownloadFailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_settings_firmware_update_check_failed, null);
        dialog.setView(view);
        dialog.show();
    }

    private void showNewestVersionDialog() {
        if (mCheckingUpdateDialog.isShowing()) {
            mCheckingUpdateDialog.dismiss();
            mCheckingUpdateDialog = null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_settings_firmware_update_already_newest, null);
        dialog.setView(view);
        dialog.show();

        AndroidSchedulers.mainThread().scheduleDirect(dialog::dismiss, 3000, TimeUnit.MILLISECONDS);
    }

    private void showNewUpdateAvailableDialog() {
        // build new version description string
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getString(R.string.settings_firmware_dialog_new_version_available_desc,
                mNewVersionData.version, mNewVersionData.package_size / (1024.0 * 1024.0)));
        for (String s : mNewVersionData.summary) {
            stringBuilder.append(s).append("\n");
        }
        // Get version change log brief
        FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_dialog_setting_72_72)
                .setTitle(R.string.settings_firmware_dialog_new_version_available)
                .setMessage(stringBuilder.toString())
                .setPositive(R.string.settings_firmware_update_now, (dialog, which) -> {
                    downloadUpdate();
                    dialog.dismiss();
                })
                .setNegative(R.string.settings_firmware_dialog_later, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void updateChangeLogView(VersionResponse.NewVersionData data) {
        mTvVersion.setText(data.version);
        List<String> features = data.change_log.getFeatures();
        if (features != null && features.size() != 0) {
            mViewNewFeatures.setVisibility(View.VISIBLE);
            StringBuilder feature = new StringBuilder();
            for (String s : features) {
                feature.append(s).append("\n");
            }
            mTvNewFeaturesDesc.setText(feature.toString());
        } else {
            mViewNewFeatures.setVisibility(View.GONE);
        }
        List<String> bugFixes = data.change_log.getBugFixes();
        if (bugFixes != null && bugFixes.size() != 0) {
            mViewBugFixes.setVisibility(View.VISIBLE);
            StringBuilder bugFix = new StringBuilder();
            for (String s : bugFixes) {
                bugFix.append(s).append("\n");
            }
            mTvBugFixesDesc.setText(bugFix.toString());
        } else {
            mViewBugFixes.setVisibility(View.GONE);
        }
        List<String> improvements = data.change_log.getImprovement();
        if (improvements != null && improvements.size() != 0) {
            mViewImprovements.setVisibility(View.VISIBLE);
            StringBuilder improvement = new StringBuilder();
            for (String s : improvements) {
                improvement.append(s).append("\n");
            }
            mTvImprovementsDesc.setText(improvement.toString());
        } else {
            mViewImprovements.setVisibility(View.GONE);
        }
        mBtnDownloadUpdate.setText(getString(R.string.settings_firmware_btn_download_firmware_desc, data.package_size / (1024.0f * 1024.0)));
    }

    private void startDownloadFirmware() {
        mHttpDownloader.getDownloadProgressObservable()
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    mBtnDownloadUpdate.setProgress(progress);
                    mBtnDownloadUpdate.setText(progress + "%");
                    if (progress == 100) {
                        mNetworkQualityChecker.stop();
                        mFirmwareStatusSubject.onNext(STATUS_UPDATE_DOWNLOADED);
                    }
                });

        mHttpDownloader.startDownload(mNewVersionData.url)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(responseBody -> {
                    // donothing
                }, e -> {
                    if (e instanceof SocketTimeoutException) {
                        // SocketTimeout
                        Logger.d("Socket timeout while downloading, please check your network is available.");
                    }
                    AndroidSchedulers.mainThread().scheduleDirect(this::showDownloadFailDialog, 0, TimeUnit.MILLISECONDS);
                    mBtnDownloadUpdate.reset();
                    mFirmwareStatusSubject.onNext(STATUS_NEW_UPDATE_AVAILABLE);
                    mBtnDownloadUpdate.setText(getString(R.string.settings_firmware_btn_download_firmware_desc, mNewVersionData.package_size / (1024.0f * 1024.0)));
                    mNetworkQualityChecker.stop();
                    LogHelper.log(e);
                });
        mNetworkQualityChecker.start();

        mFirmwareStatusSubject.onNext(STATUS_DOWNLOAD_UPDATE);
    }

    @OnClick(R.id.btn_settings_firmware_check_updates)
    void checkForUpdate() {
        showCheckingUpdateDialog();
        // send Request to update server
        ApiClient apiClient = new ApiClient(getModel().getPreferences().getApiHost());
        apiClient.getLatestVersion()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(version -> {
                    mCheckingUpdateDialog.dismiss();

                    mNewVersionData = version.data.new_version;
                    Logger.d("version %s, url %s, package_size %d summary %s",
                            version.data.new_version.version,
                            version.data.new_version.url,
                            version.data.new_version.package_size, version.data.new_version.summary.toString());

                    // Check if version is latest
                    if (version.data.new_version.version.equals(getModel().getPreferences().getLastUpdatePackageVersion())) {
                        AndroidSchedulers.mainThread().scheduleDirect(this::showNewestVersionDialog, 0, TimeUnit.MILLISECONDS);
                        getModel().getPreferences().setLastCheckVersion(version.data.new_version.version);
                    } else {
                        AndroidSchedulers.mainThread().scheduleDirect(this::showNewUpdateAvailableDialog, 0, TimeUnit.MILLISECONDS);
                        // show version in changelog
                        updateChangeLogView(version.data.new_version);
                        mFirmwareStatusSubject.onNext(STATUS_NEW_UPDATE_AVAILABLE);
                    }
                }, e -> {
                    // Request failed
                    mCheckingUpdateDialog.dismiss();
                    // show failed dialog
                    if (e instanceof UnknownHostException) {
                        Logger.w("Unable to resolve API server, please check your network connectivity.");
                    } else if (e instanceof SocketTimeoutException) {
                        Logger.w("Socket timeout, please check your network is available.");
                        AndroidSchedulers.mainThread().scheduleDirect(this::showCheckFailDialog, 1000, TimeUnit.MILLISECONDS);
                    } else {
                        LogHelper.log(e);
                    }
                });
    }

    @OnClick(R.id.btn_settings_firmware_download_update)
    void downloadUpdate() {
        if (mFirmwareStatusSubject.getValue() != STATUS_NEW_UPDATE_AVAILABLE) return;

        IFileManager fileManager = BaseApplication.getInstance().getFabLocalFileManager();
        fileManager.mount();
        long availableSpace = fileManager.getTotalSpace() - fileManager.getUsedSpace();
        long requireSpace = mNewVersionData.package_size * 3L;

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
                            if (count < requireSpace - availableSpace) {
                                IFile tempFile = files.get(i);
                                count += tempFile.length();
                                deleteFiles.add(tempFile);
                            }
                        }
                        for (IFile file2 : deleteFiles) {
                            Logger.d("Start deleting file " + file2.getName());
                            fileManager.removeFile(file2);
                        }
                        startDownloadFirmware();

                    });
        } else {
            startDownloadFirmware();
        }

    }

    @OnClick(R.id.btn_settings_firmware_update)
    void onClickUpdate() {
        if (getContext() != null) {
            saveVersionResponse(mNewVersionData);
            String filePath = getModel().getCacheDir().getAbsolutePath() + "/update.bin";
            SettingsFirmwareActivity activity = (SettingsFirmwareActivity) getContext();
            activity.gotoUpdateFirmware(filePath, true);
        }
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

    private VersionResponse.NewVersionData getCacheVersionResponse() {
        File file = new File(getModel().getCacheDir(), "version_check.json");
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        VersionResponse.NewVersionData data = null;
        String line;
        BufferedSource source;

        try {
            source = Okio.buffer(Okio.source(new FileInputStream(file)));
            while (true) {
                line = source.readUtf8Line();
                if (line == null) {
                    break;
                }

                content.append(line);
            }
            data = new Gson().fromJson(content.toString(), VersionResponse.NewVersionData.class);
        } catch (IOException e) {
            LogHelper.log(e);
        }
        return data;
    }

    // reserve
    void factoryReset() {
        if (getContext() == null) return;

        FabFullScreenDialog.create(getContext())
                .setTitle("Warning")
                .setIcon(R.drawable.ic_module_settings_80x80)
                .setMessage("Current version is " + BuildConfig.VERSION_NAME + " , would you like to reset to the factory version?")
                .setPositive(R.string.all_yes, (dialog, which) -> {
                    dialog.dismiss();
                    Intent updateIntent = new Intent("com.snapmaker.updateApkBroadcast");
                    updateIntent.putExtra("URL", "");
                    updateIntent.putExtra("OPERATION", "factory_reset");
                    getContext().sendBroadcast(updateIntent);
                })
                .setNegative(R.string.all_no, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
