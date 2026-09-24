package com.snapmaker.fabscreen.modules.preview.safety;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class PreviewCNCPrepareSafetyGogglesFragment extends BaseFragment {
    public static PreviewCNCPrepareSafetyGogglesFragment newInstance() {
        return new PreviewCNCPrepareSafetyGogglesFragment();
    }

    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_title)
    TextView mTvTitle;
    @BindView(R.id.tv_preview_laser_prepare_safety_goggles_message)
    TextView mTvContent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_safety_goggles;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mTvTitle.setText(R.string.all_safety_goggles);
        mTvContent.setText(R.string.guide_cnc_safety_goggles_content);
    }

    @OnClick(R.id.btn_preview_laser_prepare_safety_goggles_next)
    void onClickNext() {
        if (getActivity() != null) {
            if (getModel().getMachineController().isRotaryModuleAvailable()) {
                ((PreviewActivity) getActivity()).gotoCNCPrepareRotarySetOriginFragment();
            } else {
                ((PreviewActivity) getActivity()).gotoLaserPrepareSetOriginFragment(false);
            }
        }
    }
}
