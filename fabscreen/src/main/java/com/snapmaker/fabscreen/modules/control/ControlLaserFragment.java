package com.snapmaker.fabscreen.modules.control;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.common.ControlBAxisPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.ControlXYZPanelWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateSystemPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZBGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.CoordinateXYZGeminiWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.LaserPowerWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SetOrigin4AxisWidgetPresenter;
import com.snapmaker.fabscreen.modules.common.SetOriginWidgetPresenter;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.lib.LogHelper;
import fabscreen.libraries.legacy.view.FabConfirm;
import fabscreen.libraries.legacy.view.ViewPagerAdapter;
import fabscreen.libraries.legacy.view.ViewUtils;
import fabscreen.libraries.legacy.view.bottombar.BottomBar;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class ControlLaserFragment extends BaseFragment {
    private static final String TAG = "ControlLaserFragment";

    @BindView(R.id.top_bar_back)
    Button mBtnBack;
    @BindView(R.id.bb_control_bottom_bar)
    BottomBar mBbBottomBar;

    @BindView(R.id.vp_control_laser)
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

    @BindView(R.id.view_control_laser_page_axes)
    View mViewPageAxes;
    @BindView(R.id.view_control_laser_page_4axis)
    View mViewPage4thAxes;
    @BindView(R.id.view_control_laser_page_set_origin)
    View mViewPageSetOrigin;
    @BindView(R.id.view_control_laser_page_4axis_set_origin)
    View mViewPage4AxisSetOrigin;
    @BindView(R.id.view_control_laser_page_power)
    View mViewPageLaserPower;
    @BindView(R.id.view_control_10w_laser_page_power)
    View mViewPage10wLaserPower;

    // power
    @BindView(R.id.iv_control_laser_page_power)
    ImageView mIvPower;

    Button mBtnSwitch;
    private boolean mIsLaserLimitMode;
    private boolean mIsRotaryAvailable = false;

    private CoordinateSystemPresenter mCoordinateSystemPresenter;
    // axes
    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter;
    private ControlXYZPanelWidgetPresenter mControlXYZPanelWidgetPresenter;

    // rotary
    private CoordinateXYZBGeminiWidgetPresenter mCoordinateXYZBWidgetPresenter;
    private ControlBAxisPanelWidgetPresenter mControlBAxisPanelWidgetPresenter;

    // set origin
    private CoordinateXYZGeminiWidgetPresenter mCoordinateXYZWidgetPresenter2;
    private CoordinateXYZBGeminiWidgetPresenter mCoordinateXYZBWidgetPresenter2;
    private SetOriginWidgetPresenter mSetOriginPresenter;
    private SetOrigin4AxisWidgetPresenter mSetOrigin4AxisWidgetPresenter;

    // power
    private LaserPowerWidgetPresenter mLaserPowerWidgetPresenter;

    private int mHeadType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHeadType = getModel().getMachineController().getHeadType();
        mIsLaserLimitMode = mHeadType != Constants.HEAD_LASER && !getModel().getPreferences().getDebugFlag();
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
        return R.layout.fragment_control_laser;
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

        if (getModel().getMachineController().getHeadType() == Constants.HEAD_LASER_2W_IR) {
            // 2W Laser would not support laser power page,
            // which would not display or init power page when 2W Laser is plugged.
        } else {
            if (!mIsLaserLimitMode) {
                initLaserPowerPage();
            }
        }
        initSwitch();

        mCoordinateSystemPresenter = new CoordinateSystemPresenter(getContext(), getModel(), disposables);
        mCoordinateSystemPresenter.ensureCoordinate(1);
    }

    private void initSwitch() {
        if (mIsLaserLimitMode) {
            mBtnSwitch = mViewPage10wLaserPower.findViewById(R.id.btn_control_10w_laser_page_power_switch);
        } else {
            mBtnSwitch = mViewPageLaserPower.findViewById(R.id.btn_control_laser_page_power_switch);
        }
        mBtnSwitch.setOnClickListener(this::onClickSwitch);
        getModel().getSlaveComputer().watchHeaderSecurityStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(headerSecurity -> {
                    mBtnSwitch.setActivated(false);
                    mIvPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_off);
                });
    }

    private void initBottomBar() {
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_jog_mode, R.drawable.btn_all_axes_normal_64x64));
        mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.control_set_origin, R.drawable.btn_all_set_origin_normal_64x64));
        if (getModel().getMachineController().getHeadType() != Constants.HEAD_LASER_2W_IR) {
            mBbBottomBar.addItem(ViewUtils.createBottomBarItem(R.string.print_laser_power, R.drawable.btn_all_laser_power_normal_64x64));
        }
        mBbBottomBar.setBarBackgroundColor(R.color.bottom_bar_background);
        mBbBottomBar.initialize();

        mBbBottomBar.selectTab(0);
        mBbBottomBar.setOnTabSelectedListener(position ->
        {
            shutDownLaser();
            mControlViewPager.setCurrentItem(position);
        });
    }

    private void initViewPager() {
        ArrayList<View> views = new ArrayList<>();

        views.add(mIsRotaryAvailable ? mViewPage4thAxes : mViewPageAxes);
        views.add(mIsRotaryAvailable ? mViewPage4AxisSetOrigin : mViewPageSetOrigin);
        if (getModel().getMachineController().getHeadType() != Constants.HEAD_LASER_2W_IR) {
            views.add(mIsLaserLimitMode ? mViewPage10wLaserPower : mViewPageLaserPower);
        }

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

    private void initLaserPowerPage() {
        mLaserPowerWidgetPresenter = new LaserPowerWidgetPresenter(getContext(), getModel(), disposables);
        mLaserPowerWidgetPresenter.bind(mViewPageLaserPower);
        mLaserPowerWidgetPresenter.connectControl();
    }

    void onClickSwitch(View view) {
        if (mBtnSwitch.isActivated()) {
            mBtnSwitch.setActivated(false);
            getModel().getSlaveComputer().sendGcode("M5")
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(bindToLifecycle())
                    .subscribe();
            if (mIsLaserLimitMode) {
                mIvPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_off);
            }
        } else {
            if (mIsLaserLimitMode) {
                int headType = getModel().getMachineController().getHeadType();
                int confirmDialogDescResId = R.string.controller_laser_10w_safety_goggles_message;
                switch (headType) {
                    case Constants.HEAD_LASER_20W:
                        confirmDialogDescResId = R.string.controller_laser_20w_safety_goggles_message;
                        break;
                    case Constants.HEAD_LASER_40W:
                        confirmDialogDescResId = R.string.controller_laser_40w_safety_goggles_message;
                        break;
                    case Constants.HEAD_LASER_10W:
                        confirmDialogDescResId = R.string.controller_laser_10w_safety_goggles_message;
                        break;
                    default:
                        break;
                }
                final int finalConfirmDialogDescResId = confirmDialogDescResId;
                float laserPower = getModel().getMachineController().getLaserOutputPower();
                getModel().getPrintController().getHeaderSecurityStatus()
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe(headerSecurity -> {
                            if (headerSecurity.status == 0) {
                                FabConfirm.create(getContext())
                                        .setCanceledOnTouchOutSide(false)
                                        .setIcon(R.drawable.pic_dialog_warning_72x72)
                                        .setDescription(finalConfirmDialogDescResId)
                                        .setConfirm(R.string.all_ok, (dialog, which) -> {
                                            dialog.dismiss();
                                            mBtnSwitch.setActivated(true);
                                            mIvPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_on);
                                            getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "M3 P%.1f", laserPower))
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .as(bindToLifecycle())
                                                    .subscribe();
                                        })
                                        .show();
                            }
                        }, LogHelper::log);
            } else {
                mBtnSwitch.setActivated(true);
                float targetValue = mLaserPowerWidgetPresenter.getTargetValue();
                getModel().getSlaveComputer().sendGcode(String.format(Locale.US, "M3 P%.1f", targetValue))
                        .observeOn(AndroidSchedulers.mainThread())
                        .as(bindToLifecycle())
                        .subscribe();
            }
        }
    }

    private void shutDownLaser() {
        getModel().getSlaveComputer().sendGcode("M5")
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe();
        mBtnSwitch.setActivated(false);
        if (mIsLaserLimitMode) {
            mIvPower.setImageResource(R.drawable.ic_control_10w_laser_page_power_off);
        }
    }

    @Override
    protected void back() {
        shutDownLaser();
        super.back();
    }
}
