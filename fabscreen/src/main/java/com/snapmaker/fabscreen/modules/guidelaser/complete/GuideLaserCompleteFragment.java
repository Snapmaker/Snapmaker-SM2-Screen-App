package com.snapmaker.fabscreen.modules.guidelaser.complete;

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

public class GuideLaserCompleteFragment extends BaseFragment {
    public static GuideLaserCompleteFragment newInstance() {
        return new GuideLaserCompleteFragment();
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
        mTvContent.setText(R.string.guide_laser_complete_content);
    }

    @OnClick(R.id.btn_guide_complete_next)
    void onClickNext() {
        Logger.i("Guide Laser finished.");
        getModel().getPreferences().setMachineSetupLaser(true);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
