package com.snapmaker.fabscreen.modules.guidelaser.cameracalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class GuideLaserCameraCalibrationIntroFragment extends BaseFragment {
    public static GuideLaserCameraCalibrationIntroFragment newInstance() {
        return new GuideLaserCameraCalibrationIntroFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;

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
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_guide_laser_camera_calibration_360x320);

        mTvTitle.setText(R.string.laser_camera_calibration);
        mTvContent.setText(R.string.laser_camera_calibration_intro);
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((GuideLaserActivity) getActivity()).startCameraCalibrationStep1Fragment();
    }
}
