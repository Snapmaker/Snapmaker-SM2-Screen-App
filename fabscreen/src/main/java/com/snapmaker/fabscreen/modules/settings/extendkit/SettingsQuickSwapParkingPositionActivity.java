package com.snapmaker.fabscreen.modules.settings.extendkit;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class SettingsQuickSwapParkingPositionActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        startQuickSwapSetUpParkingPosition();
    }

    public void startQuickSwapSetUpParkingPosition() {
        SettingsQuickSwapParkingPositionFragment fragment = new SettingsQuickSwapParkingPositionFragment();
        addFragment(R.id.fragment_container, fragment);
    }

}
