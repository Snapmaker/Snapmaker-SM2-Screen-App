package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.FabAlert;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.FabException;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MachineSettingsFragment extends BaseFragment {

    @BindView(R.id.btn_settings_machine_model_mini)
    Button mBtnMachineModelMini;
    @BindView(R.id.btn_settings_machine_model_standard)
    Button mBtnMachineModelStandard;
    @BindView(R.id.btn_settings_machine_model_plus)
    Button mBtnMachineModelPlus;

    private Map<String, Button> mButtonMap;
    private String mSelectedMachineModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("Settings");

        mButtonMap = new HashMap<>();
        mButtonMap.put(Constants.MACHINE_TYPE_A150, mBtnMachineModelMini);
        mButtonMap.put(Constants.MACHINE_TYPE_A250, mBtnMachineModelStandard);
        mButtonMap.put(Constants.MACHINE_TYPE_A350, mBtnMachineModelPlus);

        setMachineModel(getModel().getPreferences().getMachineModel());
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_setting_machine_type;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void setMachineModel(String machineModel) {
        mSelectedMachineModel = machineModel;

        for (Map.Entry<String, Button> entry : mButtonMap.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(machineModel));
        }
    }

    @OnClick(R.id.btn_settings_machine_model_mini)
    void onSelectMachineModelMini() {
        setMachineModel(Constants.MACHINE_TYPE_A150);
    }

    @OnClick(R.id.btn_settings_machine_model_standard)
    void onSelectMachineModelStandard() {
        setMachineModel(Constants.MACHINE_TYPE_A250);
    }

    @OnClick(R.id.btn_settings_machine_model_plus)
    void onSelectMachineModelPlus() {
        setMachineModel(Constants.MACHINE_TYPE_A350);
    }

    @OnClick(R.id.btn_settings_machine_type_save)
    void onSave() {
        switch (mSelectedMachineModel) {
            case Constants.MACHINE_TYPE_A150: {
                getModel().getSlaveComputer().setWorkspace(
                        167, 0, 1, -1,
                        169, 0, 1, -1,
                        150, 0, 1, -1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(this::onSaveResult, this::onSaveError);
                break;
            }
            case Constants.MACHINE_TYPE_A250: {
                getModel().getSlaveComputer().setWorkspace(
                        244, -7, -1, 1,
                        260, 0, 1, -1,
                        235, 0, 1, -1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(this::onSaveResult, this::onSaveError);
                break;
            }
            case Constants.MACHINE_TYPE_A350: {
                getModel().getSlaveComputer().setWorkspace(
                        336, -9, -1, 1,
                        360, 0, 1, -1,
                        334, 0, 1, -1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(this::onSaveResult, this::onSaveError);
                break;
            }
        }
    }

    private void onSaveResult(boolean success) {
        if (success) {
            getModel().getPreferences().setMachineModel(mSelectedMachineModel);
            back();
        } else {
            FabAlert.alert(getContext(), "Setting machine type failed.");
        }
    }

    private void onSaveError(Throwable e) {
        LogHelper.log(e);

        if (e instanceof FabException) {
            FabAlert.alert(getContext(), e.getMessage());
        } else {
            FabAlert.alert(getContext(), "Failed.");
        }
    }
}
