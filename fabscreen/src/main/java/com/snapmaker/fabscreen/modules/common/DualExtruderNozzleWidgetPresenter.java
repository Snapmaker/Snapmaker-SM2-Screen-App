package com.snapmaker.fabscreen.modules.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.loadfilament.DualExtruderTemperature;
import com.snapmaker.fabscreen.modules.loadfilament.LoadFilamentPageAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ActionButton;
import fabscreen.libraries.legacy.view.FabAlert;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.NoScrollViewPager;
import fabscreen.libraries.legacy.view.RulerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

public class DualExtruderNozzleWidgetPresenter extends BasePresenter {
    private final static int FILAMENT_MELT_MIN_TEMP = 170;
    private final static int TEMPERATURE_HEATED_TOLERANCE = 3;

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

    private LayoutInflater mInflater;
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

    private BehaviorSubject<DualExtruderTemperature> mDualExtruderTemperatureSubject = BehaviorSubject.createDefault(new DualExtruderTemperature());
    private BehaviorSubject<Integer> mActiveExtruderSubject = BehaviorSubject.createDefault(0);
    private BehaviorSubject<Boolean> mIsMovingSubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mLeftExtruderReadySubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<Boolean> mRightExtruderReadySubject = BehaviorSubject.createDefault(false);
    private BehaviorSubject<DualExtruderTemperature> mTargetValueSubject = BehaviorSubject.createDefault(new DualExtruderTemperature());


    public DualExtruderNozzleWidgetPresenter(Context context, Model model, CompositeDisposable compositeDisposable, LayoutInflater inflater) {
        super(context, model, compositeDisposable);
        mInflater = inflater;
    }

    public void bind(View view) {
        ButterKnife.bind(this, view);

        mLeftExtruderPage = mInflater.inflate(R.layout.fragment_control_3dp_page_nozzle, null);
        mRightExtruderPage = mInflater.inflate(R.layout.fragment_control_3dp_page_nozzle, null);
        if (mLeftExtruderPage == null || mRightExtruderPage == null) {
            Logger.e("Error: adding null view into view pagers, initialization aborted.");
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
        bindPageEvents();
        bindDualExtruderStatus();
    }

    public void connectStatus() {
        bindExtrudersButtons();

        Disposable sub = mTargetValueSubject
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(dualTemp -> {
                    DualExtruderTemperature currentTemp = getDualExtruderTemperature();
                    if (currentTemp.getExtruder0TargetTemperature() != dualTemp.getExtruder0TargetTemperature()) {
                        sendTargetTemperature(0, dualTemp.getExtruder0TargetTemperature());
                    }

                    if (currentTemp.getExtruder1TargetTemperature() != dualTemp.getExtruder1TargetTemperature()) {
                        sendTargetTemperature(1, dualTemp.getExtruder1TargetTemperature());
                    }
                });
        addDisposable(sub);
    }

    public void connectPrint() {
        // Left Extruder Page
        ActionButton btnLeftExtruderLoad = mLeftExtruderPage.findViewById(R.id.btn_widget_load_filament_load);
        ActionButton btnLeftExtruderUnload = mLeftExtruderPage.findViewById(R.id.btn_widget_load_filament_unload);
        if (isViewNull(btnLeftExtruderLoad) || isViewNull(btnLeftExtruderUnload)) return;

        btnLeftExtruderLoad.setVisibility(Button.GONE);
        btnLeftExtruderUnload.setVisibility(Button.GONE);

        // Right Extruder Page
        ActionButton btnRightExtruderLoad = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_load);
        ActionButton btnRightExtruderUnload = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_unload);
        if (isViewNull(btnRightExtruderLoad) || isViewNull(btnRightExtruderUnload)) return;

        btnRightExtruderLoad.setVisibility(Button.GONE);
        btnRightExtruderUnload.setVisibility(Button.GONE);

        TextView tvLeftExtruderFilamentText = mLeftExtruderPage.findViewById(R.id.tv_widget_load_filament_text);
        TextView tvRightExtruderFilamentText = mRightExtruderPage.findViewById(R.id.tv_widget_load_filament_text);
        if (isViewNull(tvLeftExtruderFilamentText) || isViewNull(tvRightExtruderFilamentText)) return;
        tvLeftExtruderFilamentText.setVisibility(TextView.GONE);
        tvRightExtruderFilamentText.setVisibility(TextView.GONE);

