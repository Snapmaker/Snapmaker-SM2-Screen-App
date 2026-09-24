package com.snapmaker.fabscreen.modules.lasercalibration.rotary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZBGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.Laser4AxisSetOriginPagerPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.model.LaserPattern;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class LaserCalibration4AxisSetOriginFragment extends BaseFragment {
    public static LaserCalibration4AxisSetOriginFragment newInstance() {
        return new LaserCalibration4AxisSetOriginFragment();
    }

    @BindView(R.id.btn_laser_calibration_4axis_set_origin_next)
    Button mBtnNext;
    @BindView(R.id.vp_laser_calibration_4axis_control_panels)
    ViewPager mVpControlPanels;
    @BindView(R.id.tl_laser_calibration_4axis_control_panel_indicator)
    TabLayout mTlControlPanel;

    private List<View> mViews;
    private ControlPanelAdapter mPanelAdapter;
    private LaserCalibrationViewModel mViewModel;

    private CoordinateXYZBGeminiWidgetPresenter mCoordinateWidgetPresenter;
    // linear control
    private ControlXYZPanelWidgetPresenter mControlWidgetPanelPresenter;
    // rotary control
    private ControlBAxisPanelWidgetPresenter mControlRotaryPanelPresenter;

    private Laser4AxisSetOriginPagerPresenter mLaser4AxisSetOriginPagerPresenter;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initPattern();
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_4axis_set_origin;
    }

    @Override
    protected LaserCalibrationViewModel getViewModel() {
        return getViewModelProvider().get(LaserCalibrationViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLaser4AxisSetOriginPagerPresenter != null) {
            if (!mLaser4AxisSetOriginPagerPresenter.getBoundaryWarningFlag()) {
                mLaser4AxisSetOriginPagerPresenter.setBoundaryWarningFlag(true);
            }
        }
    }

    private void initPattern() {
        LaserPattern pattern = new LaserPattern(LaserPattern.SHAPE_RULER, LaserPattern.DIRECTION_Y, LaserPattern.ALIGNMENT_ENGRAVE_CENTER, 0.5f);
        mViewModel.setLaserPattern(pattern);
    }

    private void initView() {
        setTitle(R.string.control_set_origin);
        mBtnNext.setText(R.string.all_next);
        LayoutInflater inflater = getLayoutInflater();
        View controlXYZPanel = inflater.inflate(R.layout.widget_control_panel_xyz_axes_mini_for_4axis, null);
        View controlBAxisPanel = inflater.inflate(R.layout.widget_control_panel_b_axis, null);
        mViews = new ArrayList<>();
        mViews.add(controlXYZPanel);
        mViews.add(controlBAxisPanel);

        mPanelAdapter = new ControlPanelAdapter(mViews);
        mVpControlPanels.setAdapter(mPanelAdapter);
        mTlControlPanel.setupWithViewPager(mVpControlPanels);
        mTlControlPanel.setEnabled(false);

        // 4th coordinate panel
        mCoordinateWidgetPresenter = new CoordinateXYZBGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateWidgetPresenter.bind(getView());
        mCoordinateWidgetPresenter.connect();

        // xyz control panel
        mControlWidgetPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlWidgetPanelPresenter.bind(controlXYZPanel, 0.1f, 1f, 5f);
        mControlWidgetPanelPresenter.connect();
        mControlWidgetPanelPresenter.disabledZ();

        // b axis control panel
        mControlRotaryPanelPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlRotaryPanelPresenter.bind(controlBAxisPanel);
        mControlRotaryPanelPresenter.connect();

        // rotary set origin panel
        mLaser4AxisSetOriginPagerPresenter = new Laser4AxisSetOriginPagerPresenter(getContext(), getModel());
        mLaser4AxisSetOriginPagerPresenter.bindView(getLifecycle(), getView(), false);
        mLaser4AxisSetOriginPagerPresenter.setBoundary(mViewModel.getLaserPattern().getPatternBoundary());
        mLaser4AxisSetOriginPagerPresenter.disabledZ();

        mControlWidgetPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mControlRotaryPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mLaser4AxisSetOriginPagerPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mBtnNext.setEnabled(!movingEvent);
                    mControlWidgetPanelPresenter.setEnabled(!movingEvent);
                    mLaser4AxisSetOriginPagerPresenter.setEnabled(!movingEvent);
                    mControlRotaryPanelPresenter.setEnabled(!movingEvent);
                });

        start();
    }

    private Observable<FabPacketContent.GcodeResponse> turnOnLaser() {
        // We use 1% as safe viable power for 10w laser module, 0.5% as default for 1.6w laser module.
        int headType = getModel().getMachineController().getHeadType();
        float laserSafePowerPercent = getModel().getMachineController().getLaserOutputPower();
        return getModel().getSlaveComputer().sendGcode("M3 P " + laserSafePowerPercent);
    }

    private Observable<FabPacketContent.GcodeResponse> turnOffLaser() {
        return getModel().getSlaveComputer().sendGcode("M5");
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    private float calculateWorkingZ(float laserFocus) {
        float bottomZ = getModel().getPreferences().getLaserBottomZ();

        Logger.d("Get laser focus ", laserFocus);

        return Math.max(laserFocus + mViewModel.getWorkpieceDiameter() / 2, bottomZ + 10 * Constants.LASER_TEST_PATTERN_Z_DIFF + 1);
    }

    private Observable<Boolean> gotoInitialPosition() {
        float laserFocus = getModel().getMachineController().getLaserFocus();
        final float z = calculateWorkingZ(laserFocus);

        // Make sure we are at G53 before movement
        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .map(res -> true);
    }

    private Observable<Boolean> setAsOrigin(boolean success) {
        // Switch to CS#1 and then mark current position as origin
        return getModel().getMachineController().updateCoordinateSystem(1)
                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, 0, ISlaveComputer.FLAG_XYZB))
                .flatMap(this::updateCoordinateSystem)
                .map(res -> true);
    }

    private void start() {
        int calibrationMode = getModel().getPreferences().getLaser4AxisCalibrationMode();
        mMovingEventSubject.onNext(true);
        if (calibrationMode == 0) {
            gotoInitialPosition()
                    .flatMap(this::setAsOrigin)
                    .flatMap(res -> turnOnLaser())
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        Logger.e("Failed to prepare for set origin.");
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });

            turnOnLight();
        } else {
            // Manual mode we don't move it anymore
            turnOnLaser()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        Logger.e("Failed to prepare for set origin.");
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
        }
    }

    private void turnOnLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(true)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    private void turnOffLight() {
        boolean cameraLightOn = getModel().getPreferences().getLaserCameraLightOn();
        if (cameraLightOn) {
            getModel().getLaserCameraController().setCameraLighting(false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(success -> {
                        // do nothing
                    }, LogHelper::log);
        }
    }

    @OnClick(R.id.btn_laser_calibration_4axis_set_origin_next)
    void onClickNext() {
        mMovingEventSubject.onNext(true);

        turnOffLaser()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    if (getActivity() != null) {
                        int calibrationMode = getModel().getPreferences().getLaser4AxisCalibrationMode();
                        if (calibrationMode == 0) {
                            ((LaserCalibrationActivity) getActivity()).start4AxisAutoFocusStep1Fragment();
                        } else {
                            ((LaserCalibrationActivity) getActivity()).start4AxisManualFocusStep1Fragment();
                        }
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }

    @Override
    protected void back() {
        turnOffLight();

        turnOffLaser()
                .flatMap(res -> getModel().getMachineController().updateCoordinateSystem(0))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> super.back(), LogHelper::log);
    }
}
