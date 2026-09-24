package com.snapmaker.fabscreen.modules.loadfilament;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ActionButton;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.NoScrollViewPager;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DualExtruderLoadFilamentFragment extends BaseFragment {
    public static DualExtruderLoadFilamentFragment newInstance() {
        return new DualExtruderLoadFilamentFragment();
    }

    private static final int LEFT_EXTRUDER = 0;
    private static final int RIGHT_EXTRUDER = 1;

    @BindView(R.id.vp_dual_extruder_load_filament)
    NoScrollViewPager mVpViewPager;
    @BindView(R.id.view_dual_extruder_nozzle_tab)
    View mViewTab;
    @BindView(R.id.tl_dual_extruder_nozzle_tab)
    TabLayout mTlNozzleTab;
    @BindView(R.id.ll_dual_extruder_nozzle_tab_left_temp)
    View mLeftNozzleTemp;
    @BindView(R.id.ll_dual_extruder_nozzle_tab_right_temp)
    View mRightNozzleTemp;
    @BindView(R.id.tv_dual_extruder_left_temperature)
    TextView mTvLeftTemperature;
    @BindView(R.id.tv_dual_extruder_right_temperature)
    TextView mTvRightTemperature;

    private List<View> mViews;
    private LoadFilamentPageAdapter mLoadFilamentPageAdapter;
    private View mLeftExtruderPage;
    private View mRightExtruderPage;

    private RulerView mRvLeftExtruder;
    private RulerView mRvRightExtruder;

    private TextView mTvLeftTargetValue;
    private TextView mTvRightTargetValue;
    private TextView mTvLeftCurrentValue;
    private TextView mTvRightCurrentValue;

    private LoadFilamentViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = getViewModel();
    }

    @Override
    protected void back() {
        mViewModel.turnOffHead()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle()).subscribe(result -> super.back(), LogHelper::log);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();

        // Step 1: Show guide page for if load info flag was on.

        // Step 2: Show dialog when land into this page.(after the guide).
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        if (status.headTargetTemperature == 0 || status.extruder1TargetTemperature == 0) {
            showPreHeatTempDialog();
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_dual_extruder_load_filament;
    }

    @Override
    protected LoadFilamentViewModel getViewModel() {
        return getViewModelProvider().get(LoadFilamentViewModel.class);
    }

    private void initView() {
        LayoutInflater inflater = getLayoutInflater();
        mLeftExtruderPage = inflater.inflate(R.layout.fragment_control_3dp_page_nozzle, null);
        mRightExtruderPage = inflater.inflate(R.layout.fragment_control_3dp_page_nozzle, null);
        if (mLeftExtruderPage == null || mRightExtruderPage == null) {
            Logger.e("Error: adding null view into view pagers, initialization aborted.");
            back();
            return;
        }
        mViews = new ArrayList<>();
        mViews.add(mLeftExtruderPage);
        mViews.add(mRightExtruderPage);

        mLoadFilamentPageAdapter = new LoadFilamentPageAdapter(mViews);
        mVpViewPager.setAdapter(mLoadFilamentPageAdapter);
        mVpViewPager.setOffscreenPageLimit(mViews.size());

        initNozzleTab();
        initLeftPageView();
        initRightPageView();
        bindExtrudersButtons();
        bindPageEvents();
    }

    private void initNozzleTab() {
        mTlNozzleTab.setupWithViewPager(mVpViewPager);
        mTlNozzleTab.getTabAt(LEFT_EXTRUDER).setText(requireContext().getString(R.string.dual_extruder_load_filament_nozzle_l));
        mTlNozzleTab.getTabAt(RIGHT_EXTRUDER).setText(requireContext().getString(R.string.dual_extruder_load_filament_nozzle_r));

        // Don't show temperatures on tab when initializing.
        mLeftNozzleTemp.setVisibility(View.INVISIBLE);
        mRightNozzleTemp.setVisibility(View.INVISIBLE);

        mTlNozzleTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int select = tab.getPosition();
                mLeftNozzleTemp.setVisibility(select == LEFT_EXTRUDER ? View.INVISIBLE : View.VISIBLE);
                mRightNozzleTemp.setVisibility(select == RIGHT_EXTRUDER ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mTlNozzleTab.selectTab(mTlNozzleTab.getTabAt(LEFT_EXTRUDER), true);
        mRightNozzleTemp.setVisibility(View.VISIBLE);
    }

    private void bindPageEvents() {
        // Initialize first data for nozzle temperature
        DualExtruderTemperature temperatures0 = mViewModel.getDualExtruderTemperature();
        // tab value
        mTvLeftTemperature.setText(formatValue(temperatures0.getExtruder0Temperature()));
        mTvRightTemperature.setText(formatValue(temperatures0.getExtruder1Temperature()));

        // page value
        mTvLeftCurrentValue.setText((formatValue(temperatures0.getExtruder0Temperature())));
        mTvRightCurrentValue.setText((formatValue(temperatures0.getExtruder1Temperature())));
        mTvLeftTargetValue.setText(formatValue(temperatures0.getExtruder0TargetTemperature()));
        mTvRightTargetValue.setText(formatValue(temperatures0.getExtruder1TargetTemperature()));

        // ruler value
        mRvLeftExtruder.setCurrentValue(temperatures0.getExtruder0TargetTemperature());
        mRvRightExtruder.setCurrentValue(temperatures0.getExtruder1TargetTemperature());

        mViewModel.getExtrudersTemperatureObservable()
                .skip(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(temperatures -> {
                    mTvLeftTemperature.setText((formatValue(temperatures.getExtruder0Temperature())));
                    mTvRightTemperature.setText((formatValue(temperatures.getExtruder1Temperature())));
                    mTvLeftCurrentValue.setText((formatValue(temperatures.getExtruder0Temperature())));
                    mTvRightCurrentValue.setText((formatValue(temperatures.getExtruder1Temperature())));
                    mTvLeftTargetValue.setText(formatValue(temperatures.getExtruder0TargetTemperature()));
                    mTvRightTargetValue.setText(formatValue(temperatures.getExtruder1TargetTemperature()));
                });
    }

    private void initLeftPageView() {
        View viewLeftNozzle = mLeftExtruderPage.findViewById(R.id.view_control_3dp_page_nozzle);

        if (isViewNull(viewLeftNozzle)) {
            return;
        }

        TextView tvTitle = viewLeftNozzle.findViewById(R.id.tv_widget_set_value_ruler_title);
        TextView tvValueUnit = viewLeftNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_unit);
        TextView tvValueSlash = viewLeftNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_slash);
        mTvLeftCurrentValue = viewLeftNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_current);
        mTvLeftTargetValue = viewLeftNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_target);

        mRvLeftExtruder = viewLeftNozzle.findViewById(R.id.rv_widget_set_value_ruler_ruler);
        mRvLeftExtruder.setMaxValue(300);
        mRvLeftExtruder.setMinValue(0);

        if (isViewNull(tvValueUnit)
                || isViewNull(tvValueSlash)
                || isViewNull(mTvLeftCurrentValue)
                || isViewNull(mTvLeftTargetValue)
                || isViewNull(tvTitle)
                || isViewNull(mRvLeftExtruder)) {
            return;
        }

        tvTitle.setText(R.string.print_nozzle_temp);
        tvValueUnit.setText(R.string.all_unit_temperature);
        tvValueSlash.setVisibility(TextView.VISIBLE);

        mRvLeftExtruder.setOnValueChangedListener(value -> {
            float pow = (float) Math.pow(10, 1);
            float newValue = (int) (value * pow) / pow;
            mViewModel.setDualExtruderTargetValue(LEFT_EXTRUDER, newValue);
        });
    }

    private void initRightPageView() {
        View viewRightNozzle = mRightExtruderPage.findViewById(R.id.view_control_3dp_page_nozzle);

        if (isViewNull(viewRightNozzle)) {
            return;
        }

        TextView tvTitle = viewRightNozzle.findViewById(R.id.tv_widget_set_value_ruler_title);
        TextView tvValueUnit = viewRightNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_unit);
        TextView tvValueSlash = viewRightNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_slash);
        mTvRightCurrentValue = viewRightNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_current);
        mTvRightTargetValue = viewRightNozzle.findViewById(R.id.tv_widget_set_value_ruler_value_target);
        mRvRightExtruder = viewRightNozzle.findViewById(R.id.rv_widget_set_value_ruler_ruler);
        mRvRightExtruder.setMaxValue(300);
        mRvRightExtruder.setMinValue(0);

        if (isViewNull(tvValueUnit)
                || isViewNull(tvValueSlash)
                || isViewNull(mTvRightCurrentValue)
                || isViewNull(mTvRightTargetValue)
                || isViewNull(tvTitle)
                || isViewNull(mRvRightExtruder)) {
            return;
        }

        tvTitle.setText(R.string.print_nozzle_temp);
        tvValueUnit.setText(R.string.all_unit_temperature);
        tvValueSlash.setVisibility(TextView.VISIBLE);

        mRvRightExtruder.setOnValueChangedListener(value -> {
            float pow = (float) Math.pow(10, 1);
            float newValue = (int) (value * pow) / pow;
            mViewModel.setDualExtruderTargetValue(RIGHT_EXTRUDER, newValue);
        });
    }

    private void bindExtrudersButtons() {
        // Left Extruder Page
        ActionButton btnLeftExtruderLoad = mLeftExtruderPage.findViewById(R.id.btn_widget_load_filament_load);
        ActionButton btnLeftExtruderUnload = mLeftExtruderPage.findViewById(R.id.btn_widget_load_filament_unload);
        if (isViewNull(btnLeftExtruderLoad) || isViewNull(btnLeftExtruderUnload)) return;

        btnLeftExtruderLoad.setEnabled(false);
        btnLeftExtruderUnload.setEnabled(false);

        btnLeftExtruderLoad.setOnClickListener(v -> {
            mViewModel.setMoving(true);
            btnLeftExtruderLoad.setEnabled(false);
            mViewModel.requestExtruderLoad(LEFT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(result -> {
                        if (result) {
                            showExtruderOperatingDialog(0, 0);
                        } else {
                            mViewModel.setMoving(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mViewModel.setMoving(false);
                        btnLeftExtruderLoad.setEnabled(true);
                    });
        });

        btnLeftExtruderUnload.setOnClickListener(v -> {
            mViewModel.setMoving(true);
            btnLeftExtruderLoad.setEnabled(false);
            mViewModel.requestExtruderUnload(LEFT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(result -> {
                        if (result) {
                            showExtruderOperatingDialog(0, 1);
                        } else {
                            mViewModel.setMoving(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mViewModel.setMoving(false);
                        btnLeftExtruderLoad.setEnabled(true);
                    });
        });

        // Right Extruder Page
        ActionButton btnRightExtruderLoad = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_load);
        ActionButton btnRightExtruderUnload = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_unload);
        if (isViewNull(btnRightExtruderLoad) || isViewNull(btnRightExtruderUnload)) return;

        btnRightExtruderLoad.setEnabled(false);
        btnRightExtruderUnload.setEnabled(false);

        btnRightExtruderLoad.setOnClickListener(v -> {
            mViewModel.setMoving(true);
            btnLeftExtruderLoad.setEnabled(false);
            mViewModel.requestExtruderLoad(RIGHT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(result -> {
                        if (result) {
                            // Show extruding dialog
                            showExtruderOperatingDialog(1, 0);
                        } else {
                            mViewModel.setMoving(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mViewModel.setMoving(false);
                        btnLeftExtruderLoad.setEnabled(true);
                    });
        });

        btnRightExtruderUnload.setOnClickListener(v -> {
            mViewModel.setMoving(true);
            btnLeftExtruderLoad.setEnabled(false);
            mViewModel.requestExtruderUnload(RIGHT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(result -> {
                        if (result) {
                            // Show extruding dialog
                            showExtruderOperatingDialog(1, 1);
                        } else {
                            mViewModel.setMoving(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mViewModel.setMoving(false);
                        btnLeftExtruderLoad.setEnabled(true);
                    });
        });

        // Bind button enabled status with observable.
        Observable.combineLatest(mViewModel.getExtruderReadyObservable(0), mViewModel.getIsMovingObservable(), (isTempReady, isMoving) -> isTempReady && !isMoving)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ready -> {
                    btnLeftExtruderLoad.setEnabled(ready);
                    btnLeftExtruderUnload.setEnabled(ready);
                });

        Observable.combineLatest(mViewModel.getExtruderReadyObservable(1), mViewModel.getIsMovingObservable(), (isTempReady, isMoving) -> isTempReady && !isMoving)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(ready -> {
                    btnRightExtruderLoad.setEnabled(ready);
                    btnRightExtruderUnload.setEnabled(ready);
                });
    }

    /**
     * @param which     0 means Left extruder, 1 means Right extruder
     * @param operation 0 means load filament in, 1 means unload filament out.
     */
    public void showExtruderOperatingDialog(int which, int operation) {
        boolean isIntentToLoad = operation == 0;
        int dialogIconRes = operation == 0 ? R.drawable.btn_control_load_normal_50x50
                : R.drawable.btn_control_unload_normal_50x50;

        int stringDescResId = R.string.dual_extruder_load_filament_loading_preparing;
        if (which == 0) {
            stringDescResId = isIntentToLoad ? R.string.dual_extruder_load_filament_loading_left
                    : R.string.dual_extruder_load_filament_unloading_left;
        } else {
            stringDescResId = isIntentToLoad ? R.string.dual_extruder_load_filament_loading_right
                    : R.string.dual_extruder_load_filament_unloading_right;
        }

        FabConfirm.create(requireContext())
                .setCanceledOnTouchOutSide(false)
                .setIcon(dialogIconRes)
                .setDescription(stringDescResId)
                .setConfirm(R.string.all_stop, (dialog, which1) -> {
                    if (isIntentToLoad) {
                        getViewModel().requestStopExtruderLoad(which)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> {
                                    mViewModel.setMoving(false);
                                    if (success) {
                                        dialog.dismiss();
                                    } else {
                                        FabAlert.alert(requireContext(), "stop failed!");
                                    }
                                }, e -> {
                                    dialog.dismiss();
                                    LogHelper.log(e);
                                    mViewModel.setMoving(false);
                                });
                    } else {
                        getViewModel().requestStopExtruderUnload(which)
                                .observeOn(AndroidSchedulers.mainThread())
                                .as(bindToLifecycle())
                                .subscribe(success -> {
                                    mViewModel.setMoving(false);
                                    if (success) {
                                        dialog.dismiss();
                                    } else {
                                        FabAlert.alert(requireContext(), "stop failed!");
                                    }
                                }, e -> {
                                    dialog.dismiss();
                                    LogHelper.log(e);
                                    mViewModel.setMoving(false);
                                });
                    }
                }).show();
    }

    private void showPreHeatTempDialog() {
        FabConfirm.create(requireContext())
                .setCanceledOnTouchOutSide(false)
                .setDescription(R.string.dual_extruder_load_filament_pre_heated_dialog_desc)
                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                    // Set up temperature up to 200 degree.
                    mRvLeftExtruder.setCurrentValue(200);
                    mRvRightExtruder.setCurrentValue(200);
                    mViewModel.preHeatedHeadTemperature();
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .as(bindToLifecycle())
//                            .subscribe(result -> {
//                                // Do nothing.
//                            }, LogHelper::log);
                    dialog.dismiss();
                }).show();
    }

    private boolean isViewNull(@Nullable View view) {
        return (view == null);
    }

    private String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }
}
