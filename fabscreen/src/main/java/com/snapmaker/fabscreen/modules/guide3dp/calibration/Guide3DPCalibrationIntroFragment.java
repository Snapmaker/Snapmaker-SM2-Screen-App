package com.snapmaker.fabscreen.modules.guide3dp.calibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.calibration.CalibrationViewModel;
import com.snapmaker.fabscreen.modules.guide3dp.Guide3DPActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPCalibrationIntroFragment extends BaseFragment {

    private CalibrationViewModel mViewModel;

    public static Guide3DPCalibrationIntroFragment newInstance() {
        return new Guide3DPCalibrationIntroFragment();
    }

    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.btn_guide_intro_next)
    Button mBtnIntroNext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
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
    protected CalibrationViewModel getViewModel() {
        return getViewModelProvider().get(CalibrationViewModel.class);
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_guide_3dp_calibration_360x320);
        mTvTitle.setText(R.string.calibration_intro_calibrate_the_bed);
        mTvContent.setText(R.string.guide_3dp_calibration_content);
        mBtnIntroNext.setText(R.string.all_start);
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((Guide3DPActivity) getActivity()).startCalibrationFragment();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Triggered when back from other fragments.
            // This ensures that, every time we click "Start" at this page, it is a fresh new "Start".
            mViewModel.resetBehaviors();
            mViewModel.clearCalibrationPointData(false);
        }
    }
}
