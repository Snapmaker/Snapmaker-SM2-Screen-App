package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LaserShotOutputPowerFragment extends BaseFragment {
    public static LaserShotOutputPowerFragment newInstance() {
        return new LaserShotOutputPowerFragment();
    }

    private LaserShotPowerPresenter mPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_shot_output_power_modification;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mPresenter = new LaserShotPowerPresenter(getContext(), getModel(), disposables);
        mPresenter.bind(getView());
        mPresenter.connect();
    }

    @OnClick(R.id.btn_laser_shot_output_power_save)
    void onClickSave() {
        final float laserPower = mPresenter.getTargetValue();
        Logger.d("Set low-intensity laser power " + laserPower);

        getModel().getMachineController().setLaserShotOutputPower(laserPower)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> back(), LogHelper::log);
    }
}
