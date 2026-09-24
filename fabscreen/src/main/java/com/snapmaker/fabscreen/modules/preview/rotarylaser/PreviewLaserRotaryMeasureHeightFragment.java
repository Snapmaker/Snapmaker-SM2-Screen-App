package com.snapmaker.fabscreen.modules.preview.rotarylaser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.lasercalibration.LaserCalibrationActivity;
import com.snapmaker.fabscreen.modules.preview.PreviewActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class PreviewLaserRotaryMeasureHeightFragment extends BaseFragment {
    public static PreviewLaserRotaryMeasureHeightFragment newInstance() {
        return new PreviewLaserRotaryMeasureHeightFragment();
    }

    @BindView(R.id.widget_control_panel_xyz_axes_for_4axis)
    View mControlXYZPanel;
    @BindView(R.id.btn_preview_laser_4axis_measure_height_next)
    Button mBtnNext;
    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    private ControlXYZPanelWidgetPresenter mControlPanelPresenter;
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.preview_laser_prepare_4axis_measure_height);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_preview_laser_4axis_measure_height;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        mControlPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlPanelPresenter.bind(mControlXYZPanel, 0.1f, 1f, 5f);
        mControlPanelPresenter.connect();

        // bind events
        mControlPanelPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> mIsMovingSubject.onNext(isMoving));

        mIsMovingSubject.observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isMoving -> {
                    mBtnNext.setEnabled(!isMoving);
                    mBtnBack.setEnabled(!isMoving);
                });
    }

    @OnClick(R.id.btn_preview_laser_4axis_measure_height_next)
    void onClickNext() {
        // Pull up Z and move to target position, set origin for it.
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        float currentX = (float) status.x - getModel().getMachineController().getCoordinateOffsetX();
        float currentY = (float) status.y - getModel().getMachineController().getCoordinateOffsetY();
        float currentZ = (float) status.z - getModel().getMachineController().getCoordinateOffsetZ();

        int headType = getModel().getMachineController().getHeadType();
        float pullUpZ;
        if (headType == Constants.HEAD_LASER_10W) {
            pullUpZ = currentZ + Constants.LASER_10W_CAMERA_FOCAL_LENGTH;
        } else {
            pullUpZ = currentZ;
        }

        mIsMovingSubject.onNext(true);
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(result -> getModel().getSlaveComputer().gotoAbsolutePosition(currentX, currentY, pullUpZ))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .flatMap(result -> getModel().getSlaveComputer().setPosition(0, 0, 0, 0, ISlaveComputer.FLAG_XYZB))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(result -> {
                    mIsMovingSubject.onNext(false);

                    if (getActivity() != null) {
                        switch (headType) {
                            case Constants.HEAD_LASER_20W:
                            case Constants.HEAD_LASER_40W:
                                ((PreviewActivity) getActivity()).gotoLaser40wPrepareSafetyGogglesFragment(false);
                                break;
                            case Constants.HEAD_LASER_10W:
                            default:
                                ((PreviewActivity) getActivity()).gotoLaserPrepareSafetyGogglesFragment(false);
                                break;
                        }

                    }
                }, e -> {
                    mIsMovingSubject.onNext(false);
                    LogHelper.log(e);
                });
    }
}
