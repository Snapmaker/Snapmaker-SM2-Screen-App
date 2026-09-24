package com.snapmaker.fabscreen.modules.factory;

import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class FactoryLaserIndicatorPowerTestFragment extends BaseFragment {
    @BindView(R.id.tv_factory_laser_indicator_power_value)
    TextView mTvSetIndicatorPowerValue;
    @BindView(R.id.tv_get_laser_indicator_power_value)
    TextView mTvGetIndicatorPowerValue;
    @BindView(R.id.iv_control_laser_page_power)
    ImageView mIvLaserPower;
    @BindView(R.id.btn_control_10w_laser_page_power_switch)
    Button mBtnLaserSwitch;
    @BindView(R.id.btn_factory_get_laser_indicator_power)
    Button mBtnGetLaserIndicatorPower;
    @BindView(R.id.btn_factory_set_laser_indicator_power)
    Button mBtnSetLaserIndicatorPower;

    private final BehaviorSubject<Float> mLaserIndicatorPowerSubject = BehaviorSubject.createDefault(-1f);
    private final BehaviorSubject<Float> mSetLaserPowerSubject = BehaviorSubject.createDefault(1f);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle("激光弱光标定测试");
        initView();

        getModel().getSlaveComputer().sendGcode("M5")
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_factory_laser_indicator_power_test;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @Override
    protected void back() {
        getModel().getSlaveComputer().sendGcode("M5")
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe();
        super.back();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBtnLaserSwitch.isActivated()) {
            mBtnLaserSwitch.setActivated(false);
            mIvLaserPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_off);
        }
        getModel().getSlaveComputer().sendGcode("M5")
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe();
    }

    private void initView() {
        mTvSetIndicatorPowerValue.setText("1%");

        mSetLaserPowerSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
            mTvSetIndicatorPowerValue.setText(value + "%");
        });

        mLaserIndicatorPowerSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(power -> {
                    if (power < 0) {
                        Logger.w("get power error " + power);
                    } else {
                        mTvGetIndicatorPowerValue.setText(power + "%");
                    }
                });
    }

    @OnClick({
            R.id.btn_laser_indicator_power_1_percent,
            R.id.btn_laser_indicator_power_1_5_percent,
            R.id.btn_laser_indicator_power_2_percent,
            R.id.btn_laser_indicator_power_2_5_percent,
            R.id.btn_laser_indicator_power_3_percent
    })
    void onClickLaserPowerStep(View view) {
        switch (view.getId()) {
            case R.id.btn_laser_indicator_power_1_percent:
                mSetLaserPowerSubject.onNext(1f);
                break;
            case R.id.btn_laser_indicator_power_1_5_percent:
                mSetLaserPowerSubject.onNext(1.5f);
                break;
            case R.id.btn_laser_indicator_power_2_percent:
                mSetLaserPowerSubject.onNext(2f);
                break;
            case R.id.btn_laser_indicator_power_2_5_percent:
                mSetLaserPowerSubject.onNext(2.5f);
                break;
            case R.id.btn_laser_indicator_power_3_percent:
                mSetLaserPowerSubject.onNext(3f);
                break;
        }
    }

    @OnClick(R.id.btn_factory_get_laser_indicator_power)
    void onRequestLaserIndicatorPower() {
        getModel().getSlaveComputer().getLaserIndicatorPower()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(mLaserIndicatorPowerSubject::onNext, LogHelper::log);
    }

    @OnClick(R.id.btn_factory_set_laser_indicator_power)
    void onSetLaserIndicatorPower() {
        float power = mSetLaserPowerSubject.getValue();
        if (power < 0) return;

        power = Math.max(1f, Math.min(power, 3f));
        getModel().getSlaveComputer().setLaserIndicatorPower(power)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    if (success) {
                        Logger.d("set laser indicator power success");
                        onRequestLaserIndicatorPower();
                    } else {
                        Logger.d("set laser indicator power failed.");
                    }
                }, LogHelper::log);
    }

    @OnClick(R.id.btn_control_10w_laser_page_power_switch)
    void onClickSwitch() {
        if (mBtnLaserSwitch.isActivated()) {
            mBtnLaserSwitch.setActivated(false);
            getModel().getSlaveComputer().sendGcode("M5")
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe();
            mIvLaserPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_off);
        } else {
            mBtnLaserSwitch.setActivated(true);
            float power = mLaserIndicatorPowerSubject.getValue();
            Logger.d("prepare to open laser " + power);
            if (power < 0) {
                FabAlert.alert(requireContext(), "无法打开弱光，请先查询激光或者激光不在可设置范围");
                mBtnLaserSwitch.setActivated(false);
            } else {
                power = Math.max(1f, Math.min(power, 3f));
                Logger.d("Limit laser power result " + power);
                getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "M3 P%.1f", power))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe();
                mIvLaserPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_on);
            }
        }
    }
}
