package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.manual;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.data.XYZMoveController;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.DualExtruderBedLevelingViewModel;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.DualExtruderBedLevellingActivity;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import butterknife.BindView;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ManualBedLevelingFragment extends BaseFragment {
    public static Fragment newInstance() {
        return new ManualBedLevelingFragment();
    }

    @BindView(R.id.tv_cur_work_status)
    TextView mTvCurWorkStatus;
    @BindView(R.id.tv_leveling_progress)
    TextView mTvLevelingProgress;
    @BindView(R.id.tv_caution_msg)
    TextView mTvCautionMsg;
    @BindView(R.id.sbg_step_width)
    SegmentedButtonGroup mSbgStepWidth;
    @BindView(R.id.btn_up)
    Button mBtnUp;
    @BindView(R.id.btn_down)
    Button mBtnDown;
    @BindView(R.id.btn_next)
    Button mBtnNext;

    private ManualBedLevelingViewModel mFragmentViewModel;
    private DualExtruderBedLevelingViewModel mActivityViewModel;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_manual_leveling;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentViewModel = getFragmentScopeViewModel(ManualBedLevelingViewModel.class);
        mActivityViewModel = getActivityScopeViewModel(DualExtruderBedLevelingViewModel.class);
        mFragmentViewModel.startLeveling(mActivityViewModel.getSelectedGrid());
        initView();
    }

    private void initView() {
        setTitle(R.string.leveling_manual_title);

        mSbgStepWidth.setPosition(mFragmentViewModel.getStepPos(), false);
        mSbgStepWidth.setOnPositionChanged(position -> mFragmentViewModel.setStepWidthPos(position));

        FabProgressDialog progressDialog = new FabProgressDialog(requireContext());
        progressDialog.setMessage(R.string.dialog_warning_moving);

        mFragmentViewModel.getProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::refreshView, LogHelper::log);

        mFragmentViewModel.getTravellingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isTravel -> {
                    if (isTravel) {
                        showDialog(progressDialog);
                    } else {
                        dismissDialog(progressDialog);
                    }
                }, LogHelper::log);

        mFragmentViewModel.getJoggingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isJogging -> {
                    mBtnUp.setEnabled(!isJogging);
                    mBtnDown.setEnabled(!isJogging);
                    if (!isJogging) {
                        // enable next when jog finish.
                        mBtnNext.setEnabled(true);
                    }
                }, LogHelper::log);
    }

    private void refreshView(int progress) {
        if (progress == -2) {
            ((DualExtruderBedLevellingActivity) requireActivity()).goManualLevelComplete();
        } else if (progress == -1) {
            mBtnNext.setText(R.string.all_save);
            mBtnNext.setBackgroundResource(R.drawable.all_button_round_blue);
        } else {
            mBtnNext.setText(R.string.all_next);
            mBtnNext.setBackgroundResource(R.drawable.all_button_round_default2);
            mTvLevelingProgress.setText(getString(R.string.leveling_progress_desc, progress, mActivityViewModel.getLevelingTotalPoints()));
        }
    }

    @OnClick(R.id.btn_up)
    void onUpClicked() {
        mFragmentViewModel.moveZ(XYZMoveController.Direction.UP);
    }

    @OnClick(R.id.btn_down)
    void onDownClicked() {
        mFragmentViewModel.moveZ(XYZMoveController.Direction.DOWN);
    }

    @OnClick(R.id.btn_next)
    void onNextClicked() {
        if (mFragmentViewModel.getProgress() == -1) {
            mFragmentViewModel.saveLeveling();
        } else {
            mBtnNext.setEnabled(false);
            mFragmentViewModel.goToNextPoint();
        }
    }

    @Override
    protected void back() {
        // alert
        FabConfirm.create(requireContext())
                .setIcon(R.drawable.pic_dialog_warning_72x72)
                .setDescription(R.string.leveling_stop_leveling_dialog_msg)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    stopLevelingAndBack();
                })
                .setCancel(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void stopLevelingAndBack() {
        FabProgressDialog dialog = new FabProgressDialog(requireContext());
        dialog.setMessage(R.string.leveling_stop_resetting_dialog_msg);

        mFragmentViewModel.stopLeveling()
                .doOnSubscribe(disposable -> dialog.show())
                .doOnNext(success -> dialog.dismiss())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> super.back(), LogHelper::log);

    }
}
