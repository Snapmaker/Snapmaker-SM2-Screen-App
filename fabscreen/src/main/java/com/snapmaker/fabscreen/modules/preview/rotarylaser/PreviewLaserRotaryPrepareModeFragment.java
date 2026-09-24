package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.OnClick;

public class PreviewLaserRotaryPrepareModeFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.laser_choose_mode);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_mode;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_auto)
    void onClickAutoMode() {
        Logger.i("Choose auto focus mode.");
        PreviewActivity activity = (PreviewActivity) getContext();
        if (activity != null) {
            activity.gotoLaserRotarySetMaterial();
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_manual)
    void onClickManualMode() {
        Logger.i("Choose manual focus mode.");
        PreviewActivity activity = (PreviewActivity) getContext();
        if (activity != null) {
            activity.gotoLaserPrepareSafetyGogglesFragment(false);
        }
    }
}
