package com.snapmaker.fabscreen.modules.cncoriginassistant;

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
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import fabscreen.libraries.legacy.view.ControlPanelAdapter;
import fabscreen.libraries.legacy.view.FabConfirm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

public class CNCOriginAssistantSetOriginFragment extends BaseFragment {
    public static CNCOriginAssistantSetOriginFragment newInstance() {
        return new CNCOriginAssistantSetOriginFragment();
    }

    @BindView(R.id.iv_cnc_origin_assistant_set_origin_instructions)
    ImageView mIvAssistInstructions;

    @BindView(R.id.vp_cnc_origin_assistant_control_panels)
    ViewPager mVpControlPanels;
    @BindView(R.id.tl_cnc_origin_assistant_control_panel_indicator)
    TabLayout mTlControlPanel;

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.btn_widget_set_origin_x1)
    Button mBtnSetOriginX1;
    @BindView(R.id.btn_widget_set_origin_x2)
    Button mBtnSetOriginX2;
    @BindView(R.id.btn_widget_set_origin_y)
    Button mBtnSetOriginY;
    @BindView(R.id.btn_widget_set_origin_z)
    Button mBtnSetOriginZ;
    @BindView(R.id.btn_widget_set_origin_b)
    Button mBtnSetOriginB;

    private List<View> mViews;
    private ControlPanelAdapter mPanelAdapter;
    private AlertDialog mDialog;

    private CNCOriginAssistantViewModel mViewModel;

    private ControlXYZPanelWidgetPresenter mControlXYZPanelPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelPresenter;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    private BehaviorSubject<Boolean> mMovingEventSubject = BehaviorSubject.createDefault(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.preview_set_work_origin);

        initView();

        start();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_cnc_origin_assistant_set_origin;
    }

    @Override
    protected CNCOriginAssistantViewModel getViewModel() {
        return getViewModelProvider().get(CNCOriginAssistantViewModel.class);
    }

    @Override
    protected void back() {
        FabConfirm.create(getContext())
                .setDescription(R.string.cnc_origin_assistant_set_origin_back_notice)
                .setConfirm(R.string.all_yes, (dialog, which) -> {
                    dialog.dismiss();
                    super.back();
                })
                .setCancel(R.string.all_cancel, ((dialog, which) -> {
                    dialog.dismiss();
                }))
                .show();
    }

    private void initView() {
        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);

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

        // linear control panel
        mControlXYZPanelPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelPresenter.bind(controlXYZPanel, 0.1f, 1f, 5f);
        mControlXYZPanelPresenter.connect();

        // rotary control panel
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

        mViewModel.getMovingObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mMovingEventSubject.onNext(movingEvent);

                    // Handle with moving dialog.
                    if (movingEvent) {
                        if (mDialog != null && mDialog.isShowing()) {
                            mDialog.dismiss();
                        }
                        mDialog = showMachineMovingDialog();
                    } else {
                        dismissDialog();
                    }
                });

        mMovingEventSubject
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> {
                    mControlXYZPanelPresenter.setEnabled(!movingEvent);
                    mControlBAxisPanelPresenter.setEnabled(!movingEvent);
                    mBtnSetOriginX1.setEnabled(!movingEvent);
                    mBtnSetOriginX2.setEnabled(!movingEvent);
                    mBtnSetOriginY.setEnabled(!movingEvent);
                    mBtnSetOriginZ.setEnabled(!movingEvent);
                    mBtnSetOriginB.setEnabled(!movingEvent);
                    mBtnBack.setEnabled(!movingEvent);
                });

        mViewModel.getAssistPhaseObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(phase -> {
                    mBtnSetOriginX1.setVisibility((phase == CNCOriginAssistantViewModel.AssistPhase.X1 || phase == CNCOriginAssistantViewModel.AssistPhase.NOT_START)
                            ? Button.VISIBLE
                            : Button.GONE);
                    mBtnSetOriginX2.setVisibility(phase == CNCOriginAssistantViewModel.AssistPhase.X2 ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginY.setVisibility(phase == CNCOriginAssistantViewModel.AssistPhase.Y ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginZ.setVisibility(phase == CNCOriginAssistantViewModel.AssistPhase.Z ? Button.VISIBLE : Button.GONE);
                    mBtnSetOriginB.setVisibility(phase == CNCOriginAssistantViewModel.AssistPhase.B ? Button.VISIBLE : Button.GONE);

                    switch (phase) {
                        case NOT_START:
                        case X1:
                            Glide.with(this)
                                    .load(R.drawable.gif_cnc_origin_assistant_x1_360x210)
                                    .into(mIvAssistInstructions);
                            break;
                        case X2:
                            Glide.with(this)
                                    .load(R.drawable.gif_cnc_origin_assistant_x2_360x210)
                                    .into(mIvAssistInstructions);
                            break;
                        case Y:
                            Glide.with(this)
                                    .load(R.drawable.gif_cnc_origin_assistant_y_360x210)
                                    .into(mIvAssistInstructions);
                            break;
                        case Z:
                            Glide.with(this)
                                    .load(R.drawable.gif_cnc_origin_assistant_z_360x210)
                                    .into(mIvAssistInstructions);
                            break;
                        case B:
                            Glide.with(this)
                                    .load(R.drawable.gif_cnc_origin_assistant_b_360x210)
                                    .into(mIvAssistInstructions);

                            mVpControlPanels.setCurrentItem(1);
                            break;
                        case COMPLETE:
                            finish();
                            break;
                        case ERROR:
                            mDialog.dismiss();
                            Logger.w("Origin assist error.");
                            break;
                        default:
                            break;
                    }
                });
    }

    private void start() {
        mViewModel.startOriginAssist();
    }

    private void finish() {
        mMovingEventSubject.onNext(true);
        getModel().getMachineController().updateCoordinateSystem(0)
                .flatMap(ret -> getModel().getSlaveComputer().sendGcode("G91"))
                .flatMap(ret -> getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "G0 Z%.2f F1800", 5.0f)))
                .flatMap(ret -> getModel().getSlaveComputer().sendGcode("G90"))
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> {
                    mCoordinateSystemPresenter.ensureCoordinate(1);
                    mMovingEventSubject.onNext(false);
                    if (getActivity() != null) {
                        ((CNCOriginAssistantActivity) getActivity()).gotoCNCOriginAssistantCompleteFragment();
                    }
                }, e -> {
                    mMovingEventSubject.onNext(false);
                    LogHelper.log(e);
                });
    }

    public void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
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

    @OnClick(R.id.btn_widget_set_origin_x1)
    void onClickSetOriginX1() {
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginX1()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R.id.btn_widget_set_origin_x2)
    void onClickSetOriginX2() {
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginX2()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R.id.btn_widget_set_origin_y)
    void onClickSetOriginY() {
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginY()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R.id.btn_widget_set_origin_z)
    void onClickSetOriginZ() {
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginZ()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }

    @OnClick(R.id.btn_widget_set_origin_b)
    void onClickSetOriginB() {
        mMovingEventSubject.onNext(true);
        mViewModel.setOriginB()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ret -> mMovingEventSubject.onNext(false), LogHelper::log);
    }
}
