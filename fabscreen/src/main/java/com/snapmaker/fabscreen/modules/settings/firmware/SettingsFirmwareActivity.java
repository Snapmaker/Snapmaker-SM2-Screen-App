package com.snapmaker.fabscreen.modules.settings.firmware;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.update.UpdateFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class SettingsFirmwareActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        Fragment fragment = SettingsFirmwareFragment.getInstance();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoUpdateFirmware(String filePath, Boolean isLocal) {
        UpdateFragment fragment = new UpdateFragment();

        Bundle bundle = new Bundle();
        bundle.putString("file_path", filePath);
        bundle.putBoolean("is_local", isLocal);
        fragment.setArguments(bundle);

        addFragment(R.id.fragment_container, fragment);
    }
}
