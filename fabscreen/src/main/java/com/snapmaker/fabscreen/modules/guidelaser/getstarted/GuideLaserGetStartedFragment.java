package com.snapmaker.fabscreen.modules.guidelaser.getstarted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class GuideLaserGetStartedFragment extends BaseFragment {
    public static GuideLaserGetStartedFragment newInstance() {
        return new GuideLaserGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!getModel().getPreferences().getMachineSetupLaser()) {
            mBtnBack.setVisibility(View.GONE);
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
            ((GuideLaserActivity) getActivity()).startPrepareMaterialIntroFragment();
        }
    }
}
