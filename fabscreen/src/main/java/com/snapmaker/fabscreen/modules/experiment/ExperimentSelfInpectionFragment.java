package com.snapmaker.fabscreen.modules.experiment;

import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import fabscreen.libraries.legacy.base.BaseFragment;

public class ExperimentSelfInpectionFragment extends BaseFragment {

    @BindView(R.id.tv_experiment_self_inspection_temperature)
    TextView mTvTemperature;

    int mProtectTemperature;
    int mRecoveryTemperature;


    @OnTextChanged(R.id.ed_experiment_self_inspection_protect_temperature)
    void onProtectTemperatureChange(CharSequence protectTemperature) {
        try{
            mProtectTemperature = Integer.parseInt(protectTemperature.toString());
        }catch (Exception e){

        }

    }

    @OnTextChanged(R.id.ed_experiment_self_inspection_recovery_temperature)
    void onRecoveryTemperatureChange(CharSequence protectTemperature) {
        try {
            mRecoveryTemperature = Integer.parseInt(protectTemperature.toString());
        }catch (Exception e){

        }

    }

    @OnClick(R.id.btn_experiment_inspection_temperature_setting)
    void onSetting() {
        if (mProtectTemperature > 0 && mProtectTemperature < 100 && mRecoveryTemperature > 0 && mRecoveryTemperature < 100) {
            getModel().getSlaveComputer().setAbnormalTemperatureRange(mProtectTemperature, mRecoveryTemperature);
            mTvTemperature.setText(String.format("温度设置成功。保护温度：%s,恢复温度:%s", mProtectTemperature, mRecoveryTemperature));
        } else {
            mTvTemperature.setText(String.format("温度设置错误（0~100），请重新设置，保护温度：%s,恢复温度:%s", mProtectTemperature, mRecoveryTemperature));
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_experiment_self_inspection;
    }
}
