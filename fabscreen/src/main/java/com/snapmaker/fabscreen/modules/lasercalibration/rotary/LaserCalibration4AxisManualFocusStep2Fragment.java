package com.snapmaker.fabscreen.modules.lasercalibration.rotary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;

import butterknife.BindView;
import fabscreen.libraries.legacy.lib.LaserFineTuneExecutor;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibration4AxisManualFocusStep2Fragment extends BaseFragment {
    private final static String TAG = LaserCalibration4AxisManualFocusStep2Fragment.class.getSimpleName();

    private final static int STATUS_IDLE = 0;
    private final static int STATUS_LASER_TEST = 1;
    private final static int STATUS_COMPLETE = 2;
    private final static int STATUS_ERROR = 3;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.tv_guide_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_content)
    TextView mTvContent;
    @BindView(R.id.btn_guide_next)
    Button mBtnComplete;

    private LaserCalibrationViewModel mViewModel;

    private BehaviorSubject<Integer> mCalibrationStatusSubject = BehaviorSubject.createDefault(STATUS_IDLE);

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
        return R.layout.fragment_guide_laser_auto_focus_step2;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    private void initView() {
        setTitle(R.string.laser_calibration_manual_focus);

        mTvTitle.setText(R.string.laser_calibration_manual_focus);
        mTvContent.setText(R.string.laser_calibration_engraving_notice);
        mBtnComplete.setVisibility(Button.GONE);

        mCalibrationStatusSubject
                .as(bindToLifecycle())
                .subscribe(status -> {
                    switch (status) {
                        case STATUS_IDLE:
                        case STATUS_LASER_TEST:
                            mBtnBack.setVisibility(View.GONE);
                            mBtnComplete.setVisibility(View.VISIBLE);
                            mBtnComplete.setEnabled(false);
                            mBtnComplete.setText(R.string.laser_calibration_processing);
                            break;
                        case STATUS_ERROR:
                            mBtnBack.setVisibility(Button.VISIBLE);
                            break;
                        default:
                            break;
                    }
                });

        startCalibration();
    }

    private void startCalibration() {
        mCalibrationStatusSubject.onNext(STATUS_LASER_TEST);
        LaserFineTuneExecutor fineTuneExecutor = new LaserFineTuneExecutor(getModel(), disposables);
        fineTuneExecutor.setLaserPattern(mViewModel.getLaserPattern());
        fineTuneExecutor.startFineTune()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        LaserCalibrationActivity activity = (LaserCalibrationActivity) getContext();
                        if (activity != null) {
                            activity.goto4AxisManualFineTunePick();
                        }
                    } else {
                        mCalibrationStatusSubject.onNext(STATUS_ERROR);
                    }
                }, e -> {
                    LogHelper.log(e);
                    mCalibrationStatusSubject.onNext(STATUS_ERROR);
                });
    }
}
