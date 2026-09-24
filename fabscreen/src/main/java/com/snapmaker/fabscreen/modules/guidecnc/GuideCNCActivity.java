package com.snapmaker.fabscreen.modules.guidecnc;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidecnc.complete.GuideCNCCompleteFragment;
import com.snapmaker.fabscreen.modules.guidecnc.getstarted.GuideCNCGetStartedFragment;
import com.snapmaker.fabscreen.modules.guidecnc.preparematerial.GuideCNCPrepareMaterialFragment;
import com.snapmaker.fabscreen.modules.guidecnc.preparetoolhead.GuideCNCPrepareToolHeadFragment;
import com.snapmaker.fabscreen.modules.guidecnc.safety.GuideCNCSafetyGogglesFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class GuideCNCActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);

            startGetStartedFragment();
        }
    }

    /**
     * Guide CNC page 1, Get Started / Safety Instructions
     */
    public void startGetStartedFragment() {
        addFragment(R.id.fragment_container, GuideCNCGetStartedFragment.newInstance());
    }

    /**
     * Guide CNC page 2
     */
    public void startSafetyGogglesFragment() {
        addFragment(R.id.fragment_container, GuideCNCSafetyGogglesFragment.newInstance());
    }

    /**
     * Guide CNC page 3
     */
    public void startPrepareMaterialFragment() {
        addFragment(R.id.fragment_container, GuideCNCPrepareMaterialFragment.newInstance());
    }

    /**
     * Guide CNC page 4
     */
    public void startPrepareToolHeadFragment() {
        addFragment(R.id.fragment_container, GuideCNCPrepareToolHeadFragment.newInstance());
    }

    /**
     * Guide CNC page 5
     */
    public void startCompleteFragment() {
        addFragment(R.id.fragment_container, GuideCNCCompleteFragment.newInstance());
    }
}
