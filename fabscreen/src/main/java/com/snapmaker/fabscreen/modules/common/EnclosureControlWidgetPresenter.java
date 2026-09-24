package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.view.ActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class EnclosureControlWidgetPresenter extends BasePresenter {
    private static final String TAG = EnclosureControlWidgetPresenter.class.getSimpleName();

    @BindView(R.id.btn_widget_enclosure_led_strip)
    ActionButton mBtnLed;
    @BindView(R.id.btn_widget_enclosure_cooling_fan)
    ActionButton mBtnFan;

    public EnclosureControlWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    public void connectStatus() {
        // init view
        Disposable sub = getModel().getMachineController()
                .updateEnclosureStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(enclosureStatus -> {
                    mBtnLed.setEnabled(enclosureStatus.isReady());
                    mBtnFan.setEnabled(enclosureStatus.isReady());

                    mBtnLed.setActivated(enclosureStatus.isLedOn());
                    mBtnFan.setActivated(enclosureStatus.isFanOn());
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_widget_enclosure_led_strip)
    void onClickLed() {
        final boolean isLedOn = getModel().getMachineController().isEnclosureLedOn();
        final int value = isLedOn ? 0 : 100;

        Disposable sub = getModel().getSlaveComputer()
                .setEnclosureLed(value)
                .flatMap(success -> getModel().getMachineController().updateEnclosureStatus())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    mBtnLed.setActivated(status.isLedOn());
                }, LogHelper::log);
        addDisposable(sub);
    }

    @OnClick(R.id.btn_widget_enclosure_cooling_fan)
    void onClickFan() {
        final boolean isFanOn = getModel().getMachineController().isEnclosureFanOn();
        final int value = isFanOn ? 0 : 100;

        Disposable sub = getModel().getSlaveComputer()
                .setEnclosureFan(value)
                .flatMap(success -> getModel().getMachineController().updateEnclosureStatus())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    mBtnFan.setActivated(status.isFanOn());
                }, LogHelper::log);
        addDisposable(sub);
    }

}
