package com.snapmaker.fabscreen.modules.preview;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;

public class PreviewLaserPrepareModeFragment extends BaseFragment {

    private PreviewViewModel mViewModel;

    @BindView(R.id.iv_laser_prepare_mode_auto)
    ImageView mIvModeAuto;
    @BindView(R.id.iv_laser_prepare_mode_manual)
    ImageView mIvModeManual;
    @BindView(R.id.tv_laser_prepare_mode_auto_desc)
    TextView mTvModeAutoDesc;
    @BindView(R.id.tv_laser_prepare_mode_manual_desc)
    TextView mTvModeManualDesc;
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
        if (mHeadType == Constants.HEAD_LASER) {
            mIvModeAuto.setImageResource(R.drawable.pic_laser_mode_auto_240x80);
            mIvModeManual.setImageResource(R.drawable.pic_laser_mode_manual_240x80);
            mTvModeAutoDesc.setText(R.string.preview_auto_mode_desc);
            mTvModeManualDesc.setText(R.string.preview_manual_mode_desc);
        } else if (mHeadType == Constants.HEAD_LASER_10W) {
            mIvModeAuto.setImageResource(R.drawable.pic_laser_10w_mode_auto_240x80);
            mIvModeManual.setImageResource(R.drawable.pic_laser_10w_mode_manual_240x80);
            mTvModeAutoDesc.setText(R.string.preview_auto_mode_desc_10w);
            mTvModeManualDesc.setText(R.string.preview_manual_mode_desc_10w);
        }
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
        Logger.i("Choose auto focus mode.");
        PreviewActivity activity = (PreviewActivity) getContext();
        if (activity != null) {
            if (mHeadType == Constants.HEAD_LASER) {
                activity.gotoLaserPrepareMaterialFragment();
            } else if (mHeadType == Constants.HEAD_LASER_10W) {
                activity.gotoLaserPrepareModeNoteFragment(true);
            }
        }
    }

    @OnClick(R.id.btn_preview_laser_prepare_manual)
    void onClickManualMode() {
        Logger.i("Choose manual focus mode.");
        PreviewActivity activity = (PreviewActivity) requireActivity();
        if (mHeadType == Constants.HEAD_LASER) {
            activity.gotoLaserPrepareSafetyGogglesFragment(false);
        } else if (mHeadType == Constants.HEAD_LASER_10W) {
            activity.gotoLaserPrepareModeNoteFragment(false);
        }
    }
}
