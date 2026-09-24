package com.snapmaker.fabscreen.modules.settings.advancedlaser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.view.XYZControlPanel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class SettingsLaser10wTouchPlatformFragment extends BaseFragment {

    private TouchPlatformViewModel mViewModel;

    public static Fragment getInstance() {
        return new SettingsLaser10wTouchPlatformFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.xyz_panel_touch_platform)
    XYZControlPanel mControlPanel;
    @BindView(R.id.btn_laser_10w_touch_platform_complete)
    Button mBtnComplete;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = getViewModel();
        setTitle(R.string.settings_laser_touch_platform_title);

        CoordinateSystemPresenter presenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
        presenter.ensureCoordinate(1);

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
                .subscribe(success ->
                        requireFragmentManager().popBackStack(SettingsAdvancedLaser10WFragment.class.getSimpleName(), 0));
    }

    @Override
    protected void back() {
        requireFragmentManager().popBackStack(
                SettingsLaser10wToolheadFocusCalibrationFragment.class.getSimpleName(),
                1
        );
    }
}
