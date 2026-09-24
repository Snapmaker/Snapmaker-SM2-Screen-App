package com.snapmaker.fabscreen.modules.settings.extendkit;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class SettingsExtendKitActivity extends BaseActivity {
    private SettingsExtendKitViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);
        mViewModel = getViewModel(SettingsExtendKitViewModel.class);

        startQuickSwapSetUp();
    }

    public void startQuickSwapSetUp() {
        SettingsQuickSwapFragment fragment = new SettingsQuickSwapFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void startBracingKitSetUp() {
        SettingsBracingKitFragment fragment = new SettingsBracingKitFragment();
        addFragment(R.id.fragment_container, fragment);
    }

}
