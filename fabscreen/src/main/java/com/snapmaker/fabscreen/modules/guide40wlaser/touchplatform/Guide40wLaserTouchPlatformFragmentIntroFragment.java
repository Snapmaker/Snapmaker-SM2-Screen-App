package com.snapmaker.fabscreen.modules.guide40wlaser.touchplatform;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide40wlaser.Guide40wLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.view.GuideProgressBar;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide40wLaserTouchPlatformFragmentIntroFragment extends BaseFragment {
    public static Guide40wLaserTouchPlatformFragmentIntroFragment newInstance() {
        return new Guide40wLaserTouchPlatformFragmentIntroFragment();
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
        mIvCover.setImageResource(R.drawable.pic_laser_40w_3axis_prepare_platform_height_intro_360x320);

        mTvTitle.setText(R.string.settings_laser_platform_height_calibration_title);
        mTvContent.setText(R.string.settings_laser_platorm_height_calibration_desc);
        mBtNext.setText(R.string.all_start);
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
                        ((Guide40wLaserActivity) getActivity()).startPullOutFocusLeverFragment();
                    }
                });
    }
}
