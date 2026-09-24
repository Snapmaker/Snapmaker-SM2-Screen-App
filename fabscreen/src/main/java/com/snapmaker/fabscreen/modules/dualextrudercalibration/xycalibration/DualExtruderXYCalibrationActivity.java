package com.snapmaker.fabscreen.modules.dualextrudercalibration.xycalibration;

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
import androidx.lifecycle.ViewModelProvider;

import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.view.AreaScrollDrawerLayout;

public class DualExtruderXYCalibrationActivity extends BaseActivity {
    @BindView(R.id.dl_print_layout)
    AreaScrollDrawerLayout mDlLayout;

    private OnDrawerOpenedListener mOnDrawerOpenedListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        ButterKnife.bind(this);
        goXYCalibrationStart();
    }

    @Override
    protected void onDestroy() {
        DualExtruderXYCalibrationPrintViewModel viewModel = new ViewModelProvider(this).get(DualExtruderXYCalibrationPrintViewModel.class);
        viewModel.onDestroy();
        super.onDestroy();
    }

    private void initDrawerFragment() {
        Fragment drawerFragment = null;
        drawerFragment = new XYCalibrationPrintDrawer3DPDualExtruderFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.print_drawer_container, drawerFragment)
                .commit();
    }

    public void goXYCalibrationCheckResult() {
        addFragment(R.id.print_master_container, DualExtruderXYCalibrationCheckResultFragment.newInstance());
    }

    public void goXYCalibrationStart() {
        replaceFragment(R.id.print_master_container, DualExtruderXYCalibrationStartFragment.newInstance());
    }

    public void goXYCalibrationPrint() {
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

        addFragment(R.id.print_master_container, new DualExtruderXYCalibrationPrintFragment());
    }

    public void goXYCalibrationPrintComplete() {
        addFragment(R.id.print_master_container, new DualExtruderXYCalibrationPrintCompleteFragment());
    }

    public void goXYCalibrationCheckResultIntro() {
        addFragment(R.id.print_master_container, new DualExtruderXYCalibrationCheckResultIntroFragment());
    }

    public void goXYCalibrationComplete() {
        addFragment(R.id.print_master_container, new DualExtruderXYCalibrationCompleteFragment());
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
