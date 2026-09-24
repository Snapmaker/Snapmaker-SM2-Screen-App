package com.snapmaker.fabscreen.modules.preview.safety;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaser40wPrepareSafetyGogglesFragment extends BaseFragment {

    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_message)
    TextView mTvMessage;
    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_title)
    TextView mTvTitle;
    @BindView(R.id.btn_preview_laser_prepare_safety_goggles_next)
    Button mBtnNext;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
//        coordinateSystemPresenter.ensureCoordinate(1);

        int headType = getModel().getMachineController().getHeadType();
        if (headType == Constants.HEAD_LASER_40W) {
            // 40w
            mTvMessage.setText(R.string.laser_40w_safety_goggles_message);
            mTvTitle.setText(R.string.laser_safety_goggles);
            mBtnNext.setText(R.string.all_next);
        } else {
            // 20w
            mTvMessage.setText(R.string.laser_20w_safety_goggles_message);
            mTvTitle.setText(R.string.laser_safety_goggles);
            mBtnNext.setText(R.string.all_next);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        PreviewActivity activity = (PreviewActivity) requireActivity();
        if (getArguments() == null) {
            Logger.e("Could not get arguments.");
            return;
        }
        boolean autoMode = getArguments().getBoolean("auto_mode");
        if (getModel().getMachineController().isRotaryModuleAvailable()) {
            activity.gotoLaserRotarySetOriginFragment(autoMode);
            return;
        }

        getModel().getPrintController().getHeaderSecurityStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(headerSecurity -> {
                    if (headerSecurity.status == 0) {
                        if (autoMode) {
                            activity.gotoLaser40wPrepareSetOriginFragment(autoMode);
                        } else {
                            activity.gotoLaser40wTouchPlatformFragment();
                        }
                    } else {
                        Logger.e("Laser Security error %d", headerSecurity.status);
                    }
                });
    }
}
