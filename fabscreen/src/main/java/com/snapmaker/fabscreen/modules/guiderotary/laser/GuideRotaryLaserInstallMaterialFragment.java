package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class GuideRotaryLaserInstallMaterialFragment extends BaseFragment {
    public static GuideRotaryLaserInstallMaterialFragment newInstance() {
        return new GuideRotaryLaserInstallMaterialFragment();
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
        mIvCover.setImageResource(R.drawable.pic_guide_laser_rotary_install_material_360x320);

        mTvTitle.setText(R.string.cnc_origin_assistant_install_material_title);
        mTvContent.setText(R.string.laser_calibration_4axis_install_material_content);
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        if (getActivity() != null) {
            ((GuideRotaryLaserActivity) getActivity()).startMeasureHeightIntroFragment();
        }
    }
}
