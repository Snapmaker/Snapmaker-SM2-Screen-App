package com.snapmaker.fabscreen.modules.experiment;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.home.HomeActivity;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Preferences;
import fabscreen.libraries.legacy.lib.file.IFileManager;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;

public class ExperimentPreferenceFragment extends BaseFragment {
    @BindView(R.id.btn_experiment_setup_flag)
    Button mBtnUpdateFlag;
    @BindView(R.id.btn_experiment_reset)
    Button mBtnReset;
    @BindView(R.id.btn_experiment_clear_update_files)
    Button mBtnClearUpdates;

    private IFileManager mFileManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean setupFlag = getModel().getPreferences().getMachineSetupFlag();
        mBtnUpdateFlag.setText(setupFlag ? "Setup State: YES" : "Setup State: No");

        mBtnReset.setText("Reset All");

        mBtnClearUpdates.setText("Clear Update Files");
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_preference;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_experiment_setup_flag)
    void onClickSetupFlag() {
        boolean setupFlag = getModel().getPreferences().getMachineSetupFlag();
        setupFlag = !setupFlag;

        getModel().getPreferences().setMachineSetupFlag(setupFlag);
        getModel().getPreferences().setMachineSetup3DP(setupFlag);
        getModel().getPreferences().setMachineSetupLaser(setupFlag);
        getModel().getPreferences().setMachineSetupCNC(setupFlag);
        getModel().getPreferences().setMachineSetup10WLaser(setupFlag);
        getModel().getPreferences().setMachineSetupRotaryLaser(setupFlag);
        getModel().getPreferences().setMachineSetupRotaryCNC(setupFlag);
        getModel().getPreferences().setMachineSetupRotary10WLaser(setupFlag);
        getModel().getPreferences().setMachineSetupLanguage(setupFlag);
        mBtnUpdateFlag.setText(setupFlag ? "Setup State: YES" : "Setup State: No");
    }

    @OnClick(R.id.btn_experiment_reset)
    void onClickReset() {
        Preferences preferences = getModel().getPreferences();
        // Package versions should not be cleared for backup
        String packageVersion = preferences.getLastUpdatePackageVersion();
        preferences.reset();

        getModel().getPreferences().setLastUpdatePackageVersion(packageVersion);
        // Machine not restarted. Need to obtain the machine model again
        switch (getModel().getMachineController().getMachineModel()) {
            case Constants.MACHINE_MODEL_SNAPMAKER_A150: {
                preferences.setMachineModel(Constants.MACHINE_TYPE_A150);
                break;
            }
            case Constants.MACHINE_MODEL_SNAPMAKER_A250: {
                preferences.setMachineModel(Constants.MACHINE_TYPE_A250);
                break;
            }
            case Constants.MACHINE_MODEL_SNAPMAKER_A350: {
                preferences.setMachineModel(Constants.MACHINE_TYPE_A350);
                break;
            }
            case Constants.MACHINE_MODEL_UNKNOWN: {
                // todo user modified model
                break;
            }
            default:
                break;
        }

        backToHome(HomeActivity.class);
    }

    @OnClick(R.id.btn_experiment_clear_update_files)
    void onClickClearUpdates() {
        String updateFolderPath = getModel().getDataDir().getAbsolutePath() + File.separatorChar + "update";
        File updateFolder = new File(updateFolderPath);
        if (updateFolder.exists()) {
            File[] files = updateFolder.listFiles();
            for (File file : files) {
                file.delete();
            }
            FabConfirm.create(getContext())
                    .setDescription(R.string.all_done)
                    .setConfirm(R.string.all_confirm, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        } else {
            FabAlert.alert(getContext(), "Folder not existed!");
        }
    }
}
