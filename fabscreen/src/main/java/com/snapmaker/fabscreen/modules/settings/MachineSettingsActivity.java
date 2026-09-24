package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class MachineSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        MachineSettingsFragment fragment = new MachineSettingsFragment();
        addFragment(R.id.fragment_container, fragment);
    }
}
