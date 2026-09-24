package com.snapmaker.fabscreen.modules.preview.platformheight;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;
import com.snapmaker.fabscreen.view.XYZControlPanel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class PreviewLaser40wTouchPlatformFragment extends BaseFragment {

    @BindView(R.id.tv_toolhead_mask_text)
    TextView mMaskTextDesc;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_40w_touch_platform_complete)
    Button mBtnComplete;
    @BindView(R.id.xyz_panel_touch_platform)
    XYZControlPanel mControlPanel;
    @BindView(R.id.iv_laser_40w_touch_platform)
    ImageView mIvPlatformHeight;


    private TouchPlatformViewModel mViewModel;

    public static PreviewLaser40wTouchPlatformFragment newInstance() {
        return new PreviewLaser40wTouchPlatformFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnComplete.setText(R.string.all_next);
        mMaskTextDesc.setText(R.string.preview_laser_40w_prepare_low_lever_and_touch_platform_desc);
        mIvPlatformHeight.setImageResource(R.drawable.pic_laser_40w_3axis_prepare_platform_height_360x320);

        mViewModel = getViewModel();
        setTitle(R.string.all_touch_material_title);

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
        return R.layout.fragment_laser_40w_touch_platform;
    }

    @Override
    protected TouchPlatformViewModel getViewModel() {
        return getViewFragmentScopeViewModelProvider().get(TouchPlatformViewModel.class);
    }

    @OnClick(R.id.btn_laser_40w_touch_platform_complete)
    void onCompleteClicked() {
        mViewModel.setOriginZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(set -> {
                    ((PreviewActivity) requireActivity()).gotoLaser40wPullInFocusLeverFragment();
                }, LogHelper::log);
    }
}
