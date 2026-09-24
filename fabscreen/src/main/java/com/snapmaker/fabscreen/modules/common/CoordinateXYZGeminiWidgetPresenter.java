package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.snapmaker.fabscreen.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.data.Model;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class CoordinateXYZGeminiWidgetPresenter extends BasePresenter {
    private static final String TAG = CoordinateXYZGeminiWidgetPresenter.class.getSimpleName();

    @BindView(R.id.tv_widget_coordinate_absolute_x_value)
    TextView mTvAbsoluteX;
    @BindView(R.id.tv_widget_coordinate_absolute_y_value)
    TextView mTvAbsoluteY;
    @BindView(R.id.tv_widget_coordinate_absolute_z_value)
    TextView mTvAbsoluteZ;

    @BindView(R.id.tv_widget_coordinate_relative_x_value)
    TextView mTvRelativeX;
    @BindView(R.id.tv_widget_coordinate_relative_y_value)
    TextView mTvRelativeY;
    @BindView(R.id.tv_widget_coordinate_relative_z_value)
    TextView mTvRelativeZ;

    public CoordinateXYZGeminiWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
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

                    float offsetX = getModel().getMachineController().getCoordinateOffsetX();
                    float offsetY = getModel().getMachineController().getCoordinateOffsetY();
                    float offsetZ = getModel().getMachineController().getCoordinateOffsetZ();

                    mTvAbsoluteX.setText(String.format(Locale.US, "%.2f", x - offsetX));
                    mTvAbsoluteY.setText(String.format(Locale.US, "%.2f", y - offsetY));
                    mTvAbsoluteZ.setText(String.format(Locale.US, "%.2f", z - offsetZ));

                    mTvRelativeX.setText(String.format(Locale.US, "%.2f", x));
                    mTvRelativeY.setText(String.format(Locale.US, "%.2f", y));
                    mTvRelativeZ.setText(String.format(Locale.US, "%.2f", z));
                });
        addDisposable(sub);
    }
}
