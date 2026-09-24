package com.snapmaker.fabscreen.modules.dualextrudercalibration.entries;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class DualExtruderCalibrationEntriesActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);
        showDispatcherPage();
    }

    private void showDispatcherPage() {
        addFragment(R.id.fragment_container, CalibrationDispatcherFragment.newInstance());
    }
}
