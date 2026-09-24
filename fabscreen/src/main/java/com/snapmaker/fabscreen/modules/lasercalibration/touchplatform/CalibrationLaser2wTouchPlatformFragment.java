package com.snapmaker.fabscreen.modules.lasercalibration.touchplatform;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;
import com.snapmaker.fabscreen.view.XYZControlPanel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class CalibrationLaser2wTouchPlatformFragment extends BaseFragment {
    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_40w_touch_platform_complete)
    Button mBtnComplete;
    @BindView(R.id.xyz_panel_touch_platform)
    XYZControlPanel mControlPanel;
    @BindView(R.id.iv_laser_40w_touch_platform)
    ImageView mIvPlatformHeight;

    private TouchPlatformViewModel mViewModel;

    public static CalibrationLaser2wTouchPlatformFragment newInstance() {
        return new CalibrationLaser2wTouchPlatformFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnComplete.setText(R.string.all_next);
        mIvPlatformHeight.setImageResource(R.drawable.pic_laser_2w_3axis_calibration_platform_height_360x320);


        mViewModel = getViewModel();
        setTitle(R.string.settings_laser_touch_platform_title);


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
        return R.layout.fragment_laser_40w_touch_platform;
    }

    @Override
    protected TouchPlatformViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(TouchPlatformViewModel.class);
    }

    @OnClick(R.id.btn_laser_40w_touch_platform_complete)
    void onCompleteClicked() {
        mViewModel.savePlatformZOffset();

        ((LaserCalibrationActivity) requireActivity()).start2wPlatformHeightPullInFocusLever();
    }
}
