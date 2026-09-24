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
import com.snapmaker.fabscreen.modules.preview.PreviewLaserMeasureThicknessFragment;
import com.snapmaker.fabscreen.modules.preview.PreviewLaserPrepareModeNoteFragment;
import com.snapmaker.fabscreen.modules.preview.PreviewViewModel;
import com.snapmaker.fabscreen.router.Router;

import java.util.Locale;
import java.util.ResourceBundle;

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

public class PreviewLaserPrepareSetOriginFragment extends BaseFragment {

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_preview_40w_settings)
    Button mBtnSettings;

    @BindView(R.id.btn_preview_laser_prepare_set_origin_next)
    Button mBtnNext;

    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private SetOriginPagerPresenter mSetOriginPagerPresenter;
    private int mHeadType = Constants.HEAD_UNPLUGGED;
    private boolean mAutoMode = false;
    private PreviewViewModel mViewModel;
    private final BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.control_set_origin);

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

    private void initView() {
        if (getArguments() != null) {
            mAutoMode = getArguments().getBoolean("auto_mode");
        }
        mHeadType = getModel().getMachineController().getHeadType();

        if (mHeadType == Constants.HEAD_LASER_20W || mHeadType == Constants.HEAD_LASER_40W) {
            mBtnSettings.setVisibility(Button.VISIBLE);
        } else {
            mBtnSettings.setVisibility(Button.GONE);
        }

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
                    mControlPanelPresenter.setEnabled(!movingEvent);
                    mSetOriginPagerPresenter.setEnabled(!movingEvent);
                });

        CoordinateSystemPresenter coordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        coordinateSystemPresenter.ensureCoordinate(1);

        if (mHeadType == Constants.HEAD_LASER || mHeadType == Constants.HEAD_LASER_10W) {
            startLaser();
            if (mHeadType == Constants.HEAD_LASER_10W) {
                mBtnNext.setText(R.string.all_next);
            }
        }

        mViewModel = getViewModel();
        // We need to back into select mode notify page if using 10w Laser.
        // So pop back other fragments until PreviewLaserPrepareModeNoteFragment.
        mViewModel.getResultBackObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(settled -> {
                    mMovingEventSubject.onNext(false);
                    if (!settled) return;
                    requireFragmentManager().popBackStack(
                            PreviewLaserMeasureThicknessFragment.class.getSimpleName(),
                            0);
                });
    }

    private Observable<FabPacketContent.GcodeResponse> turnOnLaser() {
        // We use 1% as safe viable power for 10w laser module, 0.5% as default for 1.6w laser module.
        float laserSafePowerPercent = getModel().getMachineController().getLaserOutputPower();
        return getModel().getSlaveComputer().sendGcode("M3 P " + laserSafePowerPercent);
    }

    private Observable<FabPacketContent.GcodeResponse> turnOffLaser() {
        return getModel().getSlaveComputer().sendGcode("M5");
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    private Observable<Boolean> gotoInitialPosition() {
        if (mHeadType == Constants.HEAD_LASER_10W) {
            // already at right position
            return Observable.just(true);
        }
        float laserFocus = getModel().getMachineController().getLaserFocus();
        float thickness = getModel().getPreferences().getLaserMaterialThickness();
        final float z = laserFocus + thickness;
        final int sizeX = getModel().getMachineController().getSizeX();
        final int sizeY = getModel().getMachineController().getSizeY();
        Logger.d("Get laser focus %.2f", laserFocus);

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
                    .flatMap(response -> turnOnLaser())
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
            turnOnLaser()
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
        turnOffLaser()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(success ->
                        (mHeadType == Constants.HEAD_LASER_10W) ?
                                getModel().getPrintController().getHeaderSecurityStatus() : Observable.just(new FabPacketContent.HeaderSecurity((byte) 0))
                )
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    if (success.status == 0) {
                        Bundle arguments = getArguments();
                        if (arguments == null) {
                            return;
                        }
                        if (requireActivity().getIntent().getBooleanExtra("force_refresh", false)) {
                            Logger.d("Refreshing print page...");
                            Router.getInstance().routeToPrintPage(true).start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                        } else {
                            Router.getInstance().routeToPrintPage().start(getContext(), Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        requireActivity().finish();
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }

    @Override
    protected void back() {
        turnOffLaser()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(response -> {
                    if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_10W) {
                        if (mAutoMode) {
                            mMovingEventSubject.onNext(true);
                            mViewModel.initCameraPosition(true);
                        } else {
                            requireFragmentManager().popBackStack(
                                    PreviewLaserPrepareModeNoteFragment.class.getSimpleName(),
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
            turnOnLaser()
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
