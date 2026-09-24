package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Laser40wFireSensorSensitivityFragment extends BaseFragment {
    public static Laser40wFireSensorSensitivityFragment newInstance() {
        return new Laser40wFireSensorSensitivityFragment();
    }

    private FireSensorSensitivityPresenter mFireSensorSensitivityPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_laser_40w_fire_sensor_sensitivity;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        setTitle(R.string.settings_advance_laser_40w_fire_sensor_sensitivity);

        mFireSensorSensitivityPresenter = new FireSensorSensitivityPresenter(getContext(), getModel(), disposables);
        mFireSensorSensitivityPresenter.bind(getView());
        mFireSensorSensitivityPresenter.connect();
    }

    private void setFireSensorDisable() {
        mFireSensorSensitivityPresenter.setTargetValue(4096);
        getModel().getMachineController().setFireSensorSensitivity(4096)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::showDisableConfirmDialog, LogHelper::log);
    }

    private void showDisableConfirmDialog(boolean success) {
        FabConfirm.create(requireContext())
                .setDescription(success? R.string.dialog_laser_40w_fire_sensor_sensitivity_set_to_disable_success
                        : R.string.dialog_laser_40w_fire_sensor_sensitivity_set_to_disable_failed)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    back();
                })
                .show();
    }

    @OnClick(R.id.btn_laser_40w_fire_sensor_sensitivity_save)
    void onClickSave() {
        final int fireSensorSensitivity = (int) mFireSensorSensitivityPresenter.getTargetValue();
        Logger.d("Set Fire Sensor Sensitivity " + fireSensorSensitivity);

        getModel().getMachineController().setFireSensorSensitivity(fireSensorSensitivity)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> back(), LogHelper::log);

    }

    @OnClick(R.id.btn_laser_40w_fire_sensor_sensitivity_set_disable)
    void onClickSwitch() {
        FabConfirm.create(requireContext())
                .setDescription(R.string.dialog_laser_40w_fire_sensor_sensitivity_set_to_disable_desc)
                .setCancel(R.string.all_cancel, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .setConfirm(R.string.all_confirm, ((dialog, which) -> {
                    setFireSensorDisable();
                    dialog.dismiss();
                }))
                .show();
    }
}
