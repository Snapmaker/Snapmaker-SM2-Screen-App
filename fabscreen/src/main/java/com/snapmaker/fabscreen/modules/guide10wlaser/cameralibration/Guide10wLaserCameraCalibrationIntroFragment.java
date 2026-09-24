package com.snapmaker.fabscreen.modules.guide10wlaser.cameralibration;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide10wlaser.Guide10wLaserActivity;
import com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration.Guide10wThicknessMeasurementCalibrationIntroFragment;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.GuideProgressBar;

public class Guide10wLaserCameraCalibrationIntroFragment extends BaseFragment {
    public static Guide10wLaserCameraCalibrationIntroFragment newInstance() {
        return new Guide10wLaserCameraCalibrationIntroFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.view_guide_progress_bar)
    GuideProgressBar mGuideProgressBar;

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
        mIvCover.setImageResource(R.drawable.pic_guide_10w_laser_camera_calibration_360x320);

        mTvTitle.setText(R.string.laser_camera_calibration);
        mTvContent.setText(R.string.laser_camera_calibration_intro);
        mGuideProgressBar.setmStepNum(3);
        mGuideProgressBar.setmStepIndex(3);
        mGuideProgressBar.invalidate();
        mGuideProgressBar.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((Guide10wLaserActivity) getActivity()).start10wCameraCalibrationStep1Fragment();
    }

    @Override
    protected void back() {
        getFragmentManager().popBackStack(Guide10wThicknessMeasurementCalibrationIntroFragment.class.getSimpleName(), 0);
    }
}
