package com.snapmaker.fabscreen.modules.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;

public class PreviewLaser40wPrepareModeFragment extends BaseFragment {

    private PreviewViewModel mViewModel;

    @BindView(R.id.iv_laser_prepare_mode_auto)
    ImageView mIvModeAuto;
    @BindView(R.id.iv_laser_prepare_mode_manual)
    ImageView mIvModeManual;
    @BindView(R.id.tv_laser_prepare_mode_auto_desc)
    TextView mTvModeInputMaterialThicknessDesc;
    @BindView(R.id.tv_laser_prepare_mode_manual_desc)
    TextView mTvModeFocusLeverAssistedDesc;
    @BindView(R.id.btn_preview_laser_prepare_auto)
    Button mBtnInputMaterialThickness;
    @BindView(R.id.btn_preview_laser_prepare_manual)
    Button mBtnFocusLeverAssisted;
    private int mHeadType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    private void initView() {
        setTitle(R.string.laser_choose_mode);
        mHeadType = mViewModel.getHeadType();
        mIvModeAuto.setImageResource(R.drawable.pic_laser_10w_mode_auto_240x80);
        mIvModeManual.setImageResource(R.drawable.pic_laser_10w_mode_manual_240x80);
        mTvModeInputMaterialThicknessDesc.setText(R.string.preview_40w_input_material_thickness_desc);
        mTvModeFocusLeverAssistedDesc.setText(R.string.preview_40w_focus_lever_assisted_desc);
        mBtnInputMaterialThickness.setText(R.string.preview_40w_input_material_thickness);
        mBtnFocusLeverAssisted.setText(R.string.preview_40w_focus_lever_assisted);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_mode;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_preview_laser_prepare_auto)
    void onClickAutoMode() {
        Logger.i("Choose Input Material Thickness mode.");
        PreviewActivity activity = (PreviewActivity) getContext();
        if (activity != null) {
            activity.gotoLaser40wPrepareMaterialFragment();
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_manual)
    void onClickManualMode() {
        Logger.i("Choose Focus Lever Assisted mode.");
        PreviewActivity activity = (PreviewActivity) requireActivity();
        activity.gotoLaser40wPrepareSafetyGogglesFragment(false);
    }
}
