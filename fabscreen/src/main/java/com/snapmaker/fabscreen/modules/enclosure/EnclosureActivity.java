package com.snapmaker.fabscreen.modules.enclosure;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class EnclosureActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        startEnclosureHome();
    }

    public void startEnclosureHome() {
        addFragment(R.id.fragment_container, EnclosureHomeFragment.getInstance());
    }

    public void startEnclosureSettings() {
        addFragment(R.id.fragment_container, EnclosureSettingsFragment.getInstance());
    }
}


