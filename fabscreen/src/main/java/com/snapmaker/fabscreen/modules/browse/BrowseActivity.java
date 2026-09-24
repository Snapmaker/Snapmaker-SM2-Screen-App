package com.snapmaker.fabscreen.modules.browse;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.update.UpdateFragment;

import butterknife.ButterKnife;
import fabscreen.libraries.legacy.base.BaseActivity;

public class BrowseActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);
        ButterKnife.bind(this);

        BrowseFragment fragment = new BrowseFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void gotoUpdateFragment(String filePath, boolean isLocal) {
        UpdateFragment fragment = new UpdateFragment();

        Bundle bundle = new Bundle();
        bundle.putString("file_path", filePath);
        bundle.putBoolean("is_local", isLocal);
        fragment.setArguments(bundle);

        addFragment(R.id.fragment_container, fragment);
    }
}
