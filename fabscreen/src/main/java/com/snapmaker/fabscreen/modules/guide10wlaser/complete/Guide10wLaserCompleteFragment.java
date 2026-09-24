package com.snapmaker.fabscreen.modules.guide10wlaser.complete;

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

public class Guide10wLaserCompleteFragment extends BaseFragment {
    public static Guide10wLaserCompleteFragment newInstance() {
        return new Guide10wLaserCompleteFragment();
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
        getModel().getPreferences().setMachineSetup10WLaser(true);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
