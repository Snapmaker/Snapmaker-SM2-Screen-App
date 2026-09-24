package com.snapmaker.fabscreen.modules.guiderotary.cnc;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

import butterknife.BindView;
import butterknife.OnClick;

public class GuideRotaryCNCCompleteFragment extends BaseFragment {
    public static GuideRotaryCNCCompleteFragment newInstance() {
        return new GuideRotaryCNCCompleteFragment();
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
        mTvContent.setText(R.string.guide_cnc_rotary_complete_content);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        Logger.i("Guide Rotary CNC finished.");
        getModel().getPreferences().setMachineSetupRotaryCNC(true);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
