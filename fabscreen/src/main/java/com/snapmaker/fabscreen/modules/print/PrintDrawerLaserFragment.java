package com.snapmaker.fabscreen.modules.print;

import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.AirPurifierControlWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.EnclosureControlWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.FeedRateWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.LaserPowerWidgetPresenter;
import fabscreen.libraries.legacy.view.NoScrollViewPager;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;

import java.util.ArrayList;

import butterknife.BindView;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintDrawerLaserFragment extends BaseFragment implements PrintActivity.OnDrawerOpenedListener {
    private static final String TAG = "PrintDrawerLaser";

    @BindView(R.id.bb_print_drawer_bottom_bar)
    BottomBar mBbBottomBar;
    @BindView(R.id.vp_print_drawer_view_pager)
    NoScrollViewPager mVpViewPager;

    @BindView(R.id.view_print_settings_laser_page_power)
    View mViewPagePower;
    @BindView(R.id.view_print_settings_laser_page_work_speed)
    View mViewPageFeedRate;
    @BindView(R.id.view_print_settings_add_on_enclosure)
    View mViewPageEnclosure;
    @BindView(R.id.view_print_settings_add_on_air_purifier)
    View mViewPageAirPurifier;
    @BindView(R.id.hsv_bottom_bar)
    HorizontalScrollView mViewBottomBar;

    private boolean mIsEnclosurePlugged;
    private boolean mIsAirPurifierPlugged;

    private LaserPowerWidgetPresenter mLaserPowerWidgetPresenter;
    private FeedRateWidgetPresenter mFeedRateWidgetPresenter;
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
        return R.layout.fragment_print_drawer_laser;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        initBottomBar();
        initViewPager();
        initLaserPowerPage();
        initFeedRatePage();
        if (mIsEnclosurePlugged) {
            initEnclosurePage();
        }

        if (mIsAirPurifierPlugged) {
            initAirPurifierPage();
        }
        if (getActivity() != null) {
            ((PrintActivity) getActivity()).setOnDrawerOpenedListener(this);
        }
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_laser_power, R.drawable.btn_all_laser_power_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_feed_rate, R.drawable.btn_all_work_speed_normal_64x64));

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

        views.add(mViewPagePower);
        views.add(mViewPageFeedRate);

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

    private void initLaserPowerPage() {
        mLaserPowerWidgetPresenter = new LaserPowerWidgetPresenter(getContext(), getModel(), disposables);
        mLaserPowerWidgetPresenter.bind(mViewPagePower);
        if (getModel().getPrintController().getRecoveryFlag()) {
            getModel().getSlaveComputer()
                    .getAdjustSettingLaserPower()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(adjustSettings -> {
                        Logger.d("get Laser Power from power loss %.1f", adjustSettings.value);
                        mLaserPowerWidgetPresenter.connectPrint(adjustSettings.value);
                    }, LogHelper::log);
        } else {
            mLaserPowerWidgetPresenter.connectPrint();
        }
    }

    private void initFeedRatePage() {
        mFeedRateWidgetPresenter = new FeedRateWidgetPresenter(getContext(), getModel(), disposables);
        mFeedRateWidgetPresenter.bind(mViewPageFeedRate);
        if (getModel().getPrintController().getRecoveryFlag()) {
            getModel().getSlaveComputer()
                    .getAdjustSettingFeedRate()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(adjustSettings -> {
                        mFeedRateWidgetPresenter.connectPrint(adjustSettings.value);
                    }, LogHelper::log);
        } else {
            mFeedRateWidgetPresenter.connectPrint();
        }

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

        if (!getModel().getPrintController().getOverrideLaserPowerDirty()) {
            mLaserPowerWidgetPresenter.setTargetValue((float) machineStatus.laserPower);
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
        PrintActivity activity = (PrintActivity) getContext();
        if (activity != null) {
            activity.closeDrawer();
        }
    }
}
