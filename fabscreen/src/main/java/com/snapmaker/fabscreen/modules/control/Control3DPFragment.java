package com.snapmaker.fabscreen.modules.control;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZBWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.HeatedBedWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.LoadFilamentWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.NozzleWidgetPresenter;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Control3DPFragment extends BaseFragment {
    private static final String TAG = "Control3DPFragment";

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.bb_control_bottom_bar)
    BottomBar mBbBottomBar;
    @BindView(R.id.vp_control_3dp)
    ViewPager mControlViewPager;

    // 4th Axes
    @BindView(R.id.vp_control_4axis_panels)
    ViewPager mVpControlPanel;
    @BindView(R.id.tl_control_4axis_panel_indicator)
    TabLayout mTlControlPanel;
    @BindView(R.id.widget_control_panel_xyz_axes_for_4axis)
    View mViewControlPanel;
    @BindView(R.id.widget_control_panel_b_axis)
    View mViewRotaryPanel;

    @BindView(R.id.view_control_3dp_page_axes)
    View mViewPageAxes;
    @BindView(R.id.view_control_3dp_page_4axis)
    View mViewPage4Axes;
    @BindView(R.id.view_control_3dp_page_nozzle)
    View mViewPageNozzle;
    @BindView(R.id.view_control_3dp_page_heated_bed)
    View mViewPageHeatedBed;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;

    // axes
    private CoordinateXYZWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlXYZPanelWidgetPresenter;

    private boolean mIsRotaryAvailable = false;
    private CoordinateXYZBWidgetPresenter mCoordinateXYZBWidgetPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelWidgetPresenter;

    // nozzle
    private NozzleWidgetPresenter mNozzleWidgetPresenter;
    private LoadFilamentWidgetPresenter mLoadFilamentWidgetPresenter;

    // heated bed
    private HeatedBedWidgetPresenter mHeatedBedWidgetPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsRotaryAvailable = getModel().getMachineController().isRotaryModuleAvailable();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.all_control);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_control_3dp;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        initBottomBar();
        initViewPager();

        if (mIsRotaryAvailable) {
            init4AxisPage();
        } else {
            initAxesPage();
        }

        initNozzlePage();
        initControlHeatedBed();

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(0);
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_jog_mode, R.drawable.btn_all_axes_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_nozzle, R.drawable.btn_all_nozzle_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_heated_bed, R.drawable.btn_all_heated_bed_normal_64x64));
        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position -> mControlViewPager.setCurrentItem(position));
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mIsRotaryAvailable ? mViewPage4Axes : mViewPageAxes);
        views.add(mViewPageNozzle);
        views.add(mViewPageHeatedBed);

        ViewPagerAdapter adapter = new ViewPagerAdapter(views);
        mControlViewPager.setAdapter(adapter);
        mControlViewPager.setOffscreenPageLimit(3);
        mControlViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mBbBottomBar.selectTab(position);

                // Alert user to set nozzle target temperature.
                if (position == 1) {
                    FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
                    if (machineStatus.headTargetTemperature == 0) {
                        FabConfirm.create(getContext())
                                .setDescription(R.string.control_heat_warning)
                                .setConfirm(R.string.all_confirm, (dialog, which) -> {
                                    dialog.dismiss();
                                    mNozzleWidgetPresenter.setTargetValue(200);
                                })
                                .setCancel(R.string.all_cancel, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mControlViewPager.setCurrentItem(0);
    }

    private void initAxesPage() {
        mCoordinateXYZWidgetPresenter = new CoordinateXYZWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZWidgetPresenter.bind(mViewPageAxes);
        mCoordinateXYZWidgetPresenter.connect();

        mControlXYZPanelWidgetPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelWidgetPresenter.bind(mViewPageAxes, 0.1f, 1f, 10f);
        mControlXYZPanelWidgetPresenter.connect();
    }

    private void init4AxisPage() {
        mCoordinateXYZBWidgetPresenter = new CoordinateXYZBWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZBWidgetPresenter.bind(mViewPage4Axes);
        mCoordinateXYZBWidgetPresenter.connect();

        initControlPanelViewPager();

        // xyz axes control
        mControlXYZPanelWidgetPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelWidgetPresenter.bind(mViewPage4Axes, 0.1f, 1f, 5f);
        mControlXYZPanelWidgetPresenter.connect();

        // b axis control
        mControlBAxisPanelWidgetPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlBAxisPanelWidgetPresenter.bind(mViewPage4Axes);
        mControlBAxisPanelWidgetPresenter.connect();
    }

    private void initControlPanelViewPager() {
        ArrayList<View> controlPanelsViews = new ArrayList<>();
        controlPanelsViews.add(mViewControlPanel);
        controlPanelsViews.add(mViewRotaryPanel);

        ViewPagerAdapter adapter = new ViewPagerAdapter(controlPanelsViews);
        mVpControlPanel.setAdapter(adapter);

        mTlControlPanel.setupWithViewPager(mVpControlPanel);
        mTlControlPanel.setEnabled(false);
    }

    private void initNozzlePage() {
        mNozzleWidgetPresenter = new NozzleWidgetPresenter(getContext(), getModel(), disposables);
        mNozzleWidgetPresenter.bind(mViewPageNozzle);
        mNozzleWidgetPresenter.connectMachineStatus();

        mLoadFilamentWidgetPresenter = new LoadFilamentWidgetPresenter(getContext(), getModel(), disposables);
        mLoadFilamentWidgetPresenter.bind(mViewPageNozzle);
        mLoadFilamentWidgetPresenter.connect();
        mLoadFilamentWidgetPresenter.getIsLoadingObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(isLoading -> mBtnBack.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE));
    }

    private void initControlHeatedBed() {
        mHeatedBedWidgetPresenter = new HeatedBedWidgetPresenter(getContext(), getModel(), disposables);
        mHeatedBedWidgetPresenter.bind(mViewPageHeatedBed);
        mHeatedBedWidgetPresenter.connectMachineStatus();
    }

    private Observable<Boolean> turnOffHead() {
        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
        boolean isDualExtruder = getModel().getMachineController().getHeadType() == Constants.HEAD_3DP_DUAL_EXTRUDER;

        if (machineStatus.headTargetTemperature > 0) {
            return getModel().getSlaveComputer().sendGcode("M104 S0").flatMap(response -> {
                if (isDualExtruder && machineStatus.extruder1TargetTemperature > 0) {
                    return getModel().getSlaveComputer().sendGcode("M104 T1 S0");
                } else {
                    return Observable.just(response);
                }
            }).map(response -> true);
        } else {
            return Observable.just(true);
        }
    }

    private Observable<Boolean> turnOffBed() {
        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();
        if (machineStatus.bedTargetTemperature > 0) {
            return getModel().getSlaveComputer().sendGcode("M140 S0").map(response -> true);
        } else {
            return Observable.just(true);
        }
    }

    @OnClick(R.id.top_bar_back)
    void onBack() {
        turnOffHead()
                .flatMap(success -> turnOffBed())
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> super.back(), LogHelper::log);
    }
}
