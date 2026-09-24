package com.snapmaker.fabscreen.modules.update;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;

public class UpdateActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        Intent intent = getIntent();
        Fragment fragment = new UpdateFragment();

        Bundle bundle = new Bundle();
        bundle.putBoolean("is_local", intent.getBooleanExtra("is_local", false));
        bundle.putString("file_path", intent.getStringExtra("file_path"));
        fragment.setArguments(bundle);

        addFragment(R.id.fragment_container, fragment);
    }
}
