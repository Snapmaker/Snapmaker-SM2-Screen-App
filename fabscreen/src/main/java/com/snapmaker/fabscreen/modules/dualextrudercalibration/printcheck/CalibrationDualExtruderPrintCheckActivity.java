package com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration.DualExtruderXYCalibrationActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration.XYCalibrationPrintDrawer3DPDualExtruderFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.view.AreaScrollDrawerLayout;

public class CalibrationDualExtruderPrintCheckActivity extends BaseActivity {
    @BindView(R.id.dl_print_layout)
    AreaScrollDrawerLayout mDlLayout;

    private OnDrawerOpenedListener mOnDrawerOpenedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_print);
            ButterKnife.bind(this);
        }
        startPrintCheckGetStart();
    }

    public void startPrintCheckGetStart() {
        CalibrationDualExtruderPrintCheckGetStartedFragment fragment = new CalibrationDualExtruderPrintCheckGetStartedFragment();
        replaceFragment(R.id.print_master_container, fragment);
    }

    private void initDrawerFragment() {
        Fragment drawerFragment = null;
        drawerFragment = new CalibrationDualExtruderPrintCheckPrintDrawerFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.print_drawer_container, drawerFragment)
                .commit();
    }

    public void startPrintCheckPrint() {
        initDrawerFragment();

        mDlLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
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
        CalibrationDualExtruderPrintCheckPrintFragment fragment = new CalibrationDualExtruderPrintCheckPrintFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckPrintComplete() {
        CalibrationDualExtruderPrintCheckPrintCompleteFragment fragment = new CalibrationDualExtruderPrintCheckPrintCompleteFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckConfirmZ() {
        CalibrationDualExtruderPrintCheckConfirmZResultFragment fragment = new CalibrationDualExtruderPrintCheckConfirmZResultFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckConfirmXY() {
        CalibrationDualExtruderPrintCheckConfirmXYResultFragment fragment = new CalibrationDualExtruderPrintCheckConfirmXYResultFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckConfirmComplete() {
        CalibrationDualExtruderPrintCheckConfirmCompleteFragment fragment = new CalibrationDualExtruderPrintCheckConfirmCompleteFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void setOnDrawerOpenedListener(OnDrawerOpenedListener listener) {
        mOnDrawerOpenedListener = listener;
    }

    public void closeDrawer() {
        mDlLayout.closeDrawers();
        Logger.d("Print drawer closed.");
    }

    public void lockDrawer() {
        mDlLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
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
