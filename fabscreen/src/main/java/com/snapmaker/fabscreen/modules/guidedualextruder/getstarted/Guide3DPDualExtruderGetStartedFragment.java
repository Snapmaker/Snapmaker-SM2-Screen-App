package com.snapmaker.fabscreen.modules.guidedualextruder.getstarted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPDualExtruderGetStartedFragment extends BaseFragment {
    public static Guide3DPDualExtruderGetStartedFragment newInstance() {
        return new Guide3DPDualExtruderGetStartedFragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (!getModel().getPreferences().getMachineSetup3DPDualExtruder()) {
            mBtnBack.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_3dp_dual_extruder_get_started;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_guide_3dp_dual_extruder_get_started_ready)
    void onClickReady() {
        Router.getInstance().routeToGuide3DPDualExtruderVerticalLevelingPage().start(requireContext());
    }
}
