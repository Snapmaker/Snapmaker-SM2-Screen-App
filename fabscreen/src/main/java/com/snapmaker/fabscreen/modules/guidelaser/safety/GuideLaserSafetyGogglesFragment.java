package com.snapmaker.fabscreen.modules.guidelaser.safety;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class GuideLaserSafetyGogglesFragment extends BaseFragment {
    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_message)
    TextView mTvContent;

    public static GuideLaserSafetyGogglesFragment newInstance() {
        return new GuideLaserSafetyGogglesFragment();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTvContent.setText(R.string.laser_safety_goggles_message);
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        if (getActivity() == null) return;
        ((GuideLaserActivity) getActivity()).startSetOriginIntroFragment();
    }
}
