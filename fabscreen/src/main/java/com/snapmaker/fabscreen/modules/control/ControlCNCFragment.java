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
import com.snapmaker.fabscreen.modules.common.CoordinateXYZBGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.SetOrigin4AxisWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SetOriginWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SpindleWidgetPresenter;

import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ControlCNCFragment extends BaseFragment {
    private static final String TAG = "ControlCNCFragment";

    @BindView(R.id.top_bar_back)
    Button mBtnBack;

    @BindView(R.id.vp_control_cnc)
    ViewPager mControlViewPager;

    @BindView(R.id.bb_control_bottom_bar)
    BottomBar mBbBottomBar;

    // 4th Axes
    @BindView(R.id.vp_control_4axis_panels)
    ViewPager mVpControlPanel;
    @BindView(R.id.tl_control_4axis_panel_indicator)
    TabLayout mTlControlPanel;
    @BindView(R.id.widget_control_panel_xyz_axes_for_4axis)
    View mViewControlPanel;
    @BindView(R.id.widget_control_panel_b_axis)
    View mViewRotaryPanel;

    @BindView(R.id.view_control_laser_page_axes)
    View mViewPageAxes;
    @BindView(R.id.view_control_laser_page_4axis)
    View mViewPage4thAxes;
    @BindView(R.id.view_control_laser_page_set_origin)
    View mViewPageSetOrigin;
    @BindView(R.id.view_control_laser_page_4axis_set_origin)
    View mViewPage4AxisSetOrigin;
    @BindView(R.id.view_control_cnc_page_spindle)
    View mViewPageSpindle;

    // spindle
    @BindView(R.id.btn_control_cnc_page_spindle_switch)
    Button mBtnSwitch;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;
    // axes
    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlXYZPanelWidgetPresenter;

    // rotary
    private boolean mIsRotaryAvailable = false;
    private CoordinateXYZBGeminiWidgetPresenter mCoordinateXYZBWidgetPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelWidgetPresenter;

    // set origin
    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter2;
    private CoordinateXYZBGeminiWidgetPresenter mCoordinateXYZBWidgetPresenter2;
    private SetOriginWidgetPresenter mSetOriginPresenter;
    private SetOrigin4AxisWidgetPresenter mSetOrigin4AxisWidgetPresenter;

    // spindle
    private SpindleWidgetPresenter mSpindleWidgetPresenter;

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
        return R.layout.fragment_control_cnc;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }

    private void initView() {
        initBottomBar();
        initViewPager();

        if (mIsRotaryAvailable) {
            init4thAxesPage();
            init4thAxisSetOriginPage();
        } else {
            initAxesPage();
            initSetOriginPage();
        }

        initSpindlePage();

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(1);
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_jog_mode, R.drawable.btn_all_axes_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_set_origin, R.drawable.btn_all_set_origin_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_spindle_spend, R.drawable.btn_all_work_speed_normal_64x64));
        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position -> mControlViewPager.setCurrentItem(position));
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mIsRotaryAvailable ? mViewPage4thAxes : mViewPageAxes);
        views.add(mIsRotaryAvailable ? mViewPage4AxisSetOrigin : mViewPageSetOrigin);
        views.add(mViewPageSpindle);

        ViewPagerAdapter adapter = new ViewPagerAdapter(views);
        mControlViewPager.setAdapter(adapter);
        mControlViewPager.setOffscreenPageLimit(views.size());
        mControlViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

            @Override
            public void onPageSelected(int position) {
                mBbBottomBar.selectTab(position);
            }
        });
    }

    private void initAxesPage() {
        mCoordinateXYZWidgetPresenter = new CoordinateXYZGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZWidgetPresenter.bind(mViewPageAxes);
        mCoordinateXYZWidgetPresenter.connect();

        mControlXYZPanelWidgetPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelWidgetPresenter.bind(mViewPageAxes, 0.1f, 1f, 10f);
        mControlXYZPanelWidgetPresenter.connect();
    }

    private void init4thAxesPage() {
        mCoordinateXYZBWidgetPresenter = new CoordinateXYZBGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZBWidgetPresenter.bind(mViewPage4thAxes);
        mCoordinateXYZBWidgetPresenter.connect();

        initControlPanelViewPager();

        // xyz axes control
        mControlXYZPanelWidgetPresenter = new ControlXYZPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlXYZPanelWidgetPresenter.bind(mViewPage4thAxes, 0.1f, 1f, 5f);
        mControlXYZPanelWidgetPresenter.connect();

        // b axis control
        mControlBAxisPanelWidgetPresenter = new ControlBAxisPanelWidgetPresenter(getContext(), getModel(), disposables);
        mControlBAxisPanelWidgetPresenter.bind(mViewPage4thAxes);
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

    private void initSetOriginPage() {
        mCoordinateXYZWidgetPresenter2 = new CoordinateXYZGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZWidgetPresenter2.bind(mViewPageSetOrigin);
        mCoordinateXYZWidgetPresenter2.connect();

        mSetOriginPresenter = new SetOriginWidgetPresenter(getContext(), getModel(), disposables);
        mSetOriginPresenter.bind(mViewPageSetOrigin);
        mSetOriginPresenter.connect();
        mSetOriginPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> mBtnBack.setVisibility(movingEvent ? View.INVISIBLE : View.VISIBLE));
    }

    private void init4thAxisSetOriginPage() {
        mCoordinateXYZBWidgetPresenter2 = new CoordinateXYZBGeminiWidgetPresenter(getContext(), getModel(), disposables);
        mCoordinateXYZBWidgetPresenter2.bind(mViewPage4AxisSetOrigin);
        mCoordinateXYZBWidgetPresenter2.connect();

        mSetOrigin4AxisWidgetPresenter = new SetOrigin4AxisWidgetPresenter(getContext(), getModel(), disposables);
        mSetOrigin4AxisWidgetPresenter.bind(mViewPage4AxisSetOrigin);
        mSetOrigin4AxisWidgetPresenter.connect();
        mSetOrigin4AxisWidgetPresenter.getMovingEventObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(movingEvent -> mBtnBack.setVisibility(movingEvent ? View.INVISIBLE : View.VISIBLE));
    }

    private void initSpindlePage() {
        mSpindleWidgetPresenter = new SpindleWidgetPresenter(getContext(), getModel(), disposables);
        mSpindleWidgetPresenter.bind(mViewPageSpindle);
        mSpindleWidgetPresenter.connectControl();
    }

    @OnClick(R.id.btn_control_cnc_page_spindle_switch)
    void onClickSwitch() {
        if (mBtnSwitch.isActivated()) {
            mBtnSwitch.setActivated(false);
            getModel().getSlaveComputer().sendGcode("M5")
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        /**/
                    }, LogHelper::log);
        } else {
            mBtnSwitch.setActivated(true);
            float targetValue = mSpindleWidgetPresenter.getTargetValue();
            String gcodeCommand = "M3";
            if (getModel().getMachineController().getHeadType() == Constants.HEAD_CNC) {
                gcodeCommand = String.format(Locale.US, "M3 P%.1f", targetValue);
            } else if (getModel().getMachineController().getHeadType() == Constants.HEAD_CNC_200W) {
                gcodeCommand = String.format(Locale.US, "M3 S%.1f", targetValue);
            }
            getModel().getSlaveComputer().sendGcode(gcodeCommand)
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe(response -> {
                        /**/
                    }, LogHelper::log);
        }
    }

    @Override
    protected void back() {
        getModel().getSlaveComputer().sendGcode("M5")
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> super.back());
    }
}
