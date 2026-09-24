package com.snapmaker.fabscreen.modules.guiderotary.laser;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.OnClick;

public class GuideRotaryLaserSafetyGogglesFragment extends BaseFragment {
    public static GuideRotaryLaserSafetyGogglesFragment newInstance() {
        return new GuideRotaryLaserSafetyGogglesFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((GuideRotaryLaserActivity) getActivity()).startSetOriginIntroFragment();
    }
}
