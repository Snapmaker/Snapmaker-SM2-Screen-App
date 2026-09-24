package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.view.FabScreenDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.MEASURE_CAPTURE;

public class SettingsLaser10wThicknessMeasureCalibrationMeasureFragment extends BaseFragment {

    private Laser10wThicknessCalibrationViewModel mViewModel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_10w_calibration_measure)
    Button mBtnMeasure;

    private FabProgressDialog mProcessDialog;

    public static Fragment getInstance() {
        return new SettingsLaser10wThicknessMeasureCalibrationMeasureFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();

        mProcessDialog = new FabProgressDialog(requireContext());
        mProcessDialog.setMessage(R.string.settings_laser_processing_result);

        mViewModel.getCaptureResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    if (result.which == MEASURE_CAPTURE) {
//                        dismissDialog(mProcessDialog);
                        mBtnBack.setEnabled(true);
                        mBtnMeasure.setEnabled(true);
                        if (result.isSuccess) {
                            Laser10wThicknessMeasureCalibrationActivity activity = (Laser10wThicknessMeasureCalibrationActivity) requireActivity();
                            activity.gotoLaser10wThicknessMeasureCalibrationCongratulateFragment();
                        } else {
                            showMeasureFailDialog();
                        }
                    }
                });

        // auto measure for settings.
        mBtnMeasure.setEnabled(false);
        mBtnBack.setEnabled(false);
        AndroidSchedulers.mainThread().scheduleDirect(this::onCaptureClicked, 300, TimeUnit.MILLISECONDS);
    }

    private void showMeasureFailDialog() {
        FabScreenDialog.create(requireContext())
                .setIcon(R.drawable.pic_dialog_failed_72x72)
                .setTitle(R.string.all_failed)
                .setDescription(R.string.settings_laser_measure_thickness_fail_desc)
                .setConfirm(R.string.all_retry, (dialog, which) -> {
                    dialog.dismiss();
                    requireFragmentManager().popBackStack(
                            SettingsLaser10wThicknessMeasureCalibrationPointsFragment.class.getSimpleName(),
                            1
                    );
                })
                .setCancel(R.string.all_quit, (dialog, which) -> {
                    dialog.dismiss();
                    requireActivity().finish();
                })
                .show();
    }

    @Override
    protected Laser10wThicknessCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Laser10wThicknessCalibrationViewModel.class);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_10w_thickness_measure_calibration_measure;
    }

    @OnClick(R.id.btn_laser_10w_calibration_measure)
    void onCaptureClicked() {
//        showDialog(mProcessDialog);
        mBtnBack.setEnabled(false);
        mBtnMeasure.setEnabled(false);
        mBtnMeasure.setText(R.string.settings_laser_thickness_measure_calibration_measuring);
        mViewModel.captureAndCalculate(MEASURE_CAPTURE);
    }

    @Override
    protected void back() {
        mViewModel.switchAFAssistLight(false);
        requireFragmentManager().popBackStack(
                SettingsLaser10wThicknessMeasureCalibrationPointsFragment.class.getSimpleName(),
                1
        );
    }
}
