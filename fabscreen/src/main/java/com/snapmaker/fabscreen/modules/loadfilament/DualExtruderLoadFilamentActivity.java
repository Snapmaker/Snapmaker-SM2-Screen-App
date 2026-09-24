package com.snapmaker.fabscreen.modules.loadfilament;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class DualExtruderLoadFilamentActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

        }

        DualExtruderLoadFilamentFragment fragment = new DualExtruderLoadFilamentFragment();
        addFragment(R.id.fragment_container, fragment);
    }
}
