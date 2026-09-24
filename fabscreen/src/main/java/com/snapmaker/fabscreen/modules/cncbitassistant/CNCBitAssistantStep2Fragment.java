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
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelDisabledWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCBitAssistantStep2Fragment extends BaseFragment {
    public static CNCBitAssistantStep2Fragment newInstance() {
        return new CNCBitAssistantStep2Fragment();
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
    private ControlBAxisPanelDisabledWidgetPresenter mControlBAxisPanelPresenter;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_touch_material_title);

        initView();

        moveToMeasureBitPosition();
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
                .load(R.drawable.gif_cnc_bit_assistant_step2_360x210)
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

        mControlXYZPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelPresenter.bind(controlXYZPanel, 0.1f, 1f, 5f);
        mControlXYZPanelPresenter.disabledXY();
        mControlXYZPanelPresenter.connect();

        // Disable B Axis panel to ensure measure point is same as step1.
        mControlBAxisPanelPresenter = new ControlBAxisPanelDisabledWidgetPresenter(getContext(), getModel(), disposables);
        mControlBAxisPanelPresenter.bind(controlBAxisPanel);
        mControlBAxisPanelPresenter.connect();

        mControlXYZPanelPresenter.getMovingEventObservable()
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
    }

    private void moveToMeasureBitPosition() {
        // Return to first bit X Y position
        final float firstX = mViewModel.getBitPositionX();
        final float firstY = mViewModel.getBitPositionY();

        mMovingEventSubject.onNext(true);
        getModel().getSlaveComputer()
                .sendGcode(String.format(Locale.getDefault(), "G0 X%.2f Y%.2f F3000", firstX, firstY))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                }, e -> {
                    LogHelper.log(e);
                    mMovingEventSubject.onNext(false);
                });
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
        final float currentZ = (float) getModel().getMachineController().getMachineStatus().z;
        final float deltaZ = mViewModel.getBitPositionZ() - currentZ;

        Logger.d("deltaZ is %.2f", deltaZ);

        mMovingEventSubject.onNext(true);
        if (mDialog == null) {
            mDialog = showMachineMovingDialog();
        }

        // Apply Z Offset into work origin.
        getModel().getSlaveComputer().setPosition(0, 0, currentZ + deltaZ, ISlaveComputer.FLAG_Z)
                .flatMap(ret -> getModel().getMachineController().updateCoordinateSystem(0))
                .flatMap(success -> getModel().getSlaveComputer().sendGcode("G28 Z"))
                .flatMap(response -> getModel().getMachineController().updateCoordinateSystem(1))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> {
                    mMovingEventSubject.onNext(false);
                    dismissDialog();
                    if (getActivity() != null) {
                        ((CNCBitAssistantActivity) getActivity()).gotoCNCBitAssistantComplete();
                    }
                }, e -> {
                    LogHelper.log(e);
                    dismissDialog();
                    mMovingEventSubject.onNext(false);
                });
    }
}
