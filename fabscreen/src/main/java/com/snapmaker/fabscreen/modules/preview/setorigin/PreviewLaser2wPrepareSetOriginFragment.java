package com.snapmaker.fabscreen.modules.preview.setorigin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SetOriginPagerPresenter;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;
import com.snapmaker.fabscreen.modules.preview.PreviewLaser40wPrepareModeFragment;
import com.snapmaker.fabscreen.modules.preview.PreviewViewModel;
import com.snapmaker.fabscreen.router.Router;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class PreviewLaser2wPrepareSetOriginFragment extends BaseFragment {

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.btn_preview_laser_prepare_set_origin_next)
    Button mBtnNext;
    @BindView(R.id.btn_preview_40w_settings)
    Button mBtnSettings;

    private int mIndicatorMode = 0;
    private boolean mIsLaserIndicatorOn = false;
    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private SetOriginPagerPresenter mSetOriginPagerPresenter;
    private int mHeadType = Constants.HEAD_UNPLUGGED;
    private boolean mAutoMode = false;
    private PreviewViewModel mViewModel;
    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.control_set_origin);

        mViewModel = getViewModel();
        mViewModel.setLaserIndicatorMode(mIndicatorMode);
        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_prepare_set_origin;
    }

    @Override
    protected PreviewViewModel getViewModel() {
        return getViewModelProvider().get(PreviewViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initView() {
        if (getArguments() != null) {
            mAutoMode = getArguments().getBoolean("auto_mode");
        }
        mHeadType = getModel().getMachineController().getHeadType();

        mCoordinateXYZWidgetPresenter = new CoordinateXYZGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZWidgetPresenter.bind(getView());
        mCoordinateXYZWidgetPresenter.connect();

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(getView(), 0.1f, 1f, 10f);
        mControlPanelPresenter.connect();

        mSetOriginPagerPresenter = new SetOriginPagerPresenter(getContext(), getModel());
        mSetOriginPagerPresenter.bindView(getLifecycle(), getView());
        mSetOriginPagerPresenter.setBoundary(getModel().getPrintController().getModelBoundary());

        if (mAutoMode || mHeadType == Constants.HEAD_LASER_10W) {
            mControlPanelPresenter.disabledZ();
            mSetOriginPagerPresenter.disabledZ();
        }

        mBtnSettings.setVisibility(Button.GONE);

        mControlPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mSetOriginPagerPresenter.getMovingEventObservable()
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
                    mBtnBack.setEnabled(!movingEvent);
                    mBtnNext.setEnabled(!movingEvent);
                    mBtnSettings.setEnabled(!movingEvent);
                    mControlPanelPresenter.setEnabled(!movingEvent);
                    mSetOriginPagerPresenter.setEnabled(!movingEvent);
                });

        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        coordinateSystemPresenter.ensureCoordinate(1);

        mHeadType = getModel().getMachineController().getHeadType();
        switch (mHeadType) {
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W: {
                startLaser();
                break;
            }
            case Constants.HEAD_LASER_10W: {
                startLaser();
                mBtnNext.setText(R.string.all_next);
                break;
            }
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W:
            default: {
                break;
            }
        }

        mViewModel.getLaserIndicatorModeObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(mode -> {
                    if (mIsLaserIndicatorOn && mode != mIndicatorMode) {
                        onLaserIndicatorChanged(mode);
                    } else {
                        mIndicatorMode = mode;
                    }
                });
    }

    private void onLaserIndicatorChanged(int mode) {
        turnOffLaserIndicator()
                .flatMap(success -> {
                    mIndicatorMode = mode;
                    return turnOnLaserIndicator();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    // do nothing
                }, LogHelper::log);
    }

    private Observable<Boolean> turnOnLaserIndicator() {
        Logger.d("Turn on laser indicator, using mode %d", mIndicatorMode);
        mIsLaserIndicatorOn = true;
        float laserShotPower = getModel().getMachineController().getLaserOutputPower();
        switch (mIndicatorMode) {
            case 1:
                return getModel().getSlaveComputer().sendGcode("M3 P" + laserShotPower).map(response -> true);
            case 0:
            default:
                return getModel().getSlaveComputer().setCrossLineLaserIndicator(true);
        }
    }

    private Observable<Boolean> turnOffLaserIndicator() {
        Logger.d("Turn off laser indicator, mode %d", mIndicatorMode);
        mIsLaserIndicatorOn = false;
        switch (mIndicatorMode) {
            case 1:
                return getModel().getSlaveComputer().sendGcode("M5").map(response -> true);
            case 0:
            default:
                return getModel().getSlaveComputer().setCrossLineLaserIndicator(false);
        }
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    private Observable<Boolean> gotoInitialPosition() {

        float platformZ = getModel().getPreferences().getLaserPlatformZ();
        float thickness = getModel().getPreferences().getLaserMaterialThickness();
        final float z = platformZ + thickness;
        final int sizeX = getModel().getMachineController().getSizeX();
        final int sizeY = getModel().getMachineController().getSizeY();

        return getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 X%.2f Y%.2f F3000", sizeX / 2f, sizeY / 2f)))
                .flatMap(res -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", z)))
                .map(res -> true);
    }

    private Observable<Boolean> setAsOrigin(boolean success) {
        // Switch to CS#1 and then mark current position as origin
        return getModel().getMachineController().updateCoordinateSystem(1)
                .flatMap(res -> getModel().getSlaveComputer().setPosition(0, 0, 0, ISlaveComputer.FLAG_XYZ))
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

    @OnClick(R.id.btn_preview_laser_prepare_set_origin_next)
    void onClickStart() {
        mMovingEventSubject.onNext(true);
        turnOffLaserIndicator()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(success -> getModel().getPrintController().getHeaderSecurityStatus())
                .flatMap(success -> getModel().getSlaveComputer().setPrintOffsetWithCrossLine(mViewModel.getLaserIndicatorMode() == 0))
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    if (requireActivity().getIntent().getBooleanExtra("force_refresh", false)) {
                        Logger.d("Refreshing print page...");
                        Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                        requireActivity().finish();
                    } else {
                        if (success) {
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
    }

    @Override
    protected void back() {
        turnOffLaserIndicator()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
                        if (mAutoMode) {
                            mMovingEventSubject.onNext(true);
                            mViewModel.initCameraPosition(true);
                        } else {
                            requireFragmentManager().popBackStack(
                                    PreviewLaser40wPrepareModeFragment.class.getSimpleName(),
                                    0);
                        }
                    } else {
                        // Call back() directly to back into previous page.
                        super.back();
                    }
                }, LogHelper::log);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
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
}
