package com.snapmaker.fabscreen.modules.common;

import static fabscreen.libraries.legacy.data.print.PrintController.STATE_PAUSED;
import static fabscreen.libraries.legacy.data.print.PrintController.STATE_PRINTING;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.ceryle.segmentedbutton.SegmentedButtonGroup;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ActionButton;
import fabscreen.libraries.legacy.view.FabFullScreenDialog;
import fabscreen.libraries.legacy.view.RectEnergyBar.RectEnergyBar;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class AirPurifierControlWidgetPresenter extends BasePresenter {

    @BindView(R.id.btn_air_purifier_power)
    ActionButton mBtnFan;
    @BindView(R.id.sbg_air_purifier_fan_speed_level)
    SegmentedButtonGroup mSbgFanSpeed;
    @BindView(R.id.tv_air_purifier_error_power_off)
    TextView mTvTipPowerOff;
    @BindView(R.id.tv_air_purifier_warning_print_affect_notice)
    TextView mTvWarningNotice;
    @BindView(R.id.tv_air_purifier_tip_filter_life_low)
    TextView mTvTipFilterLife;
    @BindView(R.id.reb_air_purifier_lifetime)
    RectEnergyBar mRebLifeTime;

    private int mFanSpeed = 1;
    private boolean mIsAirPurifierOn = false;
    private boolean mIsAirPurifierPowerOff = true;
    private FabFullScreenDialog mHatchDialog;
    private FabFullScreenDialog mFilterDialog;

    private BehaviorSubject<Boolean> mAirPurifierHatchSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mAirPurifierFilterSubject = BehaviorSubject.createDefault(false);

    public AirPurifierControlWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    public void connectStatus() {
        // init view
        mSbgFanSpeed.setOnClickedButtonPosition(position -> {
            switch (position) {
                case 0:
                    mFanSpeed = 1;
                    break;
                case 1:
                    mFanSpeed = 2;
                    break;
                case 2:
                    mFanSpeed = 3;
                    break;
            }
            setAirPurifierFanSpeed();
        });

        mRebLifeTime.setMaxLevel(3);
        mRebLifeTime.initialize();
        mRebLifeTime.setPosition(2);


        // get air purifier status and fan status
        Disposable sub = getModel().getMachineController().updateAirPurifierStatus()
                .flatMap(status -> getModel().getMachineController().getAirPurifierStatusObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(airPurifierStatus -> {
                    mIsAirPurifierPowerOff = airPurifierStatus.status == FabPacketContent.AirPurifierStatus.AIR_PURIFIER_STATUS_POWER_OFF;
                    mBtnFan.setEnabled(!mIsAirPurifierPowerOff);
                    handleErrorBit(airPurifierStatus.errorBit);
                    updateTipsView();
                }, LogHelper::log);
        addDisposable(sub);

        sub = getModel().getMachineController().updateAirPurifierFan()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(airPurifierFan -> {
                    mFanSpeed = airPurifierFan.level;
                    mIsAirPurifierOn = airPurifierFan.isOn;

                    mBtnFan.setActivated(airPurifierFan.isOn);
                    mSbgFanSpeed.setPosition(airPurifierFan.level - 1, false);
                }, LogHelper::log);
        addDisposable(sub);

        sub = getModel().getMachineController().getAirPurifierFilterLifeTimeObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(life -> {
                    mRebLifeTime.setPosition(life);
                    mTvTipFilterLife.setVisibility(life == 0 ? TextView.VISIBLE : TextView.GONE);
                }, LogHelper::log);
        addDisposable(sub);

        sub = getModel().getMachineController()
                .getAirPurifierFanObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fanStatus -> {
                    mFanSpeed = fanStatus.level;
                    mIsAirPurifierOn = fanStatus.isOn;

                    mBtnFan.setActivated(fanStatus.isOn);
                    mSbgFanSpeed.setPosition(fanStatus.level - 1, false);
                    updateTipsView();
                });
        addDisposable(sub);

        // Once the Print State changes, call update tips view during print.
        sub = getModel().getPrintController().getPrintStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(printState -> updateTipsView());
        addDisposable(sub);

        sub = mAirPurifierHatchSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event) {
                        if (mHatchDialog != null && mHatchDialog.isShowing()) return;

                        showNotProperlyClosedDialog();
                    }
                });
        addDisposable(sub);

        sub = mAirPurifierFilterSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event) {
                        if (mFilterDialog != null && mFilterDialog.isShowing()) return;

                        showFilterDrawOutDialog();
                    } else {
                        if (mFilterDialog != null && mFilterDialog.isShowing()) {
                            mFilterDialog.dismiss();
                            mFilterDialog = null;
                        }
                    }
                });
        addDisposable(sub);
    }

    private void setAirPurifierFanSpeed() {
        Disposable sub = getModel().getSlaveComputer().setAirPurifierFanSpeedLevel(mFanSpeed)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ret -> {/**/}, LogHelper::log);
        addDisposable(sub);
    }

    private void handleErrorBit(byte errorBit) {
        boolean isFilterDrawOut = (errorBit & FabPacketContent.AirPurifierStatus.BIT_FILTER_DRAW_OUT) != 0;
        boolean isHatchOpened = (errorBit & FabPacketContent.AirPurifierStatus.BIT_HATCH_OPENED_WHEN_WORKING) != 0;

        // comment this code because we don't need to notify hatch open.
//        mAirPurifierHatchSubject.onNext(isHatchOpened);
        mAirPurifierFilterSubject.onNext(isFilterDrawOut);
    }

    private void showFilterDrawOutDialog() {
        mFilterDialog = FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_air_purifier_filter_not_detected_240x160)
                .setTitle(R.string.warning_air_purifier_cartridge_not_properly_seated_title)
                .setMessage(R.string.warning_air_purifier_cartridge_not_properly_seated_desc)
                .setPositive(R.string.all_confirm, (dialog, which) -> {
                    Disposable sub = getModel().getMachineController().setAirPurifierEnabled(false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ret -> {
                                mFilterDialog.dismiss();
                                mFilterDialog = null;
                            });
                    addDisposable(sub);
                });
        mFilterDialog.show();
    }

    private void showNotProperlyClosedDialog() {
        mHatchDialog = FabFullScreenDialog.create(getContext())
                .setIcon(R.drawable.pic_air_purifier_lid_opened_240x320)
                .setTitle(R.string.warning_air_purifier_not_properly_closed)
                .setMessage(R.string.warning_air_purifier_not_properly_closed_desc)
                .setPositive(R.string.all_confirm, (dialog, which) -> {
                    Disposable sub = getModel().getMachineController().setAirPurifierEnabled(false)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ret -> {
                                mHatchDialog.dismiss();
                                mHatchDialog = null;
                            });
                    addDisposable(sub);
                });
        mHatchDialog.show();
    }

    private void updateFanStatus() {
        Disposable sub = getModel().getMachineController().updateAirPurifierFan()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(airPurifierFan -> {
                    mFanSpeed = airPurifierFan.level;
                    mIsAirPurifierOn = airPurifierFan.isOn;

                    mBtnFan.setActivated(airPurifierFan.isOn);
                    mSbgFanSpeed.setPosition(airPurifierFan.level - 1, false);
                }, LogHelper::log);
        addDisposable(sub);
    }

    // Update air purifier tips. Only one tip will show up at the same time.
    private void updateTipsView() {
        if (mIsAirPurifierPowerOff) {
            // If air purifier is power off, then show up power off warning and hide notice tip.
            mTvWarningNotice.setVisibility(TextView.GONE);
            mTvTipPowerOff.setVisibility(TextView.VISIBLE);
        } else {
            mTvTipPowerOff.setVisibility(TextView.GONE);

            final int printState = getModel().getPrintController().getPrintState();
            final boolean hasPrintJob = (printState == STATE_PRINTING) || (printState == STATE_PAUSED);
            // If switching on the air purifier during 3DP or CNC printing, then show up notice tip.
            if (mIsAirPurifierOn && hasPrintJob) {
                int headType = getModel().getMachineController().getHeadType();
                switch (headType) {
                    case Constants.HEAD_3DP:
                    case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                        mTvWarningNotice.setVisibility(TextView.VISIBLE);
                        mTvWarningNotice.setText(R.string.warning_air_purifier_turn_on_during_print_3dp);
                        break;
                    }
                    case Constants.HEAD_CNC:
                    case Constants.HEAD_CNC_200W: {
                        mTvWarningNotice.setVisibility(TextView.VISIBLE);
                        mTvWarningNotice.setText(R.string.warning_air_purifier_turn_on_during_print_cnc);
                        break;
                    }
                    case Constants.HEAD_LASER:
                    case Constants.HEAD_LASER_10W:
                    case Constants.HEAD_LASER_20W:
                    case Constants.HEAD_LASER_40W:
                    case Constants.HEAD_LASER_2W_IR:
                    default: {
                        mTvWarningNotice.setVisibility(TextView.GONE);
                        break;
                    }
                }
            } else {
                mTvWarningNotice.setVisibility(TextView.GONE);
            }
        }
    }

    @OnClick(R.id.btn_air_purifier_power)
    void onClickFan() {
        mBtnFan.setEnabled(false);
        Disposable sub = getModel().getMachineController().setAirPurifierEnabled(!mIsAirPurifierOn)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ret -> {
                    mBtnFan.setEnabled(true);
                }, e -> {
                    mBtnFan.setEnabled(true);
                });
        addDisposable(sub);
    }

}
