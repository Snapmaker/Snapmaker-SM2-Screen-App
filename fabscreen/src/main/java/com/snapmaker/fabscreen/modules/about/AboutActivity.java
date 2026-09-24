package com.snapmaker.fabscreen.modules.about;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        Fragment fragment = SettingsAboutFragment.getInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoChangeName() {
        Fragment fragment = SettingsNameFragment.getInstance();
        addFragment(R.id.fragment_container, fragment);
    }
}
