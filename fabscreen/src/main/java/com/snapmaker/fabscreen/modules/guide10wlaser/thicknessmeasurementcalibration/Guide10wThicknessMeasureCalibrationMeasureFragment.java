package com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guide10wlaser.Guide10wLaserActivity;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel;
import com.snapmaker.fabscreen.view.FabProgressDialog;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.ToastHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.FabScreenDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.MEASURE_CAPTURE;

public class Guide10wThicknessMeasureCalibrationMeasureFragment extends BaseFragment {

    private Laser10wThicknessCalibrationViewModel mViewModel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_laser_10w_calibration_measure)
    Button mBtnMeasure;

    private FabProgressDialog mProcessDialog;

    public static Guide10wThicknessMeasureCalibrationMeasureFragment newInstance() {
        return new Guide10wThicknessMeasureCalibrationMeasureFragment();
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
                        mViewModel.switchAFAssistLight(false);
                        if (result.isSuccess) {
                            new ToastHelper.Builder()
                                    .setDrawable(R.drawable.pic_dialog_success_72x72)
                                    .setTitle(R.string.all_successful)
                                    .setMessage(R.string.settings_laser_process_capture_result_guide_success)
                                    .setShowTime(1500)
                                    .build()
                                    .showToast(requireContext());
                            AndroidSchedulers.mainThread().scheduleDirect(() -> {
                                if (getActivity() == null) return;
                                ((Guide10wLaserActivity) getActivity()).start10wCameraCalibrationIntroFragment();
                            }, 1500, TimeUnit.MILLISECONDS);
                        } else {
                            mBtnBack.setEnabled(true);
                            mBtnMeasure.setEnabled(true);
                            showMeasureFailDialog();
                        }
                    }
                });

        // auto measure for Guide.
        mBtnMeasure.setEnabled(false);
        mBtnBack.setEnabled(false);
        AndroidSchedulers.mainThread().scheduleDirect(this::onCaptureClicked, 300, TimeUnit.MILLISECONDS);

    }

    private void showMeasureFailDialog() {
        FabScreenDialog.create(requireContext())
                .setIcon(R.drawable.pic_dialog_failed_72x72)
                .setTitle(R.string.all_failed)
                .setDescription(R.string.guide_laser_measure_thickness_fail_desc)
                .setConfirm(R.string.all_retry, (dialog, which) -> {
                    dialog.dismiss();
                    back();
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

    @OnClick(R.id.top_bar_back)
    protected void onBackClicked() {
        back();
    }

    @Override
    protected void back() {
        mViewModel.switchAFAssistLight(false);
        getFragmentManager().popBackStack(Guide10wThicknessMeasurementCalibrationIntroFragment.class.getSimpleName(), 0);
    }
}
