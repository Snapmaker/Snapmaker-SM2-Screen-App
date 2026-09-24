package com.snapmaker.fabscreen.modules.lasercalibration.complete;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Calibration40wLaserTouchPlatformCompleteFragment extends BaseFragment {
    public static Calibration40wLaserTouchPlatformCompleteFragment newInstance() {
        return new Calibration40wLaserTouchPlatformCompleteFragment();
    }

    @BindView(R.id.tv_guide_complete_content)
    TextView mTvContent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_complete;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mTvContent.setText(R.string.laser_calibration_40w_laser_touch_platform_complete_desc);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
