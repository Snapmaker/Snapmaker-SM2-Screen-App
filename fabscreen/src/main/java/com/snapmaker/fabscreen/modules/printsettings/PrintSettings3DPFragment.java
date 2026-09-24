package com.snapmaker.fabscreen.modules.printsettings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import com.snapmaker.fabscreen.modules.common.FeedRateWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.HeatedBedWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.NozzleWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ZOffsetWidgetPresenter;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;

import java.util.ArrayList;

import butterknife.BindView;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class PrintSettings3DPFragment extends BaseFragment {
    private static final String TAG = "PrintSettings3DP";

    @BindView(R.id.bb_print_settings_bottom_bar)
    BottomBar mBbBottomBar;
    @BindView(R.id.vp_print_settings_view_pager)
    ViewPager mVpViewPager;

    @BindView(R.id.view_print_settings_3dp_page_nozzle)
    View mViewPageNozzle;
    @BindView(R.id.view_print_settings_3dp_page_heated_bed)
    View mViewPageHeatedBed;
    @BindView(R.id.view_print_settings_3dp_page_z_offset)
    View mViewPageZOffset;
    @BindView(R.id.view_print_settings_3dp_page_work_speed)
    View mViewPageFeedRate;

    private NozzleWidgetPresenter mNozzleWidgetPresenter;
    private HeatedBedWidgetPresenter mHeatedBedWidgetPresenter;
    private ZOffsetWidgetPresenter mZOffsetWidgetPresenter;
    private FeedRateWidgetPresenter mFeedRateWidgetPresenter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.print_settings_adjust_settings);

        initView();
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_print_settings_3dp;
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
        initZOffsetPage();
        initFeedRatePage();
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_nozzle, R.drawable.btn_all_nozzle_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_heated_bed, R.drawable.btn_all_heated_bed_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_z_offset, R.drawable.btn_all_z_offset_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_feed_rate, R.drawable.btn_all_work_speed_normal_64x64));
        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position -> mVpViewPager.setCurrentItem(position));
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mViewPageNozzle);
        views.add(mViewPageHeatedBed);
        views.add(mViewPageZOffset);
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

    private void initNozzlePage() {
        mNozzleWidgetPresenter = new NozzleWidgetPresenter(getContext(), getModel(), disposables);
        mNozzleWidgetPresenter.bind(mViewPageNozzle);
        mNozzleWidgetPresenter.connectPrintSettings();
    }

    private void initHeatedBedPage() {
        mHeatedBedWidgetPresenter = new HeatedBedWidgetPresenter(getContext(), getModel(), disposables);
        mHeatedBedWidgetPresenter.bind(mViewPageHeatedBed);
        mHeatedBedWidgetPresenter.connectPrintSettings();
    }

    private void initZOffsetPage() {
        mZOffsetWidgetPresenter = new ZOffsetWidgetPresenter(getContext(), getModel(), disposables);
        mZOffsetWidgetPresenter.bind(mViewPageZOffset);
        // Get live ZOffset value from controller
        getModel().getSlaveComputer()
                .getAdjustSettingZOffset()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(settings -> {
                    Logger.d("get live ZOffset %.1f", settings.value);
                    mZOffsetWidgetPresenter.connectPrintSettings(settings.value);
                }, LogHelper::log);
    }

    private void initFeedRatePage() {
        mFeedRateWidgetPresenter = new FeedRateWidgetPresenter(getContext(), getModel(), disposables);
        mFeedRateWidgetPresenter.bind(mViewPageFeedRate);
        mFeedRateWidgetPresenter.connectPrintSettings();
    }
}
