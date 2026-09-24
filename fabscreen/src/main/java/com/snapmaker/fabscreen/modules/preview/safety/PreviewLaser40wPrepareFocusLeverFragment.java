package com.snapmaker.fabscreen.modules.preview.safety;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaser40wPrepareFocusLeverFragment extends BaseFragment {

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
        mBtnNext.setText(R.string.all_next);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_40w_focus_lever;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        PreviewActivity activity = (PreviewActivity) requireActivity();
        if (getArguments() != null) {
            boolean autoMode = getArguments().getBoolean("auto_mode");
            if (getModel().getMachineController().isRotaryModuleAvailable()) {
                activity.gotoLaserRotarySetOriginFragment(autoMode);
            } else {
                if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_40W || getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_20W) {
                    getModel().getPrintController()
                            .getHeaderSecurityStatus()
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(bindToLifecycle())
                            .subscribe(headerSecurity -> {
                                if (headerSecurity.status == 0) {
                                    activity.gotoLaser40wPrepareSetOriginFragment(autoMode);
                                }
                            });
                } else {
                    activity.gotoLaser40wPrepareSetOriginFragment(autoMode);
                }
            }
        }
    }
}
