package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.content.Intent;
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
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZBGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.Laser4AxisSetOriginPagerPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationViewModel;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.router.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.MockConst;
import fabscreen.libraries.legacy.data.model.LaserPattern;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class PreviewLaserRotarySetOriginFragment extends BaseFragment {
    @BindView(R.id.btn_laser_calibration_4axis_set_origin_next)
    Button mBtnNext;

    @BindView(R.id.vp_laser_calibration_4axis_control_panels)
    ViewPager mVpControlPanels;
    @BindView(R.id.tl_laser_calibration_4axis_control_panel_indicator)
    TabLayout mTlControlPanel;
    @BindView(R.id.btn_preview_40w_settings)
    Button mBtnSettings;

    private List<View> mViews;
    private ControlPanelAdapter mPanelAdapter;

    private int mIndicatorMode = 0;
    private boolean mIsLaserIndicatorOn = false;
    private boolean mAutoMode = false;

    private CoordinateXYZBGeminiWidgetPresenter mCoordinateWidgetPresenter;

    private ControlXYZPanelWidgetPresenter mControlWidgetPanelPresenter;
    private ControlBAxisPanelWidgetPresenter mControlRotaryPanelPresenter;

    private Laser4AxisSetOriginPagerPresenter mLaser4AxisSetOriginPagerPresenter;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    private LaserCalibrationViewModel mViewModel;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.control_set_origin);

        mIndicatorMode = getModel().getPreferences().getLaserIndicatorMode();
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

    public void initPattern() {
        LaserPattern pattern = new LaserPattern(LaserPattern.SHAPE_RULER, LaserPattern.DIRECTION_Y, LaserPattern.ALIGNMENT_ENGRAVE_CENTER, 0.5f);
        mViewModel.setLaserPattern(pattern);
    }

    private void initView() {
        if (getArguments() != null) {
            mAutoMode = getArguments().getBoolean("auto_mode");
        }


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
        mControlWidgetPanelPresenter.bind(controlXYZPanel, 0.1f, 1, 5f);
        mControlWidgetPanelPresenter.connect();
        if (mAutoMode) {
            mControlWidgetPanelPresenter.disabledZ();
        }

        // b axis control panel
        mControlRotaryPanelPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlRotaryPanelPresenter.bind(controlBAxisPanel);
        mControlRotaryPanelPresenter.connect();

        // rotary set origin panel
        mLaser4AxisSetOriginPagerPresenter = new Laser4AxisSetOriginPagerPresenter(getContext(), getModel());
        mLaser4AxisSetOriginPagerPresenter.bindView(getLifecycle(), getView(), false);
        if (mAutoMode) {
            mLaser4AxisSetOriginPagerPresenter.disabledZ();
        }
        mLaser4AxisSetOriginPagerPresenter.setBoundary(getModel().getPrintController().getModelBoundary());

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
                    mBtnSettings.setEnabled(!movingEvent);
                    mControlWidgetPanelPresenter.setEnabled(!movingEvent);
                    mLaser4AxisSetOriginPagerPresenter.setEnabled(!movingEvent);
                    mControlRotaryPanelPresenter.setEnabled(!movingEvent);
                });

        int headType = getModel().getMachineController().getHeadType();
        switch (headType) {
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                mBtnSettings.setVisibility(Button.VISIBLE);
                break;
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            default:
                mBtnSettings.setVisibility(Button.GONE);
                break;
        }

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(1);


        startLaser();
    }

    private Observable<Boolean> turnOnLaserIndicator() {
        mIsLaserIndicatorOn = true;
        int headType = getModel().getMachineController().getHeadType();
        switch (headType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W:
                return turnOnLaser().map(response -> true);
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                Logger.d("Turn on laser indicator, using mode %d", mIndicatorMode);

                switch (mIndicatorMode) {
                    case 1:
                        return turnOnLaser().map(response -> true);
                    case 0:
                    default:
                        return turnOnCrossLineIndicator();
                }
            case Constants.HEAD_LASER_2W_IR:
                return turnOnCrossLineIndicator();
            default:
                return Observable.just(true);
        }
    }

    private Observable<Boolean> turnOffLaserIndicator() {
        mIsLaserIndicatorOn = false;
        int headType = getModel().getMachineController().getHeadType();
        switch (headType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_10W:
                return turnOffLaser().map(response -> true);
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W:
                Logger.d("Turn off laser indicator, mode %d", mIndicatorMode);
                switch (mIndicatorMode) {
                    case 1:
                        return turnOffLaser().map(response -> true);
                    case 0:
                    default:
                        return turnOffCrossLineIndicator();
                }
            case Constants.HEAD_LASER_2W_IR:
                return turnOffCrossLineIndicator();
            default:
                return Observable.just(true);
        }
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
    private Observable<Boolean> turnOnCrossLineIndicator() {
        return getModel().getSlaveComputer().setCrossLineLaserIndicator(true);
    }

    private Observable<Boolean> turnOffCrossLineIndicator() {
        return getModel().getSlaveComputer().setCrossLineLaserIndicator(false);
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    private Observable<Boolean> gotoInitialPosition() {
        if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
            // We assure that position was already initialized when enter in 10w laser workflow.
            return Observable.just(true);
        }

        float laserFocus = getModel().getMachineController().getLaserFocus();
//        float thickness = getModel().getPreferences().getLaserMaterialThickness();
        final float z = laserFocus + getViewModel().getWorkpieceDiameter() / 2;
        final int sizeX = getModel().getMachineController().getSizeX();
        // TODO: getY
        final int sizeY = getModel().getMachineController().getSizeY();
        float initialY = Math.min(sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - (2 * 10 + 1), sizeY - MockConst.ROTARY_MOCK_CHUCK_LENGTH - mViewModel.getWorkpieceLength());
        Logger.d("Get laser focus %.2f", laserFocus);

        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G1 X%.2f Y%.2f F3000", sizeX / 2f - 8, initialY)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G1 Z%.2f F1800", z)))
                .map(res -> true);
    }

    private Observable<Boolean> setAsOrigin(boolean success) {
        // Switch to CS#1 and then mark current position as origin
        return getModel().getMachineController().updateCoordinateSystem(1)
                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, 0, ISlaveComputer.FLAG_XYZB))
                .flatMap(this::updateCoordinateSystem)
                .map(res -> true);
    }

    private void startLaser() {
        if (mAutoMode) {
            mMovingEventSubject.onNext(true);
            gotoInitialPosition()
                    .flatMap(this::setAsOrigin)
                    .flatMap(response -> turnOnLaserIndicator())
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        mMovingEventSubject.onNext(false);
                    }, e -> {
                        Logger.e("Failed to prepare for set origin.");
                        LogHelper.log(e);
                        mMovingEventSubject.onNext(false);
                    });
        } else {
            turnOnLaserIndicator()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        // do nothing
                    }, e -> {
                        Logger.e("Failed to turn on laser.");
                        LogHelper.log(e);
                    });
        }
    }

    @OnClick(R.id.btn_laser_calibration_4axis_set_origin_next)
    void onClickNext() {
        mMovingEventSubject.onNext(true);

        turnOffLaserIndicator()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    int headType = getModel().getMachineController().getHeadType();
                    switch (headType) {
                        case Constants.HEAD_LASER_20W:
                        case Constants.HEAD_LASER_40W:
                            ((PreviewActivity) requireActivity()).gotoLaser40wPreparePrintSafetyNoticeFragment(mIndicatorMode);
                            break;
                        case Constants.HEAD_LASER_2W_IR:
                            turnOffCrossLineIndicator()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .flatMap(response -> getModel().getPrintController().getHeaderSecurityStatus())
                                    .flatMap(response -> getModel().getSlaveComputer().setPrintOffsetWithCrossLine(true))
                                    .as(bindToLifecycle())
                                    .subscribe(response1 -> {
                                        mMovingEventSubject.onNext(false);
                                        if (requireActivity().getIntent().getBooleanExtra("force_refresh", false)) {
                                            Logger.d("Refreshing print page...");
                                            Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                                        } else {
                                            if (response1) {
                                                Router.getInstance().routeToPrintPage().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                                                requireActivity().finish();
                                            } else {
                                                Logger.e("Set LaserIndicator mode failed, but start print instead.");
                                                Router.getInstance().routeToPrintPage().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                                                requireActivity().finish();
                                            }
                                        }

                                    }, e -> {
                                        LogHelper.log(e);
                                        mMovingEventSubject.onNext(false);
                                    });
                            break;
                        case Constants.HEAD_LASER:
                        case Constants.HEAD_LASER_10W:
                        default:
                            Bundle arguments = getArguments();
                            if (arguments == null) {
                                return;
                            }

                            Router.getInstance()
                                    .routeToPrintPage()
                                    .start(getContext());
                            requireActivity().finish();
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }

    @OnClick(R.id.btn_preview_40w_settings)
    void onClick40wSettings() {
        ((PreviewActivity) requireActivity()).gotoLaser40wSettingsIndicatorModeFragment();
    }

    @Override
    protected void back() {
        turnOffLaserIndicator()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> super.back(), LogHelper::log);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            int mode = getModel().getPreferences().getLaserIndicatorMode();
            if (mode != mIndicatorMode && mIsLaserIndicatorOn) {
                turnOffLaserIndicator()
                        .flatMap(success -> {
                            mIndicatorMode = mode;
                            return turnOnLaserIndicator();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(response -> {
                            // do nothing
                        }, e -> {
                            Logger.e("Failed to turn on/off laser.");
                            LogHelper.log(e);
                        });
            }
        }
    }
}
