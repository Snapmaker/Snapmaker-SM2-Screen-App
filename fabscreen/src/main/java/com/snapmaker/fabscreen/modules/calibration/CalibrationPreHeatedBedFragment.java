package com.snapmaker.fabscreen.modules.calibration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.common.HeatedBedWidgetPresenter;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CalibrationPreHeatedBedFragment extends BaseFragment {
    public static CalibrationPreHeatedBedFragment newInstance() {
        return new CalibrationPreHeatedBedFragment();
    }

    @BindView(R.id.view_calibration_pre_heated_bed_control)
    View mViewPreHeatedBedControl;

    @BindView(R.id.btn_calibration_pre_heated_bed_calibrate)
    Button mBtnCalibrate;

    private CalibrationViewModel mViewModel;

    private HeatedBedWidgetPresenter mHeatedBedWidgetPresenter;
    private BehaviorSubject<Boolean> mIsBedPreHeatedReadySubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_calibration_pre_heated_bed;
    }

    @Override
    protected void back() {
        // Turn off bed before leaving the page.
        mViewModel.turnOffBed();
        super.back();
    }

    private void initView() {
        setTitle(R.string.calibration_pre_heated_bed_title);

        initPreHeatedBedControl();

        bindEvent();
    }

    private void initPreHeatedBedControl() {
        mHeatedBedWidgetPresenter = new HeatedBedWidgetPresenter(getContext(), getModel(), disposables);
        mHeatedBedWidgetPresenter.bind(mViewPreHeatedBedControl);
        mHeatedBedWidgetPresenter.connectMachineStatus();

        // Initialize heated bed temperature.
        float initHeatBedTemp = getModel().getPreferences().get3DPCalibrationHeatedUpTemperature();
        mHeatedBedWidgetPresenter.setTargetValue(initHeatBedTemp);
    }

    private void bindEvent() {
        // Check if heated bed is already heated up with target temperature.
        // Skip first event to wait initialize temperature set up.
        getModel().getSlaveComputer().getMachineStatusObservable()
                .skip(1)
                .throttleLast(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(machineStatus -> {

                    boolean ok = (machineStatus.bedTemperature + 1 >= machineStatus.bedTargetTemperature);
                    mIsBedPreHeatedReadySubject.onNext(ok);
                });

        mIsBedPreHeatedReadySubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ready -> mBtnCalibrate.setEnabled(ready));
    }

    @Override
    protected CalibrationViewModel getViewModel() {
        return getViewModelProvider().get(CalibrationViewModel.class);
    }

    @OnClick(R.id.btn_calibration_pre_heated_bed_calibrate)
    void onClickCalibrate() {
        // Set target heated bed temperature in ViewModel. Value will be set into preferences when saving calibration.
        float heatedBedTemp = mHeatedBedWidgetPresenter.getTargetValue();
        mViewModel.setHeatedLevelingTemperature(heatedBedTemp);

        if (getActivity() != null) {
            ((CalibrationActivity) getActivity()).startCalibrationFragment();
        }
    }
}
