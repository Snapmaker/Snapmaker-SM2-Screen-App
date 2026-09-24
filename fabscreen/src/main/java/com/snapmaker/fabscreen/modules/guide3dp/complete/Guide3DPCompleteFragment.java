package com.snapmaker.fabscreen.modules.guide3dp.complete;

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

public class Guide3DPCompleteFragment extends BaseFragment {
    public static Guide3DPCompleteFragment newInstance() {
        return new Guide3DPCompleteFragment();
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
        mTvContent.setText(R.string.guide_3dp_complete_content);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        Logger.i("Guide 3DP finished.");
        getModel().getPreferences().setMachineSetup3DP(true);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
