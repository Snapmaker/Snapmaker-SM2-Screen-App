package com.snapmaker.fabscreen.modules.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import fabscreen.libraries.legacy.base.BaseFragment;
import fabscreen.libraries.legacy.base.BaseViewModel;

public class SettingsSecurityFragment extends BaseFragment {
    public static SettingsSecurityFragment getInstance() {
        return new SettingsSecurityFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTitle(R.string.settings_terms_and_conditions);
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_settings_security;
    }

    @Override
    protected BaseViewModel getViewModel() {
        return null;
    }
}
