package com.snapmaker.fabscreen.modules.lasercalibration.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;

public class LaserCalibrationMeasureHeightFragment extends BaseFragment {
    public static LaserCalibrationMeasureHeightFragment newInstance() {
        return new LaserCalibrationMeasureHeightFragment();
    }

    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.laser_calibration_measure_height);

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(view, 0.1f, 1f, 10f);
        mControlPanelPresenter.connect();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_measure_height;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    @OnClick(R.id.btn_laser_calibration_next)
    void onClickNext() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float bottomZ = (float) status.z;
        getModel().getPreferences().setLaserBottomZ(bottomZ);
        Logger.d("Set bottomZ %.2f", bottomZ);

        if (getActivity() != null) {
            ((LaserCalibrationActivity) getActivity()).gotoSafetyGogglesFragment();
        }
    }
}
