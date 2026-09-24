package com.snapmaker.fabscreen.modules.airpurifier;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class AirPurifierActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        startAirPurifierHome();
    }

    public void startAirPurifierHome() {
        addFragment(R.id.fragment_container, AirPurifierHomeFragment.newInstance());
    }
}


