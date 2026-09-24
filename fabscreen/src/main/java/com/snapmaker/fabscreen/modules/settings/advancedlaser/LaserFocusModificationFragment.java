package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LaserFocusModificationFragment extends BaseFragment {
    public static LaserFocusModificationFragment newInstance() {
        return new LaserFocusModificationFragment();
    }

    private LaserFocusPresenter mPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_focus_modification;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mPresenter = new LaserFocusPresenter(getContext(), getModel(), disposables);
        mPresenter.bind(getView());
        mPresenter.connect();
        boolean isRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();
        mPresenter.setMaxValue(isRotaryAvailable ? 150 : 40);
    }

    @OnClick(R.id.btn_laser_focus_modification_save)
    void onClickSave() {
        final float laserFocus = mPresenter.getTargetValue();
        Logger.d("Set laser focus " + laserFocus);

        getModel().getMachineController().setLaserFocus(laserFocus)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> back(), LogHelper::log);
    }
}
