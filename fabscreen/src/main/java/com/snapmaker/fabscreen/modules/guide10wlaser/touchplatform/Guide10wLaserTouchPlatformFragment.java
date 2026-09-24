package com.snapmaker.fabscreen.modules.guide10wlaser.touchplatform;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.guide10wlaser.Guide10wLaserActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;
import com.snapmaker.fabscreen.view.XYZControlPanel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class Guide10wLaserTouchPlatformFragment extends BaseFragment {
    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_10w_touch_platform_complete)
    Button mBtnComplete;
    @BindView(R.id.xyz_panel_touch_platform)
    XYZControlPanel mControlPanel;
    private TouchPlatformViewModel mViewModel;

    public static Guide10wLaserTouchPlatformFragment newInstance() {
        return new Guide10wLaserTouchPlatformFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnComplete.setText(R.string.all_next);

        CoordinateSystemPresenter presenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
        presenter.ensureCoordinate(1);

        mViewModel = getViewModel();
        setTitle(R.string.settings_laser_touch_platform_title);
        presenter.setOnCoordinateSwitchListener(() -> mViewModel.initToolheadPosition());

        mControlPanel.setStepWidths(0.1f, 1f, 10f)
                .setOnDirectionClickListener((direction, stepWidth) -> {
                    mViewModel.moveXYZByStep(direction, stepWidth);
                });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mControlPanel.setEnabled(!isMoving);
                    mBtnComplete.setEnabled(!isMoving);
                });
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_touch_platform;
    }

    @Override
    protected TouchPlatformViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(TouchPlatformViewModel.class);
    }

    @OnClick(R.id.btn_laser_10w_touch_platform_complete)
    void onCompleteClicked() {
        mViewModel.savePlatformZOffset();
        mViewModel.upLiftToolhead()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (getActivity() == null) return;
                    ((Guide10wLaserActivity) getActivity()).startThicknessMeasurementCalibrationIntroFragment();
                });
    }
}