package com.snapmaker.fabscreen.modules.remote;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.orhanobut.logger.Logger;
import com.snapmaker.fabscreen.R;

import fabscreen.libraries.legacy.base.BaseActivity;
import fabscreen.libraries.legacy.route.RoutePath;

@Route(path = RoutePath.REMOTE_ACTIVITY)
public class RemoteActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default);

        Fragment fragment = RemoteHomeFragment.getInstance();
        replaceFragment(R.id.fragment_container, fragment);
    }
}
