package com.snapmaker.fabscreen.modules.preview;

import static fabscreen.libraries.legacy.data.Constants.MACHINE_MODEL_SNAPMAKER_A150;
import static fabscreen.libraries.legacy.data.Constants.MACHINE_TYPE_A150;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PreviewLaserPrepareModeNoteFragment extends BaseFragment {

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_prepare_start_measure)
    Button mBtnStart;
    @BindView(R.id.iv_prepare_mode)
    ImageView mIvMode;
    @BindView(R.id.tv_prepare_mode_note_title)
    TextView mTvNoteTitle;
    @BindView(R.id.tv_prepare_mode_note_desc)
    TextView mTvNoteDesc;
    private boolean mIsAutoMode;
    private PreviewViewModel mViewModel;
    private PreviewActivity mActivity;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = getViewModel();
        mActivity = (PreviewActivity) requireActivity();

        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        coordinateSystemPresenter.ensureCoordinate(1);

        initView();

        mViewModel.getCameraMoveObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(settled -> {
                    if (!settled) return;
                    mActivity.gotoLaserPrepareMeasureThicknessFragment();
                });

        mViewModel.getIsMovingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnBack.setEnabled(!isMoving);
                    mBtnStart.setEnabled(!isMoving);
                });
    }

    private void initView() {
        mIsAutoMode = requireArguments().getBoolean("auto_mode");
        mIvMode.setImageResource(mIsAutoMode ? R.drawable.pic_laser_10w_mode_auto_note_240x160 : R.drawable.pic_laser_10w_mode_manual_note_240x160);
        mTvNoteTitle.setText(mIsAutoMode ? R.string.preview_auto_mode_note_title : R.string.preview_manual_mode_note_title);

        int machineModel = getModel().getMachineController().getMachineModel();
        int autoNoteResId = (machineModel == MACHINE_MODEL_SNAPMAKER_A150) ?
                R.string.preview_auto_mode_note_a150_desc :
                R.string.preview_auto_mode_note_desc;
        mTvNoteDesc.setText(mIsAutoMode ? autoNoteResId : R.string.preview_manual_mode_note_desc);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_prepare_mode_mote;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @OnClick(R.id.btn_prepare_start_measure)
    void onStartMeasureClick() {
        if (mIsAutoMode) {
            getModel().getPrintController().getHeaderSecurityStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(headerSecurity -> {
                        if (headerSecurity.status == 0) {
                            mViewModel.initCameraPosition(false);
                        }
                    }, LogHelper::log);
        } else {
            mActivity.gotoLaserPrepareTouchMaterialFragment();
        }
    }

    @Override
    protected void back() {
        mViewModel.switchAFAssistLight(false);
        requireFragmentManager().popBackStack(PreviewLaserPrepareModeFragment.class.getSimpleName(), 0);
    }
}
