package com.snapmaker.fabscreen.modules.guidelaser.measureheight;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.guidelaser.GuideLaserActivity;

import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;

public class GuideLaserMeasureHeightFragment extends BaseFragment {
    public static GuideLaserMeasureHeightFragment newInstance() {
        return new GuideLaserMeasureHeightFragment();
    }

    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.laser_calibration_measure_height);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_measure_height;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(getView(), 0.1f, 1f, 10f);
        mControlPanelPresenter.connect();
    }

    @OnClick(R.id.btn_laser_calibration_next)
    void onClickNext() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float bottomZ = (float) status.z;
        getModel().getPreferences().setLaserBottomZ(bottomZ);

        Logger.d("Set bottomZ " + bottomZ);

        if (getActivity() != null) {
            ((GuideLaserActivity) getActivity()).startSafetyGogglesFragment();
        }
    }
}
