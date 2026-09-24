package com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.auto.AutoBedLevelingFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.heat.LevelingHeatingFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.manual.ManualBedLevelingFragment;
import com.snapmaker.fabscreen.modules.dualextrudercalibration.bedleveling.manual.ManualLevelingCompleteFragment;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DualExtruderBedLevellingActivity extends BaseActivity {

    private DualExtruderBedLevelingViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel(DualExtruderBedLevelingViewModel.class);
        setContentView(R.layout.activity_default);
        showStart();
    }

    private void showStart() {
        addFragment(R.id.fragment_container, BedLevelingStartFragment.newInstance());
    }

    public void goHeating() {
        addFragment(R.id.fragment_container, LevelingHeatingFragment.newInstance());
    }

    public void goLeveling(boolean keepCurrentPageInStack) {
        Fragment targetFragment = mViewModel.getIfUserSelectAuto() ? AutoBedLevelingFragment.newInstance() : ManualBedLevelingFragment.newInstance();
        if (!keepCurrentPageInStack) {
            removeTopFragment();
        }
        addFragment(R.id.fragment_container, targetFragment);
    }

    private void removeTopFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment);
        }
    }

    public void goManualLevelComplete() {
        addFragment(R.id.fragment_container, ManualLevelingCompleteFragment.newInstance());
    }

    @Override
    public void finish() {
        mViewModel.coolDownBed()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> super.finish(), LogHelper::log);
    }
}
