package com.snapmaker.fabscreen.modules.guiderotary.laser;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;

public class GuideRotaryLaserGetStartedFragment extends BaseFragment {
    public static GuideRotaryLaserGetStartedFragment newInstance() {
        return new GuideRotaryLaserGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.tv_guide_laser_get_started_content)
    TextView mTvContent;
    private int mHeadType;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getModel().getPreferences().getMachineSetupRotaryLaser()) {
            mBtnBack.setVisibility(View.GONE);
        }
        mHeadType = getModel().getMachineController().getHeadType();
        switch (mHeadType) {
            case Constants.HEAD_LASER:
                mTvContent.setText(R.string.guide_laser_safety_instructions_content);
                break;
            case Constants.HEAD_LASER_2W_IR:
                mTvContent.setText(R.string.guide_2w_laser_safety_instructions_content);
                break;
            case Constants.HEAD_LASER_10W:
                mTvContent.setText(R.string.guide_10w_laser_safety_instructions_content);
                break;
            case Constants.HEAD_LASER_20W:
                mTvContent.setText(R.string.guide_20w_laser_safety_instructions_content);
                break;
            case Constants.HEAD_LASER_40W:
                mTvContent.setText(R.string.guide_40w_laser_safety_instructions_content);
                break;
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_laser_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_laser_get_started_next)
    void onClickNext() {
        if (getActivity() != null) {
            if (mHeadType == Constants.HEAD_LASER) {
                ((GuideRotaryLaserActivity) getActivity()).startLaserCalibrationIntroFragment();
            } else {
                ((GuideRotaryLaserActivity) getActivity()).startCompleteFragment();
            }
        }
    }
}
