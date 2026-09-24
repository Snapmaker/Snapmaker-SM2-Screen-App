package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Settings10wCameraCalibrationStep2Fragment extends BaseFragment {
    public static Settings10wCameraCalibrationStep2Fragment newInstance() {
        return new Settings10wCameraCalibrationStep2Fragment();
    }

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.tv_settings_camera_calibration_content)
    TextView mTvContent;

    @BindView(R.id.btn_settings_camera_calibration_complete)
    Button mBtnComplete;
    @BindView(R.id.iv_settings_camera_calibration)
    ImageView mIvCover;

    private Settings10wCameraCalibrationViewModel mViewModel;

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

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_camera_calibration_step2;
    }

    @Override
    protected Settings10wCameraCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Settings10wCameraCalibrationViewModel.class);
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_10w_laser_camera_calibration_240x160);
        // Start camera calibration procedure
        mViewModel.getCalibrationStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(status -> {
                    switch (status) {
                        case Settings10wCameraCalibrationViewModel.STATUS_IDLE:
                            mBtnBack.setVisibility(ImageButton.VISIBLE);
                            mBtnComplete.setEnabled(false);
                            mTvContent.setText(R.string.laser_camera_calibration_step2_desc);
                            break;
                        case Settings10wCameraCalibrationViewModel.STATUS_PROCESSING:
                            mBtnBack.setVisibility(ImageButton.GONE);
                            mBtnComplete.setEnabled(false);
                            mBtnComplete.setText(R.string.laser_calibration_processing);
                            break;
                        case Settings10wCameraCalibrationViewModel.STATUS_COMPLETE:
                            mBtnBack.setVisibility(ImageButton.VISIBLE);
                            mBtnComplete.setEnabled(true);
                            mBtnComplete.setText(R.string.all_complete);
                            break;
                        case Settings10wCameraCalibrationViewModel.STATUS_ERROR:
                            mBtnBack.setVisibility(ImageButton.VISIBLE);
                            mBtnComplete.setEnabled(true);
                            mBtnComplete.setText(R.string.all_failed);
                            break;
                        default:
                            break;
                    }
                });

        mViewModel.getProcessProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(index -> {
                    if (index != 0) {
                        mTvContent.setText(getString(R.string.laser_camera_calibration_step2_desc2, index));
                    } else {
                        mTvContent.setText(R.string.laser_camera_calibration_step2_desc);
                    }
                });

        mViewModel.start();
    }

    @OnClick(R.id.btn_settings_camera_calibration_complete)
    void onClickNext() {
        if (getActivity() == null) return;
        ((SettingsActivity) getActivity()).popSettingsPage();
    }
}
