package com.snapmaker.fabscreen.modules.guidedualextruder.printcheck;

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
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckActivity;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckConfirmCompleteFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckConfirmXYResultFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckConfirmZResultFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckPrintCompleteFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.printcheck.CalibrationDualExtruderPrintCheckPrintDrawerFragment;
import com.snapmaker.fabscreen.modules.guidedualextruder.xycalibration.Guide3DPDualExtruderXYCalibrationCheckResultFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.view.AreaScrollDrawerLayout;

public class Guide3DPDualExtruderPrintCheckActivity extends BaseActivity {
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
        startPrintCheckGetStarted();
    }

    public void startPrintCheckGetStarted() {
        Guide3DPDualExtruderPrintCheckGetStartedFragment fragment = new Guide3DPDualExtruderPrintCheckGetStartedFragment();
        replaceFragment(R.id.print_master_container, fragment);
    }

    private void initDrawerFragment() {
        Fragment drawerFragment = null;
        drawerFragment = new Guide3DPDualExtruderPrintCheckPrintDrawerFragment();
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
        addFragment(R.id.print_master_container, new Guide3DPDualExtruderPrintCheckPrintFragment());
    }

    public void startPrintCheckPrintComplete() {
        addFragment(R.id.print_master_container, new Guide3DPDualExtruderPrintCheckPrintCompleteFragment());
    }

    public void startPrintCheckConfirmZ() {
        Guide3DPDualExtruderPrintCheckConfirmZResultFragment fragment = new Guide3DPDualExtruderPrintCheckConfirmZResultFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckConfirmXY() {
        Guide3DPDualExtruderPrintCheckConfirmXYResultFragment fragment = new Guide3DPDualExtruderPrintCheckConfirmXYResultFragment();
        addFragment(R.id.print_master_container, fragment);
    }

    public void startPrintCheckConfirmComplete() {
        Guide3DPDualExtruderPrintCheckConfirmCompleteFragment fragment = new Guide3DPDualExtruderPrintCheckConfirmCompleteFragment();
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
