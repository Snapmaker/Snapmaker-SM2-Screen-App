package com.snapmaker.fabscreen.modules.print;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.preview.PreviewFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.data.Constants;
import fabscreen.libraries.legacy.route.RoutePath;
import fabscreen.libraries.legacy.view.AreaScrollDrawerLayout;

@Route(path = RoutePath.PRINT_ACTIVITY)
public class PrintActivity extends BaseActivity {

    @BindView(R.id.dl_print_layout)
    AreaScrollDrawerLayout mDlLayout;
    private OnDrawerOpenedListener mOnDrawerOpenedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_print);

        ButterKnife.bind(this);

        FragmentManager fragmentManager = getSupportFragmentManager();

        // master
        PrintFragment printFragment = new PrintFragment();
        fragmentManager.beginTransaction()
                .add(R.id.print_master_container, printFragment)
                .commit();

        // detail
        int headType = getModel().getMachineController().getHeadType();
        Fragment drawerFragment = null;
        switch (headType) {
            case Constants.HEAD_3DP: {
                drawerFragment = new PrintDrawer3DPFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.print_drawer_container, drawerFragment)
                        .commit();
                break;
            }
            case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                drawerFragment = new PrintDrawer3DPDualExtruderFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.print_drawer_container, drawerFragment)
                        .commit();
                break;
            }
            case Constants.HEAD_LASER:
            case Constants.HEAD_LASER_2W_IR:
            case Constants.HEAD_LASER_10W:
            case Constants.HEAD_LASER_20W:
            case Constants.HEAD_LASER_40W: {
                drawerFragment = new PrintDrawerLaserFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.print_drawer_container, drawerFragment)
                        .commit();
                break;
            }
            case Constants.HEAD_CNC:
            case Constants.HEAD_CNC_200W: {
                drawerFragment = new PrintDrawerCNCFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.print_drawer_container, drawerFragment)
                        .commit();
                break;
            }
            default:
                mDlLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                break;
        }

        mDlLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                if (mOnDrawerOpenedListener != null) {
                    mOnDrawerOpenedListener.onDrawerOpened();
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (mOnDrawerOpenedListener != null) {
                    mOnDrawerOpenedListener.onDrawerClosed();
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        mDlLayout.setScrollBelow(getScreenHigh() - getResources().getDimension(R.dimen.height_bottom_bar));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("force_refresh", false)) {
            Logger.d("refresh print.");
            FragmentManager fragmentManager = getSupportFragmentManager();
            final int fragmentCount = fragmentManager.getFragments().size();

            // Pop all fragments except the root one
            for (int i = 1; i < fragmentCount; i++) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            // master
            PrintFragment printFragment = new PrintFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.print_master_container, printFragment)
                    .commit();

            // detail
            int headType = getModel().getMachineController().getHeadType();
            Fragment drawerFragment = null;
            switch (headType) {
                case Constants.HEAD_3DP: {
                    drawerFragment = new PrintDrawer3DPFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.print_drawer_container, drawerFragment)
                            .commit();
                    break;
                }
                case Constants.HEAD_3DP_DUAL_EXTRUDER: {
                    drawerFragment = new PrintDrawer3DPDualExtruderFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.print_drawer_container, drawerFragment)
                            .commit();
                    break;
                }
                case Constants.HEAD_LASER:
                case Constants.HEAD_LASER_2W_IR:
                case Constants.HEAD_LASER_10W:
                case Constants.HEAD_LASER_20W:
                case Constants.HEAD_LASER_40W: {
                    drawerFragment = new PrintDrawerLaserFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.print_drawer_container, drawerFragment)
                            .commit();
                    break;
                }
                case Constants.HEAD_CNC:
                case Constants.HEAD_CNC_200W: {
                    drawerFragment = new PrintDrawerCNCFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.print_drawer_container, drawerFragment)
                            .commit();
                    break;
                }
                default:
                    mDlLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    break;
            }
        } else {
            Logger.d("onNewIntent print.");
        }
    }

    public void setOnDrawerOpenedListener(OnDrawerOpenedListener listener) {
        mOnDrawerOpenedListener = listener;
    }

    public void gotoChangeFilamentFragment() {
        PrintChangeFilamentFragment fragment = new PrintChangeFilamentFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void closeDrawer() {
        mDlLayout.closeDrawers();
        Logger.d("Print drawer closed.");
    }

    private int getScreenHigh() {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size.y;
    }

    interface OnDrawerOpenedListener {
        void onDrawerOpened();

        void onDrawerClosed();
    }
}
