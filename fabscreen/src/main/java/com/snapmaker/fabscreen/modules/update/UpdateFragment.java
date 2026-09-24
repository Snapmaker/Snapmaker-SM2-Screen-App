package com.snapmaker.fabscreen.modules.update;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.FabScreenApplication;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.home.HomeActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.lib.file.FabLocalFileManager;
import fabscreen.libraries.legacy.lib.file.FabUsbFileManager;
import fabscreen.libraries.legacy.lib.file.IFile;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import fabscreen.libraries.legacy.view.CircularProgressView;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class UpdateFragment extends BaseFragment {
    private static final String TAG = UpdateFragment.class.getSimpleName();

    @BindView(R.id.cpv_update_progress)
    CircularProgressView mCpvProgress;
    @BindView(R.id.tv_update_progress)
    TextView mTvProgress;

    @BindView(R.id.tv_update_title)
    TextView mTvTitle;
    @BindView(R.id.tv_update_desc)
    TextView mTvDesc;

    private IFileManager mFileManager;
    private UpdateViewModel mViewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_update);

        initView();
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.dispose();
        if (mFileManager != null) {
            if (mFileManager instanceof FabUsbFileManager) {
                mFileManager = null;
            } else {
                mFileManager.close();
            }
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_update;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mViewModel = new UpdateViewModel(getContext(), getModel());

        mViewModel.getUpdateMessageObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(message -> mTvTitle.setText(message));

        mViewModel.getUpdateProgressTextObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(message -> mTvDesc.setText(message));

        mViewModel.getUpdateProgressObservable()
                .throttleLast(50, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    mCpvProgress.setPercentage(100f * progress, true);
                    mTvProgress.setText(String.format(Locale.US, "%.0f", 100f * progress));
                });
    }

    private void initData() {
        if (getArguments() == null) return;

        String filePath = getArguments().getString("file_path");
        boolean isLocal = getArguments().getBoolean("is_local");

        if (isLocal) {
            mFileManager = new FabLocalFileManager(getContext(), getModel().getFilesDir().getPath());
            mFileManager.mount();
        } else {
            mFileManager = FabScreenApplication.getInstance().getFabUsbFileManager();
        }

        Logger.i(String.format("Opening file %s for upgrade.", filePath));
        mViewModel.startSearchingFile(mFileManager, filePath)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(file -> {
                    if (file != null) {
                        checkAvailableSpaceForUpdate(file);
                    } else {
                        Logger.e("file not exist");
                        FabConfirm.create(getContext())
                                .setCanceledOnTouchOutSide(false)
                                .setDescription(R.string.settings_update_failed_file_not_found)
                                .setConfirm(R.string.all_ok, (dialog, which) -> {
                                    dialog.dismiss();
                                    back();
                                })
                                .show();
                    }
                }, e -> {
                    LogHelper.log(e);
                    FabConfirm.create(getContext())
                            .setCanceledOnTouchOutSide(false)
                            .setDescription(R.string.settings_update_failed_file_not_found)
                            .setConfirm(R.string.all_ok, (dialog, which) -> {
                                dialog.dismiss();
                                back();
                            })
                            .show();
                });
    }

    private void checkAvailableSpaceForUpdate(IFile file) {
        mViewModel.checkAvailableSpaceForUpdate(file)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(available -> {
                    // Cache bin file and start update.
                    String updateCacheFolder = getModel().getCacheDir().getAbsolutePath() + File.separatorChar + "update";
                    mViewModel.saveUpdateFile(updateCacheFolder, file);
                    update(file);
                });
    }

    private void update(IFile file) {
        if (!mViewModel.parse(file)) {
            Logger.w("Failed to parse update file.");
            FabConfirm.create(getContext())
                    .setDescription(R.string.settings_update_failed_file_parse_error)
                    .setConfirm(R.string.all_ok, (dialog, which) -> {
                        dialog.dismiss();
                        back();
                    })
                    .show();
            return;
        }

        mViewModel.update()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        Logger.i("Upgrade successfully");

                        // save update file.
                        String updateDataFolder = getModel().getDataDir().getAbsolutePath() + File.separatorChar + "update";
                        mViewModel.saveUpdateFile(updateDataFolder, mViewModel.getUpdateFile());

                        FabConfirm.create(getContext())
                                .setDescription(R.string.settings_update_success)
                                .setConfirm(R.string.all_ok, (dialog, which) -> {
                                    dialog.dismiss();
                                    backToHome(HomeActivity.class);
                                })
                                .show();
                    } else {
                        FabAlert.alert(getContext(), R.string.settings_update_failed);
                        getModel().getPreferences().setMachineUpdatedFlag(false);
                    }
                });
    }
}
