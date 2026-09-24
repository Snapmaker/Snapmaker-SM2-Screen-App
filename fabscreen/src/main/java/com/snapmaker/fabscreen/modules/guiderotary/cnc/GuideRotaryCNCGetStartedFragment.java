package com.snapmaker.fabscreen.modules.guiderotary.cnc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class GuideRotaryCNCGetStartedFragment extends BaseFragment {
    public static GuideRotaryCNCGetStartedFragment newInstance() {
        return new GuideRotaryCNCGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getModel().getPreferences().getMachineSetupRotaryCNC()) {
            mBtnBack.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_cnc_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_cnc_get_started_next)
    void onClickNext() {
        if (getActivity() != null) {
            ((GuideRotaryCNCActivity) getActivity()).startOriginAssistantIntroFragment();
        }
    }
}
