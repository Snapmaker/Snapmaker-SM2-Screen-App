package com.snapmaker.fabscreen.modules.guide10wlaser.thicknessmeasurementcalibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.guide10wlaser.Guide10wLaserActivity;
import com.snapmaker.fabscreen.modules.guide10wlaser.touchplatform.Guide10wLaserTouchPlatformFragmentIntroFragment;
import com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.GuideProgressBar;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.snapmaker.fabscreen.modules.settings.advancedlaser.laser10wthicknesscalibration.Laser10wThicknessCalibrationViewModel.FIRST_CAPTURE;

public class Guide10wThicknessMeasurementCalibrationIntroFragment extends BaseFragment {
    public static Guide10wThicknessMeasurementCalibrationIntroFragment newInstance() {
        return new Guide10wThicknessMeasurementCalibrationIntroFragment();
    }

    private Laser10wThicknessCalibrationViewModel mViewModel;
    @BindView(R.id.iv_guide_intro_cover)
    ImageView mIvCover;
    @BindView(R.id.tv_guide_intro_title)
    TextView mTvTitle;
    @BindView(R.id.tv_guide_intro_content)
    TextView mTvContent;
    @BindView(R.id.view_guide_progress_bar)
    GuideProgressBar mGuideProgressBar;
    @BindView(R.id.btn_guide_intro_next)
    Button mBtNext;
    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = getViewModel();
        initView();
        // We can only go ahead after finishing switching coordinate.
        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        coordinateSystemPresenter.setOnCoordinateSwitchListener(this::prepareHead);
        coordinateSystemPresenter.ensureCoordinate(1);
        setButtonsEnabled(false);
    }


    /**
     * Set proper exposeTime(now we set 1) and init camera position for distance measuring.
     * Laser point won't be on the material surface if we don't init the camera position.
     */
    private void prepareHead() {
        mViewModel.initCameraPosition(FIRST_CAPTURE)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        setButtonsEnabled(true);
                    } else {
                        // If fail, we can't start but should have ability to back.
                        mBtnBack.setEnabled(true);
                    }
                }, LogHelper::log);
    }


    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_guide_intro;
    }

    @Override
    protected Laser10wThicknessCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(Laser10wThicknessCalibrationViewModel.class);
    }

    private void initView() {
        mIvCover.setImageResource(R.drawable.pic_10w_maesure_material_thickness_360x320);
        mBtNext.setText(R.string.all_start);
        mTvTitle.setText(R.string.settings_laser_thickness_measure_calibration_title);
        mTvContent.setText(R.string.settings_laser_thickness_measure_calibration_page_desc);
        mGuideProgressBar.setmStepNum(3);
        mGuideProgressBar.setmStepIndex(2);
        mGuideProgressBar.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_guide_intro_next)
    void onClickNext() {
        mViewModel.switchAFAssistLight(true);
        if (getActivity() == null) return;
        ((Guide10wLaserActivity) getActivity()).Guide10wThicknessMeasureCalibrationPointsFragment();
    }

    private void setButtonsEnabled(boolean enabled) {
        mBtnBack.setEnabled(enabled);
        mBtNext.setEnabled(enabled);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            prepareHead();
        }
    }

    @Override
    protected void back() {
        getActivity().getSupportFragmentManager().popBackStack(Guide10wLaserTouchPlatformFragmentIntroFragment.class.getSimpleName(), 0);
    }

}
