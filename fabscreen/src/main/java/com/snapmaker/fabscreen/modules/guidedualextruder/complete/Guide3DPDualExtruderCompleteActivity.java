package com.snapmaker.fabscreen.modules.guidedualextruder.complete;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.snapmaker.fabscreen.R;
import com.snapmaker.fabscreen.modules.guidedualextruder.getstarted.Guide3DPDualExtruderSafetyNotesFragment;

import fabscreen.libraries.legacy.base.BaseActivity;

public class Guide3DPDualExtruderCompleteActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.activity_default);
        }

        startGuideDualExtruderCompleteFragment();
    }

    public void startGuideDualExtruderCompleteFragment() {
        Guide3DPDualExtruderCompleteFragment fragment = new Guide3DPDualExtruderCompleteFragment();
        addFragment(R.id.fragment_container, fragment);
    }

}
