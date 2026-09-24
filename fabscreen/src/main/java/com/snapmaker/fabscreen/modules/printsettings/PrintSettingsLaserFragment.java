package com.snapmaker.fabscreen.modules.printsettings;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.FeedRateWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.LaserPowerWidgetPresenter;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;

import java.util.ArrayList;

import butterknife.BindView;

public class PrintSettingsLaserFragment extends BaseFragment {
    private static final String TAG = "PrintSettingsLaser";

    @BindView(R.id.bb_print_settings_bottom_bar)
    BottomBar mBbBottomBar;
    @BindView(R.id.vp_print_settings_view_pager)
    ViewPager mVpViewPager;

    @BindView(R.id.view_print_settings_laser_page_power)
    View mViewPagePower;
    @BindView(R.id.view_print_settings_laser_page_work_speed)
    View mViewPageFeedRate;

    private LaserPowerWidgetPresenter mLaserPowerWidgetPresenter;
    private FeedRateWidgetPresenter mFeedRateWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.print_settings_adjust_settings);

        Log.d(TAG, "enter fragment");

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_settings_laser;
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
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_laser_power, R.drawable.btn_all_laser_power_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_feed_rate, R.drawable.btn_all_work_speed_normal_64x64));
        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position -> mVpViewPager.setCurrentItem(position));
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mViewPagePower);
        views.add(mViewPageFeedRate);

        ViewPagerAdapter adapter = new ViewPagerAdapter(views);
        mVpViewPager.setAdapter(adapter);
        mVpViewPager.setOffscreenPageLimit(views.size());
        mVpViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mBbBottomBar.selectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mVpViewPager.setCurrentItem(0);
    }

    private void initLaserPowerPage() {
        mLaserPowerWidgetPresenter = new LaserPowerWidgetPresenter(getContext(), getModel(), disposables);
        mLaserPowerWidgetPresenter.bind(mViewPagePower);
        mLaserPowerWidgetPresenter.connectPrintSettings();
    }

    private void initFeedRatePage() {
        mFeedRateWidgetPresenter = new FeedRateWidgetPresenter(getContext(), getModel(), disposables);
        mFeedRateWidgetPresenter.bind(mViewPageFeedRate);
        mFeedRateWidgetPresenter.connectPrintSettings();
    }
}
