package com.snapmaker.fabscreen.modules.guide3dp.getstarted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guide3dp.Guide3DPActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class Guide3DPGetStartedFragment extends BaseFragment {
    public static Guide3DPGetStartedFragment newInstance() {
        return new Guide3DPGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getModel().getPreferences().getMachineSetup3DP()) {
            mBtnBack.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_3dp_get_started_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((Guide3DPActivity) getActivity()).startCalibrationIntroFragment();
    }
}
