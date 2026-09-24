package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;

public class SettingsAdvanceAirPurifierFragment extends BaseFragment {
    public static SettingsAdvanceAirPurifierFragment getInstance() {
        return new SettingsAdvanceAirPurifierFragment();
    }

    @BindView(R.id.btn_settings_air_purifier_auto_turn_on)
    Button mBtnAirPurifierAutoTurnOn;
    @BindView(R.id.btn_settings_air_purifier_auto_turn_off)
    Button mBtnAirPurifierAutoTurnOff;

    private boolean mAutoTurnOnMode;
    private boolean mAutoTurnOffMode;
    private int mHeadType = Constants.HEAD_UNPLUGGED;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_air_purifier);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_advance_air_purifier;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mHeadType = getModel().getMachineController().getHeadType();
        switch (mHeadType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                mAutoTurnOnMode = getModel().getPreferences().getAirPurifier3DPAutoFlag();
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
            case Constants.HEAD_LASER_2W_IR:
                mAutoTurnOnMode = getModel().getPreferences().getAirPurifierLaserAutoFlag();
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                mAutoTurnOnMode = getModel().getPreferences().getAirPurifierCNCAutoTurnOnFlag();
                break;
            case Constants.HEAD_UNPLUGGED:
            default:
                // unknown
                mAutoTurnOnMode = false;
                break;
        }
        mBtnAirPurifierAutoTurnOn.setActivated(mAutoTurnOnMode);

        mAutoTurnOffMode = getModel().getPreferences().getAirPurifierAutoTurnOffFlag();
        mBtnAirPurifierAutoTurnOff.setActivated(mAutoTurnOffMode);
    }

    private void setAutoTurnOnMode(boolean autoMode) {
        switch (mHeadType) {
            case Constants.HEAD_3DP:
            case Constants.HEAD_3DP_DUAL_EXTRUDER:
                getModel().getPreferences().setAirPurifier3DPAutoFlag(autoMode);
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
            case Constants.HEAD_LASER_2W_IR:
                getModel().getPreferences().setAirPurifierLaserAutoFlag(autoMode);
                break;
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
                getModel().getPreferences().setAirPurifierCNCAutoTurnOnFlag(autoMode);
                break;
            case Constants.HEAD_UNPLUGGED:
            default:
                // unknown
                break;
        }

    }

    @OnClick(R.id.btn_settings_air_purifier_auto_turn_on)
    void onClickAirPurifierAutoTurnOnMode() {
        mBtnAirPurifierAutoTurnOn.setEnabled(false);
        mAutoTurnOnMode = !mAutoTurnOnMode;
        setAutoTurnOnMode(mAutoTurnOnMode);
        mBtnAirPurifierAutoTurnOn.setActivated(mAutoTurnOnMode);
        mBtnAirPurifierAutoTurnOn.setEnabled(true);
    }

    @OnClick(R.id.btn_settings_air_purifier_auto_turn_off)
    void onClickAirPurifierAutoTurnOffMode() {
        mBtnAirPurifierAutoTurnOff.setEnabled(false);
        mAutoTurnOffMode = !mAutoTurnOffMode;
        getModel().getPreferences().setAirPurifierAutoTurnOffFlag(mAutoTurnOffMode);
        mBtnAirPurifierAutoTurnOff.setActivated(mAutoTurnOffMode);
        mBtnAirPurifierAutoTurnOff.setEnabled(true);
    }

}
