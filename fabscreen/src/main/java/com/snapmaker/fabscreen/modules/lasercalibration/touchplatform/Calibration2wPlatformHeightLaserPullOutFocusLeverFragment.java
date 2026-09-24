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
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.TouchPlatformViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.view.GuideProgressBar;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Calibration2wPlatformHeightLaserPullOutFocusLeverFragment extends BaseFragment {
    public static Calibration2wPlatformHeightLaserPullOutFocusLeverFragment newInstance() {
        return new Calibration2wPlatformHeightLaserPullOutFocusLeverFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.view_guide_progress_bar)
    GuideProgressBar mGuideProgressBar;
    @BindView(R.id.btn_guide_intro_next)
    Button mBtNext;

    private TouchPlatformViewModel mTouchPlatformViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTouchPlatformViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

        CoordinateSystemPresenter presenter = new CoordinateSystemPresenter(requireContext(), getModel(), disposables);
        presenter.ensureCoordinate(1);

        presenter.setOnCoordinateSwitchListener(() -> mTouchPlatformViewModel.initToolheadPosition());
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
                .load(R.drawable.pic_laser_2w_focus_lever_pull_out_360x320)
                .into(mIvCover);

        mTvTitle.setText(R.string.guide_40w_laser_pull_focus_lever_title);
        mTvContent.setText(R.string.guide_40w_laser_pull_out_focus_lever_desc);
        mBtNext.setText(R.string.all_next);
//        mGuideProgressBar.setmStepNum(3);
//        mGuideProgressBar.setmStepIndex(1);
//        mGuideProgressBar.invalidate();
//        mGuideProgressBar.setVisibility(View.VISIBLE);

    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        getModel().getPrintController().getHeaderSecurityStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(headerSecurity -> {
                    if (headerSecurity.status == 0) {
                        if (getActivity() == null) return;
                        ((LaserCalibrationActivity) getActivity()).start2wPlatformHeightCalibration();
                    }
                });
    }
}