        Disposable sub = mTargetValueSubject
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(dualTemp -> {
                    DualExtruderTemperature currentTemp = getDualExtruderTemperature();
                    if (currentTemp.getExtruder0TargetTemperature() != dualTemp.getExtruder0TargetTemperature()) {
                        sendTargetTemperatureOnPrint(0, dualTemp.getExtruder0TargetTemperature());
                    }

                    if (currentTemp.getExtruder1TargetTemperature() != dualTemp.getExtruder1TargetTemperature()) {
                        sendTargetTemperatureOnPrint(1, dualTemp.getExtruder1TargetTemperature());
                    }
                });
        addDisposable(sub);
    }

    protected String formatValue(float value) {
        return String.format(Locale.US, "%d", (int) value);
    }


    private void initNozzleTab() {
        mTlNozzleTab.setupWithViewPager(mVpViewPager);
        mTlNozzleTab.getTabAt(LEFT_EXTRUDER).setText(getContext().getString(R.string.dual_extruder_load_filament_nozzle_l));
        mTlNozzleTab.getTabAt(RIGHT_EXTRUDER).setText(getContext().getString(R.string.dual_extruder_load_filament_nozzle_r));

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
        DualExtruderTemperature temperatures0 = getDualExtruderTemperature();
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

        mTargetValueSubject.onNext(temperatures0);

        Disposable sub = getExtrudersTemperatureObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(temperatures -> {
                    mTvLeftTemperature.setText((formatValue(temperatures.getExtruder0Temperature())));
                    mTvRightTemperature.setText((formatValue(temperatures.getExtruder1Temperature())));
                    mTvLeftCurrentValue.setText((formatValue(temperatures.getExtruder0Temperature())));
                    mTvRightCurrentValue.setText((formatValue(temperatures.getExtruder1Temperature())));
                    mTvLeftTargetValue.setText(formatValue(temperatures.getExtruder0TargetTemperature()));
                    mTvRightTargetValue.setText(formatValue(temperatures.getExtruder1TargetTemperature()));
                });
        addDisposable(sub);
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
            float pow = (float) Math.pow(10, 0);
            float newValue = (int) (value * pow) / pow;
            setDualExtruderTargetValue(LEFT_EXTRUDER, newValue);
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
            float pow = (float) Math.pow(10, 0);
            float newValue = (int) (value * pow) / pow;
            setDualExtruderTargetValue(RIGHT_EXTRUDER, newValue);
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
            mIsMovingSubject.onNext(true);
            btnLeftExtruderLoad.setEnabled(false);
            Disposable sub = requestExtruderLoad(LEFT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result) {
                            showExtruderOperatingDialog(0, 0);
                        } else {
                            mIsMovingSubject.onNext(false);
                        }

                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                        btnLeftExtruderLoad.setEnabled(true);
                    });
            addDisposable(sub);
        });

        btnLeftExtruderUnload.setOnClickListener(v -> {
            mIsMovingSubject.onNext(true);
            btnLeftExtruderUnload.setEnabled(false);
            Disposable sub = requestExtruderUnload(LEFT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result) {
                            showExtruderOperatingDialog(0, 1);
                        } else {
                            mIsMovingSubject.onNext(false);
                        }

                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                        btnLeftExtruderUnload.setEnabled(true);
                    });
            addDisposable(sub);
        });

        // Right Extruder Page
        ActionButton btnRightExtruderLoad = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_load);
        ActionButton btnRightExtruderUnload = mRightExtruderPage.findViewById(R.id.btn_widget_load_filament_unload);
        if (isViewNull(btnRightExtruderLoad) || isViewNull(btnRightExtruderUnload)) return;

        btnRightExtruderLoad.setEnabled(false);
        btnRightExtruderUnload.setEnabled(false);

        btnRightExtruderLoad.setOnClickListener(v -> {
            btnRightExtruderLoad.setEnabled(false);
            mIsMovingSubject.onNext(true);

            Disposable sub = requestExtruderLoad(RIGHT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result) {
                            // Show extruding dialog
                            showExtruderOperatingDialog(1, 0);
                        } else {
                            mIsMovingSubject.onNext(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                        btnRightExtruderLoad.setEnabled(true);
                    });
            addDisposable(sub);
        });

        btnRightExtruderUnload.setOnClickListener(v -> {
            btnRightExtruderUnload.setEnabled(false);
            mIsMovingSubject.onNext(true);

            Disposable sub = requestExtruderUnload(RIGHT_EXTRUDER)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result) {
                            // Show extruding dialog
                            showExtruderOperatingDialog(1, 1);
                        } else {
                            mIsMovingSubject.onNext(false);
                        }
                    }, e -> {
                        LogHelper.log(e);
                        mIsMovingSubject.onNext(false);
                        btnRightExtruderUnload.setEnabled(true);
                    });
            addDisposable(sub);
        });

        // Bind button enabled status with observable.
        Disposable sub = Observable.combineLatest(getExtruderReadyObservable(0), mIsMovingSubject,
                (IsTempReady, isMoving) -> IsTempReady && !isMoving)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ready -> {
                    btnLeftExtruderLoad.setEnabled(ready);
                    btnLeftExtruderUnload.setEnabled(ready);
                });
        addDisposable(sub);

        sub = Observable.combineLatest(getExtruderReadyObservable(1), mIsMovingSubject,
                (IsTempReady, isMoving) -> IsTempReady && !isMoving)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ready -> {
                    btnRightExtruderLoad.setEnabled(ready);
                    btnRightExtruderUnload.setEnabled(ready);
                });
        addDisposable(sub);
    }


    // pasted from ViewModel

    private void bindDualExtruderStatus() {
        // Subscribe dual extruder temperature.
        Disposable sub = getModel().getMachineController()
                .getMachineStatusObservable()
                .throttleLast(Constants.THROTTLE_DURATION, TimeUnit.MILLISECONDS)
                .subscribe(machineStatus -> {
                    DualExtruderTemperature temp = new DualExtruderTemperature();
                    temp.setExtruder0Temp(machineStatus.headTemperature);
                    temp.setExtruder0TargetTemp(machineStatus.headTargetTemperature);
                    temp.setExtruder1Temperature(machineStatus.extruder1Temperature);
                    temp.setExtruder1TargetTemperature(machineStatus.extruder1TargetTemperature);
                    mDualExtruderTemperatureSubject.onNext(temp);

                    // Check extruder was ready(to extrude or retract) due to current temperature.
                    checkExtruderReadyToUse(temp);
                });
        addDisposable(sub);



        updateActiveExtruder();
    }

    public DualExtruderTemperature getDualExtruderTemperature() {
        FabPacketContent.MachineStatus status = getModel().getMachineController().getMachineStatus();
        DualExtruderTemperature data = new DualExtruderTemperature();
        data.setExtruder0Temp(status.headTemperature);
        data.setExtruder0TargetTemp(status.headTargetTemperature);
        data.setExtruder1Temperature(status.extruder1Temperature);
        data.setExtruder1TargetTemperature(status.extruder1TargetTemperature);
        return data;
    }

    public int getActiveExtruder() {
        return mActiveExtruderSubject.getValue();
    }


    public void sendTargetTemperature(int which, float value) {
        String stringExtruder = which == LEFT_EXTRUDER ? "T0" : "T1";
        Disposable sub = getModel().getSlaveComputer().sendGcode("M104 " + stringExtruder + " S" + value)
                .subscribe(response -> {/**/}, LogHelper::log);
        addDisposable(sub);
    }

    public void sendTargetTemperatureOnPrint(int which, float value) {
        getModel().getPrintController().setOverrideNozzleTemperature(which, value);
    }

    public Observable<DualExtruderTemperature> getExtrudersTemperatureObservable() {
        return mDualExtruderTemperatureSubject.hide();
    }

    public Observable<Boolean> getExtruderReadyObservable(int which) {
        // We onNext extruder ready (or not) in `checkExtruderReadyToUse`
        // Return observable directly here.
        return which == 0 ? mLeftExtruderReadySubject.hide() : mRightExtruderReadySubject.hide();
    }

    private void checkExtruderReadyToUse(DualExtruderTemperature temperature) {
        float temp0 = temperature.getExtruder0Temperature();
        float temp1 = temperature.getExtruder1Temperature();
        float targetTemp0 = temperature.getExtruder0TargetTemperature();
        float targetTemp1 = temperature.getExtruder1TargetTemperature();

        // T0
        if (targetTemp0 == 0 || mIsMovingSubject.getValue()) {
            mLeftExtruderReadySubject.onNext(false);
        } else {
            boolean isReachTargetTemp = (temp0 > targetTemp0 - TEMPERATURE_HEATED_TOLERANCE)
                    && temp0 > FILAMENT_MELT_MIN_TEMP;
            mLeftExtruderReadySubject.onNext(isReachTargetTemp);
        }

        // T1
        if (targetTemp1 == 0 || mIsMovingSubject.getValue()) {
            mRightExtruderReadySubject.onNext(false);
        } else {
            boolean isReachTargetTemp = (temp1 > targetTemp1 - TEMPERATURE_HEATED_TOLERANCE)
                    && temp1 > FILAMENT_MELT_MIN_TEMP;
            mRightExtruderReadySubject.onNext(isReachTargetTemp);
        }
    }

    public void updateActiveExtruder() {
        Disposable sub = getModel().getSlaveComputer().getActivatedExtruder()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(which -> {
                    mActiveExtruderSubject.onNext(which);
                }, LogHelper::log);
        addDisposable(sub);
    }

    private Observable<Boolean> switchExtruder(int which) {
        return getModel().getSlaveComputer().switchToExtruder(which).doOnNext(result -> updateActiveExtruder());
    }

    public Observable<Boolean> requestExtruderLoad(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestExtruderLoad(which));
        }

        return getModel().getSlaveComputer().extrudeInfinitely(0, 5);
    }

    public Observable<Boolean> requestExtruderUnload(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestExtruderUnload(which));
        }

        return getModel().getSlaveComputer().extrudeInfinitely(1, 5);
    }

    public Observable<Boolean> requestStopExtruderLoad(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestStopExtruderLoad(which));
        }

        return getModel().getSlaveComputer().stopMovement();
    }

    public Observable<Boolean> requestStopExtruderUnload(int which) {
        if (getActiveExtruder() != which) {
            // Switch extruder and continue loading.
            return switchExtruder(which)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .flatMap(result -> requestStopExtruderUnload(which));
        }

        return getModel().getSlaveComputer().stopMovement();
    }

    public void setDualExtruderTargetValue(int which, float targetValue) {
        DualExtruderTemperature lastValue = mTargetValueSubject.getValue();
        float lastTemp = 0;
        if (which == LEFT_EXTRUDER) {
            lastTemp =  lastValue.getExtruder0Temperature();
            if (targetValue != lastTemp || targetValue == 0) {
                DualExtruderTemperature newTemp = lastValue;
                newTemp.setExtruder0TargetTemp(targetValue);
                mTargetValueSubject.onNext(newTemp);
            }
        } else {
            lastTemp = lastValue.getExtruder1Temperature();
            if (targetValue != lastTemp || targetValue == 0) {
                DualExtruderTemperature newTemp = lastValue;
                newTemp.setExtruder1TargetTemperature(targetValue);
                mTargetValueSubject.onNext(newTemp);
            }
        }
    }

    public Observable<Boolean> preHeatedHeadTemperature() {
        return getModel().getSlaveComputer().sendGcode("M104 T1 S200")
                .flatMap(result -> getModel().getSlaveComputer().sendGcode("M104 T0 S200"))
                .map(response -> true);
    }

    public void preheatedTemperature() {
        setDualExtruderTargetValue(0, 200);
        setDualExtruderTargetValue(1, 200);
        mRvLeftExtruder.setCurrentValue(200);
        mRvRightExtruder.setCurrentValue(200);
    }

    public void updateTargetTempView(int which, float temperature) {
        if (which == 0) {
            mRvLeftExtruder.setCurrentValue(temperature);
        } else {
            mRvRightExtruder.setCurrentValue(temperature);
        }
    }

    /**
     *
     * @param which 0 means Left extruder, 1 means Right extruder
     *
     * @param operation 0 means load filament in, 1 means unload filament out.
     */
    public void showExtruderOperatingDialog(int which, int operation) {
        boolean isIntentToLoad = operation == 0;
        int dialogIconRes = operation == 0 ? R.drawable.btn_control_load_normal_50x50 : R.drawable.btn_control_unload_normal_50x50;
        int stringDescResId = R.string.dual_extruder_load_filament_loading_preparing;
        if (which == 0) {
            stringDescResId = isIntentToLoad ?  R.string.dual_extruder_load_filament_loading_left : R.string.dual_extruder_load_filament_unloading_left;
        } else {
            stringDescResId = isIntentToLoad ? R.string.dual_extruder_load_filament_loading_right : R.string.dual_extruder_load_filament_unloading_right;
        }
        FabConfirm.create(getContext()).setCanceledOnTouchOutSide(false).setIcon(dialogIconRes)
                .setDescription(stringDescResId)
                .setConfirm(R.string.all_stop, (dialog, which1) -> {
                    Disposable sub;
                    if (isIntentToLoad) {
                        sub = requestStopExtruderLoad(which)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(success -> {
                                    mIsMovingSubject.onNext(false);
                                    if (success) {
                                        dialog.dismiss();
                                    } else {
                                        FabAlert.alert(getContext(), "stop failed!");
                                    }
                                }, e -> {
                                    dialog.dismiss();
                                    LogHelper.log(e);
                                    mIsMovingSubject.onNext(false);
                                });
                    } else {
                        sub = requestStopExtruderUnload(which)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(success -> {
                                    mIsMovingSubject.onNext(false);
                                    if (success) {
                                        dialog.dismiss();
                                    } else {
                                        FabAlert.alert(getContext(), "stop failed!");
                                    }
                                }, e -> {
                                    dialog.dismiss();
                                    LogHelper.log(e);
                                    mIsMovingSubject.onNext(false);
                                });
                    }
                    addDisposable(sub);
                }).show();
    }


    private boolean isViewNull(@Nullable View view) {
        return (view == null);
    }
}
