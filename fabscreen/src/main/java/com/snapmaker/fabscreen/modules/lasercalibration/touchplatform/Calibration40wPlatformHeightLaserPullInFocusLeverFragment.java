package com.snapmaker.fabscreen.modules.lasercalibration.touchplatform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class Calibration40wPlatformHeightLaserPullInFocusLeverFragment extends BaseFragment {
    public static Calibration40wPlatformHeightLaserPullInFocusLeverFragment newInstance() {
        return new Calibration40wPlatformHeightLaserPullInFocusLeverFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.btn_guide_intro_next)
    Button mBtNext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_intro;
    }

    @Override
    protected TouchPlatformViewModel getViewModel() {
        return getViewModelProvider().get(TouchPlatformViewModel.class);
    }

    private void initView() {
        Glide.with(this)
                .load(R.drawable.pic_laser_20w_focus_lever_pull_in_360x320)
                .into(mIvCover);
        mTvTitle.setText(R.string.guide_40w_laser_pull_focus_lever_title);
        mTvContent.setText(R.string.guide_40w_laser_pull_in_focus_lever_desc);
        mBtNext.setText(R.string.all_next);

    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {

        ((LaserCalibrationActivity) requireActivity()).start40wPlatformHeightComplete();

    }
}
