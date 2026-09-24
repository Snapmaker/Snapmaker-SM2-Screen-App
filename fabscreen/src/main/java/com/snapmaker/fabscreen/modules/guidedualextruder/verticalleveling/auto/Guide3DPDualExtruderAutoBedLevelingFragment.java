package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.GuideDualExtruderVerticalLevelingViewModel;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.GuideDualExtruderVerticalLevellingActivity;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Guide3DPDualExtruderAutoBedLevelingFragment extends BaseFragment {

    private Guide3DPDualExtruderAutoBedLevelingViewModel mFragmentViewModel;
    private GuideDualExtruderVerticalLevelingViewModel mActivityViewModel;

    public static Fragment newInstance() {
        return new Guide3DPDualExtruderAutoBedLevelingFragment();
    }

    @BindView(R.id.tv_cur_work_status)
    TextView mTvCurWorkStatus;
    @BindView(R.id.tv_leveling_progress)
    TextView mTvLevelingProgress;
    @BindView(R.id.btn_complete)
    Button mBtnComplete;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_auto_leveling;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityViewModel = getActivityScopeViewModel(GuideDualExtruderVerticalLevelingViewModel.class);
        mFragmentViewModel = getFragmentScopeViewModel(Guide3DPDualExtruderAutoBedLevelingViewModel.class);

        mActivityViewModel.setSelectedGridPosition(1);
        mFragmentViewModel.startLeveling(mActivityViewModel.getSelectedGrid());
        initView();
    }

    private void initView() {
        mFragmentViewModel.getProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(progress -> {
                    if (progress == -1) {
                        ((GuideDualExtruderVerticalLevellingActivity) requireActivity()).goZLeveling();
                    }
                    refreshView(progress);
                }, LogHelper::log);

        mFragmentViewModel.getCalibrationResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    FabConfirm errorDialog = FabConfirm.create(requireContext());
                    String content = "";
                    boolean isSuccess = false;
                    switch (result) {
                        case 0x00:
                            isSuccess = true;
                            break;
                        case 0x01:
                            Logger.w("Probe sensor not triggered, result " + result);
                            content = "Auto Bed Leveling failed during probing heated bed. result " + result;
                            break;
                        case 0x02:
                            Logger.w("Probe point params error, result " + result);
                            content = "Auto Bed Leveling failed during probing heated bed. result " + result;
                            break;
                        case 0x07:
                            Logger.w("Hardware abnormal detected, result " + result);
                            content = "Auto Bed Leveling failed during probing heated bed. result " + result;
                            break;
                        case 0x08:
                            Logger.w("Probe data is invalid, result " + result);
                            content = requireContext().getResources().getString(R.string.dialog_dual_extruder_heated_bed_leveling_data_invalid_content);
                            break;
                        default:
                            Logger.w("Auto Bed Leveling failed during probing, result " + result);
                            content = "Auto Bed Leveling failed during probing heated bed. result " + result;
                            break;
                    }
                    if (!isSuccess) {
                        errorDialog.setIcon(R.drawable.pic_dialog_warning_72x72)
                                .setDescription(content)
                                .setConfirm(R.string.all_ok, ((dialog, which) -> {
                                    dialog.dismiss();
                                    stopLevelingAndBack();
                                }))
                                .show();
                    }
                });
    }

    private void refreshView(int progress) {
        if (progress != -1) {
            // leveling
            mTvLevelingProgress.setText(getString(R.string.leveling_progress_desc, progress, mActivityViewModel.getLevelingTotalPoints()));
            mTvCurWorkStatus.setText(R.string.dual_extruder_auto_bed_leveling_title);
            mBtnComplete.setText(R.string.all_leveling);
            mBtnComplete.setEnabled(false);
        } else {
            // complete
            mTvCurWorkStatus.setText(R.string.dual_extruder_bed_leveling_complete);
            mBtnComplete.setText(R.string.all_complete);
            mBtnComplete.setEnabled(true);
            if (mBtnTopBarBack == null) return;
            mBtnTopBarBack.setVisibility(View.INVISIBLE);
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
