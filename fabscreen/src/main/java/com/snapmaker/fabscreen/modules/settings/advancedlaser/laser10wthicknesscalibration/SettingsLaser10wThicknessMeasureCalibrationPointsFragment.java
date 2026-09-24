package com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration;

import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.FIRST_CAPTURE;
import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.SECOND_CAPTURE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.ToastHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabScreenDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;

@SuppressLint("NonConstantResourceId")
public class SettingsLaser10wThicknessMeasureCalibrationPointsFragment extends BaseFragment {

    private Laser10wThicknessCalibrationViewModel mViewModel;

    @Laser10wThicknessCalibrationViewModel.CaptureCount
    private int mCaptureCount = FIRST_CAPTURE;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.iv_laser_10w_calibration_point_example)
    ImageView mIvPointExample;
    @BindView(R.id.tv_laser_10w_calibration_point_count)
    TextView mTvPointCount;
    @BindView(R.id.btn_laser_10w_calibration_capture)
    Button mBtnCapture;
    private FabProgressDialog mProcessDialog;

    public static Fragment getInstance() {
        return new SettingsLaser10wThicknessMeasureCalibrationPointsFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();

        mProcessDialog = new FabProgressDialog(requireContext());
        mProcessDialog.setMessage(R.string.settings_laser_process_capture_result);

        mViewModel.getCaptureResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(this::onGetCaptureResult);
    }

    private void onGetCaptureResult(CalibrationCaptureResult result) {
        if (result.which != FIRST_CAPTURE && result.which != SECOND_CAPTURE) return;
//        dismissDialog(mProcessDialog);
        if (result.isSuccess) {
//            showSuccessToast();
            // Now init position for next capture
            mViewModel.initCameraPosition(mCaptureCount + 1)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
//                        setButtonsEnabled(true);
                        if (!success) return;
                        if (result.which == FIRST_CAPTURE) {
                            mCaptureCount = SECOND_CAPTURE;
                            mTvPointCount.setText(R.string.settings_laser_2nd_calibration_point);
                            mIvPointExample.setImageResource(R.drawable.pic_laser_10w_settings_2nd_calibration_point_240x240);

                            // Run second capture automatically.
                            AndroidSchedulers.mainThread().scheduleDirect(this::onCaptureClicked, 300, TimeUnit.MILLISECONDS);
                        } else if (result.which == SECOND_CAPTURE) {
                            Laser10wThicknessMeasureCalibrationActivity activity = (Laser10wThicknessMeasureCalibrationActivity) requireActivity();
                            activity.gotoLaser10wThicknessMeasureCalibrationMeasureFragment();
                        }
                    });
        } else {
            showCaptureFailDialog();
        }
    }

    private void showSuccessToast() {
        new ToastHelper.Builder()
                .setDrawable(R.drawable.pic_dialog_success_72x72)
                .setMessage(R.string.settings_laser_process_capture_result_success)
                .setShowTime(1000)
                .setYMargin(30)
                .build()
                .showToast(requireContext());
    }

    private void showCaptureFailDialog() {
        FabScreenDialog.create(requireContext())
                .setIcon(R.drawable.pic_dialog_failed_72x72)
                .setTitle(R.string.all_failed)
                .setDescription(R.string.settings_laser_process_capture_result_fail)
                .setConfirm(R.string.all_retry, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.switchAFAssistLight(false);
                    // Pop back to intro fragment. Intro fragment is the root fragment, which is not
                    // listed in the back stack(in addFragment() method we can see that), so we do
                    // the popping back as below.
                    requireFragmentManager().popBackStack(
                            SettingsLaser10wThicknessMeasureCalibrationPointsFragment.class.getSimpleName(),
                            1);
                })
                .setCancel(R.string.all_quit, (dialog, which) -> {
                    dialog.dismiss();
                    mViewModel.switchAFAssistLight(false);
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
        return R.layout.fragment_laser_10w_thickness_measure_calibration_points;
    }

    @OnClick(R.id.btn_laser_10w_calibration_capture)
    void onCaptureClicked() {
//        showDialog(mProcessDialog);
        setButtonsEnabled(false);
        mBtnCapture.setText(R.string.settings_laser_thickness_measure_calibration_capturing);
        mViewModel.captureAndCalculate(mCaptureCount);
    }

    @Override
    protected void back() {
        if (mCaptureCount == SECOND_CAPTURE) {
            setButtonsEnabled(false);
            mViewModel.initCameraPosition(FIRST_CAPTURE)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        setButtonsEnabled(true);
                        if (!success) return;
                        mCaptureCount = FIRST_CAPTURE;
                        mTvPointCount.setText(R.string.settings_laser_1st_calibration_point);
                        mIvPointExample.setImageResource(R.drawable.pic_laser_10w_settings_1st_calibration_point_240x240);
                    });
        } else {
            mViewModel.switchAFAssistLight(false);
            super.back();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        mBtnBack.setEnabled(enabled);
        mBtnCapture.setEnabled(enabled);
    }
}
