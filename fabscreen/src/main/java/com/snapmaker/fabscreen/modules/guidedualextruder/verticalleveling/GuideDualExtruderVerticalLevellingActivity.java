package com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto.Guide3DPDualExtruderAutoBedLevelingFragment;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.auto.Guide3DPDualExtruderAutoZHeightFragment;
import com.snapmaker.fabscreen.modules.guidedualextruder.verticalleveling.heat.Guide3DPDualExtruderLevelingHeatingFragment;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.lib.LogHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class GuideDualExtruderVerticalLevellingActivity extends BaseActivity {

    private GuideDualExtruderVerticalLevelingViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel(GuideDualExtruderVerticalLevelingViewModel.class);
        setContentView(R.layout.activity_default);
        showStart();
    }

    private void showStart() {
        addFragment(R.id.fragment_container, Guide3DPDualExtruderBedLevelingStartFragment.newInstance());
    }

    public void goHeating() {
        addFragment(R.id.fragment_container, Guide3DPDualExtruderLevelingHeatingFragment.newInstance());
    }

    public void goBedLeveling(boolean keepCurrentPageInStack) {
        Fragment targetFragment = Guide3DPDualExtruderAutoBedLevelingFragment.newInstance();
        if (!keepCurrentPageInStack) {
            removeTopFragment();
        }
        addFragment(R.id.fragment_container, targetFragment);
    }

    public void goZLeveling() {
        addFragment(R.id.fragment_container, Guide3DPDualExtruderAutoZHeightFragment.newInstance());
    }

    private void removeTopFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment);
        }
    }

    @Override
    public void finish() {
        mViewModel.coolDownBed()
                .observeOn(AndroidSchedulers.mainThread())
                .as(bindToLifecycle())
                .subscribe(success -> super.finish(), LogHelper::log);
    }
}
