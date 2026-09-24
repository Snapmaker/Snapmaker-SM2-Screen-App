package com.snapmaker.fabscreen.modules.guidedualextruder.complete;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.router.Router;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class Guide3DPDualExtruderCompleteFragment extends BaseFragment {
    public static Guide3DPDualExtruderCompleteFragment newInstance() {
        return new Guide3DPDualExtruderCompleteFragment();
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
        mTvContent.setText(R.string.guide_3dp_dual_extruder_complete_content);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        Logger.i("Guide 3DP Dual Extruder finished.");
        getModel().getPreferences().setMachineSetup3DPDualExtruder(true);
        Router.getInstance().routeToHomeActivity().start(requireContext(), Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
