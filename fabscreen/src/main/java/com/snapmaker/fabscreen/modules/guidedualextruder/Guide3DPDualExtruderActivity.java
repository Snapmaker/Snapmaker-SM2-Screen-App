package com.snapmaker.fabscreen.modules.guidedualextruder;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.getstarted.Guide3DPDualExtruderGetStartedFragment;
import com.snapmaker.fabscreen.modules.guidedualextruder.getstarted.Guide3DPDualExtruderSafetyNotesFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide3DPDualExtruderActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);
        }

        startGuideDualExtruderSafetyNotesFragment();
    }

    public void startGuideDualExtruderGetStartedFragment() {
        Guide3DPDualExtruderGetStartedFragment fragment = new Guide3DPDualExtruderGetStartedFragment();
        addFragment(R.id.fragment_container, fragment);
    }

    public void startGuideDualExtruderSafetyNotesFragment() {
        Guide3DPDualExtruderSafetyNotesFragment fragment = new Guide3DPDualExtruderSafetyNotesFragment();
        addFragment(R.id.fragment_container, fragment);
    }

}
