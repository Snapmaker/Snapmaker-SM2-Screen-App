package com.snapmaker.fabscreen.modules.lasercalibration.rotary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;

public class LaserCalibration4AxisMeasureHeightFragment extends BaseFragment {
    public static LaserCalibration4AxisMeasureHeightFragment newInstance() {
        return new LaserCalibration4AxisMeasureHeightFragment();
    }

    @BindView(R.id.vp_laser_calibration_4axis_control_panels)
    ViewPager mVpControlPanels;
    @BindView(R.id.tl_laser_calibration_4axis_control_panel_indicator)
    TabLayout mTlControlPanel;

    private List<View> mViews;

    private ControlPanelAdapter mPanelAdapter;

    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private ControlBAxisPanelWidgetPresenter mControlRotaryPanelPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.laser_calibration_measure_height);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_laser_calibration_4axis_measure_height;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        LayoutInflater inflater = getLayoutInflater();
        View controlXYZPanel = inflater.inflate(R.layout.widget_control_panel_xyz_axes_for_4axis, null);
        View controlBAxisPanel = inflater.inflate(R.layout.widget_control_panel_b_axis, null);
        mViews = new ArrayList<>();
        mViews.add(controlXYZPanel);
        mViews.add(controlBAxisPanel);

        mPanelAdapter = new ControlPanelAdapter(mViews);
        mVpControlPanels.setAdapter(mPanelAdapter);
        mTlControlPanel.setupWithViewPager(mVpControlPanels);
        mTlControlPanel.setEnabled(false);

        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(controlXYZPanel, 0.1f, 1f, 5f);
        mControlPanelPresenter.connect();

        // rotary control panel
        mControlRotaryPanelPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlRotaryPanelPresenter.bind(controlBAxisPanel);
        mControlRotaryPanelPresenter.connect();
    }

    @OnClick(R.id.btn_laser_calibration_4axis_next)
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
