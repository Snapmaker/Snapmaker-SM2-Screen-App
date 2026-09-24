package com.snapmaker.fabscreen.modules.cncbitassistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCBitAssistantStep1Fragment extends BaseFragment {
    public static CNCBitAssistantStep1Fragment newInstance() {
        return new CNCBitAssistantStep1Fragment();
    }

    @BindView(R.id.iv_cnc_bit_assistant_measure_bit_instructions)
    ImageView mIvBitAssistantInstruction;
    @BindView(R.id.vp_cnc_bit_assistant_control_panels)
    ViewPager mVpControlPanels;
    @BindView(R.id.tl_cnc_bit_assistant_control_panel_indicator)
    TabLayout mTlControlPanel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_bit_assistant_measure_bit_set)
    Button mBtnSet;

    private List<View> mViews;
    private ControlPanelAdapter mPanelAdapter;
    private AlertDialog mDialog;

    private CNCBitAssistantViewModel mViewModel;

    private ControlXYZPanelWidgetPresenter mControlXYZPanelPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelPresenter;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.cnc_bit_assistant);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_bit_assistant_measure_bit;
    }

    @Override
    protected CNCBitAssistantViewModel getViewModel() {
        return getViewModelProvider().get(CNCBitAssistantViewModel.class);
    }

    private void initView() {
        Glide.with(this)
                .load(R.drawable.gif_cnc_bit_assistant_step1_360x210)
                .into(mIvBitAssistantInstruction);

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

        // XYZ panel
        mControlXYZPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelPresenter.bind(controlXYZPanel, 0.1f, 1f, 5f);
        mControlXYZPanelPresenter.connect();

        // B Axis panel
        mControlBAxisPanelPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlBAxisPanelPresenter.bind(controlBAxisPanel);
        mControlBAxisPanelPresenter.connect();

        mControlXYZPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mControlBAxisPanelPresenter.getMovingEventObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);
                });

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mControlXYZPanelPresenter.setEnabled(!movingEvent);
                    mControlBAxisPanelPresenter.setEnabled(!movingEvent);
                    mBtnSet.setEnabled(!movingEvent);
                    mBtnBack.setEnabled(!movingEvent);
                });

        start();
    }

    public void start() {
        mMovingEventSubject.onNext(true);
        // Home machine before start, then goto init position
        mDialog = showMachineMovingDialog();

        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("G28"))
                .flatMap(success -> getModel().getMachineController().updateCoordinateSystem(1))
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("G0 X0 Y0 F3000"))
                .flatMap(this::updateCoordinateSystem)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    dismissDialog();
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }

    private Observable<FabPacketContent.CoordinateSystem> updateCoordinateSystem(Object response) {
        return getModel().getMachineController().updateCoordinateSystem();
    }

    public void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private AlertDialog showMachineMovingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.default_theme_dark_mask);
            dialog.getWindow().setLayout(280 * 2, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_cnc_origin_assistant_set_origin_moving, null);
        dialog.setView(view);
        dialog.show();

        return dialog;
    }

    @OnClick(R.id.btn_bit_assistant_measure_bit_set)
    void onClickSet() {
        mMovingEventSubject.onNext(true);

        if (mDialog == null) {
            mDialog = showMachineMovingDialog();
        }
        // Save current position as first bit position.
        final FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        mViewModel.setBitPosition((float) status.x, (float) status.y, (float) status.z);

        Logger.d("current Z position %.2f", status.z);

        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(system -> getModel().getSlaveComputer().sendGcode("G28 Z"))
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> {
                    mMovingEventSubject.onNext(false);
                    dismissDialog();
                    if (getActivity() != null) {
                        ((CNCBitAssistantActivity) getActivity()).gotoCNCBitAssistantStep2Intro();
                    }
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
    }
}
