package com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.AirPurifierControlWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.DualExtruderFlowRateWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.DualExtruderNozzleWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.DualExtruderZOffsetWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.EnclosureControlWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.FeedRateWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.HeatedBedWidgetPresenter;


import java.util.ArrayList;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.NoScrollViewPager;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GuideXYCalibrationPrintDrawer3DPDualExtruderFragment extends BaseFragment implements GuideDualExtruderXYCalibrationActivity.OnDrawerOpenedListener {
    private static final String TAG = "PrintDrawer3DPDualExtruder";

    @BindView(R.id.bb_print_drawer_bottom_bar)
    BottomBar mBbBottomBar;
    @BindView(R.id.vp_print_drawer_view_pager)
    NoScrollViewPager mVpViewPager;

    @BindView(R.id.widget_dual_extruder_load_filament)
    View mViewPageDualExtruder;
    @BindView(R.id.view_print_drawer_3dp_page_heated_bed)
    View mViewPageHeatedBed;
    @BindView(R.id.view_print_settings_3dp_page_left_z_offset)
    View mViewPageLeftZOffset;
    @BindView(R.id.view_print_settings_3dp_page_right_z_offset)
    View mViewPageRightZOffset;
    @BindView(R.id.view_print_settings_3dp_page_work_speed)
    View mViewPageFeedRate;
    @BindView(R.id.view_print_settings_3dp_dual_extruder_page_flow_rate)
    View mViewPageFlowRate;
    @BindView(R.id.view_print_settings_add_on_enclosure)
    View mViewPageEnclosure;
    @BindView(R.id.view_print_settings_add_on_air_purifier)
    View mViewPageAirPurifier;
    @BindView(R.id.hsv_bottom_bar)
    HorizontalScrollView mViewBottomBar;

    private boolean mIsEnclosurePlugged;
    private boolean mIsAirPurifierPlugged;

//    private NozzleWidgetPresenter mNozzleWidgetPresenter;
    private DualExtruderNozzleWidgetPresenter mDualExtruderNozzleWidgetPresenter;
    private HeatedBedWidgetPresenter mHeatedBedWidgetPresenter;
    private DualExtruderZOffsetWidgetPresenter mDualExtruderLeftZOffsetWidgetPresenter;
    private DualExtruderZOffsetWidgetPresenter mDualExtruderRightZOffsetWidgetPresenter;
    private FeedRateWidgetPresenter mFeedRateWidgetPresenter;
    private DualExtruderFlowRateWidgetPresenter mDualExtruderFlowRateWidgetPresenter;
    private EnclosureControlWidgetPresenter mEnclosureControlWidgetPresenter;
    private AirPurifierControlWidgetPresenter mAirPurifierControlWidgetPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsEnclosurePlugged = getModel().getMachineController().isEnclosureReady();
        mIsAirPurifierPlugged = getModel().getMachineController().isAirPurifierPlugged();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_drawer_3dp_dual_extruder;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        initBottomBar();
        initViewPager();
        initNozzlePage();
        initHeatedBedPage();
        initLeftZOffsetPage();
        initRightZOffsetPage();
        initFeedRatePage();
        initDualExtruderFlowRatePage();

        if (mIsEnclosurePlugged) {
            initEnclosurePage();
        }

        if (mIsAirPurifierPlugged) {
            initAirPurifierPage();
        }


        if (getActivity() != null) {
            ((GuideDualExtruderXYCalibrationActivity) getActivity()).setOnDrawerOpenedListener(this);
        }
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_nozzle, R.drawable.btn_all_nozzle_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_heated_bed, R.drawable.btn_all_heated_bed_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_left_z_offset_with_crlf, R.drawable.btn_all_z_offset_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_right_z_offset_with_crlf, R.drawable.btn_all_z_offset_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_feed_rate, R.drawable.btn_all_work_speed_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_flow_rate, R.drawable.btn_all_flow_rate_normal_64x64));

        // enclosure
        if (mIsEnclosurePlugged) {
            mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.all_enclosure, R.drawable.btn_all_enclosure_control_64x56));
        }

        if (mIsAirPurifierPlugged) {
            mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.all_air_purifier, R.drawable.btn_all_air_purifier_normal_64x64));
        }

        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position -> mVpViewPager.setCurrentItem(position));
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mViewPageDualExtruder);
        views.add(mViewPageHeatedBed);
        views.add(mViewPageLeftZOffset);
        views.add(mViewPageRightZOffset);
        views.add(mViewPageFeedRate);
        views.add(mViewPageFlowRate);

        if (mIsEnclosurePlugged) {
            views.add(mViewPageEnclosure);
        }
        if (mIsAirPurifierPlugged) {
            views.add(mViewPageAirPurifier);
        }


        ViewPagerAdapter adapter = new ViewPagerAdapter(views);
        mVpViewPager.setAdapter(adapter);
        mVpViewPager.setOffscreenPageLimit(views.size());
        mVpViewPager.setCurrentItem(0);
    }

    private void initNozzlePage() {
        LayoutInflater inflater = getLayoutInflater();
        mDualExtruderNozzleWidgetPresenter = new DualExtruderNozzleWidgetPresenter(getContext(), getModel(), disposables, inflater);
        mDualExtruderNozzleWidgetPresenter.bind(mViewPageDualExtruder);
        mDualExtruderNozzleWidgetPresenter.connectPrint();
    }

    private void initDualExtruderPage() {

    }

    private void initHeatedBedPage() {
        mHeatedBedWidgetPresenter = new HeatedBedWidgetPresenter(getContext(), getModel(), disposables);
        mHeatedBedWidgetPresenter.bind(mViewPageHeatedBed);
        mHeatedBedWidgetPresenter.connectPrint();
    }

    private void initLeftZOffsetPage() {
        mDualExtruderLeftZOffsetWidgetPresenter = new DualExtruderZOffsetWidgetPresenter(getContext(), getModel(), disposables);
        mDualExtruderLeftZOffsetWidgetPresenter.bind(0, mViewPageLeftZOffset);
        // Get live ZOffset for each print, modification persists when machine restarts.
        getModel().getSlaveComputer()
                .getLiveZOffset(0)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
                    Logger.d("get live left ZOffset %.1f", value);
                    mDualExtruderLeftZOffsetWidgetPresenter.connectPrint(0, value);
                }, LogHelper::log);
    }

    private void initRightZOffsetPage() {
        mDualExtruderRightZOffsetWidgetPresenter = new DualExtruderZOffsetWidgetPresenter(getContext(), getModel(), disposables);
        mDualExtruderRightZOffsetWidgetPresenter.bind(1, mViewPageRightZOffset);
        // Get live ZOffset for each print, modification persists when machine restarts.
        getModel().getSlaveComputer()
                .getLiveZOffset(1)
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(value -> {
                    Logger.d("get live right ZOffset %.1f", value);
                    mDualExtruderRightZOffsetWidgetPresenter.connectPrint(1, value);
                }, LogHelper::log);
    }

    private void initFeedRatePage() {
        mFeedRateWidgetPresenter = new FeedRateWidgetPresenter(getContext(), getModel(), disposables);
        mFeedRateWidgetPresenter.bind(mViewPageFeedRate);
        if (getModel().getPrintController().getRecoveryFlag()) {
            getModel().getSlaveComputer().getAdjustSettingFeedRate()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(adjustSettings -> {
                        Logger.d("get feedRate from power loss %.1f", adjustSettings.value);
                        mFeedRateWidgetPresenter.connectPrint(adjustSettings.value);
                    }, LogHelper::log);
        } else {
            mFeedRateWidgetPresenter.connectPrint();
        }
    }

    private void initDualExtruderFlowRatePage() {
        mDualExtruderFlowRateWidgetPresenter = new DualExtruderFlowRateWidgetPresenter(getContext(), getModel(), disposables);
        mDualExtruderFlowRateWidgetPresenter.bind(mViewPageFlowRate);
        mDualExtruderFlowRateWidgetPresenter.connectPrint();
    }

    private void initEnclosurePage() {
        mEnclosureControlWidgetPresenter = new EnclosureControlWidgetPresenter(getContext(), getModel(), disposables);
        mEnclosureControlWidgetPresenter.bind(mViewPageEnclosure);
        mEnclosureControlWidgetPresenter.connectStatus();
    }

    private void initAirPurifierPage() {
        mAirPurifierControlWidgetPresenter = new AirPurifierControlWidgetPresenter(getContext(), getModel(), disposables);
        mAirPurifierControlWidgetPresenter.bind(mViewPageAirPurifier);
        mAirPurifierControlWidgetPresenter.connectStatus();
    }

    @Override
    public void onDrawerOpened() {
        FabPacketContent.MachineStatus machineStatus = getModel().getSlaveComputer().getMachineStatus();

        if (!getModel().getPrintController().getOverrideNozzleTemperatureDirty()) {
            mDualExtruderNozzleWidgetPresenter.updateTargetTempView(0, machineStatus.headTargetTemperature);
            mDualExtruderNozzleWidgetPresenter.updateTargetTempView(1, machineStatus.extruder1TargetTemperature);
        }
        if (!getModel().getPrintController().getOverrideHeatedBedTemperatureDirty()) {
            mHeatedBedWidgetPresenter.setTargetValue(machineStatus.bedTargetTemperature);
        }
    }

    @Override
    public void onDrawerClosed() {
        mBbBottomBar.selectTab(0);
        mVpViewPager.setCurrentItem(0);
        mViewBottomBar.fullScroll(View.FOCUS_LEFT);
    }

    @Override
    protected void back() {
        GuideDualExtruderXYCalibrationActivity activity = (GuideDualExtruderXYCalibrationActivity) getContext();
        if (activity != null) {
            activity.closeDrawer();
        }
    }
}
