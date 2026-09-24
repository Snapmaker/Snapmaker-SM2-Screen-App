package com.snapmaker.fabscreen.modules.common;


import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.view.ActionButton;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class CoordinateXYZWidgetPresenter extends BasePresenter {
    @BindView(R.id.tv_widget_coordinate_absolute_x_value)
    TextView mTvAbsoluteX;
    @BindView(R.id.tv_widget_coordinate_absolute_y_value)
    TextView mTvAbsoluteY;
    @BindView(R.id.tv_widget_coordinate_absolute_z_value)
    TextView mTvAbsoluteZ;

    @BindView(R.id.btn_widget_coordinate_home)
    ActionButton mBtnHome;

    public CoordinateXYZWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        super(context, model, compositeDisposable);
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);
    }

    public void connect() {
        Disposable sub = getModel().getSlaveComputer().getMachineStatusObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(machineStatus -> {
                    float x = (float) machineStatus.x;
                    float y = (float) machineStatus.y;
                    float z = (float) machineStatus.z;

                    mTvAbsoluteX.setText(String.format(Locale.US, "%.2f", x));
                    mTvAbsoluteY.setText(String.format(Locale.US, "%.2f", y));
                    mTvAbsoluteZ.setText(String.format(Locale.US, "%.2f", z));
                });
        addDisposable(sub);
    }

    @OnClick(R.id.btn_widget_coordinate_home)
    void onClickHome() {
        mBtnHome.setActivated(true);

        // Note that this widget is only used for 3DP
        // We simply use CS#0, thus no coordinate system need to be updated
        Disposable sub = getModel().getSlaveComputer().sendGcode("G28")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    mBtnHome.setActivated(false);
                }, e -> {
                    LogHelper.log(e);
                    mBtnHome.setActivated(false);
                });
        addDisposable(sub);
    }
}
